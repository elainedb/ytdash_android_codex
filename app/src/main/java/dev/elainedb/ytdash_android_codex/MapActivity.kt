package dev.elainedb.ytdash_android_codex

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import coil.load
import com.google.android.material.bottomsheet.BottomSheetBehavior
import dagger.hilt.android.AndroidEntryPoint
import dev.elainedb.ytdash_android_codex.core.utils.DateUtils
import dev.elainedb.ytdash_android_codex.core.utils.IntentUtils
import dev.elainedb.ytdash_android_codex.data.database.VideoDao
import dev.elainedb.ytdash_android_codex.data.database.toVideo
import dev.elainedb.ytdash_android_codex.domain.model.Video
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@AndroidEntryPoint
class MapActivity : AppCompatActivity() {

    @Inject lateinit var videoDao: VideoDao

    private lateinit var mapView: MapView
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<android.view.View>
    private lateinit var thumbnailView: ImageView
    private lateinit var titleView: TextView
    private lateinit var channelView: TextView
    private lateinit var metaView: TextView
    private var selectedVideo: Video? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().userAgentValue = "dev.elainedb.ytdash_android_codex/1.0"
        setContentView(R.layout.activity_map)

        mapView = findViewById(R.id.mapView)
        val bottomSheet = findViewById<android.view.View>(R.id.bottomSheet)
        thumbnailView = findViewById(R.id.videoThumbnail)
        titleView = findViewById(R.id.videoTitle)
        channelView = findViewById(R.id.videoChannel)
        metaView = findViewById(R.id.videoMeta)
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet).apply {
            state = BottomSheetBehavior.STATE_HIDDEN
            isHideable = true
        }

        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        mapView.controller.setZoom(2.0)

        bottomSheet.setOnClickListener {
            selectedVideo?.let { video -> IntentUtils.openYoutubeVideo(this, video.id) }
        }

        lifecycleScope.launch {
            val videos = videoDao.getVideosWithLocation().first().map { it.toVideo() }
            renderMarkers(videos)
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
            title = getString(R.string.no_location_videos)
            return
        }

        val points = mutableListOf<GeoPoint>()
        videos.forEach { video ->
            val latitude = video.locationLatitude ?: return@forEach
            val longitude = video.locationLongitude ?: return@forEach
            val point = GeoPoint(latitude, longitude)
            points += point
            mapView.overlays += Marker(mapView).apply {
                position = point
                title = video.title
                subDescription = video.channelName
                setOnMarkerClickListener { _, _ ->
                    showVideoSheet(video)
                    true
                }
            }
        }

        if (points.isNotEmpty()) {
            val box = BoundingBox.fromGeoPoints(points)
            mapView.zoomToBoundingBox(box, true, 120)
        }
    }

    private fun showVideoSheet(video: Video) {
        selectedVideo = video
        thumbnailView.load(video.thumbnailUrl)
        titleView.text = video.title
        channelView.text = video.channelName
        val published = DateUtils.formatIsoDateTime(video.publishedAt)?.let { "Published $it" }.orEmpty()
        val recorded = DateUtils.formatIsoDate(video.recordingDate)?.let { "Recorded $it" }.orEmpty()
        val location = buildString {
            val label = listOfNotNull(video.locationCity, video.locationCountry).joinToString(", ")
            if (label.isNotBlank()) append(label)
            if (video.locationLatitude != null && video.locationLongitude != null) {
                if (isNotBlank()) append('\n')
                append("Lat ${"%.4f".format(video.locationLatitude)}, Lng ${"%.4f".format(video.locationLongitude)}")
            }
            if (video.tags.isNotEmpty()) {
                if (isNotBlank()) append('\n')
                append(video.tags.joinToString(", "))
            }
        }
        metaView.text = listOf(published, recorded, location).filter { it.isNotBlank() }.joinToString("\n")
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
    }

    companion object {
        fun newIntent(context: Context): Intent = Intent(context, MapActivity::class.java)
    }
}
