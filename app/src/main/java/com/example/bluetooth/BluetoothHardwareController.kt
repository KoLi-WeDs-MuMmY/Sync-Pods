package com.example.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothStatusCodes
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.companion.AssociationRequest
import android.companion.BluetoothDeviceFilter
import android.companion.CompanionDeviceManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.IntentSender
import android.content.pm.PackageManager
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.ParcelUuid
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.model.AudioOutputSink
import com.example.model.BluetoothDeviceUiModel
import com.example.model.BondStatus
import com.example.model.CodecInfo
import com.example.model.DeviceCategory
import com.example.model.FloatingIslandEvent
import com.example.model.RssiSample
import com.example.overlay.FloatingIslandEventBus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class BluetoothHardwareController(private val context: Context) {

  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

  private val bluetoothManager: BluetoothManager? =
    context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
  private val adapter: BluetoothAdapter? = bluetoothManager?.adapter
  private val audioManager: AudioManager? =
    context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

  private val _isBluetoothSupported = MutableStateFlow(adapter != null)
  val isBluetoothSupported: StateFlow<Boolean> = _isBluetoothSupported.asStateFlow()

  private val _isBleSupported = MutableStateFlow(
    context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)
  )
  val isBleSupported: StateFlow<Boolean> = _isBleSupported.asStateFlow()

  private val _isBluetoothEnabled = MutableStateFlow(safeIsEnabled())
  val isBluetoothEnabled: StateFlow<Boolean> = _isBluetoothEnabled.asStateFlow()

  private val _isScanning = MutableStateFlow(false)
  val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

  private val _leAudioSupported = MutableStateFlow(checkLeAudioSupported())
  val leAudioSupported: StateFlow<Boolean> = _leAudioSupported.asStateFlow()

  private val _leAudioBroadcastSupported = MutableStateFlow(checkLeAudioBroadcastSupported())
  val leAudioBroadcastSupported: StateFlow<Boolean> = _leAudioBroadcastSupported.asStateFlow()

  private val _devices = MutableStateFlow<List<BluetoothDeviceUiModel>>(emptyList())
  val devices: StateFlow<List<BluetoothDeviceUiModel>> = _devices.asStateFlow()

  private val _availableAudioSinks = MutableStateFlow<List<AudioOutputSink>>(emptyList())
  val availableAudioSinks: StateFlow<List<AudioOutputSink>> = _availableAudioSinks.asStateFlow()

  private val _rssiHistory = MutableStateFlow<Map<String, List<RssiSample>>>(emptyMap())
  val rssiHistory: StateFlow<Map<String, List<RssiSample>>> = _rssiHistory.asStateFlow()

  private val deviceMap = ConcurrentHashMap<String, BluetoothDeviceUiModel>()
  private val rawDeviceMap = ConcurrentHashMap<String, BluetoothDevice>()
  private var a2dpProxy: BluetoothA2dp? = null
  private var activeGatt: BluetoothGatt? = null
  private var rssiPollJob: Job? = null
  private var scanTimeoutJob: Job? = null
  private var receiversRegistered = false

  private val batteryServiceUuid = UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb")
  private val batteryLevelCharUuid = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb")

  private val audioDeviceCallback = object : AudioDeviceCallback() {
    override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
      refreshAudioOutputSinks()
      refreshBondedAndConnected()
    }

    override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
      refreshAudioOutputSinks()
      refreshBondedAndConnected()
    }
  }

  private val leScanCallback = object : ScanCallback() {
    override fun onScanResult(callbackType: Int, result: ScanResult) {
      handleDiscoveredDevice(result.device, result.rssi, isBle = true)
    }

    override fun onBatchScanResults(results: MutableList<ScanResult>) {
      results.forEach { handleDiscoveredDevice(it.device, it.rssi, isBle = true) }
    }

    override fun onScanFailed(errorCode: Int) {
      Log.w(TAG, "BLE scan failed with errorCode=$errorCode")
    }
  }

  private val btReceiver = object : BroadcastReceiver() {
    override fun onReceive(ctx: Context?, intent: Intent?) {
      when (intent?.action) {
        BluetoothAdapter.ACTION_STATE_CHANGED -> {
          val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
          _isBluetoothEnabled.value = (state == BluetoothAdapter.STATE_ON)
          if (state == BluetoothAdapter.STATE_ON) {
            refreshBondedAndConnected()
          }
        }
        BluetoothDevice.ACTION_FOUND -> {
          val device = getParcelableDevice(intent)
          val rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE).toInt()
          if (device != null) {
            handleDiscoveredDevice(
              device = device,
              rssi = if (rssi != Short.MIN_VALUE.toInt()) rssi else null,
              isBle = false,
            )
          }
        }
        BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
          val device = getParcelableDevice(intent) ?: return
          val bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_NONE)
          refreshBondedAndConnected()
          if (bondState == BluetoothDevice.BOND_BONDED) {
            val name = safeDeviceName(device)
            FloatingIslandEventBus.postEvent(
              FloatingIslandEvent.DeviceConnected(
                deviceName = name,
                batteryPercent = readBatteryViaReflection(device),
                codecLabel = "Paired",
              )
            )
          }
        }
        BluetoothDevice.ACTION_ACL_CONNECTED -> {
          val device = getParcelableDevice(intent) ?: return
          refreshAudioOutputSinks()
          refreshBondedAndConnected()
          val name = safeDeviceName(device)
          val battery = readBatteryViaReflection(device)
          val codec = queryCodecInfo(device).codecName
          FloatingIslandEventBus.postEvent(
            FloatingIslandEvent.DeviceConnected(
              deviceName = name,
              batteryPercent = battery,
              codecLabel = codec,
            )
          )
        }
        BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
          val device = getParcelableDevice(intent) ?: return
          refreshAudioOutputSinks()
          refreshBondedAndConnected()
          FloatingIslandEventBus.postEvent(
            FloatingIslandEvent.DeviceDisconnected(deviceName = safeDeviceName(device))
          )
        }
        BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED -> {
          refreshAudioOutputSinks()
          refreshBondedAndConnected()
        }
        ACTION_BATTERY_LEVEL_CHANGED -> {
          val device = getParcelableDevice(intent) ?: return
          val level = intent.getIntExtra(EXTRA_BATTERY_LEVEL, -1)
          if (level in 0..100) {
            updateDeviceBattery(device.address, level)
            FloatingIslandEventBus.postEvent(
              FloatingIslandEvent.BatteryStatus(
                deviceName = safeDeviceName(device),
                batteryPercent = level,
                leftPercent = null,
                rightPercent = null,
              )
            )
          }
        }
      }
    }
  }

  fun start() {
    if (!receiversRegistered) {
      val filter = IntentFilter().apply {
        addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        addAction(BluetoothDevice.ACTION_FOUND)
        addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
        addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)
        addAction(ACTION_BATTERY_LEVEL_CHANGED)
      }
      ContextCompat.registerReceiver(
        context,
        btReceiver,
        filter,
        ContextCompat.RECEIVER_EXPORTED,
      )
      audioManager?.registerAudioDeviceCallback(audioDeviceCallback, null)
      receiversRegistered = true
    }

    initA2dpProxy()
    refreshAudioOutputSinks()
    refreshBondedAndConnected()
  }

  fun stop() {
    stopScan()
    stopRssiRadarMonitor()
    if (receiversRegistered) {
      runCatching { context.unregisterReceiver(btReceiver) }
      runCatching { audioManager?.unregisterAudioDeviceCallback(audioDeviceCallback) }
      receiversRegistered = false
    }
    a2dpProxy?.let { proxy ->
      runCatching { adapter?.closeProfileProxy(BluetoothProfile.A2DP, proxy) }
    }
    a2dpProxy = null
  }

  fun refreshAudioOutputSinks() {
    val am = audioManager ?: return
    val outputs = am.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
    val mapped = outputs.mapNotNull { info ->
      val typeLabel = when (info.type) {
        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> "Bluetooth A2DP"
        AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> "Bluetooth SCO"
        AudioDeviceInfo.TYPE_WIRED_HEADSET,
        AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> "3.5mm Wired"
        AudioDeviceInfo.TYPE_USB_HEADSET,
        AudioDeviceInfo.TYPE_USB_DEVICE -> "USB-C Audio"
        AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> "Phone Speaker"
        else -> when {
          Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            (info.type == AudioDeviceInfo.TYPE_BLE_HEADSET || info.type == AudioDeviceInfo.TYPE_BLE_SPEAKER) ->
            "BLE Audio"
          Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            info.type == AudioDeviceInfo.TYPE_BLE_BROADCAST ->
            "Auracast"
          else -> null
        }
      } ?: return@mapNotNull null

      val isBt = info.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
        info.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
        (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
          (info.type == AudioDeviceInfo.TYPE_BLE_HEADSET || info.type == AudioDeviceInfo.TYPE_BLE_SPEAKER))

      val isWired = info.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
        info.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
        info.type == AudioDeviceInfo.TYPE_USB_HEADSET ||
        info.type == AudioDeviceInfo.TYPE_USB_DEVICE

      val address = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) info.address else ""
      val rawName = info.productName?.toString()?.takeIf { it.isNotBlank() } ?: typeLabel

      AudioOutputSink(
        id = info.id,
        name = rawName,
        typeLabel = typeLabel,
        address = address,
        isBluetooth = isBt,
        isScoOrComm = info.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
        isWiredOrUsb = isWired,
        isSpeaker = info.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER,
      )
    }
    _availableAudioSinks.value = mapped
  }

  fun findAudioDeviceInfoById(id: Int?): AudioDeviceInfo? {
    if (id == null) return null
    return audioManager?.getDevices(AudioManager.GET_DEVICES_OUTPUTS)?.firstOrNull { it.id == id }
  }

  fun startCompanionPairing(
    onChooserLauncher: (IntentSender) -> Unit,
    onError: (String) -> Unit,
  ) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
      onError("Companion Device Pairing requires Android 8.0+")
      return
    }
    val cdm = context.getSystemService(Context.COMPANION_DEVICE_SERVICE) as? CompanionDeviceManager
    if (cdm == null) {
      onError("Companion Device service unavailable")
      return
    }

    val request = AssociationRequest.Builder()
      .addDeviceFilter(BluetoothDeviceFilter.Builder().build())
      .setSingleDevice(false)
      .build()

    runCatching {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        cdm.associate(
          request,
          context.mainExecutor,
          object : CompanionDeviceManager.Callback() {
            override fun onAssociationPending(intentSender: IntentSender) {
              onChooserLauncher(intentSender)
            }

            override fun onFailure(error: CharSequence?) {
              onError(error?.toString() ?: "Companion pairing canceled")
            }
          },
        )
      } else {
        @Suppress("DEPRECATION")
        cdm.associate(
          request,
          object : CompanionDeviceManager.Callback() {
            override fun onDeviceFound(chooserLauncher: IntentSender) {
              onChooserLauncher(chooserLauncher)
            }

            override fun onFailure(error: CharSequence?) {
              onError(error?.toString() ?: "Companion pairing canceled")
            }
          },
          null,
        )
      }
    }.onFailure { err ->
      onError(err.message ?: "Unable to launch Companion pairing")
    }
  }

  @SuppressLint("MissingPermission")
  fun bondDevice(device: BluetoothDevice?) {
    if (device == null || !PermissionHelper.hasBluetoothPermissions(context)) return
    runCatching {
      if (device.bondState == BluetoothDevice.BOND_NONE) {
        device.createBond()
      }
    }
  }

  private fun initA2dpProxy() {
    val btAdapter = adapter ?: return
    if (!PermissionHelper.hasBluetoothPermissions(context)) return
    runCatching {
      btAdapter.getProfileProxy(
        context,
        object : BluetoothProfile.ServiceListener {
          override fun onServiceConnected(profile: Int, proxy: BluetoothProfile?) {
            if (profile == BluetoothProfile.A2DP && proxy is BluetoothA2dp) {
              a2dpProxy = proxy
              refreshBondedAndConnected()
            }
          }

          override fun onServiceDisconnected(profile: Int) {
            if (profile == BluetoothProfile.A2DP) {
              a2dpProxy = null
            }
          }
        },
        BluetoothProfile.A2DP,
      )
    }
  }

  @SuppressLint("MissingPermission")
  fun refreshBondedAndConnected() {
    _isBluetoothSupported.value = (adapter != null)
    _isBluetoothEnabled.value = safeIsEnabled()
    _leAudioSupported.value = checkLeAudioSupported()
    _leAudioBroadcastSupported.value = checkLeAudioBroadcastSupported()
    refreshAudioOutputSinks()

    val btAdapter = adapter ?: return
    if (!PermissionHelper.hasBluetoothPermissions(context)) {
      _devices.value = emptyList()
      return
    }

    try {
      val connectedAudioAddresses = queryAudioOutputAddresses()
      val a2dpConnectedAddresses = runCatching {
        a2dpProxy?.connectedDevices?.map { it.address }?.toSet() ?: emptySet()
      }.getOrDefault(emptySet())

      val gattConnectedAddresses = runCatching {
        bluetoothManager?.getConnectedDevices(BluetoothProfile.GATT)?.map { it.address }?.toSet()
          ?: emptySet()
      }.getOrDefault(emptySet())

      val bonded = btAdapter.bondedDevices.orEmpty()
      for (device in bonded) {
        rawDeviceMap[device.address] = device
        val isConnected = device.address in connectedAudioAddresses ||
          device.address in a2dpConnectedAddresses ||
          device.address in gattConnectedAddresses ||
          isDeviceConnectedViaReflection(device)

        val existing = deviceMap[device.address]
        val battery = readBatteryViaReflection(device) ?: existing?.batteryPercent
        val codecInfo = if (isConnected) queryCodecInfo(device) else existing?.codecInfo ?: CodecInfo()
        val category = inferCategory(device)

        deviceMap[device.address] = BluetoothDeviceUiModel(
          address = device.address,
          name = safeDeviceName(device),
          category = category,
          bondStatus = BondStatus.BONDED,
          isConnected = isConnected,
          isAudioSink = isAudioDevice(device) || isConnected,
          isLeAudioSupported = _leAudioSupported.value && (device.type == BluetoothDevice.DEVICE_TYPE_LE || device.type == BluetoothDevice.DEVICE_TYPE_DUAL),
          activeProfile = when {
            isConnected && a2dpConnectedAddresses.contains(device.address) -> "A2DP High-Res"
            isConnected && connectedAudioAddresses.contains(device.address) -> "Audio Output"
            isConnected -> "BLE / GATT"
            else -> "Standby"
          },
          rssiDbm = existing?.rssiDbm,
          batteryPercent = battery,
          codecInfo = codecInfo,
          lastSeenTimestamp = System.currentTimeMillis(),
        )
      }
      emitSortedDevices()
    } catch (se: SecurityException) {
      Log.w(TAG, "Missing Bluetooth permission during refresh", se)
    }
  }

  @SuppressLint("MissingPermission")
  fun startScan() {
    val btAdapter = adapter ?: return
    if (!PermissionHelper.hasBluetoothPermissions(context) || !safeIsEnabled()) return

    stopScan()
    _isScanning.value = true

    try {
      val leScanner = btAdapter.bluetoothLeScanner
      if (leScanner != null) {
        val settings = ScanSettings.Builder()
          .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
          .build()
        leScanner.startScan(null, settings, leScanCallback)
      }
      if (!btAdapter.isDiscovering) {
        btAdapter.startDiscovery()
      }
    } catch (se: SecurityException) {
      Log.w(TAG, "Scan failed due to missing permission", se)
      _isScanning.value = false
      return
    }

    scanTimeoutJob = scope.launch {
      delay(15_000L)
      stopScan()
    }
  }

  @SuppressLint("MissingPermission")
  fun stopScan() {
    scanTimeoutJob?.cancel()
    scanTimeoutJob = null
    _isScanning.value = false
    val btAdapter = adapter ?: return
    if (!PermissionHelper.hasBluetoothPermissions(context)) return
    runCatching {
      btAdapter.bluetoothLeScanner?.stopScan(leScanCallback)
      if (btAdapter.isDiscovering) {
        btAdapter.cancelDiscovery()
      }
    }
  }

  @SuppressLint("MissingPermission")
  fun pairOrConnectDevice(address: String): Boolean {
    val device = rawDeviceMap[address] ?: runCatching {
      adapter?.getRemoteDevice(address)
    }.getOrNull() ?: return false

    if (!PermissionHelper.hasBluetoothPermissions(context)) return false

    return try {
      when (device.bondState) {
        BluetoothDevice.BOND_NONE -> {
          device.createBond()
        }
        BluetoothDevice.BOND_BONDED -> {
          connectGattForTelemetry(address)
          invokeA2dpConnect(device)
        }
        else -> true
      }
    } catch (se: SecurityException) {
      Log.w(TAG, "Pair/connect blocked by missing permission", se)
      false
    }
  }

  @SuppressLint("MissingPermission")
  fun startRssiRadarMonitor(address: String) {
    stopRssiRadarMonitor()
    if (!PermissionHelper.hasBluetoothPermissions(context)) return
    if (rawDeviceMap[address] == null) return

    connectGattForTelemetry(address)

    rssiPollJob = scope.launch {
      while (isActive) {
        val gatt = activeGatt
        if (gatt != null) {
          runCatching { gatt.readRemoteRssi() }
        }
        delay(850L)
      }
    }
  }

  @SuppressLint("MissingPermission")
  fun stopRssiRadarMonitor() {
    rssiPollJob?.cancel()
    rssiPollJob = null
    activeGatt?.let { gatt ->
      runCatching {
        gatt.disconnect()
        gatt.close()
      }
    }
    activeGatt = null
  }

  @SuppressLint("MissingPermission")
  private fun connectGattForTelemetry(address: String) {
    if (!PermissionHelper.hasBluetoothPermissions(context)) return
    val device = rawDeviceMap[address] ?: return

    activeGatt?.let { old ->
      runCatching {
        old.disconnect()
        old.close()
      }
    }

    runCatching {
      activeGatt = device.connectGatt(
        context,
        false,
        object : BluetoothGattCallback() {
          override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
              runCatching {
                gatt.discoverServices()
                gatt.readRemoteRssi()
              }
            }
          }

          override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
              val batteryChar = gatt.getService(batteryServiceUuid)
                ?.getCharacteristic(batteryLevelCharUuid)
              if (batteryChar != null) {
                runCatching { gatt.readCharacteristic(batteryChar) }
              }
            }
          }

          @Suppress("DEPRECATION")
          override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int,
          ) {
            if (status == BluetoothGatt.GATT_SUCCESS && characteristic.uuid == batteryLevelCharUuid) {
              val level = characteristic.value?.firstOrNull()?.toInt()?.and(0xFF)
              if (level != null && level in 0..100) {
                updateDeviceBattery(gatt.device.address, level)
              }
            }
          }

          override fun onReadRemoteRssi(gatt: BluetoothGatt, rssi: Int, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
              recordRssiSample(gatt.device.address, rssi)
            }
          }
        },
        BluetoothDevice.TRANSPORT_AUTO,
      )
    }
  }

  @SuppressLint("MissingPermission")
  private fun handleDiscoveredDevice(device: BluetoothDevice, rssi: Int?, isBle: Boolean) {
    if (!PermissionHelper.hasBluetoothPermissions(context)) return
    rawDeviceMap[device.address] = device

    val existing = deviceMap[device.address]
    val name = safeDeviceName(device)
    if (name == "Unknown Device" && existing == null && !isBle) return

    val bond = when (device.bondState) {
      BluetoothDevice.BOND_BONDED -> BondStatus.BONDED
      BluetoothDevice.BOND_BONDING -> BondStatus.BONDING
      else -> BondStatus.NONE
    }

    deviceMap[device.address] = BluetoothDeviceUiModel(
      address = device.address,
      name = if (name != "Unknown Device") name else (existing?.name ?: "BLE Device (${device.address.takeLast(5)})"),
      category = inferCategory(device),
      bondStatus = bond,
      isConnected = existing?.isConnected ?: false,
      isAudioSink = isAudioDevice(device),
      isLeAudioSupported = _leAudioSupported.value && isBle,
      activeProfile = existing?.activeProfile ?: if (isBle) "BLE Broadcast / GATT" else "Classic BT",
      rssiDbm = rssi ?: existing?.rssiDbm,
      batteryPercent = existing?.batteryPercent ?: readBatteryViaReflection(device),
      codecInfo = existing?.codecInfo ?: CodecInfo(),
      lastSeenTimestamp = System.currentTimeMillis(),
    )

    if (rssi != null) {
      recordRssiSample(device.address, rssi)
    } else {
      emitSortedDevices()
    }
  }

  private fun recordRssiSample(address: String, rssi: Int) {
    deviceMap[address]?.let { existing ->
      deviceMap[address] = existing.copy(
        rssiDbm = rssi,
        lastSeenTimestamp = System.currentTimeMillis(),
      )
    }

    val currentMap = _rssiHistory.value.toMutableMap()
    val updatedList = (currentMap[address].orEmpty() + RssiSample(System.currentTimeMillis(), rssi))
      .takeLast(36)
    currentMap[address] = updatedList
    _rssiHistory.value = currentMap

    emitSortedDevices()
  }

  private fun updateDeviceBattery(address: String, level: Int) {
    deviceMap[address]?.let { existing ->
      deviceMap[address] = existing.copy(batteryPercent = level)
      emitSortedDevices()
    }
  }

  private fun emitSortedDevices() {
    _devices.value = deviceMap.values.sortedWith(
      compareByDescending<BluetoothDeviceUiModel> { it.isConnected }
        .thenByDescending { it.bondStatus == BondStatus.BONDED }
        .thenByDescending { it.rssiDbm ?: -120 }
        .thenBy { it.name }
    )
  }

  private fun queryAudioOutputAddresses(): Set<String> {
    val am = audioManager ?: return emptySet()
    val outputs = am.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
    val addresses = mutableSetOf<String>()
    for (info in outputs) {
      val isBtType = info.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
        info.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
        (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && (
          info.type == AudioDeviceInfo.TYPE_BLE_HEADSET ||
            info.type == AudioDeviceInfo.TYPE_BLE_SPEAKER
          )) ||
        (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
          info.type == AudioDeviceInfo.TYPE_BLE_BROADCAST)
      if (isBtType && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && info.address.isNotBlank()) {
        addresses.add(info.address)
      }
    }
    return addresses
  }

  private fun queryCodecInfo(device: BluetoothDevice): CodecInfo {
    val proxy = a2dpProxy
    if (proxy != null) {
      val codecFromA2dp = runCatching {
        val method = proxy.javaClass.getMethod("getCodecStatus", BluetoothDevice::class.java)
        val status = method.invoke(proxy, device) ?: return@runCatching null
        val configMethod = status.javaClass.getMethod("getCodecConfig")
        val config = configMethod.invoke(status) ?: return@runCatching null
        val typeInt = config.javaClass.getMethod("getCodecType").invoke(config) as? Int ?: -1
        val sampleRateInt = config.javaClass.getMethod("getSampleRate").invoke(config) as? Int ?: 0
        val bitsInt = config.javaClass.getMethod("getBitsPerSample").invoke(config) as? Int ?: 0
        val channelInt = config.javaClass.getMethod("getChannelMode").invoke(config) as? Int ?: 0

        val codecName = when (typeInt) {
          0 -> "SBC"
          1 -> "AAC"
          2 -> "aptX"
          3 -> "aptX HD"
          4 -> "LDAC"
          5 -> "LC3 (LE Audio)"
          else -> "Bluetooth Audio"
        }
        val sampleRateLabel = when (sampleRateInt) {
          1 -> "44.1 kHz"
          2 -> "48.0 kHz"
          4 -> "88.2 kHz"
          8 -> "96.0 kHz"
          else -> "48.0 kHz"
        }
        val bitsLabel = when (bitsInt) {
          1 -> "16-bit"
          2 -> "24-bit"
          4 -> "32-bit"
          else -> "24-bit"
        }
        val channelLabel = if (channelInt == 1) "Mono" else "Stereo"
        val estLatency = when (typeInt) {
          5 -> 32
          2, 3 -> 95
          4 -> 130
          1 -> 150
          else -> 185
        }
        CodecInfo(
          codecName = codecName,
          sampleRateKhz = sampleRateLabel,
          bitsPerSample = bitsLabel,
          channelMode = channelLabel,
          bitrateLabel = if (typeInt == 4) "Up to 990 kbps" else "Adaptive",
          estimatedLatencyMs = estLatency,
        )
      }.getOrNull()
      if (codecFromA2dp != null) return codecFromA2dp
    }
    return CodecInfo()
  }

  private fun invokeA2dpConnect(device: BluetoothDevice): Boolean {
    val proxy = a2dpProxy ?: return false
    return runCatching {
      val method = proxy.javaClass.getMethod("connect", BluetoothDevice::class.java)
      (method.invoke(proxy, device) as? Boolean) ?: false
    }.getOrDefault(false)
  }

  private fun readBatteryViaReflection(device: BluetoothDevice): Int? {
    return runCatching {
      val method = device.javaClass.getMethod("getBatteryLevel")
      val result = method.invoke(device) as? Int ?: -1
      if (result in 0..100) result else null
    }.getOrNull()
  }

  private fun isDeviceConnectedViaReflection(device: BluetoothDevice): Boolean {
    return runCatching {
      val method = device.javaClass.getMethod("isConnected")
      (method.invoke(device) as? Boolean) ?: false
    }.getOrDefault(false)
  }

  @SuppressLint("MissingPermission")
  private fun safeDeviceName(device: BluetoothDevice): String {
    if (!PermissionHelper.hasBluetoothPermissions(context)) return device.address
    return runCatching {
      val aliasOrName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        device.alias ?: device.name
      } else {
        device.name
      }
      aliasOrName?.takeIf { it.isNotBlank() } ?: "Unknown Device"
    }.getOrDefault("Unknown Device")
  }

  @SuppressLint("MissingPermission")
  private fun inferCategory(device: BluetoothDevice): DeviceCategory {
    val btClass = runCatching { device.bluetoothClass }.getOrNull()
    val name = safeDeviceName(device).lowercase()
    return when {
      name.contains("bud") || name.contains("pod") || name.contains("ear") -> DeviceCategory.EARBUDS
      name.contains("speaker") || name.contains("boom") || name.contains("sound") -> DeviceCategory.SPEAKER
      name.contains("watch") || name.contains("band") -> DeviceCategory.WATCH
      btClass?.deviceClass == BluetoothClass.Device.AUDIO_VIDEO_WEARABLE_HEADSET -> DeviceCategory.EARBUDS
      btClass?.deviceClass == BluetoothClass.Device.AUDIO_VIDEO_HEADPHONES -> DeviceCategory.HEADPHONES
      btClass?.deviceClass == BluetoothClass.Device.AUDIO_VIDEO_LOUDSPEAKER ||
        btClass?.deviceClass == BluetoothClass.Device.AUDIO_VIDEO_PORTABLE_AUDIO -> DeviceCategory.SPEAKER
      btClass?.majorDeviceClass == BluetoothClass.Device.Major.WEARABLE -> DeviceCategory.WATCH
      btClass?.majorDeviceClass == BluetoothClass.Device.Major.AUDIO_VIDEO -> DeviceCategory.HEADPHONES
      else -> DeviceCategory.OTHER
    }
  }

  @SuppressLint("MissingPermission")
  private fun isAudioDevice(device: BluetoothDevice): Boolean {
    val major = runCatching { device.bluetoothClass?.majorDeviceClass }.getOrNull()
    if (major == BluetoothClass.Device.Major.AUDIO_VIDEO) return true
    val uuids: Array<ParcelUuid>? = runCatching { device.uuids }.getOrNull()
    return uuids?.any {
      val s = it.uuid.toString().lowercase()
      s.startsWith("0000110b") || s.startsWith("0000110e") || s.startsWith("0000111e") || s.startsWith("0000184e")
    } == true
  }

  private fun safeIsEnabled(): Boolean {
    return runCatching { adapter?.isEnabled == true }.getOrDefault(false)
  }

  private fun checkLeAudioSupported(): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return false
    val btAdapter = adapter ?: return false
    return runCatching {
      btAdapter.isLeAudioSupported == BluetoothStatusCodes.FEATURE_SUPPORTED
    }.getOrDefault(false)
  }

  private fun checkLeAudioBroadcastSupported(): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return false
    val btAdapter = adapter ?: return false
    return runCatching {
      btAdapter.isLeAudioBroadcastSourceSupported == BluetoothStatusCodes.FEATURE_SUPPORTED
    }.getOrDefault(false)
  }

  @Suppress("DEPRECATION")
  private fun getParcelableDevice(intent: Intent): BluetoothDevice? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
    } else {
      intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
    }
  }

  companion object {
    private const val TAG = "BluePulseBT"
    private const val ACTION_BATTERY_LEVEL_CHANGED = "android.bluetooth.device.action.BATTERY_LEVEL_CHANGED"
    private const val EXTRA_BATTERY_LEVEL = "android.bluetooth.device.extra.BATTERY_LEVEL"
  }
}
