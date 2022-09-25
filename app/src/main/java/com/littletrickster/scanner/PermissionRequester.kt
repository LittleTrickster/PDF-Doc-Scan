package com.littletrickster.scanner

import android.Manifest
import android.content.Intent
import android.net.Uri
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.google.accompanist.permissions.rememberMultiplePermissionsState

val myPermissions = listOf(
//    Manifest.permission.ACCESS_FINE_LOCATION,
//    Manifest.permission.ACCESS_COARSE_LOCATION,
//    Manifest.permission.INTERNET,
//    Manifest.permission.FOREGROUND_SERVICE,
    Manifest.permission.CAMERA
)

@Composable
fun PermissionRequester(children: @Composable () -> Unit) {
    val permissions = rememberMultiplePermissionsState(myPermissions)
    LaunchedEffect(null) {
        permissions.launchMultiplePermissionRequest()
    }


    when {

        permissions.allPermissionsGranted -> {
            children()
        }

        !permissions.allPermissionsGranted -> {
            val context = LocalContext.current
            Button(onClick = {
                if (permissions.shouldShowRationale) {
                    permissions.launchMultiplePermissionRequest()
                } else {
                    val intent = Intent("android.settings.APPLICATION_DETAILS_SETTINGS")
                    intent.data = Uri.parse("package:" + context.packageName)
                    context.startActivity(intent)
                }
            }) {
                Text("Missing permissions")
            }
        }


    }

}