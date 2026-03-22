package dev.elainedb.ytdash_android_codex.utils

import android.content.Context
import android.location.Geocoder
import android.os.Build
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import kotlin.coroutines.resume

object LocationUtils {
    suspend fun getCityAndCountry(
        context: Context,
        latitude: Double,
        longitude: Double,
    ): Pair<String?, String?> {
        if (!Geocoder.isPresent()) return null to null

        val geocoder = Geocoder(context, Locale.getDefault())
        val address = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                suspendCancellableCoroutine { continuation ->
                    geocoder.getFromLocation(latitude, longitude, 1) { addresses ->
                        continuation.resume(addresses.firstOrNull())
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                geocoder.getFromLocation(latitude, longitude, 1)?.firstOrNull()
            }
        }.getOrNull() ?: return null to null

        val city = address.locality
            ?: address.subAdminArea
            ?: address.adminArea
            ?: address.subLocality
            ?: address.thoroughfare

        return city to address.countryName
    }
}
