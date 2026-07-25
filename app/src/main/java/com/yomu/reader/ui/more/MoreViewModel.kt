package com.yomu.reader.ui.more

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yomu.reader.update.UpdateInfo
import com.yomu.reader.update.UpdateManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class UpdateUiState(
    val checking: Boolean = false,
    val checked: Boolean = false,
    val update: UpdateInfo? = null,
    val downloading: Boolean = false,
    val error: String? = null,
)

class MoreViewModel(private val updateManager: UpdateManager) : ViewModel() {

    private val _update = MutableStateFlow(UpdateUiState())
    val update: StateFlow<UpdateUiState> = _update.asStateFlow()

    init {
        checkForUpdate()
    }

    fun checkForUpdate() {
        if (_update.value.checking) return
        viewModelScope.launch {
            _update.value = _update.value.copy(checking = true, error = null)
            try {
                val info = updateManager.check()
                _update.value = _update.value.copy(checking = false, checked = true, update = info)
            } catch (e: Exception) {
                _update.value = _update.value.copy(checking = false, checked = true, error = e.message ?: "Check failed")
            }
        }
    }

    fun install() {
        val info = _update.value.update ?: return
        viewModelScope.launch {
            _update.value = _update.value.copy(downloading = true, error = null)
            try {
                updateManager.downloadAndInstall(info)
            } catch (e: Exception) {
                _update.value = _update.value.copy(error = e.message ?: "Download failed")
            } finally {
                _update.value = _update.value.copy(downloading = false)
            }
        }
    }
}
