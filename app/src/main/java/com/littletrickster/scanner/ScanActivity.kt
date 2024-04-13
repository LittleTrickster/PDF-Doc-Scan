package com.littletrickster.scanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
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

