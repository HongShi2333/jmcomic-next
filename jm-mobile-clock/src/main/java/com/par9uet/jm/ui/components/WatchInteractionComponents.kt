package com.par9uet.jm.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/** A short, one-purpose watch page. The input method resizes the entire page, not one text field. */
@Composable
internal fun WatchPanelDialog(
    title: String,
    onDismiss: () -> Unit,
    content: LazyListScope.() -> Unit,
) {
    Dialog(onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        val keyboardVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
        CompositionLocalProvider(LocalWatchListActive provides true) {
            Box(Modifier.fillMaxSize().imePadding()) {
                WatchScaffold(title, onNavigateBack = onDismiss, showTopBar = !keyboardVisible) {
                    WatchLazyColumn(Modifier.fillMaxSize(), edgeScalingEnabled = false, content = content)
                }
            }
        }
    }
}

@Composable
internal fun WatchActionRow(
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    selected: Boolean? = null,
    onClick: () -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick,
        enabled = enabled,
        interactionSource = source,
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).watchPressFeedback(source)
            .semantics {
                if (selected != null) { this.selected = selected; role = Role.Checkbox }
            },
        shape = MaterialTheme.shapes.large,
        color = if (selected == true) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainer,
        contentColor = if (selected == true) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface,
    ) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (icon != null) Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium,
                    maxLines = 3, overflow = TextOverflow.Ellipsis)
                if (!subtitle.isNullOrBlank()) Text(subtitle, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            if (selected != false) Icon(
                if (selected == true) Icons.Rounded.Check else Icons.Rounded.ChevronRight,
                contentDescription = null, modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
internal fun WatchActionPair(
    first: String,
    second: String,
    firstEnabled: Boolean = true,
    secondEnabled: Boolean = true,
    onFirst: () -> Unit,
    onSecond: () -> Unit,
) {
    val firstSource = remember { MutableInteractionSource() }
    val secondSource = remember { MutableInteractionSource() }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        FilledTonalButton(onClick = onFirst, enabled = firstEnabled,
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
            modifier = Modifier.weight(1f).heightIn(min = 48.dp).watchPressFeedback(firstSource),
            interactionSource = firstSource,
        ) {
            Text(first, style = MaterialTheme.typography.labelMedium, maxLines = 2)
        }
        FilledTonalButton(onClick = onSecond, enabled = secondEnabled,
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
            modifier = Modifier.weight(1f).heightIn(min = 48.dp).watchPressFeedback(secondSource),
            interactionSource = secondSource,
        ) {
            Text(second, style = MaterialTheme.typography.labelMedium, maxLines = 2)
        }
    }
}

@Composable
internal fun WatchTextEditorDialog(
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    confirmLabel: String = "完成",
    confirmEnabled: Boolean = true,
    singleLine: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    hint: String? = null,
) {
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val canConfirm by rememberUpdatedState(confirmEnabled)
    val confirm by rememberUpdatedState(onConfirm)
    val commit = { if (canConfirm) { keyboard?.hide(); confirm() } }
    WatchPanelDialog(title, onDismiss) {
        item {
            // This editor is opened by an explicit tap. Request focus only once it is attached.
            LaunchedEffect(Unit) { withFrameNanos { }; focus.requestFocus() }
            OutlinedTextField(
                value = value, onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth().focusRequester(focus),
                label = { Text(title, style = MaterialTheme.typography.labelSmall) },
                singleLine = singleLine, minLines = if (singleLine) 1 else 2, maxLines = if (singleLine) 1 else 5,
                textStyle = MaterialTheme.typography.bodySmall,
                visualTransformation = if (keyboardType == KeyboardType.Password) {
                    androidx.compose.ui.text.input.PasswordVisualTransformation()
                } else androidx.compose.ui.text.input.VisualTransformation.None,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType,
                    imeAction = if (confirmLabel == "发送") ImeAction.Send else ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { commit() }, onSend = { commit() }),
            )
        }
        if (hint != null) item { Text(hint, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        item {
            val confirmSource = remember { MutableInteractionSource() }
            Button(
                onClick = commit,
                enabled = confirmEnabled,
                interactionSource = confirmSource,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).watchPressFeedback(confirmSource),
            ) {
                Text(confirmLabel)
            }
        }
    }
}
