package vn.dautruongtrithuc.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import vn.dautruongtrithuc.game.PlayerState

@Composable
fun ResultScreen(
    players: List<PlayerState>,
    winners: Set<Int>,
    solo: Boolean,
    ranked: Boolean = false,
    room: Boolean = false,
    friends: Set<String> = emptySet(),
    onAddFriend: (PlayerState) -> Unit = {},
    onReplay: () -> Unit,
    onHome: () -> Unit,
) {
    val iWon = 0 in winners
    val title = when {
        solo -> "HOÀN THÀNH"
        iWon -> "CHIẾN THẮNG"
        else -> "CHÚC MAY MẮN\nLẦN SAU"
    }
    val sorted = players.sortedByDescending { it.score }
    val me = players.firstOrNull { it.id == 0 }

    Column(
        Modifier
            .fillMaxSize()
            .background(ScreenBrush)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(24.dp))
        Text(if (solo || iWon) "★ ★ ★" else "☆", color = Gold, fontSize = 26.sp)
        Spacer(Modifier.height(8.dp))
        Text(
            title,
            color = Gold,
            fontSize = 36.sp,
            lineHeight = 40.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(10.dp))
        if (me != null) {
            Text(
                if (solo) "Bạn đạt ${me.score} điểm" else "Điểm của bạn: ${me.score}",
                color = TextMain,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Spacer(Modifier.height(24.dp))

        sorted.forEachIndexed { idx, p ->
            val rank = sorted.count { it.score > p.score } + 1
            val win = p.id in winners
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (win) Color(0x33FFD45E) else Color(0x14FFFFFF))
                    .border(1.dp, if (win) GoldMid else Color(0x22FFFFFF), RoundedCornerShape(16.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("$rank", color = if (win) Gold else TextMuted, fontSize = 20.sp, fontWeight = FontWeight.Black,
                    modifier = Modifier.width(28.dp))
                Box {
                    Avatar(
                        p.name, p.palette, size = 44.dp, ring = win,
                        rating = if (p.personaId != null || p.id == 0) "${p.rating}" else "BOT",
                        friend = p.personaId?.let { it in friends },
                        onAddFriend = { onAddFriend(p) },
                        art = p.avatar,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OneLine(if (p.id == 0) "${p.name} (bạn)" else p.name, size = 16, weight = FontWeight.Bold)
                        if (win) {
                            Spacer(Modifier.width(6.dp))
                            Text("♛", color = Gold, fontSize = 16.sp)
                        }
                    }
                    Text(
                        when {
                            p.personaId != null -> p.province.ifBlank { "Người chơi" }
                            p.isBot -> "Máy"
                            else -> "Người chơi"
                        },
                        color = TextMuted,
                        fontSize = 12.sp,
                    )
                }
                p.ratingDelta?.let { d ->
                    Text(
                        if (d >= 0) "+$d" else "$d",
                        color = if (d >= 0) GreenOk else RedBad,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(end = 10.dp),
                    )
                }
                Text("${p.score}", color = if (win) Gold else TextMain, fontSize = 24.sp, fontWeight = FontWeight.Black)
            }
            if (idx == sorted.lastIndex) Spacer(Modifier.height(4.dp))
        }

        Spacer(Modifier.height(14.dp))
        Text(
            if (ranked && me != null) {
                val d = me.ratingDelta ?: 0
                "Điểm xếp hạng: ${me.rating} → ${me.rating + d} (${if (d >= 0) "+$d" else "$d"})"
            } else if (room) "Trận giao hữu không tính điểm xếp hạng."
            else "Trận luyện tập không tính điểm xếp hạng.",
            color = TextMuted,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BlueButton(modifier = Modifier.weight(1f).height(52.dp), onClick = onHome) {
                Text("Về sảnh", color = TextMain, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            GoldButton(modifier = Modifier.weight(1f).height(52.dp), onClick = onReplay) {
                Text(if (ranked) "Trận mới" else if (room) "Phòng mới" else "Chơi lại", color = Ink, fontWeight = FontWeight.Black, fontSize = 16.sp)
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}
