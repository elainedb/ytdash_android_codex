package dev.elainedb.ytdash_android_codex.utils

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.security.MessageDigest

object SigningUtils {
    fun getSha1Signature(context: Context): String {
        return runCatching {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNING_CERTIFICATES
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNATURES
                )
            }

            val signatureBytes = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.signingInfo?.apkContentsSigners?.firstOrNull()?.toByteArray()
            } else {
                @Suppress("DEPRECATION")
                packageInfo.signatures?.firstOrNull()?.toByteArray()
            } ?: return ""

            MessageDigest.getInstance("SHA-1")
                .digest(signatureBytes)
                .joinToString("") { "%02X".format(it) }
        }.getOrDefault("")
    }
}
