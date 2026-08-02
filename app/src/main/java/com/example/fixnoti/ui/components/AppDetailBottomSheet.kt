package com.example.fixnoti.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.example.fixnoti.model.AppDetailStatus
import com.example.fixnoti.model.AppInfo
import com.example.fixnoti.model.OpStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDetailBottomSheet(
    app: AppInfo,
    status: AppDetailStatus?,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onFixSingleApp: () -> Unit,
    onRevokeSinglePermission: (String) -> Unit,
    onRevokeAllPermissions: () -> Unit,
    onOpenAppSettings: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = { onDismiss() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            // Header: App Info
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                val iconBitmap = app.icon?.toBitmap()?.asImageBitmap()
                if (iconBitmap != null) {
                    Image(
                        bitmap = iconBitmap,
                        contentDescription = app.appName,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = app.appName.take(1).uppercase(),
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = app.appName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        text = app.packageName,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Divider()
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Trạng thái Cấu hình Chạy nền & Quyền hệ thống:",
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (isLoading || status == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                // 1. DeviceIdle Whitelist
                DetailItemRow(
                    title = "1. DeviceIdle Whitelist",
                    subtitle = "Danh sách bỏ qua tối ưu hóa pin hệ thống",
                    isOk = status.isWhitelisted,
                    statusText = if (status.isWhitelisted) "ĐÃ BỎ QUA TỐI ƯU PIN" else "CHƯA BỎ QUA TỐI ƯU PIN",
                    actionText = if (status.isWhitelisted) "Thu hồi" else null,
                    onActionClick = if (status.isWhitelisted) { { onRevokeSinglePermission("WHITELIST") } } else null
                )

                // 2. Standby Bucket
                val isBucketOk = status.standbyBucket.contains("ACTIVE", ignoreCase = true) ||
                        status.standbyBucket.contains("EXEMPTED", ignoreCase = true) ||
                        status.standbyBucket.contains("10") ||
                        status.standbyBucket.contains("5")

                DetailItemRow(
                    title = "2. Standby Bucket",
                    subtitle = "Nhóm phân loại ưu tiên chạy ngầm",
                    isOk = isBucketOk,
                    statusText = "Hiện tại: ${status.standbyBucket}",
                    actionText = if (isBucketOk) "Thu hồi" else null,
                    onActionClick = if (isBucketOk) { { onRevokeSinglePermission("STANDBY_BUCKET") } } else null
                )

                // 3. RUN_IN_BACKGROUND
                DetailItemRow(
                    title = "3. Quyền RUN_IN_BACKGROUND",
                    subtitle = "Cho phép dịch vụ ứng dụng chạy ngầm",
                    isOk = status.runInBackground.isOk(),
                    statusText = "Trạng thái: ${status.runInBackground.name}",
                    actionText = if (status.runInBackground.isOk()) "Thu hồi" else null,
                    onActionClick = if (status.runInBackground.isOk()) { { onRevokeSinglePermission("RUN_IN_BACKGROUND") } } else null
                )

                // 4. RUN_ANY_IN_BACKGROUND
                DetailItemRow(
                    title = "4. Quyền RUN_ANY_IN_BACKGROUND",
                    subtitle = "Cho phép tác vụ ngầm/Alarm/Broadcast",
                    isOk = status.runAnyInBackground.isOk(),
                    statusText = "Trạng thái: ${status.runAnyInBackground.name}",
                    actionText = if (status.runAnyInBackground.isOk()) "Thu hồi" else null,
                    onActionClick = if (status.runAnyInBackground.isOk()) { { onRevokeSinglePermission("RUN_ANY_IN_BACKGROUND") } } else null
                )

                // 5. Auto Start (10008)
                val autoStartText = when (status.autoStart) {
                    OpStatus.ALLOWED -> "ĐÃ BẬT (ALLOW)"
                    OpStatus.IGNORED -> "ĐÃ TẮT (IGNORE)"
                    OpStatus.DENIED -> "ĐÃ TẮT (DENY)"
                    OpStatus.UNKNOWN -> "Không lấy được giá trị"
                }
                DetailItemRow(
                    title = "5. Auto Start (Tự khởi chạy)",
                    subtitle = "Quyền tự khởi chạy hệ thống (AppOp 10008)",
                    isOk = status.autoStart.isOk(),
                    statusText = autoStartText,
                    actionText = "Sửa quyền",
                    onActionClick = { onOpenAppSettings() }
                )

                if (!app.isGoogleGms) {
                    DetailItemRow(
                        title = "6. MIUI millet_white",
                        subtitle = "Danh sách trắng Millet Freeze Killer",
                        isOk = status.isMilletWhite,
                        statusText = if (status.isMilletWhite) "ĐÃ CÓ TRONG MILLET_WHITE" else "CHƯA CÓ TRONG MILLET_WHITE",
                        actionText = if (status.isMilletWhite) "Thu hồi" else null,
                        onActionClick = if (status.isMilletWhite) { { onRevokeSinglePermission("MILLET_WHITE") } } else null
                    )

                    DetailItemRow(
                        title = "7. MIUI cloud_lowlatency_whitelist",
                        subtitle = "Danh sách ưu tiên độ trễ thấp Cloud",
                        isOk = status.isCloudLowLatency,
                        statusText = if (status.isCloudLowLatency) "ĐÃ CÓ TRONG LOWLATENCY_WHITELIST" else "CHƯA CÓ TRONG LOWLATENCY_WHITELIST",
                        actionText = if (status.isCloudLowLatency) "Thu hồi" else null,
                        onActionClick = if (status.isCloudLowLatency) { { onRevokeSinglePermission("CLOUD_LOWLATENCY") } } else null
                    )

                    DetailItemRow(
                        title = "8. MIUI MILLET_NO_RESTRICT_APP",
                        subtitle = "Danh sách ứng dụng Millet không hạn chế",
                        isOk = status.isMilletNoRestrict,
                        statusText = if (status.isMilletNoRestrict) "ĐÃ CÓ TRONG MILLET_NO_RESTRICT" else "CHƯA CÓ TRONG MILLET_NO_RESTRICT",
                        actionText = if (status.isMilletNoRestrict) "Thu hồi" else null,
                        onActionClick = if (status.isMilletNoRestrict) { { onRevokeSinglePermission("MILLET_NO_RESTRICT") } } else null
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Nút Sửa riêng ứng dụng này
                Button(
                    onClick = { onFixSingleApp() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(imageVector = Icons.Outlined.Build, contentDescription = "Fix App")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "CẤP QUYỀN / TỐI ƯU RIÊNG APP NÀY", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Nút Xóa toàn bộ quyền đối với app đang xem
                OutlinedButton(
                    onClick = { onRevokeAllPermissions() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                ) {
                    Icon(imageVector = Icons.Outlined.Delete, contentDescription = "Revoke All")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "XÓA BỎ TOÀN BỘ QUYỀN VỀ MẶC ĐỊNH", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun DetailItemRow(
    title: String,
    subtitle: String,
    isOk: Boolean,
    statusText: String,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isOk) Icons.Default.CheckCircle else Icons.Default.Warning,
            contentDescription = null,
            tint = if (isOk) Color(0xFF2E7D32) else Color(0xFFD32F2F),
            modifier = Modifier.size(26.dp)
        )

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(text = subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = statusText,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isOk) Color(0xFF2E7D32) else Color(0xFFD32F2F)
            )
        }

        if (actionText != null && onActionClick != null) {
            Spacer(modifier = Modifier.width(6.dp))
            OutlinedButton(
                onClick = onActionClick,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                modifier = Modifier.height(32.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = if (actionText == "Sửa quyền") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
            ) {
                Text(
                    text = actionText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
