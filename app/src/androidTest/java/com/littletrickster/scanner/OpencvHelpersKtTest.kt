package com.littletrickster.scanner

import org.junit.Assert
import org.junit.Test
import org.opencv.android.OpenCVLoader
import org.opencv.core.Point


class OpencvHelpersKtTest {

    @Test
    fun rotationTest() {
        OpenCVLoader.initDebug()

        val list = listOf(Point(3.0, 1.0))

//        list.rotate(90,Point(1.5,3.5))

        list.rotate(90, Point(1.5, 1.0))


        val first = list.first()

        Assert.assertEquals(1.0, first.x, 0.1)
        Assert.assertEquals(3.0, first.y, 0.1)

    }
}