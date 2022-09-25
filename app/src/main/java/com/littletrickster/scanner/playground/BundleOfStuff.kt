package com.littletrickster.scanner.playground

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier

@Composable
private fun BundleOfStuff() {
    var current by remember { mutableStateOf(0) }
    Column(modifier = Modifier.fillMaxSize()) {


        LazyRow(modifier = Modifier.fillMaxWidth()) {
            item {
                Button(onClick = { current = 0 }) {
                    Text("Simple")
                }
            }
            item {
                Button(onClick = { current = 1 }) {
                    Text("Live")
                }
            }
        }


        when (current) {
            0 -> CameraSimple()
            1 -> LiveCameraProcessing()
        }
    }
}


