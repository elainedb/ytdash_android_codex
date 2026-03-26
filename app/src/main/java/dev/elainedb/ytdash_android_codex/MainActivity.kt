package dev.elainedb.ytdash_android_codex

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import dev.elainedb.ytdash_android_codex.ui.VideoListScreen
import dev.elainedb.ytdash_android_codex.ui.theme.YTDashACodexTheme
import dev.elainedb.ytdash_android_codex.utils.VideoIntentHelper
import dev.elainedb.ytdash_android_codex.viewmodel.VideoListViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<VideoListViewModel> {
        VideoListViewModel.Factory(ServiceLocator.repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (GoogleSignIn.getLastSignedInAccount(this)?.email.isNullOrBlank()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        enableEdgeToEdge()
        setContent {
            YTDashACodexTheme {
                val uiState by viewModel.uiState.collectAsState()
                val countries by viewModel.availableCountries.collectAsState()
                val channels by viewModel.availableChannels.collectAsState()
                val filterOptions by viewModel.filterOptions.collectAsState()
                val sortOption by viewModel.sortOption.collectAsState()

                VideoListScreen(
                    uiState = uiState,
                    filterOptions = filterOptions,
                    sortOption = sortOption,
                    availableCountries = countries,
                    availableChannels = channels,
                    onRefresh = viewModel::refreshVideos,
                    onApplyFilter = viewModel::applyFilter,
                    onClearFilters = viewModel::clearFilters,
                    onApplySorting = viewModel::applySorting,
                    onOpenMap = { startActivity(MapActivity.newIntent(this)) },
                    onLogout = ::logout,
                    onVideoClick = { video -> VideoIntentHelper.openVideo(this, video.id) }
                )
            }
        }
    }

    private fun logout() {
        lifecycleScope.launch {
            ServiceLocator.googleSignInClient(this@MainActivity).signOut()
                .addOnCompleteListener {
                    startActivity(
                        Intent(this@MainActivity, LoginActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                    )
                    finish()
                }
        }
    }
}
