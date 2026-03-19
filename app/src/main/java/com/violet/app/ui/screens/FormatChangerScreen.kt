package com.violet.app.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PhotoSizeSelectLarge
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.violet.app.data.PrefsRepository
import com.violet.app.processing.convertImageFormat
import com.violet.app.ui.components.*
import com.violet.app.ui.theme.VioletSuccess
import kotlinx.coroutines.launch

@Composable
fun FormatChangerScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs   = remember { PrefsRepository(context) }
    val scope   = rememberCoroutineScope()

    var inputUri  by remember { mutableStateOf<Uri?>(null) }
    var outputUri by remember { mutableStateOf<Uri?>(null) }
    var format    by remember { mutableStateOf("JPG") }

    var progress  by remember { mutableStateOf(0f) }
    var isRunning by remember { mutableStateOf(false) }
    var isDone    by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { prefs.get(PrefsRepository.PFC_INPUT_URI,  "").collect { if (it.isNotBlank()) inputUri  = Uri.parse(it) } }
    LaunchedEffect(Unit) { prefs.get(PrefsRepository.PFC_OUTPUT_URI, "").collect { if (it.isNotBlank()) outputUri = Uri.parse(it) } }
    LaunchedEffect(Unit) { prefs.get(PrefsRepository.PFC_FORMAT, "JPG").collect { format = it } }

    fun savePrefs() = scope.launch {
        prefs.save(PrefsRepository.PFC_INPUT_URI,  inputUri?.toString()  ?: "")
        prefs.save(PrefsRepository.PFC_OUTPUT_URI, outputUri?.toString() ?: "")
        prefs.save(PrefsRepository.PFC_FORMAT, format)
    }

    val canStart = inputUri != null && outputUri != null && !isRunning

    ScreenScaffold(
        title    = "Format Changer",
        subtitle = "Convert images between JPG, PNG and WEBP",
        icon     = Icons.Rounded.PhotoSizeSelectLarge,
        onBack   = onBack,
    ) {
        SectionLabel("Input")
        FolderPickerRow(
            label    = "Images Folder",
            uri      = inputUri,
            onPicked = { inputUri = it; savePrefs() },
        )

        SectionLabel("Output")
        FolderPickerRow(
            label    = "Output Folder",
            uri      = outputUri,
            onPicked = { outputUri = it; savePrefs() },
        )

        SectionLabel("Target Format")
        FormatSelector(
            selected = format,
            onSelect = { format = it; savePrefs() },
        )

        if (isRunning || isDone) {
            VioletProgressBar(progress = progress)
            if (isDone) {
                Text(
                    text  = "✓  Done! Converted images saved.",
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
                        convertImageFormat(
                            context      = context,
                            inputUri     = inputUri!!,
                            outputUri    = outputUri!!,
                            targetFormat = format,
                            onProgress   = { progress = it },
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
