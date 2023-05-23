package com.littletrickster.scanner

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.SparseArray
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.core.content.FileProvider
import androidx.core.util.forEach
import androidx.core.util.size
import androidx.exifinterface.media.ExifInterface
import com.google.common.util.concurrent.ListenableFuture
import com.tom_roush.pdfbox.io.IOUtils
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.graphics.image.JPEGFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.yield
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc
import java.io.File
import java.nio.ByteBuffer
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine


fun File.rotate90Degrees() {
    val exifInterface = ExifInterface(this.absolutePath)

    val orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)

    val newOrientation = when (orientation) {
        ExifInterface.ORIENTATION_NORMAL, ExifInterface.ORIENTATION_UNDEFINED -> ExifInterface.ORIENTATION_ROTATE_90
        ExifInterface.ORIENTATION_ROTATE_90 -> ExifInterface.ORIENTATION_ROTATE_180
        ExifInterface.ORIENTATION_ROTATE_180 -> ExifInterface.ORIENTATION_ROTATE_270
//        ExifInterface.ORIENTATION_ROTATE_270 -> ExifInterface.ORIENTATION_NORMAL
        else -> ExifInterface.ORIENTATION_NORMAL
    }

    exifInterface.setAttribute(ExifInterface.TAG_ORIENTATION, newOrientation.toString())
    exifInterface.saveAttributes()
}


//really slow
fun Bitmap.rotateImage(angle: Float): Bitmap {
    if (angle == 0.0f) return this
    val matrix = Matrix()
    matrix.postRotate(angle)
    return Bitmap.createBitmap(
        this, 0, 0, this.width, this.height, matrix, true
    )
}


//fast resized bitmap might not be precise
fun File.toBitmap(wanted: Int, from: Int = wanted - 100, to: Int = wanted + 100): Pair<Bitmap, Int> {
    val bounds = this.getImageBounds()
    val rotation = bounds.rotation
    return toScaledBitmap(wanted, from, to, bounds.largest()) to rotation
}

fun File.toScaledBitmap(wanted: Int, from: Int = wanted - 100, to: Int = wanted + 100, originalLargestDimension: Int): Bitmap {
    val bitmapOptions = BitmapFactory.Options().apply {
        inScaled = false
    }

    if (originalLargestDimension <= to) {
        return BitmapFactory.decodeFile(absolutePath, bitmapOptions)
    }

    val divider = findDivider(wanted, from.toDouble(), to.toDouble(), originalLargestDimension)
    bitmapOptions.inSampleSize = divider


    val firstBitmap = BitmapFactory.decodeFile(absolutePath, bitmapOptions)


    if (to > firstBitmap.largestDimension) return firstBitmap
    val newDimensions = calcResizedDimensions(firstBitmap.height, firstBitmap.width, wanted.toDouble())

    val finalBitmap = Bitmap.createScaledBitmap(
        firstBitmap, newDimensions.calculatedWidth.toInt(), newDimensions.calculatedHeight.toInt(), true
    )
    if (firstBitmap !== finalBitmap) {
        firstBitmap.recycle()

    }
    return finalBitmap
}

fun File.toBitmap(): Bitmap {
    val bitmapOptions = BitmapFactory.Options().apply {
        inScaled = false
    }
    return BitmapFactory.decodeFile(absolutePath, bitmapOptions)
}


fun File.bitmapRotation(): Int {
    return exifToDegrees(
        ExifInterface(this).getAttributeInt(
            ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL
        )
    )
}

//slow to rotate
fun File.toRotatedBitmap(): Bitmap {
    val degs = this.bitmapRotation()

    val bitmapOptions = BitmapFactory.Options().apply {
        inScaled = false
    }

    val nonRotated = BitmapFactory.decodeFile(this.absolutePath, bitmapOptions)
    val rotated = nonRotated.rotateImage(degs.toFloat())
    return rotated

}

fun clampA4(width: Int, height: Int): Pair<Int, Int> {
    val a4width = 1240f
    val a4height = 1754f

    var flip = width > height

    var scaledWidth: Float
    var scaledHeight: Float
    if (!flip) {
        scaledWidth = width.toFloat()
        scaledHeight = height.toFloat()
    } else {
        scaledWidth = height.toFloat()
        scaledHeight = width.toFloat()
    }

    if (scaledWidth > a4width) {
        val dif = scaledWidth / a4width
        scaledHeight /= dif
        scaledWidth /= dif
    }

    if (scaledHeight > a4height) {
        val dif = scaledHeight / a4height
        scaledHeight /= dif
        scaledWidth /= dif
    }
    return if (!flip) Pair(scaledWidth.toInt(), scaledHeight.toInt())
    else Pair(scaledHeight.toInt(), scaledWidth.toInt())
}

data class ImageBounds(
    val rotatedWidth: Int, val rotatedHeight: Int, val originalWidth: Int, val originalHeight: Int, val rotation: Int
) {
    fun largest(): Int {
        return if (originalHeight > originalWidth) originalHeight
        else originalWidth
    }

    fun clampA4(): ImageBounds {
        val (newOriginalWidth, newOriginalHeight) = clampA4(originalWidth, originalHeight)

        val newRotatedWidth: Int
        val newRotatedHeight: Int

        when (rotation) {
            90, 270 -> {
                newRotatedWidth = newOriginalHeight
                newRotatedHeight = newOriginalWidth
            }
            else -> {
                newRotatedWidth = newOriginalWidth
                newRotatedHeight = newOriginalHeight
            }
        }
        return copy(
            rotatedHeight = newRotatedHeight,
            rotatedWidth = newRotatedWidth,
            originalHeight = newOriginalHeight,
            originalWidth = newOriginalWidth
        )
    }
}


data class ResizedDimensions(
    val calculatedHeight: Double,
    val calculatedWidth: Double,
    val scale: Double,
)

fun calcResizedDimensions(originalHeight: Int, originalWidth: Int, maxDimension: Double): ResizedDimensions {
    val calculatedHeight: Double
    val calculatedWidth: Double
    val scale: Double

    if (originalHeight < maxDimension && originalWidth < maxDimension) {

        return ResizedDimensions(originalHeight.toDouble(), originalWidth.toDouble(), 1.0)

    } else if (originalHeight > originalWidth) {
        calculatedHeight = maxDimension
        calculatedWidth = maxDimension * originalWidth / originalHeight
        scale = originalWidth / calculatedWidth

    } else {
        calculatedWidth = maxDimension
        calculatedHeight = maxDimension * originalHeight / originalWidth
        scale = originalHeight / calculatedHeight
    }

    return ResizedDimensions(calculatedHeight = calculatedHeight, calculatedWidth = calculatedWidth, scale = scale)

}

//this is weird
fun findDivider(wanted: Int, from: Double, to: Double, original: Int): Int {
    var divider = findDivider(wanted, original)
    while (true) {
        val current = original / divider.toDouble()
        if (current in from..to) break
        if (current < from) {
            divider--
            break
        }
        divider++
    }
    return divider
}

fun findDivider(wanted: Int, original: Int): Int {
    var divider = 1

    while (true) {
        val nextDivider = divider * 2
        val divided = original / nextDivider
        if (divided < wanted) break

        divider = nextDivider
    }

    return divider
}

fun Bitmap.getImageBounds(degrees: Int): ImageBounds {
    return calculateImageBounds(degrees, height, width)
}

fun File.getImageBounds(): ImageBounds {
    val degrees = this.bitmapRotation()
    val bitmapOptions = BitmapFactory.Options().apply {
        inJustDecodeBounds = true
        inScaled = true
    }

    BitmapFactory.decodeFile(this.absolutePath, bitmapOptions)

    val imageWidth = bitmapOptions.outWidth
    val imageHeight = bitmapOptions.outHeight

    return calculateImageBounds(degrees, imageHeight, imageWidth)
}

private fun calculateImageBounds(
    degrees: Int, imageHeight: Int, imageWidth: Int
): ImageBounds {
    return if (degrees % 180 == 90) {
        ImageBounds(
            rotatedWidth = imageHeight,
            rotatedHeight = imageWidth,
            originalWidth = imageWidth,
            originalHeight = imageHeight,
            rotation = degrees
        )
    } else ImageBounds(
        rotatedWidth = imageWidth,
        rotatedHeight = imageHeight,
        originalWidth = imageWidth,
        originalHeight = imageHeight,
        rotation = degrees
    )
}

fun exifToDegrees(exifOrientation: Int): Int = when (exifOrientation) {
    ExifInterface.ORIENTATION_ROTATE_90 -> 90
    ExifInterface.ORIENTATION_ROTATE_180 -> 180
    ExifInterface.ORIENTATION_ROTATE_270 -> 270
    else -> 0
}


val Canvas.largestDimension: Int
    get() {
        return if (width > height) width
        else height
    }

val Bitmap.largestDimension: Int
    get() {
        return if (width > height) width
        else height
    }

private val IOExecutor = Dispatchers.IO.asExecutor()
private val DefaultExecutor = Dispatchers.Default.asExecutor()

@Suppress("BlockingMethodInNonBlockingContext")
suspend fun <T> ListenableFuture<T>.await(): T {
    return suspendCancellableCoroutine<T> {
        it.invokeOnCancellation { this.cancel(true) }
        this@await.addListener({
            it.resume(this@await.get())
        }, IOExecutor)
    }
}

suspend fun ImageCapture.takePicture(folder: File, name: String): File {
    return takePicture("${folder.path}/$name")
}

suspend fun ImageCapture.takePicture(filePath: String): File {
    val file = File(filePath)
    val outputFileOptions: ImageCapture.OutputFileOptions = ImageCapture.OutputFileOptions.Builder(file).build()
    return suspendCoroutine<File> {
        takePicture(outputFileOptions, DefaultExecutor, object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                it.resume(file)
            }

            override fun onError(exception: ImageCaptureException) {
                it.resumeWithException(exception)
            }
        })

    }

}

suspend fun ImageCapture.getImage(): ImageProxy {
    return suspendCoroutine<ImageProxy> {
        takePicture(DefaultExecutor, object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(imageProxy: ImageProxy) {
                it.resume(imageProxy)
            }

            override fun onError(exception: ImageCaptureException) {
                it.resumeWithException(exception)
            }
        })
    }
}


fun File.getFirstPageBitmap(maxDimension: Int = 0): Bitmap? {

    try {
        val pdfRenderer = PdfRenderer(ParcelFileDescriptor.open(this, ParcelFileDescriptor.MODE_READ_ONLY))
        val pageCount = pdfRenderer.pageCount
        if (pageCount != 0) {
            val page = pdfRenderer.openPage(0)

            val calculatedHeight: Int
            val calculatedWidth: Int

            when {
                maxDimension == 0 -> {
                    calculatedHeight = page.height
                    calculatedWidth = page.width
                }
                page.height > page.width -> {
                    calculatedHeight = maxDimension
                    calculatedWidth = maxDimension * page.width / page.height


                }
                else -> {
                    calculatedWidth = maxDimension
                    calculatedHeight = maxDimension * page.height / page.width
                }
            }

            val bitmap = Bitmap.createBitmap(calculatedWidth, calculatedHeight, Bitmap.Config.ARGB_8888)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            return bitmap
        }
        return null
    } catch (e: Exception) {
        return null
    }

}

fun File.getFirstPageByteBuffer(): ByteBuffer? {
    val bitmap = getFirstPageBitmap()
    return if (bitmap != null) {
        val byteBuffer = ByteBuffer.allocate(bitmap.byteCount)
        bitmap.copyPixelsToBuffer(byteBuffer)
        byteBuffer
    } else {
        null
    }

}

fun Context.tempFolder(): File {
    return File(filesDir, "temp_folder").also(File::mkdirs)
}

fun Context.getImageFolder(): File {
    return File(filesDir, "scan_images").also(File::mkdirs)
}

fun Context.getEffectImageFolder(): File {
    return File(filesDir, "scan_effect-images").also(File::mkdirs)
}

fun Context.getUnwrappedImageFolder(): File {
    return File(filesDir, "scan_unwrapped-images").also(File::mkdirs)
}


fun Context.getPdfFolder(): File {
    return File(filesDir, "pdf_files").also(File::mkdirs)
}

fun Context.getPdfThumbnailsFolder(): File {
    return File(cacheDir, "pdf_thumbnails").also(File::mkdirs)
}

fun Context.deletePdf(file: File, extra: String = "") {
    val thumbnailFolder = this.getPdfThumbnailsFolder()
    val cached = File(thumbnailFolder, "${file.name}$extra")
    cached.delete()

    file.delete()
}


fun Context.fileProvider(file: File): Uri {
    return FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".provider", file)
}

fun Context.fileReturn(file: File) {
    val uri = fileProvider(file)
    val intent = Intent()
    intent.data = uri
    intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
    (this as Activity).apply {
        setResult(Activity.RESULT_OK, intent)
        finish()
    }
}

fun Context.viewFile(file: File) {
    val uri = fileProvider(file)
    val intent = Intent(Intent.ACTION_VIEW, uri)
    intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
    startActivity(intent)
}

fun Context.shareFile(file: File, mime: String) {
    val uri = fileProvider(file)
    val intent = Intent(Intent.ACTION_SEND)
    intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
    intent.putExtra(Intent.EXTRA_STREAM, uri)
    intent.type = mime
    startActivity(Intent.createChooser(intent, null))
}

suspend fun File.getOrCreateEffectImageFile(context: Context, mode: Int): File {
    val file = File(context.getEffectImageFolder(), this.name)
    if (!file.exists()) {
        val mat = Mat()
        val rotation = this.bitmapRotation()

        Utils.bitmapToMat(this.toBitmap(), mat)
        yield()
        val next = when (mode) {
            0 -> {
                mat
            }
            1 -> {
                val finalMat = autoBrightness(mat)
                mat.release()
                finalMat
            }
            2 -> {

                val bwMat = Mat()
                Imgproc.cvtColor(mat, bwMat, Imgproc.COLOR_RGB2GRAY)
                val finalMat = autoBrightnessGray(bwMat)
                bwMat.release()
                finalMat
            }
            3 -> {//might work might not unfinished
                val temp = autoBrightness(mat)
                val temp2 = documentMat(temp)
                temp.release()
                val bwMat = Mat()
                Imgproc.cvtColor(temp2, bwMat, Imgproc.COLOR_RGB2GRAY)
                temp2.release()
                bwMat
            }
            else -> {
                val final = autoBrightness(mat)
                mat.release()
                final
            }
        }
        yield()
        val bitmap = next.toBitmap()
        next.release()
        yield()
        file.saveJPEG(bitmap = bitmap, rotation = rotation)

    }
    return file
}

private fun File.writeRotation(rotation: Int) {
    val exifInterface = ExifInterface(this.absolutePath)

    val newOrientation = when (rotation) {
        0 -> ExifInterface.ORIENTATION_NORMAL
        90 -> ExifInterface.ORIENTATION_ROTATE_90
        180 -> ExifInterface.ORIENTATION_ROTATE_180
        else -> ExifInterface.ORIENTATION_ROTATE_270
    }

    exifInterface.setAttribute(ExifInterface.TAG_ORIENTATION, newOrientation.toString())
    exifInterface.saveAttributes()
}

fun File.savePNG(bitmap: Bitmap, rotation: Int = 0) {
    this.outputStream().use {
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        it.flush()
    }
    this.writeRotation(rotation)
}


fun File.saveJPEG(bitmap: Bitmap, quality: Int = 91, rotation: Int = 0) {
    this.outputStream().use {
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, it)
        it.flush()
    }
    if (rotation != 0) this.writeRotation(rotation)
}

fun <E> SparseArray<E>.toList(): List<E> {
    val list = ArrayList<E>(this.size)
    forEach { _, value ->
        list.add(value)
    }
    return list
}

suspend fun generatePDF(context: Context, mode: Int, folderToSave: File, images: List<File>, fileName: String): File? {

    val pdfFile = File(folderToSave, fileName)
    PDDocument().use { document ->

        val files = images.map {
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
            return null
        }

        document.save(pdfFile)
        return pdfFile
    }
}

fun Context.saveToExternal(uri: Uri, file: File) {
    contentResolver.openOutputStream(uri)?.use { outStream ->
        file.inputStream().use { inStream ->
            IOUtils.copy(inStream, outStream)
        }
    }
}

fun lerp(start: Float, stop: Float, amount: Float): Float {
    return (1 - amount) * start + amount * stop
}