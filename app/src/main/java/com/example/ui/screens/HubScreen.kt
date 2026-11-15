package com.example.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.BluetoothSearching
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.BluetoothAudio
import androidx.compose.material.icons.rounded.BluetoothConnected
import androidx.compose.material.icons.rounded.BluetoothDisabled
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Radar
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SettingsBluetooth
import androidx.compose.material.icons.rounded.Speaker
import androidx.compose.material.icons.rounded.ViewInAr
import androidx.compose.material.icons.rounded.Watch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.AudioOutputSink
import com.example.model.BluetoothDeviceUiModel
import com.example.model.BondStatus
import com.example.model.DeviceCategory
import com.example.model.FeatureGuideId
import com.example.ui.MainUiState
import com.example.ui.components.ExpressiveInfoButton
import com.example.ui.components.ScallopedExpressiveRing
import com.example.ui.theme.LocalBluePulseColors

@Composable
fun HubScreen(
  state: MainUiState,
  onToggleScan: () -> Unit,
  onRefresh: () -> Unit,
  onLaunchCompanionPair: () -> Unit,
  onOpen3DModal: (BluetoothDeviceUiModel?) -> Unit,
  onPairOrConnect: (BluetoothDeviceUiModel) -> Unit,
  onOpenRadarForDevice: (String) -> Unit,
  onOpenEqForDevice: (String) -> Unit,
  onOpenPermissions: () -> Unit,
  onOpenSystemBt: () -> Unit,
  onPreviewIsland: () -> Unit,
  onOpenGuide: (FeatureGuideId) -> Unit,
  modifier: Modifier = Modifier,
) {
  val extended = LocalBluePulseColors.current

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
        HeroCockpitCard(
          state = state,
          onToggleScan = onToggleScan,
          onRefresh = onRefresh,
          onLaunchCompanionPair = onLaunchCompanionPair,
          onOpen3DPreview = { onOpen3DModal(state.devices.firstOrNull { it.isConnected } ?: state.devices.firstOrNull()) },
          onPreviewIsland = onPreviewIsland,
          onOpenHubGuide = { onOpenGuide(FeatureGuideId.HUB_PAIRING) },
          onOpenIslandGuide = { onOpenGuide(FeatureGuideId.FLOATING_ISLAND) },
        )
      }

      if (state.availableAudioSinks.isNotEmpty()) {
        item {
          ActiveAudioSinksRow(sinks = state.availableAudioSinks)
        }
      }

      if (!state.permissions.bluetoothGranted || !state.permissions.overlayGranted) {
        item {
          Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f),
            border = extended.cardBorder,
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = if (!state.permissions.bluetoothGranted) {
                    "Bluetooth Permissions Needed"
                  } else {
                    "Enable System Floating Island"
                  },
                  style = MaterialTheme.typography.titleSmall,
                  color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                  text = if (!state.permissions.bluetoothGranted) {
                    "Allow Nearby Devices so BluePulse can scan, pair, and read battery & RSSI."
                  } else {
                    "Grant Display Over Other Apps for system-wide Dynamic Island pop-ups."
                  },
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              Spacer(modifier = Modifier.width(10.dp))
              FilledTonalButton(
                onClick = onOpenPermissions,
                modifier = Modifier
                  .minimumInteractiveComponentSize()
                  .testTag("hub_grant_permissions_btn"),
              ) {
                Text("Setup")
              }
            }
          }
        }
      }

      if (!state.isBluetoothSupported || !state.isBluetoothEnabled) {
        item {
          Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceContainer,
            border = extended.cardBorder,
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              Icon(
                imageVector = Icons.Rounded.BluetoothDisabled,
                contentDescription = "Bluetooth inactive",
                tint = extended.signalWarn,
                modifier = Modifier.size(28.dp),
              )
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = if (!state.isBluetoothSupported) {
                    "No Physical Bluetooth Radio Detected"
                  } else {
                    "Bluetooth Radio Is Turned Off"
                  },
                  style = MaterialTheme.typography.titleSmall,
                )
                Text(
                  text = if (!state.isBluetoothSupported) {
                    "Dual Audio Bridge, Wired/USB-C routing, 3D Preview, and EQ remain active."
                  } else {
                    "Turn on Bluetooth in system settings to discover and pair wireless audio gear."
                  },
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              if (state.isBluetoothSupported) {
                OutlinedButton(
                  onClick = onOpenSystemBt,
                  modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("hub_open_bt_settings_btn"),
                ) {
                  Text("Enable")
                }
              }
            }
          }
        }
      }

      val connected = state.devices.filter { it.isConnected }
      val others = state.devices.filterNot { it.isConnected }

      if (connected.isNotEmpty()) {
        item {
          SectionHeader(
            title = "Active Wireless Links (${connected.size})",
            subtitle = "Tap 3D for interactive case & battery view",
          )
        }
        items(connected, key = { "conn_${it.address}" }) { device ->
          DeviceCard(
            device = device,
            onPrimaryAction = { onPairOrConnect(device) },
            onOpen3D = { onOpen3DModal(device) },
            onRadarClick = { onOpenRadarForDevice(device.address) },
            onEqClick = { onOpenEqForDevice(device.address) },
          )
        }
      }

      item {
        SectionHeader(
          title = if (others.isNotEmpty()) "Paired & Nearby Devices (${others.size})" else "Device Discovery",
          subtitle = if (state.isScanning) {
            "Scanning Classic & BLE advertisements..."
          } else {
            "Use Scan or Companion Pair to connect headsets"
          },
        )
      }

      if (others.isEmpty() && connected.isEmpty()) {
        item {
          EmptyScannerCard(
            isScanning = state.isScanning,
            onToggleScan = onToggleScan,
            onLaunchCompanionPair = onLaunchCompanionPair,
            onOpenSystemBt = onOpenSystemBt,
          )
        }
      } else {
        items(others, key = { "dev_${it.address}" }) { device ->
          DeviceCard(
            device = device,
            onPrimaryAction = { onPairOrConnect(device) },
            onOpen3D = { onOpen3DModal(device) },
            onRadarClick = { onOpenRadarForDevice(device.address) },
            onEqClick = { onOpenEqForDevice(device.address) },
          )
        }
      }
    }
  }
}

@Composable
private fun HeroCockpitCard(
  state: MainUiState,
  onToggleScan: () -> Unit,
  onRefresh: () -> Unit,
  onLaunchCompanionPair: () -> Unit,
  onOpen3DPreview: () -> Unit,
  onPreviewIsland: () -> Unit,
  onOpenHubGuide: () -> Unit,
  onOpenIslandGuide: () -> Unit,
) {
  val extended = LocalBluePulseColors.current
  val connectedCount = state.devices.count { it.isConnected }

  Surface(
    shape = MaterialTheme.shapes.extraLarge,
    color = MaterialTheme.colorScheme.surfaceContainer,
    border = extended.cardBorder,
  ) {
    Box(modifier = Modifier.fillMaxWidth()) {
      Image(
        painter = painterResource(id = R.drawable.img_audio_cockpit_hero),
        contentDescription = "BluePulse wireless audio cockpit banner",
        contentScale = ContentScale.Crop,
        alpha = if (extended.isAmoled) 0.34f else 0.24f,
        modifier = Modifier
          .fillMaxWidth()
          .height(226.dp),
      )

      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(226.dp)
          .background(
            Brush.verticalGradient(
              colors = listOf(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.96f),
              )
            )
          ),
      )

      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f),
          ) {
            ScallopedExpressiveRing(
              isAnimating = state.isScanning || connectedCount > 0,
              ringColor = if (connectedCount > 0) extended.signalGood else MaterialTheme.colorScheme.primary,
              size = 64.dp,
              petals = 10,
            ) {
              Icon(
                imageVector = if (connectedCount > 0) {
                  Icons.Rounded.BluetoothConnected
                } else {
                  Icons.Rounded.BluetoothAudio
                },
                contentDescription = "Bluetooth Engine",
                tint = if (connectedCount > 0) extended.signalGood else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(26.dp),
              )
            }

            Column {
              Text(
                text = "Wireless Hub",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onSurface,
              )
              Text(
                text = buildString {
                  append(if (state.isBluetoothEnabled) "Radio Online" else "Radio Standby")
                  append(" • ")
                  append(
                    if (state.dualSync.leAudioSupported) {
                      "LE Audio Ready"
                    } else {
                      "${state.availableAudioSinks.size} Audio Sinks"
                    }
                  )
                },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
              )
            }
          }

          Row(verticalAlignment = Alignment.CenterVertically) {
            ExpressiveInfoButton(
              contentDescription = "Learn how Bluetooth Hub and Companion Pairing work",
              testTag = "hub_info_btn",
              onClick = onOpenHubGuide,
            )
            IconButton(
              onClick = onRefresh,
              modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag("hub_refresh_button"),
            ) {
              Icon(
                imageVector = Icons.Rounded.Refresh,
                contentDescription = "Refresh Bluetooth devices",
              )
            }
          }
        }

        // Row 1: Primary Expressive Coral Scan + Companion Pair
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Button(
            onClick = onToggleScan,
            colors = ButtonDefaults.buttonColors(
              containerColor = extended.coralHero,
              contentColor = Color.White,
            ),
            shape = RoundedCornerShape(50),
            modifier = Modifier
              .weight(1f)
              .height(50.dp)
              .testTag("hub_scan_toggle_button"),
          ) {
            if (state.isScanning) {
              CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = Color.White,
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text("Stop Scan")
            } else {
              Icon(
                imageVector = Icons.AutoMirrored.Rounded.BluetoothSearching,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text("Scan")
            }
          }

          FilledTonalButton(
            onClick = onLaunchCompanionPair,
            shape = RoundedCornerShape(50),
            modifier = Modifier
              .weight(1.15f)
              .height(50.dp)
              .testTag("hub_companion_pair_button"),
          ) {
            Icon(
              imageVector = Icons.Rounded.SettingsBluetooth,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Companion Pair", maxLines = 1, overflow = TextOverflow.Ellipsis)
          }
        }

        // Row 2: 3D Pop-Up Card + Floating Island Pop-Up + (i) guide
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          OutlinedButton(
            onClick = onOpen3DPreview,
            shape = RoundedCornerShape(50),
            modifier = Modifier
              .weight(1f)
              .height(48.dp)
              .testTag("hub_3d_popup_button"),
          ) {
            Icon(
              imageVector = Icons.Rounded.ViewInAr,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("3D Pop-Up", maxLines = 1)
          }

          FilledTonalButton(
            onClick = onPreviewIsland,
            shape = RoundedCornerShape(50),
            modifier = Modifier
              .weight(1f)
              .height(48.dp)
              .testTag("hub_preview_island_button"),
          ) {
            Icon(
              imageVector = Icons.Rounded.AutoAwesome,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Island", maxLines = 1)
          }

          ExpressiveInfoButton(
            contentDescription = "How Floating Island notifications work",
            testTag = "hub_island_info_btn",
            onClick = onOpenIslandGuide,
          )
        }
      }
    }
  }
}

@Composable
private fun ActiveAudioSinksRow(sinks: List<AudioOutputSink>) {
  val extended = LocalBluePulseColors.current
  Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
    Text(
      text = "Detected Audio Output Sinks (${sinks.size})",
      style = MaterialTheme.typography.titleSmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      items(sinks, key = { it.id }) { sink ->
        val accent = when {
          sink.isBluetooth -> MaterialTheme.colorScheme.primary
          sink.isWiredOrUsb -> extended.signalGood
          else -> extended.coralHero
        }
        Surface(
          shape = MaterialTheme.shapes.small,
          color = accent.copy(alpha = 0.14f),
          border = extended.cardBorder,
        ) {
          Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(
              text = sink.name,
              style = MaterialTheme.typography.labelLarge,
              color = MaterialTheme.colorScheme.onSurface,
              maxLines = 1,
            )
            Text(
              text = sink.typeLabel,
              style = MaterialTheme.typography.labelSmall,
              color = accent,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun DeviceCard(
  device: BluetoothDeviceUiModel,
  onPrimaryAction: () -> Unit,
  onOpen3D: () -> Unit,
  onRadarClick: () -> Unit,
  onEqClick: () -> Unit,
) {
  val extended = LocalBluePulseColors.current
  val categoryIcon = when (device.category) {
    DeviceCategory.EARBUDS -> Icons.Rounded.BluetoothAudio
    DeviceCategory.HEADPHONES -> Icons.Rounded.Headphones
    DeviceCategory.SPEAKER -> Icons.Rounded.Speaker
    DeviceCategory.WATCH -> Icons.Rounded.Watch
    DeviceCategory.OTHER -> Icons.Rounded.Bluetooth
  }

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
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Box(
          modifier = Modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(
              if (device.isConnected) {
                extended.signalGood.copy(alpha = 0.16f)
              } else {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
              }
            ),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = categoryIcon,
            contentDescription = device.category.label,
            tint = if (device.isConnected) extended.signalGood else MaterialTheme.colorScheme.primary,
          )
        }

        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = device.name,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text = "${device.address} • ${device.activeProfile}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }

        Surface(
          shape = RoundedCornerShape(50),
          color = when {
            device.isConnected -> extended.signalGood.copy(alpha = 0.18f)
            device.bondStatus == BondStatus.BONDED -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
            else -> MaterialTheme.colorScheme.surfaceContainerHigh
          },
        ) {
          Text(
            text = if (device.isConnected) "ACTIVE" else device.bondStatus.label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = if (device.isConnected) extended.signalGood else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
          )
        }
      }

      if (device.rssiDbm != null || device.batteryPercent != null || device.isConnected) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          device.rssiDbm?.let { rssi ->
            TelemetryPill(
              text = "$rssi dBm",
              color = if (rssi >= -65) extended.signalGood else extended.signalWarn,
            )
          }
          device.batteryPercent?.let { bat ->
            TelemetryPill(
              text = "$bat% BAT",
              color = if (bat > 20) extended.signalGood else extended.signalBad,
            )
          }
          if (device.isConnected) {
            TelemetryPill(
              text = "${device.codecInfo.codecName} • ${device.codecInfo.sampleRateKhz}",
              color = MaterialTheme.colorScheme.primary,
            )
          }
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        FilledTonalButton(
          onClick = onPrimaryAction,
          shape = RoundedCornerShape(50),
          modifier = Modifier
            .weight(1f)
            .height(48.dp)
            .testTag("device_connect_${device.address}"),
        ) {
          Text(
            when {
              device.isConnected -> "Sync Link"
              device.bondStatus == BondStatus.BONDED -> "Connect"
              else -> "Pair Device"
            }
          )
        }

        FilledTonalIconButton(
          onClick = onOpen3D,
          modifier = Modifier
            .minimumInteractiveComponentSize()
            .testTag("device_3d_${device.address}"),
        ) {
          Icon(
            imageVector = Icons.Rounded.ViewInAr,
            contentDescription = "Open 3D animated card for ${device.name}",
            tint = extended.coralHero,
          )
        }

        IconButton(
          onClick = onRadarClick,
          modifier = Modifier
            .minimumInteractiveComponentSize()
            .testTag("device_radar_${device.address}"),
        ) {
          Icon(
            imageVector = Icons.Rounded.Radar,
            contentDescription = "Track device signal on radar",
            tint = MaterialTheme.colorScheme.primary,
          )
        }

        IconButton(
          onClick = onEqClick,
          modifier = Modifier
            .minimumInteractiveComponentSize()
            .testTag("device_eq_${device.address}"),
        ) {
          Icon(
            imageVector = Icons.Rounded.GraphicEq,
            contentDescription = "Open EQ profile for device",
            tint = MaterialTheme.colorScheme.secondary,
          )
        }
      }
    }
  }
}

@Composable
private fun TelemetryPill(text: String, color: Color) {
  Surface(
    shape = RoundedCornerShape(10.dp),
    color = color.copy(alpha = 0.14f),
  ) {
    Text(
      text = text,
      style = MaterialTheme.typography.labelSmall,
      color = color,
      modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
    )
  }
}

@Composable
private fun EmptyScannerCard(
  isScanning: Boolean,
  onToggleScan: () -> Unit,
  onLaunchCompanionPair: () -> Unit,
  onOpenSystemBt: () -> Unit,
) {
  val extended = LocalBluePulseColors.current
  Surface(
    shape = MaterialTheme.shapes.large,
    color = MaterialTheme.colorScheme.surfaceContainer,
    border = extended.cardBorder,
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Icon(
        imageVector = Icons.Rounded.SettingsBluetooth,
        contentDescription = "No paired Bluetooth devices",
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(44.dp),
      )
      Text(
        text = if (isScanning) "Listening for Bluetooth Beacons..." else "No Paired or Nearby Devices Yet",
        style = MaterialTheme.typography.titleMedium,
      )
      Text(
        text = "Put your earbuds or speaker into pairing mode, then tap Start Scan, Companion Pair, or open System Bluetooth.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
          onClick = onToggleScan,
          modifier = Modifier
            .minimumInteractiveComponentSize()
            .testTag("empty_scan_button"),
        ) {
          Text(if (isScanning) "Scanning..." else "Start Scan")
        }
        FilledTonalButton(
          onClick = onLaunchCompanionPair,
          modifier = Modifier
            .minimumInteractiveComponentSize()
            .testTag("empty_companion_button"),
        ) {
          Text("Companion")
        }
        OutlinedButton(
          onClick = onOpenSystemBt,
          modifier = Modifier
            .minimumInteractiveComponentSize()
            .testTag("empty_bt_settings_button"),
        ) {
          Text("System BT")
        }
      }
    }
  }
}

@Composable
private fun SectionHeader(title: String, subtitle: String) {
  Column(modifier = Modifier.padding(top = 4.dp)) {
    Text(
      text = title,
      style = MaterialTheme.typography.titleMedium,
      color = MaterialTheme.colorScheme.onSurface,
    )
    Text(
      text = subtitle,
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}
