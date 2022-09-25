package com.littletrickster.scanner

import android.app.Activity
import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.io.File
import java.util.*


@Composable
fun Root() {
    val scope = rememberCoroutineScope()

    val context = LocalContext.current
    val tempFolder = remember { context.tempFolder() }
    val originalImageFolder = remember { context.getImageFolder() }
    val unwrappedImageFolder = remember { context.getUnwrappedImageFolder() }
    val effectImageFolder = remember { context.getEffectImageFolder() }

    var ocSaveTask by remember { mutableStateOf<Deferred<File>?>(null) }



    LaunchedEffect(null) {
        val ocFiles = originalImageFolder.listFiles()!!
        val uwFiles = unwrappedImageFolder.listFiles()!!
        val efFiles = effectImageFolder.listFiles()!!
        val temp = File(tempFolder, "temp-image.jpg")
        temp.delete()
        if (ocFiles.size != uwFiles.size) {
            ocFiles.forEach(File::delete)
            uwFiles.forEach(File::delete)
            efFiles.forEach(File::delete)

        }
    }

    var bitmapAndRotation by remember {
        mutableStateOf<Pair<Bitmap, Int>?>(null)
    }

    val finalBitmap by remember { derivedStateOf { bitmapAndRotation?.first } }
    val rotation by remember { derivedStateOf { bitmapAndRotation?.second } }




    BackHandler {
        (context as Activity).finish()
    }

    var currentTab by remember { mutableStateOf(0) }


    finalBitmap?.also { finalBitmap ->

        PolygonSet(
            originalBitmap = finalBitmap,
            rotation = rotation!!,
            back = {
                val t = bitmapAndRotation
                bitmapAndRotation = null
                t?.first?.recycle()
            },
            unwrappedReturn = {
                scope.launch(Dispatchers.IO) {
                    val date = Date().time

                    val unwrappedMat = it()
                    val unwrappedBitmap = unwrappedMat.toBitmap()
                    unwrappedMat.release()

                    val unwrappedFile = File(unwrappedImageFolder, "$date.jpg")
                    unwrappedFile.saveJPEG(bitmap = unwrappedBitmap, rotation = rotation!!)
                    unwrappedBitmap.recycle()

                    val originalFile = File(originalImageFolder, "$date.jpg")
                    val tempFile = ocSaveTask!!.await()
                    tempFile.renameTo(originalFile)




                    bitmapAndRotation = null

                }
            }
        )


    } ?: run {
        Column {
            if (currentTab == 0 || currentTab == 1) TopAppBar {
                TabRow(selectedTabIndex = 0, modifier = Modifier.fillMaxHeight(),
                    contentColor = MaterialTheme.colors.onSurface,
                    indicator = {}, divider = {}) {
                    Tab(selected = currentTab == 0, onClick = { currentTab = 0 }) {
                        Text(stringResource(R.string.scan))
                    }
                    Tab(selected = currentTab == 1, onClick = { currentTab = 1 }) {
                        Text("PDF")
                    }
                }
            }

            when (currentTab) {

                0 -> {
                    TakeImage(
                        fileReceived = {
                            bitmapAndRotation = it

                            ocSaveTask = scope.async(Dispatchers.IO) {

                                val tempFile = File(tempFolder, "temp-image.jpg")
                                tempFile.saveJPEG(bitmap = it.first, quality = 98, rotation = it.second)

                                tempFile
                            }
                        },
                        showImages = {
                            currentTab = 3
                        })

                }
                1 -> {
                    PdfBrowser()
                }
                3 -> {
                    ImageBrowser(back = { currentTab = 0 })
                }
            }
        }
    }
}