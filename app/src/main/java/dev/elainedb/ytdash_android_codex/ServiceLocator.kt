package dev.elainedb.ytdash_android_codex

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import dev.elainedb.ytdash_android_codex.config.ConfigHelper
import dev.elainedb.ytdash_android_codex.database.VideoDatabase
import dev.elainedb.ytdash_android_codex.network.YouTubeApiService
import dev.elainedb.ytdash_android_codex.repository.YouTubeRepository
import dev.elainedb.ytdash_android_codex.utils.SigningUtils
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.create
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory

object ServiceLocator {
    private lateinit var appContext: Context

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    private val configHelper: ConfigHelper by lazy {
        ConfigHelper(appContext)
    }

    private val apiService: YouTubeApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://www.googleapis.com/youtube/v3/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create()
    }

    private val okHttpClient: OkHttpClient by lazy {
        val packageName = appContext.packageName
        val packageSignature = SigningUtils.getSha1Signature(appContext)
        val logger = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("X-Android-Package", packageName)
                    .header("X-Android-Cert", packageSignature)
                    .build()
                chain.proceed(request)
            }
            .addInterceptor(logger)
            .build()
    }

    private val database by lazy {
        VideoDatabase.getDatabase(appContext)
    }

    val repository: YouTubeRepository by lazy {
        YouTubeRepository(
            context = appContext,
            apiService = apiService,
            videoDao = database.videoDao(),
            configHelper = configHelper
        )
    }

    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    fun googleSignInClient(context: Context): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()
        return GoogleSignIn.getClient(context, gso)
    }

    fun configHelper(): ConfigHelper = configHelper
}
