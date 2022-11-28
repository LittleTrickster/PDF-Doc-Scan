package com.littletrickster.scanner

import android.graphics.*
import android.os.Build
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import androidx.camera.core.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.scale
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import org.opencv.core.Mat
import java.io.File
import kotlin.math.max
import kotlin.math.min

@Immutable
abstract class BaseImageProvider {
    abstract val bmpFlow: SharedFlow<Pair<Bitmap, Int>>
}

@Immutable
open class ImageProvider : BaseImageProvider() {

    private val bmpFlowTemp = MutableSharedFlow<Pair<Bitmap, Int>>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    override
    val bmpFlow: SharedFlow<Pair<Bitmap, Int>> = bmpFlowTemp

    fun emit(bitmap: Bitmap) {
        bmpFlowTemp.tryEmit(bitmap to 0)
    }

    fun emit(bitmap: Bitmap, rotation: Int) {
        bmpFlowTemp.tryEmit(bitmap to rotation)
    }

}

@Immutable
open class ImageProviderFromMat(scope: CoroutineScope) : BaseImageProvider() {
    private val matChannel = Channel<Pair<Mat, Int>>(capacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    override
    val bmpFlow: SharedFlow<Pair<Bitmap, Int>> = matChannel.receiveAsFlow()
        .map {
            it.first.toBitmap() to it.second
        }
        .buffer(capacity = 0, onBufferOverflow = BufferOverflow.DROP_OLDEST)
        .flowOn(Dispatchers.Default)
        .shareIn(scope, SharingStarted.WhileSubscribed(), replay = 1)

    fun emit(mat: Mat) {
        matChannel.trySend(mat to 0)
    }

    fun emit(mat: Mat, rotation: Int) {
        matChannel.trySend(mat to rotation)
    }

}

@Composable
fun rememberImageProvider(): ImageProvider {
    return remember { ImageProvider() }
}

@Composable
fun rememberImageProviderFromMat(): ImageProviderFromMat {
    val scope = rememberCoroutineScope()
    return remember { ImageProviderFromMat(scope) }
}


@Composable
fun MyImage(
    modifier: Modifier = Modifier,
    file: File?,
    processing: Boolean = false,
    update: Int = 0
) {


    val context = LocalContext.current
    val file by rememberUpdatedState(file)


    var bitmapAndRotation by remember { mutableStateOf<Pair<Bitmap, Int>?>(null) }
    var size by remember { mutableStateOf<IntSize?>(null) }
    val bitmapAndRotationInner by rememberUpdatedState(bitmapAndRotation)


    val ref = remember {
        object : View(context) {
            override fun onDraw(canvas: Canvas) {
                val bitmap = bitmapAndRotationInner?.first ?: return
                fullDraw(canvas, this, bitmap, bitmapAndRotationInner?.second ?: 0, true)
            }
        }
    }

    AndroidView(modifier = modifier.onSizeChanged { size = it }, factory = { ref })

    LaunchedEffect(file, size, processing, update) {
        if (processing) return@LaunchedEffect
        if (file?.exists() != true) return@LaunchedEffect

        val size = size
        if (size == null || size.height == 0 || size.width == 0) return@LaunchedEffect
        val temp = file?.toBitmap(max(size.height, ref.height))

        bitmapAndRotation = temp


        ref.postInvalidate()

    }

}


@Composable
fun MyImage(
    modifier: Modifier = Modifier,
    bitmap: Bitmap?,
    rotation: Int = 0,
    filter: Boolean = false,
//    scaleDown: Boolean = false
) {
    val context = LocalContext.current
    val bitmap by rememberUpdatedState(bitmap)
    val rotation by rememberUpdatedState(rotation)
    val filter by rememberUpdatedState(filter)


    val ref = remember {
        object : View(context) {
            override fun onDraw(canvas: Canvas) {
                val bitmap = bitmap ?: return
                fullDraw(canvas, this, bitmap, rotation, filter)
            }
        }
    }

    AndroidView(modifier = modifier, factory = { ref })

    LaunchedEffect(bitmap, rotation, filter) {
        ref.invalidate()
    }


}

private val transparentPaint = Paint().apply {
    color = Color.TRANSPARENT
}

private fun fullDraw(
    canvas: Canvas,
    view: View,
    bitmap: Bitmap,
    rotation: Int,
    filter: Boolean = false
) {
    canvas.drawRect(0f, 0f, canvas.width.toFloat(), canvas.height.toFloat(), transparentPaint)
    canvas.rotate(rotation.toFloat(), canvas.width / 2f, canvas.height / 2f)

    val bitmapToDraw =
//        if (bitmap.width < canvas.maximumBitmapWidth && bitmap.height <= canvas.maximumBitmapHeight) bitmap
        if (bitmap.width < 3000 && bitmap.height <= 3000) bitmap
        else {
            val newDimensions = calcResizedDimensions(bitmap.height, bitmap.width, canvas.largestDimension.toDouble())
            bitmap.scale(newDimensions.calculatedWidth.toInt(), newDimensions.calculatedHeight.toInt(), true)
        }

    canvas.drawScaledBitmap(view, bitmapToDraw, rotation, filter)
    if (bitmapToDraw !== bitmap) bitmapToDraw.recycle()
}

@Composable
fun ImagePlayer(
    modifier: Modifier = Modifier,
    imageProvider: BaseImageProvider
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var surfaceCreated by remember {
        mutableStateOf(false)
    }
//    var hwAccelerated by remember {
//        mutableStateOf(false)
//    }

    var surfaceChanged by remember {
        mutableStateOf(0)
    }

    val ref = remember {

        SurfaceView(context).apply {

//            setLayerType(View.LAYER_TYPE_HARDWARE, null)


            holder.addCallback(object : SurfaceHolder.Callback {
                override fun surfaceCreated(holder: SurfaceHolder) {
//                    hwAccelerated = this@apply.isHardwareAccelerated
                    surfaceCreated = true

                }

                override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                    surfaceChanged++
                }

                override fun surfaceDestroyed(holder: SurfaceHolder) {
                    surfaceCreated = false
                }

            })

        }

    }

    AndroidView(modifier = modifier, factory = { ref })

    LaunchedEffect(surfaceCreated, surfaceChanged, imageProvider) {
        if (surfaceCreated) {

            imageProvider.bmpFlow
                .onEach { (bitmap, rotation) ->


//                    val canvas = ref.holder.lockCanvas() ?: return@onEach

                    val canvas = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        ref.holder.lockHardwareCanvas() ?: return@onEach
                    } else {
                        ref.holder.lockCanvas() ?: return@onEach
                    }

                    fullDraw(canvas, ref, bitmap, rotation)

//            if (mFpsMeter != null) {
//                mFpsMeter.measure()
//                mFpsMeter.draw(canvas, 20f, 30f)
//            }

                    ref.holder.unlockCanvasAndPost(canvas)


                }
                .flowOn(Dispatchers.Default)
                .launchIn(this)
        }
    }

}


fun Canvas.drawScaledBitmap(surface: View, bitmap: Bitmap, rotation: Int = 0, filter: Boolean = false) {

    val bounds = bitmap.getImageBounds(rotation)


    val mScale = min(surface.height.toFloat() / bounds.rotatedHeight, surface.width.toFloat() / bounds.rotatedWidth)

    drawBitmap(
        bitmap,
        Rect(0, 0, bitmap.width, bitmap.height),
        Rect(
            ((width - mScale * bitmap.width) / 2).toInt(),
            ((height - mScale * bitmap.height) / 2).toInt(),
            ((width - mScale * bitmap.width) / 2 + mScale * bitmap.width).toInt(),
            ((height - mScale * bitmap.height) / 2 + mScale * bitmap.height).toInt()
        ),
        if (filter) Paint().apply {
            isAntiAlias = true
            isFilterBitmap = true
            isDither = true
        }
        else null
    )
}


@Composable
fun ImageAnalyser(
    enabled: Boolean = true,
    imageAnalysis: ImageAnalysis? = null,
    imageCapture: ImageCapture? = null,
    preview: Preview? = null,
    cameraSelector: CameraSelector = remember {
        CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()
    },
    analyze: ((ImageProxy) -> Unit)? = null
) {


    val cameraProviderFuture = rememberCameraProvider()

    val customImageOwner = customImageOwner(enabled)

    val cameraExecutor = remember { Dispatchers.Default.asExecutor() }


    LaunchedEffect(null) {
        val cameraProvider = cameraProviderFuture.await()

        val useCases = ArrayList<UseCase>()

        analyze?.also { analyze ->
            imageAnalysis?.also { imageAnalysis ->
                useCases.add(imageAnalysis)
                imageAnalysis.setAnalyzer(cameraExecutor) { image ->
                    analyze(image)
                    image.close()
                }
            }
        }

        preview?.also(useCases::add)

        imageCapture?.also { imageCapture ->
            useCases.add(imageCapture)
        }

        var camera = cameraProvider.bindToLifecycle(customImageOwner, cameraSelector, *useCases.toTypedArray())


    }

}


@Suppress("unused")
@Composable
fun customImageOwner(enabled: Boolean = true): LifecycleOwner {
    val lifecycleOwner = LocalLifecycleOwner.current

    val owner = remember {

        object : LifecycleOwner {
            val lifecycleRegistry = LifecycleRegistry(this)
            override fun getLifecycle() = lifecycleRegistry

            private var enabled_: Boolean = enabled
            val observer = LifecycleEventObserver { source, event ->
                if (enabled_) {
                    lifecycleRegistry.currentState = event.targetState
                }
            }

            fun enable() {
                this.enabled_ = true
                lifecycleRegistry.currentState = lifecycleOwner.lifecycle.currentState
            }

            fun disable() {
                this.enabled_ = false
                lifecycleRegistry.currentState = Lifecycle.State.CREATED
            }

            init {
                lifecycleOwner.lifecycle.addObserver(observer)
            }


            fun removeObserver() {
                lifecycleOwner.lifecycle.removeObserver(observer)
            }

        }
    }

    LaunchedEffect(enabled) {
        if (enabled) owner.enable()
        else owner.disable()
    }

    DisposableEffect(null) {
        onDispose {
            owner.removeObserver()
            owner.lifecycleRegistry.currentState = Lifecycle.State.DESTROYED

        }
    }

    return owner
}
