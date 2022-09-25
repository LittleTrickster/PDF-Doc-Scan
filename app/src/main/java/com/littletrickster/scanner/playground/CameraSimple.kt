package com.littletrickster.scanner.playground

import android.annotation.SuppressLint
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.littletrickster.scanner.fileProvider
import com.littletrickster.scanner.toRotatedBitmap
import java.io.File

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun CameraSimple() {
    Scaffold(topBar = {
        TopAppBar(title = { Text("Name") })
    }) {

        val context = LocalContext.current
        val file = File(context.filesDir, "picFromCamera")

        var exist by remember { mutableStateOf(0) }

        val originalBitmap = remember(exist) {
            if (exist != 0) file.toRotatedBitmap()
            else null
        }

        val uri = context.fileProvider(file)

        val launcher =
            rememberLauncherForActivityResult(contract = ActivityResultContracts.TakePicture()) {
                if (it) {
                    exist++
                } else exist = 0
            }



        Column(
            modifier = Modifier
                .fillMaxSize()


        ) {

            Button(onClick = {
                launcher.launch(uri)
            }) {
                Text("GetImage")
            }

            originalBitmap?.let {
                ImageAndOtherStuff(bitmap = it)
            }


        }

    }
}
