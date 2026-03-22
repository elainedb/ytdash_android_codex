package dev.elainedb.ytdash_android_codex

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import dev.elainedb.ytdash_android_codex.database.VideoDatabase
import dev.elainedb.ytdash_android_codex.model.Video
import dev.elainedb.ytdash_android_codex.repository.YouTubeRepository
import dev.elainedb.ytdash_android_codex.ui.theme.YTDashACodexTheme
import dev.elainedb.ytdash_android_codex.utils.DateUtils
import dev.elainedb.ytdash_android_codex.utils.openVideo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class MapActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().load(applicationContext, getPreferences(MODE_PRIVATE))

        val repository = YouTubeRepository(
            context = applicationContext,
            videoDao = VideoDatabase.getInstance(applicationContext).videoDao(),
        )

        setContent {
            YTDashACodexTheme {
                MapScreen(repository = repository)
            }
        }
    }

    companion object {
        fun newIntent(context: Context): Intent = Intent(context, MapActivity::class.java)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MapScreen(repository: YouTubeRepository) {
    val context = LocalContext.current
    var selectedVideo by remember { mutableStateOf<Video?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    AndroidView(
        factory = { androidContext ->
            MapView(androidContext).apply {
                layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                setMultiTouchControls(true)
                controller.setZoom(3.5)

                CoroutineScope(Dispatchers.Main).launch {
                    val videos = repository.getVideosWithLocation()
                    val points = mutableListOf<GeoPoint>()
                    videos.forEach { video ->
                        val lat = video.locationLatitude ?: return@forEach
                        val lng = video.locationLongitude ?: return@forEach
                        val point = GeoPoint(lat, lng)
                        points += point
                        overlays += Marker(this@apply).apply {
                            position = point
                            title = video.title
                            subDescription = video.channelName
                            setOnMarkerClickListener { _, _ ->
                                selectedVideo = video
                                true
                            }
                        }
                    }

                    if (points.isNotEmpty()) {
                        zoomToBoundingBox(BoundingBox.fromGeoPointsSafe(points), true, 96)
                    }
                }
            }
        }
    )

    selectedVideo?.let { video ->
        ModalBottomSheet(
            onDismissRequest = { selectedVideo = null },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { openVideo(context, video.id) }
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AsyncImage(
                    model = video.thumbnailUrl,
                    contentDescription = video.title,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(video.title, style = MaterialTheme.typography.titleMedium)
                Text(video.channelName)
                Text(DateUtils.formatIsoDate(video.publishedAt))
                if (video.tags.isNotEmpty()) {
                    Text(video.tags.joinToString(", "))
                }
                Text(
                    listOfNotNull(video.locationCity, video.locationCountry).joinToString(", ")
                        .ifBlank { "${video.locationLatitude}, ${video.locationLongitude}" }
                )
                if (!video.recordingDate.isNullOrBlank()) {
                    Text("Recorded ${DateUtils.formatIsoDate(video.recordingDate)}")
                }
            }
        }
    }
}
