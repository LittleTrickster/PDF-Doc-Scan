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


@Composable
private fun <T> SharedPreferences.baseGetShared(key: String, default: T, getValue: SharedPreferences.(key: String, default: T) -> T): State<T> {


    val obj: MutableState<T> = remember {
        mutableStateOf(getValue(key, default))
    }

    val function = remember {
        SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
            obj.value = sharedPreferences.getValue(key, default)
        }
    }

    DisposableEffect(null) {
        registerOnSharedPreferenceChangeListener(function)
        onDispose {
            unregisterOnSharedPreferenceChangeListener(function)
        }
    }

    return obj
}


@Composable
fun SharedPreferences.getSharedString(key: String, default: String) = baseGetShared(key, default) { key, default ->
    getString(key, default)!!
}


@Composable
fun SharedPreferences.getSharedInt(key: String, default: Int) = baseGetShared(key, default) { key, default ->
    getInt(key, default)
}