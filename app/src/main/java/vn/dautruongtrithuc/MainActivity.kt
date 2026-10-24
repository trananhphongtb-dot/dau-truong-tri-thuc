package vn.dautruongtrithuc

import android.os.Bundle
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

class MainActivity : ComponentActivity() {

    private lateinit var prefs: Prefs
    private lateinit var fx: SoundFx

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        prefs = Prefs(this)
        QuestionBank.load(this)
        fx = SoundFx(this, prefs)

        setContent {
            AppTheme {
                var config by remember { mutableStateOf<MatchConfig?>(null) }
                var matchKey by remember { mutableIntStateOf(0) }
                val current = config
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
