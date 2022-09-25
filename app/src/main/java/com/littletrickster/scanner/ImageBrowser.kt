package com.littletrickster.scanner

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.core.content.edit
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.calculateCurrentOffsetForPage
import com.google.accompanist.pager.rememberPagerState
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.graphics.image.JPEGFactory
import kotlinx.coroutines.*
import java.io.File
import java.io.FileNotFoundException
import java.time.LocalDateTime
import kotlin.math.absoluteValue


@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun ImageBrowser(back: () -> Unit) {
    val context = LocalContext.current

    var loading by remember { mutableStateOf(false) }
    var changingEffect by remember { mutableStateOf(false) }
    var changedRotation by remember { mutableStateOf(0) }


    val prefs = rememberScannerSharedPrefs()
    var mode by remember {
        var saved = prefs.getInt("effect_mode", 2)
        if (saved == 3) saved = 2

        mutableStateOf(saved)
    }


    val scope = rememberCoroutineScope()
    val isPick = LocalIsPick.current

    var wantToDeleteAll by remember { mutableStateOf(false) }

    BackHandler(!loading, back)

    val imageFolder = remember {
        context.getImageFolder()
    }
    val unwrappedFolder = remember {
        context.getUnwrappedImageFolder()
    }
    val effectFolder = remember {
        context.getEffectImageFolder()
    }
    val pdfFolder = remember {
        context.getPdfFolder()
    }


    val images by observeFile(imageFolder)
    val unwrapped by observeFile(unwrappedFolder)

    val sortedImages = remember(unwrapped) {
        unwrapped.sortedBy(File::getName)
    }

    val activeImageEffectProcessors = remember { mutableStateMapOf<String, Job>() }

    val pagerState = rememberPagerState(images.size - 1)

    val currentPage = remember(sortedImages, pagerState.currentPage) {
        sortedImages.getOrNull(pagerState.currentPage)
    }


    Scaffold(topBar = {
        TopAppBar(
            title = {
            },
            navigationIcon = {
                IconButton(onClick = { back() }) {
                    Icon(Icons.Filled.ArrowBack, null)
                }
            },
            actions = {


                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {


                    Text(
                        text = stringResource(R.string.save_pdf), textAlign = TextAlign.Center, modifier = Modifier
                            .clickable(enabled = !loading) {

                                loading = true

                                scope.launch(Dispatchers.Default) {
                                    activeImageEffectProcessors.values.joinAll()

                                    val pdfFile = File(pdfFolder, "${LocalDateTime.now()}.pdf")
                                    PDDocument().use { document ->

                                        val files = sortedImages.map {
                                            it.getOrCreateEffectImageFile(context, mode)
                                        }

                                        files.forEach { file ->

                                            val bounds = file.getImageBounds()
                                            val rotation = bounds.rotation

                                            val a4LikeBounds = bounds.clampA4()

                                            file.inputStream()
                                                .use { fileStream ->
                                                    val image = JPEGFactory.createFromStream(document, fileStream)

                                                    val page = PDPage(
                                                        PDRectangle(
                                                            a4LikeBounds.originalWidth.toFloat(),
                                                            a4LikeBounds.originalHeight.toFloat()
                                                        )
                                                    )
                                                    document.addPage(page)
                                                    page.rotation = rotation

                                                    PDPageContentStream(document, page).use { contentStream ->
                                                        contentStream.drawImage(
                                                            image,
                                                            0f,
                                                            0f,
                                                            a4LikeBounds.originalWidth.toFloat(),
                                                            a4LikeBounds.originalHeight.toFloat()
                                                        )
                                                    }
                                                }
                                        }

                                        val created = pdfFile.createNewFile()
                                        if (!created) {
                                            loading = false
                                            return@launch
                                        }

                                        document.save(pdfFile)
                                    }

                                    images.forEach(File::delete)
                                    unwrapped.forEach(File::delete)
                                    effectFolder.listFiles()!!.forEach(File::delete)

                                    withContext(Dispatchers.Main) {
                                        Toast
                                            .makeText(context, "Ok", Toast.LENGTH_SHORT)
                                            .show()
                                    }
                                    if (isPick) {
                                        val uri = context.fileProvider(pdfFile)
                                        val intent = Intent()
                                        intent.data = uri
                                        intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                                        (context as Activity).apply {
                                            setResult(Activity.RESULT_OK, intent)
                                            finish()
                                        }
                                    } else {
                                        back()
                                    }
                                    loading = false
                                }


                            }
                    )
                }

            }

        )
    }
    ) {
        ConstraintLayout(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            val (image, bottomBar) = createRefs()
            DisableOverscroll {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.constrainAs(image) {
                        top.linkTo(parent.top)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                        bottom.linkTo(bottomBar.top)
                        height = Dimension.fillToConstraints
                    },
//            modifier = Modifier.size(600.dp,300.dp),
                    contentPadding = PaddingValues(all = 20.dp),
                    count = sortedImages.size
                ) { page ->

                    val imageFile = sortedImages[page]
                    val zoomScale by remember { mutableStateOf(1f) }



                    Box(
                        Modifier
                            .graphicsLayer {
                                // Calculate the absolute offset for the current page from the
                                // scroll position. We use the absolute value which allows us to mirror
                                // any effects for both directions
                                val pageOffset = calculateCurrentOffsetForPage(page).absoluteValue

                                // We animate the scaleX + scaleY, between 85% and 100%
                                lerp(0.85f, 1f, 1f - pageOffset.coerceIn(0f, 1f))
                                    .also { scale ->
                                        scaleX = scale * zoomScale
                                        scaleY = scale * zoomScale
                                    }

                                // We animate the alpha, between 50% and 100%
                                alpha = lerp(0.5f, 1f, 1f - pageOffset.coerceIn(0f, 1f))

                            }
                    ) {

                        var imageLoading by remember { mutableStateOf(true) }

                        var file by remember { mutableStateOf<File?>(null) }

                        LaunchedEffect(imageFile, mode, changedRotation) {
                            imageLoading = true
                            try {
                                for (r in 0..3) {
                                    try {
                                        val job = async(Dispatchers.Default) {
                                            file = imageFile.getOrCreateEffectImageFile(context, mode)
                                        }
                                        activeImageEffectProcessors[imageFile.name] = job
                                        //might throw no such file rare exception
                                        job.join()
                                        break
                                    } catch (e: FileNotFoundException) { //todo synchronize and test more
                                        if (r == 2) {
                                            imageFile.delete() //unwrapped
                                            File(imageFolder, imageFile.name).delete() //original
                                            File(effectFolder, imageFile.name).delete() //effect

//                                          CrashWrapper.recordCrash(e)

                                            Toast.makeText(context, context.getString(R.string.error), Toast.LENGTH_SHORT).show()
                                            if (unwrappedFolder.listFiles()!!.isEmpty()) {
                                                back()
                                            }
                                        }
                                        delay(1_000)
                                    }
                                }

                                imageLoading = false
                            } finally {
                                activeImageEffectProcessors.remove(imageFile.name)
                            }
                        }


                        MyImage(
                            modifier = Modifier
                                .padding(5.dp)
                                .fillMaxSize()
                                .clickable {
                                    file?.also { file ->

                                        val uri = context.fileProvider(file)
                                        val intent = Intent(Intent.ACTION_VIEW, uri)
                                        intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                                        context.startActivity(intent)

//                                        val intent = Intent(context, ImageViewerActivity::class.java)
//                                        intent.putExtra("simple", true)
//                                        intent.putExtra("url", file.path)
//                                        context.startActivity(intent)

                                    }
                                },
                            file = file,
                            processing = loading or imageLoading,
                            update = changedRotation + mode
                        )

                        if (file == null || imageLoading) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }

                        CloseButton {
                            try {
                                imageFile.delete()
                                File(imageFolder, imageFile.name).delete()
                                File(effectFolder, imageFile.name).delete()
                            } catch (e: Exception) {//Todo
                            }
                            if (unwrappedFolder.listFiles()!!.isEmpty()) {
                                back()
                            }
                        }
                    }

                }
            }
            Column(
                modifier = Modifier
                    .constrainAs(bottomBar) {
                        bottom.linkTo(parent.bottom)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                    }
                    .fillMaxWidth(),

                ) {


                Row(
                    modifier = Modifier
                        .padding(bottom = 10.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        modifier = Modifier
                            .background(MaterialTheme.colors.primary, shape = CircleShape)
                            .padding(vertical = 5.dp, horizontal = 8.dp),
                        text = "${pagerState.currentPage + 1}/${sortedImages.size}"
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colors.background),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {


//                IconButton(onClick = {
//                }) {
//                    Icon(
//                        modifier = Modifier.size(24.dp),
//                        painter = rememberVectorPainter(image = Icons.Default.Add),
//                        contentDescription = null
//                    )
//                }

                    Box(modifier = Modifier.size(24.dp)) {

                    }

//                IconButton(onClick = {
//
//                }) {
//                    Icon(
//                        modifier = Modifier.size(24.dp),
//                        painter = painterResource(id = R.drawable.ic_baseline_crop_24),
//                        contentDescription = null
//                    )
//                }
                    IconButton(onClick = {
                        changingEffect = true
                    }) {
                        Icon(
                            modifier = Modifier.size(24.dp),
                            imageVector = Icons.Filled.Palette,
                            contentDescription = null
                        )
                    }


                    IconButton(onClick = {

                        //todo check if image exist
                        if (currentPage != null && currentPage.exists() && !loading) {
                            loading = true

                            scope.launch(Dispatchers.Default) {

                                //need effect image to exist
                                activeImageEffectProcessors[currentPage.name]?.join()

                                currentPage.rotate90Degrees()
                                File(effectFolder, currentPage.name).rotate90Degrees()
                                changedRotation++
                                loading = false

                            }


//                        Toast.makeText(context,"im here ${currentPage}",Toast.LENGTH_SHORT).show()

                        }


                    }) {
                        Icon(
                            modifier = Modifier.size(24.dp),
                            imageVector = Icons.Default.RotateRight,
                            contentDescription = null
                        )
                    }
                    IconButton(onClick = {
                        wantToDeleteAll = true

//                    back()
                    }) {
                        Icon(
                            modifier = Modifier.size(24.dp),
                            imageVector = Icons.Default.Delete,
                            contentDescription = null
                        )
                    }
                }

            }

            if (wantToDeleteAll) AlertDialog(
                onDismissRequest = { wantToDeleteAll = false },
                confirmButton = {
                    Button(onClick = {
                        images.forEach(File::delete)
                        unwrapped.forEach(File::delete)
                        effectFolder.listFiles()!!.forEach(File::delete)
                        back()
                    }) {
                        Text(stringResource(R.string.yes))
                    }
                },
                dismissButton = {
                    Button(onClick = { wantToDeleteAll = false }) {
                        Text(stringResource(R.string.no))
                    }
                },
                title = { Text(stringResource(R.string.delete_all_images)) }
            )
        }
    }

    if (changingEffect) ChangingEffect(back = { changingEffect = false }, change = {
        changingEffect = false

        if (mode != it) {
            loading = true
            scope.launch(Dispatchers.Default) {
                val activeJobs = activeImageEffectProcessors.values.toList()
                activeJobs.forEach(Job::cancel)
                activeJobs.joinAll()


                prefs.edit {
                    this.putInt("effect_mode", it)
                }
                mode = it

                effectFolder.listFiles()!!.forEach(File::delete)
                loading = false


            }
        }
    })
    LoadingDialog(loading = loading)
}


@Composable
private fun ChangingEffect(back: () -> Unit, change: (nr: Int) -> Unit) {
    Dialog(
        onDismissRequest = { back() },
        DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
    ) {
        Box(
            contentAlignment = Alignment.Center,
        ) {
            LazyColumn {
                for (currentMode in 0..2) {
                    item {
                        Card(modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                change(currentMode)
                            }) {
                            Text(modifier = Modifier.padding(10.dp), text = modeName(currentMode))
                        }
                    }
                }

            }
        }
    }
}


@Composable
fun modeName(mode: Int): String {
    return when (mode) {
        0 -> {
            stringResource(R.string.normal)

        }
        1 -> {
            stringResource(R.string.auto)

        }
        2 -> {
            stringResource(R.string.black_and_white)
        }

        3 -> {
            stringResource(R.string.document)

        }

        else -> {
            "Histogram"
        }

    }
}
