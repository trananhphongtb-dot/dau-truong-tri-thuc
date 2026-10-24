package vn.dautruongtrithuc.game

import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import vn.dautruongtrithuc.audio.SoundFx
import vn.dautruongtrithuc.data.Prefs
import vn.dautruongtrithuc.data.Question
import vn.dautruongtrithuc.data.QuestionBank
import kotlin.random.Random

/** bots = 0 là luyện tập một mình; level 0 dễ, 1 vừa, 2 khó. */
data class MatchConfig(val bots: Int, val level: Int)

data class PlayerState(
    val id: Int,
    val name: String,
    val isBot: Boolean,
    val palette: Int,
    val score: Int = 0,
)

enum class Phase { INTRO, PLAY, REVEAL, END }

val PART_NAMES = listOf("", "Khởi động", "Đấu Trí", "Về Đích")

class MatchViewModel : ViewModel() {

    val players = mutableStateListOf<PlayerState>()
    var part by mutableIntStateOf(0)
        private set
    var phase by mutableStateOf(Phase.INTRO)
        private set
    var introLeft by mutableIntStateOf(0)
        private set
    var question by mutableStateOf<Question?>(null)
        private set
    var qNo by mutableIntStateOf(0)
        private set
    var qTotal by mutableIntStateOf(0)
        private set
    var timeLeftMs by mutableLongStateOf(0L)
        private set
    var timeTotalMs by mutableLongStateOf(1L)
        private set
    var humanChoice by mutableStateOf<Int?>(null)
        private set
    var reveal by mutableStateOf(false)
        private set
    var locked by mutableStateOf<Set<Int>>(emptySet())
        private set
    var turnId by mutableStateOf<Int?>(null)
        private set
    var banner by mutableStateOf("")
        private set
    var roundText by mutableStateOf("")
        private set
    var winners by mutableStateOf<Set<Int>>(emptySet())
        private set

    var solo = true
        private set

    private var started = false
    private var job: Job? = null
    private lateinit var cfg: MatchConfig
    private lateinit var prefs: Prefs
    private lateinit var fx: SoundFx
    private val rnd = Random(System.nanoTime())
    private val usedIds = HashSet<String>()
    private var pSeen: Set<String> = emptySet()
    private var pWrong: Set<String> = emptySet()
    private val shownNow = LinkedHashSet<String>()
    private val resultsNow = LinkedHashMap<String, Boolean>()

    // Trạng thái trong một câu hỏi
    private var questionStart = 0L
    private var humanAnswerAt = -1L
    private var skipFlag = false
    private val botAnswers = HashMap<Int, Pair<Int, Long>>()
    private val steals = ArrayList<Triple<Int, Int, Long>>()
    private var mainChoice: Int? = null
    private var mainDone = false

    fun start(config: MatchConfig, p: Prefs, f: SoundFx) {
        if (started) return
        started = true
        cfg = config
        prefs = p
        fx = f
        solo = config.bots == 0
        pSeen = p.practiceSeen()
        pWrong = p.wrong()
        players.clear()
        players.add(PlayerState(0, p.name.ifBlank { "Bạn" }, isBot = false, palette = 0))
        val names = listOf("An", "Bình", "Chi", "Dũng", "Hà", "Khoa", "Linh", "Nam").shuffled(rnd)
        repeat(config.bots.coerceIn(0, 3)) { i ->
            players.add(PlayerState(i + 1, names[i], isBot = true, palette = i + 1))
        }
        job = viewModelScope.launch {
            intro(1)
            part1()
            intro(2)
            part2()
            intro(3)
            part3()
            finish()
        }
    }

    /** Dừng trận khi người chơi thoát giữa chừng. */
    fun stop() {
        job?.cancel()
        flush()
        if (::fx.isInitialized) fx.stopVoice()
    }

    // ------------------------------------------------------------------ Người chơi bấm

    fun choose(i: Int) {
        if (phase != Phase.PLAY || reveal || timeLeftMs <= 0L) return
        val now = SystemClock.elapsedRealtime() - questionStart
        when (part) {
            1 -> if (humanChoice == null) humanChoice = i
            2 -> if (humanChoice == null) {
                humanChoice = i
                humanAnswerAt = now
                locked = locked + 0
                fx.play("lock")
            }
            3 -> {
                if (mainDone || humanChoice != null) return
                if (turnId == 0) {
                    humanChoice = i
                    mainChoice = i
                    mainDone = true
                } else if (!solo) {
                    humanChoice = i
                    steals.add(Triple(0, i, now))
                    locked = locked + 0
                    fx.play("lock")
                }
            }
        }
    }

    fun skip() {
        if (part == 1 && phase == Phase.PLAY && humanChoice == null && !reveal) skipFlag = true
    }

    fun canAnswer(): Boolean {
        if (phase != Phase.PLAY || reveal || question == null || timeLeftMs <= 0L) return false
        return when (part) {
            3 -> !mainDone && humanChoice == null && (turnId == 0 || !solo)
            else -> humanChoice == null
        }
    }

    // ------------------------------------------------------------------ Các phần thi

    private suspend fun intro(n: Int) {
        part = n
        phase = Phase.INTRO
        question = null
        reveal = false
        humanChoice = null
        banner = ""
        roundText = ""
        turnId = null
        locked = emptySet()
        timeLeftMs = 0L
        fx.play("chime")
        delay(700)
        fx.say("Chào mừng đến với phần thi ${PART_NAMES[n]}")
        introLeft = 6
        while (introLeft > 0) {
            delay(1000)
            introLeft -= 1
            if (introLeft in 1..3) fx.play("tick")
        }
        phase = Phase.PLAY
    }

    /** Đồng hồ đếm ngược chạy song song, kêu tích tắc mỗi giây. */
    private fun CoroutineScope.timer(totalMs: Long, isDone: () -> Boolean): Job {
        timeTotalMs = totalMs
        timeLeftMs = totalMs
        val start = SystemClock.elapsedRealtime()
        return launch {
            var lastSec = ((totalMs + 999) / 1000).toInt()
            while (timeLeftMs > 0L && !isDone()) {
                delay(50)
                timeLeftMs = (totalMs - (SystemClock.elapsedRealtime() - start)).coerceAtLeast(0L)
                val sec = ((timeLeftMs + 999) / 1000).toInt()
                if (sec != lastSec) {
                    lastSec = sec
                    if (sec in 1..3) fx.play("tick_hi") else if (sec > 0) fx.play("tick")
                }
            }
        }
    }

    private fun tier(q: Question): Int = when {
        q.id !in pSeen -> 0
        q.id in pWrong -> 1
        else -> 2
    }

    private fun pick(count: Int, levels: List<Int>): List<Question> {
        var qs = QuestionBank.pick(count, levels, usedIds) { tier(it) }
        if (qs.size < count) {
            qs = qs + QuestionBank.pick(count - qs.size, listOf(1, 2, 3), usedIds + qs.map { it.id }) { tier(it) }
        }
        usedIds.addAll(qs.map { it.id })
        return qs
    }

    /** Phần 1 – Khởi động: 2 phút, trả lời liên tục, đúng +10, được bỏ qua. */
    private suspend fun part1() = coroutineScope {
        val qs = pick(60, listOf(1, 2))
        qTotal = qs.size
        roundText = "2 phút"
        val clock = timer(120_000L) { false }
        players.filter { it.isBot }.forEach { bot ->
            launch {
                while (timeLeftMs > 0L) {
                    delay(botThinkPart1())
                    if (timeLeftMs <= 0L) break
                    if (rnd.nextFloat() < accuracy(2)) addScore(bot.id, 10)
                }
            }
        }
        var i = 0
        while (timeLeftMs > 0L && i < qs.size) {
            val q = qs[i]
            show(q)
            qNo = i + 1
            humanChoice = null
            reveal = false
            skipFlag = false
            questionStart = SystemClock.elapsedRealtime()
            while (timeLeftMs > 0L && humanChoice == null && !skipFlag) delay(40)
            val c = humanChoice
            if (skipFlag && c == null) result(q, false)
            if (c != null) {
                result(q, c == q.answer)
                reveal = true
                if (c == q.answer) {
                    addScore(0, 10)
                    fx.play("correct")
                } else {
                    fx.play("wrong")
                }
                delay(500)
            }
            i++
        }
        clock.join()
        coroutineContext.cancelChildren()
        reveal = false
        question = null
        fx.stopVoice()
        fx.play("buzz")
        flush()
        banner = "Hết giờ phần Khởi động!"
        delay(1800)
    }

    /** Phần 2 – Đấu Trí: 10 câu, 15 giây; đúng nhanh nhất +30, nhì +20, ba +10. */
    private suspend fun part2() {
        val qs = pick(10, listOf(2, 3))
        qTotal = qs.size
        for ((i, q) in qs.withIndex()) {
            qNo = i + 1
            roundText = "Câu ${i + 1}/${qs.size}"
            humanChoice = null
            humanAnswerAt = -1L
            reveal = false
            locked = emptySet()
            botAnswers.clear()
            banner = "Trả lời đúng càng nhanh, điểm càng cao"
            show(q)
            coroutineScope {
                val start = SystemClock.elapsedRealtime()
                questionStart = start
                val clock = timer(15_000L) { humanChoice != null && botAnswers.size >= cfg.bots }
                players.filter { it.isBot }.forEach { bot ->
                    val at = botAnswerTime(2)
                    val ok = rnd.nextFloat() < accuracy(q.level)
                    val ch = if (ok) q.answer else wrongChoice(q)
                    launch {
                        delay(at)
                        if (timeLeftMs > 0L) {
                            botAnswers[bot.id] = ch to (SystemClock.elapsedRealtime() - start)
                            locked = locked + bot.id
                            fx.play("lock")
                        }
                    }
                }
                clock.join()
                coroutineContext.cancelChildren()
            }
            reveal = true
            fx.stopVoice()
            result(q, humanChoice == q.answer)
            val answers = ArrayList<Triple<Int, Int, Long>>()
            humanChoice?.let { answers.add(Triple(0, it, humanAnswerAt)) }
            botAnswers.forEach { (id, a) -> answers.add(Triple(id, a.first, a.second)) }
            val correct = answers.filter { it.second == q.answer }.sortedBy { it.third }
            val msgs = ArrayList<String>()
            if (solo) {
                correct.firstOrNull()?.let {
                    val pts = when {
                        it.third <= 5_000L -> 30
                        it.third <= 10_000L -> 20
                        else -> 10
                    }
                    addScore(0, pts)
                    msgs.add("Bạn +$pts")
                }
            } else {
                val pts = listOf(30, 20, 10)
                correct.take(3).forEachIndexed { k, a ->
                    addScore(a.first, pts[k])
                    msgs.add("${nameOf(a.first)} +${pts[k]}")
                }
            }
            fx.play(if (humanChoice == q.answer) "correct" else "wrong")
            banner = if (msgs.isEmpty()) "Không ai trả lời đúng" else msgs.joinToString("   ·   ")
            delay(2600)
        }
        question = null
        reveal = false
        flush()
    }

    /** Phần 3 – Về Đích: xoay vòng 5 vòng, 10 giây/câu, người khác được giành quyền. */
    private suspend fun part3() {
        val rounds = 5
        val order = players.map { it.id }
        val qs = pick(rounds * order.size, listOf(3, 2))
        qTotal = qs.size
        var k = 0
        for (r in 1..rounds) {
            for (pid in order) {
                if (k >= qs.size) break
                val q = qs[k]
                k++
                qNo = k
                roundText = "Vòng $r/$rounds"
                turnId = pid
                humanChoice = null
                reveal = false
                locked = emptySet()
                steals.clear()
                mainChoice = null
                mainDone = false
                val isBotTurn = pid != 0
                banner = if (pid == 0) {
                    if (solo) "Lượt của bạn" else "Lượt của bạn · người khác có thể giành quyền"
                } else {
                    "Lượt của ${nameOf(pid)} · bạn có thể chọn trước để giành quyền"
                }
                show(q)
                coroutineScope {
                    val start = SystemClock.elapsedRealtime()
                    questionStart = start
                    val clock = timer(10_000L) { mainDone }
                    if (isBotTurn) {
                        val at = botAnswerTime(3)
                        val ok = rnd.nextFloat() < accuracy(q.level)
                        val ch = if (ok) q.answer else wrongChoice(q)
                        launch {
                            delay(at)
                            if (timeLeftMs > 0L && !mainDone) {
                                mainChoice = ch
                                mainDone = true
                            }
                        }
                    }
                    players.filter { it.isBot && it.id != pid }.forEach { bot ->
                        if (rnd.nextFloat() < stealChance()) {
                            val at = 1_500L + rnd.nextLong(6_500L)
                            val ok = rnd.nextFloat() < accuracy(q.level)
                            val ch = if (ok) q.answer else wrongChoice(q)
                            launch {
                                delay(at)
                                if (timeLeftMs > 0L && !mainDone) {
                                    steals.add(Triple(bot.id, ch, SystemClock.elapsedRealtime() - start))
                                    locked = locked + bot.id
                                    fx.play("lock")
                                }
                            }
                        }
                    }
                    clock.join()
                    coroutineContext.cancelChildren()
                }
                reveal = true
                fx.stopVoice()
                val hc = humanChoice
                if (pid == 0) result(q, hc == q.answer) else if (hc != null) result(q, hc == q.answer)
                val msgs = ArrayList<String>()
                val mc = mainChoice
                var someoneRight = false
                if (mc != null && mc == q.answer) {
                    addScore(pid, 10)
                    msgs.add("${nameOf(pid)} đúng +10")
                    someoneRight = true
                } else {
                    if (mc != null) {
                        addScore(pid, -1)
                        msgs.add("${nameOf(pid)} sai −1")
                    } else {
                        msgs.add("${nameOf(pid)} hết giờ")
                    }
                    for (s in steals.sortedBy { it.third }) {
                        if (s.second == q.answer) {
                            addScore(s.first, 10)
                            msgs.add("${nameOf(s.first)} giành quyền đúng +10")
                            someoneRight = true
                            break
                        } else {
                            addScore(s.first, -1)
                            msgs.add("${nameOf(s.first)} giành quyền sai −1")
                        }
                    }
                }
                fx.play(if (someoneRight) "correct" else "wrong")
                banner = msgs.joinToString("   ·   ")
                delay(2800)
            }
        }
        question = null
        reveal = false
        turnId = null
    }

    private fun finish() {
        flush()
        val max = players.maxOf { it.score }
        winners = players.filter { it.score == max }.map { it.id }.toSet()
        prefs.played = prefs.played + 1
        val me = players.first { it.id == 0 }
        if (solo) {
            if (me.score > prefs.bestSolo) prefs.bestSolo = me.score
            fx.play("win")
            fx.say("Xin chúc mừng! Bạn đã hoàn thành phần thi với ${me.score} điểm")
        } else if (0 in winners) {
            prefs.wins = prefs.wins + 1
            fx.play("win")
            fx.say("Xin chúc mừng ${me.name}, bạn đã chiến thắng!")
        } else {
            fx.play("lose")
            fx.say("Chúc bạn may mắn lần sau!")
        }
        phase = Phase.END
    }

    // ------------------------------------------------------------------ Tiện ích

    /** Hiện câu hỏi mới: ghi nhận đã gặp và đọc to câu hỏi + đáp án. */
    private fun show(q: Question) {
        question = q
        shownNow.add(q.id)
        if (prefs.readQuestions) {
            val o = q.options
            fx.say("${q.text}. A: ${o[0]}. B: ${o[1]}. C: ${o[2]}. D: ${o[3]}.")
        }
    }

    private fun result(q: Question, ok: Boolean) {
        resultsNow[q.id] = ok
    }

    private fun flush() {
        if (!::prefs.isInitialized) return
        prefs.savePractice(shownNow, resultsNow)
        shownNow.clear()
        resultsNow.clear()
    }

    private fun addScore(id: Int, delta: Int) {
        val i = players.indexOfFirst { it.id == id }
        if (i >= 0) players[i] = players[i].copy(score = players[i].score + delta)
    }

    fun nameOf(id: Int): String = if (id == 0) "Bạn" else players.firstOrNull { it.id == id }?.name ?: "?"

    private fun accuracy(level: Int): Float {
        val base = when (cfg.level) {
            0 -> 0.50f
            1 -> 0.68f
            else -> 0.84f
        }
        return (base - 0.08f * (level - 2)).coerceIn(0.2f, 0.95f)
    }

    private fun botThinkPart1(): Long = when (cfg.level) {
        0 -> rnd.nextLong(4_500L, 9_000L)
        1 -> rnd.nextLong(3_200L, 6_500L)
        else -> rnd.nextLong(2_200L, 4_500L)
    }

    private fun botAnswerTime(p: Int): Long = if (p == 2) {
        when (cfg.level) {
            0 -> rnd.nextLong(5_000L, 13_000L)
            1 -> rnd.nextLong(3_000L, 10_000L)
            else -> rnd.nextLong(2_000L, 7_000L)
        }
    } else {
        when (cfg.level) {
            0 -> rnd.nextLong(4_000L, 9_800L)
            1 -> rnd.nextLong(3_000L, 8_000L)
            else -> rnd.nextLong(1_800L, 6_000L)
        }
    }

    private fun stealChance(): Float = when (cfg.level) {
        0 -> 0.20f
        1 -> 0.30f
        else -> 0.40f
    }

    private fun wrongChoice(q: Question): Int = (0..3).filter { it != q.answer }.random(rnd)
}
