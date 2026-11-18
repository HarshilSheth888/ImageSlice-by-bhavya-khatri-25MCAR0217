package com.example.imageslice

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton

class MainActivity : AppCompatActivity() {

    private lateinit var gridImageView: GridImageView
    private lateinit var tvRowsLabel: TextView
    private lateinit var tvColsLabel: TextView
    private lateinit var seekBarRows: SeekBar
    private lateinit var seekBarCols: SeekBar
    private lateinit var tvSelectionStatus: TextView
    private lateinit var btnViewSlice: MaterialButton
    private lateinit var btnViewAllGrid: MaterialButton

    private var currentBitmap: Bitmap? = null

    private val selectImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        uri?.let { loadUriImage(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initializeViews()
        setupListeners()
        loadDefaultSampleImage()
    }

    private fun initializeViews() {
        gridImageView = findViewById(R.id.gridImageView)
        tvRowsLabel = findViewById(R.id.tvRowsLabel)
        tvColsLabel = findViewById(R.id.tvColsLabel)
        seekBarRows = findViewById(R.id.seekBarRows)
        seekBarCols = findViewById(R.id.seekBarCols)
        tvSelectionStatus = findViewById(R.id.tvSelectionStatus)
        btnViewSlice = findViewById(R.id.btnViewSlice)
        btnViewAllGrid = findViewById(R.id.btnViewAllGrid)

        // Initial label states (progress = 2 -> 3 divisions)
        updateRowsLabel(gridImageView.rowCount)
        updateColsLabel(gridImageView.colCount)
    }

    private fun setupListeners() {
        // Image picker button
        findViewById<MaterialButton>(R.id.btnSelectImage).setOnClickListener {
            selectImageLauncher.launch("image/*")
        }

        // Load sample image button
        findViewById<MaterialButton>(R.id.btnLoadSample).setOnClickListener {
            loadDefaultSampleImage()
            Toast.makeText(this, R.string.sample_loaded, Toast.LENGTH_SHORT).show()
        }

        // Horizontal Divisions (Rows) SeekBar: 1 to 8 (progress 0 to 7)
        seekBarRows.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    val rows = progress + 1
                    gridImageView.rowCount = rows
                    updateRowsLabel(rows)
                    updateSelectionState()
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            },
        )

        // Vertical Divisions (Columns) SeekBar: 1 to 8 (progress 0 to 7)
        seekBarCols.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    val cols = progress + 1
                    gridImageView.colCount = cols
                    updateColsLabel(cols)
                    updateSelectionState()
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            }
        )

        // Box selection on touch
        gridImageView.onBoxSelectedListener = { row, col ->
            updateSelectionState(row, col)
        }

        // View Selected Slice button
        btnViewSlice.setOnClickListener {
            openSelectedSlice()
        }

        // View All Slices in GridView button
        btnViewAllGrid.setOnClickListener {
            openGridViewActivity()
        }
    }

    private fun updateRowsLabel(rows: Int) {
        tvRowsLabel.text = getString(R.string.rows_label, rows)
    }

    private fun updateColsLabel(cols: Int) {
        tvColsLabel.text = getString(R.string.cols_label, cols)
    }

    private fun updateSelectionState(
        row: Int? = gridImageView.selectedRow,
        col: Int? = gridImageView.selectedCol
    ) {
        if ((row != null) && (col != null) && (row in 0 until gridImageView.rowCount) && (col in 0 until gridImageView.colCount)) {
            val totalRows = gridImageView.rowCount
            val totalCols = gridImageView.colCount
            val sectionIndex = row * totalCols + col + 1
            val totalSections = totalRows * totalCols

            tvSelectionStatus.text = getString(
                R.string.selected_box_info,
                row + 1,
                col + 1,
                sectionIndex,
                totalSections
            )
            tvSelectionStatus.setTextColor(getColor(R.color.primary))
            btnViewSlice.isEnabled = true
        } else {
            tvSelectionStatus.text = getString(R.string.selection_hint)
            tvSelectionStatus.setTextColor(getColor(R.color.text_secondary))
            btnViewSlice.isEnabled = false
        }
    }

    private fun loadDefaultSampleImage() {
        val drawable = AppCompatResources.getDrawable(this, R.drawable.sample_image)
        if (drawable != null) {
            val bitmap = ImageProcessingHelper.drawableToBitmap(drawable, 800, 600)
            setBitmap(bitmap)
        }
    }

    private fun loadUriImage(uri: Uri) {
        val bitmap = ImageProcessingHelper.loadBitmapFromUri(this, uri, 2048)
        if (bitmap != null) {
            setBitmap(bitmap)
            Toast.makeText(this, R.string.image_loaded, Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, R.string.image_load_failed, Toast.LENGTH_SHORT).show()
        }
    }

    private fun setBitmap(bitmap: Bitmap) {
        currentBitmap = bitmap
        ImageStorage.sourceBitmap = bitmap
        gridImageView.setImageBitmap(bitmap)
        gridImageView.clearSelection()
        updateSelectionState()
    }

    private fun openSelectedSlice() {
        val bitmap = currentBitmap ?: return
        val row = gridImageView.selectedRow ?: return
        val col = gridImageView.selectedCol ?: return
        val totalRows = gridImageView.rowCount
        val totalCols = gridImageView.colCount

        val croppedSlice = ImageProcessingHelper.cropSection(bitmap, row, col, totalRows, totalCols)
        ImageStorage.currentSliceBitmap = croppedSlice

        val intent = Intent(this, SliceDetailActivity::class.java).apply {
            putExtra(SliceDetailActivity.EXTRA_ROW, row)
            putExtra(SliceDetailActivity.EXTRA_COL, col)
            putExtra(SliceDetailActivity.EXTRA_TOTAL_ROWS, totalRows)
            putExtra(SliceDetailActivity.EXTRA_TOTAL_COLS, totalCols)
            putExtra(SliceDetailActivity.EXTRA_INDEX, row * totalCols + col + 1)
        }
        startActivity(intent)
    }

    private fun openGridViewActivity() {
        val bitmap = currentBitmap ?: return
        val totalRows = gridImageView.rowCount
        val totalCols = gridImageView.colCount

        val slices = ImageProcessingHelper.sliceAll(bitmap, totalRows, totalCols)
        ImageStorage.allSlices = slices

        val intent = Intent(this, SliceGridActivity::class.java).apply {
            putExtra(SliceGridActivity.EXTRA_TOTAL_ROWS, totalRows)
            putExtra(SliceGridActivity.EXTRA_TOTAL_COLS, totalCols)
        }
        startActivity(intent)
    }
}
