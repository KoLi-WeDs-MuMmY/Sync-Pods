package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoMode
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.audio.AudioSyncDspEngine
import com.example.data.DeviceProfileEntity
import com.example.model.FeatureGuideId
import com.example.ui.MainUiState
import com.example.ui.components.ExpressiveInfoButton
import com.example.ui.theme.LocalBluePulseColors
import kotlin.math.roundToInt

@Composable
fun EqRulesScreen(
  state: MainUiState,
  onSelectEqDevice: (String) -> Unit,
  onApplyPreset: (String) -> Unit,
  onBandChange: (Int, Int) -> Unit,
  onBassVirtualizerBalanceChange: (Int, Int, Float) -> Unit,
  onUpdateAutoRules: (String, String, Boolean, Boolean, Boolean, Int) -> Unit,
  onOpenGuide: (FeatureGuideId) -> Unit,
  modifier: Modifier = Modifier,
) {
  val extended = LocalBluePulseColors.current
  val selectedAddress = state.selectedEqDeviceAddress ?: "DEFAULT_PROFILE"
  val selectedDevice = state.devices.firstOrNull { it.address == selectedAddress }
  val selectedDeviceName = selectedDevice?.name ?: "Default Audio Profile"
  val savedRule = state.savedProfiles.firstOrNull { it.address == selectedAddress }
    ?: DeviceProfileEntity(address = selectedAddress, customName = selectedDeviceName)

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
      item {
        Surface(
          shape = MaterialTheme.shapes.extraLarge,
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
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f),
              ) {
                Icon(
                  imageVector = Icons.Rounded.GraphicEq,
                  contentDescription = "Per-device Equalizer",
                  tint = MaterialTheme.colorScheme.primary,
                )
                Column {
                  Text("Per-Device EQ & Rules", style = MaterialTheme.typography.headlineSmall)
                  Text(
                    text = if (state.dspHardwareAttached) {
                      "Session DSP Live • $selectedDeviceName"
                    } else {
                      "Software DSP + Room Saved • $selectedDeviceName"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = if (state.dspHardwareAttached) {
                      extended.signalGood
                    } else {
                      MaterialTheme.colorScheme.primary
                    },
                  )
                }
              }

              ExpressiveInfoButton(
                contentDescription = "How audible 5-band EQ and auto-connect rules work",
                testTag = "eq_info_btn",
                onClick = { onOpenGuide(FeatureGuideId.EQUALIZER_RULES) },
              )
            }

            if (state.devices.isNotEmpty()) {
              LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.devices, key = { it.address }) { dev ->
                  FilterChip(
                    selected = dev.address == selectedAddress,
                    onClick = { onSelectEqDevice(dev.address) },
                    label = { Text(dev.name) },
                    modifier = Modifier.testTag("eq_device_chip_${dev.address}"),
                  )
                }
              }
            }

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              items(AudioSyncDspEngine.PRESETS.keys.toList()) { preset ->
                FilterChip(
                  selected = state.activeEqPreset == preset,
                  onClick = { onApplyPreset(preset) },
                  label = { Text(preset) },
                  modifier = Modifier.testTag("eq_preset_${preset.lowercase().replace(" ", "_")}"),
                )
              }
            }
          }
        }
      }

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
                  imageVector = Icons.Rounded.Tune,
                  contentDescription = "5-Band Equalizer",
                  tint = MaterialTheme.colorScheme.secondary,
                )
                Text("5-Band Graphic Equalizer", style = MaterialTheme.typography.titleMedium)
              }

              Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
              ) {
                Text(
                  text = state.activeEqPreset,
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                )
              }
            }

            state.eqBands.forEach { band ->
              Column {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                  Text(band.centerFreqLabel, style = MaterialTheme.typography.labelMedium)
                  val db = band.levelMillibels / 100f
                  Text(
                    text = String.format("%+.1f dB", db),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                  )
                }
                Slider(
                  value = band.levelMillibels.toFloat(),
                  onValueChange = { onBandChange(band.index, it.roundToInt()) },
                  valueRange = band.minMillibels.toFloat()..band.maxMillibels.toFloat(),
                  modifier = Modifier.testTag("eq_band_slider_${band.index}"),
                )
              }
            }

            Column {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Text("Bass Boost", style = MaterialTheme.typography.bodySmall)
                Text("${state.bassBoostStrength / 10}%", style = MaterialTheme.typography.labelMedium)
              }
              Slider(
                value = state.bassBoostStrength.toFloat(),
                onValueChange = {
                  onBassVirtualizerBalanceChange(
                    it.roundToInt(),
                    state.virtualizerStrength,
                    state.channelBalance,
                  )
                },
                valueRange = 0f..1000f,
                modifier = Modifier.testTag("eq_bass_slider"),
              )
            }

            Column {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Text("Spatial Soundstage Width", style = MaterialTheme.typography.bodySmall)
                Text("${state.virtualizerStrength / 10}%", style = MaterialTheme.typography.labelMedium)
              }
              Slider(
                value = state.virtualizerStrength.toFloat(),
                onValueChange = {
                  onBassVirtualizerBalanceChange(
                    state.bassBoostStrength,
                    it.roundToInt(),
                    state.channelBalance,
                  )
                },
                valueRange = 0f..1000f,
                modifier = Modifier.testTag("eq_virtualizer_slider"),
              )
            }

            Column {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Text("L / R Earbud Balance", style = MaterialTheme.typography.bodySmall)
                Text(
                  text = when {
                    state.channelBalance < -0.05f -> "L ${(-state.channelBalance * 100).roundToInt()}%"
                    state.channelBalance > 0.05f -> "R ${(state.channelBalance * 100).roundToInt()}%"
                    else -> "Balanced"
                  },
                  style = MaterialTheme.typography.labelMedium,
                )
              }
              Slider(
                value = state.channelBalance,
                onValueChange = {
                  onBassVirtualizerBalanceChange(
                    state.bassBoostStrength,
                    state.virtualizerStrength,
                    it,
                  )
                },
                valueRange = -1f..1f,
                modifier = Modifier.testTag("eq_balance_slider"),
              )
            }
          }
        }
      }

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
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Icon(
                imageVector = Icons.Rounded.BatteryChargingFull,
                contentDescription = "Battery Telemetry",
                tint = extended.signalGood,
              )
              Text("Earbud Battery Telemetry", style = MaterialTheme.typography.titleMedium)
            }

            val battery = selectedDevice?.batteryPercent
            if (battery != null) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Text(selectedDeviceName, style = MaterialTheme.typography.bodyMedium)
                Text(
                  text = "$battery%",
                  style = MaterialTheme.typography.titleMedium,
                  color = if (battery <= 20) extended.signalBad else extended.signalGood,
                )
              }
              LinearProgressIndicator(
                progress = { battery / 100f },
                modifier = Modifier.fillMaxWidth(),
                color = if (battery <= 20) extended.signalBad else extended.signalGood,
              )
            } else {
              Text(
                text = "Battery levels appear automatically when a paired Bluetooth headset reports GATT Battery Service (0x180F) or Android battery metadata.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      }

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
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Icon(
                imageVector = Icons.Rounded.AutoMode,
                contentDescription = "Auto-Connect Rules",
                tint = MaterialTheme.colorScheme.primary,
              )
              Column {
                Text("Auto-Connect Automation Rules", style = MaterialTheme.typography.titleMedium)
                Text(
                  text = "Saved per device ($selectedDeviceName)",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }

            RuleSwitchRow(
              title = "Auto-Apply Saved EQ & Latency",
              subtitle = "Restore EQ curve & ms delay immediately on Bluetooth connect",
              checked = savedRule.autoConnectApplyEq,
              testTag = "rule_apply_eq_switch",
              onCheckedChange = {
                onUpdateAutoRules(
                  selectedAddress,
                  selectedDeviceName,
                  it,
                  savedRule.autoLaunchIsland,
                  savedRule.autoSetVolume,
                  savedRule.autoVolumePercent,
                )
              },
            )

            RuleSwitchRow(
              title = "Trigger Floating Island Pop-Up",
              subtitle = "Show animated capsule card with codec & battery on connect",
              checked = savedRule.autoLaunchIsland,
              testTag = "rule_launch_island_switch",
              onCheckedChange = {
                onUpdateAutoRules(
                  selectedAddress,
                  selectedDeviceName,
                  savedRule.autoConnectApplyEq,
                  it,
                  savedRule.autoSetVolume,
                  savedRule.autoVolumePercent,
                )
              },
            )

            RuleSwitchRow(
              title = "Auto-Normalize Output Volume",
              subtitle = "Lock safe startup media volume at ${savedRule.autoVolumePercent}%",
              checked = savedRule.autoSetVolume,
              testTag = "rule_auto_volume_switch",
              onCheckedChange = {
                onUpdateAutoRules(
                  selectedAddress,
                  selectedDeviceName,
                  savedRule.autoConnectApplyEq,
                  savedRule.autoLaunchIsland,
                  it,
                  savedRule.autoVolumePercent,
                )
              },
            )

            if (savedRule.autoSetVolume) {
              Slider(
                value = savedRule.autoVolumePercent.toFloat(),
                onValueChange = {
                  onUpdateAutoRules(
                    selectedAddress,
                    selectedDeviceName,
                    savedRule.autoConnectApplyEq,
                    savedRule.autoLaunchIsland,
                    savedRule.autoSetVolume,
                    it.roundToInt(),
                  )
                },
                valueRange = 10f..100f,
                modifier = Modifier.testTag("rule_auto_volume_slider"),
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun RuleSwitchRow(
  title: String,
  subtitle: String,
  checked: Boolean,
  testTag: String,
  onCheckedChange: (Boolean) -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
      Text(text = title, style = MaterialTheme.typography.titleSmall)
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    Switch(
      checked = checked,
      onCheckedChange = onCheckedChange,
      modifier = Modifier.testTag(testTag),
    )
  }
}
