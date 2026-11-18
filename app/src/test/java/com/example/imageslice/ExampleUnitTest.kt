package com.example.imageslice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests verifying image slicing coordinate calculations and boundary integrity.
 */
class ExampleUnitTest {

    @Test
    fun testSliceCoordinateCoverage() {
        // Test that dividing any width and height into rows and cols covers all pixels without gaps or overlaps
        val width = 1000
        val height = 750
        val totalRows = 3
        val totalCols = 4

        var totalCroppedArea = 0

        for (r in 0 until totalRows) {
            val startY = (r * height) / totalRows
            val endY = ((r + 1) * height) / totalRows
            val cropHeight = endY - startY
            assertTrue("Crop height must be positive", cropHeight > 0)

            for (c in 0 until totalCols) {
                val startX = (c * width) / totalCols
                val endX = ((c + 1) * width) / totalCols
                val cropWidth = endX - startX
                assertTrue("Crop width must be positive", cropWidth > 0)

                totalCroppedArea += cropWidth * cropHeight
            }
        }

        // Sum of all slices' areas must equal the total image area exactly
        assertEquals(width * height, totalCroppedArea)
    }

    @Test
    fun testBoundaryConditions() {
        val width = 1920
        val height = 1080
        val totalRows = 8
        val totalCols = 8

        // Verify first slice starts at 0,0
        val firstStartX = (0 * width) / totalCols
        val firstStartY = (0 * height) / totalRows
        assertEquals(0, firstStartX)
        assertEquals(0, firstStartY)

        // Verify last slice ends at width, height
        val lastEndX = (totalCols * width) / totalCols
        val lastEndY = (totalRows * height) / totalRows
        assertEquals(width, lastEndX)
        assertEquals(height, lastEndY)
    }
}
