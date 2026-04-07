package dev.elainedb.ytdash_android_codex.location

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.elainedb.ytdash_android_codex.BuildConfig
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
    @Volatile
    private var lastNominatimRequestAt = 0L

    suspend fun resolveLocation(
        latitude: Double?,
        longitude: Double?,
        locationDescription: String? = null
    ): Pair<String?, String?> {
        if (latitude == null || longitude == null) {
            return parseLocationDescription(locationDescription) ?: (null to null)
        }

        val key = roundedKey(latitude, longitude)
        cache[key]?.let { return it }

        return semaphore.withPermit {
            cache[key]?.let { return@withPermit it }
            val resolved = geocodeWithRetry(latitude, longitude)
                ?: reverseGeocodeWithNominatim(latitude, longitude)
                ?: parseLocationDescription(locationDescription)
                ?: (null to null)
            cache[key] = resolved
            resolved
        }
    }

    private suspend fun geocodeWithRetry(latitude: Double, longitude: Double): Pair<String?, String?>? {
        val delays = listOf(500L, 1_000L, 2_000L)
        repeat(delays.size) { index ->
            val result = runCatching { geocodeWithAndroid(latitude, longitude) }.getOrNull()
            if (result != null) return result
            delay(delays[index])
        }
        return runCatching { geocodeWithAndroid(latitude, longitude) }.getOrNull()
    }

    private suspend fun geocodeWithAndroid(latitude: Double, longitude: Double): Pair<String?, String?>? {
        if (!Geocoder.isPresent()) return null
        val geocoder = Geocoder(context, Locale.getDefault())
        return withContext(Dispatchers.IO) {
            val address = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                suspendCoroutine<Address?> { continuation ->
                    geocoder.getFromLocation(latitude, longitude, 1) { addresses ->
                        continuation.resume(addresses.firstOrNull())
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                geocoder.getFromLocation(latitude, longitude, 1)?.firstOrNull()
            }
            address?.toCityCountry()
        }
    }

    private suspend fun reverseGeocodeWithNominatim(latitude: Double, longitude: Double): Pair<String?, String?>? {
        val sinceLast = System.currentTimeMillis() - lastNominatimRequestAt
        if (sinceLast < 1_000L) delay(1_000L - sinceLast)

        return withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url("https://nominatim.openstreetmap.org/reverse?format=jsonv2&lat=$latitude&lon=$longitude")
                .header("User-Agent", "${BuildConfig.APPLICATION_ID}/1.0")
                .build()
            lastNominatimRequestAt = System.currentTimeMillis()
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body?.string().orEmpty()
                if (body.isBlank()) return@withContext null
                val address = JSONObject(body).optJSONObject("address") ?: return@withContext null
                val city = listOf("city", "town", "village", "county", "state")
                    .firstNotNullOfOrNull { key -> address.optString(key).takeIf(String::isNotBlank) }
                val country = address.optString("country").takeIf(String::isNotBlank)
                city to country
            }
        }
    }

    private fun parseLocationDescription(locationDescription: String?): Pair<String?, String?>? {
        val value = locationDescription?.trim().orEmpty()
        if (value.isBlank()) return null
        val match = Regex("""^\s*([^,]+)\s*,\s*([^,]+)\s*$""").find(value) ?: return null
        return match.groupValues[1].trim() to match.groupValues[2].trim()
    }

    private fun roundedKey(latitude: Double, longitude: Double): Pair<Double, Double> {
        fun roundThree(value: Double) = kotlin.math.round(value * 1_000.0) / 1_000.0
        return roundThree(latitude) to roundThree(longitude)
    }

    private fun Address.toCityCountry(): Pair<String?, String?> {
        val city = locality
            ?: subAdminArea
            ?: adminArea
            ?: subLocality
            ?: thoroughfare
        return city to countryName
    }
}
