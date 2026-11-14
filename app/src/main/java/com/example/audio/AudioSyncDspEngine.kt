package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import android.util.Log
import com.example.model.CalibrationMode
import com.example.model.EqBandUiModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.roundToInt
import kotlin.math.sin

class AudioSyncDspEngine(context: Context) {

  private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

  private var equalizer: Equalizer? = null
  private var bassBoost: BassBoost? = null
  private var virtualizer: Virtualizer? = null

  private var calibrationJob: Job? = null
  private var activeAudioTrack: AudioTrack? = null

  private val _eqBands = MutableStateFlow(defaultBands())
  val eqBands: StateFlow<List<EqBandUiModel>> = _eqBands.asStateFlow()

  private val _dspAttachedToHardware = MutableStateFlow(false)
  val dspAttachedToHardware: StateFlow<Boolean> = _dspAttachedToHardware.asStateFlow()

  private val _isPlayingCalibration = MutableStateFlow(false)
  val isPlayingCalibration: StateFlow<Boolean> = _isPlayingCalibration.asStateFlow()

  private val _calibrationBeatTick = MutableStateFlow(0)
  val calibrationBeatTick: StateFlow<Int> = _calibrationBeatTick.asStateFlow()

  @Volatile private var currentVolA = 0.75f
  @Volatile private var currentVolB = 0.75f
  @Volatile private var currentDelayAMs = 0
  @Volatile private var currentDelayBMs = 0
  @Volatile private var currentBalanceA = 0f
  @Volatile private var currentBalanceB = 0f
  @Volatile private var currentMode = CalibrationMode.SYNC_PULSE

  init {
    attachGlobalAudioEffects(sessionId = 0)
  }

  private fun attachGlobalAudioEffects(sessionId: Int) {
    runCatching {
      equalizer?.release()
      val eq = Equalizer(0, sessionId).apply { enabled = true }
      equalizer = eq
      val bandCount = eq.numberOfBands.toInt().coerceAtMost(5)
      val range = eq.bandLevelRange
      val minMb = range.getOrElse(0) { -1500 }.toInt()
      val maxMb = range.getOrElse(1) { 1500 }.toInt()
      val bands = (0 until bandCount).map { idx ->
        val freqHz = eq.getCenterFreq(idx.toShort()) / 1000
        val label = if (freqHz >= 1000) "${freqHz / 1000}kHz" else "${freqHz}Hz"
        EqBandUiModel(
          index = idx,
          centerFreqLabel = label,
          levelMillibels = eq.getBandLevel(idx.toShort()).toInt(),
          minMillibels = minMb,
          maxMillibels = maxMb,
        )
      }
      if (bands.isNotEmpty()) {
        _eqBands.value = bands
      }
      _dspAttachedToHardware.value = true
    }.onFailure {
      Log.w(TAG, "Global AudioEffect session unavailable; using local DSP state", it)
      _dspAttachedToHardware.value = false
    }

    runCatching {
      bassBoost?.release()
      bassBoost = BassBoost(0, sessionId).apply { enabled = true }
    }
    runCatching {
      virtualizer?.release()
      virtualizer = Virtualizer(0, sessionId).apply { enabled = true }
    }
  }

  fun setBandLevel(bandIndex: Int, levelMillibels: Int) {
    val updated = _eqBands.value.map { band ->
      if (band.index == bandIndex) {
        val clamped = levelMillibels.coerceIn(band.minMillibels, band.maxMillibels)
        runCatching {
          equalizer?.setBandLevel(bandIndex.toShort(), clamped.toShort())
        }
        band.copy(levelMillibels = clamped)
      } else {
        band
      }
    }
    _eqBands.value = updated
  }

  fun applyEqPreset(levels: List<Int>, bassStrength: Int, virtualizerStrength: Int) {
    val current = _eqBands.value
    val updated = current.mapIndexed { idx, band ->
      val level = levels.getOrElse(idx) { 0 }.coerceIn(band.minMillibels, band.maxMillibels)
      runCatching {
        equalizer?.setBandLevel(band.index.toShort(), level.toShort())
      }
      band.copy(levelMillibels = level)
    }
    _eqBands.value = updated

    runCatching {
      bassBoost?.takeIf { it.strengthSupported }?.setStrength(bassStrength.coerceIn(0, 1000).toShort())
    }
    runCatching {
      virtualizer?.takeIf { it.strengthSupported }?.setStrength(virtualizerStrength.coerceIn(0, 1000).toShort())
    }
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

    calibrationJob = scope.launch {
      val sampleRate = 48_000
      val intervalMs = 600
      val totalFrames = (sampleRate * intervalMs) / 1000
      val minBuf = AudioTrack.getMinBufferSize(
        sampleRate,
        AudioFormat.CHANNEL_OUT_STEREO,
        AudioFormat.ENCODING_PCM_16BIT,
      ).coerceAtLeast(totalFrames * 4)

      val track = runCatching {
        AudioTrack.Builder()
          .setAudioAttributes(
            AudioAttributes.Builder()
              .setUsage(AudioAttributes.USAGE_MEDIA)
              .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
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
      }.getOrNull()

      if (track == null) {
        // Fallback visual beat tick if AudioTrack hardware is unavailable
        var beat = 0
        while (isActive) {
          _calibrationBeatTick.value = ++beat
          kotlinx.coroutines.delay(intervalMs.toLong())
        }
        return@launch
      }

      activeAudioTrack = track
      attachGlobalAudioEffects(track.audioSessionId)
      runCatching { track.play() }

      val pcmBuffer = ShortArray(totalFrames * 2)
      var beat = 0

      while (isActive) {
        _calibrationBeatTick.value = ++beat
        synthesizeDualCalibrationBlock(
          outBuffer = pcmBuffer,
          frames = totalFrames,
          sampleRate = sampleRate,
          beatNumber = beat,
        )
        runCatching {
          track.write(pcmBuffer, 0, pcmBuffer.size)
        }
      }
    }
  }

  private fun synthesizeDualCalibrationBlock(
    outBuffer: ShortArray,
    frames: Int,
    sampleRate: Int,
    beatNumber: Int,
  ) {
    // Normalize relative latency so positive offset delays that output relative to the earlier one
    val minDelay = minOf(currentDelayAMs, currentDelayBMs)
    val startSampleA = (((currentDelayAMs - minDelay).coerceAtLeast(0)) * sampleRate / 1000) + (sampleRate / 20)
    val startSampleB = (((currentDelayBMs - minDelay).coerceAtLeast(0)) * sampleRate / 1000) + (sampleRate / 20)

    val pulseDurationSamples = (sampleRate * 0.085).toInt()
    val freqA = when (currentMode) {
      CalibrationMode.SYNC_PULSE -> 660.0
      CalibrationMode.STEREO_SWEEP -> if (beatNumber % 2 == 0) 520.0 else 0.0
      CalibrationMode.PHASE_CHIRP -> 880.0
    }
    val freqB = when (currentMode) {
      CalibrationMode.SYNC_PULSE -> 660.0
      CalibrationMode.STEREO_SWEEP -> if (beatNumber % 2 == 1) 780.0 else 0.0
      CalibrationMode.PHASE_CHIRP -> 880.0
    }

    val gainLFromA = currentVolA * (1f - currentBalanceA.coerceAtLeast(0f))
    val gainRFromB = currentVolB * (1f - (-currentBalanceB).coerceAtLeast(0f))

    for (i in 0 until frames) {
      var sampleA = 0.0
      val dtA = i - startSampleA
      if (dtA in 0 until pulseDurationSamples && freqA > 0.0) {
        val env = exp(-dtA / (sampleRate * 0.022))
        val sweep = if (currentMode == CalibrationMode.PHASE_CHIRP) freqA + (dtA * 0.04) else freqA
        sampleA = sin(2.0 * PI * sweep * dtA / sampleRate) * env
      }

      var sampleB = 0.0
      val dtB = i - startSampleB
      if (dtB in 0 until pulseDurationSamples && freqB > 0.0) {
        val env = exp(-dtB / (sampleRate * 0.022))
        val sweep = if (currentMode == CalibrationMode.PHASE_CHIRP) freqB + (dtB * 0.04) else freqB
        sampleB = sin(2.0 * PI * sweep * dtB / sampleRate) * env
      }

      val leftOut = (sampleA * gainLFromA).coerceIn(-0.98, 0.98)
      val rightOut = (sampleB * gainRFromB).coerceIn(-0.98, 0.98)

      outBuffer[i * 2] = (leftOut * Short.MAX_VALUE).toInt().toShort()
      outBuffer[i * 2 + 1] = (rightOut * Short.MAX_VALUE).toInt().toShort()
    }
  }

  fun stopCalibrationTone() {
    calibrationJob?.cancel()
    calibrationJob = null
    _isPlayingCalibration.value = false
    activeAudioTrack?.let { track ->
      runCatching {
        track.pause()
        track.flush()
        track.stop()
        track.release()
      }
    }
    activeAudioTrack = null
  }

  fun release() {
    stopCalibrationTone()
    runCatching { equalizer?.release() }
    runCatching { bassBoost?.release() }
    runCatching { virtualizer?.release() }
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
