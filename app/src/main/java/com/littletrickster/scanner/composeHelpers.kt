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