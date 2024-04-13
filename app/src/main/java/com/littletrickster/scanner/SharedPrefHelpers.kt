@file:Suppress("NAME_SHADOWING")

package com.littletrickster.scanner

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext

@Composable
fun rememberScannerSharedPrefs(context: Context = LocalContext.current) = remember {
    context.scannerSharedPrefs()
}


fun Context.scannerSharedPrefs() = getSharedPreferences("scanner", Context.MODE_PRIVATE)!!
