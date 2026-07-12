package com.thefoxworks.tzafon.data.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.thefoxworks.tzafon.R

/**
 * FR-AUDIO-1 — Application-scoped SoundPool that plays the short "task done"
 * chime (`res/raw/chime_done`) on Open → Done via checkbox.
 *
 * `USAGE_ASSISTANCE_SONIFICATION` + `CONTENT_TYPE_SONIFICATION` route through
 * the notification/assistance stream, so the device's silent / Do-Not-Disturb
 * profile is authoritative (FR-AUDIO-1.3 / FR-AUDIO-1.7). SoundPool pre-loads
 * the asset once so playback is low-latency from the first tap.
 *
 * `playDone` is fire-and-forget: if the pool hasn't finished loading yet or the
 * asset failed, the call is a silent no-op — it never blocks or throws
 * (FR-AUDIO-1.9). The state write path is unaware audio exists.
 */
class ChimePlayer(context: Context) {

    private val pool: SoundPool =
        SoundPool.Builder()
            .setMaxStreams(1)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            .build()

    /** `0` sentinel until the load completes; `loaded` gates playback. */
    private var soundId: Int = 0
    @Volatile private var loaded: Boolean = false

    init {
        pool.setOnLoadCompleteListener { _, _, status -> if (status == 0) loaded = true }
        soundId = pool.load(context.applicationContext, R.raw.chime_done, 1)
    }

    fun playDone() {
        if (!loaded || soundId == 0) return
        // Left/right volume conservative — the asset itself is already soft
        // (~ -8 dBFS peak). Rate 1.0f = original pitch.
        pool.play(soundId, 0.8f, 0.8f, 1, 0, 1.0f)
    }

    fun release() {
        loaded = false
        pool.release()
    }
}
