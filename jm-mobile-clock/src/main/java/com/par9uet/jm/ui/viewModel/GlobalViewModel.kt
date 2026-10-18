package com.par9uet.jm.ui.viewModel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.par9uet.jm.store.InitManager
import com.par9uet.jm.task.AppInitTask
import com.par9uet.jm.utils.log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GlobalViewModel(
    private val appInitTaskList: List<AppInitTask>,
    private val initManager: InitManager
) : ViewModel() {
    private val _initialized = MutableStateFlow(false)
    val initialized = _initialized.asStateFlow()
    private var initStarted = false

    @Synchronized
    fun init() {
        if (initStarted) return
        initStarted = true
        viewModelScope.launch {
            initManager.initialize(appInitTaskList)
            _initialized.value = true
            runCatching { log("全局初始化", "完成全局初始化") }
        }
    }
}
