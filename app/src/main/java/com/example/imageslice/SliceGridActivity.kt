package com.example.imageslice

import android.content.Intent
import android.os.Bundle
import android.widget.GridView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.appbar.MaterialToolbar

class SliceGridActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_slice_grid)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.rootSliceGrid)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbarGrid)
        toolbar.setNavigationOnClickListener {
            finish()
        }

        val gridView = findViewById<GridView>(R.id.gridViewSlices)
        val tvSummary = findViewById<TextView>(R.id.tvGridSummary)

        val totalRows = intent.getIntExtra(EXTRA_TOTAL_ROWS, 3)
        val totalCols = intent.getIntExtra(EXTRA_TOTAL_COLS, 3)

        gridView.numColumns = totalCols

        val slices = ImageStorage.allSlices
        tvSummary.text = getString(
            R.string.grid_summary_format,
            totalRows,
            totalCols,
            slices.size,
        )

        val adapter = SliceGridAdapter(this, slices) { selectedItem ->
            ImageStorage.currentSliceBitmap = selectedItem.bitmap

            val intent = Intent(this, SliceDetailActivity::class.java).apply {
                putExtra(SliceDetailActivity.EXTRA_ROW, selectedItem.row)
                putExtra(SliceDetailActivity.EXTRA_COL, selectedItem.col)
                putExtra(SliceDetailActivity.EXTRA_TOTAL_ROWS, selectedItem.totalRows)
                putExtra(SliceDetailActivity.EXTRA_TOTAL_COLS, selectedItem.totalCols)
                putExtra(SliceDetailActivity.EXTRA_INDEX, selectedItem.index)
            }
            startActivity(intent)
        }

        gridView.adapter = adapter
    }

    companion object {
        const val EXTRA_TOTAL_ROWS = "extra_total_rows"
        const val EXTRA_TOTAL_COLS = "extra_total_cols"
    }
}
