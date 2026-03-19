package com.violet.app.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FolderZip
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.violet.app.data.PrefsRepository
import com.violet.app.processing.makeZips
import com.violet.app.ui.components.*
import com.violet.app.ui.theme.VioletSuccess
import androidx.compose.material3.*
import kotlinx.coroutines.launch

@Composable
fun ZipMakerScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs   = remember { PrefsRepository(context) }
    val scope   = rememberCoroutineScope()

    var inputUri  by remember { mutableStateOf<Uri?>(null) }
    var outputUri by remember { mutableStateOf<Uri?>(null) }
    var progress  by remember { mutableStateOf(0f) }
    var isRunning by remember { mutableStateOf(false) }
    var isDone    by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        prefs.get(PrefsRepository.ZIP_INPUT_URI,  "").collect { if (it.isNotBlank()) inputUri  = Uri.parse(it) }
    }
    LaunchedEffect(Unit) {
        prefs.get(PrefsRepository.ZIP_OUTPUT_URI, "").collect { if (it.isNotBlank()) outputUri = Uri.parse(it) }
    }

    fun savePrefs() = scope.launch {
        prefs.save(PrefsRepository.ZIP_INPUT_URI,  inputUri?.toString()  ?: "")
        prefs.save(PrefsRepository.ZIP_OUTPUT_URI, outputUri?.toString() ?: "")
    }

    val canStart = inputUri != null && outputUri != null && !isRunning

    ScreenScaffold(
        title    = "Zip Maker",
        subtitle = "Pack chapter folders into ZIP archives",
        icon     = Icons.Rounded.FolderZip,
        onBack   = onBack,
    ) {
        SectionLabel("Input")
        FolderPickerRow(
            label    = "Chapters Folder",
            uri      = inputUri,
            onPicked = { inputUri = it; savePrefs() },
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
                    text  = "✓  Done! ZIP files saved to output folder.",
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
                        makeZips(
                            context    = context,
                            inputUri   = inputUri!!,
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
