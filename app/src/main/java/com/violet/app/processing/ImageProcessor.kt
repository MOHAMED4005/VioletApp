package com.violet.app.processing

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

// ── Helpers ─────────────────────────────────────────────────────

private fun imageFiles(context: Context, folderUri: Uri): List<DocumentFile> {
    val dir = DocumentFile.fromTreeUri(context, folderUri) ?: return emptyList()
    return dir.listFiles()
        .filter { it.isFile && it.type?.startsWith("image/") == true }
        .sortedBy { it.name }
}

private fun loadBitmap(context: Context, file: DocumentFile): Bitmap? =
    try {
        context.contentResolver.openInputStream(file.uri)?.use {
            BitmapFactory.decodeStream(it)
        }
    } catch (e: Exception) { null }

private fun saveBitmap(
    context: Context,
    dir: DocumentFile,
    name: String,
    bmp: Bitmap,
    format: String,
) {
    val mimeType = when (format.uppercase()) {
        "PNG"  -> "image/png"
        "WEBP" -> "image/webp"
        else   -> "image/jpeg"
    }
    val compressFormat = when (format.uppercase()) {
        "PNG"  -> Bitmap.CompressFormat.PNG
        "WEBP" -> Bitmap.CompressFormat.WEBP_LOSSLESS
        else   -> Bitmap.CompressFormat.JPEG
    }
    val ext = format.lowercase()
    val file = dir.createFile(mimeType, name) ?: return
    context.contentResolver.openOutputStream(file.uri)?.use { out ->
        bmp.compress(compressFormat, 95, out)
    }
}

private fun verticalMerge(bitmaps: List<Bitmap>): Bitmap {
    val width  = bitmaps.maxOf { it.width }
    val height = bitmaps.sumOf { it.height }
    val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(result)
    var y = 0
    bitmaps.forEach { bmp ->
        canvas.drawBitmap(bmp, 0f, y.toFloat(), null)
        y += bmp.height
    }
    return result
}

private fun pageNum(n: Int) = "%02d".format(n)

// ── 1. Vertimerge — merge by pixels ─────────────────────────────

suspend fun vertimergeByPixels(
    context: Context,
    inputUri: Uri,
    outputUri: Uri,
    targetHeight: Int,
    format: String,
    onProgress: (Float) -> Unit,
) = withContext(Dispatchers.IO) {
    val outDir = DocumentFile.fromTreeUri(context, outputUri)!!
    val subFolders = DocumentFile.fromTreeUri(context, inputUri)!!
        .listFiles().filter { it.isDirectory }.sortedBy { it.name }

    val allFolders = if (subFolders.isEmpty())
        listOf(DocumentFile.fromTreeUri(context, inputUri)!!)
    else subFolders

    allFolders.forEachIndexed { fi, folder ->
        val images = imageFiles(context, folder.uri)
        if (images.isEmpty()) return@forEachIndexed

        val targetDir = if (subFolders.isEmpty()) outDir
        else outDir.createDirectory(folder.name ?: "chapter_$fi")!!

        val queue   = mutableListOf<Bitmap>()
        var current = 0
        var imgIdx  = 1

        images.forEach { imgFile ->
            val bmp = loadBitmap(context, imgFile) ?: return@forEach
            var remaining = bmp.height
            var consumed  = 0

            while (remaining > 0) {
                val space = targetHeight - current
                if (remaining <= space) {
                    queue.add(Bitmap.createBitmap(bmp, 0, consumed, bmp.width, remaining))
                    current += remaining
                    remaining = 0
                } else {
                    queue.add(Bitmap.createBitmap(bmp, 0, consumed, bmp.width, space))
                    val merged = verticalMerge(queue)
                    saveBitmap(context, targetDir, pageNum(imgIdx), merged, format)
                    imgIdx++
                    queue.clear()
                    current   = 0
                    consumed += space
                    remaining -= space
                }
            }
            bmp.recycle()
        }

        if (queue.isNotEmpty()) {
            val merged = verticalMerge(queue)
            saveBitmap(context, targetDir, pageNum(imgIdx), merged, format)
        }

        onProgress((fi + 1f) / allFolders.size)
    }
}

// ── 2. Vertimerge — merge by page count ─────────────────────────

suspend fun vertimergeByPages(
    context: Context,
    inputUri: Uri,
    outputUri: Uri,
    pageCount: Int,
    saveAsZip: Boolean,
    format: String,
    onProgress: (Float) -> Unit,
) = withContext(Dispatchers.IO) {
    val outDir = DocumentFile.fromTreeUri(context, outputUri)!!
    val subFolders = DocumentFile.fromTreeUri(context, inputUri)!!
        .listFiles().filter { it.isDirectory }.sortedBy { it.name }

    val allFolders = if (subFolders.isEmpty())
        listOf(DocumentFile.fromTreeUri(context, inputUri)!!)
    else subFolders

    allFolders.forEachIndexed { fi, folder ->
        val images = imageFiles(context, folder.uri)
        if (images.isEmpty()) return@forEachIndexed

        val bitmaps = images.mapNotNull { loadBitmap(context, it) }
        if (bitmaps.isEmpty()) return@forEachIndexed

        val merged = verticalMerge(bitmaps)
        bitmaps.forEach { it.recycle() }

        val pieceH = merged.height / pageCount
        val pieces  = mutableListOf<Pair<String, Bitmap>>()

        for (i in 0 until pageCount) {
            val top    = i * pieceH
            val bottom = if (i == pageCount - 1) merged.height else (i + 1) * pieceH
            val piece  = Bitmap.createBitmap(merged, 0, top, merged.width, bottom - top)
            pieces.add(Pair(pageNum(i + 1), piece))
        }
        merged.recycle()

        val chapterName = folder.name ?: "chapter_$fi"

        if (saveAsZip) {
            // Write pieces to a ZIP in output folder
            val zipFile = outDir.createFile("application/zip", "$chapterName.zip")!!
            context.contentResolver.openOutputStream(zipFile.uri)?.use { os ->
                ZipOutputStream(os).use { zos ->
                    val compressFormat = when (format.uppercase()) {
                        "PNG"  -> Bitmap.CompressFormat.PNG
                        "WEBP" -> Bitmap.CompressFormat.WEBP_LOSSLESS
                        else   -> Bitmap.CompressFormat.JPEG
                    }
                    pieces.forEach { (name, bmp) ->
                        zos.putNextEntry(ZipEntry("$name.${format.lowercase()}"))
                        bmp.compress(compressFormat, 95, zos)
                        zos.closeEntry()
                        bmp.recycle()
                    }
                }
            }
        } else {
            val targetDir = if (subFolders.isEmpty()) outDir
            else outDir.createDirectory(chapterName)!!
            pieces.forEach { (name, bmp) ->
                saveBitmap(context, targetDir, name, bmp, format)
                bmp.recycle()
            }
        }

        onProgress((fi + 1f) / allFolders.size)
    }
}

// ── 3. Zip Maker ─────────────────────────────────────────────────

suspend fun makeZips(
    context: Context,
    inputUri: Uri,
    outputUri: Uri,
    onProgress: (Float) -> Unit,
) = withContext(Dispatchers.IO) {
    val inputDir  = DocumentFile.fromTreeUri(context, inputUri)!!
    val outputDir = DocumentFile.fromTreeUri(context, outputUri)!!

    val chapters = inputDir.listFiles().filter { it.isDirectory }.sortedBy { it.name }
    if (chapters.isEmpty()) return@withContext

    chapters.forEachIndexed { idx, chapter ->
        val zipFile = outputDir.createFile("application/zip", "${chapter.name}.zip")!!
        context.contentResolver.openOutputStream(zipFile.uri)?.use { os ->
            ZipOutputStream(os).use { zos ->
                chapter.listFiles().filter { it.isFile }.sortedBy { it.name }.forEach { file ->
                    zos.putNextEntry(ZipEntry(file.name ?: "file"))
                    context.contentResolver.openInputStream(file.uri)?.use { ins ->
                        ins.copyTo(zos)
                    }
                    zos.closeEntry()
                }
            }
        }
        onProgress((idx + 1f) / chapters.size)
    }
}

// ── 4. Watermark ─────────────────────────────────────────────────

private fun applyWatermark(page: Bitmap, logo: Bitmap, opacity: Float): Bitmap {
    val result = page.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(result)
    val paint  = Paint().apply { alpha = (opacity * 255).toInt() }

    // Scale logo to half page width
    val lw   = result.width / 2
    val lh   = (lw * logo.height.toFloat() / logo.width).toInt()
    val scaled = Bitmap.createScaledBitmap(logo, lw, lh, true)

    // Find first white zone from top-left
    val topMarginPx    = (result.height * 5f / 29.7f).toInt()
    val bottomMarginPx = (result.height * 5f / 29.7f).toInt()

    fun isWhiteAt(y: Int): Boolean {
        if (y + lh > result.height) return false
        for (row in y until y + lh) {
            for (col in 0 until lw) {
                val pixel = result.getPixel(col, row)
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8)  and 0xFF
                val b =  pixel         and 0xFF
                if (r < 245 || g < 245 || b < 245) return false
            }
        }
        return true
    }

    // Place at top white zone
    var placed = 0
    for (y in topMarginPx until (result.height / 2 - lh)) {
        if (isWhiteAt(y)) {
            canvas.drawBitmap(scaled, 0f, y.toFloat(), paint)
            placed++
            break
        }
    }

    // Place at bottom white zone
    val bottomStart = result.height / 2
    val bottomEnd   = result.height - bottomMarginPx - lh
    for (y in bottomStart until bottomEnd) {
        if (isWhiteAt(y)) {
            canvas.drawBitmap(scaled, 0f, y.toFloat(), paint)
            placed++
            break
        }
    }

    scaled.recycle()
    return result
}

suspend fun applyWatermarks(
    context: Context,
    startUri: Uri,
    endUri: Uri,
    logoUri: Uri,
    chapterUri: Uri,
    outputUri: Uri,
    onProgress: (Float) -> Unit,
) = withContext(Dispatchers.IO) {
    val logo = context.contentResolver.openInputStream(logoUri)?.use {
        BitmapFactory.decodeStream(it)
    } ?: return@withContext

    val startBmp = context.contentResolver.openInputStream(startUri)?.use {
        BitmapFactory.decodeStream(it)
    }
    val endBmp = context.contentResolver.openInputStream(endUri)?.use {
        BitmapFactory.decodeStream(it)
    }

    val outDir   = DocumentFile.fromTreeUri(context, outputUri)!!
    val chapters = DocumentFile.fromTreeUri(context, chapterUri)!!
        .listFiles().filter { it.isDirectory }.sortedBy { it.name }

    val allFolders = if (chapters.isEmpty())
        listOf(DocumentFile.fromTreeUri(context, chapterUri)!!)
    else chapters

    allFolders.forEachIndexed { fi, folder ->
        val chImages = imageFiles(context, folder.uri)
        val allPages = mutableListOf<Pair<String, Bitmap?>>()

        // Build: start + chapter pages + end
        startBmp?.let { allPages.add("start" to it) }
        chImages.forEach { allPages.add(it.name!! to loadBitmap(context, it)) }
        endBmp?.let { allPages.add("end" to it) }

        val total   = allPages.size
        val chName  = folder.name ?: "chapter_$fi"
        val chOutDir = if (chapters.isEmpty()) outDir else outDir.createDirectory(chName)!!

        allPages.forEachIndexed { pageIdx, (_, bmp) ->
            if (bmp == null) return@forEachIndexed
            val name = pageNum(pageIdx + 1)
            val isEdge = pageIdx == 0 || pageIdx == total - 1
            val output = if (isEdge) bmp else applyWatermark(bmp, logo, 0.5f)
            saveBitmap(context, chOutDir, name, output, "JPG")
            if (!isEdge) output.recycle()
        }

        onProgress((fi + 1f) / allFolders.size)
    }

    logo.recycle()
}

// ── 5. Photo Format Changer ──────────────────────────────────────

suspend fun convertImageFormat(
    context: Context,
    inputUri: Uri,
    outputUri: Uri,
    targetFormat: String,
    onProgress: (Float) -> Unit,
) = withContext(Dispatchers.IO) {
    val images = imageFiles(context, inputUri)
    val outDir = DocumentFile.fromTreeUri(context, outputUri)!!

    images.forEachIndexed { idx, file ->
        val bmp = loadBitmap(context, file) ?: return@forEachIndexed
        val baseName = file.name?.substringBeforeLast('.') ?: "image_$idx"
        saveBitmap(context, outDir, baseName, bmp, targetFormat)
        bmp.recycle()
        onProgress((idx + 1f) / images.size)
    }
}
