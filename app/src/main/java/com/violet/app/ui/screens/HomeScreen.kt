package com.violet.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.violet.app.ui.navigation.Routes
import com.violet.app.ui.theme.*

data class ToolCard(
    val title: String,
    val subtitle: String,
    val route: String,
    val icon: ImageVector,
    val accentColor: Color,
)

@Composable
fun HomeScreen(nav: NavController) {
    val tools = listOf(
        ToolCard(
            title    = "Vertimerge",
            subtitle = "Merge chapter images vertically by pixels or page count",
            route    = Routes.VERTIMERGE,
            icon     = Icons.Rounded.TableRows,
            accentColor = Color(0xFF9B6DFF),
        ),
        ToolCard(
            title    = "Zip Maker",
            subtitle = "Pack chapter folders into ZIP archives",
            route    = Routes.ZIP_MAKER,
            icon     = Icons.Rounded.FolderZip,
            accentColor = Color(0xFF6DB8FF),
        ),
        ToolCard(
            title    = "Watermark",
            subtitle = "Stamp your logo onto chapter pages",
            route    = Routes.WATERMARK,
            icon     = Icons.Rounded.BrokenImage,
            accentColor = Color(0xFFAD6DFF),
        ),
        ToolCard(
            title    = "Format Changer",
            subtitle = "Convert images between JPG, PNG and WEBP",
            route    = Routes.FORMAT,
            icon     = Icons.Rounded.PhotoSizeSelectLarge,
            accentColor = Color(0xFF6DFFCA),
        ),
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VioletDeep)
            .padding(top = 64.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        // ── Header ─────────────────────────────────────────────
        Column(
            modifier = Modifier.padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "VIOLET",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = VioletAccent,
                    letterSpacing = 4.sp,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                ),
            )
            Text(
                text = "Your image toolkit",
                style = MaterialTheme.typography.displayLarge.copy(
                    color = VioletOnSurface,
                    fontSize = 28.sp,
                ),
            )
            Text(
                text = "Four powerful tools, one clean workspace.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = VioletMuted,
                ),
            )
        }

        Spacer(Modifier.height(32.dp))
        HorizontalDivider(
            color = VioletBorder,
            thickness = 0.5.dp,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        Spacer(Modifier.height(24.dp))

        // ── Tool Cards ─────────────────────────────────────────
        Column(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            tools.forEach { tool ->
                ToolCardItem(tool) { nav.navigate(tool.route) }
            }
        }

        Spacer(Modifier.height(48.dp))

        // ── Footer ─────────────────────────────────────────────
        Text(
            text    = "v1.0",
            style   = MaterialTheme.typography.labelSmall.copy(
                color = VioletBorder,
            ),
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 24.dp),
        )
    }
}

@Composable
private fun ToolCardItem(tool: ToolCard, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(VioletSurface)
            .border(1.dp, VioletBorder, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Icon box
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(tool.accentColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = tool.icon,
                contentDescription = null,
                tint = tool.accentColor,
                modifier = Modifier.size(24.dp),
            )
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text  = tool.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = VioletOnSurface,
                ),
            )
            Text(
                text  = tool.subtitle,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = VioletMuted,
                    fontSize = 12.sp,
                ),
            )
        }

        Icon(
            imageVector = Icons.Rounded.ArrowForwardIos,
            contentDescription = null,
            tint = VioletMuted,
            modifier = Modifier.size(14.dp),
        )
    }
}
