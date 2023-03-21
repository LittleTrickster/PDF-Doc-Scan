package com.littletrickster.scanner

import android.app.Activity
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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

    var fileToSave: File? by remember { mutableStateOf(null) }

    val saveExternalLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) {
            val uri = it.data?.data ?: return@rememberLauncherForActivityResult
            context.saveToExternal(uri, fileToSave ?: return@rememberLauncherForActivityResult)
            Toast.makeText(context, context.getString(R.string.saved), Toast.LENGTH_SHORT).show()
        }
        fileToSave = null
    }


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
                PdfLine(file, time, saveExternal = {
                    fileToSave = it
                    val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
                    intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    intent.putExtra(Intent.EXTRA_TITLE, it.name)
                    intent.type = "application/pdf"
                    saveExternalLauncher.launch(intent)
                })
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
private fun PdfLine(file: File, time: LocalTime, saveExternal: (file: File) -> Unit = {}) {
    val context = LocalContext.current

    val isPick = LocalIsPick.current
    val bitmap by if (!LocalInspectionMode.current) rememberPdfThumbnail(file)
    else remember { mutableStateOf(null) }


    var contextMenuExpanded by remember { mutableStateOf(false) }

    var tryingToDelete by remember { mutableStateOf(false) }

    Card(modifier = Modifier
        .clickable {
            if (isPick) {
                context.fileReturn(file)

            } else {
                context.viewFile(file)
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
                DropdownMenu(expanded = contextMenuExpanded, onDismissRequest = { contextMenuExpanded = false }) {

                    DropdownMenuItem(onClick = {
                        tryingToDelete = true
                    }, content = {
                        Text(text = stringResource(id = R.string.deletus))
                    })
                    DropdownMenuItem(onClick = {
                        context.shareFile(file, "application/pdf")
                        contextMenuExpanded = false

                    }, content = {
                        Text(text = stringResource(R.string.share))
                    })
                    DropdownMenuItem(onClick = {
                        saveExternal(file)
                        contextMenuExpanded = false
                    }, content = {
                        Text(text = stringResource(R.string.save_to))
                    })

                }
            }


        }
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