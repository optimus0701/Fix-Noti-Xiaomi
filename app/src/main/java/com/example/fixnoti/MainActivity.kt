package com.example.fixnoti

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.fixnoti.shizuku.ShizukuManager
import com.example.fixnoti.ui.MainViewModel
import com.example.fixnoti.ui.components.AppListScreen

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private lateinit var shizukuManager: ShizukuManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Đăng ký Shizuku Listener & Tự động xin quyền khi ứng dụng khởi chạy
        shizukuManager = ShizukuManager { isGranted ->
            viewModel.updateShizukuStatus(isGranted)
            if (isGranted) {
                viewModel.loadApps(applicationContext)
            }
        }
        shizukuManager.registerListeners()

        // Tải danh sách app khi mở app
        viewModel.loadApps(applicationContext)

        setContent {
            FixNotiTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppListScreen(
                        viewModel = viewModel,
                        onRequestShizukuPermission = {
                            shizukuManager.checkAndRequestPermissionWithRetry()
                        }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Tự động kiểm tra & hỏi quyền Shizuku nếu chưa được cấp khi quay lại màn hình
        shizukuManager.checkAndRequestPermissionWithRetry()
    }

    override fun onDestroy() {
        super.onDestroy()
        shizukuManager.unregisterListeners()
    }
}

@Composable
fun FixNotiTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(),
        content = content
    )
}
