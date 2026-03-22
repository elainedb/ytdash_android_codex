package dev.elainedb.ytdash_android_codex

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import dev.elainedb.ytdash_android_codex.database.VideoDatabase
import dev.elainedb.ytdash_android_codex.repository.YouTubeRepository
import dev.elainedb.ytdash_android_codex.ui.screens.VideoListRoute
import dev.elainedb.ytdash_android_codex.ui.theme.YTDashACodexTheme
import dev.elainedb.ytdash_android_codex.viewmodel.VideoListViewModel
import dev.elainedb.ytdash_android_codex.viewmodel.VideoListViewModelFactory

class MainActivity : ComponentActivity() {
    private val viewModel: VideoListViewModel by viewModels {
        VideoListViewModelFactory(
            YouTubeRepository(
                context = applicationContext,
                videoDao = VideoDatabase.getInstance(applicationContext).videoDao(),
            )
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            YTDashACodexTheme {
                VideoListRoute(
                    viewModel = viewModel,
                    onOpenMap = { startActivity(MapActivity.newIntent(this)) },
                    onLogout = { logout() }
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
