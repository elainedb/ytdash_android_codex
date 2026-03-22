package dev.elainedb.ytdash_android_codex

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import coil.load
import com.google.android.material.bottomsheet.BottomSheetDialog
import dev.elainedb.ytdash_android_codex.config.ConfigHelper
import dev.elainedb.ytdash_android_codex.database.VideoDatabase
import dev.elainedb.ytdash_android_codex.network.NetworkModule
import dev.elainedb.ytdash_android_codex.repository.YouTubeRepository
import dev.elainedb.ytdash_android_codex.utils.DateUtils
import dev.elainedb.ytdash_android_codex.utils.VideoIntentHelper
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class MapActivity : ComponentActivity() {
    private lateinit var mapView: MapView
    private lateinit var repository: YouTubeRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Configuration.getInstance().userAgentValue = packageName
        mapView = MapView(this).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(3.0)
        }
        setContentView(mapView)

        repository = YouTubeRepository(
            context = applicationContext,
            videoDao = VideoDatabase.getInstance(applicationContext).videoDao(),
            apiService = NetworkModule.createYouTubeApiService(applicationContext),
            config = ConfigHelper.loadConfig(applicationContext),
        )

        lifecycleScope.launch {
            val videos = repository.getVideosWithLocation()
            if (videos.isEmpty()) {
                setTitle(R.string.map_no_locations)
                return@launch
            }

            val geoPoints = mutableListOf<GeoPoint>()
            videos.forEach { video ->
                val latitude = video.locationLatitude ?: return@forEach
                val longitude = video.locationLongitude ?: return@forEach
                val point = GeoPoint(latitude, longitude)
                geoPoints += point
                mapView.overlays += Marker(mapView).apply {
                    position = point
                    title = video.title
                    setOnMarkerClickListener { _, _ ->
                        showBottomSheet(video)
                        true
                    }
                }
            }

            if (geoPoints.isNotEmpty()) {
                val box = BoundingBox.fromGeoPoints(geoPoints)
                mapView.post {
                    mapView.zoomToBoundingBox(box, true, 96)
                }
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

    override fun onDestroy() {
        mapView.onDetach()
        super.onDestroy()
    }

    private fun showBottomSheet(video: dev.elainedb.ytdash_android_codex.model.Video) {
        val dialog = BottomSheetDialog(this)
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 24, 32, 32)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        val imageView = android.widget.ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                320
            )
            scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
            load(video.thumbnailUrl)
        }
        val titleView = TextView(this).apply { text = video.title }
        val detailsView = TextView(this).apply {
            val location = listOfNotNull(video.locationCity, video.locationCountry).joinToString(", ")
            text = buildString {
                appendLine(video.channelName)
                appendLine(DateUtils.toDisplayDate(video.publishedAt).orEmpty())
                if (video.tags.isNotEmpty()) appendLine(video.tags.joinToString(", "))
                if (location.isNotBlank()) appendLine(location)
                if (video.locationLatitude != null && video.locationLongitude != null) {
                    appendLine("${video.locationLatitude}, ${video.locationLongitude}")
                }
                DateUtils.toDisplayDate(video.recordingDate)?.let { appendLine(it) }
            }
        }

        container.addView(imageView)
        container.addView(titleView)
        container.addView(detailsView)
        container.setOnClickListener {
            dialog.dismiss()
            VideoIntentHelper.openVideo(this, video.id)
        }

        dialog.setContentView(container)
        dialog.behavior.peekHeight = (resources.displayMetrics.heightPixels * 0.25f).toInt()
        dialog.show()
    }

    companion object {
        fun newIntent(context: Context): Intent = Intent(context, MapActivity::class.java)
    }
}
