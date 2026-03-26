package dev.elainedb.ytdash_android_codex

import android.app.Application
import org.osmdroid.config.Configuration

class YTDashApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Configuration.getInstance().load(this, getSharedPreferences("osmdroid", MODE_PRIVATE))
        ServiceLocator.initialize(this)
    }
}
