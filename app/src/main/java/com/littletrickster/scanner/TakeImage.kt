package com.littletrickster.scanner

import android.graphics.Bitmap
import android.widget.Toast
import androidx.camera.core.ImageCapture
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.CircularProgressIndicator
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


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

    var capturing by remember { mutableStateOf(false) }


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



    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        PointPreview(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            imageCaptureConfig = imageCaptureConfig
        )

        BottomBar(modifier = Modifier.fillMaxWidth(),
            capturing = capturing,
            captureClick = {
                if (capturing) return@BottomBar
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

            }, currentFlashMode = currentFlashMode,
            modeClick = {
                val nextMode = when (currentFlashMode) {
                    ImageCapture.FLASH_MODE_OFF -> ImageCapture.FLASH_MODE_AUTO
                    ImageCapture.FLASH_MODE_AUTO -> ImageCapture.FLASH_MODE_ON
                    else -> ImageCapture.FLASH_MODE_OFF
                }
                currentFlashMode = nextMode
            },
            imageClick = { showImages() }
        )
    }

}


@Preview
@Composable
fun BottomBar(
    modifier: Modifier = Modifier,
    capturing: Boolean = false,
    captureClick: () -> Unit = {},
    currentFlashMode: Int = ImageCapture.FLASH_MODE_AUTO,
    modeClick: () -> Unit = {},
    imageClick: () -> Unit = {}
) {

    Box(
        modifier = Modifier
            .height(100.dp)
            .then(modifier)
    ) {

        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {

            Box(modifier = Modifier.size(50.dp)) {

            }
            Box(modifier = Modifier.size(50.dp)) {

            }

            CaptureButton(enabled = !capturing) {
                captureClick()
            }

            val flashAuto = rememberVectorPainter(Icons.Filled.FlashAuto)
            val flashOff = rememberVectorPainter(Icons.Filled.FlashOff)
            val flashOn = rememberVectorPainter(Icons.Filled.FlashOn)


            val currentPainter = when (currentFlashMode) {
                ImageCapture.FLASH_MODE_AUTO -> flashAuto
                ImageCapture.FLASH_MODE_OFF -> flashOff
                else -> flashOn
            }

            Image(painter = currentPainter, "flash",
                Modifier
                    .clickable {
                        modeClick()
                    }
                    .padding(15.dp),
                colorFilter = ColorFilter.tint(Color.White)
            )

            TakenImagesSmallView(click = { imageClick() })

        }

    }
}


val offsetAnim = tween<Offset>(durationMillis = 220, easing = LinearEasing)


fun defaultResolutionSelector() = ResolutionSelector.Builder().apply {
    setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
}.build()


@Preview
@Composable
fun CaptureButtonPreview(
) {
    var enabled by remember { mutableStateOf(true) }
    CaptureButton(enabled) { enabled = !enabled }
}


@Composable
fun CaptureButton(
    enabled: Boolean = true,
    click: () -> Unit = {}
) {
    val interactionSource = remember { MutableInteractionSource() }

    val clicked by interactionSource.collectIsPressedAsState()

    val delta by animateDpAsState(
        targetValue = if (clicked) 20.dp else 0.dp,
        animationSpec = tween(durationMillis = 220, easing = LinearEasing)
    )
    val alpha by animateFloatAsState(
        targetValue = if (enabled) 1f else 0f,
        animationSpec = tween(durationMillis = 220, easing = LinearEasing)
    )



    Box(
        modifier = Modifier
            .clickable(
                enabled = enabled,
                onClick = click,
                interactionSource = interactionSource,
                indication = null
            )
            .padding(2.dp)
            .border(2.dp, Color.White, CircleShape) // inner border
            .padding(6.dp + delta / 2) // padding

    ) {

        if (!enabled) {
            CircularProgressIndicator(
                Modifier
                    .alpha(1f - alpha)
                    .size(50.dp - delta),
//                color = Color.Black,
                strokeCap = StrokeCap.Round
            )
        }

        Box(
            Modifier
                .alpha(alpha)
                .size(50.dp - delta)
                .background(Color.White, CircleShape)

        )


    }
}