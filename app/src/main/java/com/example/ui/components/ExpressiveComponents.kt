package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.model.EqBandUiModel
import com.example.model.FeatureGuideContent
import com.example.ui.theme.LocalBluePulseColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun BentoColorCard(
  containerColor: Color,
  contentColor: Color,
  modifier: Modifier = Modifier,
  shape: Shape = MaterialTheme.shapes.large,
  border: BorderStroke? = null,
  onClick: (() -> Unit)? = null,
  content: @Composable ColumnScope.() -> Unit,
) {
  val clickMod = if (onClick != null) {
    Modifier
      .clip(shape)
      .clickable(onClick = onClick)
  } else {
    Modifier
  }

  Surface(
    modifier = modifier.then(clickMod),
    shape = shape,
    color = containerColor,
    contentColor = contentColor,
    border = border,
  ) {
    CompositionLocalProvider(LocalContentColor provides contentColor) {
      Column(content = content)
    }
  }
}

@Composable
fun DualChannelWaveVisualizer(
  isPlaying: Boolean,
  waveColorA: Color,
  waveColorB: Color,
  modifier: Modifier = Modifier,
) {
  val transition = rememberInfiniteTransition(label = "dual_waves")
  val phase by transition.animateFloat(
    initialValue = 0f,
    targetValue = (2f * PI.toFloat()),
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = if (isPlaying) 1400 else 4200, easing = LinearEasing),
      repeatMode = RepeatMode.Restart,
    ),
    label = "dual_wave_phase",
  )

  Canvas(
    modifier = modifier
      .fillMaxWidth()
      .height(34.dp),
  ) {
    val midY = size.height / 2f
    val ampA = if (isPlaying) size.height * 0.36f else size.height * 0.16f
    val ampB = if (isPlaying) size.height * 0.28f else size.height * 0.12f

    val pathA = Path()
    val pathB = Path()
    var x = 0f
    while (x <= size.width) {
      val norm = (x / size.width.coerceAtLeast(1f)) * 2f * PI.toFloat() * 2.2f
      val yA = midY + ampA * sin(norm - phase)
      val yB = midY + ampB * sin(norm * 1.35f + phase * 1.2f)
      if (x == 0f) {
        pathA.moveTo(x, yA)
        pathB.moveTo(x, yB)
      } else {
        pathA.lineTo(x, yA)
        pathB.lineTo(x, yB)
      }
      x += 5f
    }

    drawPath(
      path = pathB,
      color = waveColorB.copy(alpha = 0.72f),
      style = Stroke(width = 2.6.dp.toPx(), cap = StrokeCap.Round),
    )
    drawPath(
      path = pathA,
      color = waveColorA,
      style = Stroke(width = 3.2.dp.toPx(), cap = StrokeCap.Round),
    )
  }
}

@Composable
fun AnimatedSpectrumBars(
  bands: List<EqBandUiModel>,
  isPlaying: Boolean,
  barColor: Color,
  modifier: Modifier = Modifier,
) {
  val transition = rememberInfiniteTransition(label = "eq_spectrum_bars")
  val pulse by transition.animateFloat(
    initialValue = 0.65f,
    targetValue = 1.0f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 720, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse,
    ),
    label = "bar_pulse",
  )

  Row(
    modifier = modifier.height(32.dp),
    horizontalArrangement = Arrangement.spacedBy(6.dp),
    verticalAlignment = Alignment.Bottom,
  ) {
    bands.take(5).forEachIndexed { idx, band ->
      val norm = ((band.levelMillibels + 1500f) / 3000f).coerceIn(0.18f, 1f)
      val animOffset = if (isPlaying) {
        if (idx % 2 == 0) pulse else (1.65f - pulse)
      } else {
        0.82f + (idx % 3) * 0.08f
      }
      val fraction = (norm * animOffset).coerceIn(0.24f, 1f)
      Box(
        modifier = Modifier
          .weight(1f)
          .height((32f * fraction).dp)
          .clip(RoundedCornerShape(50))
          .background(barColor.copy(alpha = 0.85f)),
      )
    }
  }
}

@Composable
fun AnimatedRadarMiniBeacon(
  isScanning: Boolean,
  hasTarget: Boolean,
  inkColor: Color,
  pulseColor: Color,
  modifier: Modifier = Modifier,
) {
  val transition = rememberInfiniteTransition(label = "mini_radar")
  val pingScale by transition.animateFloat(
    initialValue = 0.35f,
    targetValue = 1.0f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = if (isScanning) 1100 else 2200, easing = LinearEasing),
      repeatMode = RepeatMode.Restart,
    ),
    label = "ping_scale",
  )

  Box(modifier = modifier.size(46.dp), contentAlignment = Alignment.Center) {
    ScallopedExpressiveRing(
      isAnimating = isScanning || hasTarget,
      ringColor = inkColor.copy(alpha = 0.7f),
      trackColor = inkColor.copy(alpha = 0.15f),
      size = 46.dp,
      petals = 9,
    )
    Canvas(modifier = Modifier.size(34.dp)) {
      val c = Offset(size.width / 2f, size.height / 2f)
      drawCircle(
        color = pulseColor.copy(alpha = (1f - pingScale) * 0.65f),
        radius = (size.minDimension * 0.45f) * pingScale,
        center = c,
        style = Stroke(width = 2.dp.toPx()),
      )
      drawCircle(
        color = inkColor,
        radius = 4.dp.toPx(),
        center = c,
      )
    }
  }
}

@Composable
fun ScallopedExpressiveRing(
  isAnimating: Boolean,
  ringColor: Color = MaterialTheme.colorScheme.primary,
  trackColor: Color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
  size: Dp = 84.dp,
  petals: Int = 10,
  modifier: Modifier = Modifier,
  centerContent: @Composable () -> Unit = {},
) {
  val transition = rememberInfiniteTransition(label = "scalloped_ring")
  val rotationDeg by transition.animateFloat(
    initialValue = 0f,
    targetValue = if (isAnimating) 360f else 0f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 12_000, easing = LinearEasing),
      repeatMode = RepeatMode.Restart,
    ),
    label = "ring_rot",
  )

  Box(
    modifier = modifier.size(size),
    contentAlignment = Alignment.Center,
  ) {
    Canvas(modifier = Modifier.size(size)) {
      val cx = this.size.width / 2f
      val cy = this.size.height / 2f
      val baseR = this.size.minDimension * 0.41f
      val amp = this.size.minDimension * 0.055f

      val path = Path()
      val steps = 160
      val rotRad = Math.toRadians(rotationDeg.toDouble())
      for (i in 0..steps) {
        val theta = (2.0 * PI * i / steps) + rotRad
        val r = baseR + amp * sin(petals * theta).toFloat()
        val x = cx + r * cos(theta).toFloat()
        val y = cy + r * sin(theta).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
      }
      path.close()

      drawPath(
        path = path,
        color = trackColor,
        style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round),
      )

      drawPath(
        path = path,
        color = ringColor,
        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
      )
    }
    centerContent()
  }
}

@Composable
fun WavyAudioScrubber(
  progress: Float,
  isPlaying: Boolean,
  activeColor: Color = MaterialTheme.colorScheme.primary,
  inactiveColor: Color = MaterialTheme.colorScheme.outlineVariant,
  modifier: Modifier = Modifier,
) {
  val transition = rememberInfiniteTransition(label = "wavy_scrubber")
  val phase by transition.animateFloat(
    initialValue = 0f,
    targetValue = if (isPlaying) (2f * PI.toFloat()) else 0f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 1300, easing = LinearEasing),
      repeatMode = RepeatMode.Restart,
    ),
    label = "wave_phase",
  )

  Canvas(
    modifier = modifier
      .fillMaxWidth()
      .height(26.dp),
  ) {
    val clamped = progress.coerceIn(0f, 1f)
    val midY = size.height / 2f
    val splitX = size.width * clamped

    val activePath = Path()
    val amp = if (isPlaying) 4.5.dp.toPx() else 2.5.dp.toPx()
    val freq = 0.06f
    val stepPx = 4f
    var x = 0f
    while (x <= splitX) {
      val y = midY + amp * sin(x * freq - phase)
      if (x == 0f) activePath.moveTo(x, y) else activePath.lineTo(x, y)
      x += stepPx
    }
    activePath.lineTo(splitX, midY + amp * sin(splitX * freq - phase))

    drawLine(
      color = inactiveColor,
      start = Offset(splitX, midY),
      end = Offset(size.width, midY),
      strokeWidth = 3.dp.toPx(),
      cap = StrokeCap.Round,
    )

    if (splitX > 2f) {
      drawPath(
        path = activePath,
        color = activeColor,
        style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round),
      )
    }

    drawLine(
      color = activeColor,
      start = Offset(splitX, midY - 8.dp.toPx()),
      end = Offset(splitX, midY + 8.dp.toPx()),
      strokeWidth = 3.5.dp.toPx(),
      cap = StrokeCap.Round,
    )
  }
}

@Composable
fun ExpressiveInfoButton(
  contentDescription: String,
  testTag: String,
  onClick: () -> Unit,
  tint: Color = MaterialTheme.colorScheme.primary,
  modifier: Modifier = Modifier,
) {
  IconButton(
    onClick = onClick,
    modifier = modifier
      .minimumInteractiveComponentSize()
      .testTag(testTag),
  ) {
    Icon(
      imageVector = Icons.Outlined.Info,
      contentDescription = contentDescription,
      tint = tint,
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeatureGuideBottomSheet(
  guide: FeatureGuideContent,
  onDismiss: () -> Unit,
) {
  val extended = LocalBluePulseColors.current
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surfaceContainer,
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 22.dp)
        .padding(bottom = 28.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Text(
        text = guide.title,
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Text(
        text = guide.subtitle,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      guide.steps.forEachIndexed { index, step ->
        Surface(
          shape = MaterialTheme.shapes.medium,
          color = MaterialTheme.colorScheme.surfaceContainerHigh,
          border = extended.cardBorder,
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
          ) {
            Box(
              modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
              contentAlignment = Alignment.Center,
            ) {
              Text(
                text = "${index + 1}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimary,
              )
            }
            Text(
              text = step,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurface,
              modifier = Modifier.weight(1f),
            )
          }
        }
      }

      Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
        border = extended.cardBorder,
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          verticalAlignment = Alignment.Top,
        ) {
          Icon(
            imageVector = Icons.Rounded.Lightbulb,
            contentDescription = "Pro tip",
            tint = MaterialTheme.colorScheme.primary,
          )
          Text(
            text = guide.proTip,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
          )
        }
      }

      Spacer(modifier = Modifier.height(4.dp))

      Button(
        onClick = onDismiss,
        modifier = Modifier
          .fillMaxWidth()
          .height(50.dp)
          .testTag("feature_guide_close_btn"),
      ) {
        Text("Got It")
      }
    }
  }
}
