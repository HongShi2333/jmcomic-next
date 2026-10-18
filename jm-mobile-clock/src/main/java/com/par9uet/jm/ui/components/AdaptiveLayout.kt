package com.par9uet.jm.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Bounded nested lists/forms inside the full-screen watch dialog; never impose a 180dp minimum. */
@Composable
fun adaptiveDialogMaxHeight(max: Dp = 420.dp, reserved: Dp = 220.dp): Dp =
    minOf(max, (LocalConfiguration.current.screenHeightDp * .55f).dp).coerceAtLeast(48.dp)
