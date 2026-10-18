package com.par9uet.jm.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.par9uet.jm.store.ToastManager
import com.par9uet.jm.utils.copyComicImage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.getKoin

/** Observe taps without consuming them; only a confirmed long press owns the gesture. */
@Composable
fun Modifier.comicLongPress(label: String, action: () -> Unit): Modifier {
    val latestAction by rememberUpdatedState(action)
    return semantics { onLongClick(label) { latestAction(); true } }
        .pointerInput(Unit) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                val held = awaitLongPressOrCancellation(down.id)
                if (held != null && currentEvent.changes.count { it.pressed } == 1) {
                    latestAction()
                    do {
                        val event = awaitPointerEvent()
                        event.changes.forEach { it.consume() }
                    } while (event.changes.any { it.pressed })
                }
            }
        }
}

@Composable
fun Modifier.copyComicText(text: String): Modifier {
    val context = LocalContext.current
    val toast: ToastManager = getKoin().get()
    return comicLongPress("复制$text") {
        (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
            .setPrimaryClip(ClipData.newPlainText("漫画信息", text))
        toast.showAsync("已复制")
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CopyableComicAuthor(author: String, onClick: () -> Unit) {
    val context = LocalContext.current
    val toast: ToastManager = getKoin().get()
    Surface(
        modifier = Modifier.combinedClickable(
            onClick = onClick,
            onLongClickLabel = "复制作者",
            onLongClick = {
                (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                    .setPrimaryClip(ClipData.newPlainText("作者", author))
                toast.showAsync("已复制作者")
            },
        ),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Text(author, Modifier.padding(horizontal = 12.dp, vertical = 10.dp), style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun Modifier.copyComicCover(label: String, bitmap: () -> Bitmap?): Modifier {
    val context = LocalContext.current
    val toast: ToastManager = getKoin().get()
    val scope = rememberCoroutineScope()
    var copying by remember { mutableStateOf(false) }
    return comicLongPress("复制封面") {
        if (!copying) {
            copying = true
            scope.launch {
                try {
                    val image = withContext(Dispatchers.IO) { bitmap() }
                    if (image == null) toast.showAsync("封面尚未加载")
                    else {
                        copyComicImage(context, label, image)
                        toast.showAsync("已复制封面图片")
                    }
                } catch (error: CancellationException) {
                    throw error
                } catch (_: OutOfMemoryError) {
                    toast.showAsync("内存不足，暂时无法复制图片")
                } catch (error: Exception) {
                    toast.showAsync(error.message ?: "复制图片失败")
                } finally {
                    copying = false
                }
            }
        }
    }
}
