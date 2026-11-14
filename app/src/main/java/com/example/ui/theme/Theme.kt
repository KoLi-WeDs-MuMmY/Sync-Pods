package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

@Immutable
data class BluePulseExtendedColors(
  val signalGood: Color,
  val signalWarn: Color,
  val signalBad: Color,
  val isAmoled: Boolean,
  val cardBorder: BorderStroke?,
)

val LocalBluePulseColors = staticCompositionLocalOf {
  BluePulseExtendedColors(
    signalGood = SignalEmerald,
    signalWarn = AmberLatency,
    signalBad = CoralAlert,
    isAmoled = true,
    cardBorder = BorderStroke(1.dp, AmoledOutlineVariant),
  )
}

private val LightColorScheme = lightColorScheme(
  primary = CobaltPrimaryLight,
  onPrimary = Color.White,
  primaryContainer = CobaltContainerLight,
  onPrimaryContainer = Color(0xFF001D36),
  secondary = TealSecondaryLight,
  onSecondary = Color.White,
  secondaryContainer = Color(0xFFD1F2FF),
  onSecondaryContainer = Color(0xFF002635),
  tertiary = IndigoTertiaryLight,
  onTertiary = Color.White,
  background = LightBackground,
  onBackground = LightOnSurface,
  surface = LightSurface,
  onSurface = LightOnSurface,
  surfaceVariant = LightSurfaceVariant,
  onSurfaceVariant = LightOnSurfaceVariant,
  surfaceContainerLowest = Color.White,
  surfaceContainerLow = Color(0xFFF8FAFC),
  surfaceContainer = LightSurfaceContainer,
  surfaceContainerHigh = LightSurfaceContainerHigh,
  surfaceContainerHighest = Color(0xFFD8E1EC),
  outline = LightOutline,
  outlineVariant = LightOutlineVariant,
  error = CoralAlert,
  onError = Color.White,
)

private val DarkColorScheme = darkColorScheme(
  primary = CyanElectric,
  onPrimary = Color(0xFF001F29),
  primaryContainer = Color(0xFF003B4D),
  onPrimaryContainer = CyanSoft,
  secondary = TealSecondaryDark,
  onSecondary = Color(0xFF001E2B),
  secondaryContainer = Color(0xFF0B354D),
  onSecondaryContainer = Color(0xFFBAE6FD),
  tertiary = IndigoTertiaryDark,
  onTertiary = Color(0xFF1E1B4B),
  background = DarkBackground,
  onBackground = DarkOnSurface,
  surface = DarkSurface,
  onSurface = DarkOnSurface,
  surfaceVariant = DarkSurfaceVariant,
  onSurfaceVariant = DarkOnSurfaceVariant,
  surfaceContainerLowest = Color(0xFF0A0E14),
  surfaceContainerLow = DarkSurface,
  surfaceContainer = DarkSurfaceContainer,
  surfaceContainerHigh = DarkSurfaceContainerHigh,
  surfaceContainerHighest = Color(0xFF283548),
  outline = DarkOutline,
  outlineVariant = DarkOutlineVariant,
  error = CoralAlert,
  onError = Color(0xFF2D0606),
)

private val AmoledColorScheme = darkColorScheme(
  primary = CyanElectric,
  onPrimary = Color(0xFF001B24),
  primaryContainer = Color(0xFF003344),
  onPrimaryContainer = CyanSoft,
  secondary = TealSecondaryDark,
  onSecondary = Color(0xFF001924),
  secondaryContainer = Color(0xFF08293D),
  onSecondaryContainer = Color(0xFFBAE6FD),
  tertiary = IndigoTertiaryDark,
  onTertiary = Color(0xFF17143B),
  background = AmoledBackground,
  onBackground = AmoledOnSurface,
  surface = AmoledSurface,
  onSurface = AmoledOnSurface,
  surfaceVariant = AmoledSurfaceVariant,
  onSurfaceVariant = AmoledOnSurfaceVariant,
  surfaceContainerLowest = Color(0xFF000000),
  surfaceContainerLow = Color(0xFF030508),
  surfaceContainer = AmoledSurfaceContainer,
  surfaceContainerHigh = AmoledSurfaceContainerHigh,
  surfaceContainerHighest = Color(0xFF131B26),
  outline = AmoledOutline,
  outlineVariant = AmoledOutlineVariant,
  error = CoralAlert,
  onError = Color(0xFF2B0505),
)

val BluePulseShapes = Shapes(
  small = RoundedCornerShape(10.dp),
  medium = RoundedCornerShape(16.dp),
  large = RoundedCornerShape(22.dp),
  extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun BluePulseTheme(
  themeMode: ThemeMode = ThemeMode.AMOLED,
  content: @Composable () -> Unit,
) {
  val colorScheme = when (themeMode) {
    ThemeMode.LIGHT -> LightColorScheme
    ThemeMode.DARK -> DarkColorScheme
    ThemeMode.AMOLED -> AmoledColorScheme
  }

  val extendedColors = when (themeMode) {
    ThemeMode.LIGHT -> BluePulseExtendedColors(
      signalGood = SignalEmeraldLight,
      signalWarn = AmberLatency,
      signalBad = CoralAlert,
      isAmoled = false,
      cardBorder = BorderStroke(1.dp, LightOutlineVariant),
    )
    ThemeMode.DARK -> BluePulseExtendedColors(
      signalGood = SignalEmerald,
      signalWarn = AmberLatency,
      signalBad = CoralAlert,
      isAmoled = false,
      cardBorder = BorderStroke(1.dp, DarkOutlineVariant),
    )
    ThemeMode.AMOLED -> BluePulseExtendedColors(
      signalGood = SignalEmerald,
      signalWarn = AmberLatency,
      signalBad = CoralAlert,
      isAmoled = true,
      cardBorder = BorderStroke(1.dp, AmoledOutlineVariant),
    )
  }

  val view = LocalView.current
  if (!view.isInEditMode) {
    SideEffect {
      val window = (view.context as? Activity)?.window
      if (window != null) {
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars =
          themeMode == ThemeMode.LIGHT
      }
    }
  }

  CompositionLocalProvider(LocalBluePulseColors provides extendedColors) {
    MaterialTheme(
      colorScheme = colorScheme,
      typography = BluePulseTypography,
      shapes = BluePulseShapes,
      content = content,
    )
  }
}
