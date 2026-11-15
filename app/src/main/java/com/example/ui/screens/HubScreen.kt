package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.BluetoothSearching
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.BluetoothAudio
import androidx.compose.material.icons.rounded.BluetoothDisabled
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
import com.example.ui.components.AnimatedRadarMiniBeacon
import com.example.ui.components.AnimatedSpectrumBars
import com.example.ui.components.BentoColorCard
import com.example.ui.components.DualChannelWaveVisualizer
import com.example.ui.components.ExpressiveInfoButton
import com.example.ui.components.ScallopedExpressiveRing
import com.example.ui.theme.CondensedHeroMetricStyle
import com.example.ui.theme.CondensedMediumMetricStyle
import com.example.ui.theme.LocalBluePulseColors

@Composable
fun HubScreen(
  state: MainUiState,
  onToggleScan: () -> Unit,
  onRefresh: () -> Unit,
  onLaunchCompanionPair: () -> Unit,
  onOpen3DModal: (BluetoothDeviceUiModel?) -> Unit,
  onPairOrConnect: (BluetoothDeviceUiModel) -> Unit,
  onOpenDualSyncTab: () -> Unit,
  onQuickToggleDualPlay: () -> Unit,
  onOpenRadarTab: () -> Unit,
  onOpenEqTab: () -> Unit,
  onOpenRadarForDevice: (String) -> Unit,
  onOpenEqForDevice: (String) -> Unit,
  onOpenPermissions: () -> Unit,
  onOpenSystemBt: () -> Unit,
  onPreviewIsland: () -> Unit,
  onOpenGuide: (FeatureGuideId) -> Unit,
  modifier: Modifier = Modifier,
) {
  val extended = LocalBluePulseColors.current
  val bento = extended.bento

  Box(
    modifier = modifier.fillMaxSize(),
    contentAlignment = Alignment.TopCenter,
  ) {
    LazyColumn(
      modifier = Modifier
        .widthIn(max = 760.dp)
        .fillMaxSize(),
      contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 108.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      // 1. ATTRACT: Editorial Lifestyle Hero with Model Wearing Earbuds + Tilted Organic Frame
      item {
        EditorialLifestyleHeroCard(
          state = state,
          onToggleScan = onToggleScan,
          onOpen3DPreview = {
            onOpen3DModal(state.devices.firstOrNull { it.isConnected } ?: state.devices.firstOrNull())
          },
          onOpenGuide = { onOpenGuide(FeatureGuideId.HUB_PAIRING) },
        )
      }

      // 2. HOOK: Asymmetric Color-Blocked Bento Grid with Live Animations Inside Boxes
      item {
        AsymmetricFeatureBentoGrid(
          state = state,
          onOpenDualSyncTab = onOpenDualSyncTab,
          onQuickToggleDualPlay = onQuickToggleDualPlay,
          onOpenRadarTab = onOpenRadarTab,
          onOpenEqTab = onOpenEqTab,
        )
      }

      // 3. Staggered Quick Utility Capsule Row (Companion Pair, Island Pop-Up, Refresh, Island Info)
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Surface(
            shape = RoundedCornerShape(50),
            color = bento.plumBg,
            contentColor = bento.plumInk,
            modifier = Modifier
              .weight(1.2f)
              .height(50.dp)
              .clip(RoundedCornerShape(50))
              .clickable(onClick = onLaunchCompanionPair)
              .testTag("hub_companion_pair_button"),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 14.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.Center,
            ) {
              Icon(Icons.Rounded.SettingsBluetooth, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Companion Pair", style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
          }

          Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            border = extended.cardBorder,
            modifier = Modifier
              .weight(0.95f)
              .height(50.dp)
              .clip(RoundedCornerShape(20.dp))
              .clickable(onClick = onPreviewIsland)
              .testTag("hub_preview_island_button"),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.Center,
            ) {
              Icon(
                imageVector = Icons.Rounded.AutoAwesome,
                contentDescription = null,
                tint = extended.coralHero,
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text("Island", style = MaterialTheme.typography.labelLarge, maxLines = 1)
            }
          }

          FilledTonalIconButton(
            onClick = onRefresh,
            modifier = Modifier
              .size(50.dp)
              .testTag("hub_refresh_button"),
          ) {
            Icon(Icons.Rounded.Refresh, contentDescription = "Refresh Bluetooth devices")
          }

          ExpressiveInfoButton(
            contentDescription = "How Floating Island notifications work",
            testTag = "hub_island_info_btn",
            onClick = { onOpenGuide(FeatureGuideId.FLOATING_ISLAND) },
          )
        }
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
            color = bento.lavenderBg.copy(alpha = 0.22f),
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
                    "Enable Nearby Devices Access"
                  } else {
                    "Unlock System Floating Island"
                  },
                  style = MaterialTheme.typography.titleSmall,
                )
                Text(
                  text = if (!state.permissions.bluetoothGranted) {
                    "Required for live Bluetooth scanning, battery telemetry, and RSSI radar."
                  } else {
                    "Allow Display Over Other Apps for system-wide Dynamic Island pop-ups."
                  },
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              Spacer(modifier = Modifier.width(10.dp))
              Button(
                onClick = onOpenPermissions,
                shape = RoundedCornerShape(50),
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
            title = "Active Links (${connected.size})",
            subtitle = "Tap 3D for rotating case & live L/R battery rings",
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
          title = if (others.isNotEmpty()) "Paired & Nearby (${others.size})" else "Device Discovery",
          subtitle = if (state.isScanning) {
            "Scanning Classic & BLE beacons..."
          } else {
            "Scan or use Companion Pair to add wireless headsets"
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
private fun EditorialLifestyleHeroCard(
  state: MainUiState,
  onToggleScan: () -> Unit,
  onOpen3DPreview: () -> Unit,
  onOpenGuide: () -> Unit,
) {
  val extended = LocalBluePulseColors.current
  val bento = extended.bento
  val connectedCount = state.devices.count { it.isConnected }

  BentoColorCard(
    containerColor = bento.lavenderBg,
    contentColor = bento.lavenderInk,
    shape = MaterialTheme.shapes.extraLarge,
  ) {
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
        Surface(
          shape = RoundedCornerShape(50),
          color = bento.lavenderInk.copy(alpha = 0.12f),
        ) {
          Text(
            text = if (connectedCount > 0) {
              "$connectedCount LIVE • ${state.availableAudioSinks.size} SINKS"
            } else if (state.isBluetoothEnabled) {
              "RADIO READY • ${state.availableAudioSinks.size} SINKS"
            } else {
              "STUDIO READY • DUAL ENGINE"
            },
            style = MaterialTheme.typography.labelSmall,
            color = bento.lavenderInk,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
          )
        }

        ExpressiveInfoButton(
          contentDescription = "Learn how Bluetooth Hub and Companion Pairing work",
          testTag = "hub_info_btn",
          tint = bento.lavenderInk,
          onClick = onOpenGuide,
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        // Editorial Headline + Asymmetric Action Pills
        Column(
          modifier = Modifier.weight(1.15f),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Text(
            text = "Pure Dual\nSound.",
            style = MaterialTheme.typography.displayMedium,
            color = bento.lavenderInk,
          )
          Text(
            text = "Pair earbuds in 3D, sync dual outputs, and sculpt live audio.",
            style = MaterialTheme.typography.bodySmall,
            color = bento.lavenderInk.copy(alpha = 0.78f),
          )

          Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Button(
              onClick = onToggleScan,
              colors = ButtonDefaults.buttonColors(
                containerColor = bento.coralBg,
                contentColor = bento.coralInk,
              ),
              shape = RoundedCornerShape(50),
              contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
              modifier = Modifier
                .height(48.dp)
                .testTag("hub_scan_toggle_button"),
            ) {
              if (state.isScanning) {
                CircularProgressIndicator(
                  modifier = Modifier.size(16.dp),
                  strokeWidth = 2.dp,
                  color = bento.coralInk,
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Stop", style = MaterialTheme.typography.labelLarge)
              } else {
                Icon(
                  imageVector = Icons.AutoMirrored.Rounded.BluetoothSearching,
                  contentDescription = null,
                  modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Scan", style = MaterialTheme.typography.labelLarge)
              }
            }

            Surface(
              shape = RoundedCornerShape(18.dp),
              color = bento.plumBg,
              contentColor = bento.plumInk,
              modifier = Modifier
                .height(48.dp)
                .clip(RoundedCornerShape(18.dp))
                .clickable(onClick = onOpen3DPreview)
                .testTag("hub_3d_popup_button"),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Icon(Icons.Rounded.ViewInAr, contentDescription = "Open 3D Pop-Up", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(5.dp))
                Text("3D", style = MaterialTheme.typography.labelLarge)
              }
            }
          }
        }

        // Tilted Organic Squircle Portrait of Model Wearing Earbuds + Scalloped Badge
        Box(
          modifier = Modifier
            .weight(0.85f)
            .height(178.dp),
          contentAlignment = Alignment.Center,
        ) {
          Image(
            painter = painterResource(id = R.drawable.img_model_earbuds_hero),
            contentDescription = "Model wearing wireless lavender earbuds",
            contentScale = ContentScale.Crop,
            modifier = Modifier
              .fillMaxSize()
              .graphicsLayer {
                rotationZ = 3.2f
              }
              .clip(RoundedCornerShape(topStart = 44.dp, topEnd = 22.dp, bottomStart = 22.dp, bottomEnd = 44.dp)),
          )

          Box(
            modifier = Modifier
              .align(Alignment.BottomStart)
              .padding(6.dp),
          ) {
            ScallopedExpressiveRing(
              isAnimating = state.isScanning || connectedCount > 0,
              ringColor = bento.limeBg,
              trackColor = bento.plumBg.copy(alpha = 0.85f),
              size = 46.dp,
              petals = 10,
            ) {
              Box(
                modifier = Modifier
                  .size(30.dp)
                  .clip(CircleShape)
                  .background(bento.plumBg),
                contentAlignment = Alignment.Center,
              ) {
                Icon(
                  imageVector = Icons.Rounded.BluetoothAudio,
                  contentDescription = null,
                  tint = bento.limeBg,
                  modifier = Modifier.size(16.dp),
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun AsymmetricFeatureBentoGrid(
  state: MainUiState,
  onOpenDualSyncTab: () -> Unit,
  onQuickToggleDualPlay: () -> Unit,
  onOpenRadarTab: () -> Unit,
  onOpenEqTab: () -> Unit,
) {
  val extended = LocalBluePulseColors.current
  val bento = extended.bento
  val primaryDevice = state.devices.firstOrNull { it.isConnected } ?: state.devices.firstOrNull()
  val rssiText = primaryDevice?.rssiDbm?.toString() ?: "-48"

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .height(288.dp),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    // Left Tall Tile: Color-Blocked Coral/Plum Dual-Audio Bridge Card with Lifestyle Art & Live Dual Waves
    BentoColorCard(
      containerColor = bento.coralBg,
      contentColor = bento.coralInk,
      shape = RoundedCornerShape(30.dp),
      onClick = onOpenDualSyncTab,
      modifier = Modifier
        .weight(1.08f)
        .fillMaxHeight()
        .testTag("hub_bento_dual_card"),
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween,
      ) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(82.dp)
            .clip(RoundedCornerShape(20.dp)),
        ) {
          Image(
            painter = painterResource(id = R.drawable.img_dual_audio_sharing),
            contentDescription = "Two listeners sharing dual audio wireless stream",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
          )
          Surface(
            shape = RoundedCornerShape(50),
            color = bento.plumBg.copy(alpha = 0.88f),
            modifier = Modifier
              .align(Alignment.TopStart)
              .padding(6.dp),
          ) {
            Text(
              text = state.dualSync.routeMode.badge,
              style = MaterialTheme.typography.labelSmall,
              color = bento.limeBg,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            )
          }
        }

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
          Text(
            text = "Dual Audio\nBridge",
            style = MaterialTheme.typography.headlineSmall,
            color = bento.coralInk,
          )
          Text(
            text = state.mediaPlayback.trackTitle,
            style = MaterialTheme.typography.labelSmall,
            color = bento.coralInk.copy(alpha = 0.78f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }

        DualChannelWaveVisualizer(
          isPlaying = state.dualSync.isCalibrationPlaying,
          waveColorA = bento.plumBg,
          waveColorB = Color.White,
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Surface(
            shape = RoundedCornerShape(50),
            color = bento.plumBg,
            contentColor = bento.plumInk,
            modifier = Modifier
              .height(48.dp)
              .clip(RoundedCornerShape(50))
              .clickable(onClick = onQuickToggleDualPlay)
              .testTag("hub_bento_quick_play"),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 14.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(
                imageVector = if (state.dualSync.isCalibrationPlaying) {
                  Icons.Rounded.Pause
                } else {
                  Icons.Rounded.PlayArrow
                },
                contentDescription = "Play or pause Dual Audio",
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = if (state.dualSync.isCalibrationPlaying) "Live" else "Play",
                style = MaterialTheme.typography.labelLarge,
              )
            }
          }

          Icon(
            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
            contentDescription = "Open Dual Sync Studio",
            tint = bento.coralInk,
          )
        }
      }
    }

    // Right Stacked Column: Acid Lime Radar Tile + Electric Cyan EQ Spectrum Tile
    Column(
      modifier = Modifier
        .weight(0.92f)
        .fillMaxHeight(),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      // Top Right: Acid Lime Radar Card with Giant Condensed dBm & Scalloped Beacon
      BentoColorCard(
        containerColor = bento.limeBg,
        contentColor = bento.limeInk,
        shape = RoundedCornerShape(28.dp),
        onClick = onOpenRadarTab,
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth()
          .testTag("hub_bento_radar_card"),
      ) {
        Column(
          modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
          verticalArrangement = Arrangement.SpaceBetween,
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text("RADAR", style = MaterialTheme.typography.labelSmall, color = bento.limeInk)
            AnimatedRadarMiniBeacon(
              isScanning = state.isScanning,
              hasTarget = primaryDevice != null,
              inkColor = bento.limeInk,
              pulseColor = bento.plumBg,
            )
          }

          Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Text(
              text = rssiText,
              style = CondensedHeroMetricStyle,
              color = bento.limeInk,
            )
            Text(
              text = "dBm",
              style = MaterialTheme.typography.labelMedium,
              color = bento.limeInk.copy(alpha = 0.78f),
              modifier = Modifier.padding(bottom = 8.dp),
            )
          }
        }
      }

      // Bottom Right: Electric Cyan EQ Card with Dancing 5-Band Spectrum Inside
      BentoColorCard(
        containerColor = bento.cyanBg,
        contentColor = bento.cyanInk,
        shape = RoundedCornerShape(28.dp),
        onClick = onOpenEqTab,
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth()
          .testTag("hub_bento_eq_card"),
      ) {
        Column(
          modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
          verticalArrangement = Arrangement.SpaceBetween,
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text("EQ LAB", style = MaterialTheme.typography.labelSmall, color = bento.cyanInk)
            Text(
              text = state.activeEqPreset,
              style = MaterialTheme.typography.labelSmall,
              color = bento.cyanInk.copy(alpha = 0.8f),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }

          AnimatedSpectrumBars(
            bands = state.eqBands,
            isPlaying = state.dualSync.isCalibrationPlaying,
            barColor = bento.cyanInk,
            modifier = Modifier.fillMaxWidth(),
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
      text = "Active Hardware Audio Sinks (${sinks.size})",
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
            .size(48.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
              if (device.isConnected) {
                extended.bento.limeBg
              } else {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
              }
            ),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = categoryIcon,
            contentDescription = device.category.label,
            tint = if (device.isConnected) extended.bento.limeInk else MaterialTheme.colorScheme.primary,
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

        device.batteryPercent?.let { bat ->
          Text(
            text = "$bat%",
            style = CondensedMediumMetricStyle,
            color = if (bat > 20) extended.signalGood else extended.signalBad,
          )
        } ?: Surface(
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

      if (device.rssiDbm != null || device.isConnected) {
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
        .padding(22.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Icon(
        imageVector = Icons.Rounded.SettingsBluetooth,
        contentDescription = "No paired Bluetooth devices",
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(40.dp),
      )
      Text(
        text = if (isScanning) "Listening for Bluetooth Beacons..." else "Ready for Your First Pair",
        style = MaterialTheme.typography.headlineSmall,
      )
      Text(
        text = "Put your earbuds or speaker into pairing mode, then start Scan, Companion Pair, or open System Bluetooth.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
          onClick = onToggleScan,
          shape = RoundedCornerShape(50),
          modifier = Modifier
            .minimumInteractiveComponentSize()
            .testTag("empty_scan_button"),
        ) {
          Text(if (isScanning) "Scanning..." else "Scan")
        }
        FilledTonalButton(
          onClick = onLaunchCompanionPair,
          shape = RoundedCornerShape(50),
          modifier = Modifier
            .minimumInteractiveComponentSize()
            .testTag("empty_companion_button"),
        ) {
          Text("Companion")
        }
        OutlinedButton(
          onClick = onOpenSystemBt,
          shape = RoundedCornerShape(50),
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
      style = MaterialTheme.typography.headlineSmall,
      color = MaterialTheme.colorScheme.onSurface,
    )
    Text(
      text = subtitle,
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}
