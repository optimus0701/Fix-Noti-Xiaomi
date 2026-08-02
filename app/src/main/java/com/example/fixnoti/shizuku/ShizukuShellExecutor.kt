package com.example.fixnoti.shizuku

import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.reflect.Method

object ShizukuShellExecutor {

    private var newProcessMethod: Method? = null

    init {
        findNewProcessMethod()
    }

    private fun findNewProcessMethod() {
        try {
            val methods = Shizuku::class.java.declaredMethods
            for (m in methods) {
                if (m.name == "newProcess") {
                    m.isAccessible = true
                    newProcessMethod = m
                    break
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun isShizukuAvailable(): Boolean {
        return try {
            Shizuku.pingBinder()
        } catch (e: Throwable) {
            false
        }
    }

    fun isPermissionGranted(): Boolean {
        return try {
            if (!isShizukuAvailable()) return false
            if (Shizuku.isPreV11()) return false
            Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED
        } catch (e: Throwable) {
            false
        }
    }

    fun executeCommand(command: String): String {
        if (!isPermissionGranted()) {
            return "ERROR: Shizuku permission not granted"
        }
        return try {
            if (newProcessMethod == null) {
                findNewProcessMethod()
            }
            val process = newProcessMethod?.invoke(null, arrayOf("sh", "-c", command), null, null) as? Process
                ?: return "ERROR: Cannot invoke Shizuku newProcess"

            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                output.append(line).append("\n")
            }
            process.waitFor()
            reader.close()
            output.toString().trim()
        } catch (e: Throwable) {
            e.printStackTrace()
            "ERROR: ${e.localizedMessage}"
        }
    }
}
