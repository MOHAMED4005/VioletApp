package app.violet.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

// ─────────────────────────────────────────────────────────────────────────────
// Shared helpers
// ─────────────────────────────────────────────────────────────────────────────

private val SUPPORTED = setOf("jpg", "jpeg", "png", "webp", "bmp")

fun File.imageFiles(): List<File> =
    listFiles()
        ?.filter { it.isFile && it.extension.lowercase() in SUPPORTED }
        ?.sortedBy { it.name }
        ?: emptyList()

fun File.subDirs(): List<File> =
    listFiles()?.filter { it.isDirectory }?.sortedBy { it.name } ?: emptyList()

fun Bitmap.CompressFormat.ext(): String = when (this) {
    Bitmap.CompressFormat.PNG  -> "png"
    Bitmap.CompressFormat.WEBP_LOSSLESS,
    Bitmap.CompressFormat.WEBP_LOSSY -> "webp"
    else                       -> "jpg"
}

fun formatFromString(s: String): Bitmap.CompressFormat = when (s.uppercase()) {
    "PNG"  -> Bitmap.CompressFormat.PNG
    "WEBP" -> Bitmap.CompressFormat.WEBP_LOSSY
    else   -> Bitmap.CompressFormat.JPEG
}

fun Bitmap.saveTo(file: File, format: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG) {
    FileOutputStream(file).use { out ->
        compress(format, 95, out)
    }
}

fun loadBitmap(file: File): Bitmap =
    BitmapFactory.decodeFile(file.absolutePath)
        ?: throw IllegalArgumentException("Cannot decode: ${file.name}")

// ─────────────────────────────────────────────────────────────────────────────
// 1. VERTIMERGE — by pixel height
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Merges images in [images] into fixed-height slices of [targetHeight] pixels.
 * Output files are named 01.ext, 02.ext, …
 */
fun mergeByPixels(
    images: List<File>,
    outDir: File,
    targetHeight: Int,
    format: Bitmap.CompressFormat,
    onProgress: (Int, Int) -> Unit   // (current, total) in terms of input images
) {
    val ext = format.ext()
    val parts = mutableListOf<Bitmap>()
    var currentH = 0
    var count = 1
    val total = images.size

    images.forEachIndexed { idx, imgFile ->
        onProgress(idx, total)
        val bmp = loadBitmap(imgFile)
        val W = bmp.width
        val H = bmp.height
        var usedTop = 0
        var remaining = H

        while (remaining > 0) {
            val space = targetHeight - currentH
            if (remaining <= space) {
                parts.add(Bitmap.createBitmap(bmp, 0, usedTop, W, remaining))
                currentH += remaining
                remaining = 0
            } else {
                parts.add(Bitmap.createBitmap(bmp, 0, usedTop, W, space))
                // flush
                val canvas = Bitmap.createBitmap(W, targetHeight, Bitmap.Config.RGB_565)
                val c = Canvas(canvas)
                var y = 0
                for (p in parts) { c.drawBitmap(p, 0f, y.toFloat(), null); y += p.height }
                canvas.saveTo(File(outDir, "%02d.$ext".format(count)), format)
                count++
                parts.clear()
                currentH = 0
                usedTop += space
                remaining -= space
            }
        }
        bmp.recycle()
    }

    if (parts.isNotEmpty()) {
        val totalH = parts.sumOf { it.height }
        val W = parts[0].width
        val canvas = Bitmap.createBitmap(W, totalH, Bitmap.Config.RGB_565)
        val c = Canvas(canvas)
        var y = 0
        for (p in parts) { c.drawBitmap(p, 0f, y.toFloat(), null); y += p.height }
        canvas.saveTo(File(outDir, "%02d.$ext".format(count)), format)
    }
    onProgress(total, total)
}

// ─────────────────────────────────────────────────────────────────────────────
// 1b. VERTIMERGE — by page count
// ─────────────────────────────────────────────────────────────────────────────

fun mergeByPages(
    images: List<File>,
    outDir: File,
    pageCount: Int,
    format: Bitmap.CompressFormat,
    saveAsZip: Boolean,
    chapterName: String,
    onProgress: (Int, Int) -> Unit
) {
    val ext  = format.ext()
    val total = images.size
    onProgress(0, total)

    // Load and stitch all into one tall bitmap
    val loaded = images.map { loadBitmap(it) }
    val maxW   = loaded.maxOf { it.width }
    val totalH = loaded.sumOf { it.height }

    val strip  = Bitmap.createBitmap(maxW, totalH, Bitmap.Config.RGB_565)
    val canvas = Canvas(strip)
    var y = 0
    loaded.forEach { b -> canvas.drawBitmap(b, 0f, y.toFloat(), null); y += b.height; b.recycle() }
    onProgress(total / 2, total)

    val pieceH = totalH / pageCount
    val outFiles = mutableListOf<File>()

    for (i in 0 until pageCount) {
        val top    = i * pieceH
        val bottom = if (i == pageCount - 1) totalH else (i + 1) * pieceH
        val part   = Bitmap.createBitmap(strip, 0, top, maxW, bottom - top)
        val file   = File(outDir, "%02d.$ext".format(i + 1))
        part.saveTo(file, format)
        part.recycle()
        outFiles.add(file)
    }
    strip.recycle()

    if (saveAsZip) {
        val zipFile = File(outDir, "$chapterName.zip")
        ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
            outFiles.forEach { f ->
                zos.putNextEntry(ZipEntry(f.name))
                f.inputStream().use { it.copyTo(zos) }
                zos.closeEntry()
            }
        }
        outFiles.forEach { it.delete() }
    }
    onProgress(total, total)
}

// ─────────────────────────────────────────────────────────────────────────────
// 2. ZIP MAKER
// ─────────────────────────────────────────────────────────────────────────────

fun makeZip(
    images: List<File>,
    zipFile: File,
    onProgress: (Int, Int) -> Unit
) {
    val total = images.size
    ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
        images.forEachIndexed { i, file ->
            onProgress(i, total)
            val entryName = "%02d.${file.extension}".format(i + 1)
            zos.putNextEntry(ZipEntry(entryName))
            file.inputStream().use { it.copyTo(zos) }
            zos.closeEntry()
        }
    }
    onProgress(total, total)
}

// ─────────────────────────────────────────────────────────────────────────────
// 3. WATERMARK
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Stamps [logo] at 50% opacity onto white zones of [page].
 * Logo is resized to half the page width, placed in the first
 * fully-white horizontal strip found within the margin zone.
 */
fun stampWatermark(page: Bitmap, logo: Bitmap): Bitmap {
    val W = page.width
    val H = page.height

    val logoW = W / 2
    val logoH = (logoW.toFloat() * logo.height / logo.width).toInt()
    val logoScaled = Bitmap.createScaledBitmap(logo, logoW, logoH, true)

    val marginPx = (H * 5f / 29.7f).toInt()

    val result = page.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(result)
    val paint  = Paint().apply { alpha = 128 }  // 50% opacity

    for (y in marginPx..(H - marginPx - logoH)) {
        // Sample the leftmost logoW columns in the logo zone
        var allWhite = true
        outer@ for (row in y until y + logoH) {
            for (col in 0 until minOf(logoW, W)) {
                val pixel = page.getPixel(col, row)
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8)  and 0xFF
                val b =  pixel         and 0xFF
                if (r < 245 || g < 245 || b < 245) {
                    allWhite = false
                    break@outer
                }
            }
        }
        if (allWhite) {
            canvas.drawBitmap(logoScaled, 0f, y.toFloat(), paint)
            break
        }
    }
    logoScaled.recycle()
    return result
}

// ─────────────────────────────────────────────────────────────────────────────
// 4. FORMAT CHANGER
// ─────────────────────────────────────────────────────────────────────────────

fun convertFormat(
    images: List<File>,
    outDir: File,
    format: Bitmap.CompressFormat,
    onProgress: (Int, Int) -> Unit
) {
    val ext   = format.ext()
    val total = images.size
    images.forEachIndexed { i, file ->
        onProgress(i, total)
        val bmp  = loadBitmap(file)
        val out  = File(outDir, "${file.nameWithoutExtension}.$ext")
        bmp.saveTo(out, format)
        bmp.recycle()
    }
    onProgress(total, total)
}
