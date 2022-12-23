package com.littletrickster.scanner

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import androidx.camera.core.ImageProxy
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import org.opencv.ximgproc.Ximgproc
import java.nio.ByteBuffer
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

val greenScalar = Scalar(0.0, 255.0, 0.0)
val redScalar = Scalar(255.0, 0.0, 0.0)
val blueScalar = Scalar(0.0, 0.0, 255.0)

fun Mat.toBitmap(): Bitmap {
    val bitmap = Bitmap.createBitmap(this.cols(), this.rows(), Bitmap.Config.ARGB_8888)
    Utils.matToBitmap(this, bitmap)
    return bitmap
}


fun Bitmap.toImmutable(): Bitmap = this.copy(config, false)

fun Bitmap.toHwImmutable(): Bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
    this.copy(Bitmap.Config.HARDWARE, false)
} else {
    toImmutable()
}


fun ImageProxy.rotatedWidth() = when (imageInfo.rotationDegrees) {
    90, 270 -> height
    else -> width
}

fun ImageProxy.rotatedHeight() = when (imageInfo.rotationDegrees) {
    90, 270 -> width
    else -> height
}


fun ImageProxy.captureBitmap(): Bitmap {
    val buffer = planes[0].buffer
    val byteArray = ByteArray(buffer.remaining())
    buffer.get(byteArray)
    return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
}

fun ImageProxy.yuvToMat(): Mat {
    val planes = planes
    val width: Int = this.width
    val height: Int = this.height
    val chromaPixelStride = planes[1].pixelStride
    val mat = if (chromaPixelStride == 2) { // Chroma channels are interleaved
        val mRgba = Mat()
//        assert(planes[0].pixelStride == 1)
//        assert(planes[2].pixelStride == 2)
        val yPlane = planes[0].buffer
        val yPlaneStep = planes[0].rowStride
        val uvPlane1 = planes[1].buffer
        val uvPlane1Step = planes[1].rowStride
        val uvPlane2 = planes[2].buffer
        val uvPlane2Step = planes[2].rowStride
        val yMat = Mat(height, width, CvType.CV_8UC1, yPlane, yPlaneStep.toLong())
        val uvMat1 = Mat(height / 2, width / 2, CvType.CV_8UC2, uvPlane1, uvPlane1Step.toLong())
        val uvMat2 = Mat(height / 2, width / 2, CvType.CV_8UC2, uvPlane2, uvPlane2Step.toLong())
        val addrDiff = uvMat2.dataAddr() - uvMat1.dataAddr()
        if (addrDiff > 0) {
            uvMat2.release()
//            assert(addrDiff == 1L)
            Imgproc.cvtColorTwoPlane(yMat, uvMat1, mRgba, Imgproc.COLOR_YUV2RGBA_NV12)
            yMat.release()
            uvMat1.release()
        } else {
            uvMat1.release()
//            assert(addrDiff == -1L)
            Imgproc.cvtColorTwoPlane(yMat, uvMat2, mRgba, Imgproc.COLOR_YUV2RGBA_NV21)
            yMat.release()
            uvMat2.release()
        }
        mRgba
    } else { // Chroma channels are not interleaved
        val mRgba = Mat()
        val yuvBytes = ByteArray(width * (height + height / 2))
        val yPlane = planes[0].buffer
        val uPlane = planes[1].buffer
        val vPlane = planes[2].buffer
        var yuvBytesOffset = 0
        val yPlaneStep = planes[0].rowStride
        val pixels = width * height
        if (yPlaneStep == width) {
            yPlane[yuvBytes, 0, pixels]
            yuvBytesOffset = pixels
        } else {
            val padding = yPlaneStep - width
            for (i in 0 until height) {
                yPlane[yuvBytes, yuvBytesOffset, width]
                yuvBytesOffset += width
                if (i < height - 1) {
                    yPlane.position(yPlane.position() + padding)
                }
            }
//            assert(yuvBytesOffset == pixels)
        }
        val chromaRowStride = planes[1].rowStride
        val chromaRowPadding = chromaRowStride - width / 2
        if (chromaRowPadding == 0) {
            // When the row stride of the chroma channels equals their width, we can copy
            // the entire channels in one go
            uPlane[yuvBytes, yuvBytesOffset, pixels / 4]
            yuvBytesOffset += pixels / 4
            vPlane[yuvBytes, yuvBytesOffset, pixels / 4]
        } else {
            // When not equal, we need to copy the channels row by row
            for (i in 0 until height / 2) {
                uPlane[yuvBytes, yuvBytesOffset, width / 2]
                yuvBytesOffset += width / 2
                if (i < height / 2 - 1) {
                    uPlane.position(uPlane.position() + chromaRowPadding)
                }
            }
            for (i in 0 until height / 2) {
                vPlane[yuvBytes, yuvBytesOffset, width / 2]
                yuvBytesOffset += width / 2
                if (i < height / 2 - 1) {
                    vPlane.position(vPlane.position() + chromaRowPadding)
                }
            }
        }
        val yuvMat = Mat(height + height / 2, width, CvType.CV_8UC1)
        yuvMat.put(0, 0, yuvBytes)
        Imgproc.cvtColor(yuvMat, mRgba, Imgproc.COLOR_YUV2RGBA_I420, 4)
        yuvMat.release()
        mRgba
    }


    return mat
}


fun ImageProxy.toGrayMat(): Mat {
    val planes = planes
    val yPlane: ByteBuffer = planes[0].buffer
    val yPlaneStep: Int = planes[0].rowStride
    val mat = Mat(height, width, CvType.CV_8UC1, yPlane, yPlaneStep.toLong())

    return mat
}

fun Mat.toImageBitmap() = toBitmap().asImageBitmap()


fun Mat.resizeMax(dest: Mat, maxDimension: Double): Double {

    val newDimensions =
        calcResizedDimensions(originalHeight = this.rows(), originalWidth = this.cols(), maxDimension = maxDimension)

    Imgproc.resize(this, dest, Size(newDimensions.calculatedWidth, newDimensions.calculatedHeight))
    return newDimensions.scale

}

//expensive depending on size
fun Mat.simpleRotate(degrees: Int) {

//    return
    val rotationCode = when (degrees) {
        90 -> Core.ROTATE_90_CLOCKWISE
        -90, 270 -> Core.ROTATE_90_COUNTERCLOCKWISE
        180 -> Core.ROTATE_180
        else -> return
    }


    Core.rotate(this, this, rotationCode)

}


fun reverseDegrees(degrees: Int): Int {
    return when (degrees) {
        0 -> 0
        else -> 360 - degrees
    }
}

fun List<Point>.rotateCopy(rotation: Int, point: Point) = map(Point::clone).rotate(rotation, point)


fun List<Point>.rotateReverseCopy(rotation: Int, point: Point) = rotateCopy(reverseDegrees(rotation), point)


fun List<Point>.rotateReverse(rotation: Int, point: Point) = rotate(reverseDegrees(rotation), point)


fun List<Point>.rotate(angle: Int, center: Point): List<Point> {

    val rot = Imgproc.getRotationMatrix2D(center, -angle.toDouble(), 1.0)
    val cos = abs(rot[0, 0][0])
    val sin = abs(rot[0, 1][0])


    val newWidth = center.y * 2 * sin + center.x * 2 * cos
    val newHeight = center.y * 2 * cos + center.x * 2 * sin


    //replace with put maybe
    rot.at<Double>(0, 2).v += newWidth / 2.0 - center.x
    rot.at<Double>(1, 2).v += newHeight / 2.0 - center.y


    forEach {
        var x = 0
        x += (rot[0, 0][0] * it.x.toInt()).toInt()
        x += (rot[0, 1][0] * it.y.toInt()).toInt()
        x += rot[0, 2][0].toInt()


        var y = 0
        y += (rot[1, 0][0] * it.x.toInt()).toInt()
        y += (rot[1, 1][0] * it.y.toInt()).toInt()
        y += rot[1, 2][0].toInt()

        it.x = x.toDouble()
        it.y = y.toDouble()
    }
    return this
}


/*
//trash
fun List<Point>.rotate(rotation: Int, around: Point): List<Point> {

    if (rotation == 0) return this

    val rad = rotation / 180.0 * PI


    val cos = cos(rad)
    val sin = sin(rad)



    forEach { point ->

        val deltaX = point.x - around.x
        val deltaY = point.y - around.y

        val x = (deltaX * cos) - (deltaY * sin) + around.x
        val y = (deltaX * sin) + (deltaY * cos) + around.y

        point.x = x
        point.y = y

    }


    return this
}
*/

//old simple
fun getCornersFromPoints(points: List<Point>): List<Point> {

    //Bottom-right point has the largest (x + y) value. Top-left has point smallest (x + y) value.
    // The bottom-left point has the smallest (x — y) value. The top-right point has the largest (x — y) value

    val bottomRight = points.maxByOrNull { it.x + it.y }!!
    val topLeft = points.minByOrNull { it.x + it.y }!!
    val bottomLeft = points.minByOrNull { it.x - it.y }!!
    val topRight = points.maxByOrNull { it.x - it.y }!!

    return listOf(bottomRight, topLeft, bottomLeft, topRight)
}

fun Offset.toCvPoint(): Point {
    return Point(this.x.toDouble(), this.y.toDouble())
}

fun Point.toOffset(): Offset {
    return Offset(this.x.toFloat(), this.y.toFloat())
}


fun getAutoBrightnessAlphaAndBeta(input: Mat): Pair<Double, Double> {
    val gray = Mat()
    Imgproc.cvtColor(input, gray, Imgproc.COLOR_BGR2GRAY)
    val alphaAndBeta = getAutoBrightnessAlphaAndBetaFromGray(gray)
    gray.release()
    return alphaAndBeta
}

fun getAutoBrightnessAlphaAndBetaFromGray(gray: Mat): Pair<Double, Double> {
    val hist = Mat()

    Imgproc.calcHist(listOf(gray), MatOfInt(0), Mat(), hist, MatOfInt(10), MatOfFloat(0f, 256f))

    val total = gray.total()
    val histSize = hist.rows()
    val histSizeDouble = histSize.toDouble()

    var sum = 0.0
    for (index in 0 until histSize) {
        val i = hist[index, 0][0]
        val calculated = (histSize - index) / histSizeDouble * i / total
        sum += calculated
    }


    val alpha = 1.05 + sum / 3
    val beta = 0.0 + sum * 100

//    Log.d("debugHist", "alpha = $alpha sum= $sum  beta= $beta")
//    val str = StringBuilder()
//    for (i in 0 until hist.rows()) {
//        str.append("${hist[i, 0][0]}, ")
//    }
//    Log.d("debugHist", "rows ${hist.rows()} columns ${hist.cols()} $str")


    return alpha to beta

}

fun autoBrightnessGray(gray: Mat): Mat {
    val (alpha, beta) = getAutoBrightnessAlphaAndBetaFromGray(gray)
    val final = Mat()
    Core.convertScaleAbs(gray, final, alpha, beta)
    return final
}


fun autoBrightness(input: Mat): Mat {
    val (alpha, beta) = getAutoBrightnessAlphaAndBeta(input)
    val final = Mat()
    Core.convertScaleAbs(input, final, alpha, beta)
    return final
}

fun documentMat(original: Mat): Mat {
    val blurred = Mat()
    Imgproc.blur(original, blurred, Size(5.0, 5.0))

    val (b, g, r) = List(3) { Mat() }.also { Core.split(blurred, it) }
    val temp = Mat()
    Core.min(b, g, temp)
    val temp2 = Mat()
    Core.min(temp, r, temp2)


    val temp3 = Mat()
    Core.max(b, g, temp3)
    val temp4 = Mat()
    Core.max(temp3, r, temp4)

    val divided = Mat()
    Core.divide(temp2, temp4, divided)
    val result = Mat()
    Core.multiply(divided, Scalar(255.0), result)

    val maskInverted = Mat()
    Imgproc.threshold(result, maskInverted, 0.0, 255.0, Imgproc.THRESH_BINARY + Imgproc.THRESH_OTSU)

//    val finalInvertedMask = Mat()
//    Imgproc.blur(result, result, Size(10.0, 10.0))
//    Core.max(result, maskInverted, finalInvertedMask)
    Imgproc.dilate(maskInverted, maskInverted, Mat(), Point(-1.0, -1.0), 1)

    val mask = Mat()
    Core.bitwise_not(maskInverted, mask)

    val gray = Mat()
    Imgproc.cvtColor(original, gray, Imgproc.COLOR_BGR2GRAY)
    val text = Mat()
    Ximgproc.niBlackThreshold(
        gray,
        text,
        255.0,
        Imgproc.THRESH_BINARY,
        41,
        -0.085,
        Ximgproc.BINARIZATION_NICK
    )

    val bg = Mat()
    Core.bitwise_and(text, text, bg, maskInverted)


//    Imgproc.blur(mask, mask, Size(10.0, 10.0))
//    Core.multiply(mask,Scalar(1.2),mask)

    val colored = Mat()
    Core.bitwise_and(original, original, colored, mask)
    val bwStuff = Mat()
    Imgproc.cvtColor(bg, bwStuff, Imgproc.COLOR_GRAY2BGR)
    val finalColor = Mat()
    Imgproc.cvtColor(colored, finalColor, Imgproc.COLOR_RGBA2RGB)
    val out = Mat()

    Core.add(bwStuff, finalColor, out)

    return out
}


fun unwrap(originalMat: Mat, points: List<Point>, scale: Double = 1.0): Mat {
    val (bottomRight, topLeft, bottomLeft, topRight) = points
    return unwrap(originalMat, topLeft, topRight, bottomLeft, bottomRight, scale)
}

fun unwrap(
    originalMat: Mat,
    topLeft: Point,
    topRight: Point,
    bottomLeft: Point,
    bottomRight: Point,
    scale: Double = 1.0
): Mat {

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


    val tempWidth = (if (w1 < w2) w1 else w2)
    val tempHeight = (if (h1 < h2) h1 else h2)

    val (maxWidth, maxHeight) = clampA4(tempWidth.toInt(), tempHeight.toInt())
//    val maxWidth = tempWidth.toInt()
//    val maxHeight = tempHeight.toInt()

    val dst = Mat.zeros(maxHeight, maxWidth, CvType.CV_8UC3)


    val mDest = MatOfPoint2f(
        Point(0.0, 0.0),
        Point((maxWidth - 1).toDouble(), 0.0),
        Point(0.0, (maxHeight - 1).toDouble()),
        Point((maxWidth - 1).toDouble(), (maxHeight - 1).toDouble())
    )

    val pointMat = MatOfPoint2f(
        Point(x1, y1),
        Point(x2, y2),
        Point(x3, y3),
        Point(x4, y4)
    )


    val transformationMatrix = Imgproc.getPerspectiveTransform(pointMat, mDest)

    Imgproc.warpPerspective(originalMat, dst, transformationMatrix, dst.size())

    return dst
}

operator fun List<Point>.timesAssign(scale: Double) {
    forEach { point ->
        point.x *= scale
        point.y *= scale
    }
}