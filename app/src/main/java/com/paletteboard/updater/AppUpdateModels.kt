package com.paletteboard.updater

data class AppUpdateInfo(
    val versionLabel: String,
    val releaseTitle: String,
    val releaseNotes: String,
    val downloadUrl: String,
    val assetSizeBytes: Long,
    val assetUpdatedAtEpochMs: Long,
)

data class AppUpdateUiState(
    val currentVersionLabel: String = "",
    val latestRelease: AppUpdateInfo? = null,
    val updateAvailable: Boolean = false,
    val isChecking: Boolean = false,
    val isDownloading: Boolean = false,
    val downloadProgress: Float? = null,
    val statusMessage: String? = null,
    val errorMessage: String? = null,
)
