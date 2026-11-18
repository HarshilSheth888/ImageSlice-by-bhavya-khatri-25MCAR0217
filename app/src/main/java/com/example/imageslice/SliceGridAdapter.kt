package com.example.imageslice

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView

class SliceGridAdapter(
    private val context: Context,
    private val slices: List<SliceItem>,
    private val onSliceClicked: (SliceItem) -> Unit,
) : BaseAdapter() {

    override fun getCount(): Int = slices.size

    override fun getItem(position: Int): SliceItem = slices[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view: View = convertView ?: LayoutInflater.from(context).inflate(
            R.layout.item_slice_grid, parent, false
        )

        val item = getItem(position)
        val ivThumbnail = view.findViewById<ImageView>(R.id.ivSliceThumbnail)
        val tvSliceIndex = view.findViewById<TextView>(R.id.tvSliceIndex)
        val tvSliceCoords = view.findViewById<TextView>(R.id.tvSliceCoords)

        ivThumbnail.setImageBitmap(item.bitmap)
        tvSliceIndex.text = context.getString(
            R.string.section_number,
            item.index,
            item.totalRows * item.totalCols
        )
        tvSliceCoords.text = context.getString(
            R.string.position_info,
            item.row + 1,
            item.totalRows,
            item.col + 1,
            item.totalCols
        )

        view.setOnClickListener {
            onSliceClicked(item)
        }

        return view
    }
}
