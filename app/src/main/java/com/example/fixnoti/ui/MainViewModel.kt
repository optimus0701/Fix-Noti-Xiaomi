package com.example.fixnoti.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fixnoti.model.AppDetailStatus
import com.example.fixnoti.model.AppInfo
import com.example.fixnoti.model.FixLog
import com.example.fixnoti.repository.AppRepository
import com.example.fixnoti.shizuku.ShizukuShellExecutor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MainUiState(
    val isShizukuGranted: Boolean = false,
    val isLoading: Boolean = false,
    val isShowAllApps: Boolean = false,
    val appList: List<AppInfo> = emptyList(),
    val searchQuery: String = "",
    val detailApp: AppInfo? = null,
    val detailStatus: AppDetailStatus? = null,
    val isDetailLoading: Boolean = false,
    val isFixing: Boolean = false,
    val fixProgress: Float = 0f,
    val currentFixApp: String = "",
    val fixLogs: List<FixLog> = emptyList(),
    val isFixFinished: Boolean = false
)

class MainViewModel(
    private val repository: AppRepository = AppRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    fun updateShizukuStatus(isGranted: Boolean) {
        _uiState.update { it.copy(isShizukuGranted = isGranted) }
    }

    fun loadApps(context: Context, showAll: Boolean? = null) {
        viewModelScope.launch {
            val targetShowAll = showAll ?: _uiState.value.isShowAllApps
            _uiState.update { it.copy(isLoading = true, isShowAllApps = targetShowAll) }
            val apps = repository.getInstalledApps(context, targetShowAll)
            val isGranted = ShizukuShellExecutor.isPermissionGranted()
            _uiState.update {
                it.copy(
                    appList = apps,
                    isLoading = false,
                    isShizukuGranted = isGranted
                )
            }
        }
    }

    fun toggleShowAllApps(context: Context) {
        loadApps(context, !_uiState.value.isShowAllApps)
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun toggleAppSelection(packageName: String) {
        _uiState.update { state ->
            val updated = state.appList.map { app ->
                if (app.packageName == packageName) app.copy(isSelected = !app.isSelected) else app
            }
            state.copy(appList = updated)
        }
    }

    fun toggleSelectAll() {
        _uiState.update { state ->
            val filtered = getFilteredApps(state.appList, state.searchQuery)
            val allSelected = filtered.isNotEmpty() && filtered.all { it.isSelected }
            val targetState = !allSelected

            val filteredPkgs = filtered.map { it.packageName }.toSet()
            val updated = state.appList.map { app ->
                if (filteredPkgs.contains(app.packageName)) app.copy(isSelected = targetState) else app
            }
            state.copy(appList = updated)
        }
    }

    fun openAppDetail(app: AppInfo) {
        viewModelScope.launch {
            _uiState.update { it.copy(detailApp = app, isDetailLoading = true, detailStatus = null) }
            val status = repository.checkAppDetailStatus(app.packageName)
            _uiState.update {
                it.copy(
                    isDetailLoading = false,
                    detailStatus = status,
                    appList = updateAppDetailInList(it.appList, app.packageName, status)
                )
            }
        }
    }

    fun closeAppDetail() {
        _uiState.update { it.copy(detailApp = null, detailStatus = null) }
    }

    fun fixAppFromDetail(app: AppInfo) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isFixing = true,
                    isFixFinished = false,
                    fixProgress = 0f,
                    currentFixApp = app.appName,
                    fixLogs = listOf(FixLog(app.appName, app.packageName, "Bắt đầu tối ưu cho ${app.appName}..."))
                )
            }

            val newStatus = repository.fixApp(app) { log ->
                _uiState.update { state ->
                    state.copy(fixLogs = state.fixLogs + log)
                }
            }

            _uiState.update {
                it.copy(
                    isFixing = true,
                    isFixFinished = true,
                    fixProgress = 1f,
                    detailStatus = newStatus,
                    appList = updateAppDetailInList(it.appList, app.packageName, newStatus)
                )
            }
        }
    }

    fun revokeSinglePermission(app: AppInfo, permissionType: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isDetailLoading = true) }
            val newStatus = repository.revokeSinglePermission(app, permissionType)
            _uiState.update {
                it.copy(
                    isDetailLoading = false,
                    detailStatus = newStatus,
                    appList = updateAppDetailInList(it.appList, app.packageName, newStatus)
                )
            }
        }
    }

    fun revokeAllPermissionsFromDetail(app: AppInfo) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isFixing = true,
                    isFixFinished = false,
                    fixProgress = 0f,
                    currentFixApp = app.appName,
                    fixLogs = listOf(FixLog(app.appName, app.packageName, "Bắt đầu hủy bỏ tất cả cấu hình cho ${app.appName}..."))
                )
            }

            val newStatus = repository.revokeAllPermissions(app) { log ->
                _uiState.update { state ->
                    state.copy(fixLogs = state.fixLogs + log)
                }
            }

            _uiState.update {
                it.copy(
                    isFixing = true,
                    isFixFinished = true,
                    fixProgress = 1f,
                    detailStatus = newStatus,
                    appList = updateAppDetailInList(it.appList, app.packageName, newStatus)
                )
            }
        }
    }

    fun openAppSettings(packageName: String) {
        viewModelScope.launch {
            repository.openAppSettings(packageName)
        }
    }

    fun fixSelectedApps() {
        val selectedApps = _uiState.value.appList.filter { it.isSelected }
        if (selectedApps.isEmpty()) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isFixing = true,
                    isFixFinished = false,
                    fixProgress = 0f,
                    currentFixApp = "",
                    fixLogs = listOf(FixLog("Hệ thống", "system", "Khởi tạo tiến trình Fix cho ${selectedApps.size} ứng dụng..."))
                )
            }

            val total = selectedApps.size
            var currentAppList = _uiState.value.appList

            selectedApps.forEachIndexed { index, app ->
                _uiState.update {
                    it.copy(
                        currentFixApp = app.appName,
                        fixProgress = (index + 1).toFloat() / total
                    )
                }

                val newStatus = repository.fixApp(app) { log ->
                    _uiState.update { state ->
                        state.copy(fixLogs = state.fixLogs + log)
                    }
                }

                currentAppList = updateAppDetailInList(currentAppList, app.packageName, newStatus)
                _uiState.update { it.copy(appList = currentAppList) }
            }

            _uiState.update {
                it.copy(
                    fixProgress = 1f,
                    isFixFinished = true,
                    fixLogs = it.fixLogs + FixLog("Hệ thống", "system", "🎉 Hoàn tất sửa chữa toàn bộ ứng dụng đã chọn!", isSuccess = true)
                )
            }
        }
    }

    fun closeFixProgressDialog() {
        _uiState.update { it.copy(isFixing = false, fixLogs = emptyList(), isFixFinished = false) }
    }

    fun openGcmDiagnostics() {
        viewModelScope.launch {
            repository.openGcmDiagnostics()
        }
    }

    private fun updateAppDetailInList(
        list: List<AppInfo>,
        packageName: String,
        status: AppDetailStatus
    ): List<AppInfo> {
        return list.map {
            if (it.packageName == packageName) it.copy(detailStatus = status) else it
        }
    }

    companion object {
        fun getFilteredApps(apps: List<AppInfo>, query: String): List<AppInfo> {
            if (query.isBlank()) return apps
            val q = query.trim().lowercase()
            return apps.filter {
                it.appName.lowercase().contains(q) || it.packageName.lowercase().contains(q)
            }
        }
    }
}
