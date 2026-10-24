package vn.dautruongtrithuc.data

import android.content.Context
import org.json.JSONObject

data class Question(
    val id: String,
    val field: String,
    val level: Int,
    val text: String,
    val options: List<String>,
    val answer: Int,
)

class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("dttt", Context.MODE_PRIVATE)

    var name: String
        get() = sp.getString("name", "Bạn") ?: "Bạn"
        set(value) { sp.edit().putString("name", value).apply() }

    var soundOn: Boolean
        get() = sp.getBoolean("sound", true)
        set(value) { sp.edit().putBoolean("sound", value).apply() }

    var voiceOn: Boolean
        get() = sp.getBoolean("voice", true)
        set(value) { sp.edit().putBoolean("voice", value).apply() }

    var played: Int
        get() = sp.getInt("played", 0)
        set(value) { sp.edit().putInt("played", value).apply() }

    var wins: Int
        get() = sp.getInt("wins", 0)
        set(value) { sp.edit().putInt("wins", value).apply() }

    var bestSolo: Int
        get() = sp.getInt("bestSolo", 0)
        set(value) { sp.edit().putInt("bestSolo", value).apply() }

    fun seen(): MutableSet<String> = HashSet(sp.getStringSet("seen", emptySet()) ?: emptySet())

    fun addSeen(ids: Collection<String>) {
        val s = seen()
        s.addAll(ids)
        sp.edit().putStringSet("seen", s).apply()
    }

    fun resetSeen() {
        sp.edit().remove("seen").apply()
    }
}

object QuestionBank {
    var all: List<Question> = emptyList()
        private set

    val fieldCount: Int get() = all.map { it.field }.distinct().size

    fun load(context: Context) {
        if (all.isNotEmpty()) return
        val text = context.assets.open("cau_hoi.json").bufferedReader(Charsets.UTF_8).use { it.readText() }
        val arr = JSONObject(text).getJSONArray("cau_hoi")
        val list = ArrayList<Question>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val opts = listOf(o.getString("A"), o.getString("B"), o.getString("C"), o.getString("D"))
            val ans = "ABCD".indexOf(o.getString("dap_an"))
            if (ans < 0) continue
            list.add(
                Question(
                    id = o.getString("id"),
                    field = o.getString("linh_vuc"),
                    level = o.getInt("do_kho"),
                    text = o.getString("cau_hoi"),
                    options = opts,
                    answer = ans,
                )
            )
        }
        all = list
    }

    /**
     * Chọn câu hỏi trộn đều các lĩnh vực, ưu tiên câu người chơi chưa gặp.
     * Các mức độ khó trong [levels] được ưu tiên theo thứ tự liệt kê.
     */
    fun pick(prefs: Prefs, count: Int, levels: List<Int>, exclude: Set<String> = emptySet()): List<Question> {
        val seen = prefs.seen()
        if (seen.size > all.size * 0.9) prefs.resetSeen()
        val usable = all.filter { it.level in levels && it.id !in exclude }
        val byField = usable.groupBy { it.field }
        val pools = HashMap<String, ArrayDeque<Question>>()
        for ((field, qs) in byField) {
            val ordered = ArrayList<Question>()
            for (lv in levels) {
                val ofLevel = qs.filter { it.level == lv }
                ordered.addAll(ofLevel.filter { it.id !in seen }.shuffled())
            }
            for (lv in levels) {
                val ofLevel = qs.filter { it.level == lv }
                ordered.addAll(ofLevel.filter { it.id in seen }.shuffled())
            }
            pools[field] = ArrayDeque(ordered)
        }
        val order = pools.keys.shuffled()
        val out = ArrayList<Question>()
        var idx = 0
        while (out.size < count && pools.values.any { it.isNotEmpty() }) {
            val deque = pools[order[idx % order.size]]
            idx++
            if (deque != null && deque.isNotEmpty()) out.add(deque.removeFirst())
        }
        prefs.addSeen(out.map { it.id })
        return out.shuffled()
    }
}
