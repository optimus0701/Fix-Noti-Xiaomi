package com.example.fixnoti.model

import android.graphics.drawable.Drawable

enum class OpStatus {
    ALLOWED,
    IGNORED,
    DENIED,
    DEFAULT,
    UNKNOWN;

    fun isOk(): Boolean = this == ALLOWED
}

data class AppDetailStatus(
    val isWhitelisted: Boolean = false,
    val standbyBucket: String = "UNKNOWN",
    val runInBackground: OpStatus = OpStatus.UNKNOWN,
    val runAnyInBackground: OpStatus = OpStatus.UNKNOWN,
    val autoStart: OpStatus = OpStatus.UNKNOWN,
    val autoRevokePermissions: OpStatus = OpStatus.UNKNOWN,
    val isMilletWhiteSupported: Boolean = false,
    val isMilletWhite: Boolean = false,
    val isCloudLowLatencySupported: Boolean = false,
    val isCloudLowLatency: Boolean = false,
    val isMilletNoRestrictSupported: Boolean = false,
    val isMilletNoRestrict: Boolean = false
) {
    fun isAllOptimized(isGms: Boolean = false): Boolean {
        val isBucketOk = standbyBucket.contains("ACTIVE", ignoreCase = true) ||
                standbyBucket.contains("EXEMPTED", ignoreCase = true) ||
                standbyBucket.contains("10") ||
                standbyBucket.contains("5")

        val autoStartOk = autoStart.isOk() || autoStart == OpStatus.UNKNOWN
        val autoRevokeOk = autoRevokePermissions == OpStatus.IGNORED || autoRevokePermissions == OpStatus.UNKNOWN
        val baseOk = isWhitelisted && isBucketOk && runInBackground.isOk() && runAnyInBackground.isOk() && autoStartOk && autoRevokeOk

        val milletWhiteOk = !isMilletWhiteSupported || isMilletWhite
        val cloudLowLatencyOk = !isCloudLowLatencySupported || isCloudLowLatency
        val milletNoRestrictOk = !isMilletNoRestrictSupported || isMilletNoRestrict

        return if (isGms) {
            baseOk
        } else {
            baseOk && milletWhiteOk && cloudLowLatencyOk && milletNoRestrictOk
        }
    }
}

data class AppInfo(
    val appName: String,
    val packageName: String,
    val icon: Drawable? = null,
    val isSelected: Boolean = false,
    val isGoogleGms: Boolean = false,
    val detailStatus: AppDetailStatus? = null
)

data class FixLog(
    val appName: String,
    val packageName: String,
    val actionText: String,
    val isSuccess: Boolean = true
)
