package com.littletrickster.scanner

import androidx.camera.core.Preview
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView


@Composable
fun previewView(modifier: Modifier = Modifier, builder: PreviewView.() -> Unit = {}): Preview.SurfaceProvider {
    val context = LocalContext.current
    val view = remember { PreviewView(context) }
    val surfaceProvider = remember { view.surfaceProvider }

    AndroidView(modifier = modifier, factory = {
        view.apply(builder)
    })

    return surfaceProvider
}


