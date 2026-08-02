package com.example.fixnoti.shizuku

import android.content.pm.PackageManager
import rikka.shizuku.Shizuku

class ShizukuManager(
    private val onPermissionResult: (Boolean) -> Unit
) {

    private val requestCode = 1001

    private val permissionListener = Shizuku.OnRequestPermissionResultListener { reqCode, grantResult ->
        if (reqCode == requestCode) {
            val granted = grantResult == PackageManager.PERMISSION_GRANTED
            onPermissionResult(granted)
        }
    }

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        checkAndRequestPermission()
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        onPermissionResult(false)
    }

    fun registerListeners() {
        try {
            Shizuku.addRequestPermissionResultListener(permissionListener)
            Shizuku.addBinderReceivedListener(binderReceivedListener)
            Shizuku.addBinderDeadListener(binderDeadListener)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun unregisterListeners() {
        try {
            Shizuku.removeRequestPermissionResultListener(permissionListener)
            Shizuku.removeBinderReceivedListener(binderReceivedListener)
            Shizuku.removeBinderDeadListener(binderDeadListener)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun checkAndRequestPermission(): Boolean {
        if (!ShizukuShellExecutor.isShizukuAvailable()) {
            onPermissionResult(false)
            return false
        }

        return try {
            if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                onPermissionResult(true)
                true
            } else {
                Shizuku.requestPermission(requestCode)
                false
            }
        } catch (e: Exception) {
            onPermissionResult(false)
            false
        }
    }
}
