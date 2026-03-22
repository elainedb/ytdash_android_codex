package dev.elainedb.ytdash_android_codex.utils

import android.content.Context
import android.location.Geocoder
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.Locale
import kotlin.coroutines.resume

object LocationUtils {
    suspend fun reverseGeocode(
        context: Context,
        latitude: Double,
        longitude: Double,
    ): Pair<String?, String?> {
        if (!Geocoder.isPresent()) return null to null

        val geocoder = Geocoder(context, Locale.getDefault())
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            suspendCancellableCoroutine { continuation ->
                geocoder.getFromLocation(latitude, longitude, 1) { addresses ->
                    val address = addresses.firstOrNull()
                    continuation.resume(address.toResolvedCity() to address?.countryName)
                }
            }
        } else {
            withContext(Dispatchers.IO) {
                try {
                    @Suppress("DEPRECATION")
                    val address = geocoder.getFromLocation(latitude, longitude, 1)?.firstOrNull()
                    address.toResolvedCity() to address?.countryName
                } catch (_: IOException) {
                    null to null
                }
            }
        }
    }
}

private fun android.location.Address?.toResolvedCity(): String? {
    return this?.locality
        ?: this?.subAdminArea
        ?: this?.adminArea
        ?: this?.subLocality
        ?: this?.thoroughfare
}
