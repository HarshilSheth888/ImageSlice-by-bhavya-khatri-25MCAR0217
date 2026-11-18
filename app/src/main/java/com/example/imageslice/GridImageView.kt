package com.example.imageslice

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatImageView
import kotlin.math.abs

/**
 * Custom ImageView that renders adjustable horizontal and vertical grid lines
 * over the displayed image and allows the user to touch/select individual grid boxes.
 */
class GridImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : AppCompatImageView(context, attrs, defStyleAttr) {

    var rowCount: Int = 3
        set(value) {
            field = value.coerceAtLeast(1)
            // Clamp selection if rows decreased
            if ((selectedRow != null) && (selectedRow!! >= field)) {
                selectedRow = null
                selectedCol = null
            }
            invalidate()
        }

    var colCount: Int = 3
        set(value) {
            field = value.coerceAtLeast(1)
            // Clamp selection if columns decreased
            if ((selectedCol != null) && (selectedCol!! >= field)) {
                selectedRow = null
                selectedCol = null
            }
            invalidate()
        }

    var selectedRow: Int? = null
        private set

    var selectedCol: Int? = null
        private set

    var onBoxSelectedListener: ((row: Int, col: Int) -> Unit)? = null

    // Paints
    private val gridLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = dpToPx(1.5f)
    }

    private val gridShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(153, 0, 0, 0) // 60% black for high contrast
        style = Paint.Style.STROKE
        strokeWidth = dpToPx(3f)
    }

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = dpToPx(2.5f)
    }

    private val selectionFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(100, 33, 150, 243) // Translucent Material Blue
        style = Paint.Style.FILL
    }

    private val selectionStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(255, 193, 7) // Material Amber
        style = Paint.Style.STROKE
        strokeWidth = dpToPx(3.5f)
    }

    private val labelTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = dpToPx(12f)
        textAlign = Paint.Align.CENTER
        setShadowLayer(dpToPx(2f), 1f, 1f, Color.BLACK)
    }

    private val labelBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(128, 0, 0, 0)
        style = Paint.Style.FILL
    }

    // Touch tracking
    private var downX = 0f
    private var downY = 0f
    private val touchSlop = dpToPx(8f)

    init {
        scaleType = ScaleType.FIT_CENTER
        isClickable = true
        isFocusable = true
    }

    /**
     * Programmatically select a grid box.
     */
    fun selectBox(row: Int, col: Int) {
        if (row in 0 until rowCount && col in 0 until colCount) {
            selectedRow = row
            selectedCol = col
            invalidate()
            onBoxSelectedListener?.invoke(row, col)
        }
    }

    /**
     * Clears any active box selection.
     */
    fun clearSelection() {
        selectedRow = null
        selectedCol = null
        invalidate()
    }

    /**
     * Calculates the exact rectangular bounds of the displayed image within this ImageView,
     * accounting for scaleType (primarily FIT_CENTER).
     */
    fun getDisplayedImageBounds(): RectF {
        val d = drawable ?: return RectF(
            paddingLeft.toFloat(),
            paddingTop.toFloat(),
            (width - paddingRight).toFloat(),
            (height - paddingBottom).toFloat(),
        )

        val viewWidth = (width - paddingLeft - paddingRight).toFloat()
        val viewHeight = (height - paddingTop - paddingBottom).toFloat()
        if (viewWidth <= 0f || viewHeight <= 0f) return RectF()

        val dWidth = d.intrinsicWidth.toFloat()
        val dHeight = d.intrinsicHeight.toFloat()
        if (dWidth <= 0f || dHeight <= 0f) {
            return RectF(
                paddingLeft.toFloat(),
                paddingTop.toFloat(),
                paddingLeft + viewWidth,
                paddingTop + viewHeight,
            )
        }

        val viewRatio = viewWidth / viewHeight
        val imageRatio = dWidth / dHeight

        val drawWidth: Float
        val drawHeight: Float

        if (imageRatio > viewRatio) {
            drawWidth = viewWidth
            drawHeight = drawWidth / imageRatio
        } else {
            drawHeight = viewHeight
            drawWidth = drawHeight * imageRatio
        }

        val drawLeft = paddingLeft + (viewWidth - drawWidth) / 2f
        val drawTop = paddingTop + (viewHeight - drawHeight) / 2f

        return RectF(drawLeft, drawTop, drawLeft + drawWidth, drawTop + drawHeight)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (drawable == null) return

        val imgRect = getDisplayedImageBounds()
        if (imgRect.width() <= 0f || imgRect.height() <= 0f) return

        val cellWidth = imgRect.width() / colCount
        val cellHeight = imgRect.height() / rowCount

        // 1. Draw Selected Box Highlight (if any)
        val sRow = selectedRow
        val sCol = selectedCol
        if (sRow != null && sCol != null && sRow in 0 until rowCount && sCol in 0 until colCount) {
            val boxLeft = imgRect.left + sCol * cellWidth
            val boxTop = imgRect.top + sRow * cellHeight
            val boxRight = boxLeft + cellWidth
            val boxBottom = boxTop + cellHeight

            // Fill selected box
            canvas.drawRect(boxLeft, boxTop, boxRight, boxBottom, selectionFillPaint)

            // Outline selected box with amber border
            canvas.drawRect(boxLeft, boxTop, boxRight, boxBottom, selectionStrokePaint)
        }

        // 2. Draw Horizontal Grid Lines
        for (r in 1 until rowCount) {
            val y = imgRect.top + r * cellHeight
            // Draw shadow for contrast
            canvas.drawLine(imgRect.left, y, imgRect.right, y, gridShadowPaint)
            // Draw crisp white line
            canvas.drawLine(imgRect.left, y, imgRect.right, y, gridLinePaint)
        }

        // 3. Draw Vertical Grid Lines
        for (c in 1 until colCount) {
            val x = imgRect.left + c * cellWidth
            // Draw shadow for contrast
            canvas.drawLine(x, imgRect.top, x, imgRect.bottom, gridShadowPaint)
            // Draw crisp white line
            canvas.drawLine(x, imgRect.top, x, imgRect.bottom, gridLinePaint)
        }

        // 4. Draw Outer Image Grid Border
        canvas.drawRect(imgRect, borderPaint)

        // 5. Draw Section Number Badges in each cell
        var sectionNum = 1
        for (r in 0 until rowCount) {
            for (c in 0 until colCount) {
                val cx = imgRect.left + c * cellWidth + cellWidth / 2f
                val cy = imgRect.top + r * cellHeight + cellHeight / 2f

                val isSelected = (r == sRow && c == sCol)
                val badgeText = sectionNum.toString()

                // Draw small badge pill background
                val badgeRadius = dpToPx(10f)
                canvas.drawCircle(cx, cy, badgeRadius, if (isSelected) selectionStrokePaint else labelBgPaint)

                // Draw text centered
                val textY = cy - (labelTextPaint.descent() + labelTextPaint.ascent()) / 2f
                canvas.drawText(badgeText, cx, textY, labelTextPaint)

                sectionNum++
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled || drawable == null) {
            return super.onTouchEvent(event)
        }

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downY = event.y
                return true
            }

            MotionEvent.ACTION_UP -> {
                val diffX = abs(event.x - downX)
                val diffY = abs(event.y - downY)

                if (diffX <= touchSlop && diffY <= touchSlop) {
                    val imgRect = getDisplayedImageBounds()
                    if (imgRect.contains(event.x, event.y)) {
                        val cellWidth = imgRect.width() / colCount
                        val cellHeight = imgRect.height() / rowCount

                        val col = ((event.x - imgRect.left) / cellWidth).toInt().coerceIn(0, colCount - 1)
                        val row = ((event.y - imgRect.top) / cellHeight).toInt().coerceIn(0, rowCount - 1)

                        selectedRow = row
                        selectedCol = col
                        invalidate()

                        performClick()
                        onBoxSelectedListener?.invoke(row, col)
                        return true
                    }
                }
            }
        }
        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    private fun dpToPx(dp: Float): Float {
        return dp * resources.displayMetrics.density
    }
}
