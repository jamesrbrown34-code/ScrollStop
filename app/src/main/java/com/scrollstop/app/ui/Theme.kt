package com.scrollstop.app.ui

import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.scrollstop.app.premium.AppTheme

internal val Background = Color(0xFF0A0A0A)
internal val PanelColor = Color(0xFF161616)
internal val TextPrimary = Color(0xFFF5F5F5)
internal val TextSecondary = Color(0xFF8A8A8A)
internal val PremiumGold = Color(0xFFE0A526)

/** A theme's accent: the bright highlight plus its darker container variant. */
internal data class AppAccent(val highlight: Color, val container: Color)

internal var accentState by mutableStateOf(AppTheme.MONOCHROME.accent)
internal val Accent: Color get() = accentState.highlight
internal val AccentContainer: Color get() = accentState.container

internal fun applyAccent(accent: AppAccent) {
    accentState = accent
}

internal val AppTheme.accent: AppAccent
    get() = when (this) {
        AppTheme.MONOCHROME -> AppAccent(Color.White, Color(0xFF2A2A2A))
        AppTheme.FOREST -> AppAccent(Color(0xFF9ED9BA), Color(0xFF395B4B))
        AppTheme.OCEAN -> AppAccent(Color(0xFF9EC9E0), Color(0xFF33505E))
        AppTheme.EMBER -> AppAccent(Color(0xFFE8C89A), Color(0xFF5E4A33))
    }

internal val AppColorScheme = darkColorScheme(
    primary = Accent,
    onPrimary = Background,
    primaryContainer = AccentContainer,
    onPrimaryContainer = TextPrimary,
    secondary = Accent,
    onSecondary = Background,
    secondaryContainer = AccentContainer,
    onSecondaryContainer = TextPrimary,
    background = Background,
    onBackground = TextPrimary,
    surface = PanelColor,
    onSurface = TextPrimary,
    surfaceVariant = PanelColor,
    onSurfaceVariant = TextSecondary,
    outline = AccentContainer,
    outlineVariant = AccentContainer
)
