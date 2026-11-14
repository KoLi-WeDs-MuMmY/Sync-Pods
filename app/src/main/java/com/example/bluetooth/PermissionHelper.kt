package com.example.bluetooth

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.example.model.PermissionStatus

object PermissionHelper {
  fun requiredBluetoothPermissions(): Array<String> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      arrayOf(
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.BLUETOOTH_ADVERTISE,
      )
    } else {
      arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
      )
    }
  }

  fun requiredNotificationPermission(): String? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      Manifest.permission.POST_NOTIFICATIONS
    } else {
      null
    }
  }

  fun hasBluetoothPermissions(context: Context): Boolean {
    return requiredBluetoothPermissions().all { perm ->
      ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
    }
  }

  fun hasNotificationPermission(context: Context): Boolean {
    val perm = requiredNotificationPermission() ?: return true
    return ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
  }

  fun canDrawOverlays(context: Context): Boolean {
    return Settings.canDrawOverlays(context)
  }

  fun getPermissionStatus(context: Context): PermissionStatus {
    return PermissionStatus(
      bluetoothGranted = hasBluetoothPermissions(context),
      notificationsGranted = hasNotificationPermission(context),
      overlayGranted = canDrawOverlays(context),
    )
  }

  fun createOverlaySettingsIntent(context: Context): Intent {
    return Intent(
      Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
      Uri.parse("package:${context.packageName}"),
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
  }

  fun createBluetoothSettingsIntent(): Intent {
    return Intent(Settings.ACTION_BLUETOOTH_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
  }
}
