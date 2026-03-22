package dev.elainedb.ytdash_android_codex

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import dev.elainedb.ytdash_android_codex.config.ConfigHelper
import dev.elainedb.ytdash_android_codex.database.VideoDatabase
import dev.elainedb.ytdash_android_codex.network.NetworkModule
import dev.elainedb.ytdash_android_codex.repository.YouTubeRepository
import dev.elainedb.ytdash_android_codex.ui.VideoListScreen
import dev.elainedb.ytdash_android_codex.ui.theme.YTDashACodexTheme
import dev.elainedb.ytdash_android_codex.viewmodel.VideoListViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: VideoListViewModel by viewModels {
        VideoListViewModel.Factory(
            YouTubeRepository(
                context = applicationContext,
                videoDao = VideoDatabase.getInstance(applicationContext).videoDao(),
                apiService = NetworkModule.createYouTubeApiService(applicationContext),
                config = ConfigHelper.loadConfig(applicationContext),
            )
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val uiState by viewModel.uiState.collectAsState()
            val filterOptions by viewModel.filterOptions.collectAsState()
            val sortOption by viewModel.sortOption.collectAsState()
            val availableCountries by viewModel.availableCountries.collectAsState()
            val availableChannels by viewModel.availableChannels.collectAsState()

            YTDashACodexTheme {
                VideoListScreen(
                    uiState = uiState,
                    filterOptions = filterOptions,
                    sortOption = sortOption,
                    availableCountries = availableCountries,
                    availableChannels = availableChannels,
                    onLogout = { logout() },
                    onRefresh = viewModel::refreshVideos,
                    onApplyFilter = viewModel::applyFilter,
                    onApplySort = viewModel::applySorting,
                )
            }
        }
    }

    private fun logout() {
        GoogleSignIn.getClient(
            this,
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestEmail().build()
        ).signOut().addOnCompleteListener {
            startActivity(
                Intent(this, LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
            )
        }
    }
}
