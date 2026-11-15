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
  val coralHero: Color,
  val coralContainer: Color,
  val isAmoled: Boolean,
  val cardBorder: BorderStroke?,
)

val LocalBluePulseColors = staticCompositionLocalOf {
  BluePulseExtendedColors(
    signalGood = MintSignal,
    signalWarn = AmberLatency,
    signalBad = AlertRed,
    coralHero = CoralExpressive,
    coralContainer = CoralContainerDark,
    isAmoled = true,
    cardBorder = BorderStroke(1.dp, AmoledOutlineVariant),
  )
}

private val LightColorScheme = lightColorScheme(
  primary = RoyalPlum,
  onPrimary = Color.White,
  primaryContainer = LightSurfaceContainerHigh,
  onPrimaryContainer = Color(0xFF280B54),
  secondary = DeepPlumPrimary,
  onSecondary = Color.White,
  secondaryContainer = Color(0xFFF0E4FF),
  onSecondaryContainer = Color(0xFF2A0F54),
  tertiary = CoralExpressive,
  onTertiary = Color.White,
  tertiaryContainer = CoralContainerLight,
  onTertiaryContainer = Color(0xFF4C0519),
  background = LightBackground,
  onBackground = LightOnSurface,
  surface = LightSurface,
  onSurface = LightOnSurface,
  surfaceVariant = LightSurfaceVariant,
  onSurfaceVariant = LightOnSurfaceVariant,
  surfaceContainerLowest = Color.White,
  surfaceContainerLow = Color(0xFFFCFAFF),
  surfaceContainer = LightSurfaceContainer,
  surfaceContainerHigh = LightSurfaceContainerHigh,
  surfaceContainerHighest = Color(0xFFDEC9FA),
  outline = LightOutline,
  outlineVariant = LightOutlineVariant,
  error = AlertRed,
  onError = Color.White,
)

private val PlumDarkColorScheme = darkColorScheme(
  primary = LavenderLuminous,
  onPrimary = Color(0xFF280D4F),
  primaryContainer = Color(0xFF432175),
  onPrimaryContainer = LavenderSoft,
  secondary = OrchidVibrant,
  onSecondary = Color(0xFF2D0F4F),
  secondaryContainer = Color(0xFF391D63),
  onSecondaryContainer = LavenderSoft,
  tertiary = CoralExpressive,
  onTertiary = Color(0xFF3E0415),
  tertiaryContainer = CoralContainerDark,
  onTertiaryContainer = Color(0xFFFFD9E2),
  background = PlumDarkBackground,
  onBackground = PlumDarkOnSurface,
  surface = PlumDarkSurface,
  onSurface = PlumDarkOnSurface,
  surfaceVariant = PlumDarkSurfaceVariant,
  onSurfaceVariant = PlumDarkOnSurfaceVariant,
  surfaceContainerLowest = Color(0xFF0E071B),
  surfaceContainerLow = PlumDarkSurface,
  surfaceContainer = PlumDarkSurfaceContainer,
  surfaceContainerHigh = PlumDarkSurfaceContainerHigh,
  surfaceContainerHighest = Color(0xFF3B2463),
  outline = PlumDarkOutline,
  outlineVariant = PlumDarkOutlineVariant,
  error = AlertRed,
  onError = Color(0xFF2D0606),
)

private val AmoledColorScheme = darkColorScheme(
  primary = LavenderLuminous,
  onPrimary = Color(0xFF240B47),
  primaryContainer = Color(0xFF381B63),
  onPrimaryContainer = LavenderSoft,
  secondary = OrchidVibrant,
  onSecondary = Color(0xFF270B45),
  secondaryContainer = Color(0xFF2C154F),
  onSecondaryContainer = LavenderSoft,
  tertiary = CoralExpressive,
  onTertiary = Color(0xFF380312),
  tertiaryContainer = CoralContainerDark,
  onTertiaryContainer = Color(0xFFFFD9E2),
  background = AmoledBackground,
  onBackground = AmoledOnSurface,
  surface = AmoledSurface,
  onSurface = AmoledOnSurface,
  surfaceVariant = AmoledSurfaceVariant,
  onSurfaceVariant = AmoledOnSurfaceVariant,
  surfaceContainerLowest = Color(0xFF000000),
  surfaceContainerLow = Color(0xFF05030A),
  surfaceContainer = AmoledSurfaceContainer,
  surfaceContainerHigh = AmoledSurfaceContainerHigh,
  surfaceContainerHighest = Color(0xFF1E1233),
  outline = AmoledOutline,
  outlineVariant = AmoledOutlineVariant,
  error = AlertRed,
  onError = Color(0xFF2B0505),
)

val BluePulseShapes = Shapes(
  small = RoundedCornerShape(14.dp),
  medium = RoundedCornerShape(22.dp),
  large = RoundedCornerShape(28.dp),
  extraLarge = RoundedCornerShape(34.dp),
)

@Composable
fun BluePulseTheme(
  themeMode: ThemeMode = ThemeMode.DARK,
  content: @Composable () -> Unit,
) {
  val colorScheme = when (themeMode) {
    ThemeMode.LIGHT -> LightColorScheme
    ThemeMode.DARK -> PlumDarkColorScheme
    ThemeMode.AMOLED -> AmoledColorScheme
  }

  val extendedColors = when (themeMode) {
    ThemeMode.LIGHT -> BluePulseExtendedColors(
      signalGood = MintSignalLight,
      signalWarn = AmberLatency,
      signalBad = AlertRed,
      coralHero = CoralExpressive,
      coralContainer = CoralContainerLight,
      isAmoled = false,
      cardBorder = BorderStroke(1.dp, LightOutlineVariant),
    )
    ThemeMode.DARK -> BluePulseExtendedColors(
      signalGood = MintSignal,
      signalWarn = AmberLatency,
      signalBad = AlertRed,
      coralHero = CoralExpressive,
      coralContainer = CoralContainerDark,
      isAmoled = false,
      cardBorder = BorderStroke(1.dp, PlumDarkOutlineVariant),
    )
    ThemeMode.AMOLED -> BluePulseExtendedColors(
      signalGood = MintSignal,
      signalWarn = AmberLatency,
      signalBad = AlertRed,
      coralHero = CoralExpressive,
      coralContainer = CoralContainerDark,
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
