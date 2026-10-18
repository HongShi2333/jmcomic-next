package com.par9uet.jm.ui

import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.core.view.doOnPreDraw
import androidx.lifecycle.Lifecycle

/**
 * A separate window covers Compose dialogs too. FLAG_SECURE is set before
 * content is drawn: onPause alone can be too late for the task snapshot.
 * The original composition stays alive, preserving downloads and editor drafts.
 */
internal class BackgroundPrivacyCover(private val activity: Activity) {
    private var cover: Dialog? = null
    private var generation = 0
    internal val isShowing: Boolean get() = cover?.isShowing == true

    init {
        activity.window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            activity.setRecentsScreenshotEnabled(false)
        }
    }

    fun show() {
        generation++
        if (activity.isFinishing || activity.isDestroyed || isShowing) return
        val dialog = cover ?: Dialog(activity, android.R.style.Theme_Material_NoActionBar).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setCancelable(false)
            setContentView(View(activity).apply {
                setBackgroundColor(Color.BLACK)
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
            })
            window?.apply {
                setBackgroundDrawable(ColorDrawable(Color.BLACK))
                addFlags(WindowManager.LayoutParams.FLAG_SECURE or
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN)
                clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                setWindowAnimations(0)
                statusBarColor = Color.BLACK
                navigationBarColor = Color.BLACK
            }
        }.also { cover = it }
        dialog.show()
        dialog.window?.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
    }

    fun revealAfterResume(lifecycle: Lifecycle) {
        val expectedGeneration = ++generation
        // Let the app-lock observer recompose before removing the opaque cover.
        activity.window.decorView.doOnPreDraw {
            if (generation == expectedGeneration && lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                cover?.dismiss()
            }
        }
        activity.window.decorView.invalidate()
    }

    fun dispose() {
        generation++
        cover?.dismiss()
        cover = null
    }
}
