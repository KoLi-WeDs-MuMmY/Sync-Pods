package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BluetoothConnected
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.BluetoothDeviceUiModel
import com.example.ui.theme.CondensedMediumMetricStyle
import com.example.ui.theme.LocalBluePulseColors
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Earbuds3DConnectionSheet(
  device: BluetoothDeviceUiModel,
  onConnectAndRoute: () -> Unit,
  onOpenDualStudio: () -> Unit,
  onShowGuide: () -> Unit,
  onDismiss: () -> Unit,
) {
  val extended = LocalBluePulseColors.current
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  val openProgress = remember { Animatable(0f) }
  var userYawOffset by remember { mutableFloatStateOf(0f) }

  LaunchedEffect(device.address) {
    openProgress.snapTo(0f)
    openProgress.animateTo(
      targetValue = 1f,
      animationSpec = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow,
      ),
    )
  }

  val transition = rememberInfiniteTransition(label = "earbuds_3d")
  val spinAngle by transition.animateFloat(
    initialValue = -22f,
    targetValue = 22f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 3600, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse,
    ),
    label = "spin_yaw",
  )
  val hoverBob by transition.animateFloat(
    initialValue = -6f,
    targetValue = 6f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 2200, easing = LinearEasing),
      repeatMode = RepeatMode.Reverse,
    ),
    label = "hover_bob",
  )

  val density = LocalDensity.current.density
  val primaryColor = MaterialTheme.colorScheme.primary
  val secondaryColor = MaterialTheme.colorScheme.secondary
  val coralColor = extended.coralHero

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
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        ExpressiveInfoButton(
          contentDescription = "How 3D Bluetooth pairing works",
          testTag = "earbuds_3d_info_btn",
          onClick = onShowGuide,
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
          Text(
            text = device.name,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Text(
            text = if (device.isConnected) {
              "CONNECTED • ${device.codecInfo.codecName}"
            } else {
              "READY TO PAIR • DRAG TO ROTATE 3D"
            },
            style = MaterialTheme.typography.labelMedium,
            color = if (device.isConnected) extended.signalGood else primaryColor,
          )
        }

        IconButton(
          onClick = onDismiss,
          modifier = Modifier.testTag("earbuds_3d_close_btn"),
        ) {
          Icon(Icons.Rounded.Close, contentDescription = "Close 3D modal")
        }
      }

      // 3D Stage: Scalloped Halo + Animated Opening Case + Levitating 3D Earbuds
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(250.dp)
          .pointerInput(device.address) {
            detectHorizontalDragGestures { _, dragAmount ->
              userYawOffset = (userYawOffset + dragAmount * 0.45f).coerceIn(-65f, 65f)
            }
          },
        contentAlignment = Alignment.Center,
      ) {
        ScallopedExpressiveRing(
          isAnimating = true,
          ringColor = primaryColor.copy(alpha = 0.55f),
          trackColor = primaryColor.copy(alpha = 0.14f),
          size = 236.dp,
          petals = 12,
        )

        val effectiveYaw = spinAngle + userYawOffset
        val lidOpen = openProgress.value

        Canvas(
          modifier = Modifier
            .size(220.dp)
            .graphicsLayer {
              rotationY = effectiveYaw * 0.65f
              rotationX = 8f - (lidOpen * 4f)
              cameraDistance = 14f * density
            },
        ) {
          val w = size.width
          val h = size.height
          val cx = w / 2f

          // Ground shadow beneath floating 3D charging case
          drawOval(
            brush = Brush.radialGradient(
              colors = listOf(primaryColor.copy(alpha = 0.35f), Color.Transparent),
              center = Offset(cx, h * 0.88f),
              radius = w * 0.38f,
            ),
            topLeft = Offset(cx - w * 0.34f, h * 0.80f),
            size = Size(w * 0.68f, h * 0.14f),
          )

          // Charging Case Back Lid (animates open upward with 3D tilt)
          val lidHeight = h * 0.24f
          val lidY = h * (0.48f - 0.16f * lidOpen)
          drawRoundRect(
            brush = Brush.linearGradient(
              colors = listOf(Color(0xFF472B75), Color(0xFF281548)),
              start = Offset(cx - w * 0.26f, lidY),
              end = Offset(cx + w * 0.26f, lidY + lidHeight),
            ),
            topLeft = Offset(cx - w * 0.25f, lidY),
            size = Size(w * 0.50f, lidHeight),
            cornerRadius = CornerRadius(24.dp.toPx(), 24.dp.toPx()),
          )

          // Charging Case Lower Body with 3D bevel highlight
          val caseTop = h * 0.52f
          val caseH = h * 0.28f
          drawRoundRect(
            brush = Brush.verticalGradient(
              colors = listOf(Color(0xFF5D3A96), Color(0xFF2A164B), Color(0xFF1A0D30)),
              startY = caseTop,
              endY = caseTop + caseH,
            ),
            topLeft = Offset(cx - w * 0.27f, caseTop),
            size = Size(w * 0.54f, caseH),
            cornerRadius = CornerRadius(26.dp.toPx(), 26.dp.toPx()),
          )
          drawRoundRect(
            color = primaryColor.copy(alpha = 0.45f),
            topLeft = Offset(cx - w * 0.27f, caseTop),
            size = Size(w * 0.54f, caseH),
            cornerRadius = CornerRadius(26.dp.toPx(), 26.dp.toPx()),
            style = Stroke(width = 1.5.dp.toPx()),
          )

          // Case LED Pairing Indicator Dot
          drawCircle(
            color = if (device.isConnected) Color(0xFF34D399) else coralColor,
            radius = 4.dp.toPx(),
            center = Offset(cx, caseTop + caseH * 0.45f),
          )

          // Levitating 3D Left & Right Earbuds rising above the open case
          val floatOffset = (-28.dp.toPx() * lidOpen) + hoverBob
          val yawFactor = sin(Math.toRadians(effectiveYaw.toDouble())).toFloat()
          val perspectiveScaleL = 1f + (yawFactor * 0.08f)
          val perspectiveScaleR = 1f - (yawFactor * 0.08f)

          draw3DEarbud(
            center = Offset(cx - w * 0.14f, h * 0.42f + floatOffset),
            isLeft = true,
            scale = perspectiveScaleL,
            highlight = primaryColor,
            accent = coralColor,
          )
          draw3DEarbud(
            center = Offset(cx + w * 0.14f, h * 0.42f + floatOffset),
            isLeft = false,
            scale = perspectiveScaleR,
            highlight = secondaryColor,
            accent = coralColor,
          )
        }
      }

      // L / R / Case Battery Telemetry Row
      val overallBat = device.batteryPercent
      val leftBat = device.leftBatteryPercent ?: overallBat
      val rightBat = device.rightBatteryPercent ?: overallBat
      val caseBat = device.caseBatteryPercent ?: overallBat?.let { (it + 8).coerceAtMost(100) }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        BatteryRingCard("Left Bud", leftBat, primaryColor, Modifier.weight(1f))
        BatteryRingCard("Right Bud", rightBat, secondaryColor, Modifier.weight(1f))
        BatteryRingCard("Case", caseBat, coralColor, Modifier.weight(1f))
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Button(
          onClick = onConnectAndRoute,
          colors = ButtonDefaults.buttonColors(
            containerColor = extended.coralHero,
            contentColor = Color.White,
          ),
          modifier = Modifier
            .weight(1f)
            .height(52.dp)
            .testTag("earbuds_3d_connect_btn"),
        ) {
          Icon(Icons.Rounded.BluetoothConnected, contentDescription = null)
          Spacer(modifier = Modifier.width(8.dp))
          Text(if (device.isConnected) "Sync Now" else "Connect")
        }

        OutlinedButton(
          onClick = onOpenDualStudio,
          modifier = Modifier
            .weight(1f)
            .height(52.dp)
            .testTag("earbuds_3d_dual_studio_btn"),
        ) {
          Text("Dual Studio")
        }
      }
    }
  }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.draw3DEarbud(
  center: Offset,
  isLeft: Boolean,
  scale: Float,
  highlight: Color,
  accent: Color,
) {
  val dir = if (isLeft) -1f else 1f
  val headR = 16.dp.toPx() * scale
  val stemW = 8.dp.toPx() * scale
  val stemH = 26.dp.toPx() * scale

  // Earbud stem with cylindrical gradient
  drawRoundRect(
    brush = Brush.horizontalGradient(
      colors = listOf(Color(0xFFEDE2FF), Color(0xFFB794F4), Color(0xFF6B46C1)),
      startX = center.x - stemW / 2f,
      endX = center.x + stemW / 2f,
    ),
    topLeft = Offset(center.x - stemW / 2f, center.y + headR * 0.2f),
    size = Size(stemW, stemH),
    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
  )

  // Metallic charging contact tip at bottom of stem
  drawCircle(
    color = accent,
    radius = 2.6.dp.toPx() * scale,
    center = Offset(center.x, center.y + headR * 0.2f + stemH - 3.dp.toPx()),
  )

  // Spherical 3D earbud acoustic head
  drawCircle(
    brush = Brush.radialGradient(
      colors = listOf(Color.White, highlight, Color(0xFF4C1D95)),
      center = Offset(center.x - 4.dp.toPx(), center.y - 4.dp.toPx()),
      radius = headR * 1.3f,
    ),
    radius = headR,
    center = center,
  )

  // Acoustic silicone tip angled outward/inward
  val tipOffset = Offset(center.x - dir * headR * 0.72f, center.y - headR * 0.1f)
  drawCircle(
    color = Color(0xFF1F1135),
    radius = headR * 0.48f,
    center = tipOffset,
  )
  drawCircle(
    color = highlight.copy(alpha = 0.6f),
    radius = headR * 0.48f,
    center = tipOffset,
    style = Stroke(width = 1.5.dp.toPx()),
  )

  // Specular highlight arc
  val arcPath = Path().apply {
    moveTo(center.x - headR * 0.45f, center.y - headR * 0.45f)
    quadraticTo(center.x, center.y - headR * 0.75f, center.x + headR * 0.4f, center.y - headR * 0.35f)
  }
  drawPath(
    path = arcPath,
    color = Color.White.copy(alpha = 0.75f),
    style = Stroke(width = 2.dp.toPx()),
  )
}

@Composable
private fun BatteryRingCard(
  label: String,
  percent: Int?,
  color: Color,
  modifier: Modifier = Modifier,
) {
  val extended = LocalBluePulseColors.current
  Surface(
    modifier = modifier,
    shape = MaterialTheme.shapes.medium,
    color = MaterialTheme.colorScheme.surfaceContainerHigh,
    border = extended.cardBorder,
  ) {
    Column(
      modifier = Modifier.padding(12.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Text(
        text = percent?.let { "$it%" } ?: "--",
        style = CondensedMediumMetricStyle,
        color = if (percent != null) color else MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}
