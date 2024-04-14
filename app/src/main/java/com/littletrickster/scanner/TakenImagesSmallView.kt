package com.littletrickster.scanner

import android.graphics.Bitmap
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun TakenImagesSmallView(click: () -> Unit) {
    if (LocalInspectionMode.current) {
        Box(modifier = Modifier.size(50.dp))
        return
    }
    val context = LocalContext.current
    val imageFolder = remember {
        context.getImageFolder()
    }
    val images by observeFile(imageFolder)

    val firstImage = remember(images) {
        images.maxByOrNull(File::lastModified)
    }
    var bitmapAndRotation by remember { mutableStateOf<Pair<Bitmap, Int>?>(null) }

    LaunchedEffect(firstImage) {
        launch(Dispatchers.IO) {
            bitmapAndRotation = firstImage?.toBitmap(300)
        }
    }

    if (firstImage != null) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clickable(onClick = click)
        ) {
            MyImage(
                bitmap = bitmapAndRotation?.first,
                rotation = bitmapAndRotation?.second ?: 0,
                filter = true
            )
            Badge(modifier = Modifier.align(Alignment.TopEnd), nr = images.size)

        }

    } else {
        Box(modifier = Modifier.size(50.dp)) {

        }
    }
}