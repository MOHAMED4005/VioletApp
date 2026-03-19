package com.violet.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ── Violet dark palette ─────────────────────────────────────────
val VioletDeep        = Color(0xFF0E0C14)   // near-black with purple cast
val VioletSurface     = Color(0xFF16121F)   // card surface
val VioletElevated    = Color(0xFF1E1830)   // elevated elements
val VioletBorder      = Color(0xFF2A2240)   // subtle borders
val VioletAccent      = Color(0xFF9B6DFF)   // primary accent — bright violet
val VioletAccentSoft  = Color(0xFF7B52D4)   // dimmer variant
val VioletHighlight   = Color(0xFFBFA0FF)   // text highlights / chip labels
val VioletMuted       = Color(0xFF6B5E8A)   // muted text
val VioletOnSurface   = Color(0xFFE8E0FF)   // primary text on dark
val VioletSuccess     = Color(0xFF52C48A)   // progress / done state
val VioletDanger      = Color(0xFFFF6B6B)   // error
val VioletWhite       = Color(0xFFFFFFFF)

private val DarkColorScheme = darkColorScheme(
    primary            = VioletAccent,
    onPrimary          = VioletWhite,
    primaryContainer   = VioletElevated,
    onPrimaryContainer = VioletHighlight,
    secondary          = VioletAccentSoft,
    onSecondary        = VioletWhite,
    background         = VioletDeep,
    onBackground       = VioletOnSurface,
    surface            = VioletSurface,
    onSurface          = VioletOnSurface,
    surfaceVariant     = VioletElevated,
    onSurfaceVariant   = VioletMuted,
    outline            = VioletBorder,
    error              = VioletDanger,
    onError            = VioletWhite,
)

@Composable
fun VioletTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography  = VioletTypography,
        content     = content,
    )
}

// Typography using system default (will look clean on all devices)
val VioletTypography = androidx.compose.material3.Typography(
    displayLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize   = 32.sp,
        color      = VioletOnSurface,
        letterSpacing = (-0.5).sp,
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize   = 20.sp,
        color      = VioletOnSurface,
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize   = 15.sp,
        color      = VioletOnSurface,
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize   = 14.sp,
        color      = VioletOnSurface,
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize   = 11.sp,
        color      = VioletMuted,
        letterSpacing = 0.8.sp,
    ),
)
