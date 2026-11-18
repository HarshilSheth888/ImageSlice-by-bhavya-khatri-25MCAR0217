package com.example.imageslice

import android.graphics.Bitmap

/**
 * Singleton holder for active Bitmaps and slices to avoid Binder IPC transaction size limitations.
 */
object ImageStorage {
    var sourceBitmap: Bitmap? = null
    var currentSliceBitmap: Bitmap? = null
    var allSlices: List<SliceItem> = emptyList()
}
