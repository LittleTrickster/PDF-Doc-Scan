package com.littletrickster.scanner

import org.junit.Assert.assertEquals
import org.junit.Test
import org.opencv.android.OpenCVLoader
import org.opencv.core.Point

class RotationTest {

    @Test
    fun rotationTest() {
        OpenCVLoader.initDebug()

        val points = listOf(Point(0.0, 0.0)).rotate(90, Point(1.0, 1.0))

        val point = points.first()
        assertEquals(2.0, point.x, 0.1)
        assertEquals(2.0, point.y, 0.1)

    }

}