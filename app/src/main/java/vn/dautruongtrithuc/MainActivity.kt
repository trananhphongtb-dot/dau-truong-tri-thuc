package vn.dautruongtrithuc

import android.content.Intent
import android.os.Bundle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.font.FontWeight
import vn.dautruongtrithuc.ui.Gold
import vn.dautruongtrithuc.ui.Navy1
import vn.dautruongtrithuc.ui.TextMuted
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import vn.dautruongtrithuc.audio.SoundFx
import vn.dautruongtrithuc.data.Prefs
import vn.dautruongtrithuc.data.QuestionBank
import vn.dautruongtrithuc.game.MatchConfig
import vn.dautruongtrithuc.ui.AppTheme
import vn.dautruongtrithuc.ui.HomeScreen
import vn.dautruongtrithuc.ui.MatchScreen

/** Link mời vào phòng: mở trang web, trang này chuyển sang app (hoặc Google Play nếu chưa cài). */
fun inviteLink(code: String) = "https://trananhphongtb-dot.github.io/dau-truong-tri-thuc/join/?r=$code"

class MainActivity : ComponentActivity() {

    private var joinCode by mutableStateOf<String?>(null)

    private fun readJoin(i: Intent?) {
        val d = i?.data ?: return
        val code = d.getQueryParameter("r") ?: return
        if (code.all { it.isDigit() } && code.length in 4..8) joinCode = code
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        readJoin(intent)
    }


    private lateinit var prefs: Prefs
    private lateinit var fx: SoundFx

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        prefs = Prefs(this)
        QuestionBank.load(this)
        fx = SoundFx(this, prefs)
        readJoin(intent)

        setContent {
            AppTheme {
                var config by remember { mutableStateOf<MatchConfig?>(null) }
                var matchKey by remember { mutableIntStateOf(0) }
                val current = config
                val jc = joinCode
                if (jc != null) {
                    AlertDialog(
                        onDismissRequest = { joinCode = null },
                        containerColor = Navy1,
                        title = { Text("Vào phòng #$jc", color = Gold, fontWeight = FontWeight.Black) },
                        text = {
                            Text(
                                "Không vào được phòng này. Bạn có thể tự tạo phòng mới và mời bạn bè.",
                                color = TextMuted,
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = { joinCode = null; matchKey++; config = MatchConfig(0, 1, room = true) }) {
                                Text("Tạo phòng", color = Gold, fontWeight = FontWeight.Black)
                            }
                        },
                        dismissButton = { TextButton(onClick = { joinCode = null }) { Text("Đóng", color = TextMuted) } },
                    )
                }
                if (current == null) {
                    HomeScreen(prefs, fx) {
                        matchKey++
                        config = it
                    }
                } else {
                    BackHandler { config = null }
                    MatchScreen(
                        config = current,
                        key = matchKey,
                        prefs = prefs,
                        fx = fx,
                        onReplay = { matchKey++ },
                        onHome = { config = null },
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::fx.isInitialized) fx.recheck()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!isChangingConfigurations) fx.release()
    }
}
