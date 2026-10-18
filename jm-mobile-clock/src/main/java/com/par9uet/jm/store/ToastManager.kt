package com.par9uet.jm.store

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class ToastManager {
    private val _message = MutableSharedFlow<String>()
    val message = _message.asSharedFlow()

    fun showAsync(text: String) {
        CoroutineScope(Dispatchers.Main).launch {
            _message.emit(text)
        }
    }
}