package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.AppPreferences
import com.example.model.PermissionStatus
import com.example.ui.theme.LocalBluePulseColors
import com.example.ui.theme.ThemeMode

@Composable
fun ThemeAndSettingsDialog(
  preferences: AppPreferences,
  permissions: PermissionStatus,
  onSelectTheme: (ThemeMode) -> Unit,
  onToggleSystemOverlay: (Boolean) -> Unit,
  onToggleRadarHaptics: (Boolean) -> Unit,
  onOpenOverlaySettings: () -> Unit,
  onPreviewIsland: () -> Unit,
  onOpenPermissions: () -> Unit,
  onDismiss: () -> Unit,
) {
  val extended = LocalBluePulseColors.current

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = MaterialTheme.colorScheme.surfaceContainer,
    title = {
      Text(
        text = "Appearance & Island Settings",
        style = MaterialTheme.typography.titleLarge,
      )
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
          text = "Material 3 Theme",
          style = MaterialTheme.typography.titleSmall,
          color = MaterialTheme.colorScheme.primary,
        )

        ThemeMode.entries.forEach { mode ->
          val selected = preferences.themeMode == mode
          val icon = when (mode) {
            ThemeMode.AMOLED -> Icons.Rounded.NightsStay
            ThemeMode.DARK -> Icons.Rounded.DarkMode
            ThemeMode.LIGHT -> Icons.Rounded.LightMode
          }
          Surface(
            modifier = Modifier
              .fillMaxWidth()
              .testTag("theme_option_${mode.name.lowercase()}")
              .clickable { onSelectTheme(mode) },
            shape = MaterialTheme.shapes.small,
            color = if (selected) {
              MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            } else {
              MaterialTheme.colorScheme.surfaceContainerHigh
            },
            border = if (selected) {
              BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
            } else {
              extended.cardBorder
            },
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                Icon(
                  imageVector = icon,
                  contentDescription = mode.label,
                  tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Column {
                  Text(
                    text = mode.label,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                  )
                  Text(
                    text = when (mode) {
                      ThemeMode.AMOLED -> "Pure #000000 black with plum-lavender accents"
                      ThemeMode.DARK -> "Deep Plum & Soft Lavender M3 Expressive"
                      ThemeMode.LIGHT -> "Lavender Daylight Material 3 Expressive"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
              if (selected) {
                Icon(
                  imageVector = Icons.Rounded.Check,
                  contentDescription = "Selected",
                  tint = MaterialTheme.colorScheme.primary,
                )
              }
            }
          }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "System-Wide Floating Island",
              style = MaterialTheme.typography.titleSmall,
            )
            Text(
              text = if (permissions.overlayGranted) {
                "Overlay permission granted"
              } else {
                "Tap below to enable Display Over Other Apps"
              },
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          Switch(
            checked = preferences.systemOverlayEnabled && permissions.overlayGranted,
            onCheckedChange = { enabled ->
              if (enabled && !permissions.overlayGranted) {
                onOpenOverlaySettings()
              } else {
                onToggleSystemOverlay(enabled)
              }
            },
            modifier = Modifier.testTag("switch_system_overlay"),
          )
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Radar Proximity Haptics",
              style = MaterialTheme.typography.titleSmall,
            )
            Text(
              text = "Vibrate subtly when near a tracked device",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          Switch(
            checked = preferences.radarHapticsEnabled,
            onCheckedChange = onToggleRadarHaptics,
            modifier = Modifier.testTag("switch_radar_haptics"),
          )
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Button(
            onClick = {
              onPreviewIsland()
              onDismiss()
            },
            modifier = Modifier
              .weight(1f)
              .testTag("preview_island_button"),
          ) {
            Icon(
              imageVector = Icons.Rounded.AutoAwesome,
              contentDescription = null,
              modifier = Modifier.padding(end = 6.dp),
            )
            Text("Test Island")
          }
          TextButton(
            onClick = {
              onDismiss()
              onOpenPermissions()
            },
            modifier = Modifier.testTag("manage_permissions_button"),
          ) {
            Text("Permissions")
          }
        }
      }
    },
    confirmButton = {
      TextButton(
        onClick = onDismiss,
        modifier = Modifier.testTag("settings_close_button"),
      ) {
        Text("Close")
      }
    },
  )
}
