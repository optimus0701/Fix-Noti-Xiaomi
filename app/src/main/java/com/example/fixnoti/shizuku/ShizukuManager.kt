package com.example.fixnoti.shizuku

import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import rikka.shizuku.Shizuku

class ShizukuManager(
    private val onPermissionResult: (Boolean) -> Unit
) {

    private val requestCode = 1001
    private val handler = Handler(Looper.getMainLooper())

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

            // Tự động kiểm tra và yêu cầu quyền ngay khi đăng ký listener lúc mở app
            checkAndRequestPermissionWithRetry()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun unregisterListeners() {
        try {
            handler.removeCallbacksAndMessages(null)
            Shizuku.removeRequestPermissionResultListener(permissionListener)
            Shizuku.removeBinderReceivedListener(binderReceivedListener)
            Shizuku.removeBinderDeadListener(binderDeadListener)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun checkAndRequestPermissionWithRetry(retryCount: Int = 3) {
        if (checkAndRequestPermission()) return

        if (retryCount > 0 && !ShizukuShellExecutor.isShizukuAvailable()) {
            handler.postDelayed({
                checkAndRequestPermissionWithRetry(retryCount - 1)
            }, 500)
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
