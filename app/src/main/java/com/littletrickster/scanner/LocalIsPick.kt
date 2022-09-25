package com.littletrickster.scanner

import android.app.Activity
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

val LocalIsPick = staticCompositionLocalOf { false }

@Composable
fun ProvideIsPick(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val isPick = remember {
        when ((context as Activity).intent?.action) {
            Intent.ACTION_PICK, Intent.ACTION_GET_CONTENT -> true
            else -> false
        }
    }

    CompositionLocalProvider(LocalIsPick provides isPick, content = content)
}