package dev.elainedb.ytdash_android_codex

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import dev.elainedb.ytdash_android_codex.core.utils.IntentUtils
import dev.elainedb.ytdash_android_codex.presentation.videolist.VideoListViewModel
import dev.elainedb.ytdash_android_codex.ui.VideoListScreen
import dev.elainedb.ytdash_android_codex.ui.theme.YTDashACodexTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: VideoListViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            YTDashACodexTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                val filterOptions by viewModel.filterOptions.collectAsStateWithLifecycle()
                val sortOption by viewModel.sortOption.collectAsStateWithLifecycle()
                val availableCountries by viewModel.availableCountries.collectAsStateWithLifecycle()
                val availableChannels by viewModel.availableChannels.collectAsStateWithLifecycle()

                VideoListScreen(
                    uiState = uiState,
                    filterOptions = filterOptions,
                    sortOption = sortOption,
                    availableCountries = availableCountries,
                    availableChannels = availableChannels,
                    onRefresh = { viewModel.refreshVideos(true) },
                    onOpenMap = { startActivity(MapActivity.newIntent(this)) },
                    onApplyFilter = viewModel::applyFilter,
                    onSortChange = viewModel::applySorting,
                    onClearFilters = viewModel::clearFilters,
                    onOpenVideo = { IntentUtils.openYoutubeVideo(this, it) },
                    onLogout = {
                        viewModel.signOut { success ->
                            if (success) {
                                startActivity(
                                    Intent(this, LoginActivity::class.java).apply {
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    }
                                )
                            }
                        }
                    }
                )
            }
        }
    }
}
