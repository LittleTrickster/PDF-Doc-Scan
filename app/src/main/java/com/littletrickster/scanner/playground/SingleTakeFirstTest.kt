package com.littletrickster.scanner.playground

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.Slider
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.Dp
import com.littletrickster.scanner.*
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import org.opencv.ximgproc.Ximgproc
import kotlin.math.pow
import kotlin.math.sqrt

@Composable
fun ImageAndOtherStuff(originalMat: Mat) {


    val (originalBwResizedMat, scale) = remember(originalMat) {
        val bwMat = Mat()
        Imgproc.cvtColor(originalMat, bwMat, Imgproc.COLOR_RGB2GRAY)

        val resizedMat = Mat()
        val scale = bwMat.resizeMax(resizedMat, 500.0)
        Imgproc.GaussianBlur(bwMat, bwMat, Size(7.0, 7.0), 0.0)
        resizedMat to scale
    }

    var threshold1 by remember { mutableStateOf(1.0) }
    var threshold2 by remember { mutableStateOf(1.0) }

    val cannyMat = remember(originalBwResizedMat, threshold1, threshold2) {

        val nMat = Mat()
        Imgproc.Canny(originalBwResizedMat, nMat, threshold1, threshold2)
//        Imgproc.dilate(nMat, nMat,  Mat(),  Point(-1.0, -1.0), 1);
        nMat
    }


    val contourInfo = remember(cannyMat) {
        val list = ArrayList<MatOfPoint>()
        Imgproc.findContours(cannyMat, list, Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE)

        var maxVal = 0.0
        var maxValIdx = 0
        for (contourIdx in 0 until list.size) {
            val contourArea = Imgproc.contourArea(list[contourIdx])
            if (maxVal < contourArea) {
                maxVal = contourArea
                maxValIdx = contourIdx
            }
        }
        object {
            val contours = list
            val largest = maxValIdx
        }
    }


    val largest = remember(contourInfo) { contourInfo.contours[contourInfo.largest] }


    val withContours = remember(contourInfo) {
        val mat = Mat()
        Imgproc.cvtColor(cannyMat, mat, Imgproc.COLOR_GRAY2RGB)
        Imgproc.drawContours(mat, contourInfo.contours, contourInfo.largest, greenScalar, 5)
        mat
    }


    val points = remember(largest) {
        getCornersFromPoints(largest.toList())
    }


    val (bottomRight, topLeft, bottomLeft, topRight) = points

    val withDots = remember(withContours, points) {
        val mat = Mat()
        withContours.copyTo(mat)

        points.forEach {
            Imgproc.circle(mat, it, 1, redScalar, 20)
        }
        mat
    }


    val unwrapped = remember(points) {


        val x1 = topLeft.x * scale
        val x2 = topRight.x * scale
        val x3 = bottomLeft.x * scale
        val x4 = bottomRight.x * scale
        val y1 = topLeft.y * scale
        val y2 = topRight.y * scale
        val y3 = bottomLeft.y * scale
        val y4 = bottomRight.y * scale


        val w1 = sqrt((x4 - x3).pow(2.0) * 2)
        val w2 = sqrt((x2 - x1).pow(2.0) * 2)
        val h1 = sqrt((y2 - y4).pow(2.0) * 2)
        val h2 = sqrt((y1 - y3).pow(2.0) * 2)


        val maxWidth = (if (w1 < w2) w1 else w2).toInt()
        val maxHeight = (if (h1 < h2) h1 else h2).toInt()


        val dst = Mat.zeros(maxHeight, maxWidth, CvType.CV_8UC3)!!


        // corners of destination image with the sequence [tl, tr, bl, br]
        val dstPts: MutableList<Point> = ArrayList()
        val imgPts: MutableList<Point> = ArrayList()
        dstPts.add(Point(0.0, 0.0))
        dstPts.add(Point((maxWidth - 1).toDouble(), 0.0))
        dstPts.add(Point(0.0, (maxHeight - 1).toDouble()))
        dstPts.add(Point((maxWidth - 1).toDouble(), (maxHeight - 1).toDouble()))

        imgPts.add(Point(x1, y1))
        imgPts.add(Point(x2, y2))
        imgPts.add(Point(x3, y3))
        imgPts.add(Point(x4, y4))

        val mdst = MatOfPoint2f()
        mdst.fromList(dstPts)
        val mimg = MatOfPoint2f()
        mimg.fromList(imgPts)

        val pointMap = MatOfPoint2f()
        pointMap.fromList(points)
        // get transformation matrix

        val transmtx = Imgproc.getPerspectiveTransform(mimg, mdst)
        // apply perspective transformation
        Imgproc.warpPerspective(originalMat, dst, transmtx, dst.size())


        dst
    }


    //from ScanIt magic
    val paperUnwrapped1 = remember(unwrapped) {
        val mat = Mat()
        val alpha = 1.9f
        val beta = -80f
        unwrapped.copyTo(mat)

        unwrapped.convertTo(mat, -1, alpha.toDouble(), beta.toDouble())
        mat
    }

    val paperUnwrapped2 = remember(unwrapped) {
        val mat = Mat()
        Imgproc.cvtColor(unwrapped, mat, Imgproc.COLOR_RGB2GRAY)
        val result = Mat()
        Imgproc.adaptiveThreshold(
            mat,
            result,
            255.0,
            Imgproc.ADAPTIVE_THRESH_MEAN_C,
            Imgproc.THRESH_BINARY,
            41,
            40.0
        )
        result
    }


    val paperUnwrapped3 = remember(unwrapped) {
        val mat = Mat()
        Imgproc.cvtColor(unwrapped, mat, Imgproc.COLOR_RGB2GRAY)
        val result = Mat()
        Ximgproc.niBlackThreshold(
            mat,
            result,
            255.0,
            Imgproc.THRESH_BINARY,
            41,
            -0.15,
            Ximgproc.BINARIZATION_NICK
        )
        result
    }

    val paperUnwrapped4 = remember(paperUnwrapped1) {
        val mat = documentMat(paperUnwrapped1)
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGR2GRAY)
        mat
    }



    Slider(value = threshold1.toFloat(), valueRange = 1f..1000f, onValueChange = {
        threshold1 = it.toDouble()
    })
    Text("$threshold1")

    Slider(value = threshold2.toFloat(), valueRange = 1f..1000f, onValueChange = {
        threshold2 = it.toDouble()
    })
    Text("$threshold2")

    val scrollState = rememberScrollState()


    BoxWithConstraints {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {


            val original = remember(originalMat) { originalMat.toImageBitmap() }
            val originalBwResized = remember(originalBwResizedMat) {
                originalBwResizedMat.toImageBitmap()
            }
            val cannyBitmap = remember(cannyMat) { cannyMat.toImageBitmap() }
            val withContoursBitmap =
                remember(withContours) { withContours.toImageBitmap() }
            val dotBitmap = remember(withDots) { withDots.toImageBitmap() }
            val unwrappedBitmap = remember(unwrapped) { unwrapped.toImageBitmap() }
            val paperUnwrappedBitmap1 =
                remember(paperUnwrapped1) { paperUnwrapped1.toImageBitmap() }
            val paperUnwrappedBitmap2 =
                remember(paperUnwrapped2) { paperUnwrapped2.toImageBitmap() }
            val paperUnwrappedBitmap3 =
                remember(paperUnwrapped3) { paperUnwrapped3.toImageBitmap() }
            val paperUnwrappedBitmap4 =
                remember(paperUnwrapped4) { paperUnwrapped4.toImageBitmap() }


            val maxWidth = this@BoxWithConstraints.maxWidth
            val maxHeight = this@BoxWithConstraints.maxHeight

            PotentialImage(original, maxWidth, maxHeight)
//            Divider()
//            potentialImage(originalBwResized, maxWidth, maxHeight)
//            Divider()
//            potentialImage(cannyBitmap, maxWidth, maxHeight)
//            Divider()
//            potentialImage(withContoursBitmap, maxWidth, maxHeight)
            Divider()
            PotentialImage(dotBitmap, maxWidth, maxHeight)
            Divider()
            PotentialImage(unwrappedBitmap, maxWidth, maxHeight)
            Divider()
            PotentialImage(paperUnwrappedBitmap1, maxWidth, maxHeight)
            Divider()
            PotentialImage(paperUnwrappedBitmap2, maxWidth, maxHeight)
            Divider()
            PotentialImage(paperUnwrappedBitmap3, maxWidth, maxHeight)
            Divider()
            PotentialImage(paperUnwrappedBitmap4, maxWidth, maxHeight)
            Divider()


        }
    }
}

@Composable
fun ImageAndOtherStuff(bitmap: Bitmap) {

    val originalMat = remember(bitmap) {
        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)

        val mat3 = Mat()
        Imgproc.cvtColor(mat, mat3, Imgproc.COLOR_BGRA2BGR)
        mat3
    }

    ImageAndOtherStuff(originalMat = originalMat)


}


@Composable
fun PotentialImage(image: ImageBitmap?, width: Dp, height: Dp) {
    Card(
        modifier = Modifier
            .height(height)
            .width(width)
    ) {
        image?.also {
            Image(
                modifier = Modifier
                    .fillMaxSize(),
                bitmap = image, contentDescription = null
            )
        }
    }
}

