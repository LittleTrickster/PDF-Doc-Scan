package com.littletrickster.scanner


import android.annotation.SuppressLint
import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Scaffold
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.graphics.scale
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.Point
import kotlin.math.min
import kotlin.math.roundToInt


@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun PolygonSet(
    originalBitmap: Bitmap,
    rotation: Int,
    back: (() -> Unit)? = null,
    setPoints:  (List<Point>)->Unit
) {
    var loading by remember { mutableStateOf(false) }

    back?.also { BackHandler(true, it) }

    val imageBounds = remember(originalBitmap) {
        originalBitmap.getImageBounds(rotation)
    }


    val points = remember(originalBitmap) {
        val newDimensions = calcResizedDimensions(originalBitmap.height, originalBitmap.width, 500.0)

        val scaledBitmap =
            originalBitmap.scale(newDimensions.calculatedWidth.toInt(), newDimensions.calculatedHeight.toInt(), true)

        val scale = originalBitmap.width / scaledBitmap.width.toDouble()


        val scaledMat = Mat()
        Utils.bitmapToMat(scaledBitmap, scaledMat)
        scaledBitmap.recycle()


        val points = getPoints(scaledMat)
        scaledMat.release()
        points *= scale

        points.rotate(rotation, Point((originalBitmap.width - 1) / 2.0, (originalBitmap.height - 1) / 2.0))
        points
    }





    val offsetPoints = remember {
        mutableStateListOf(*Array(points.size){points[it].toOffset()})
    }



    Scaffold(topBar = {
        TopAppBar(
            title = { },
            navigationIcon = back?.let {
                {
                    IconButton(onClick = {
                        it.invoke()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "backIcon")
                    }
                }
            },
            actions = {
                IconButton(onClick = {

                    loading = true

                    val cPoints = offsetPoints.map { it.toCvPoint() }

                    val finalPoints = cPoints.rotateReverseCopy(
                        rotation,
                        Point((imageBounds.rotatedWidth - 1) / 2.0,
                            (imageBounds.rotatedHeight - 1) / 2.0)
                    )

                    setPoints(finalPoints)


                }) {
                    Icon(imageVector = Icons.Filled.Check, contentDescription = "check")
                }
            }

        )
    }) {

        var parentSize by remember { mutableStateOf(IntSize(1, 1)) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { parentSize = it }
        ) {


            val mScale = remember(parentSize, imageBounds) {
                min(
                    parentSize.height.toFloat() / imageBounds.rotatedHeight.toFloat(),
                    parentSize.width.toFloat() / imageBounds.rotatedWidth.toFloat()
                )
            }

            val scaledWidth = remember(mScale) {
                imageBounds.rotatedWidth * mScale
            }

            val scaledHeight = remember(mScale) {
                imageBounds.rotatedHeight * mScale
            }


            val verticalOffset = remember(scaledHeight) { (parentSize.height - scaledHeight) / 2 }
            val horizontalOffset = remember(scaledWidth) { (parentSize.width - scaledWidth) / 2 }
            val offset = remember(verticalOffset, horizontalOffset) { Offset(horizontalOffset, verticalOffset) }


            MyImage(modifier = Modifier.fillMaxSize(), originalBitmap, rotation = rotation, filter = true)


            offsetPoints.forEachIndexed { index, offsetPoint ->
                TargetCircle(
                    coordinates = offsetPoint,
                    setOffset = { offsetPoints[index] = it },
                    horizontalOffset = horizontalOffset,
                    scaledWidth = scaledWidth,
                    verticalOffset = verticalOffset,
                    scaledHeight = scaledHeight,
                    scale = mScale
                )
            }

            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {

                for (i in offsetPoints.indices) {
                    val currentOffset  = offsetPoints[i]
                    val nextOffset = offsetPoints[(i + 1) % points.size]

                    drawLine(
                        color = Color.Green,
                        strokeWidth = Stroke.DefaultMiter * 2,
                        cap = StrokeCap.Round,
                        start = currentOffset * mScale + offset,
                        end = nextOffset * mScale + offset
                    )

                }
            }
        }


    }
    LoadingDialog(loading = loading)
}


@Composable
private fun TargetCircle(
    coordinates: Offset,
    setOffset: (Offset) -> Unit = {},
    horizontalOffset: Float,
    scaledWidth: Float,
    verticalOffset: Float,
    scaledHeight: Float,
    scale: Float,
    circleColor: Color = Color.Green,
) {

    var temp by remember { mutableStateOf(Offset(1f, 1f)) }

    Box(
        Modifier
//            .offset { IntOffset((coordinates.x - 25.dp.toPx()).roundToInt(), (coordinates.y - 25.dp.toPx()).roundToInt()) }
            .offset {
                IntOffset(
                    (horizontalOffset + coordinates.x * scale - 25.dp.toPx()).roundToInt(),
                    (verticalOffset + coordinates.y * scale - 25.dp.toPx()).roundToInt()
                )
            }

            .background(Color(255, 255, 255, 40), shape = CircleShape)
            .size(47.dp)
            .border(3.dp, circleColor, shape = CircleShape)
            .pointerInput(horizontalOffset, scaledWidth, verticalOffset, scaledHeight, scale) {

                detectDragGestures(
                    onDragStart = { temp = coordinates },
                    onDrag = { change, dragAmount ->
                        val nextX = (temp.x + dragAmount.x / scale)
                        val nextY = (temp.y + dragAmount.y / scale)
                        val viewX = nextX * scale + horizontalOffset
                        val viewY = nextY * scale + verticalOffset

                        temp = Offset(nextX, nextY)
                        when {
                            viewX < horizontalOffset -> return@detectDragGestures
                            viewX > horizontalOffset + scaledWidth -> return@detectDragGestures
                            viewY < verticalOffset -> return@detectDragGestures
                            viewY > verticalOffset + scaledHeight -> return@detectDragGestures
                        }
                        setOffset(Offset(nextX, nextY))
                    },
//                    onDragEnd = { temp = coordinates }
                )
            }
    )
}

