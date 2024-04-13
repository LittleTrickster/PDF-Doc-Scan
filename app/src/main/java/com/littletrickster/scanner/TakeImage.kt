package com.littletrickster.scanner

import android.graphics.Bitmap
import android.widget.Toast
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.opencv.core.Mat
import org.opencv.core.Point
import kotlin.math.min
import kotlin.math.roundToInt


@Composable
fun TakeImage(
    fileReceived: (file: Pair<Bitmap, Int>) -> Unit = {},
    showImages: () -> Unit = {}
) {

    val context = LocalContext.current
    val prefs = rememberScannerSharedPrefs()

    var currentFlashMode by remember {
        mutableStateOf(prefs.getInt("flash_mode", ImageCapture.FLASH_MODE_AUTO))
    }

    var imageWidth by remember { mutableStateOf(1) }
    var imageHeight by remember { mutableStateOf(1) }

    var points by remember { mutableStateOf(emptyList<Point>()) }


    val flashAuto = rememberVectorPainter(Icons.Filled.FlashAuto)
    val flashOff = rememberVectorPainter(Icons.Filled.FlashOff)
    val flashOn = rememberVectorPainter(Icons.Filled.FlashOn)

    var capturing by remember { mutableStateOf(false) }

    val analysisConfig = remember {
        ImageAnalysis.Builder().setResolutionSelector(defaultResolutionSelector())
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
            .build()
    }

    val imageCaptureConfig = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .setResolutionSelector(defaultResolutionSelector())
            .build()
    }

    LaunchedEffect(null) {
        val flow = snapshotFlow { currentFlashMode }

        flow.drop(1).onEach {
            prefs
                .edit {
                    this.putInt("flash_mode", it)
                }
        }.launchIn(this)

        flow.onEach { imageCaptureConfig.flashMode = it }
            .launchIn(this)
    }

    val scope = rememberCoroutineScope()



    ConstraintLayout(modifier = Modifier.fillMaxSize()) {
        val (topBar, videoPreview, bottomBar) = createRefs()

        var parentSize by remember { mutableStateOf(IntSize(1, 1)) }


        val mScale = remember(parentSize, imageWidth, imageHeight) {
            min(
                parentSize.height.toFloat() / imageHeight.toFloat(), parentSize.width.toFloat() / imageWidth.toFloat()
            )
        }

        val scaledWidth = remember(mScale, imageWidth) {
            imageWidth * mScale
        }

        val scaledHeight = remember(mScale, imageHeight) {
            imageHeight * mScale
        }


        val verticalOffset = remember(scaledHeight, parentSize) { (parentSize.height - scaledHeight) / 2 }
        val horizontalOffset = remember(scaledWidth, parentSize) { (parentSize.width - scaledWidth) / 2 }



        Box(modifier = Modifier
            .height(100.dp)

            .constrainAs(bottomBar) {
                start.linkTo(parent.start)
                bottom.linkTo(parent.bottom)
                end.linkTo(parent.end)
            }) {

            Row(
                modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically
            ) {

                Box(modifier = Modifier.size(50.dp)) {

                }
                Box(modifier = Modifier.size(50.dp)) {

                }

                CaptureButton(enabled = !capturing) {
                    if (!capturing) {
                        capturing = true
                        scope.launch(Dispatchers.IO) {
                            try {

//                                val file = imageCaptureConfig.takePicture(tempFolder, "temp-image.jpg")
                                val image = imageCaptureConfig.getImage()
                                val bitmap = image.captureBitmap()
                                val rotation = image.imageInfo.rotationDegrees
                                image.close()

                                fileReceived(bitmap to rotation)

                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            } finally {
                                capturing = false
                            }
                        }
                    }

                }


                val currentPainter = when (currentFlashMode) {
                    ImageCapture.FLASH_MODE_AUTO -> flashAuto
                    ImageCapture.FLASH_MODE_OFF -> flashOff
                    else -> flashOn
                }

                Image(painter = currentPainter, "flash",
                    Modifier
                        .clickable {
                            val nextMode = when (currentFlashMode) {
                                ImageCapture.FLASH_MODE_OFF -> ImageCapture.FLASH_MODE_AUTO
                                ImageCapture.FLASH_MODE_AUTO -> ImageCapture.FLASH_MODE_ON
                                else -> ImageCapture.FLASH_MODE_OFF
                            }
                            currentFlashMode = nextMode


                        }
                        .padding(15.dp), colorFilter = ColorFilter.tint(Color.White)

                )

                TakenImagesSmallView(click = { showImages() })

            }

        }



        Box(modifier = Modifier
            .constrainAs(videoPreview) {
                top.linkTo(parent.top)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
                bottom.linkTo(bottomBar.top)
                height = Dimension.fillToConstraints
            }
            .onSizeChanged { parentSize = it }) {

            val surfaceProvider = previewView(modifier = Modifier.fillMaxSize(), builder = {
                scaleType = PreviewView.ScaleType.FIT_CENTER
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            })
            val preview = remember {
                val preview: Preview = Preview.Builder()
                    .setResolutionSelector(defaultResolutionSelector())
                    .build()

                preview.setSurfaceProvider(surfaceProvider)
                preview
            }

            ImageAnalyser(
                imageAnalysis = analysisConfig,
                imageCapture = imageCaptureConfig,
                preview = preview,
                analyze = {
                val mat = it.yuvToMat()

                val resized = Mat()
                val scale = mat.resizeMax(resized, 300.0)
                mat.release()
                val foundPoints = getPoints(resized)
                foundPoints.rotate(it.imageInfo.rotationDegrees, Point(resized.width() / 2.0, resized.height() / 2.0))

                resized.release()

                foundPoints *= scale


                imageWidth = it.rotatedWidth()
                imageHeight = it.rotatedHeight()
                points = foundPoints
            })

            repeat(4) {
                SimpleTargetCircle(
                    getOffset = { points.getOrNull(it)?.toOffset() },
                    horizontalOffset = horizontalOffset,
                    verticalOffset = verticalOffset,
                    scale = mScale
                )
            }

        }

    }

}


val offsetAnim = tween<Offset>(durationMillis = 220, easing = LinearEasing)

@Composable
private fun SimpleTargetCircle(
    getOffset: () -> Offset?,
    horizontalOffset: Float,
    verticalOffset: Float,
    scale: Float,
    circleColor: Color = Color.Green,
) {

    val offset = getOffset() ?: return


    val animatedOffset by animateOffsetAsState(
        targetValue = offset,
        animationSpec = offsetAnim
    )


    Box(
        Modifier
            .offset {
                IntOffset(
                    (horizontalOffset + animatedOffset.x * scale - 20.dp.toPx()).roundToInt(),
                    (verticalOffset + animatedOffset.y * scale - 20.dp.toPx()).roundToInt()
                )
            }

            .background(Color(255, 255, 255, 40), shape = CircleShape)
            .size(37.dp)
            .border(3.dp, circleColor, shape = CircleShape))
}

fun defaultResolutionSelector() = ResolutionSelector.Builder().apply {
    setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
}.build()

@androidx.compose.ui.tooling.preview.Preview
@Composable
fun CaptureButton(
    enabled: Boolean = true,
    click: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .clickable(enabled = enabled, onClick = click)
            .padding(2.dp)
            .border(2.dp, Color.White, CircleShape) // inner border
            .padding(6.dp) // padding
            .size(50.dp)
            .background(Color.White, CircleShape)
    )
}