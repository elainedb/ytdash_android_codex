package dev.elainedb.ytdash_android_codex.data.location

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@Singleton
class LocationUtils @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient
) {
    private val cache = ConcurrentHashMap<Pair<Double, Double>, Pair<String?, String?>>()
    private val semaphore = Semaphore(5)
    private val lastNominatimRequestAt = AtomicLong(0L)

    suspend fun resolveLocation(
        latitude: Double?,
        longitude: Double?,
        locationDescription: String?
    ): Pair<String?, String?> {
        if (latitude == null || longitude == null) {
            return parseDescriptionFallback(locationDescription)
        }

        val cacheKey = roundedKey(latitude, longitude)
        cache[cacheKey]?.let { return it }

        return semaphore.withPermit {
            cache[cacheKey] ?: resolveWithRetry(latitude, longitude, locationDescription).also {
                cache[cacheKey] = it
            }
        }
    }

    private suspend fun resolveWithRetry(
        latitude: Double,
        longitude: Double,
        locationDescription: String?
    ): Pair<String?, String?> {
        val delays = listOf(0L, 500L, 1_000L, 2_000L)
        delays.forEachIndexed { index, backoff ->
            if (backoff > 0) delay(backoff)
            val geocoderResult = runCatching {
                geocodeWithAndroid(latitude, longitude)
            }.getOrNull()
            if (geocoderResult?.first != null || geocoderResult?.second != null) {
                return geocoderResult
            }
            if (index == delays.lastIndex) {
                val nominatim = reverseWithNominatim(latitude, longitude)
                if (nominatim.first != null || nominatim.second != null) {
                    return nominatim
                }
            }
        }

        return parseDescriptionFallback(locationDescription)
    }

    private suspend fun geocodeWithAndroid(latitude: Double, longitude: Double): Pair<String?, String?> {
        if (!Geocoder.isPresent()) return null to null
        val geocoder = Geocoder(context, Locale.getDefault())
        val address = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            suspendCoroutine<Address?> { continuation ->
                geocoder.getFromLocation(latitude, longitude, 1) { addresses ->
                    continuation.resume(addresses.firstOrNull())
                }
            }
        } else {
            @Suppress("DEPRECATION")
            withContext(Dispatchers.IO) {
                geocoder.getFromLocation(latitude, longitude, 1)?.firstOrNull()
            }
        }
        return address?.resolveCity() to address?.countryName
    }

    private suspend fun reverseWithNominatim(latitude: Double, longitude: Double): Pair<String?, String?> {
        val elapsed = System.currentTimeMillis() - lastNominatimRequestAt.get()
        if (elapsed < 1_000L) {
            delay(1_000L - elapsed)
        }

        return withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url("https://nominatim.openstreetmap.org/reverse?format=jsonv2&lat=$latitude&lon=$longitude")
                .header("User-Agent", "dev.elainedb.ytdash_android_codex/1.0")
                .build()

            runCatching {
                okHttpClient.newCall(request).execute().use { response ->
                    lastNominatimRequestAt.set(System.currentTimeMillis())
                    if (!response.isSuccessful) return@use null to null
                    val body = response.body?.string().orEmpty()
                    val json = JSONObject(body).optJSONObject("address")
                    val city = json?.optString("city")
                        ?.ifBlank { null }
                        ?: json?.optString("town")?.ifBlank { null }
                        ?: json?.optString("county")?.ifBlank { null }
                    val country = json?.optString("country")?.ifBlank { null }
                    city to country
                }
            }.getOrDefault(null to null)
        }
    }

    private fun parseDescriptionFallback(description: String?): Pair<String?, String?> {
        if (description.isNullOrBlank()) return null to null
        val pattern = Regex("""([A-Za-z .'-]+),\s*([A-Za-z .'-]+)""")
        val match = pattern.find(description) ?: return null to null
        return match.groupValues[1].trim() to match.groupValues[2].trim()
    }

    private fun roundedKey(latitude: Double, longitude: Double): Pair<Double, Double> {
        fun round(value: Double): Double = String.format(Locale.US, "%.3f", value).toDouble()
        return round(latitude) to round(longitude)
    }

    private fun Address.resolveCity(): String? {
        return locality
            ?: subAdminArea
            ?: adminArea
            ?: subLocality
            ?: thoroughfare
    }
}
