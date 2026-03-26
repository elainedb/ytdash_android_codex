package dev.elainedb.ytdash_android_codex.utils

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.IOException
import java.util.Locale
import kotlin.coroutines.resume

object LocationUtils {
    suspend fun reverseGeocode(
        context: Context,
        latitude: Double,
        longitude: Double
    ): Pair<String?, String?> {
        if (!Geocoder.isPresent()) {
            return null to null
        }

        val geocoder = Geocoder(context, Locale.getDefault())
        return runCatching {
            val address = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                geocoder.getFromLocationAsync(latitude, longitude)
            } else {
                @Suppress("DEPRECATION")
                geocoder.getFromLocation(latitude, longitude, 1)?.firstOrNull()
            }
            address?.toLocationParts() ?: (null to null)
        }.getOrElse { null to null }
    }

    private suspend fun Geocoder.getFromLocationAsync(
        latitude: Double,
        longitude: Double
    ): Address? = suspendCancellableCoroutine { continuation ->
        getFromLocation(latitude, longitude, 1) { addresses ->
            continuation.resume(addresses.firstOrNull())
        }
    }

    private fun Address.toLocationParts(): Pair<String?, String?> {
        val city = locality
            ?: subAdminArea
            ?: adminArea
            ?: subLocality
            ?: thoroughfare
        return city to countryName
    }
}
