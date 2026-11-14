package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.BluetoothDeviceUiModel
import com.example.model.CalibrationMode
import com.example.ui.MainUiState
import com.example.ui.theme.LocalBluePulseColors
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun DualSyncScreen(
  state: MainUiState,
  onSetLinked: (Boolean) -> Unit,
  onMasterVolumeChange: (Float) -> Unit,
  onAssignDeviceA: (String) -> Unit,
  onAssignDeviceB: (String) -> Unit,
  onDeviceAVolumeChange: (Float) -> Unit,
  onDeviceBVolumeChange: (Float) -> Unit,
  onDeviceALatencyChange: (Int) -> Unit,
  onDeviceBLatencyChange: (Int) -> Unit,
  onDeviceABalanceChange: (Float) -> Unit,
  onDeviceBBalanceChange: (Float) -> Unit,
  onCalibrationModeChange: (CalibrationMode) -> Unit,
  onToggleCalibrationTone: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val extended = LocalBluePulseColors.current
  val sync = state.dualSync
  val deviceAName = state.devices.firstOrNull { it.address == sync.deviceAAddress }?.name ?: "Output A (Left / Primary)"
  val deviceBName = state.devices.firstOrNull { it.address == sync.deviceBAddress }?.name ?: "Output B (Right / Secondary)"
  val latencyDelta = sync.deviceBLatencyMs - sync.deviceALatencyMs

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
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                Icon(
                  imageVector = if (sync.isLinked) Icons.Rounded.Link else Icons.Rounded.LinkOff,
                  contentDescription = "Dual link mode",
                  tint = MaterialTheme.colorScheme.primary,
                )
                Column {
                  Text(
                    text = if (sync.isLinked) "Synchronized Dual Output" else "Independent Channel Control",
                    style = MaterialTheme.typography.titleMedium,
                  )
                  Text(
                    text = if (sync.isLinked) {
                      "Both outputs mirror identical volume & delay"
                    } else {
                      "Adjust Output A and Output B separately (Δ ${formatMs(latencyDelta)})"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }

              Switch(
                checked = sync.isLinked,
                onCheckedChange = onSetLinked,
                modifier = Modifier.testTag("dual_sync_link_switch"),
              )
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Icon(
                imageVector = Icons.Rounded.VolumeUp,
                contentDescription = "Master output volume",
                tint = MaterialTheme.colorScheme.primary,
              )
              Column(modifier = Modifier.weight(1f)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                  Text("Master Output Gain", style = MaterialTheme.typography.labelLarge)
                  Text(
                    text = "${(sync.masterVolume * 100).roundToInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                  )
                }
                Slider(
                  value = sync.masterVolume,
                  onValueChange = onMasterVolumeChange,
                  modifier = Modifier.testTag("dual_master_volume_slider"),
                )
              }
            }

            Surface(
              shape = MaterialTheme.shapes.small,
              color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
              Text(
                text = if (sync.leAudioSupported) {
                  "Hardware Status: Bluetooth LE Audio (${if (sync.leAudioBroadcastSupported) "Auracast Broadcast Ready" else "CIS Unicast Ready"}) + Dual Studio active."
                } else {
                  "Hardware Status: Classic A2DP handset detected. Use Dual Studio below to calibrate split L/R channels & millisecond delay offsets."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(10.dp),
              )
            }
          }
        }
      }

      item {
        OutputDeckCard(
          label = "OUTPUT A",
          selectedDeviceName = deviceAName,
          availableDevices = state.devices,
          volume = sync.deviceAVolume,
          latencyMs = sync.deviceALatencyMs,
          balance = sync.deviceABalance,
          enabledIndividualControls = !sync.isLinked,
          tagPrefix = "deck_a",
          onSelectDevice = onAssignDeviceA,
          onVolumeChange = onDeviceAVolumeChange,
          onLatencyChange = onDeviceALatencyChange,
          onBalanceChange = onDeviceABalanceChange,
        )
      }

      item {
        OutputDeckCard(
          label = "OUTPUT B",
          selectedDeviceName = deviceBName,
          availableDevices = state.devices,
          volume = sync.deviceBVolume,
          latencyMs = sync.deviceBLatencyMs,
          balance = sync.deviceBBalance,
          enabledIndividualControls = !sync.isLinked,
          tagPrefix = "deck_b",
          onSelectDevice = onAssignDeviceB,
          onVolumeChange = onDeviceBVolumeChange,
          onLatencyChange = onDeviceBLatencyChange,
          onBalanceChange = onDeviceBBalanceChange,
        )
      }

      item {
        CalibrationStudioCard(
          isPlaying = sync.isCalibrationPlaying,
          beatIndex = sync.calibrationBeatIndex,
          mode = sync.calibrationMode,
          latencyAMs = sync.deviceALatencyMs,
          latencyBMs = sync.deviceBLatencyMs,
          onSelectMode = onCalibrationModeChange,
          onTogglePlay = onToggleCalibrationTone,
          onResetOffsets = {
            onDeviceALatencyChange(0)
            onDeviceBLatencyChange(0)
          },
        )
      }
    }
  }
}

@Composable
private fun OutputDeckCard(
  label: String,
  selectedDeviceName: String,
  availableDevices: List<BluetoothDeviceUiModel>,
  volume: Float,
  latencyMs: Int,
  balance: Float,
  enabledIndividualControls: Boolean,
  tagPrefix: String,
  onSelectDevice: (String) -> Unit,
  onVolumeChange: (Float) -> Unit,
  onLatencyChange: (Int) -> Unit,
  onBalanceChange: (Float) -> Unit,
) {
  val extended = LocalBluePulseColors.current
  var dropdownExpanded by remember { mutableStateOf(false) }

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
          modifier = Modifier.weight(1f),
        ) {
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
          ) {
            Text(
              text = label,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.primary,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )
          }

          Box {
            OutlinedButton(
              onClick = { if (availableDevices.isNotEmpty()) dropdownExpanded = true },
              modifier = Modifier.testTag("${tagPrefix}_device_selector"),
            ) {
              Icon(
                imageVector = Icons.Rounded.Headphones,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(selectedDeviceName, maxLines = 1)
            }
            DropdownMenu(
              expanded = dropdownExpanded,
              onDismissRequest = { dropdownExpanded = false },
            ) {
              availableDevices.forEach { dev ->
                DropdownMenuItem(
                  text = { Text("${dev.name} (${dev.address.takeLast(5)})") },
                  onClick = {
                    onSelectDevice(dev.address)
                    dropdownExpanded = false
                  },
                )
              }
            }
          }
        }

        Text(
          text = formatMs(latencyMs),
          style = MaterialTheme.typography.labelMedium,
          color = if (abs(latencyMs) > 60) extended.signalWarn else MaterialTheme.colorScheme.primary,
        )
      }

      Column {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text(
            text = if (enabledIndividualControls) "Channel Gain" else "Channel Gain (Linked)",
            style = MaterialTheme.typography.bodySmall,
          )
          Text(
            text = "${(volume * 100).roundToInt()}%",
            style = MaterialTheme.typography.labelMedium,
          )
        }
        Slider(
          value = volume,
          onValueChange = onVolumeChange,
          modifier = Modifier.testTag("${tagPrefix}_volume_slider"),
        )
      }

      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text("Audio Latency Offset", style = MaterialTheme.typography.bodySmall)
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            FilledTonalIconButton(
              onClick = { onLatencyChange(latencyMs - 5) },
              modifier = Modifier
                .size(36.dp)
                .testTag("${tagPrefix}_latency_minus"),
            ) {
              Icon(Icons.Rounded.Remove, contentDescription = "Decrease latency by 5ms", modifier = Modifier.size(16.dp))
            }
            Text(
              text = formatMs(latencyMs),
              style = MaterialTheme.typography.labelMedium,
            )
            FilledTonalIconButton(
              onClick = { onLatencyChange(latencyMs + 5) },
              modifier = Modifier
                .size(36.dp)
                .testTag("${tagPrefix}_latency_plus"),
            ) {
              Icon(Icons.Rounded.Add, contentDescription = "Increase latency by 5ms", modifier = Modifier.size(16.dp))
            }
          }
        }

        Slider(
          value = latencyMs.toFloat(),
          onValueChange = { onLatencyChange(it.roundToInt()) },
          valueRange = -250f..250f,
          modifier = Modifier.testTag("${tagPrefix}_latency_slider"),
        )
      }

      Column {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text("Stereo Balance (L / R)", style = MaterialTheme.typography.bodySmall)
          Text(
            text = when {
              balance < -0.08f -> "L ${(-balance * 100).roundToInt()}%"
              balance > 0.08f -> "R ${(balance * 100).roundToInt()}%"
              else -> "Center"
            },
            style = MaterialTheme.typography.labelMedium,
          )
        }
        Slider(
          value = balance,
          onValueChange = onBalanceChange,
          valueRange = -1f..1f,
          modifier = Modifier.testTag("${tagPrefix}_balance_slider"),
        )
      }
    }
  }
}

@Composable
private fun CalibrationStudioCard(
  isPlaying: Boolean,
  beatIndex: Int,
  mode: CalibrationMode,
  latencyAMs: Int,
  latencyBMs: Int,
  onSelectMode: (CalibrationMode) -> Unit,
  onTogglePlay: () -> Unit,
  onResetOffsets: () -> Unit,
) {
  val extended = LocalBluePulseColors.current
  val flashActive = isPlaying && beatIndex > 0
  val flashColor by animateColorAsState(
    targetValue = if (flashActive && beatIndex % 2 == 0) {
      MaterialTheme.colorScheme.primary
    } else if (flashActive) {
      extended.signalGood
    } else {
      MaterialTheme.colorScheme.surfaceContainerHigh
    },
    label = "calibration_flash",
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
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Icon(
            imageVector = Icons.Rounded.GraphicEq,
            contentDescription = "Calibration Studio",
            tint = MaterialTheme.colorScheme.primary,
          )
          Column {
            Text("Audio Sync Calibration Studio", style = MaterialTheme.typography.titleMedium)
            Text(mode.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
        }

        Box(
          modifier = Modifier
            .size(18.dp)
            .clip(CircleShape)
            .background(flashColor),
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        CalibrationMode.entries.forEach { option ->
          FilterChip(
            selected = mode == option,
            onClick = { onSelectMode(option) },
            label = { Text(option.title) },
            modifier = Modifier.testTag("calibration_mode_${option.name.lowercase()}"),
          )
        }
      }

      val primaryColor = MaterialTheme.colorScheme.primary
      val emeraldColor = extended.signalGood
      val trackColor = MaterialTheme.colorScheme.surfaceContainerHigh

      Canvas(
        modifier = Modifier
          .fillMaxWidth()
          .height(54.dp)
          .clip(RoundedCornerShape(12.dp))
          .background(trackColor),
      ) {
        val midY = size.height / 2f
        val centerX = size.width / 2f
        drawLine(
          color = primaryColor.copy(alpha = 0.25f),
          start = Offset(centerX, 6.dp.toPx()),
          end = Offset(centerX, size.height - 6.dp.toPx()),
          strokeWidth = 2.dp.toPx(),
        )
        val posA = centerX + (latencyAMs / 250f) * (size.width * 0.4f)
        val posB = centerX + (latencyBMs / 250f) * (size.width * 0.4f)
        drawLine(
          color = primaryColor,
          start = Offset(posA, 10.dp.toPx()),
          end = Offset(posA, midY - 2.dp.toPx()),
          strokeWidth = 5.dp.toPx(),
          cap = StrokeCap.Round,
        )
        drawLine(
          color = emeraldColor,
          start = Offset(posB, midY + 2.dp.toPx()),
          end = Offset(posB, size.height - 10.dp.toPx()),
          strokeWidth = 5.dp.toPx(),
          cap = StrokeCap.Round,
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Button(
          onClick = onTogglePlay,
          modifier = Modifier
            .weight(1f)
            .testTag("calibration_play_toggle"),
        ) {
          Icon(
            imageVector = if (isPlaying) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
            contentDescription = null,
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(if (isPlaying) "Stop Test Pulse" else "Run Sync Test")
        }

        OutlinedButton(
          onClick = onResetOffsets,
          modifier = Modifier.testTag("calibration_reset_btn"),
        ) {
          Text("Zero Delay")
        }
      }
    }
  }
}

private fun formatMs(ms: Int): String = if (ms >= 0) "+${ms} ms" else "$ms ms"
