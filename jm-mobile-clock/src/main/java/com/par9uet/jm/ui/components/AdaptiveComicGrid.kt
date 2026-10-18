package com.par9uet.jm.ui.components

import androidx.compose.foundation.lazy.grid.GridCells

/** The watch uses readable horizontal rows, never phone-sized two-column cover tiles. */
fun adaptiveComicGridCells(columns: Int = 0): GridCells = GridCells.Fixed(1)
