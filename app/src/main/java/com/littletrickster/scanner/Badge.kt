package com.littletrickster.scanner

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun Badge(nr: Int, modifier: Modifier = Modifier) {
    val str = if (nr > 99) "+99"
    else nr.toString()
    BadgedText(str, modifier)
}


@Preview
@Composable
fun BadgePreview() {
    Badge(nr = 5)
}

@Composable
fun BadgedText(text: String, modifier: Modifier = Modifier) {
    Text(
        modifier = Modifier
            .background(color = Color.Red, shape = RoundedCornerShape(6.dp))
            .border(width = 1.dp, color = Color.White, shape = RoundedCornerShape(6.dp))
            .padding(start = 2.dp, end = 3.dp, top = 2.dp, bottom = 2.dp)
            .then(modifier),
        textAlign = TextAlign.Center,
        color = Color.White,
        text = text
    )
}

@Composable
fun CloseButton(close: () -> Unit) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .background(Color.Red, shape = CircleShape)
            .border(1.dp, Color.White, shape = CircleShape)
            .clickable(onClick = close)
    ) {
        Icon(modifier = Modifier.align(Alignment.Center), imageVector = Icons.Default.Close, contentDescription = null, tint = Color.White)

    }
}

@Preview
@Composable
private fun ClosePreview() {
    CloseButton {}
}
