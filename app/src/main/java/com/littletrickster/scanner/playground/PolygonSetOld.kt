package com.littletrickster.scanner.playground


import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.littletrickster.scanner.*
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import java.io.File

@Composable
fun PolygonSetOld(file: File, back: (() -> Unit)? = null, unwrappedReturn: (finalMat: Mat) -> Unit) {

    val fileExist = remember(file) { file.exists() }

    if (!fileExist) return


    var loading by remember { mutableStateOf(false) }

    back?.also { BackHandler(true, it) }

    val imageBounds = remember(file) {
        file.getImageBounds()
    }

    val rotation = remember(file) {
        file.bitmapRotation()
    }


    val bitmap = remember {
        file.toBitmap()
    }
    val originalMat = remember {
        val originalMat = Mat()
        Utils.bitmapToMat(bitmap, originalMat)
        originalMat
    }

    val rotatedPoints = remember<List<Point>?>(file) {


        val bgrMat = Mat()
        Imgproc.cvtColor(originalMat, bgrMat, Imgproc.COLOR_BGRA2BGR)

        val gray = Mat()
        Imgproc.cvtColor(bgrMat, gray, Imgproc.COLOR_BGR2GRAY)
        bgrMat.release()

        val resizedBw = Mat()
        val scale = gray.resizeMax(resizedBw, 500.0)
        gray.release()

        val blurredBw = Mat()
        Imgproc.GaussianBlur(resizedBw, blurredBw, Size(7.0, 7.0), 0.0)
        resizedBw.release()

        val cannyMat = Mat()
        Imgproc.Canny(blurredBw, cannyMat, 40.0, 80.0)
        blurredBw.release()
        Imgproc.dilate(cannyMat, cannyMat, Mat(), Point(-1.0, -1.0), 2)
        cannyMat.simpleRotate(rotation)

        val list = ArrayList<MatOfPoint>()
        Imgproc.findContours(
            cannyMat,
            list,
            Mat(),
            Imgproc.RETR_LIST,
            Imgproc.CHAIN_APPROX_SIMPLE
        )

        var maxVal = 0.0
        var maxValIdx = 0
        for (contourIdx in 0 until list.size) {
            val contourArea = Imgproc.contourArea(list[contourIdx])
            if (maxVal < contourArea) {
                maxVal = contourArea
                maxValIdx = contourIdx
            }
        }

        val largestNr = maxValIdx

        if (list.size == 0) {
            return@remember null
        }

        val largest = list[largestNr]
        val points = getCornersFromPoints(largest.toList())



        points.forEach {
            it.x *= scale
            it.y *= scale
        }


        points

    }

    val notRotatedPoints = remember<List<Point>?>(file) {
        val bgrMat = Mat()
        Imgproc.cvtColor(originalMat, bgrMat, Imgproc.COLOR_BGRA2BGR)


        val gray = Mat()
        Imgproc.cvtColor(bgrMat, gray, Imgproc.COLOR_BGR2GRAY)
        bgrMat.release()

        val resizedBw = Mat()
        val scale = gray.resizeMax(resizedBw, 500.0)
        gray.release()

        val blurredBw = Mat()
        Imgproc.GaussianBlur(resizedBw, blurredBw, Size(7.0, 7.0), 0.0)
        resizedBw.release()

        val cannyMat = Mat()
        Imgproc.Canny(blurredBw, cannyMat, 40.0, 80.0)
        blurredBw.release()
        Imgproc.dilate(cannyMat, cannyMat, Mat(), Point(-1.0, -1.0), 2)

        val list = ArrayList<MatOfPoint>()
        Imgproc.findContours(
            cannyMat,
            list,
            Mat(),
            Imgproc.RETR_LIST,
            Imgproc.CHAIN_APPROX_SIMPLE
        )

        var maxVal = 0.0
        var maxValIdx = 0
        for (contourIdx in 0 until list.size) {
            val contourArea = Imgproc.contourArea(list[contourIdx])
            if (maxVal < contourArea) {
                maxVal = contourArea
                maxValIdx = contourIdx
            }
        }

        val largestNr = maxValIdx

        if (list.size == 0) {
            return@remember null
        }

        val largest = list[largestNr]
        val points = getCornersFromPoints(largest.toList())



        points.forEach {
            it.x *= scale
            it.y *= scale
        }


        points

    }


    val infiniteTransition = rememberInfiniteTransition()
    val angle by infiniteTransition.animateFloat(
        initialValue = 0F,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(10_000, easing = LinearEasing)
        )
    )


    val rotatedFunPoints = remember(rotation){
        val centerX = (originalMat.cols()-1) / 2.0
        val centerY = (originalMat.rows()-1) / 2.0
        notRotatedPoints?.rotateCopy(rotation,Point(centerX,centerY))
    }


    val rotatedMat = remember {
        val mat =Mat()
        originalMat.copyTo(mat)
        mat.simpleRotate(rotation)
        mat
    }

    val imageProvider = rememberImageProviderFromMat()

    LaunchedEffect(rotatedFunPoints) {
        val mat = Mat()
        rotatedMat.copyTo(mat)
//        notRotatedPoints?.forEach {
//            Imgproc.circle(mat, it, 1, redScalar, 100)
//        }

        rotatedPoints?.forEach {
            Imgproc.circle(mat, it, 1, greenScalar, 100)
        }


        val centerX = (originalMat.cols()-1) / 2.0
        val centerY = (originalMat.rows()-1) / 2.0
        Imgproc.circle(mat, Point(centerX,centerY), 1, Scalar(255.0,255.0,255.0), 100)

        rotatedFunPoints?.forEach {
            it.x-=500
            it.y+=500

            Imgproc.circle(mat, it, 1, blueScalar, 100)
        }

        imageProvider.emit(mat)
    }



    ImagePlayer(
        modifier = Modifier.fillMaxSize(),
        imageProvider = imageProvider
    )

//    Image(bitmap = finalMat, contentDescription = null,
//        modifier  = Modifier.fillMaxSize(),
//    )


}