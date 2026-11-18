package com.example.imageslice

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton

class SliceDetailActivity : AppCompatActivity() {

    private var sliceBitmap: Bitmap? = null
    private var row: Int = 0
    private var col: Int = 0
    private var totalRows: Int = 3
    private var totalCols: Int = 3
    private var index: Int = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_slice_detail)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.rootSliceDetail)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbarDetail)
        toolbar.setNavigationOnClickListener {
            finish()
        }

        row = intent.getIntExtra(EXTRA_ROW, 0)
        col = intent.getIntExtra(EXTRA_COL, 0)
        totalRows = intent.getIntExtra(EXTRA_TOTAL_ROWS, 3)
        totalCols = intent.getIntExtra(EXTRA_TOTAL_COLS, 3)
        index = intent.getIntExtra(EXTRA_INDEX, (row * totalCols) + col + 1)

        sliceBitmap = ImageStorage.currentSliceBitmap

        val ivDetailSlice = findViewById<ImageView>(R.id.ivDetailSlice)
        val tvDetailSectionBadge = findViewById<TextView>(R.id.tvDetailSectionBadge)
        val tvDetailPosition = findViewById<TextView>(R.id.tvDetailPosition)
        val tvDetailResolution = findViewById<TextView>(R.id.tvDetailResolution)
        val btnShare = findViewById<MaterialButton>(R.id.btnShareSlice)
        val btnSave = findViewById<MaterialButton>(R.id.btnSaveSlice)

        sliceBitmap?.let { bmp ->
            ivDetailSlice.setImageBitmap(bmp)
            tvDetailResolution.text = getString(R.string.dimensions_info, bmp.width, bmp.height)
        } ?: run {
            tvDetailResolution.text = getString(R.string.bitmap_unavailable)
        }

        tvDetailSectionBadge.text = getString(R.string.section_number, index, totalRows * totalCols)
        tvDetailPosition.text = getString(
            R.string.position_info,
            row + 1,
            totalRows,
            col + 1,
            totalCols,
        )

        btnShare.setOnClickListener {
            shareSlice()
        }

        btnSave.setOnClickListener {
            saveSlice()
        }
    }

    private fun shareSlice() {
        val bmp = sliceBitmap ?: return
        val file = ImageProcessingHelper.saveBitmapToCache(this, bmp, "slice_${row + 1}_${col + 1}.png")
        if (file != null) {
            try {
                val uri = FileProvider.getUriForFile(
                    this,
                    "${applicationContext.packageName}.fileprovider",
                    file,
                )
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(
                        Intent.EXTRA_TEXT,
                        "ImageSlice: Section #$index (Row ${row + 1}, Col ${col + 1})",
                    )
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(Intent.createChooser(shareIntent, getString(R.string.share_slice)))
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(
                    this,
                    getString(R.string.share_error, e.message ?: ""),
                    Toast.LENGTH_SHORT,
                ).show()
            }
        } else {
            Toast.makeText(this, R.string.share_prepare_failed, Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveSlice() {
        val bmp = sliceBitmap ?: return
        val success = ImageProcessingHelper.saveBitmapToGallery(
            this,
            bmp,
            "ImageSlice_${System.currentTimeMillis()}_R${row + 1}_C${col + 1}",
        )
        if (success) {
            Toast.makeText(this, R.string.saved_successfully, Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, R.string.save_failed, Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        const val EXTRA_ROW = "extra_row"
        const val EXTRA_COL = "extra_col"
        const val EXTRA_TOTAL_ROWS = "extra_total_rows"
        const val EXTRA_TOTAL_COLS = "extra_total_cols"
        const val EXTRA_INDEX = "extra_index"
    }
}
