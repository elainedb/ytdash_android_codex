package dev.elainedb.ytdash_android_codex

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dev.elainedb.ytdash_android_codex.ui.MapScreen
import dev.elainedb.ytdash_android_codex.ui.theme.YTDashACodexTheme

class MapActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            YTDashACodexTheme {
                MapScreen(
                    repository = ServiceLocator.repository,
                    onBack = ::finish
                )
            }
        }
    }

    companion object {
        fun newIntent(context: Context): Intent = Intent(context, MapActivity::class.java)
    }
}
