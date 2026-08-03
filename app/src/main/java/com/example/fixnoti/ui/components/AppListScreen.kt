package com.example.fixnoti.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fixnoti.ui.MainUiState
import com.example.fixnoti.ui.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppListScreen(
    viewModel: MainViewModel,
    onRequestShizukuPermission: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    // Tự động kiểm tra và yêu cầu quyền Shizuku khi màn hình được tạo
    LaunchedEffect(Unit) {
        if (!uiState.isShizukuGranted) {
            onRequestShizukuPermission()
        }
    }

    val filteredApps = MainViewModel.getFilteredApps(uiState.appList, uiState.searchQuery)
    val selectedCount = uiState.appList.count { it.isSelected }
    val allFilteredSelected = filteredApps.isNotEmpty() && filteredApps.all { it.isSelected }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "FixNoti Xiaomi",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                        Text(
                            text = "Tối ưu hóa chạy ngầm & thông báo qua Shizuku",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    // Nút GcmDiagnostics
                    Button(
                        onClick = { viewModel.openGcmDiagnostics() },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        modifier = Modifier
                            .height(34.dp)
                            .padding(end = 4.dp)
                    ) {
                        Text(
                            text = "GcmDiagnostics",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Refresh Button
                    IconButton(onClick = { viewModel.loadApps(context) }) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Tải lại danh sách")
                    }

                    // Shizuku Badge
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (uiState.isShizukuGranted) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (uiState.isShizukuGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (uiState.isShizukuGranted) Color(0xFF2E7D32) else Color(0xFFC62828),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (uiState.isShizukuGranted) "Shizuku OK" else "Thiếu Shizuku",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (uiState.isShizukuGranted) Color(0xFF2E7D32) else Color(0xFFC62828)
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (selectedCount > 0) {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.fixSelectedApps() },
                    icon = { Icon(imageVector = Icons.Default.Build, contentDescription = "Fix") },
                    text = { Text(text = "FIX THÔNG BÁO ($selectedCount)", fontWeight = FontWeight.Bold) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Banner Cảnh báo Shizuku nếu chưa cấp quyền (Tự động mở app hỏi quyền theo feedback)
            if (!uiState.isShizukuGranted) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "⚠️ Ứng dụng chưa được cấp quyền Shizuku!",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Vui lòng khởi chạy Shizuku và bấm nút bên dưới để cấp quyền thực thi lệnh Shell.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { onRequestShizukuPermission() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text(text = "CẤP QUYỀN SHIZUKU NGAY")
                        }
                    }
                }
            }

            // Thanh Chuyển Đổi Chế Độ Xem: Ứng dụng Đề xuất vs Tất cả ứng dụng
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (uiState.isShowAllApps) MaterialTheme.colorScheme.surfaceVariant
                    else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (uiState.isShowAllApps) "Đang hiển thị: Tất cả ứng dụng" else "Đang hiển thị: App Đề xuất (Bank & MXH)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (uiState.isShowAllApps) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = if (uiState.isShowAllApps) "Danh sách đầy đủ ứng dụng người dùng đã cài" else "Lọc các ứng dụng Ngân hàng & Mạng xã hội từ GitHub",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = { viewModel.toggleShowAllApps(context) },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (uiState.isShowAllApps) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Text(
                            text = if (uiState.isShowAllApps) "Chỉ hiện App Đề xuất" else "TẢI TẤT CẢ ỨNG DỤNG",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Thanh tìm kiếm App
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.onSearchQueryChange(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                placeholder = { Text(text = "Tìm kiếm ứng dụng...") },
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Search") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Thanh Tích chọn tất cả
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = allFilteredSelected,
                        onCheckedChange = { viewModel.toggleSelectAll() }
                    )
                    Text(
                        text = "Tích chọn tất cả (${filteredApps.size} apps)",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }

                if (selectedCount > 0) {
                    Text(
                        text = "Đã chọn: $selectedCount",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 13.sp
                    )
                }
            }

            Divider()

            // Danh sách ứng dụng
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (filteredApps.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Không tìm thấy ứng dụng phù hợp",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(
                        items = filteredApps,
                        key = { it.packageName }
                    ) { app ->
                        AppItemRow(
                            app = app,
                            onToggleSelect = { viewModel.toggleAppSelection(app.packageName) },
                            onOpenDetail = { viewModel.openAppDetail(app) }
                        )
                    }
                }
            }
        }
    }

    // Modal BottomSheet hiển thị chi tiết ứng dụng khi bấm nút (i)
    uiState.detailApp?.let { app ->
        AppDetailBottomSheet(
            app = app,
            status = uiState.detailStatus,
            isLoading = uiState.isDetailLoading,
            onDismiss = { viewModel.closeAppDetail() },
            onFixSingleApp = { viewModel.fixAppFromDetail(app) },
            onRevokeSinglePermission = { permType -> viewModel.revokeSinglePermission(app, permType) },
            onRevokeAllPermissions = { viewModel.revokeAllPermissionsFromDetail(app) },
            onOpenAppSettings = { viewModel.openAppSettings(app.packageName) }
        )
    }

    // Modal Progress Dialog khi thực thi Fix (Hiển thị Tiến trình & Logs realtime + Cảnh báo không thoát app)
    if (uiState.isFixing) {
        FixProgressDialog(
            progress = uiState.fixProgress,
            currentApp = uiState.currentFixApp,
            logs = uiState.fixLogs,
            isFinished = uiState.isFixFinished,
            onDismiss = { viewModel.closeFixProgressDialog() }
        )
    }
}
