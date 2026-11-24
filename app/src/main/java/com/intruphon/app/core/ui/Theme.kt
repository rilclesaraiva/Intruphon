package com.intruphon.app.core.ui

import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import com.intruphon.app.data.settings.AppSettings
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

// Material 3 Expressive look built from the stable Material 3 API: large rounded shapes,
// heavier type for titles and spring motion (see the page transition in MainActivity).
private val ExpressiveShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(24.dp),
    large = RoundedCornerShape(32.dp),
    extraLarge = RoundedCornerShape(44.dp),
)

private val base = Typography()
private val ExpressiveTypography = Typography(
    displaySmall = base.displaySmall.copy(fontWeight = FontWeight.Bold),
    headlineLarge = base.headlineLarge.copy(fontWeight = FontWeight.Bold),
    headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.Bold),
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    titleSmall = base.titleSmall.copy(fontWeight = FontWeight.SemiBold),
    labelLarge = base.labelLarge.copy(fontWeight = FontWeight.Bold),
)

@Composable
fun IntruphonTheme(content: @Composable () -> Unit) {
    val mode by AppSettings.themeFlow.collectAsState()
    val dark = when (mode) {
        AppSettings.ThemeMode.SYSTEM -> isSystemInDarkTheme()
        AppSettings.ThemeMode.LIGHT -> false
        AppSettings.ThemeMode.DARK, AppSettings.ThemeMode.AMOLED -> true
    }
    val context = LocalContext.current
    val useSystem by AppSettings.dynamicColorFlow.collectAsState()
    val scheme = when {
        useSystem && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> IntruphonDark
        else -> IntruphonLight
    }
    val colors = if (mode == AppSettings.ThemeMode.AMOLED) scheme.toAmoled() else scheme

    // The system bars follow the chosen theme, not the phone's: dark icons on a light page and the other way round.
    val activity = context as? ComponentActivity
    SideEffect {
        activity?.enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT) { dark },
            navigationBarStyle = SystemBarStyle.auto(
                android.graphics.Color.argb(0xe6, 0xff, 0xff, 0xff),
                android.graphics.Color.argb(0x80, 0x1b, 0x1b, 0x1b),
            ) { dark },
        )
    }
    MaterialTheme(colorScheme = colors, shapes = ExpressiveShapes, typography = ExpressiveTypography, content = content)
}

// The app's own colors: neutral near-black surfaces (no blue cast, so nothing turns blue) with the icon's pale ice blue
// (#C7DDE8) only as the accent.
private val IntruphonDark = darkColorScheme(
    primary = Color(0xFFC7DDE8), onPrimary = Color(0xFF101417),
    primaryContainer = Color(0xFF2C3438), onPrimaryContainer = Color(0xFFDCE6EB),
    secondary = Color(0xFFB4C4CC), onSecondary = Color(0xFF101417),
    secondaryContainer = Color(0xFF2B3136), onSecondaryContainer = Color(0xFFDDE5E9),
    tertiary = Color(0xFFA9C0D4), onTertiary = Color(0xFF101417),
    tertiaryContainer = Color(0xFF2D343A), onTertiaryContainer = Color(0xFFDCE4EB),
    background = Color(0xFF0E0F10), onBackground = Color(0xFFE6E8EA),
    surface = Color(0xFF0E0F10), onSurface = Color(0xFFE6E8EA),
    surfaceVariant = Color(0xFF24282B), onSurfaceVariant = Color(0xFFB4BABF),
    outline = Color(0xFF70777C), outlineVariant = Color(0xFF33383C),
    inverseSurface = Color(0xFFE3E6E8), inverseOnSurface = Color(0xFF0E0F10), inversePrimary = Color(0xFF2F5F7A),
    surfaceTint = Color(0xFFC7DDE8),
    surfaceDim = Color(0xFF0B0C0D),
    surfaceContainerLowest = Color(0xFF0B0C0D), surfaceContainerLow = Color(0xFF141516),
    surfaceContainer = Color(0xFF191B1D), surfaceContainerHigh = Color(0xFF202325), surfaceContainerHighest = Color(0xFF272B2E),
)

private val IntruphonLight = lightColorScheme(
    primary = Color(0xFF2F5F7A), onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFC7DDE8), onPrimaryContainer = Color(0xFF0C1620),
    secondary = Color(0xFF4C6B7C), onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD5E6EE), onSecondaryContainer = Color(0xFF0C1620),
    tertiary = Color(0xFF3F6E9C), onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFD2E4F7), onTertiaryContainer = Color(0xFF0C1620),
    background = Color(0xFFF4F9FB), onBackground = Color(0xFF0C1620),
    surface = Color(0xFFF4F9FB), onSurface = Color(0xFF0C1620),
    surfaceVariant = Color(0xFFDCE6EB), onSurfaceVariant = Color(0xFF3F535F),
    outline = Color(0xFF6F8591), outlineVariant = Color(0xFFC3D2DA),
    inverseSurface = Color(0xFF1B2A36), inverseOnSurface = Color(0xFFE3EEF3), inversePrimary = Color(0xFFA9CBDD),
    surfaceTint = Color(0xFF2F5F7A),
    surfaceDim = Color(0xFFD5DFE5),
    surfaceContainerLowest = Color(0xFFFFFFFF), surfaceContainerLow = Color(0xFFEEF4F7),
    surfaceContainer = Color(0xFFE8EFF3), surfaceContainerHigh = Color(0xFFE2EAEF), surfaceContainerHighest = Color(0xFFDCE5EA),
)

/** The dark colors with a pure black page and near-black surfaces, so an OLED screen switches its pixels off. */
private fun ColorScheme.toAmoled() = copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceDim = Color.Black,
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color(0xFF0B0B0B),
    surfaceContainer = Color(0xFF111111),
    surfaceContainerHigh = Color(0xFF181818),
    surfaceContainerHighest = Color(0xFF1F1F1F),
)
