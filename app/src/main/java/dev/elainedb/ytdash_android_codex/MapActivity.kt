package dev.elainedb.ytdash_android_codex

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import coil.load
import com.google.android.material.bottomsheet.BottomSheetBehavior
import dagger.hilt.android.AndroidEntryPoint
import dev.elainedb.ytdash_android_codex.domain.repository.VideoRepository
import dev.elainedb.ytdash_android_codex.domain.model.Video
import dev.elainedb.ytdash_android_codex.core.error.Result
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import javax.inject.Inject

@AndroidEntryPoint
class MapActivity : AppCompatActivity() {
    @Inject lateinit var videoRepository: VideoRepository

    private lateinit var mapView: MapView
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>
    private var selectedVideo: Video? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        Configuration.getInstance().userAgentValue = "${BuildConfig.APPLICATION_ID}/1.0"
        mapView = findViewById(R.id.mapView)
        bottomSheetBehavior = BottomSheetBehavior.from(findViewById(R.id.bottomSheet)).apply {
            state = BottomSheetBehavior.STATE_HIDDEN
            peekHeight = resources.displayMetrics.heightPixels / 4
            isHideable = true
        }

        lifecycleScope.launch {
            when (val result = videoRepository.getVideosWithLocation()) {
                is Result.Success -> renderMarkers(result.data)
                is Result.Error -> findViewById<TextView>(R.id.bottomSheetTitle).text = result.failure.message
            }
        }
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        mapView.onPause()
        super.onPause()
    }

    private fun renderMarkers(videos: List<Video>) {
        if (videos.isEmpty()) {
            findViewById<TextView>(R.id.bottomSheetTitle).text = getString(R.string.map_empty)
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            return
        }

        val points = mutableListOf<GeoPoint>()
        videos.forEach { video ->
            val latitude = video.locationLatitude ?: return@forEach
            val longitude = video.locationLongitude ?: return@forEach
            if (!latitude.isValidMapLatitude() || !longitude.isValidMapLongitude()) {
                return@forEach
            }
            val point = GeoPoint(latitude, longitude)
            points += point
            val marker = Marker(mapView).apply {
                position = point
                title = video.title
                setOnMarkerClickListener { _, _ ->
                    showVideo(video)
                    true
                }
            }
            mapView.overlays.add(marker)
        }
        mapView.invalidate()
        if (points.isNotEmpty()) {
            mapView.zoomController.setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.ALWAYS)
            mapView.setMultiTouchControls(true)
            fitMapToPoints(points)
        } else {
            findViewById<TextView>(R.id.bottomSheetTitle).text = getString(R.string.map_empty)
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        }
    }

    private fun fitMapToPoints(points: List<GeoPoint>) {
        val validPoints = points.filter { point ->
            point.latitude.isFinite() &&
                point.longitude.isFinite() &&
                point.latitude.isValidMapLatitude() &&
                point.longitude.isValidMapLongitude()
        }
        if (validPoints.isEmpty()) {
            findViewById<TextView>(R.id.bottomSheetTitle).text = getString(R.string.map_empty)
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            return
        }

        if (validPoints.size == 1) {
            mapView.controller.setZoom(12.0)
            mapView.controller.setCenter(validPoints.first())
            return
        }

        val north = validPoints.maxOf { it.latitude }.coerceAtMost(MAX_LATITUDE)
        val south = validPoints.minOf { it.latitude }.coerceAtLeast(MIN_LATITUDE)
        val east = validPoints.maxOf { it.longitude }.coerceAtMost(MAX_LONGITUDE)
        val west = validPoints.minOf { it.longitude }.coerceAtLeast(MIN_LONGITUDE)

        if (north <= south || east <= west) {
            mapView.controller.setZoom(5.0)
            mapView.controller.setCenter(validPoints.first())
            return
        }

        val boundingBox = BoundingBox(north, east, south, west)
        mapView.post {
            runCatching { mapView.zoomToBoundingBox(boundingBox, true, 80) }
                .onFailure {
                    mapView.controller.setZoom(5.0)
                    mapView.controller.setCenter(validPoints.first())
                }
        }
    }

    private fun showVideo(video: Video) {
        selectedVideo = video
        findViewById<ImageView>(R.id.bottomSheetThumbnail).load(video.thumbnailUrl)
        findViewById<TextView>(R.id.bottomSheetTitle).text = video.title
        findViewById<TextView>(R.id.bottomSheetChannel).text = video.channelName
        findViewById<TextView>(R.id.bottomSheetPublishedAt).text = video.publishedAt.take(10)
        findViewById<TextView>(R.id.bottomSheetTags).text = video.tags.joinToString(", ")
        findViewById<TextView>(R.id.bottomSheetLocation).text = buildString {
            append(listOfNotNull(video.locationCity, video.locationCountry).joinToString(", "))
            if (video.locationLatitude != null && video.locationLongitude != null) {
                if (isNotBlank()) append(" • ")
                append("${video.locationLatitude}, ${video.locationLongitude}")
            }
        }
        findViewById<TextView>(R.id.bottomSheetRecordingDate).text = video.recordingDate?.take(10).orEmpty()
        findViewById<View>(R.id.bottomSheet).setOnClickListener {
            openVideo(video.id)
        }
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
    }

    private fun openVideo(videoId: String) {
        val appIntent = Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:$videoId"))
        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=$videoId"))
        runCatching { startActivity(appIntent) }
            .onFailure { startActivity(webIntent) }
    }

    companion object {
        private const val MIN_LATITUDE = -85.05112877980658
        private const val MAX_LATITUDE = 85.05112877980658
        private const val MIN_LONGITUDE = -180.0
        private const val MAX_LONGITUDE = 180.0

        fun newIntent(context: Context): Intent = Intent(context, MapActivity::class.java)
    }
}

private fun Double.isValidMapLatitude(): Boolean {
    return this in -85.05112877980658..85.05112877980658
}

private fun Double.isValidMapLongitude(): Boolean {
    return this in -180.0..180.0
}
