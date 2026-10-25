package vn.dautruongtrithuc.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import vn.dautruongtrithuc.game.PlayerState
import kotlin.math.abs

/** Sân khấu: đèn, màn LED, sàn, khán giả và các bục thí sinh xếp vòng cung. */
@Composable
fun Stage(
    players: List<PlayerState>,
    turnId: Int?,
    locked: Set<Int>,
    timeFraction: Float,
    timeText: String,
    showTimer: Boolean,
    modifier: Modifier = Modifier,
    friends: Set<String> = emptySet(),
    onAddFriend: (PlayerState) -> Unit = {},
) {
    BoxWithConstraints(modifier.clip(RoundedCornerShape(18.dp))) {
        Canvas(Modifier.matchParentSize()) { drawStage() }
        if (showTimer) {
            TimerRing(
                fraction = timeFraction,
                text = timeText,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 10.dp),
            )
        }
        val w = maxWidth
        val h = maxHeight
        val xs = when (players.size) {
            1 -> listOf(0.5f)
            2 -> listOf(0.3f, 0.7f)
            3 -> listOf(0.2f, 0.5f, 0.8f)
            else -> listOf(0.13f, 0.38f, 0.62f, 0.87f)
        }
        val podW = 76.dp
        val podH = 104.dp
        players.forEachIndexed { i, p ->
            val x = xs.getOrElse(i) { 0.5f }
            val edge = abs(x - 0.5f) * 2f
            val baseY = h * 0.66f + (edge * 24f).dp
            Podium(
                player = p,
                isTurn = p.id == turnId,
                isLocked = p.id in locked,
                isFriend = p.personaId?.let { it in friends },
                onAddFriend = { onAddFriend(p) },
                modifier = Modifier
                    .width(podW)
                    .offset(x = w * x - podW / 2, y = baseY - podH),
            )
        }
    }
}

@Composable
private fun Podium(
    player: PlayerState,
    isTurn: Boolean,
    isLocked: Boolean,
    isFriend: Boolean?,
    onAddFriend: () -> Unit,
    modifier: Modifier,
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box {
            Avatar(
                player.name, player.palette, size = 40.dp, ring = isTurn,
                rating = when {
                    player.personaId != null || player.id == 0 -> "${player.rating}"
                    else -> "BOT"
                },
                friend = isFriend,
                onAddFriend = onAddFriend,
            )
            if (isLocked) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 30.dp, y = (-10).dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Navy1)
                        .border(1.dp, Gold, RoundedCornerShape(8.dp))
                        .padding(horizontal = 4.dp, vertical = 1.dp),
                ) {
                    Text("✓ Đã chọn", color = Gold, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                }
            }
        }
        Spacer(Modifier.height(5.dp))
        val faceBrush = if (isTurn) {
            Brush.verticalGradient(listOf(GoldMid, Color(0xFFC98A12), Color(0xFF8A5A06)))
        } else {
            Brush.verticalGradient(listOf(Color(0xFF2A5BC4), Color(0xFF163A8A), Color(0xFF0E275F)))
        }
        val textColor = if (isTurn) Color(0xFF2A1A00) else TextMain
        Box(
            Modifier
                .fillMaxWidth()
                .height(if (isTurn) 54.dp else 48.dp)
                .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp, bottomStart = 4.dp, bottomEnd = 4.dp))
                .background(faceBrush),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text(
                    player.name,
                    color = textColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text("${player.score}", color = textColor, fontSize = 17.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun TimerRing(fraction: Float, text: String, modifier: Modifier) {
    Box(modifier.size(64.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            val sw = 6.dp.toPx()
            drawCircle(Color(0xFF081A44), radius = size.minDimension / 2 - sw / 2)
            drawArc(
                color = Color.White.copy(alpha = 0.14f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(sw / 2, sw / 2),
                size = Size(size.width - sw, size.height - sw),
                style = Stroke(sw),
            )
            drawArc(
                color = if (fraction < 0.3f) RedBad else Gold,
                startAngle = -90f,
                sweepAngle = 360f * fraction.coerceIn(0f, 1f),
                useCenter = false,
                topLeft = Offset(sw / 2, sw / 2),
                size = Size(size.width - sw, size.height - sw),
                style = Stroke(sw, cap = StrokeCap.Round),
            )
        }
        Text(text, color = GoldLight, fontSize = 22.sp, fontWeight = FontWeight.Black)
    }
}

private fun DrawScope.drawStage() {
    val w = size.width
    val h = size.height
    drawRect(
        Brush.radialGradient(
            colors = listOf(Color(0xFF1F4CA6), Color(0xFF0E2763), Color(0xFF071637)),
            center = Offset(w / 2, h * 0.3f),
            radius = w * 0.9f,
        )
    )
    // Màn LED phía sau
    val ledLeft = w * 0.12f
    val ledTop = h * 0.09f
    val ledW = w * 0.76f
    val ledH = h * 0.34f
    drawRoundRect(
        brush = Brush.verticalGradient(listOf(Color(0xFF123274), Color(0xFF0A1E4D)), startY = ledTop, endY = ledTop + ledH),
        topLeft = Offset(ledLeft, ledTop),
        size = Size(ledW, ledH),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(12.dp.toPx()),
    )
    val grid = 12.dp.toPx()
    var gx = ledLeft
    while (gx < ledLeft + ledW) {
        drawLine(SkyLine.copy(alpha = 0.08f), Offset(gx, ledTop), Offset(gx, ledTop + ledH), 1.dp.toPx())
        gx += grid
    }
    var gy = ledTop
    while (gy < ledTop + ledH) {
        drawLine(SkyLine.copy(alpha = 0.08f), Offset(ledLeft, gy), Offset(ledLeft + ledW, gy), 1.dp.toPx())
        gy += grid
    }
    // Sàn sân khấu
    val floorTop = h * 0.60f
    val floorBottom = h * 0.88f
    val floor = Path().apply {
        moveTo(w * 0.08f, floorTop)
        lineTo(w * 0.92f, floorTop)
        lineTo(w, floorBottom)
        lineTo(0f, floorBottom)
        close()
    }
    drawPath(floor, Brush.verticalGradient(listOf(Color(0xFF1A4296), Color(0xFF0D2766)), startY = floorTop, endY = floorBottom))
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(Color(0x66AAD2FF), Color.Transparent),
            center = Offset(w / 2, h * 0.78f),
            radius = w * 0.3f,
        ),
        topLeft = Offset(w * 0.2f, h * 0.70f),
        size = Size(w * 0.6f, h * 0.16f),
    )
    // Đường viền cong ôm các bục
    drawArc(
        color = SkyLine,
        startAngle = 195f,
        sweepAngle = 150f,
        useCenter = false,
        topLeft = Offset(w * 0.04f, h * 0.58f),
        size = Size(w * 0.92f, h * 0.42f),
        style = Stroke(3.dp.toPx()),
    )
    // Mép sân khấu phát sáng
    drawLine(GoldMid, Offset(0f, floorBottom), Offset(w, floorBottom), 3.dp.toPx())
    // Chùm đèn rọi
    val beams = listOf(
        Triple(w * 0.12f, w * 0.22f, Color(0x73FFD66E)),
        Triple(w * 0.5f, w * 0.5f, Color(0x40B4D7FF)),
        Triple(w * 0.88f, w * 0.78f, Color(0x40B4D7FF)),
    )
    for ((top, bottom, c) in beams) {
        val beam = Path().apply {
            moveTo(top - 6.dp.toPx(), 0f)
            lineTo(top + 6.dp.toPx(), 0f)
            lineTo(bottom + 55.dp.toPx(), floorBottom)
            lineTo(bottom - 55.dp.toPx(), floorBottom)
            close()
        }
        drawPath(beam, Brush.verticalGradient(listOf(c, Color.Transparent), startY = 0f, endY = floorBottom))
    }
    // Dàn đèn
    val lamps = 7
    for (i in 0 until lamps) {
        val cx = w * (0.1f + 0.8f * i / (lamps - 1))
        val c = if (i % 2 == 0) Color(0xFFFFF3C4) else Color(0xFFD6EBFF)
        drawCircle(c.copy(alpha = 0.3f), radius = 9.dp.toPx(), center = Offset(cx, 9.dp.toPx()))
        drawCircle(c, radius = 4.dp.toPx(), center = Offset(cx, 9.dp.toPx()))
    }
    // Khán giả
    val back = Color(0xFF0B1B3E)
    val front = Color(0xFF02081A)
    drawRect(back, topLeft = Offset(0f, h * 0.94f), size = Size(w, h * 0.06f))
    val step = 30.dp.toPx()
    var x = step / 2
    while (x < w + step) {
        drawCircle(back, radius = 12.dp.toPx(), center = Offset(x, h * 0.935f))
        drawCircle(front, radius = 14.dp.toPx(), center = Offset(x - step / 2, h * 0.985f))
        x += step
    }
}
