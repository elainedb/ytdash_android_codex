package dev.elainedb.ytdash_android_codex.util

import android.content.Context
import android.location.Geocoder
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import kotlin.math.round

@Singleton
class LocationUtils @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient
) {
    private val cache = ConcurrentHashMap<Pair<Double, Double>, Pair<String?, String?>>()
    private val geocodeSemaphore = Semaphore(5)
    @Volatile
    private var lastNominatimCallTimeMillis: Long = 0L

    suspend fun resolveLocation(
        latitude: Double?,
        longitude: Double?,
        locationDescription: String? = null
    ): Pair<String?, String?> {
        if (latitude == null || longitude == null) {
            return parseLocationDescription(locationDescription)
        }

        val key = roundedKey(latitude, longitude)
        cache[key]?.let { return it }

        return geocodeSemaphore.withPermit {
            cache[key] ?: reverseGeocodeWithFallback(latitude, longitude, locationDescription).also {
                cache[key] = it
            }
        }
    }

    private suspend fun reverseGeocodeWithFallback(
        latitude: Double,
        longitude: Double,
        locationDescription: String?
    ): Pair<String?, String?> {
        repeat(3) { attempt ->
            val geocoderResult = runCatching { reverseWithGeocoder(latitude, longitude) }.getOrNull()
            if (geocoderResult != null && (geocoderResult.first != null || geocoderResult.second != null)) {
                return geocoderResult
            }
            delay(listOf(500L, 1000L, 2000L)[attempt])
        }

        val nominatimResult = runCatching { reverseWithNominatim(latitude, longitude) }.getOrNull()
        if (nominatimResult != null && (nominatimResult.first != null || nominatimResult.second != null)) {
            return nominatimResult
        }

        return parseLocationDescription(locationDescription)
    }

    private suspend fun reverseWithGeocoder(latitude: Double, longitude: Double): Pair<String?, String?> =
        withContext(Dispatchers.IO) {
            if (!Geocoder.isPresent()) return@withContext Pair(null, null)
            val geocoder = Geocoder(context, Locale.getDefault())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                kotlinx.coroutines.suspendCancellableCoroutine { continuation ->
                    geocoder.getFromLocation(latitude, longitude, 1) { addresses ->
                        val address = addresses.firstOrNull()
                        continuation.resume(address.toLocationPair(), onCancellation = null)
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                val address = geocoder.getFromLocation(latitude, longitude, 1)?.firstOrNull()
                address.toLocationPair()
            }
        }

    private suspend fun reverseWithNominatim(latitude: Double, longitude: Double): Pair<String?, String?> =
        withContext(Dispatchers.IO) {
            val waitTime = 1000L - (System.currentTimeMillis() - lastNominatimCallTimeMillis)
            if (waitTime > 0) delay(waitTime)
            lastNominatimCallTimeMillis = System.currentTimeMillis()

            val request = Request.Builder()
                .url("https://nominatim.openstreetmap.org/reverse?format=jsonv2&lat=$latitude&lon=$longitude")
                .header("User-Agent", "dev.elainedb.ytdash_android_codex/1.0")
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext Pair(null, null)
                val body = response.body?.string().orEmpty()
                val json = JSONObject(body)
                val address = json.optJSONObject("address")
                val city = address?.optString("city")
                    .takeUnless { it.isNullOrBlank() }
                    ?: address?.optString("town").takeUnless { it.isNullOrBlank() }
                    ?: address?.optString("state").takeUnless { it.isNullOrBlank() }
                val country = address?.optString("country").takeUnless { it.isNullOrBlank() }
                Pair(city, country)
            }
        }

    private fun parseLocationDescription(locationDescription: String?): Pair<String?, String?> {
        if (locationDescription.isNullOrBlank()) return Pair(null, null)
        val parts = locationDescription.split(",").map { it.trim() }.filter { it.isNotBlank() }
        return when {
            parts.size >= 2 -> Pair(parts[0], parts.last())
            parts.size == 1 -> Pair(parts[0], null)
            else -> Pair(null, null)
        }
    }

    private fun android.location.Address?.toLocationPair(): Pair<String?, String?> {
        if (this == null) return Pair(null, null)
        val city = locality
            ?: subAdminArea
            ?: adminArea
            ?: subLocality
            ?: thoroughfare
        return Pair(city, countryName)
    }

    private fun roundedKey(latitude: Double, longitude: Double): Pair<Double, Double> {
        return Pair(round(latitude * 1000.0) / 1000.0, round(longitude * 1000.0) / 1000.0)
    }
}
