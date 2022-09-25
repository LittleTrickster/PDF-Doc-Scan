package com.littletrickster.scanner

import android.app.Activity
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId


@Composable
fun PdfBrowser() {
    val context = LocalContext.current

    val pdfFolder = remember {
        context.getPdfFolder()
    }

    val pdfFiles by observeFile(pdfFolder)

    //Todo to io from main thread
    val grouped by remember {
        derivedStateOf {
            pdfFiles.sortedByDescending(File::lastModified).map {
                it to Instant.ofEpochMilli(it.lastModified()).atZone(ZoneId.systemDefault()).toLocalDateTime()
            }.groupBy(keySelector = {
                it.second.toLocalDate()
            }, valueTransform = {
                it.first to it.second.toLocalTime()
            })
        }
    }

    if (grouped.isNotEmpty()) LazyColumn(
        modifier = Modifier
            .padding(5.dp)
            .fillMaxSize(),
    ) {
        grouped.forEach { (date, pairs) ->
            stickyHeader(key = date) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    val now = remember {
                        LocalDate.now()
                    }
                    val yesterday = remember {
                        now.minusDays(1)
                    }

                    val headerText = when {
                        now == date -> {
                            stringResource(R.string.today)
                        }
                        yesterday == date -> {
                            stringResource(R.string.yesterday)
                        }
                        else -> {
                            date.toString()
                        }
                    }

                    Text(headerText, fontSize = 20.sp, textAlign = TextAlign.Center)

                }
            }

            items(items = pairs, key = { it.first.name }) { (file, time) ->
                PdfLine(file, time)
            }
        }
    }
    else {
        Box(modifier = Modifier.fillMaxSize()) {
            Text(modifier = Modifier.align(Alignment.Center), text = stringResource(R.string.no_pdf_documents), fontSize = 22.sp)
        }
    }


}

@Preview
@Composable
private fun PdfLinePreview() {
    PdfLine(File("File Name"), LocalTime.now())
}

@Composable
private fun PdfLine(file: File, time: LocalTime) {
    val isPick = LocalIsPick.current
    val bitmap by if (LocalInspectionMode.current) remember {
        mutableStateOf(null)
    }
    else rememberPdfThumbnail(file)


    val context = LocalContext.current
    var contextMenuExpanded by remember { mutableStateOf(false) }

    var tryingToDelete by remember { mutableStateOf(false) }

    Card(modifier = Modifier
        .clickable {
            val uri = context.fileProvider(file)
            if (isPick) {
                val intent = Intent()
                intent.data = uri
                intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                (context as Activity).apply {
                    setResult(Activity.RESULT_OK, intent)
                    finish()
                }

            } else {
                val intent = Intent(Intent.ACTION_VIEW, uri)
                intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                context.startActivity(intent)
            }
        }
        .fillMaxWidth()) {
        Row(modifier = Modifier.padding(10.dp)) {

            Card(
                modifier = Modifier.size(120.dp)
            ) {

                if (LocalInspectionMode.current) Box(
                    modifier = Modifier
                        .size(120.dp)
                        .background(Color.Gray)
                )
                else MyImage(modifier = Modifier.size(120.dp), bitmap = bitmap)


            }

            Column(
                modifier = Modifier
                    .padding(start = 3.dp)
                    .weight(1f)
            ) {
                Text("${file.name}")
                Text("${time.hour.toString().padStart(2, '0')}:${time.minute.toString().padStart(2, '0')}")
            }
            IconButton(modifier = Modifier.align(Alignment.CenterVertically), onClick = { contextMenuExpanded = true }) {

                Icon(modifier = Modifier.padding(5.dp), imageVector = Icons.Filled.MoreVert, contentDescription = null)
            }


        }
    }



    DropdownMenu(expanded = contextMenuExpanded, onDismissRequest = { contextMenuExpanded = false }) {

        DropdownMenuItem(onClick = {
            tryingToDelete = true
        }, content = {
            Text(text = stringResource(id = R.string.deletus))
        })
        DropdownMenuItem(onClick = {

            val uri = context.fileProvider(file)
            val intent = Intent(Intent.ACTION_SEND)

            intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
//                        intent.putExtra(Intent.EXTRA_TEXT,"FILE")
            intent.putExtra(Intent.EXTRA_STREAM, uri)
            intent.type = "application/pdf"
            context.startActivity(Intent.createChooser(intent, null))

            contextMenuExpanded = false


        }, content = {
            Text(text = stringResource(R.string.share))
        })
    }
    if (tryingToDelete) {
        AlertDialog(onDismissRequest = {
            contextMenuExpanded = false
            tryingToDelete = false
        }, title = { Text(stringResource(R.string.to_delete_pdf)) }, confirmButton = {
            Button(onClick = {
                contextMenuExpanded = false
                tryingToDelete = false
                context.deletePdf(file)
            }) {
                Text(stringResource(id = R.string.yes))
            }
        }, dismissButton = {
            Button(onClick = {
                contextMenuExpanded = false
                tryingToDelete = false
            }) {
                Text(stringResource(id = R.string.no))
            }
        }, properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
        )
    }

}