package com.littletrickster.scanner

import android.content.Context
import android.graphics.Bitmap
import android.os.FileObserver
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.coroutines.CoroutineContext

@Composable
fun rememberCameraProvider(): ListenableFuture<ProcessCameraProvider> {
    val context = LocalContext.current
    return remember { ProcessCameraProvider.getInstance(context) }
}

@Composable
fun rememberSingleThreadExecutorService(): ExecutorService {
    val executor = remember { Executors.newSingleThreadExecutor() }
    DisposableEffect(null) {
        onDispose {
            executor.shutdown()
        }
    }
    return executor
}

@Composable
fun dpToSp(dp: Dp) = with(LocalDensity.current) { dp.toSp() }


@Composable
fun observeFile(folder: File): State<List<File>> {

    val imagesState = remember { mutableStateOf(folder.listFiles()!!.toList()) }
    var images by imagesState

    DisposableEffect(null) {


        val s = object : FileObserver(folder.path) {
            override fun onEvent(event: Int, path: String?) {
                when (event) {
                    CREATE,
                    DELETE,
                    MODIFY -> {
                        images = folder.listFiles()!!.toList()
                    }
                }
            }
        }

        s.startWatching()

        onDispose {
            s.stopWatching()
        }

    }

    return imagesState
}


@Composable
fun rememberPdfThumbnail(file: File, extra: String = ""): State<Bitmap?> {
    val context = LocalContext.current
    val thumbnailFolder = remember { context.getPdfThumbnailsFolder() }

    val bitmapState = remember(file) { mutableStateOf<Bitmap?>(null) }
    var bitmap by bitmapState

    LaunchedEffect(file) {
        withContext(Dispatchers.IO) {
            val cached = File(thumbnailFolder, "${file.name}$extra")
            if (!cached.exists()) {
                val firstPage = file.getFirstPageBitmap(500)
                yield()
                if (firstPage != null) {
                    cached.saveJPEG(firstPage)
                    firstPage

                }
            }
            try {
                bitmap = cached.toBitmap()
            } catch (e: Exception) {
            }

        }
    }
    return bitmapState
}



@Composable
fun DisableOverscroll(content: @Composable () -> Unit) =
    CompositionLocalProvider(LocalOverscrollConfiguration provides null, content = content)




@Composable
fun LaunchedEffectAsync(key: () -> Unit, context: CoroutineContext = Dispatchers.IO, block: suspend CoroutineScope.() -> Unit) {
    LaunchedEffect(null) {
        withContext(context) {
            compute(key = key, block = block)
        }
    }
}


@Suppress("SuspendFunctionOnCoroutineScope")
suspend fun CoroutineScope.compute(
    key: () -> Unit,
    block: suspend CoroutineScope.() -> Unit

) {
    // Objects read the last time block was run
    val readSet = mutableSetOf<Any>()
    val readObserver: (Any) -> Unit = { readSet.add(it) }

    // This channel may not block or lose data on a trySend call.
    val appliedChanges = Channel<Set<Any>>(Channel.UNLIMITED)

    // Register the apply observer before running for the first time
    // so that we don't miss updates.
    val unregisterApplyObserver = Snapshot.registerApplyObserver { changed, _ ->
        appliedChanges.trySend(changed)
    }

    try {
        Snapshot.takeSnapshot(readObserver).run {
            try {
                enter(key)
                block()
            } finally {
                dispose()
            }
        }
//        emit(lastValue)

        while (true) {
            var found = false
            var changedObjects = appliedChanges.receive()

            // Poll for any other changes before running block to minimize the number of
            // additional times it runs for the same data
            while (true) {
                // Assumption: readSet will typically be smaller than changed
                found = found || readSet.intersects(changedObjects)
                changedObjects = appliedChanges.tryReceive().getOrNull() ?: break
            }

            if (found) {
                readSet.clear()
                Snapshot.takeSnapshot(readObserver).run {
                    try {
                        enter(key)
                        block()
                    } finally {
                        dispose()
                    }
                }
            }
        }
    } finally {
        unregisterApplyObserver.dispose()
    }
}


private fun <T> Set<T>.intersects(other: Set<T>): Boolean = if (size < other.size) any { it in other } else other.any { it in this }