package ru.mlbb.blocker.data

data class MlbbPackageInfo(
    val packageName: String,
    val installed: Boolean,
    val enabled: Boolean,
    val versionName: String?,
    val uid: Int?,
)
