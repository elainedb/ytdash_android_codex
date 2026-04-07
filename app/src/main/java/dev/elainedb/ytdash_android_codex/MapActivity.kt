package dev.elainedb.ytdash_android_codex

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import coil.load
import com.google.android.material.bottomsheet.BottomSheetDialog
import dagger.hilt.android.AndroidEntryPoint
import dev.elainedb.ytdash_android_codex.core.error.Result
import dev.elainedb.ytdash_android_codex.domain.repository.YouTubeRepository
import dev.elainedb.ytdash_android_codex.domain.model.Video
import dev.elainedb.ytdash_android_codex.util.DateFormatter
import dev.elainedb.ytdash_android_codex.util.VideoIntentHelper
import javax.inject.Inject
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@AndroidEntryPoint
class MapActivity : AppCompatActivity() {
    @Inject lateinit var repository: YouTubeRepository

    private lateinit var mapView: MapView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().userAgentValue = "dev.elainedb.ytdash_android_codex/1.0"
        Configuration.getInstance().load(applicationContext, getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
        setContentView(R.layout.activity_map)
        mapView = findViewById(R.id.mapView)
        mapView.setMultiTouchControls(true)

        lifecycleScope.launch {
            when (val result = repository.getVideosWithLocation()) {
                is Result.Success -> renderMarkers(result.data)
                is Result.Error -> Unit
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
        if (videos.isEmpty()) return
        val points = mutableListOf<GeoPoint>()
        videos.forEach { video ->
            val latitude = video.locationLatitude ?: return@forEach
            val longitude = video.locationLongitude ?: return@forEach
            val point = GeoPoint(latitude, longitude)
            points += point
            val marker = Marker(mapView).apply {
                position = point
                title = video.title
                subDescription = video.channelName
                setOnMarkerClickListener { _, _ ->
                    showBottomSheet(video)
                    true
                }
            }
            mapView.overlays.add(marker)
        }

        if (points.isNotEmpty()) {
            val boundingBox = BoundingBox.fromGeoPoints(points)
            mapView.zoomController.setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.ALWAYS)
            mapView.post {
                mapView.zoomToBoundingBox(boundingBox, true, 64)
            }
        }
    }

    private fun showBottomSheet(video: Video) {
        val view = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_video, null)
        val dialog = BottomSheetDialog(this)
        dialog.setContentView(view)

        view.findViewById<ImageView>(R.id.videoThumbnail).load(video.thumbnailUrl)
        view.findViewById<TextView>(R.id.videoTitle).text = video.title
        view.findViewById<TextView>(R.id.videoChannel).text = video.channelName
        view.findViewById<TextView>(R.id.videoPublished).text =
            getString(R.string.published_format, DateFormatter.toDisplayDate(video.publishedAt))
        view.findViewById<TextView>(R.id.videoTags).text = video.tags.joinToString(", ").ifBlank { "No tags" }
        view.findViewById<TextView>(R.id.videoLocation).text = listOfNotNull(
            video.locationCity,
            video.locationCountry,
            video.locationLatitude?.let { lat ->
                video.locationLongitude?.let { lon -> "$lat, $lon" }
            }
        ).joinToString(" | ")
        view.findViewById<TextView>(R.id.videoRecordingDate).text =
            video.recordingDate?.let { getString(R.string.recorded_format, DateFormatter.toDisplayDate(it)) }
                ?: getString(R.string.recording_date_unknown)

        view.setOnClickListener {
            dialog.dismiss()
            VideoIntentHelper.openVideo(this, video.id)
        }
        dialog.show()
    }

    companion object {
        fun newIntent(context: Context): Intent = Intent(context, MapActivity::class.java)
    }
}
