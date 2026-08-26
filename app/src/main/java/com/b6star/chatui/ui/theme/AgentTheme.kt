package com.b6star.chatui.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Defines the available theme types.
 */
enum class AgentThemeType {
    DEFAULT, BLUE, GREEN, RED, PURPLE
}

/**
 * Color specifications used throughout the app.
 */
data class AgentColors(
    val isDark: Boolean,
    // Base UI
    val background: Color,
    val onBackground: Color, // General text color independent of the custom theme
    val surface: Color,
    val metadataText: Color,
    val onSurfaceVariant: Color,
    val error: Color,
    val errorContainer: Color,
    val onErrorContainer: Color,
    // Messages
    val userBubble: Color,
    val assistantBubble: Color,
    val userText: Color,
    val assistantText: Color,
    val primary: Color,      // Heading (#), link, and button accent color
    val onPrimary: Color,    // Text color on buttons
    val emphasis: Color,     // Color for **emphasis** (separate from headings)
    val quote: Color,        // > Quote text
    val inlineCodeText: Color,
    val inlineCodeBackground: Color,
    // Code/Mermaid
    val codeBackground: Color,
    val codeBackgroundHex: String,
    val mermaidErrorHex: String,
    // Other
    val attachmentRemoveIcon: Color = Color.White,
    val geminiIconTint: Color = Color.Unspecified
)

/**
 * 0. DEFAULT theme (Monochrome)
 */
val DefaultLightColors = AgentColors(
    isDark = false,
    background = Color.White,
    onBackground = Color(0xFF1A1A1B),
    surface = Color(0xFFF6F7F8),
    metadataText = Color(0xFF7C7C7C),
    onSurfaceVariant = Color(0xFF7C7C7C),
    error = Color(0xFFEA0027),
    errorContainer = Color(0xFFFFE5E9),
    onErrorContainer = Color(0xFFB3001E),
    userBubble = Color(0xFFE2E2E2),
    assistantBubble = Color.Transparent,
    userText = Color(0xFF1A1A1B),
    assistantText = Color(0xFF1A1A1B),
    primary = Color(0xFF1A1A1B),
    onPrimary = Color.White,
    emphasis = Color.Black,
    quote = Color(0xFF666666),
    inlineCodeText = Color(0xFFD10000),
    inlineCodeBackground = Color(0xFFEEEEEE),
    codeBackground = Color(0xFFE2E8F0),
    codeBackgroundHex = "#E2E8F0",
    mermaidErrorHex = "#EA0027"
)

val DefaultDarkColors = AgentColors(
    isDark = true,
    background = Color(0xFF030303),
    onBackground = Color(0xFFD7DADC),
    surface = Color(0xFF1A1A1B),
    metadataText = Color(0xFF818384),
    onSurfaceVariant = Color(0xFF818384),
    error = Color(0xFFFF4500),
    errorContainer = Color(0xFF341009),
    onErrorContainer = Color(0xFFFFB399),
    userBubble = Color(0xFF272729),
    assistantBubble = Color.Transparent,
    userText = Color(0xFFD7DADC),
    assistantText = Color(0xFFD7DADC),
    primary = Color.White,
    onPrimary = Color.Black,
    emphasis = Color.White,
    quote = Color(0xFF818384),
    inlineCodeText = Color(0xFFD93812),
    inlineCodeBackground = Color(0xFF2D2D2D),
    codeBackground = Color(0xFF1A1A1B),
    codeBackgroundHex = "#1A1A1B",
    mermaidErrorHex = "#FF4500"
)

/**
 * 1. BLUE theme
 */
val BlueLightColors = AgentColors(
    isDark = false,
    background = Color(0xFFF5F7FA),
    onBackground = Color(0xFF1E293B),
    surface = Color(0xFFF8FAFC),
    metadataText = Color(0xFF64748B),
    onSurfaceVariant = Color(0xFF64748B),
    error = Color(0xFFEF4444),
    errorContainer = Color(0xFFFFE4E6),
    onErrorContainer = Color(0xFF991B1B),
    userBubble = Color(0xFFDBEAFE),
    assistantBubble = Color.Transparent,
    userText = Color(0xFF1E40AF),
    assistantText = Color(0xFF1E293B),
    primary = Color(0xFF1C00D8),
    onPrimary = Color.White,
    emphasis = Color(0xFF001E2F),     
    quote = Color(0xFF475569),
    inlineCodeText = Color(0xFFD81B60),
    inlineCodeBackground = Color(0xFFF1F5F9),
    codeBackground = Color(0xFFBCC6D0),
    codeBackgroundHex = "#BCC6D0",
    mermaidErrorHex = "#FF5252"
)

val BlueDarkColors = AgentColors(
    isDark = true,
    background = Color(0xFF0F172A),
    onBackground = Color(0xFFE2E8F0),
    surface = Color(0xFF1E293B),
    metadataText = Color(0xFF64748B),
    onSurfaceVariant = Color(0xFF94A3B8),
    error = Color(0xFFEF4444),
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFECDD3),
    userBubble = Color(0xFF1E3A8A),
    assistantBubble = Color.Transparent,
    userText = Color(0xFFDBEAFE),
    assistantText = Color(0xFFE2E8F0),
    primary = Color(0xFFD1E4FF),      
    onPrimary = Color(0xFF003258),
    emphasis = Color(0xFF90CAF9),     
    quote = Color(0xFFCBD5E1),
    inlineCodeText = Color(0xFFF472B6),
    inlineCodeBackground = Color(0xFF334155),
    codeBackground = Color(0xFF1E1E1E),
    codeBackgroundHex = "#1E1E1E",
    mermaidErrorHex = "#FF5252"
)

/**
 * 2. GREEN theme
 */
val GreenLightColors = BlueLightColors.copy(
    background = Color(0xFFF0F4F0),
    onBackground = Color(0xFF1B2E1B),
    userBubble = Color(0xFFE8F5E9),
    userText = Color(0xFF1B5E20),
    primary = Color(0xFF2E7D32),
    onPrimary = Color.White,
    emphasis = Color(0xFF002105),
    inlineCodeText = Color(0xFF2E7D32)
)

val GreenDarkColors = BlueDarkColors.copy(
    background = Color(0xFF0D1B0D),
    onBackground = Color(0xFFE8F5E9),
    userBubble = Color(0xFF1B301B),
    userText = Color(0xFFE8F5E9),
    primary = Color(0xFF81C784),
    onPrimary = Color(0xFF00390A),
    emphasis = Color(0xFFA5D6A7),
    inlineCodeText = Color(0xFF81C784)
)

/**
 * 3. RED theme
 */
val RedLightColors = BlueLightColors.copy(
    background = Color(0xFFFEF2F2),
    onBackground = Color(0xFF450A0A),
    userBubble = Color(0xFFFEE2E2),
    userText = Color(0xFF991B1B),
    primary = Color(0xFFDC2626),
    onPrimary = Color.White,
    emphasis = Color(0xFF450A0A),
    inlineCodeText = Color(0xFFDC2626)
)

val RedDarkColors = BlueDarkColors.copy(
    background = Color(0xFF180505),
    onBackground = Color(0xFFFEE2E2),
    userBubble = Color(0xFF3B0B0B),
    userText = Color(0xFFFEE2E2),
    primary = Color(0xFFF87171),
    onPrimary = Color(0xFF620007),
    emphasis = Color(0xFFFFCDD2),
    inlineCodeText = Color(0xFFF87171)
)

/**
 * 4. PURPLE theme
 */
val PurpleLightColors = BlueLightColors.copy(
    background = Color(0xFFFAF5FF),
    onBackground = Color(0xFF2E004F),
    userBubble = Color(0xFFF3E8FF),
    userText = Color(0xFF6B21A8),
    primary = Color(0xFF9333EA),
    onPrimary = Color.White,
    emphasis = Color(0xFF2E004F),
    inlineCodeText = Color(0xFF9333EA)
)

val PurpleDarkColors = BlueDarkColors.copy(
    background = Color(0xFF11081C),
    onBackground = Color(0xFFF3E8FF),
    userBubble = Color(0xFF260D42),
    userText = Color(0xFFF3E8FF),
    primary = Color(0xFFC084FC),
    onPrimary = Color(0xFF3B0060),
    emphasis = Color(0xFFE1BEE7),
    inlineCodeText = Color(0xFFC084FC)
)

internal val LocalAgentColors = staticCompositionLocalOf { DefaultLightColors }

object AgentTheme {
    val colors: AgentColors
        @Composable
        @ReadOnlyComposable
        get() = LocalAgentColors.current
}

@Composable
fun AgentChatTheme(
    themeType: AgentThemeType = AgentThemeType.DEFAULT,
    isDark: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = when (themeType) {
        AgentThemeType.DEFAULT -> if (isDark) DefaultDarkColors else DefaultLightColors
        AgentThemeType.BLUE -> if (isDark) BlueDarkColors else BlueLightColors
        AgentThemeType.GREEN -> if (isDark) GreenDarkColors else GreenLightColors
        AgentThemeType.RED -> if (isDark) RedDarkColors else RedLightColors
        AgentThemeType.PURPLE -> if (isDark) PurpleDarkColors else PurpleLightColors
    }

    val colorScheme = if (isDark) {
        darkColorScheme(
            primary = colors.primary,
            onPrimary = colors.onPrimary,
            primaryContainer = colors.primary.copy(alpha = 0.2f),
            onPrimaryContainer = colors.primary,
            secondary = colors.primary,
            onSecondary = colors.onPrimary,
            secondaryContainer = colors.primary.copy(alpha = 0.2f),
            onSecondaryContainer = colors.primary,
            background = colors.background,
            onBackground = colors.onBackground,
            surface = colors.surface,
            onSurface = colors.onBackground,
            surfaceVariant = colors.surface,
            onSurfaceVariant = colors.onSurfaceVariant,
            error = colors.error,
            errorContainer = colors.errorContainer,
            onErrorContainer = colors.onErrorContainer
        )
    } else {
        lightColorScheme(
            primary = colors.primary,
            onPrimary = colors.onPrimary,
            primaryContainer = colors.primary.copy(alpha = 0.1f),
            onPrimaryContainer = colors.primary,
            secondary = colors.primary,
            onSecondary = colors.onPrimary,
            secondaryContainer = colors.primary.copy(alpha = 0.1f),
            onSecondaryContainer = colors.primary,
            background = colors.background,
            onBackground = colors.onBackground,
            surface = colors.surface,
            onSurface = colors.onBackground,
            surfaceVariant = colors.surface,
            onSurfaceVariant = colors.onSurfaceVariant,
            error = colors.error,
            errorContainer = colors.errorContainer,
            onErrorContainer = colors.onErrorContainer
        )
    }

    val selectionColors = TextSelectionColors(
        handleColor = colors.primary,
        backgroundColor = colors.primary.copy(alpha = 0.4f)
    )

    CompositionLocalProvider(
        LocalAgentColors provides colors,
        LocalTextSelectionColors provides selectionColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content
        )
    }
}
