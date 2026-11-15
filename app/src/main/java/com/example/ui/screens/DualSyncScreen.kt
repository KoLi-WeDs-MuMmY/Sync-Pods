package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.SpeakerGroup
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.AudioOutputSink
import com.example.model.BluetoothDeviceUiModel
import com.example.model.CalibrationMode
import com.example.model.DualAudioRouteMode
import com.example.model.FeatureGuideId
import com.example.model.MediaPlaybackState
import com.example.ui.MainUiState
import com.example.ui.components.ExpressiveInfoButton
import com.example.ui.components.WavyAudioScrubber
import com.example.ui.theme.LocalBluePulseColors
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun DualSyncScreen(
  state: MainUiState,
  onSetDualRouteMode: (DualAudioRouteMode) -> Unit,
  onSetMonoMixSplit: (Boolean) -> Unit,
  onAssignSinkA: (Int) -> Unit,
  onAssignSinkB: (Int) -> Unit,
  onPickAudioFile: () -> Unit,
  onClearAudioFile: () -> Unit,
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
  onOpenGuide: (FeatureGuideId) -> Unit,
  modifier: Modifier = Modifier,
) {
  val extended = LocalBluePulseColors.current
  val sync = state.dualSync

  val sinkAName = state.availableAudioSinks.firstOrNull { it.id == sync.sinkAId }?.let {
    "${it.name} (${it.typeLabel})"
  } ?: "Auto Primary Sink"
  val sinkBName = state.availableAudioSinks.firstOrNull { it.id == sync.sinkBId }?.let {
    "${it.name} (${it.typeLabel})"
  } ?: "Auto Secondary Sink"

  val deviceAName = state.devices.firstOrNull { it.address == sync.deviceAAddress }?.name ?: "Output A Profile"
  val deviceBName = state.devices.firstOrNull { it.address == sync.deviceBAddress }?.name ?: "Output B Profile"
  val latencyDelta = sync.deviceBLatencyMs - sync.deviceALatencyMs

  Box(
    modifier = modifier.fillMaxSize(),
    contentAlignment = Alignment.TopCenter,
  ) {
    LazyColumn(
      modifier = Modifier
        .widthIn(max = 760.dp)
        .fillMaxSize(),
      contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 104.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      // 1. 4-Mode Dual-Audio Bridge Strategy Selector (works even on non-Dual-BT phones)
      item {
        DualBridgeStrategyCard(
          routeMode = sync.routeMode,
          monoMixForSplit = sync.monoMixForSplit,
          leAudioSupported = sync.leAudioSupported,
          leAudioBroadcastSupported = sync.leAudioBroadcastSupported,
          onSelectMode = onSetDualRouteMode,
          onSetMonoMixSplit = onSetMonoMixSplit,
          onOpenGuide = { onOpenGuide(FeatureGuideId.DUAL_BRIDGE) },
        )
      }

      // 2. Expressive Dual Audio File Player & Calibration Studio (Coral Hero Play + Wavy Scrubber)
      item {
        DualMediaPlayerAndCalibrationCard(
          media = state.mediaPlayback,
          mode = sync.calibrationMode,
          isPlaying = sync.isCalibrationPlaying,
          beatIndex = sync.calibrationBeatIndex,
          latencyAMs = sync.deviceALatencyMs,
          latencyBMs = sync.deviceBLatencyMs,
          onPickAudioFile = onPickAudioFile,
          onClearAudioFile = onClearAudioFile,
          onSelectMode = onCalibrationModeChange,
          onTogglePlay = onToggleCalibrationTone,
          onResetOffsets = {
            onDeviceALatencyChange(0)
            onDeviceBLatencyChange(0)
          },
          onOpenLatencyGuide = { onOpenGuide(FeatureGuideId.LATENCY_CALIBRATION) },
        )
      }

      // 3. Synchronized vs Independent Master Gain Card
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
                modifier = Modifier.weight(1f),
              ) {
                Icon(
                  imageVector = if (sync.isLinked) Icons.Rounded.Link else Icons.Rounded.LinkOff,
                  contentDescription = "Dual link mode",
                  tint = MaterialTheme.colorScheme.primary,
                )
                Column {
                  Text(
                    text = if (sync.isLinked) "Synchronized Dual Output" else "Independent Output Control",
                    style = MaterialTheme.typography.titleMedium,
                  )
                  Text(
                    text = if (sync.isLinked) {
                      "Output A & B share identical volume & delay"
                    } else {
                      "Separate gain & latency calibrated (Δ ${formatMs(latencyDelta)})"
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
                imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                contentDescription = "Master output volume",
                tint = MaterialTheme.colorScheme.primary,
              )
              Column(modifier = Modifier.weight(1f)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                  Text("Master Bridge Gain", style = MaterialTheme.typography.labelLarge)
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
          }
        }
      }

      // 4. Output A Deck (AudioSink + Bluetooth Profile + Gain + ms Latency + L/R Balance)
      item {
        OutputDeckCard(
          label = "OUTPUT A",
          selectedSinkName = sinkAName,
          availableSinks = state.availableAudioSinks,
          selectedDeviceName = deviceAName,
          availableDevices = state.devices,
          volume = sync.deviceAVolume,
          latencyMs = sync.deviceALatencyMs,
          balance = sync.deviceABalance,
          enabledIndividualControls = !sync.isLinked,
          tagPrefix = "deck_a",
          onSelectSink = onAssignSinkA,
          onSelectDevice = onAssignDeviceA,
          onVolumeChange = onDeviceAVolumeChange,
          onLatencyChange = onDeviceALatencyChange,
          onBalanceChange = onDeviceABalanceChange,
        )
      }

      // 5. Output B Deck
      item {
        OutputDeckCard(
          label = "OUTPUT B",
          selectedSinkName = sinkBName,
          availableSinks = state.availableAudioSinks,
          selectedDeviceName = deviceBName,
          availableDevices = state.devices,
          volume = sync.deviceBVolume,
          latencyMs = sync.deviceBLatencyMs,
          balance = sync.deviceBBalance,
          enabledIndividualControls = !sync.isLinked,
          tagPrefix = "deck_b",
          onSelectSink = onAssignSinkB,
          onSelectDevice = onAssignDeviceB,
          onVolumeChange = onDeviceBVolumeChange,
          onLatencyChange = onDeviceBLatencyChange,
          onBalanceChange = onDeviceBBalanceChange,
        )
      }
    }
  }
}

@Composable
private fun DualBridgeStrategyCard(
  routeMode: DualAudioRouteMode,
  monoMixForSplit: Boolean,
  leAudioSupported: Boolean,
  leAudioBroadcastSupported: Boolean,
  onSelectMode: (DualAudioRouteMode) -> Unit,
  onSetMonoMixSplit: (Boolean) -> Unit,
  onOpenGuide: () -> Unit,
) {
  val extended = LocalBluePulseColors.current

  Surface(
    shape = MaterialTheme.shapes.extraLarge,
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
          modifier = Modifier.weight(1f),
        ) {
          Box(
            modifier = Modifier
              .size(42.dp)
              .clip(CircleShape)
              .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = Icons.Rounded.SpeakerGroup,
              contentDescription = "Dual Audio Bridge",
              tint = MaterialTheme.colorScheme.primary,
            )
          }
          Column {
            Text("Dual-Audio Bridge", style = MaterialTheme.typography.headlineSmall)
            Text(
              text = if (leAudioSupported) {
                "LE Audio (${if (leAudioBroadcastSupported) "Auracast" else "Unicast"}) + Universal Bridge"
              } else {
                "Universal Dual-Output active on Classic BT phones"
              },
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.primary,
            )
          }
        }

        ExpressiveInfoButton(
          contentDescription = "How Dual-Audio Bridge works on non-dual-Bluetooth phones",
          testTag = "dual_bridge_info_btn",
          onClick = onOpenGuide,
        )
      }

      LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(DualAudioRouteMode.entries) { mode ->
          val selected = routeMode == mode
          Surface(
            shape = MaterialTheme.shapes.medium,
            color = if (selected) {
              MaterialTheme.colorScheme.primaryContainer
            } else {
              MaterialTheme.colorScheme.surfaceContainerHigh
            },
            border = extended.cardBorder,
            modifier = Modifier
              .width(172.dp)
              .clip(MaterialTheme.shapes.medium)
              .clickable { onSelectMode(mode) }
              .testTag("route_mode_${mode.name.lowercase()}"),
          ) {
            Column(
              modifier = Modifier.padding(12.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Surface(
                shape = RoundedCornerShape(50),
                color = if (selected) extended.coralHero else MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
              ) {
                Text(
                  text = mode.badge,
                  style = MaterialTheme.typography.labelSmall,
                  color = if (selected) Color.White else MaterialTheme.colorScheme.primary,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                )
              }
              Text(
                text = mode.title,
                style = MaterialTheme.typography.titleSmall,
                color = if (selected) {
                  MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                  MaterialTheme.colorScheme.onSurface
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Text(
                text = mode.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }
        }
      }

      if (routeMode == DualAudioRouteMode.TWS_SPLIT_LR) {
        Surface(
          shape = MaterialTheme.shapes.medium,
          color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
              Text("Mono-Mix L & R Earbuds", style = MaterialTheme.typography.titleSmall)
              Text(
                text = "Send full mixed stereo into both Left & Right buds when sharing",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            Switch(
              checked = monoMixForSplit,
              onCheckedChange = onSetMonoMixSplit,
              modifier = Modifier.testTag("dual_mono_split_switch"),
            )
          }
        }
      }
    }
  }
}

@Composable
private fun DualMediaPlayerAndCalibrationCard(
  media: MediaPlaybackState,
  mode: CalibrationMode,
  isPlaying: Boolean,
  beatIndex: Int,
  latencyAMs: Int,
  latencyBMs: Int,
  onPickAudioFile: () -> Unit,
  onClearAudioFile: () -> Unit,
  onSelectMode: (CalibrationMode) -> Unit,
  onTogglePlay: () -> Unit,
  onResetOffsets: () -> Unit,
  onOpenLatencyGuide: () -> Unit,
) {
  val extended = LocalBluePulseColors.current
  val flashActive = isPlaying && beatIndex > 0
  val flashColor by animateColorAsState(
    targetValue = if (flashActive && beatIndex % 2 == 0) {
      extended.coralHero
    } else if (flashActive) {
      extended.signalGood
    } else {
      MaterialTheme.colorScheme.surfaceContainerHigh
    },
    label = "calibration_flash",
  )

  Surface(
    shape = MaterialTheme.shapes.extraLarge,
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
          modifier = Modifier.weight(1f),
        ) {
          Icon(
            imageVector = if (media.isCustomFile) Icons.Rounded.MusicNote else Icons.Rounded.GraphicEq,
            contentDescription = "Dual Audio Player",
            tint = extended.coralHero,
          )
          Column {
            Text(
              text = media.trackTitle,
              style = MaterialTheme.typography.titleMedium,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text = if (media.isCustomFile) {
                "Decoded Local Track • Dual Stream Active"
              } else {
                mode.description
              },
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(14.dp)
              .clip(CircleShape)
              .background(flashColor),
          )
          ExpressiveInfoButton(
            contentDescription = "How millisecond latency sync works",
            testTag = "dual_latency_info_btn",
            onClick = onOpenLatencyGuide,
          )
        }
      }

      // Local Audio File Picker + Clear
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        FilledTonalButton(
          onClick = onPickAudioFile,
          shape = RoundedCornerShape(50),
          modifier = Modifier
            .weight(1f)
            .height(48.dp)
            .testTag("dual_pick_audio_btn"),
        ) {
          Icon(Icons.Rounded.AudioFile, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(if (media.isCustomFile) "Change Track" else "Pick Audio File (MP3/WAV)")
        }

        if (media.isCustomFile) {
          IconButton(
            onClick = onClearAudioFile,
            modifier = Modifier
              .minimumInteractiveComponentSize()
              .testTag("dual_clear_audio_btn"),
          ) {
            Icon(Icons.Rounded.Clear, contentDescription = "Return to built-in synth")
          }
        }
      }

      if (!media.isCustomFile) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          items(CalibrationMode.entries) { option ->
            FilterChip(
              selected = mode == option,
              onClick = { onSelectMode(option) },
              label = { Text(option.title) },
              modifier = Modifier.testTag("calibration_mode_${option.name.lowercase()}"),
            )
          }
        }
      }

      // Expressive Wavy Scrubber & Time Readout
      val progress = if (media.durationMs > 0) {
        (media.positionMs.toFloat() / media.durationMs.toFloat()).coerceIn(0f, 1f)
      } else {
        0f
      }
      WavyAudioScrubber(
        progress = progress,
        isPlaying = isPlaying,
        activeColor = extended.coralHero,
      )
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(
          text = formatClock(media.positionMs),
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
          text = formatClock(media.durationMs),
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      // Delay Alignment Timeline Visualizer
      val primaryColor = MaterialTheme.colorScheme.primary
      val emeraldColor = extended.signalGood
      val trackColor = MaterialTheme.colorScheme.surfaceContainerHigh

      Canvas(
        modifier = Modifier
          .fillMaxWidth()
          .height(46.dp)
          .clip(RoundedCornerShape(14.dp))
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
          start = Offset(posA, 8.dp.toPx()),
          end = Offset(posA, midY - 2.dp.toPx()),
          strokeWidth = 5.dp.toPx(),
          cap = StrokeCap.Round,
        )
        drawLine(
          color = emeraldColor,
          start = Offset(posB, midY + 2.dp.toPx()),
          end = Offset(posB, size.height - 8.dp.toPx()),
          strokeWidth = 5.dp.toPx(),
          cap = StrokeCap.Round,
        )
      }

      // Coral Hero Play/Pause Pill + Zero Delay
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Button(
          onClick = onTogglePlay,
          colors = ButtonDefaults.buttonColors(
            containerColor = extended.coralHero,
            contentColor = Color.White,
          ),
          shape = RoundedCornerShape(50),
          modifier = Modifier
            .weight(1.3f)
            .height(54.dp)
            .testTag("calibration_play_toggle"),
        ) {
          Icon(
            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
            contentDescription = null,
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = if (isPlaying) {
              "PAUSE"
            } else if (media.isCustomFile) {
              "PLAY DUAL"
            } else {
              "RUN SYNC"
            },
          )
        }

        OutlinedButton(
          onClick = onResetOffsets,
          shape = RoundedCornerShape(50),
          modifier = Modifier
            .weight(0.8f)
            .height(54.dp)
            .testTag("calibration_reset_btn"),
        ) {
          Text("Zero ms")
        }
      }
    }
  }
}

@Composable
private fun OutputDeckCard(
  label: String,
  selectedSinkName: String,
  availableSinks: List<AudioOutputSink>,
  selectedDeviceName: String,
  availableDevices: List<BluetoothDeviceUiModel>,
  volume: Float,
  latencyMs: Int,
  balance: Float,
  enabledIndividualControls: Boolean,
  tagPrefix: String,
  onSelectSink: (Int) -> Unit,
  onSelectDevice: (String) -> Unit,
  onVolumeChange: (Float) -> Unit,
  onLatencyChange: (Int) -> Unit,
  onBalanceChange: (Float) -> Unit,
) {
  val extended = LocalBluePulseColors.current
  var sinkMenuExpanded by remember { mutableStateOf(false) }
  var btMenuExpanded by remember { mutableStateOf(false) }

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
        Surface(
          shape = RoundedCornerShape(50),
          color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
        ) {
          Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
          )
        }

        Text(
          text = formatMs(latencyMs),
          style = MaterialTheme.typography.labelMedium,
          color = if (abs(latencyMs) > 60) extended.signalWarn else MaterialTheme.colorScheme.primary,
        )
      }

      // Sink & Bluetooth Profile Pickers
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Box(modifier = Modifier.weight(1.1f)) {
          OutlinedButton(
            onClick = { if (availableSinks.isNotEmpty()) sinkMenuExpanded = true },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
              .fillMaxWidth()
              .height(48.dp)
              .testTag("${tagPrefix}_sink_selector"),
          ) {
            Text(
              text = selectedSinkName,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
          DropdownMenu(
            expanded = sinkMenuExpanded,
            onDismissRequest = { sinkMenuExpanded = false },
          ) {
            availableSinks.forEach { sink ->
              DropdownMenuItem(
                text = { Text("${sink.name} • ${sink.typeLabel}") },
                onClick = {
                  onSelectSink(sink.id)
                  sinkMenuExpanded = false
                },
              )
            }
          }
        }

        Box(modifier = Modifier.weight(0.9f)) {
          OutlinedButton(
            onClick = { if (availableDevices.isNotEmpty()) btMenuExpanded = true },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
              .fillMaxWidth()
              .height(48.dp)
              .testTag("${tagPrefix}_device_selector"),
          ) {
            Icon(
              imageVector = Icons.Rounded.Headphones,
              contentDescription = null,
              modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(selectedDeviceName, maxLines = 1, overflow = TextOverflow.Ellipsis)
          }
          DropdownMenu(
            expanded = btMenuExpanded,
            onDismissRequest = { btMenuExpanded = false },
          ) {
            availableDevices.forEach { dev ->
              DropdownMenuItem(
                text = { Text("${dev.name} (${dev.address.takeLast(5)})") },
                onClick = {
                  onSelectDevice(dev.address)
                  btMenuExpanded = false
                },
              )
            }
          }
        }
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
                .minimumInteractiveComponentSize()
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
                .minimumInteractiveComponentSize()
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

private fun formatMs(ms: Int): String = if (ms >= 0) "+${ms} ms" else "$ms ms"

private fun formatClock(ms: Long): String {
  val totalSec = (ms / 1000L).coerceAtLeast(0L)
  val min = totalSec / 60L
  val sec = totalSec % 60L
  return "%d:%02d".format(min, sec)
}
