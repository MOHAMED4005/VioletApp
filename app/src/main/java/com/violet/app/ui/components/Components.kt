package com.violet.app.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.violet.app.ui.theme.*

// ── Folder picker row ────────────────────────────────────────────

@Composable
fun FolderPickerRow(
    label: String,
    uri: Uri?,
    onPicked: (Uri) -> Unit,
    modifier: Modifier = Modifier,
) {
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { it?.let(onPicked) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(VioletElevated)
            .border(1.dp, VioletBorder, RoundedCornerShape(12.dp))
            .clickable { launcher.launch(null) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = Icons.Rounded.Folder,
            contentDescription = null,
            tint = VioletAccent,
            modifier = Modifier.size(20.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text  = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = VioletMuted,
                    letterSpacing = 0.6.sp,
                ),
            )
            Text(
                text  = if (uri != null) uri.lastPathSegment ?: "Selected" else "Tap to choose folder",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = if (uri != null) VioletHighlight else VioletMuted,
                ),
                maxLines = 1,
            )
        }
        Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = VioletMuted,
            modifier = Modifier.size(16.dp),
        )
    }
}

// ── File picker row (single file) ────────────────────────────────

@Composable
fun FilePickerRow(
    label: String,
    uri: Uri?,
    mimeType: String = "image/*",
    onPicked: (Uri) -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Rounded.Image,
) {
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { it?.let(onPicked) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(VioletElevated)
            .border(1.dp, VioletBorder, RoundedCornerShape(12.dp))
            .clickable { launcher.launch(arrayOf(mimeType)) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = VioletAccent,
            modifier = Modifier.size(20.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text  = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = VioletMuted,
                    letterSpacing = 0.6.sp,
                ),
            )
            Text(
                text  = if (uri != null) uri.lastPathSegment?.substringAfterLast('/') ?: "Selected" else "Tap to choose file",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = if (uri != null) VioletHighlight else VioletMuted,
                ),
                maxLines = 1,
            )
        }
        Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = VioletMuted,
            modifier = Modifier.size(16.dp),
        )
    }
}

// ── Format selector chips ─────────────────────────────────────────

@Composable
fun FormatSelector(
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val formats = listOf("JPG", "PNG", "WEBP")
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        formats.forEach { fmt ->
            val isSelected = fmt == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (isSelected) VioletAccent.copy(alpha = 0.18f) else VioletElevated
                    )
                    .border(
                        1.dp,
                        if (isSelected) VioletAccent else VioletBorder,
                        RoundedCornerShape(10.dp),
                    )
                    .clickable { onSelect(fmt) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = fmt,
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = if (isSelected) VioletAccent else VioletMuted,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        fontSize = 13.sp,
                    ),
                )
            }
        }
    }
}

// ── Progress section ─────────────────────────────────────────────

@Composable
fun VioletProgressBar(progress: Float, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text  = "Progress",
                style = MaterialTheme.typography.labelSmall,
            )
            Text(
                text  = "${(progress * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall.copy(color = VioletAccent),
            )
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color           = VioletAccent,
            trackColor      = VioletBorder,
        )
    }
}

// ── Start / action button ─────────────────────────────────────────

@Composable
fun StartButton(
    label: String = "Start",
    enabled: Boolean = true,
    loading: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick  = onClick,
        enabled  = enabled && !loading,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = VioletAccent,
            contentColor   = Color.White,
            disabledContainerColor = VioletBorder,
            disabledContentColor   = VioletMuted,
        ),
        shape = RoundedCornerShape(14.dp),
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = Color.White,
                strokeWidth = 2.dp,
            )
            Spacer(Modifier.width(10.dp))
        }
        Text(
            text = if (loading) "Processing…" else label,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold,
            ),
        )
    }
}

// ── Section label ─────────────────────────────────────────────────

@Composable
fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(
            color = VioletAccent,
            letterSpacing = 1.2.sp,
            fontWeight = FontWeight.Bold,
        ),
    )
}

// ── Screen scaffold ───────────────────────────────────────────────

@Composable
fun ScreenScaffold(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onBack: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VioletDeep)
            .padding(top = 56.dp)
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.Rounded.ArrowBackIosNew,
                    contentDescription = "Back",
                    tint = VioletOnSurface,
                )
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(VioletAccent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = VioletAccent, modifier = Modifier.size(20.dp))
            }
            Column {
                Text(title, style = MaterialTheme.typography.headlineMedium)
                Text(subtitle, style = MaterialTheme.typography.labelSmall)
            }
        }

        Spacer(Modifier.height(8.dp))
        HorizontalDivider(color = VioletBorder, thickness = 0.5.dp)
        Spacer(Modifier.height(4.dp))

        // Scrollable content
        val scrollState = androidx.compose.foundation.rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(Modifier.height(4.dp))
            content()
            Spacer(Modifier.height(40.dp))
        }
    }
}
