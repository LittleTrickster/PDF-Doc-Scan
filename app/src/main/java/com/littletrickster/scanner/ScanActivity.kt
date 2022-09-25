package com.littletrickster.scanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.littletrickster.scanner.ui.theme.ScannerTheme


class ScanActivity : ComponentActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {

            ScannerTheme {
                PermissionRequester {
                    ProvideIsPick {
                        Surface(color = MaterialTheme.colors.background) {
//                            bundleOfStuff()
//                            LiveCameraProcess()
                            Root()
                        }
                    }
                }
            }

        }

    }


}

