package com.littletrickster.scanner.playground

import android.graphics.Color
import androidx.camera.core.*
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Button
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.littletrickster.scanner.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.Point
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc


@Composable
fun LiveCameraProcessing() {

    val originalImage = rememberImageProviderFromMat()
    val bwImage = rememberImageProviderFromMat()
    val cannyImage = rememberImageProviderFromMat()
    val contourImage = rememberImageProviderFromMat()
    val cornerImage = rememberImageProviderFromMat()
    val unwrappedImage = rememberImageProviderFromMat()
    val lastPaperImage = rememberImageProviderFromMat()
    val huge = rememberImageProviderFromMat()
    val hugeUnwrapped = rememberImageProviderFromMat()


    val stuffForUnwrapping = remember {
        object {
            var points: List<Point> = emptyList()
            var width = 0
            var height = 0
        }
    }


    var start by remember { mutableStateOf(false) }


    val analysisConfig = remember {
        ImageAnalysis.Builder()
//            .setTargetResolution(android.util.Size(800, 600))
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
//            .setTargetAspectRatio(AspectRatio.RATIO_16_9)
//            .setTargetRotation(Surface.ROTATION_180)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
            .build()
    }
    val imageCaptureConfig = remember {
        ImageCapture.Builder()
//            .setCaptureMode(CAPTURE_MODE_MAXIMIZE_QUALITY)
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
//            .setBufferFormat(ImageFormat.YUV_420_888)
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .build()
    }


//    LaunchedEffect(null) {
//        withContext(Dispatchers.IO) {
//            while (true) {
//                val proxy = imageCaptureConfig.getImage()
//                val original: Mat = Mat()
//                val buffer = proxy.planes[0].buffer
//                val byteArray = ByteArray(buffer.remaining())
//                buffer.get(byteArray)
//                val bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
//                Utils.bitmapToMat(bitmap, original)
//
//
//
//
//                val scale = original.cols().toDouble() / stuffForUnwrapping.width.toDouble()
//
//
////            ePoints.forEach {
////                Imgproc.circle(original, it, 1, redScalar, 150)
////            }
//
//                val points = stuffForUnwrapping.points
//                if (points.isEmpty()) continue
//                val unwrapped = unwrap(original, stuffForUnwrapping.points, scale)
//
//                val mat = documentMat(unwrapped)
//                Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGR2GRAY)
//
////            huge.emit(original)
//
//
//                hugeUnwrapped.emit(mat,proxy.imageInfo.rotationDegrees)
//
//            }
//        }
//    }

    ImageAnalyser(
        enabled = start,
        imageAnalysis = analysisConfig,
        imageCapture = imageCaptureConfig, analyze = { it: ImageProxy ->

//            val original = Testing.rgba(it)

            val rotation = it.imageInfo.rotationDegrees
            val original = it.yuvToMat()


            originalImage.emit(original, rotation)

            val originalBw = it.toGrayMat()
            val resizedBw = Mat()
            val scale = originalBw.resizeMax(resizedBw, 500.0)
            val blurredBw = Mat()
            Imgproc.GaussianBlur(resizedBw, blurredBw, Size(7.0, 7.0), 0.0)

            bwImage.emit(originalBw, rotation)

            val cannyMat = Mat()
            Imgproc.Canny(blurredBw, cannyMat, 40.0, 80.0)
            Imgproc.dilate(cannyMat, cannyMat, Mat(), Point(-1.0, -1.0), 2)
            cannyImage.emit(cannyMat)

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

            if (list.size == 0) return@ImageAnalyser

            val largest = list[largestNr]


            val withContoursMat = Mat()
            Imgproc.cvtColor(resizedBw, withContoursMat, Imgproc.COLOR_GRAY2RGB)
            Imgproc.drawContours(withContoursMat, list, largestNr, greenScalar, 5)

            contourImage.emit(withContoursMat, rotation)

            val points = getCornersFromPoints(largest.toList())
            stuffForUnwrapping.height = blurredBw.rows()
            stuffForUnwrapping.width = blurredBw.cols()
            stuffForUnwrapping.points = points


            val withDots = Mat()
            withContoursMat.copyTo(withDots)

            points.forEach {
                Imgproc.circle(withDots, it, 1, redScalar, 20)
            }

            cornerImage.emit(withDots, rotation)

            val unwrappedMat = unwrap(originalMat = original, points = points, scale = scale)

            unwrappedImage.emit(unwrappedMat, rotation)


            val mat = documentMat(unwrappedMat)
            Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGR2GRAY)
            lastPaperImage.emit(mat, rotation)
        })


    BoxWithConstraints {
        val maxHeight = maxWidth
        val maxWidth = maxWidth
        DisableOverscroll {
            LazyColumn {
                item {
                    Button(onClick = { start = !start }) {
                        Text("started $start")
                    }
                }

//            item {
//                ImagePlayer(
//                    modifier = Modifier
//                        .width(maxWidth)
//                        .height(maxHeight),
//                    imageProvider = huge
//                )
//            }

                item {
                    ImagePlayer(
                        modifier = Modifier
                            .width(maxWidth)
                            .height(maxHeight),
                        imageProvider = originalImage
                    )
                }

                item { Divider() }

                item {

                    Box(
                        modifier = Modifier
                            .width(maxWidth)
                            .height(maxHeight),
                    )
                }
                item { Divider() }

                item {
                    Box(
                        modifier = Modifier
                            .width(maxWidth)
                            .height(maxHeight),
                    )
                }
                item { Divider() }
//
                item {
                    Box(
                        modifier = Modifier
                            .width(maxWidth)
                            .height(maxHeight),
                    )
                }
                item { Divider() }

                item {
                    Box(
                        modifier = Modifier
                            .width(maxWidth)
                            .height(maxHeight),
                    )
                }
                item { Divider() }

                item {
                    Box(
                        modifier = Modifier
                            .width(maxWidth)
                            .height(maxHeight),
                    )
                }
                item { Divider() }

                item {
                    Box(
                        modifier = Modifier
                            .width(maxWidth)
                            .height(maxHeight),
                    )
                }
                item { Divider() }

                item {
                    Box(
                        modifier = Modifier
                            .width(maxWidth)
                            .height(maxHeight),
                    )
                }


            }

        }
    }

}


@Composable
fun CameraPreview(newImage: (ImageBitmap) -> Unit) {

    val cameraProviderFuture = rememberCameraProvider()
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = rememberSingleThreadExecutorService()

    AndroidView(
        factory = { context ->
            PreviewView(context).apply {
                setBackgroundColor(Color.GREEN)
                scaleType = PreviewView.ScaleType.FIT_START
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                post {
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()

                        val preview: Preview = Preview.Builder()
                            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                            .build()

                        val cameraSelector: CameraSelector = CameraSelector.Builder()
                            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                            .build()

                        preview.setSurfaceProvider(surfaceProvider)

                        val imageAnalysis = ImageAnalysis.Builder()
                            .setTargetResolution(android.util.Size(720, 720))
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
//                            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                            .build()

                        imageAnalysis.setAnalyzer(cameraExecutor) { image ->

                            runBlocking(Dispatchers.Default) {
                                withContext(Dispatchers.Default) {
                                    val mat = image.toGrayMat()

                                    newImage(mat.toImageBitmap())


                                }
                            }



                            image.close()
                        }


                        var camera = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageAnalysis
                        )


                    }, ContextCompat.getMainExecutor(context))
                }
            }
        }
    )
}
