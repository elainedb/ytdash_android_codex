package dev.elainedb.ytdash_android_codex

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import dev.elainedb.ytdash_android_codex.core.error.Result
import dev.elainedb.ytdash_android_codex.domain.repository.AuthRepository
import dev.elainedb.ytdash_android_codex.domain.usecase.SignOut
import dev.elainedb.ytdash_android_codex.presentation.VideoListViewModel
import dev.elainedb.ytdash_android_codex.ui.VideoListScreen
import dev.elainedb.ytdash_android_codex.ui.theme.YTDashACodexTheme
import dev.elainedb.ytdash_android_codex.util.VideoIntentHelper
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var authRepository: AuthRepository
    @Inject lateinit var signOut: SignOut

    private val viewModel: VideoListViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val uiState by viewModel.uiState.collectAsState()
            val filterOptions by viewModel.filterOptions.collectAsState()
            val sortOption by viewModel.sortOption.collectAsState()
            val availableChannels by viewModel.availableChannels.collectAsState()
            val availableCountries by viewModel.availableCountries.collectAsState()
            YTDashACodexTheme {
                VideoListScreen(
                    uiState = uiState,
                    filterOptions = filterOptions,
                    sortOption = sortOption,
                    availableChannels = availableChannels,
                    availableCountries = availableCountries,
                    onRefresh = { viewModel.loadVideos(forceRefresh = true) },
                    onViewMap = { startActivity(MapActivity.newIntent(this)) },
                    onFilterApply = viewModel::applyFilter,
                    onSortApply = viewModel::applySorting,
                    onClearFilters = viewModel::clearFilters,
                    onLogout = ::logout,
                    onOpenVideo = { VideoIntentHelper.openVideo(this, it) }
                )
            }
        }
    }

    private fun logout() {
        lifecycleScope.launch {
            when (signOut(Unit)) {
                is Result.Success, is Result.Error -> {
                    startActivity(
                        Intent(this@MainActivity, LoginActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                    )
                }
            }
        }
    }
}
