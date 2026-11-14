package com.example.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.BluetoothSearching
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Radar
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.BluetoothDeviceUiModel
import com.example.model.RssiSample
import com.example.ui.MainUiState
import com.example.ui.MainViewModel
import com.example.ui.theme.LocalBluePulseColors
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun RadarSignalScreen(
  state: MainUiState,
  onSelectTarget: (String) -> Unit,
  onToggleScan: () -> Unit,
  onStopRadar: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val extended = LocalBluePulseColors.current
  val haptics = LocalHapticFeedback.current
  val targetDevice = state.devices.firstOrNull { it.address == state.radarTargetAddress }
  val samples = targetDevice?.let { state.rssiHistory[it.address].orEmpty() }.orEmpty()
  val liveRssi = targetDevice?.rssiDbm ?: samples.lastOrNull()?.rssiDbm

  LaunchedEffect(liveRssi, state.preferences.radarHapticsEnabled) {
    if (state.preferences.radarHapticsEnabled && liveRssi != null && liveRssi >= -55) {
      haptics.performHapticFeedback(HapticFeedbackType.LongPress)
    }
  }

  Box(
    modifier = modifier.fillMaxSize(),
    contentAlignment = Alignment.TopCenter,
  ) {
    LazyColumn(
      modifier = Modifier
        .widthIn(max = 760.dp)
        .fillMaxSize(),
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      item {
        Surface(
          shape = MaterialTheme.shapes.large,
          color = MaterialTheme.colorScheme.surfaceContainer,
          border = extended.cardBorder,
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Icon(
                  imageVector = Icons.Rounded.Radar,
                  contentDescription = "Signal Radar",
                  tint = MaterialTheme.colorScheme.primary,
                )
                Text("Live RSSI Device Finder", style = MaterialTheme.typography.titleMedium)
              }

              Button(
                onClick = onToggleScan,
                modifier = Modifier.testTag("radar_scan_toggle"),
              ) {
                Icon(
                  imageVector = Icons.AutoMirrored.Rounded.BluetoothSearching,
                  contentDescription = null,
                  modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (state.isScanning) "Stop" else "Ping")
              }
            }

            if (state.devices.isNotEmpty()) {
              LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.devices, key = { it.address }) { dev ->
                  FilterChip(
                    selected = dev.address == targetDevice?.address,
                    onClick = { onSelectTarget(dev.address) },
                    label = { Text(dev.name) },
                    modifier = Modifier.testTag("radar_chip_${dev.address}"),
                  )
                }
              }
            } else {
              Text(
                text = "No paired or scanned devices yet. Run a scan from the Hub or tap Ping to locate nearby beacons.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      }

      item {
        RadarSonarCard(
          targetDevice = targetDevice,
          liveRssi = liveRssi,
          isScanning = state.isScanning,
          onStopRadar = onStopRadar,
        )
      }

      item {
        RssiWaveformCard(
          samples = samples,
          liveRssi = liveRssi,
        )
      }

      item {
        CodecInspectorCard(targetDevice = targetDevice)
      }
    }
  }
}

@Composable
private fun RadarSonarCard(
  targetDevice: BluetoothDeviceUiModel?,
  liveRssi: Int?,
  isScanning: Boolean,
  onStopRadar: () -> Unit,
) {
  val extended = LocalBluePulseColors.current
  val primary = MaterialTheme.colorScheme.primary
  val signalColor = when {
    liveRssi == null -> MaterialTheme.colorScheme.onSurfaceVariant
    liveRssi >= -58 -> extended.signalGood
    liveRssi >= -76 -> primary
    else -> extended.signalWarn
  }

  val transition = rememberInfiniteTransition(label = "radar_sweep")
  val sweepAngle by transition.animateFloat(
    initialValue = 0f,
    targetValue = 360f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 2800, easing = LinearEasing),
      repeatMode = RepeatMode.Restart,
    ),
    label = "sweep_deg",
  )

  Surface(
    shape = MaterialTheme.shapes.large,
    color = MaterialTheme.colorScheme.surfaceContainer,
    border = extended.cardBorder,
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(220.dp),
        contentAlignment = Alignment.Center,
      ) {
        Canvas(modifier = Modifier.size(204.dp)) {
          val center = Offset(size.width / 2f, size.height / 2f)
          val maxRadius = size.minDimension / 2f

          listOf(0.33f, 0.66f, 1.0f).forEach { factor ->
            drawCircle(
              color = primary.copy(alpha = 0.22f),
              radius = maxRadius * factor,
              center = center,
              style = Stroke(width = 1.5.dp.toPx()),
            )
          }

          drawLine(
            color = primary.copy(alpha = 0.14f),
            start = Offset(center.x - maxRadius, center.y),
            end = Offset(center.x + maxRadius, center.y),
            strokeWidth = 1.dp.toPx(),
          )
          drawLine(
            color = primary.copy(alpha = 0.14f),
            start = Offset(center.x, center.y - maxRadius),
            end = Offset(center.x, center.y + maxRadius),
            strokeWidth = 1.dp.toPx(),
          )

          if (isScanning || targetDevice != null) {
            val rad = Math.toRadians(sweepAngle.toDouble())
            val end = Offset(
              x = center.x + (maxRadius * cos(rad)).toFloat(),
              y = center.y + (maxRadius * sin(rad)).toFloat(),
            )
            drawLine(
              brush = Brush.linearGradient(
                colors = listOf(primary.copy(alpha = 0.1f), primary),
                start = center,
                end = end,
              ),
              start = center,
              end = end,
              strokeWidth = 3.dp.toPx(),
              cap = StrokeCap.Round,
            )
          }

          drawCircle(
            color = primary,
            radius = 6.dp.toPx(),
            center = center,
          )

          if (liveRssi != null) {
            val normalizedDist = ((-liveRssi - 35f) / 65f).coerceIn(0.12f, 0.94f)
            val angleRad = Math.toRadians(((targetDevice?.address?.hashCode() ?: 45) % 360).toDouble())
            val dotPos = Offset(
              x = center.x + (maxRadius * normalizedDist * cos(angleRad)).toFloat(),
              y = center.y + (maxRadius * normalizedDist * sin(angleRad)).toFloat(),
            )
            drawCircle(
              color = signalColor.copy(alpha = 0.28f),
              radius = 18.dp.toPx(),
              center = dotPos,
            )
            drawCircle(
              color = signalColor,
              radius = 8.dp.toPx(),
              center = dotPos,
            )
          }
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column {
          Text(
            text = targetDevice?.name ?: "No Active Radar Target",
            style = MaterialTheme.typography.titleMedium,
          )
          Text(
            text = liveRssi?.let {
              "${MainViewModel.estimateProximityLabel(it)} • $it dBm"
            } ?: "Select a device or start scanning to sample live RSSI",
            style = MaterialTheme.typography.labelMedium,
            color = signalColor,
          )
        }

        if (targetDevice != null) {
          OutlinedButton(
            onClick = onStopRadar,
            modifier = Modifier.testTag("radar_stop_track_button"),
          ) {
            Text("Pause")
          }
        }
      }
    }
  }
}

@Composable
private fun RssiWaveformCard(
  samples: List<RssiSample>,
  liveRssi: Int?,
) {
  val extended = LocalBluePulseColors.current
  val primary = MaterialTheme.colorScheme.primary
  val surfaceBg = MaterialTheme.colorScheme.surfaceContainerHigh

  Surface(
    shape = MaterialTheme.shapes.large,
    color = MaterialTheme.colorScheme.surfaceContainer,
    border = extended.cardBorder,
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Icon(
            imageVector = Icons.Rounded.Sensors,
            contentDescription = "Signal strength history",
            tint = primary,
          )
          Text("Signal Strength Waveform (dBm)", style = MaterialTheme.typography.titleMedium)
        }
        Text(
          text = liveRssi?.let { "$it dBm" } ?: "-- dBm",
          style = MaterialTheme.typography.labelMedium,
          color = primary,
        )
      }

      Canvas(
        modifier = Modifier
          .fillMaxWidth()
          .height(110.dp)
          .clip(RoundedCornerShape(14.dp))
          .background(surfaceBg),
      ) {
        if (samples.size >= 2) {
          val path = Path()
          val stepX = size.width / (samples.size - 1).coerceAtLeast(1)
          samples.forEachIndexed { idx, sample ->
            val normY = ((-sample.rssiDbm - 30f) / 70f).coerceIn(0.05f, 0.95f)
            val x = idx * stepX
            val y = normY * size.height
            if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
          }
          drawPath(
            path = path,
            color = primary,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
          )
        } else {
          drawLine(
            color = primary.copy(alpha = 0.25f),
            start = Offset(12.dp.toPx(), size.height / 2f),
            end = Offset(size.width - 12.dp.toPx(), size.height / 2f),
            strokeWidth = 2.dp.toPx(),
          )
        }
      }
    }
  }
}

@Composable
private fun CodecInspectorCard(targetDevice: BluetoothDeviceUiModel?) {
  val extended = LocalBluePulseColors.current
  val codec = targetDevice?.codecInfo

  Surface(
    shape = MaterialTheme.shapes.large,
    color = MaterialTheme.colorScheme.surfaceContainer,
    border = extended.cardBorder,
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Icon(
          imageVector = Icons.Rounded.GraphicEq,
          contentDescription = "Audio Codec Inspector",
          tint = MaterialTheme.colorScheme.secondary,
        )
        Text("Audio Codec & Link Telemetry", style = MaterialTheme.typography.titleMedium)
      }

      if (codec == null) {
        Text(
          text = "Connect or select an audio Bluetooth device to inspect A2DP / LE Audio codec parameters.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      } else {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          CodecSpecTile("Codec", codec.codecName, Modifier.weight(1f))
          CodecSpecTile("Sample Rate", codec.sampleRateKhz, Modifier.weight(1f))
          CodecSpecTile("Bit Depth", codec.bitsPerSample, Modifier.weight(1f))
        }
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          CodecSpecTile("Channel", codec.channelMode, Modifier.weight(1f))
          CodecSpecTile("Bitrate", codec.bitrateLabel, Modifier.weight(1f))
          CodecSpecTile("Est. Latency", "${codec.estimatedLatencyMs} ms", Modifier.weight(1f))
        }
      }
    }
  }
}

@Composable
private fun CodecSpecTile(
  label: String,
  value: String,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier,
    shape = MaterialTheme.shapes.small,
    color = MaterialTheme.colorScheme.surfaceContainerHigh,
  ) {
    Column(
      modifier = Modifier.padding(10.dp),
      verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Text(
        text = value,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurface,
      )
    }
  }
}
