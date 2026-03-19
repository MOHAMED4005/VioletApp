package com.violet.app.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.violet.app.data.PrefsRepository
import com.violet.app.processing.*
import com.violet.app.ui.components.*
import com.violet.app.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun VertimergeScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs   = remember { PrefsRepository(context) }
    val scope   = rememberCoroutineScope()

    // Persistent state
    var inputUri    by remember { mutableStateOf<Uri?>(null) }
    var outputUri   by remember { mutableStateOf<Uri?>(null) }
    var mergeMode   by remember { mutableStateOf("pixels") }  // "pixels" | "pages"
    var pixelHeight by remember { mutableStateOf("5000") }
    var pageCount   by remember { mutableStateOf("10") }
    var saveAsZip   by remember { mutableStateOf(false) }
    var format      by remember { mutableStateOf("JPG") }

    // Load saved prefs
    LaunchedEffect(Unit) {
        prefs.get(PrefsRepository.VM_INPUT_URI,  "").collect { if (it.isNotBlank()) inputUri  = Uri.parse(it) }
    }
    LaunchedEffect(Unit) {
        prefs.get(PrefsRepository.VM_OUTPUT_URI, "").collect { if (it.isNotBlank()) outputUri = Uri.parse(it) }
    }
    LaunchedEffect(Unit) {
        prefs.get(PrefsRepository.VM_MERGE_MODE,   "pixels").collect { mergeMode   = it }
    }
    LaunchedEffect(Unit) {
        prefs.get(PrefsRepository.VM_PIXEL_HEIGHT, 5000).collect { pixelHeight = it.toString() }
    }
    LaunchedEffect(Unit) {
        prefs.get(PrefsRepository.VM_PAGE_COUNT,   10).collect   { pageCount   = it.toString() }
    }
    LaunchedEffect(Unit) {
        prefs.get(PrefsRepository.VM_SAVE_AS_ZIP,  false).collect { saveAsZip = it }
    }
    LaunchedEffect(Unit) {
        prefs.get(PrefsRepository.VM_FORMAT, "JPG").collect { format = it }
    }

    var progress  by remember { mutableStateOf(0f) }
    var isRunning by remember { mutableStateOf(false) }
    var isDone    by remember { mutableStateOf(false) }

    val canStart = inputUri != null && outputUri != null && !isRunning

    fun savePrefs() = scope.launch {
        prefs.save(PrefsRepository.VM_INPUT_URI,    inputUri?.toString()  ?: "")
        prefs.save(PrefsRepository.VM_OUTPUT_URI,   outputUri?.toString() ?: "")
        prefs.save(PrefsRepository.VM_MERGE_MODE,   mergeMode)
        prefs.save(PrefsRepository.VM_PIXEL_HEIGHT, pixelHeight.toIntOrNull() ?: 5000)
        prefs.save(PrefsRepository.VM_PAGE_COUNT,   pageCount.toIntOrNull()   ?: 10)
        prefs.save(PrefsRepository.VM_SAVE_AS_ZIP,  saveAsZip)
        prefs.save(PrefsRepository.VM_FORMAT,       format)
    }

    ScreenScaffold(
        title    = "Vertimerge",
        subtitle = "Merge chapter images vertically",
        icon     = Icons.Rounded.TableRows,
        onBack   = onBack,
    ) {
        // Input folder
        SectionLabel("Input")
        FolderPickerRow(
            label    = "Chapters Folder",
            uri      = inputUri,
            onPicked = { inputUri = it; savePrefs() },
        )

        // Output folder
        SectionLabel("Output")
        FolderPickerRow(
            label    = "Output Folder",
            uri      = outputUri,
            onPicked = { outputUri = it; savePrefs() },
        )

        // Merge mode
        SectionLabel("Merge Mode")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf("pixels" to "By Pixels", "pages" to "By Page Count").forEach { (mode, label) ->
                val selected = mergeMode == mode
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selected) VioletAccent.copy(.18f) else VioletElevated)
                        .border(1.dp, if (selected) VioletAccent else VioletBorder, RoundedCornerShape(10.dp))
                        .clickable { mergeMode = mode; savePrefs() }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text  = label,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = if (selected) VioletAccent else VioletMuted,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            fontSize = 13.sp,
                        ),
                    )
                }
            }
        }

        // Mode-specific options
        if (mergeMode == "pixels") {
            SectionLabel("Target Height (px)")
            OutlinedTextField(
                value         = pixelHeight,
                onValueChange = { pixelHeight = it; savePrefs() },
                singleLine    = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                placeholder   = { Text("e.g. 5000", color = VioletMuted) },
                colors        = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = VioletAccent,
                    unfocusedBorderColor = VioletBorder,
                    focusedTextColor     = VioletOnSurface,
                    unfocusedTextColor   = VioletOnSurface,
                    cursorColor          = VioletAccent,
                ),
                shape    = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            SectionLabel("Number of Pages per Chapter")
            OutlinedTextField(
                value         = pageCount,
                onValueChange = { pageCount = it; savePrefs() },
                singleLine    = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                placeholder   = { Text("e.g. 10", color = VioletMuted) },
                colors        = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = VioletAccent,
                    unfocusedBorderColor = VioletBorder,
                    focusedTextColor     = VioletOnSurface,
                    unfocusedTextColor   = VioletOnSurface,
                    cursorColor          = VioletAccent,
                ),
                shape    = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            )

            // ZIP toggle — only when mode is pages
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(VioletElevated)
                    .border(1.dp, VioletBorder, RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text("Package as ZIP", style = MaterialTheme.typography.bodyMedium)
                    Text("Each chapter saved inside a ZIP file", style = MaterialTheme.typography.labelSmall)
                }
                Switch(
                    checked         = saveAsZip,
                    onCheckedChange = { saveAsZip = it; savePrefs() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor  = VioletWhite,
                        checkedTrackColor  = VioletAccent,
                        uncheckedTrackColor = VioletBorder,
                    ),
                )
            }
        }

        // Output format
        SectionLabel("Output Format")
        FormatSelector(selected = format, onSelect = { format = it; savePrefs() })

        // Progress
        if (isRunning || isDone) {
            VioletProgressBar(progress = progress)
            if (isDone) {
                Text(
                    text  = "✓  Done! Files saved to output folder.",
                    style = MaterialTheme.typography.bodyMedium.copy(color = VioletSuccess),
                )
            }
        }

        // Start button
        StartButton(
            label   = "Start",
            enabled = canStart,
            loading = isRunning,
            onClick = {
                scope.launch {
                    isRunning = true
                    isDone    = false
                    progress  = 0f
                    try {
                        if (mergeMode == "pixels") {
                            vertimergeByPixels(
                                context     = context,
                                inputUri    = inputUri!!,
                                outputUri   = outputUri!!,
                                targetHeight = pixelHeight.toIntOrNull() ?: 5000,
                                format      = format,
                                onProgress  = { progress = it },
                            )
                        } else {
                            vertimergeByPages(
                                context    = context,
                                inputUri   = inputUri!!,
                                outputUri  = outputUri!!,
                                pageCount  = pageCount.toIntOrNull() ?: 10,
                                saveAsZip  = saveAsZip,
                                format     = format,
                                onProgress = { progress = it },
                            )
                        }
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
