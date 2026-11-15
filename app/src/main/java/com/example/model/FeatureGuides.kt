package com.example.model

enum class FeatureGuideId {
  HUB_PAIRING,
  EARBUDS_3D,
  DUAL_BRIDGE,
  LATENCY_CALIBRATION,
  RADAR_FINDER,
  CODEC_INSPECTOR,
  EQUALIZER_RULES,
  FLOATING_ISLAND,
}

data class FeatureGuideContent(
  val id: FeatureGuideId,
  val title: String,
  val subtitle: String,
  val steps: List<String>,
  val proTip: String,
)

object FeatureGuides {
  fun get(id: FeatureGuideId): FeatureGuideContent = when (id) {
    FeatureGuideId.HUB_PAIRING -> FeatureGuideContent(
      id = FeatureGuideId.HUB_PAIRING,
      title = "Bluetooth Hub & Companion Pairing",
      subtitle = "Scan Classic & BLE devices, pair natively, and inspect active audio links.",
      steps = listOf(
        "Tap Grant Permissions first if Nearby Devices is not enabled yet.",
        "Tap 'Companion Pair' to open Android's native accessory pairing dialog, or tap 'Scan' to list nearby Bluetooth & BLE beacons.",
        "Tap any device's 3D icon to open the rotating earbuds connection modal, or tap Connect / Inspect Link.",
      ),
      proTip = "If an already-paired headset does not auto-connect from the app, tap System BT to toggle its A2DP Media Audio profile.",
    )

    FeatureGuideId.EARBUDS_3D -> FeatureGuideContent(
      id = FeatureGuideId.EARBUDS_3D,
      title = "3D Connection & Battery Card",
      subtitle = "Interactive iOS-style 3D case and earbuds preview for pairing and status checks.",
      steps = listOf(
        "The 3D case opens automatically with levitating Left & Right earbuds when pairing or inspecting a device.",
        "Drag left/right on the 3D earbuds stage to spin them smoothly and inspect live L, R, and Case battery rings.",
        "Tap 'Connect & Route' to link the device and jump straight into Dual Sync.",
      ),
      proTip = "Battery rings populate automatically from Android Bluetooth battery metadata or GATT Battery Service (0x180F).",
    )

    FeatureGuideId.DUAL_BRIDGE -> FeatureGuideContent(
      id = FeatureGuideId.DUAL_BRIDGE,
      title = "Dual Audio Bridge (Works On Every Phone)",
      subtitle = "Play music or sync audio across two outputs even when your phone lacks hardware Dual Bluetooth.",
      steps = listOf(
        "Choose a routing mode at the top: 'BT + USB-C / Wired' plays simultaneously to Bluetooth + wired/speaker; 'TWS Split L/R' lets two listeners share one pair of earbuds with separate L & R volume/delay; 'A2DP + SCO Dual BT' bridges two Bluetooth devices; 'LE Audio' uses native multi-BT on Android 13+.",
        "Tap 'Pick Audio File' to load any MP3, AAC, M4A, or WAV from your phone—or press Play on the built-in Synth Loop.",
        "Assign Output Sink A and Output Sink B, then switch 'Linked' off to control each device's gain and delay independently.",
      ),
      proTip = "On phones without Android 13 LE Audio or Samsung Dual Audio, 'BT + USB-C / Wired' and 'TWS Split L/R' work 100% natively in real time.",
    )

    FeatureGuideId.LATENCY_CALIBRATION -> FeatureGuideContent(
      id = FeatureGuideId.LATENCY_CALIBRATION,
      title = "Millisecond Latency & Audio Sync",
      subtitle = "Eliminate lip-sync lag and echo between two speakers or earbuds.",
      steps = listOf(
        "Switch 'Sync Pulse' or 'Phase Chirp' in the Dual Player and press Play.",
        "Unlink Output A and Output B, then nudge the slower/faster output's Latency Offset slider (-250 ms to +250 ms) using the ±5 ms buttons until both pulses align into a single crisp snap.",
        "Your calibrated offset is automatically saved to Room for that device.",
      ),
      proTip = "Typical Bluetooth SBC/AAC adds ~120–180 ms compared to wired headphones. Add +140 ms to the wired Output B to sync it with Bluetooth Output A!",
    )

    FeatureGuideId.RADAR_FINDER -> FeatureGuideContent(
      id = FeatureGuideId.RADAR_FINDER,
      title = "RSSI Proximity Radar Finder",
      subtitle = "Locate misplaced earbuds or Bluetooth devices using real-time signal strength.",
      steps = listOf(
        "Select your missing device chip at the top and tap 'Ping'.",
        "Walk slowly while watching the scalloped sonar ring and dBm meter. Stronger values closer to -40 dBm mean you are within 1 meter.",
        "Enable Radar Haptics in Settings to feel a gentle vibration when the device enters Immediate range.",
      ),
      proTip = "Walls and pockets attenuate 2.4 GHz Bluetooth signals by 10–15 dBm; rotate slowly for the clearest line-of-sight reading.",
    )

    FeatureGuideId.CODEC_INSPECTOR -> FeatureGuideContent(
      id = FeatureGuideId.CODEC_INSPECTOR,
      title = "Live Codec & Signal Graph",
      subtitle = "Monitor active Bluetooth audio quality (LC3, LDAC, aptX, AAC, SBC) and signal stability.",
      steps = listOf(
        "Connect an A2DP or LE Audio headset and select it in the Radar tab.",
        "View live dBm fluctuations on the waveform chart to spot wireless interference.",
        "Inspect the Codec tiles for Sample Rate, Bit Depth, Channel Mode, and estimated hardware delay.",
      ),
      proTip = "For gaming or video sync, LC3 and aptX Adaptive offer much lower baseline latency than AAC or LDAC 990 kbps.",
    )

    FeatureGuideId.EQUALIZER_RULES -> FeatureGuideContent(
      id = FeatureGuideId.EQUALIZER_RULES,
      title = "Audible 5-Band EQ & Auto-Connect Rules",
      subtitle = "Shape sound per headset and automate EQ, Island pop-ups, and volume on connect.",
      steps = listOf(
        "Select a Bluetooth device chip at the top and pick a preset or drag the 5 EQ faders (60 Hz to 14 kHz).",
        "Because BluePulse binds Equalizer & BassBoost directly to the active Dual Player audio session (plus software fallback), every slider change is immediately audible during playback.",
        "Toggle the Auto-Connect switches below to save rules for that device's MAC address.",
      ),
      proTip = "Try 'Low-Latency Game' for crisp footsteps and dialogue, or 'Deep Bass' with 65% Bass Boost for open-fit earbuds.",
    )

    FeatureGuideId.FLOATING_ISLAND -> FeatureGuideContent(
      id = FeatureGuideId.FLOATING_ISLAND,
      title = "System-Wide Floating Island Overlay",
      subtitle = "iOS-style animated Dynamic Island pill for live battery, sync, and pairing alerts.",
      steps = listOf(
        "Grant 'Display Over Other Apps' in the Permissions sheet so the pill can float above any app.",
        "Tap the pill anytime to expand quick controls, or swipe upward to dismiss it.",
        "Use 'Test Island' in Settings or Hub to preview the spring pop-up animation anytime.",
      ),
      proTip = "Even without overlay permission, BluePulse automatically displays the Floating Island inside the app.",
    )
  }
}
