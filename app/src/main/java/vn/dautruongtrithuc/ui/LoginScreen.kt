package vn.dautruongtrithuc.ui

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import vn.dautruongtrithuc.data.Net

/** Màn hình đăng nhập bắt buộc khi mở app lần đầu. */
@Composable
fun LoginScreen() {
    val ctx = LocalContext.current
    val activity = ctx as? Activity
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxSize()
            .background(ScreenBrush)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("⚔", color = Gold, fontSize = 64.sp)
        Spacer(Modifier.height(10.dp))
        Text("ĐẤU TRƯỜNG", color = GoldLight, fontSize = 24.sp, fontWeight = FontWeight.Black, letterSpacing = 4.sp)
        Text("TRÍ THỨC", color = Gold, fontSize = 44.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(18.dp))
        Text(
            "Thi kiến thức theo phong cách gameshow với bạn bè và người chơi khắp nơi.",
            color = TextMuted,
            fontSize = 15.sp,
            lineHeight = 22.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(40.dp))
        GoldButton(
            modifier = Modifier.fillMaxWidth().height(58.dp),
            onClick = {
                if (busy || activity == null) return@GoldButton
                busy = true
                scope.launch {
                    val err = Net.signInWithGoogle(activity)
                    busy = false
                    if (err != null) Toast.makeText(ctx, err, Toast.LENGTH_LONG).show()
                }
            },
        ) {
            Text(if (busy) "Đang đăng nhập…" else "G   Đăng nhập bằng Google", color = Ink, fontWeight = FontWeight.Black, fontSize = 17.sp)
        }
        Spacer(Modifier.height(14.dp))
        Text(
            "Điểm xếp hạng, bạn bè và ảnh đại diện được lưu theo tài khoản Google của bạn.",
            color = TextMuted.copy(alpha = 0.75f),
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
        )
    }
}
