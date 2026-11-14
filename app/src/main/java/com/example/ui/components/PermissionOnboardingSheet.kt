package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.BluetoothSearching
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.PermissionStatus
import com.example.ui.theme.LocalBluePulseColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionOnboardingSheet(
  permissions: PermissionStatus,
  onRequestBluetooth: () -> Unit,
  onRequestNotifications: () -> Unit,
  onRequestOverlay: () -> Unit,
  onDismiss: () -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surfaceContainer,
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp)
        .padding(bottom = 28.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Text(
        text = stringResource(R.string.perm_title),
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Text(
        text = stringResource(R.string.perm_subtitle),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      PermissionItemCard(
        icon = Icons.AutoMirrored.Rounded.BluetoothSearching,
        title = stringResource(R.string.perm_bt_title),
        description = stringResource(R.string.perm_bt_desc),
        granted = permissions.bluetoothGranted,
        actionLabel = stringResource(R.string.perm_grant_bt),
        testTag = "grant_bluetooth_button",
        onClick = onRequestBluetooth,
      )

      PermissionItemCard(
        icon = Icons.Rounded.NotificationsActive,
        title = stringResource(R.string.perm_notif_title),
        description = stringResource(R.string.perm_notif_desc),
        granted = permissions.notificationsGranted,
        actionLabel = stringResource(R.string.perm_grant_notif),
        testTag = "grant_notifications_button",
        onClick = onRequestNotifications,
      )

      PermissionItemCard(
        icon = Icons.Rounded.Layers,
        title = stringResource(R.string.perm_overlay_title),
        description = stringResource(R.string.perm_overlay_desc),
        granted = permissions.overlayGranted,
        actionLabel = stringResource(R.string.perm_grant_overlay),
        testTag = "grant_overlay_button",
        onClick = onRequestOverlay,
      )

      Spacer(modifier = Modifier.height(4.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
      ) {
        TextButton(
          onClick = onDismiss,
          modifier = Modifier.testTag("permissions_continue_button"),
        ) {
          Text(if (permissions.bluetoothGranted) "Done" else "Continue Without BT")
        }
      }
    }
  }
}

@Composable
private fun PermissionItemCard(
  icon: ImageVector,
  title: String,
  description: String,
  granted: Boolean,
  actionLabel: String,
  testTag: String,
  onClick: () -> Unit,
) {
  val extended = LocalBluePulseColors.current
  Surface(
    shape = MaterialTheme.shapes.medium,
    color = MaterialTheme.colorScheme.surfaceContainerHigh,
    border = extended.cardBorder,
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Icon(
        imageVector = icon,
        contentDescription = title,
        tint = if (granted) extended.signalGood else MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(28.dp),
      )

      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Text(
          text = title,
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
          text = description,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      if (granted) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          Icon(
            imageVector = Icons.Rounded.CheckCircle,
            contentDescription = stringResource(R.string.perm_granted),
            tint = extended.signalGood,
            modifier = Modifier.size(18.dp),
          )
          Text(
            text = stringResource(R.string.perm_granted),
            style = MaterialTheme.typography.labelMedium,
            color = extended.signalGood,
          )
        }
      } else {
        FilledTonalButton(
          onClick = onClick,
          modifier = Modifier.testTag(testTag),
        ) {
          Text(actionLabel)
        }
      }
    }
  }
}
