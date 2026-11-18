package com.example.imageslice

import android.graphics.Bitmap

/**
 * Data class representing an individual slice of the image.
 */
data class SliceItem(
    val bitmap: Bitmap,
    val row: Int,
    val col: Int,
    val totalRows: Int,
    val totalCols: Int,
    val index: Int
)
