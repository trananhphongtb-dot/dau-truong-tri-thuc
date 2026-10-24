package vn.dautruongtrithuc.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.speech.tts.TextToSpeech
import vn.dautruongtrithuc.R
import vn.dautruongtrithuc.data.Prefs
import java.util.Locale

/** Hiệu ứng âm thanh (tiếng đồng hồ, chuông…) và giọng đọc tiếng Việt. */
class SoundFx(context: Context, private val prefs: Prefs) : TextToSpeech.OnInitListener {

    private val pool: SoundPool = SoundPool.Builder()
        .setMaxStreams(6)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val ids: Map<String, Int> = mapOf(
        "tick" to pool.load(context, R.raw.tick, 1),
        "tick_hi" to pool.load(context, R.raw.tick_hi, 1),
        "chime" to pool.load(context, R.raw.chime, 1),
        "correct" to pool.load(context, R.raw.correct, 1),
        "wrong" to pool.load(context, R.raw.wrong, 1),
        "lock" to pool.load(context, R.raw.lock, 1),
        "buzz" to pool.load(context, R.raw.buzz, 1),
        "win" to pool.load(context, R.raw.win, 1),
        "lose" to pool.load(context, R.raw.lose, 1),
    )

    private var tts: TextToSpeech? = null
    private var ttsReady = false

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale("vi", "VN"))
            ttsReady = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
        }
    }

    fun play(name: String) {
        if (!prefs.soundOn) return
        val id = ids[name] ?: return
        pool.play(id, 1f, 1f, 1, 0, 1f)
    }

    fun say(text: String) {
        if (!prefs.voiceOn || !ttsReady) return
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "dttt")
    }

    fun stopVoice() {
        tts?.stop()
    }

    fun release() {
        pool.release()
        tts?.shutdown()
        tts = null
    }
}
