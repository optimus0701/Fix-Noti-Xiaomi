package com.example.fixnoti.repository

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.example.fixnoti.model.AppDetailStatus
import com.example.fixnoti.model.AppInfo
import com.example.fixnoti.model.FixLog
import com.example.fixnoti.model.OpStatus
import com.example.fixnoti.shizuku.ShizukuShellExecutor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class AppRepository {

    companion object {
        private const val JSDELIVR_RAW_URL = "https://cdn.jsdelivr.net/gh/optimus0701/Fix-Noti-Xiaomi@master/user_apps.txt"
        private const val GITHUB_RAW_URL = "https://raw.githubusercontent.com/optimus0701/Fix-Noti-Xiaomi/master/user_apps.txt"

        @Volatile
        private var cachedRecommendedPackages: Set<String>? = null

        private val DEFAULT_RECOMMENDED_PACKAGES = setOf(
            // Mạng xã hội & Nhắn tin
            "com.zing.zalo",
            "com.facebook.orca",
            "com.facebook.katana",
            "org.telegram.messenger",
            "com.whatsapp",
            "com.instagram.android",
            "com.instagram.barcelona",
            "com.zhiliaoapp.musically",
            "com.ss.android.ugc.trill",
            "com.locket.Locket",
            "com.discord",
            "com.viber.voip",
            "jp.naver.line.android",
            "com.tencent.mm",
            "com.twitter.android",
            "com.skype.raider",
            // Ngân hàng & Ví điện tử
            "com.mservice.momotransfer",
            "com.mbmobile",
            "com.vietcombank.phone",
            "vn.com.techcombank.bb.app",
            "com.vpb.neo",
            "com.vnpay.bidv",
            "com.vietinbank.ipay",
            "com.vnpay.agribank3g",
            "com.acb.mobile",
            "com.tpb.mb.gprsauto",
            "com.sacombank.mbanking",
            "com.msb.mb",
            "com.vib.myvib2",
            "vn.cake.app",
            "vn.vnpay.vnpaywallet",
            "vn.com.vng.zalopay",
            "com.bplus.vtpay",
            "com.airpay.consumer"
        )
    }

    suspend fun fetchRecommendedPackageNames(): Set<String> = withContext(Dispatchers.IO) {
        cachedRecommendedPackages?.let { return@withContext it }

        val urls = listOf(JSDELIVR_RAW_URL, GITHUB_RAW_URL)
        for (urlString in urls) {
            try {
                val result = withTimeoutOrNull(2500L) {
                    val url = java.net.URL(urlString)
                    val connection = url.openConnection() as java.net.HttpURLConnection
                    connection.connectTimeout = 2000
                    connection.readTimeout = 2000
                    connection.requestMethod = "GET"
                    connection.setRequestProperty("User-Agent", "FixNotiXiaomi/1.0")

                    if (connection.responseCode == java.net.HttpURLConnection.HTTP_OK) {
                        val text = connection.inputStream.bufferedReader().use { it.readText() }
                        val parsed = text.lines()
                            .map { line ->
                                var l = line.trim()
                                if (l.startsWith("package:")) l = l.substring("package:".length).trim()
                                l
                            }
                            .filter { line -> line.isNotEmpty() && !line.startsWith("#") }
                            .toSet()

                        if (parsed.isNotEmpty()) parsed else null
                    } else null
                }
                if (result != null && result.isNotEmpty()) {
                    cachedRecommendedPackages = result
                    return@withContext result
                }
            } catch (e: Exception) {
                // Ignore network failure and try next or fallback
            }
        }

        cachedRecommendedPackages = DEFAULT_RECOMMENDED_PACKAGES
        DEFAULT_RECOMMENDED_PACKAGES
    }

    suspend fun getInstalledApps(context: Context, showAll: Boolean = false): List<AppInfo> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val resultPackageNames = mutableSetOf<String>()
        val appInfoMap = mutableMapOf<String, AppInfo>()

        // 1. Thử lấy danh sách package từ PackageManager chuẩn của Android
        try {
            val packages = pm.getInstalledPackages(PackageManager.GET_META_DATA)
            for (pkg in packages) {
                val appInfo = pkg.applicationInfo ?: continue
                val packageName = pkg.packageName
                val isUserApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) == 0 ||
                        (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
                val isGoogleGms = packageName == "com.google.android.gms"

                if (isUserApp || isGoogleGms) {
                    resultPackageNames.add(packageName)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Nếu Shizuku đã được cấp quyền, lấy thêm từ Shizuku Shell (bỏ qua giới hạn MIUI Security)
        if (ShizukuShellExecutor.isPermissionGranted()) {
            try {
                val shellOutput = ShizukuShellExecutor.executeCommand("pm list packages")
                shellOutput.lines().forEach { line ->
                    val trimmed = line.trim()
                    if (trimmed.startsWith("package:")) {
                        val pkgName = trimmed.substring("package:".length).trim()
                        if (pkgName.isNotEmpty()) {
                            resultPackageNames.add(pkgName)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 3. Dự phòng cho MIUI/Xiaomi: Nếu danh sách ứng dụng bị hạn chế do MIUI chặn `getInstalledPackages`,
        // ta trực tiếp kiểm tra sự tồn tại của các ứng dụng trong danh sách Đề xuất (Banking & MXH) bằng `getApplicationInfo`
        val recommendedPackages = fetchRecommendedPackageNames()
        val targetPackageSet = if (showAll) {
            resultPackageNames + recommendedPackages + "com.google.android.gms"
        } else {
            recommendedPackages + "com.google.android.gms"
        }

        // 4. Xây dựng danh sách AppInfo đầy đủ với tên ứng dụng và icon
        for (packageName in targetPackageSet) {
            val isGoogleGms = packageName == "com.google.android.gms"
            if (!showAll && !isGoogleGms && !recommendedPackages.contains(packageName)) {
                continue
            }

            try {
                val appInfo = pm.getApplicationInfo(packageName, 0)
                val isUserApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) == 0 ||
                        (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0

                if (isUserApp || isGoogleGms) {
                    val label = try {
                        pm.getApplicationLabel(appInfo).toString()
                    } catch (e: Exception) {
                        packageName
                    }
                    val icon = try {
                        pm.getApplicationIcon(appInfo)
                    } catch (e: Exception) {
                        null
                    }

                    appInfoMap[packageName] = AppInfo(
                        appName = if (isGoogleGms) "$label (Google Play Services)" else label,
                        packageName = packageName,
                        icon = icon,
                        isGoogleGms = isGoogleGms
                    )
                }
            } catch (e: PackageManager.NameNotFoundException) {
                // Ứng dụng không thực sự được cài đặt trên máy này
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val resultList = appInfoMap.values.toList()
        resultList.sortedWith(compareByDescending<AppInfo> { it.isGoogleGms }.thenBy { it.appName.lowercase() })
    }

    suspend fun checkAppDetailStatus(packageName: String): AppDetailStatus = withContext(Dispatchers.IO) {
        // 1. Check DeviceIdle Whitelist
        val whitelistOutput = ShizukuShellExecutor.executeCommand("dumpsys deviceidle whitelist")
        val isWhitelisted = whitelistOutput.contains(packageName, ignoreCase = true)

        // 2. Check Standby Bucket
        val standbyOutput = ShizukuShellExecutor.executeCommand("am get-standby-bucket $packageName")
        val standbyBucket = parseStandbyBucket(standbyOutput)

        // 3. Check AppOp RUN_IN_BACKGROUND
        val bgOutput = ShizukuShellExecutor.executeCommand("cmd appops get $packageName RUN_IN_BACKGROUND")
        val runInBackground = parseOpStatus(bgOutput)

        // 4. Check AppOp RUN_ANY_IN_BACKGROUND
        val anyBgOutput = ShizukuShellExecutor.executeCommand("cmd appops get $packageName RUN_ANY_IN_BACKGROUND")
        val runAnyInBackground = parseOpStatus(anyBgOutput)

        // 5. Check AppOp 10008 (Auto Start)
        val autoStartOutput = ShizukuShellExecutor.executeCommand("cmd appops get $packageName 10008")
        val autoStart = parseOpStatus(autoStartOutput)

        // 6, 7, 8. Check MIUI System Tables (Skip for Google Play Services)
        val isGms = packageName == "com.google.android.gms"
        val isMilletWhite = if (isGms) true else isPkgInSystemSetting("millet_white", packageName)
        val isCloudLowLatency = if (isGms) true else isPkgInSystemSetting("cloud_lowlatency_whitelist", packageName)
        val isMilletNoRestrict = if (isGms) true else isPkgInSystemSetting("MILLET_NO_RESTRICT_APP", packageName)

        AppDetailStatus(
            isWhitelisted = isWhitelisted,
            standbyBucket = standbyBucket,
            runInBackground = runInBackground,
            runAnyInBackground = runAnyInBackground,
            autoStart = autoStart,
            isMilletWhite = isMilletWhite,
            isCloudLowLatency = isCloudLowLatency,
            isMilletNoRestrict = isMilletNoRestrict
        )
    }

    suspend fun fixApp(
        app: AppInfo,
        onLog: suspend (FixLog) -> Unit
    ): AppDetailStatus = withContext(Dispatchers.IO) {
        val name = app.appName
        val pkg = app.packageName

        // 1. Fix Whitelist
        onLog(FixLog(name, pkg, "Đang thêm vào DeviceIdle Whitelist (bỏ qua tối ưu pin)..."))
        ShizukuShellExecutor.executeCommand("cmd deviceidle whitelist +$pkg")

        // 2. Fix Standby Bucket -> ACTIVE
        onLog(FixLog(name, pkg, "Đang thiết lập Standby Bucket -> ACTIVE..."))
        ShizukuShellExecutor.executeCommand("am set-standby-bucket $pkg active")

        // 3. Fix AppOp RUN_IN_BACKGROUND -> allow
        onLog(FixLog(name, pkg, "Đang bật quyền RUN_IN_BACKGROUND -> ALLOW..."))
        ShizukuShellExecutor.executeCommand("cmd appops set $pkg RUN_IN_BACKGROUND allow")

        // 4. Fix AppOp RUN_ANY_IN_BACKGROUND -> allow
        onLog(FixLog(name, pkg, "Đang bật quyền RUN_ANY_IN_BACKGROUND -> ALLOW..."))
        ShizukuShellExecutor.executeCommand("cmd appops set $pkg RUN_ANY_IN_BACKGROUND allow")

        // 5. Fix MIUI System Table Keys (Ngoại trừ Dịch vụ Google Play)
        if (!app.isGoogleGms) {
            onLog(FixLog(name, pkg, "Đang thêm vào MIUI System: millet_white..."))
            addToSystemSetting("millet_white", pkg)

            onLog(FixLog(name, pkg, "Đang thêm vào MIUI System: cloud_lowlatency_whitelist..."))
            addToSystemSetting("cloud_lowlatency_whitelist", pkg)

            onLog(FixLog(name, pkg, "Đang thêm vào MIUI System: MILLET_NO_RESTRICT_APP..."))
            addToSystemSetting("MILLET_NO_RESTRICT_APP", pkg)
        }

        onLog(FixLog(name, pkg, "✓ Hoàn thành tối ưu cho $name!", isSuccess = true))

        // Re-check status to verify fix
        checkAppDetailStatus(pkg)
    }

    suspend fun revokeSinglePermission(
        app: AppInfo,
        permissionType: String,
        onLog: (suspend (FixLog) -> Unit)? = null
    ): AppDetailStatus = withContext(Dispatchers.IO) {
        val name = app.appName
        val pkg = app.packageName

        when (permissionType) {
            "WHITELIST" -> {
                onLog?.invoke(FixLog(name, pkg, "Đang loại bỏ khỏi DeviceIdle Whitelist..."))
                ShizukuShellExecutor.executeCommand("cmd deviceidle whitelist -$pkg")
            }
            "STANDBY_BUCKET" -> {
                onLog?.invoke(FixLog(name, pkg, "Đang đặt Standby Bucket -> RARE..."))
                ShizukuShellExecutor.executeCommand("am set-standby-bucket $pkg rare")
            }
            "RUN_IN_BACKGROUND" -> {
                onLog?.invoke(FixLog(name, pkg, "Đang đặt RUN_IN_BACKGROUND -> IGNORE..."))
                ShizukuShellExecutor.executeCommand("cmd appops set $pkg RUN_IN_BACKGROUND ignore")
            }
            "RUN_ANY_IN_BACKGROUND" -> {
                onLog?.invoke(FixLog(name, pkg, "Đang đặt RUN_ANY_IN_BACKGROUND -> IGNORE..."))
                ShizukuShellExecutor.executeCommand("cmd appops set $pkg RUN_ANY_IN_BACKGROUND ignore")
            }
            "MILLET_WHITE" -> {
                onLog?.invoke(FixLog(name, pkg, "Đang xóa khỏi MIUI System: millet_white..."))
                removeFromSystemSetting("millet_white", pkg)
            }
            "CLOUD_LOWLATENCY" -> {
                onLog?.invoke(FixLog(name, pkg, "Đang xóa khỏi MIUI System: cloud_lowlatency_whitelist..."))
                removeFromSystemSetting("cloud_lowlatency_whitelist", pkg)
            }
            "MILLET_NO_RESTRICT" -> {
                onLog?.invoke(FixLog(name, pkg, "Đang xóa khỏi MIUI System: MILLET_NO_RESTRICT_APP..."))
                removeFromSystemSetting("MILLET_NO_RESTRICT_APP", pkg)
            }
            "AUTO_START" -> {
                openAppSettings(pkg)
            }
        }
        checkAppDetailStatus(pkg)
    }

    suspend fun revokeAllPermissions(
        app: AppInfo,
        onLog: suspend (FixLog) -> Unit
    ): AppDetailStatus = withContext(Dispatchers.IO) {
        val name = app.appName
        val pkg = app.packageName

        onLog(FixLog(name, pkg, "Đang loại bỏ khỏi DeviceIdle Whitelist..."))
        ShizukuShellExecutor.executeCommand("cmd deviceidle whitelist -$pkg")

        onLog(FixLog(name, pkg, "Đang đặt lại Standby Bucket -> RARE..."))
        ShizukuShellExecutor.executeCommand("am set-standby-bucket $pkg rare")

        onLog(FixLog(name, pkg, "Đang tắt quyền RUN_IN_BACKGROUND -> IGNORE..."))
        ShizukuShellExecutor.executeCommand("cmd appops set $pkg RUN_IN_BACKGROUND ignore")

        onLog(FixLog(name, pkg, "Đang tắt quyền RUN_ANY_IN_BACKGROUND -> IGNORE..."))
        ShizukuShellExecutor.executeCommand("cmd appops set $pkg RUN_ANY_IN_BACKGROUND ignore")

        if (!app.isGoogleGms) {
            onLog(FixLog(name, pkg, "Đang xóa khỏi MIUI System: millet_white..."))
            removeFromSystemSetting("millet_white", pkg)

            onLog(FixLog(name, pkg, "Đang xóa khỏi MIUI System: cloud_lowlatency_whitelist..."))
            removeFromSystemSetting("cloud_lowlatency_whitelist", pkg)

            onLog(FixLog(name, pkg, "Đang xóa khỏi MIUI System: MILLET_NO_RESTRICT_APP..."))
            removeFromSystemSetting("MILLET_NO_RESTRICT_APP", pkg)
        }

        onLog(FixLog(name, pkg, "✓ Hoàn tất hủy bỏ tất cả cấu hình cho $name!", isSuccess = true))

        checkAppDetailStatus(pkg)
    }

    private fun isPkgInSystemSetting(key: String, packageName: String): Boolean {
        val output = ShizukuShellExecutor.executeCommand("settings get system $key").trim()
        if (output == "null" || output.isBlank()) return false
        val list = output.split(';', ',', ':', ' ').map { it.trim() }
        return list.contains(packageName)
    }

    private fun addToSystemSetting(key: String, packageName: String) {
        val currentRaw = ShizukuShellExecutor.executeCommand("settings get system $key").trim()
        val current = if (currentRaw == "null") "" else currentRaw

        // Tự động phát hiện dấu phân cách (separator) từ chuỗi hiện tại
        val isMilletWhite = key.equals("millet_white", ignoreCase = true)
        val delimiter: Char = when {
            isMilletWhite -> ';'
            current.contains(',') -> ','
            current.contains(';') -> ';'
            current.contains(':') -> ':'
            else -> ',' // Mặc định dùng dấu phẩy cho các key khác ngoại trừ millet_white
        }

        // Tách danh sách hiện tại theo tất cả các dấu phân cách có thể
        val existingList = current.split(';', ',', ':', ' ')
            .map { it.trim() }
            .filter { it.isNotEmpty() && it != "null" }
            .toMutableList()

        if (!existingList.contains(packageName)) {
            existingList.add(packageName)
        }

        // Tạo chuỗi giá trị mới phù hợp với dấu phân cách
        val newValue = if (delimiter == ';') {
            existingList.joinToString(";") + ";"
        } else {
            existingList.joinToString(delimiter.toString())
        }

        ShizukuShellExecutor.executeCommand("settings put system $key \"$newValue\"")
    }

    private fun removeFromSystemSetting(key: String, packageName: String) {
        val currentRaw = ShizukuShellExecutor.executeCommand("settings get system $key").trim()
        if (currentRaw == "null" || currentRaw.isBlank()) return

        val isMilletWhite = key.equals("millet_white", ignoreCase = true)
        val delimiter: Char = when {
            isMilletWhite -> ';'
            currentRaw.contains(',') -> ','
            currentRaw.contains(';') -> ';'
            currentRaw.contains(':') -> ':'
            else -> ','
        }

        val existingList = currentRaw.split(';', ',', ':', ' ')
            .map { it.trim() }
            .filter { it.isNotEmpty() && it != "null" }
            .toMutableList()

        if (existingList.contains(packageName)) {
            existingList.remove(packageName)

            val newValue = if (existingList.isEmpty()) {
                ""
            } else if (delimiter == ';') {
                existingList.joinToString(";") + ";"
            } else {
                existingList.joinToString(delimiter.toString())
            }

            ShizukuShellExecutor.executeCommand("settings put system $key \"$newValue\"")
        }
    }

    suspend fun openGcmDiagnostics(): String = withContext(Dispatchers.IO) {
        ShizukuShellExecutor.executeCommand("am start -n com.google.android.gms/.gcm.GcmDiagnostics")
    }

    suspend fun openAppSettings(packageName: String): String = withContext(Dispatchers.IO) {
        ShizukuShellExecutor.executeCommand("am start -a android.settings.APPLICATION_DETAILS_SETTINGS -d package:$packageName")
    }

    private fun parseStandbyBucket(output: String): String {
        return when {
            output.contains("EXEMPTED", ignoreCase = true) || output.trim() == "5" -> "EXEMPTED (5)"
            output.contains("ACTIVE", ignoreCase = true) || output.trim() == "10" -> "ACTIVE (10)"
            output.contains("WORKING_SET", ignoreCase = true) || output.trim() == "20" -> "WORKING_SET (20)"
            output.contains("FREQUENT", ignoreCase = true) || output.trim() == "30" -> "FREQUENT (30)"
            output.contains("RARE", ignoreCase = true) || output.trim() == "40" -> "RARE (40)"
            output.contains("RESTRICTED", ignoreCase = true) || output.trim() == "45" -> "RESTRICTED (45)"
            output.isNotEmpty() -> output
            else -> "UNKNOWN"
        }
    }

    private fun parseOpStatus(output: String): OpStatus {
        return when {
            output.contains("allow", ignoreCase = true) -> OpStatus.ALLOWED
            output.contains("ignore", ignoreCase = true) -> OpStatus.IGNORED
            output.contains("deny", ignoreCase = true) -> OpStatus.DENIED
            else -> OpStatus.UNKNOWN
        }
    }
}
