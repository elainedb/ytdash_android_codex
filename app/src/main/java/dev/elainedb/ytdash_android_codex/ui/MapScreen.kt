package dev.elainedb.ytdash_android_codex.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import dev.elainedb.ytdash_android_codex.R
import dev.elainedb.ytdash_android_codex.model.Video
import dev.elainedb.ytdash_android_codex.repository.YouTubeRepository
import dev.elainedb.ytdash_android_codex.utils.VideoIntentHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    repository: YouTubeRepository,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var videos by remember { mutableStateOf<List<Video>>(emptyList()) }
    var selectedVideo by remember { mutableStateOf<Video?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    val mapView = remember(context) {
        MapView(context).apply {
            setMultiTouchControls(true)
            controller.setZoom(4.0)
        }
    }

    LaunchedEffect(Unit) {
        videos = withContext(Dispatchers.IO) { repository.getVideosWithLocation() }
        isLoading = false
    }

    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDetach()
        }
    }

    LaunchedEffect(videos, mapView) {
        if (videos.isEmpty()) return@LaunchedEffect

        val markerModels = withContext(Dispatchers.Default) {
            videos.mapNotNull { video ->
                val lat = video.locationLatitude ?: return@mapNotNull null
                val lon = video.locationLongitude ?: return@mapNotNull null
                video to GeoPoint(lat, lon)
            }
        }

        mapView.overlays.clear()
        val points = ArrayList<GeoPoint>(markerModels.size)
        markerModels.forEach { (video, point) ->
            points += point
            mapView.overlays += Marker(mapView).apply {
                position = point
                title = video.title
                setOnMarkerClickListener { _, _ ->
                    selectedVideo = video
                    true
                }
            }
        }

        mapView.post {
            if (points.isNotEmpty()) {
                mapView.zoomToBoundingBox(BoundingBox.fromGeoPointsSafe(points), true, 120)
            }
            mapView.invalidate()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = androidx.compose.ui.res.stringResource(R.string.map_title)) },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text(text = androidx.compose.ui.res.stringResource(R.string.back)) }
                }
            )
        }
    ) { innerPadding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (videos.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp)
            ) {
                Text(text = androidx.compose.ui.res.stringResource(R.string.map_empty))
            }
        } else {
            AndroidView(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                factory = { mapView }
            )
        }
    }

    if (selectedVideo != null) {
        ModalBottomSheet(onDismissRequest = { selectedVideo = null }) {
            selectedVideo?.let { video ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp)
                        .clickable { VideoIntentHelper.openVideo(context, video.id) }
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AsyncImage(
                        model = video.thumbnailUrl,
                        contentDescription = video.title,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                    )
                    Text(video.title, style = MaterialTheme.typography.titleLarge)
                    Text(video.channelName, style = MaterialTheme.typography.bodyMedium)
                    Text("${context.getString(R.string.publication_date)}: ${video.publishedAt.take(10)}")
                    formatLocation(video)?.let {
                        Text("${context.getString(R.string.location)}: $it")
                    }
                    video.recordingDate?.take(10)?.let {
                        Text("${context.getString(R.string.recording_date)}: $it")
                    }
                    if (video.tags.isNotEmpty()) {
                        Text("${context.getString(R.string.tags)}: ${video.tags.joinToString(", ")}")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}
