package com.par9uet.jm.utils

import android.view.KeyEvent

/** Routes hardware volume keys to the active reader, when one has registered. */
object VolumeKeyPageTurnDispatcher {
    private val lock = Any()
    private var handler: ((Boolean) -> Unit)? = null
    private var handlerToken: Any? = null

    fun register(onVolumeKey: (isVolumeUp: Boolean) -> Unit): () -> Unit {
        val token = Any()
        synchronized(lock) {
            handler = onVolumeKey
            handlerToken = token
        }
        return {
            synchronized(lock) {
                if (handlerToken === token) {
                    handler = null
                    handlerToken = null
                }
            }
        }
    }

    fun handle(event: KeyEvent): Boolean {
        val isVolumeKey = event.keyCode == KeyEvent.KEYCODE_VOLUME_UP ||
            event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN
        if (!isVolumeKey) return false

        val currentHandler = synchronized(lock) { handler }
        if (currentHandler == null) return false

        if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
            currentHandler(event.keyCode == KeyEvent.KEYCODE_VOLUME_UP)
        }
        // Consume both key actions while reading so the system volume is unchanged.
        return true
    }
}
