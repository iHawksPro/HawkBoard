package com.paletteboard.updater

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.paletteboard.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant

class AppUpdateManager(
    private val context: Context,
    private val json: Json,
) {
    suspend fun fetchLatestRelease(): AppUpdateInfo? = withContext(Dispatchers.IO) {
        val connection = openConnection(LATEST_RELEASE_URL)
        try {
            when (connection.responseCode) {
                HttpURLConnection.HTTP_OK -> parseRelease(connection.inputStream.bufferedReader().use { it.readText() })
                else -> throw IOException("Update check failed with HTTP ${connection.responseCode}.")
            }
        } finally {
            connection.disconnect()
        }
    }

    fun isUpdateAvailable(release: AppUpdateInfo): Boolean {
        return release.assetUpdatedAtEpochMs > BuildConfig.BUILD_TIME_UTC
    }

    suspend fun downloadUpdate(
        release: AppUpdateInfo,
        onProgress: (Float?) -> Unit,
    ): File = withContext(Dispatchers.IO) {
        val connection = openConnection(release.downloadUrl)
        try {
            when (connection.responseCode) {
                HttpURLConnection.HTTP_OK -> {
                    val updatesDir = File(context.cacheDir, "apk-updates").apply { mkdirs() }
                    val targetFile = File(updatesDir, RELEASE_ASSET_NAME)
                    val totalBytes = connection.contentLengthLong.takeIf { it > 0 } ?: release.assetSizeBytes.takeIf { it > 0 }
                    connection.inputStream.use { input ->
                        targetFile.outputStream().use { output ->
                            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                            var downloadedBytes = 0L
                            while (true) {
                                val read = input.read(buffer)
                                if (read == -1) break
                                output.write(buffer, 0, read)
                                downloadedBytes += read
                                if (totalBytes != null) {
                                    onProgress((downloadedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f))
                                } else {
                                    onProgress(null)
                                }
                            }
                            output.flush()
                        }
                    }
                    targetFile
                }

                else -> throw IOException("Update download failed with HTTP ${connection.responseCode}.")
            }
        } finally {
            connection.disconnect()
        }
    }

    fun canRequestPackageInstalls(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    fun createInstallPermissionIntent(): Intent = Intent(
        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
        Uri.parse("package:${context.packageName}"),
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    fun createInstallIntent(apkFile: File): Intent {
        val authority = "${context.packageName}.fileprovider"
        val apkUri = FileProvider.getUriForFile(context, authority, apkFile)
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, APK_MIME_TYPE)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private fun parseRelease(payload: String): AppUpdateInfo? {
        val root = json.parseToJsonElement(payload).jsonObject
        val assets = root["assets"]?.jsonArray.orEmpty()
        val asset = assets
            .map { it.jsonObject }
            .firstOrNull { it["name"]?.jsonPrimitive?.content == RELEASE_ASSET_NAME }
            ?: return null

        val assetUpdatedAt = parseIsoInstant(
            asset["updated_at"]?.jsonPrimitive?.content
                ?: root["published_at"]?.jsonPrimitive?.content,
        )
        val releaseNotes = root["body"]?.jsonPrimitive?.content?.trim().orEmpty()
        val versionLabel = root["tag_name"]?.jsonPrimitive?.content
            ?: root["name"]?.jsonPrimitive?.content
            ?: "Latest release"
        val releaseTitle = root["name"]?.jsonPrimitive?.content?.ifBlank { versionLabel } ?: versionLabel

        return AppUpdateInfo(
            versionLabel = versionLabel,
            releaseTitle = releaseTitle,
            releaseNotes = releaseNotes,
            downloadUrl = asset["browser_download_url"]?.jsonPrimitive?.content
                ?: throw IOException("Release asset is missing a download URL."),
            assetSizeBytes = asset["size"]?.jsonPrimitive?.longOrNull ?: 0L,
            assetUpdatedAtEpochMs = assetUpdatedAt,
        )
    }

    private fun parseIsoInstant(value: String?): Long {
        return runCatching { Instant.parse(value).toEpochMilli() }.getOrDefault(0L)
    }

    private fun openConnection(rawUrl: String): HttpURLConnection {
        return (URL(rawUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15_000
            readTimeout = 30_000
            instanceFollowRedirects = true
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "HawkBoard")
        }
    }

    private companion object {
        const val LATEST_RELEASE_URL = "https://api.github.com/repos/iHawksPro/HawkBoard/releases/latest"
        const val RELEASE_ASSET_NAME = "HawkBoard-release-signed.apk"
        const val APK_MIME_TYPE = "application/vnd.android.package-archive"
    }
}
