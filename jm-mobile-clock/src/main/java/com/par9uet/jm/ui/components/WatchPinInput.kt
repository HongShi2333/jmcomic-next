package com.par9uet.jm.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.abs

/** Familiar 3-by-4 keypad; all ten digits and delete are visible without swiping. */
@Composable
fun WatchPinInput(
    title: String, length: Int, entered: Int, isError: Boolean,
    onDigit: (Int) -> Unit, onDelete: () -> Unit,
    modifier: Modifier = Modifier, onCancel: (() -> Unit)? = null, onOptions: (() -> Unit)? = null,
) {
    Surface(modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val w = maxWidth
            val h = maxHeight
            val keySize = minOf(w * .22f, h * .18f, 48.dp)
            Text(
                if (isError) "密码错误，请重试" else title,
                modifier = Modifier.align(Alignment.TopCenter).offset(y = h * .07f).width(w * .62f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis,
                color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            )
            Row(
                Modifier.align(Alignment.TopCenter).offset(y = h * .185f)
                    .semantics { contentDescription = "已输入 $entered / $length 位密码" },
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                repeat(length.coerceIn(4, 8)) { index ->
                    Box(Modifier.size(4.dp).background(
                        if (index < entered) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                        CircleShape,
                    ))
                }
            }
            for (row in 0..3) {
                for (column in 0..2) {
                    val digit = if (row < 3) row * 3 + column + 1 else if (column == 1) 0 else null
                    val secondary = onCancel ?: onOptions
                    val enabled = digit != null || (row == 3 && column == 2 && (entered > 0 || isError)) ||
                        (row == 3 && column == 0 && secondary != null)
                    val centerX = w * (.5f + (column - 1) * if (row == 3) .18f else .23f)
                    val centerY = h * (.32f + row * .175f)
                    val interaction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                    Surface(
                        onClick = {
                            when {
                                digit != null -> onDigit(digit)
                                column == 2 -> onDelete()
                                else -> secondary?.invoke()
                            }
                        },
                        enabled = enabled,
                        interactionSource = interaction,
                        modifier = Modifier.offset(x = centerX - keySize / 2, y = centerY - keySize / 2)
                            .size(keySize).watchPressFeedback(interaction),
                        shape = CircleShape,
                        color = if (digit != null) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.background,
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            if (digit != null) Text(
                                digit.toString(), fontSize = 19.sp, lineHeight = 22.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.semantics { contentDescription = "数字 $digit" },
                            ) else if (column == 2) Icon(Icons.AutoMirrored.Rounded.Backspace,
                                contentDescription = "删除上一位", modifier = Modifier.size(18.dp))
                            else if (secondary != null) Icon(
                                if (onCancel != null) Icons.Rounded.Close else Icons.Rounded.Menu,
                                contentDescription = if (onCancel != null) "取消设置" else "切换验证方式",
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Pattern dots live in the middle/lower dial; no phone card or nested scrolling. */
@Composable
fun WatchPatternInputPage(title: String, onHeaderClick: (() -> Unit)? = null, content: @Composable () -> Unit) {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val side = minOf(maxWidth, maxHeight) * .76f
            Box(Modifier.align(Alignment.TopCenter).offset(y = maxHeight * .55f - side / 2).size(side), contentAlignment = Alignment.Center) { content() }
            if (onHeaderClick != null) TextButton(onClick = onHeaderClick, modifier = Modifier.align(Alignment.TopCenter).offset(y = 2.dp).width(maxWidth * .68f).height(32.dp)) {
                Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelLarge)
            } else Text(title, modifier = Modifier.align(Alignment.TopCenter).offset(y = maxHeight * .065f).width(maxWidth * .68f), textAlign = androidx.compose.ui.text.style.TextAlign.Center, style = MaterialTheme.typography.labelMedium, maxLines = 1)
        }
    }
}
