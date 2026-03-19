package com.violet.app.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BrokenImage
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Star
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.*
import com.violet.app.data.PrefsRepository
import com.violet.app.processing.applyWatermarks
import com.violet.app.ui.components.*
import com.violet.app.ui.theme.VioletSuccess
import kotlinx.coroutines.launch

@Composable
fun WatermarkScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs   = remember { PrefsRepository(context) }
    val scope   = rememberCoroutineScope()

    var startUri   by remember { mutableStateOf<Uri?>(null) }
    var endUri     by remember { mutableStateOf<Uri?>(null) }
    var logoUri    by remember { mutableStateOf<Uri?>(null) }
    var chapterUri by remember { mutableStateOf<Uri?>(null) }
    var outputUri  by remember { mutableStateOf<Uri?>(null) }

    var progress  by remember { mutableStateOf(0f) }
    var isRunning by remember { mutableStateOf(false) }
    var isDone    by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { prefs.get(PrefsRepository.WM_START_URI,   "").collect { if (it.isNotBlank()) startUri   = Uri.parse(it) } }
    LaunchedEffect(Unit) { prefs.get(PrefsRepository.WM_END_URI,     "").collect { if (it.isNotBlank()) endUri     = Uri.parse(it) } }
    LaunchedEffect(Unit) { prefs.get(PrefsRepository.WM_LOGO_URI,    "").collect { if (it.isNotBlank()) logoUri    = Uri.parse(it) } }
    LaunchedEffect(Unit) { prefs.get(PrefsRepository.WM_CHAPTER_URI, "").collect { if (it.isNotBlank()) chapterUri = Uri.parse(it) } }
    LaunchedEffect(Unit) { prefs.get(PrefsRepository.WM_OUTPUT_URI,  "").collect { if (it.isNotBlank()) outputUri  = Uri.parse(it) } }

    fun savePrefs() = scope.launch {
        prefs.save(PrefsRepository.WM_START_URI,   startUri?.toString()   ?: "")
        prefs.save(PrefsRepository.WM_END_URI,     endUri?.toString()     ?: "")
        prefs.save(PrefsRepository.WM_LOGO_URI,    logoUri?.toString()    ?: "")
        prefs.save(PrefsRepository.WM_CHAPTER_URI, chapterUri?.toString() ?: "")
        prefs.save(PrefsRepository.WM_OUTPUT_URI,  outputUri?.toString()  ?: "")
    }

    val canStart = startUri != null && endUri != null && logoUri != null &&
            chapterUri != null && outputUri != null && !isRunning

    ScreenScaffold(
        title    = "Watermark",
        subtitle = "Stamp your logo onto chapter pages",
        icon     = Icons.Rounded.BrokenImage,
        onBack   = onBack,
    ) {
        SectionLabel("Start Page")
        FilePickerRow(
            label    = "Start Page Image",
            uri      = startUri,
            onPicked = { startUri = it; savePrefs() },
            icon     = Icons.Rounded.Image,
        )

        SectionLabel("End Page")
        FilePickerRow(
            label    = "End Page Image",
            uri      = endUri,
            onPicked = { endUri = it; savePrefs() },
            icon     = Icons.Rounded.Image,
        )

        SectionLabel("Logo / Watermark")
        FilePickerRow(
            label    = "Logo Image File",
            uri      = logoUri,
            onPicked = { logoUri = it; savePrefs() },
            icon     = Icons.Rounded.Star,
        )

        SectionLabel("Chapter Folder")
        FolderPickerRow(
            label    = "Chapter Images Folder",
            uri      = chapterUri,
            onPicked = { chapterUri = it; savePrefs() },
        )

        SectionLabel("Output")
        FolderPickerRow(
            label    = "Output Folder",
            uri      = outputUri,
            onPicked = { outputUri = it; savePrefs() },
        )

        if (isRunning || isDone) {
            VioletProgressBar(progress = progress)
            if (isDone) {
                Text(
                    text  = "✓  Done! Watermarked pages saved.",
                    style = MaterialTheme.typography.bodyMedium.copy(color = VioletSuccess),
                )
            }
        }

        StartButton(
            enabled = canStart,
            loading = isRunning,
            onClick = {
                scope.launch {
                    isRunning = true
                    isDone    = false
                    progress  = 0f
                    try {
                        applyWatermarks(
                            context    = context,
                            startUri   = startUri!!,
                            endUri     = endUri!!,
                            logoUri    = logoUri!!,
                            chapterUri = chapterUri!!,
                            outputUri  = outputUri!!,
                            onProgress = { progress = it },
                        )
                        progress = 1f
                        isDone   = true
                    } catch (e: Exception) {
                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    } finally {
                        isRunning = false
                    }
                }
            },
        )
    }
}
