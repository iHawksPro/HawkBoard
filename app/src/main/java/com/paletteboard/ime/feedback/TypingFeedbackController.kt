package com.paletteboard.ime.feedback

import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.paletteboard.domain.model.KeyCodes
import com.paletteboard.domain.model.KeyboardKeySpec
import com.paletteboard.domain.model.UserSettings

class TypingFeedbackController(
    context: Context,
) {
    private val audioManager = context.getSystemService(AudioManager::class.java)
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Vibrator::class.java)
    }

    fun performKeyFeedback(
        key: KeyboardKeySpec,
        settings: UserSettings,
    ) {
        playSound(key, settings.soundPackId)
        vibrate(settings.hapticProfileId)
    }

    private fun playSound(
        key: KeyboardKeySpec,
        soundPackId: String,
    ) {
        if (audioManager?.ringerMode != AudioManager.RINGER_MODE_NORMAL) return
        val (effect, volume) = when (key.code) {
            KeyCodes.BACKSPACE -> AudioManager.FX_KEYPRESS_DELETE to volumeFor(soundPackId) * 0.82f
            KeyCodes.ENTER -> AudioManager.FX_KEYPRESS_RETURN to volumeFor(soundPackId)
            KeyCodes.SPACE -> AudioManager.FX_KEYPRESS_SPACEBAR to volumeFor(soundPackId)
            else -> AudioManager.FX_KEYPRESS_STANDARD to volumeFor(soundPackId)
        }
        audioManager?.playSoundEffect(effect, volume)
    }

    private fun vibrate(hapticProfileId: String) {
        val deviceVibrator = vibrator ?: return
        if (!deviceVibrator.hasVibrator()) return
        val (duration, amplitude) = when (hapticProfileId) {
            "light" -> 7L to 32
            "strong" -> 18L to 96
            else -> 10L to 58
        }
        deviceVibrator.vibrate(VibrationEffect.createOneShot(duration, amplitude))
    }

    private fun volumeFor(soundPackId: String): Float = when (soundPackId) {
        "soft" -> 0.1f
        "arcade" -> 0.3f
        else -> 0.18f
    }
}
