package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.example.model.CalibrationMode
import com.example.model.DualAudioRouteMode
import com.example.model.EqBandUiModel
import com.example.model.MediaPlaybackState
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
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.roundToInt
import kotlin.math.sin

class AudioSyncDspEngine(private val context: Context) {

  private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

  private val activeEqualizers = mutableListOf<Equalizer>()
  private val activeBassBoosts = mutableListOf<BassBoost>()

  private var playbackJob: Job? = null
  private var activeTrackA: AudioTrack? = null
  private var activeTrackB: AudioTrack? = null

  private val _eqBands = MutableStateFlow(defaultBands())
  val eqBands: StateFlow<List<EqBandUiModel>> = _eqBands.asStateFlow()

  private val _dspAttachedToHardware = MutableStateFlow(false)
  val dspAttachedToHardware: StateFlow<Boolean> = _dspAttachedToHardware.asStateFlow()

  private val _isPlayingCalibration = MutableStateFlow(false)
  val isPlayingCalibration: StateFlow<Boolean> = _isPlayingCalibration.asStateFlow()

  private val _calibrationBeatTick = MutableStateFlow(0)
  val calibrationBeatTick: StateFlow<Int> = _calibrationBeatTick.asStateFlow()

  private val _mediaPlayback = MutableStateFlow(MediaPlaybackState())
  val mediaPlayback: StateFlow<MediaPlaybackState> = _mediaPlayback.asStateFlow()

  @Volatile private var currentVolA = 0.78f
  @Volatile private var currentVolB = 0.78f
  @Volatile private var currentDelayAMs = 0
  @Volatile private var currentDelayBMs = 0
  @Volatile private var currentBalanceA = 0f
  @Volatile private var currentBalanceB = 0f
  @Volatile private var currentBassStrength = 150
  @Volatile private var currentVirtualizerStrength = 100
  @Volatile private var currentSoftwareGainMultiplier = 1f
  @Volatile private var currentMode = CalibrationMode.AMBIENT_GROOVE
  @Volatile private var currentRouteMode = DualAudioRouteMode.BT_PLUS_WIRED_USB
  @Volatile private var currentMonoMixSplit = true
  @Volatile private var preferredDeviceA: AudioDeviceInfo? = null
  @Volatile private var preferredDeviceB: AudioDeviceInfo? = null

  init {
    attachEffectsToSessions(listOf(0))
  }

  private fun attachEffectsToSessions(sessionIds: List<Int>) {
    releaseAudioEffects()
    var anyAttached = false
    for (sessionId in sessionIds.distinct()) {
      runCatching {
        val eq = Equalizer(0, sessionId).apply { enabled = true }
        val currentBands = _eqBands.value
        val count = eq.numberOfBands.toInt().coerceAtMost(currentBands.size)
        for (i in 0 until count) {
          eq.setBandLevel(i.toShort(), currentBands[i].levelMillibels.toShort())
        }
        activeEqualizers.add(eq)
        anyAttached = true
      }.onFailure {
        Log.w(TAG, "Equalizer attach failed on session=$sessionId", it)
      }

      runCatching {
        val bb = BassBoost(0, sessionId).apply {
          enabled = true
          if (strengthSupported) setStrength(currentBassStrength.coerceIn(0, 1000).toShort())
        }
        activeBassBoosts.add(bb)
      }
    }
    _dspAttachedToHardware.value = anyAttached
  }

  private fun releaseAudioEffects() {
    activeEqualizers.forEach { runCatching { it.release() } }
    activeEqualizers.clear()
    activeBassBoosts.forEach { runCatching { it.release() } }
    activeBassBoosts.clear()
  }

  fun setBandLevel(bandIndex: Int, levelMillibels: Int) {
    val updated = _eqBands.value.map { band ->
      if (band.index == bandIndex) {
        val clamped = levelMillibels.coerceIn(band.minMillibels, band.maxMillibels)
        activeEqualizers.forEach { eq ->
          runCatching {
            if (bandIndex < eq.numberOfBands.toInt()) {
              eq.setBandLevel(bandIndex.toShort(), clamped.toShort())
            }
          }
        }
        band.copy(levelMillibels = clamped)
      } else {
        band
      }
    }
    _eqBands.value = updated
    recomputeSoftwareEqPresence()
  }

  fun applyEqPreset(levels: List<Int>, bassStrength: Int, virtualizerStrength: Int) {
    currentBassStrength = bassStrength.coerceIn(0, 1000)
    currentVirtualizerStrength = virtualizerStrength.coerceIn(0, 1000)

    val current = _eqBands.value
    val updated = current.mapIndexed { idx, band ->
      val level = levels.getOrElse(idx) { 0 }.coerceIn(band.minMillibels, band.maxMillibels)
      activeEqualizers.forEach { eq ->
        runCatching {
          if (idx < eq.numberOfBands.toInt()) {
            eq.setBandLevel(idx.toShort(), level.toShort())
          }
        }
      }
      band.copy(levelMillibels = level)
    }
    _eqBands.value = updated

    activeBassBoosts.forEach { bb ->
      runCatching {
        if (bb.strengthSupported) bb.setStrength(currentBassStrength.toShort())
      }
    }
    recomputeSoftwareEqPresence()
  }

  private fun recomputeSoftwareEqPresence() {
    val avgDb = _eqBands.value.map { it.levelMillibels / 100f }.average().toFloat()
    // Subtle software gain compensation when hardware Equalizer isn't attached
    currentSoftwareGainMultiplier = (1f + (avgDb * 0.03f)).coerceIn(0.65f, 1.35f)
  }

  fun getSystemMusicVolumePercent(): Int {
    val am = audioManager ?: return 70
    val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
    val curr = am.getStreamVolume(AudioManager.STREAM_MUSIC)
    return ((curr * 100f) / max).roundToInt().coerceIn(0, 100)
  }

  fun setSystemMusicVolumePercent(percent: Int) {
    val am = audioManager ?: return
    val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
    val target = ((percent.coerceIn(0, 100) / 100f) * max).roundToInt()
    runCatching {
      am.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0)
    }
  }

  fun updateDualRouting(
    routeMode: DualAudioRouteMode,
    monoMixForSplit: Boolean,
    sinkA: AudioDeviceInfo?,
    sinkB: AudioDeviceInfo?,
  ) {
    val needsRestart = _isPlayingCalibration.value && (
      routeMode != currentRouteMode ||
        preferredDeviceA?.id != sinkA?.id ||
        preferredDeviceB?.id != sinkB?.id
      )
    currentRouteMode = routeMode
    currentMonoMixSplit = monoMixForSplit
    preferredDeviceA = sinkA
    preferredDeviceB = sinkB
    activeTrackA?.let { runCatching { it.preferredDevice = sinkA } }
    activeTrackB?.let { runCatching { it.preferredDevice = sinkB } }
    if (needsRestart) {
      stopCalibrationTone()
      startCalibrationTone()
    }
  }

  fun updateDualCalibrationParameters(
    volA: Float,
    volB: Float,
    delayAMs: Int,
    delayBMs: Int,
    balanceA: Float,
    balanceB: Float,
    mode: CalibrationMode,
  ) {
    currentVolA = volA.coerceIn(0f, 1f)
    currentVolB = volB.coerceIn(0f, 1f)
    currentDelayAMs = delayAMs.coerceIn(-250, 250)
    currentDelayBMs = delayBMs.coerceIn(-250, 250)
    currentBalanceA = balanceA.coerceIn(-1f, 1f)
    currentBalanceB = balanceB.coerceIn(-1f, 1f)
    currentMode = mode
  }

  fun loadAudioFile(uri: Uri) {
    val title = queryDisplayName(uri) ?: "Custom Audio Track"
    val durationMs = probeDurationMs(uri) ?: 180_000L
    val wasPlaying = _isPlayingCalibration.value
    if (wasPlaying) stopCalibrationTone()
    _mediaPlayback.value = MediaPlaybackState(
      trackTitle = title,
      selectedUriString = uri.toString(),
      isPlaying = false,
      positionMs = 0L,
      durationMs = durationMs,
      isCustomFile = true,
    )
    if (wasPlaying) {
      startCalibrationTone()
    }
  }

  fun clearCustomAudioFile() {
    val wasPlaying = _isPlayingCalibration.value
    if (wasPlaying) stopCalibrationTone()
    _mediaPlayback.value = MediaPlaybackState(
      trackTitle = currentMode.title,
      selectedUriString = null,
      isPlaying = false,
      positionMs = 0L,
      durationMs = 30_000L,
      isCustomFile = false,
    )
    if (wasPlaying) startCalibrationTone()
  }

  fun toggleCalibrationTone() {
    if (_isPlayingCalibration.value) {
      stopCalibrationTone()
    } else {
      startCalibrationTone()
    }
  }

  fun startCalibrationTone() {
    if (_isPlayingCalibration.value) return
    _isPlayingCalibration.value = true
    _mediaPlayback.value = _mediaPlayback.value.copy(isPlaying = true)

    val uriStr = _mediaPlayback.value.selectedUriString
    playbackJob = scope.launch {
      if (uriStr != null) {
        runCatching {
          streamDecodedAudioFile(Uri.parse(uriStr))
        }.onFailure { err ->
          Log.w(TAG, "Custom audio decode fallback to synth loop", err)
          streamSynthesizedAudio()
        }
      } else {
        streamSynthesizedAudio()
      }
    }
  }

  private fun createTrack(sampleRate: Int, isCommunicationStream: Boolean, preferred: AudioDeviceInfo?): AudioTrack? {
    val minBuf = AudioTrack.getMinBufferSize(
      sampleRate,
      AudioFormat.CHANNEL_OUT_STEREO,
      AudioFormat.ENCODING_PCM_16BIT,
    ).coerceAtLeast(sampleRate / 5 * 4)

    val usage = if (isCommunicationStream) {
      AudioAttributes.USAGE_VOICE_COMMUNICATION
    } else {
      AudioAttributes.USAGE_MEDIA
    }
    val contentType = if (isCommunicationStream) {
      AudioAttributes.CONTENT_TYPE_SPEECH
    } else {
      AudioAttributes.CONTENT_TYPE_MUSIC
    }

    return runCatching {
      AudioTrack.Builder()
        .setAudioAttributes(
          AudioAttributes.Builder()
            .setUsage(usage)
            .setContentType(contentType)
            .build()
        )
        .setAudioFormat(
          AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(sampleRate)
            .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
            .build()
        )
        .setBufferSizeInBytes(minBuf)
        .setTransferMode(AudioTrack.MODE_STREAM)
        .build()
        .apply {
          if (preferred != null) {
            preferredDevice = preferred
          }
        }
    }.getOrNull()
  }

  private fun setupDualTracks(sampleRate: Int): Pair<AudioTrack?, AudioTrack?> {
    val useScoForB = currentRouteMode == DualAudioRouteMode.BT_A2DP_PLUS_SCO
    if (useScoForB) {
      runCatching {
        audioManager?.mode = AudioManager.MODE_IN_COMMUNICATION
        @Suppress("DEPRECATION")
        audioManager?.startBluetoothSco()
        @Suppress("DEPRECATION")
        audioManager?.isBluetoothScoOn = true
      }
    }

    val trackA = createTrack(sampleRate, isCommunicationStream = false, preferred = preferredDeviceA)
    val needsSecondTrack = currentRouteMode != DualAudioRouteMode.TWS_SPLIT_LR &&
      (preferredDeviceB != null || useScoForB)
    val trackB = if (needsSecondTrack) {
      createTrack(sampleRate, isCommunicationStream = useScoForB, preferred = preferredDeviceB)
    } else {
      null
    }

    activeTrackA = trackA
    activeTrackB = trackB

    val sessions = listOfNotNull(trackA?.audioSessionId, trackB?.audioSessionId).ifEmpty { listOf(0) }
    attachEffectsToSessions(sessions)

    runCatching { trackA?.play() }
    runCatching { trackB?.play() }
    return trackA to trackB
  }

  private suspend fun streamSynthesizedAudio() {
    val sampleRate = 48_000
    val chunkFrames = 2048
    val (trackA, trackB) = setupDualTracks(sampleRate)

    if (trackA == null) {
      var beat = 0
      while (scope.isActive && _isPlayingCalibration.value) {
        _calibrationBeatTick.value = ++beat
        _mediaPlayback.value = _mediaPlayback.value.copy(
          positionMs = ((beat * 550L) % 30_000L),
        )
        delay(550L)
      }
      return
    }

    val ringCapacity = sampleRate // 1 second circular delay buffer
    val ringL = FloatArray(ringCapacity)
    val ringR = FloatArray(ringCapacity)
    var ringWritePos = 0

    val outA = ShortArray(chunkFrames * 2)
    val outB = ShortArray(chunkFrames * 2)

    var totalSamples = 0L
    var beatCounter = 0

    while (scope.isActive && _isPlayingCalibration.value) {
      val chunkStartSample = totalSamples
      for (i in 0 until chunkFrames) {
        val sampleIdx = chunkStartSample + i
        val t = sampleIdx.toDouble() / sampleRate
        val (srcL, srcR) = synthesizeSampleAt(sampleIdx, t, sampleRate)

        ringL[ringWritePos] = srcL
        ringR[ringWritePos] = srcR

        writeDelayedOutputs(
          ringL = ringL,
          ringR = ringR,
          ringWritePos = ringWritePos,
          ringCapacity = ringCapacity,
          sampleRate = sampleRate,
          frameIndex = i,
          outA = outA,
          outB = outB,
        )

        ringWritePos = (ringWritePos + 1) % ringCapacity
      }

      totalSamples += chunkFrames
      val newBeat = (totalSamples / (sampleRate / 2)).toInt()
      if (newBeat != beatCounter) {
        beatCounter = newBeat
        _calibrationBeatTick.value = beatCounter
      }
      _mediaPlayback.value = _mediaPlayback.value.copy(
        positionMs = ((totalSamples * 1000L) / sampleRate) % 30_000L,
      )

      runCatching { trackA.write(outA, 0, outA.size) }
      if (trackB != null) {
        runCatching { trackB.write(outB, 0, outB.size) }
      }
    }
  }

  private suspend fun streamDecodedAudioFile(uri: Uri) {
    val extractor = MediaExtractor()
    try {
      extractor.setDataSource(context, uri, null)
      var trackIndex = -1
      var format: MediaFormat? = null
      for (i in 0 until extractor.trackCount) {
        val f = extractor.getTrackFormat(i)
        val mime = f.getString(MediaFormat.KEY_MIME).orEmpty()
        if (mime.startsWith("audio/")) {
          trackIndex = i
          format = f
          break
        }
      }
      if (trackIndex < 0 || format == null) {
        streamSynthesizedAudio()
        return
      }
      extractor.selectTrack(trackIndex)
      val mime = format.getString(MediaFormat.KEY_MIME)!!
      var sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
      var channelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)

      val (trackA, trackB) = setupDualTracks(sampleRate)
      if (trackA == null) {
        streamSynthesizedAudio()
        return
      }

      val ringCapacity = sampleRate
      val ringL = FloatArray(ringCapacity)
      val ringR = FloatArray(ringCapacity)
      var ringWritePos = 0

      val codec = MediaCodec.createDecoderByType(mime)
      codec.configure(format, null, null, 0)
      codec.start()

      val bufferInfo = MediaCodec.BufferInfo()
      var inputDone = false

      try {
        while (scope.isActive && _isPlayingCalibration.value) {
          if (!inputDone) {
            val inIdx = codec.dequeueInputBuffer(5_000L)
            if (inIdx >= 0) {
              val inBuf = codec.getInputBuffer(inIdx)!!
              val sampleSize = extractor.readSampleData(inBuf, 0)
              if (sampleSize < 0) {
                // Loop seamlessly back to start
                extractor.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
                codec.queueInputBuffer(inIdx, 0, 0, 0, 0)
              } else {
                val pts = extractor.sampleTime
                codec.queueInputBuffer(inIdx, 0, sampleSize, pts, 0)
                extractor.advance()
              }
            }
          }

          val outIdx = codec.dequeueOutputBuffer(bufferInfo, 5_000L)
          if (outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
            val newFmt = codec.outputFormat
            sampleRate = newFmt.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            channelCount = newFmt.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
          } else if (outIdx >= 0) {
            val outBuf = codec.getOutputBuffer(outIdx)
            if (outBuf != null && bufferInfo.size > 0) {
              outBuf.position(bufferInfo.offset)
              outBuf.limit(bufferInfo.offset + bufferInfo.size)
              val shortBuf = outBuf.asShortBuffer()
              val totalShorts = shortBuf.remaining()
              val frames = totalShorts / channelCount.coerceAtLeast(1)
              if (frames > 0) {
                val pcmOutA = ShortArray(frames * 2)
                val pcmOutB = ShortArray(frames * 2)
                for (f in 0 until frames) {
                  val sL = shortBuf.get() / 32768f
                  val sR = if (channelCount >= 2) shortBuf.get() / 32768f else sL
                  for (extra in 2 until channelCount) shortBuf.get()

                  ringL[ringWritePos] = sL
                  ringR[ringWritePos] = sR

                  writeDelayedOutputs(
                    ringL = ringL,
                    ringR = ringR,
                    ringWritePos = ringWritePos,
                    ringCapacity = ringCapacity,
                    sampleRate = sampleRate,
                    frameIndex = f,
                    outA = pcmOutA,
                    outB = pcmOutB,
                  )
                  ringWritePos = (ringWritePos + 1) % ringCapacity
                }

                _mediaPlayback.value = _mediaPlayback.value.copy(
                  positionMs = bufferInfo.presentationTimeUs / 1000L,
                )
                runCatching { trackA.write(pcmOutA, 0, pcmOutA.size) }
                if (trackB != null) {
                  runCatching { trackB.write(pcmOutB, 0, pcmOutB.size) }
                }
              }
            }
            codec.releaseOutputBuffer(outIdx, false)
          }
        }
      } finally {
        runCatching {
          codec.stop()
          codec.release()
        }
      }
    } finally {
      runCatching { extractor.release() }
    }
  }

  private fun writeDelayedOutputs(
    ringL: FloatArray,
    ringR: FloatArray,
    ringWritePos: Int,
    ringCapacity: Int,
    sampleRate: Int,
    frameIndex: Int,
    outA: ShortArray,
    outB: ShortArray,
  ) {
    val minDelay = minOf(currentDelayAMs, currentDelayBMs)
    val delayFramesA = (((currentDelayAMs - minDelay).coerceAtLeast(0)) * sampleRate / 1000)
      .coerceIn(0, ringCapacity - 1)
    val delayFramesB = (((currentDelayBMs - minDelay).coerceAtLeast(0)) * sampleRate / 1000)
      .coerceIn(0, ringCapacity - 1)

    val readIdxA = (ringWritePos - delayFramesA + ringCapacity) % ringCapacity
    val readIdxB = (ringWritePos - delayFramesB + ringCapacity) % ringCapacity

    val rawAL = ringL[readIdxA]
    val rawAR = ringR[readIdxA]
    val rawBL = ringL[readIdxB]
    val rawBR = ringR[readIdxB]

    val eqBoost = if (activeEqualizers.isEmpty()) currentSoftwareGainMultiplier else 1f

    if (currentRouteMode == DualAudioRouteMode.TWS_SPLIT_LR) {
      // Output A drives Left earbud, Output B drives Right earbud independently!
      val monoA = if (currentMonoMixSplit) (rawAL + rawAR) * 0.5f else rawAL
      val monoB = if (currentMonoMixSplit) (rawBL + rawBR) * 0.5f else rawBR
      val leftOut = (monoA * currentVolA * eqBoost).coerceIn(-0.98f, 0.98f)
      val rightOut = (monoB * currentVolB * eqBoost).coerceIn(-0.98f, 0.98f)
      outA[frameIndex * 2] = (leftOut * Short.MAX_VALUE).toInt().toShort()
      outA[frameIndex * 2 + 1] = (rightOut * Short.MAX_VALUE).toInt().toShort()
    } else {
      val panAL = currentVolA * (1f - currentBalanceA.coerceAtLeast(0f)) * eqBoost
      val panAR = currentVolA * (1f - (-currentBalanceA).coerceAtLeast(0f)) * eqBoost
      outA[frameIndex * 2] = ((rawAL * panAL).coerceIn(-0.98f, 0.98f) * Short.MAX_VALUE).toInt().toShort()
      outA[frameIndex * 2 + 1] = ((rawAR * panAR).coerceIn(-0.98f, 0.98f) * Short.MAX_VALUE).toInt().toShort()

      val panBL = currentVolB * (1f - currentBalanceB.coerceAtLeast(0f)) * eqBoost
      val panBR = currentVolB * (1f - (-currentBalanceB).coerceAtLeast(0f)) * eqBoost
      outB[frameIndex * 2] = ((rawBL * panBL).coerceIn(-0.98f, 0.98f) * Short.MAX_VALUE).toInt().toShort()
      outB[frameIndex * 2 + 1] = ((rawBR * panBR).coerceIn(-0.98f, 0.98f) * Short.MAX_VALUE).toInt().toShort()
    }
  }

  private fun synthesizeSampleAt(sampleIdx: Long, t: Double, sampleRate: Int): Pair<Float, Float> {
    val intervalSamples = (sampleRate * 0.55).toLong()
    val posInBeat = (sampleIdx % intervalSamples).toInt()
    val beatNum = (sampleIdx / intervalSamples).toInt()

    return when (currentMode) {
      CalibrationMode.AMBIENT_GROOVE -> {
        val chords = arrayOf(
          doubleArrayOf(220.0, 277.18, 329.63, 440.0), // A major
          doubleArrayOf(196.0, 246.94, 293.66, 392.0), // G major
          doubleArrayOf(174.61, 220.0, 261.63, 349.23), // F major
          doubleArrayOf(164.81, 207.65, 246.94, 329.63), // E major
        )
        val chord = chords[(beatNum / 2) % chords.size]
        val env = exp(-posInBeat / (sampleRate * 0.32)) * 0.25
        var l = 0.0
        var r = 0.0
        chord.forEachIndexed { idx, freq ->
          val wave = sin(2.0 * PI * freq * t)
          l += wave * (if (idx % 2 == 0) 1.0 else 0.7)
          r += wave * (if (idx % 2 == 1) 1.0 else 0.7)
        }
        val sub = sin(2.0 * PI * (chord[0] * 0.5) * t) * exp(-posInBeat / (sampleRate * 0.16)) * 0.35
        ((l * env + sub).toFloat().coerceIn(-0.9f, 0.9f)) to
          ((r * env + sub).toFloat().coerceIn(-0.9f, 0.9f))
      }

      CalibrationMode.SYNC_PULSE -> {
        val pulseLen = (sampleRate * 0.08).toInt()
        if (posInBeat < pulseLen) {
          val env = exp(-posInBeat / (sampleRate * 0.02)) * 0.75
          val s = (sin(2.0 * PI * 660.0 * t) * env).toFloat()
          s to s
        } else {
          0f to 0f
        }
      }

      CalibrationMode.STEREO_SWEEP -> {
        val pulseLen = (sampleRate * 0.1).toInt()
        if (posInBeat < pulseLen) {
          val env = exp(-posInBeat / (sampleRate * 0.028)) * 0.75
          val isLeft = beatNum % 2 == 0
          val freq = if (isLeft) 523.25 else 783.99
          val s = (sin(2.0 * PI * freq * t) * env).toFloat()
          if (isLeft) s to 0f else 0f to s
        } else {
          0f to 0f
        }
      }

      CalibrationMode.PHASE_CHIRP -> {
        val pulseLen = (sampleRate * 0.09).toInt()
        if (posInBeat < pulseLen) {
          val env = exp(-posInBeat / (sampleRate * 0.022)) * 0.75
          val sweep = 600.0 + (posInBeat * 0.08)
          val s = (sin(2.0 * PI * sweep * t) * env).toFloat()
          s to s
        } else {
          0f to 0f
        }
      }
    }
  }

  private fun queryDisplayName(uri: Uri): String? {
    return runCatching {
      context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) cursor.getString(0) else null
      }
    }.getOrNull()
  }

  private fun probeDurationMs(uri: Uri): Long? {
    return runCatching {
      val extractor = MediaExtractor()
      extractor.setDataSource(context, uri, null)
      var dur: Long? = null
      for (i in 0 until extractor.trackCount) {
        val f = extractor.getTrackFormat(i)
        if (f.getString(MediaFormat.KEY_MIME).orEmpty().startsWith("audio/") &&
          f.containsKey(MediaFormat.KEY_DURATION)
        ) {
          dur = f.getLong(MediaFormat.KEY_DURATION) / 1000L
          break
        }
      }
      extractor.release()
      dur
    }.getOrNull()
  }

  fun stopCalibrationTone() {
    playbackJob?.cancel()
    playbackJob = null
    _isPlayingCalibration.value = false
    _mediaPlayback.value = _mediaPlayback.value.copy(isPlaying = false)

    listOf(activeTrackA, activeTrackB).forEach { track ->
      track?.let {
        runCatching {
          it.pause()
          it.flush()
          it.stop()
          it.release()
        }
      }
    }
    activeTrackA = null
    activeTrackB = null

    if (currentRouteMode == DualAudioRouteMode.BT_A2DP_PLUS_SCO) {
      runCatching {
        @Suppress("DEPRECATION")
        audioManager?.isBluetoothScoOn = false
        @Suppress("DEPRECATION")
        audioManager?.stopBluetoothSco()
        audioManager?.mode = AudioManager.MODE_NORMAL
      }
    }
  }

  fun release() {
    stopCalibrationTone()
    releaseAudioEffects()
  }

  companion object {
    private const val TAG = "BluePulseDSP"

    fun defaultBands(): List<EqBandUiModel> = listOf(
      EqBandUiModel(0, "60Hz", 0),
      EqBandUiModel(1, "230Hz", 0),
      EqBandUiModel(2, "910Hz", 0),
      EqBandUiModel(3, "3.6kHz", 0),
      EqBandUiModel(4, "14kHz", 0),
    )

    val PRESETS: Map<String, Triple<List<Int>, Int, Int>> = mapOf(
      "Studio Flat" to Triple(listOf(0, 0, 0, 0, 0), 150, 100),
      "Deep Bass" to Triple(listOf(650, 400, 50, 100, 200), 750, 250),
      "Vocal Clarity" to Triple(listOf(-200, 100, 550, 600, 250), 150, 350),
      "Low-Latency Game" to Triple(listOf(300, 150, 250, 500, 400), 400, 650),
      "Spatial Air" to Triple(listOf(150, -100, 200, 450, 650), 300, 850),
    )
  }
}
