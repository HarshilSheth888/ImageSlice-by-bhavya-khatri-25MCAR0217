package com.example.imageslice

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

object ImageProcessingHelper {

    /**
     * Crops a specific grid section from the given bitmap using exact coordinates.
     */
    fun cropSection(
        bitmap: Bitmap,
        row: Int,
        col: Int,
        totalRows: Int,
        totalCols: Int,
    ): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        val startX = (col * width) / totalCols
        val startY = (row * height) / totalRows
        val endX = ((col + 1) * width) / totalCols
        val endY = ((row + 1) * height) / totalRows

        val cropWidth = (endX - startX).coerceAtLeast(1)
        val cropHeight = (endY - startY).coerceAtLeast(1)

        return Bitmap.createBitmap(bitmap, startX, startY, cropWidth, cropHeight)
    }

    /**
     * Slices the entire bitmap into all row * col grid sections.
     */
    fun sliceAll(
        bitmap: Bitmap,
        totalRows: Int,
        totalCols: Int,
    ): List<SliceItem> {
        val slices = mutableListOf<SliceItem>()
        var index = 1
        for (r in 0 until totalRows) {
            for (c in 0 until totalCols) {
                val cropped = cropSection(bitmap, r, c, totalRows, totalCols)
                slices.add(SliceItem(cropped, r, c, totalRows, totalCols, index++))
            }
        }
        return slices
    }

    /**
     * Safely decodes a bitmap from a content Uri, handling downsampling and EXIF orientation.
     */
    fun loadBitmapFromUri(context: Context, uri: Uri, maxDimension: Int = 2048): Bitmap? {
        return try {
            // Read bounds first
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }

            // Calculate sample size
            var inSampleSize = 1
            var w = options.outWidth
            var h = options.outHeight
            while ((w > maxDimension) || (h > maxDimension)) {
                w /= 2
                h /= 2
                inSampleSize *= 2
            }

            // Decode scaled bitmap
            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
            }
            val sampledBitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            } ?: return null

            // Handle EXIF orientation
            val orientation = try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val exif = ExifInterface(stream)
                    exif.getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL,
                    )
                } ?: ExifInterface.ORIENTATION_NORMAL
            } catch (_: Exception) {
                ExifInterface.ORIENTATION_NORMAL
            }

            rotateBitmapIfNeeded(sampledBitmap, orientation)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun rotateBitmapIfNeeded(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            else -> return bitmap
        }
        return try {
            val rotated = Bitmap.createBitmap(
                bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true,
            )
            if (rotated != bitmap) {
                bitmap.recycle()
            }
            rotated
        } catch (_: Exception) {
            bitmap
        }
    }

    /**
     * Converts any Drawable (vector or bitmap) to a Bitmap.
     */
    fun drawableToBitmap(drawable: Drawable, targetWidth: Int = 800, targetHeight: Int = 600): Bitmap {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            return drawable.bitmap
        }

        val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else targetWidth
        val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else targetHeight

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    /**
     * Saves a bitmap to the app cache directory for intent sharing.
     */
    fun saveBitmapToCache(context: Context, bitmap: Bitmap, fileName: String = "temp_slice.png"): File? {
        return try {
            val cachePath = File(context.cacheDir, "slices")
            if (!cachePath.exists()) {
                cachePath.mkdirs()
            }
            val file = File(cachePath, fileName)
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Saves a bitmap to the device's Pictures directory using MediaStore on API 29+ or external files.
     */
    fun saveBitmapToGallery(context: Context, bitmap: Bitmap, title: String): Boolean {
        var outputStream: OutputStream? = null
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, "$title.png")
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                    put(
                        MediaStore.MediaColumns.RELATIVE_PATH,
                        Environment.DIRECTORY_PICTURES + File.separator + "ImageSlice",
                    )
                }
                val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                outputStream = imageUri?.let { resolver.openOutputStream(it) }
            } else {
                val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val appDir = File(picturesDir, "ImageSlice")
                if (!appDir.exists()) {
                    appDir.mkdirs()
                }
                val file = File(appDir, "$title.png")
                outputStream = FileOutputStream(file)
            }

            outputStream?.let {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                it.flush()
                true
            } ?: false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            outputStream?.close()
        }
    }
}
