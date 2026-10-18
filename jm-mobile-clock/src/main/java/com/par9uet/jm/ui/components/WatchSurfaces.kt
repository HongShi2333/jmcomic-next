package com.par9uet.jm.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

private fun watchDialogProperties(properties: DialogProperties) = DialogProperties(
    dismissOnBackPress = properties.dismissOnBackPress,
    dismissOnClickOutside = properties.dismissOnClickOutside,
    securePolicy = properties.securePolicy,
    usePlatformDefaultWidth = false,
    decorFitsSystemWindows = false,
)

/** Full-screen, vertically ordered confirmation. Back/security policy is never weakened. */
@Composable
fun WatchAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: @Composable (() -> Unit)? = null,
    icon: @Composable (() -> Unit)? = null,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    shape: Shape = MaterialTheme.shapes.large,
    containerColor: Color = MaterialTheme.colorScheme.background,
    iconContentColor: Color = MaterialTheme.colorScheme.primary,
    titleContentColor: Color = MaterialTheme.colorScheme.onSurface,
    textContentColor: Color = MaterialTheme.colorScheme.onSurface,
    tonalElevation: Dp = 0.dp,
    properties: DialogProperties = DialogProperties(),
) {
    Dialog(onDismissRequest, properties = watchDialogProperties(properties)) {
        Surface(Modifier.fillMaxSize().then(modifier).clip(if (watchIsRound()) CircleShape else shape), color = containerColor) {
            WatchDialogContent(title = title, text = text, icon = icon, confirmButton = confirmButton, dismissButton = dismissButton)

        }
    }
}

/** For existing editors with their own bounded scrolling. No phone minimum width. */
@Composable
fun WatchBasicAlertDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    properties: DialogProperties = DialogProperties(),
    content: @Composable () -> Unit,
) {
    Dialog(onDismissRequest, properties = watchDialogProperties(properties)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Box(Modifier.fillMaxSize().imePadding().padding(horizontal = 12.dp,
                vertical = if (watchIsRound()) 24.dp else 8.dp).then(modifier)) { content() }
        }
    }
}

/** Watch sheets are full-screen pages, never a phone half-sheet plus drag handle. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchModalBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(),
    containerColor: Color = MaterialTheme.colorScheme.background,
    dragHandle: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(onDismissRequest, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Surface(Modifier.fillMaxSize(), color = containerColor) {
            Column(Modifier.fillMaxSize().imePadding().padding(horizontal = 12.dp,
                vertical = if (watchIsRound()) 24.dp else 8.dp).then(modifier), content = content)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchTopAppBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors(),
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    Row(Modifier.fillMaxWidth().padding(top = if (watchIsRound()) 8.dp else 0.dp,
        start = if (watchIsRound()) 24.dp else 8.dp, end = if (watchIsRound()) 24.dp else 8.dp)
        .heightIn(min = 48.dp).then(modifier), verticalAlignment = Alignment.CenterVertically) {
        navigationIcon()
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { ProvideTextStyle(MaterialTheme.typography.titleSmall) { title() } }
        actions()
    }
}

@Composable
fun WatchPageScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    Surface(Modifier.fillMaxSize().clip(if (watchIsRound()) CircleShape else MaterialTheme.shapes.extraSmall)) {
        Scaffold(modifier = modifier.fillMaxSize(), topBar = topBar, bottomBar = bottomBar,
            containerColor = MaterialTheme.colorScheme.background, contentWindowInsets = WindowInsets(0), content = content)
    }
}

/** The real dialog body, also rendered by the watch layout previews without a second window. */
@Composable
fun WatchDialogContent(
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    icon: @Composable (() -> Unit)? = null,
    dismissButton: @Composable (() -> Unit)? = null,
) {
            WatchLazyColumn(modifier.fillMaxSize().imePadding(), edgeStartPadding = false, contentPadding = PaddingValues(top = if (watchIsRound()) 24.dp else 8.dp)) {
                if (icon != null) item { Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { icon() } }
                if (title != null) item { ProvideTextStyle(MaterialTheme.typography.titleSmall) {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { title() }
                } }
                if (text != null) item { ProvideTextStyle(MaterialTheme.typography.bodyMedium) { text() } }
                item { Box(Modifier.fillMaxWidth().heightIn(min = 48.dp), contentAlignment = Alignment.Center) { confirmButton() } }
                if (dismissButton != null) item {
                    Box(Modifier.fillMaxWidth().heightIn(min = 48.dp), contentAlignment = Alignment.Center) { dismissButton() }
                }
            }
}
