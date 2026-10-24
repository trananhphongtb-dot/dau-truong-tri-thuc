package vn.dautruongtrithuc.ui

import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import vn.dautruongtrithuc.audio.SoundFx
import vn.dautruongtrithuc.data.Prefs
import vn.dautruongtrithuc.game.MatchConfig
import vn.dautruongtrithuc.game.MatchViewModel
import vn.dautruongtrithuc.game.PART_NAMES
import vn.dautruongtrithuc.game.Phase

val FIELD_NAMES = mapOf(
    "TOAN" to "Toán", "LY" to "Vật lý", "HOA" to "Hóa học", "SINH" to "Sinh học",
    "VAN" to "Văn học", "SU" to "Lịch sử", "DIA" to "Địa lý", "ANH" to "Tiếng Anh",
    "TIN" to "Tin học", "DN" to "Danh nhân", "SK" to "Sự kiện", "TT" to "Thường thức",
)

@Composable
fun MatchScreen(
    config: MatchConfig,
    key: Int,
    prefs: Prefs,
    fx: SoundFx,
    onReplay: () -> Unit,
    onHome: () -> Unit,
) {
    val vm: MatchViewModel = viewModel(key = "match$key")
    val activity = LocalContext.current as? ComponentActivity
    LaunchedEffect(vm) { vm.start(config, prefs, fx) }
    DisposableEffect(vm) {
        onDispose { if (activity?.isChangingConfigurations != true) vm.stop() }
    }

    var askExit by remember { mutableStateOf(false) }
    BackHandler(enabled = vm.phase != Phase.END) { askExit = true }

    if (vm.phase == Phase.END) {
        ResultScreen(
            players = vm.players.toList(),
            winners = vm.winners,
            solo = vm.solo,
            onReplay = onReplay,
            onHome = onHome,
        )
        return
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(ScreenBrush)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Column(Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
            Header(vm, onClose = { askExit = true })
            Spacer(Modifier.height(6.dp))
            val secs = ((vm.timeLeftMs + 999) / 1000).toInt()
            Stage(
                players = vm.players.toList(),
                turnId = vm.turnId,
                locked = vm.locked,
                timeFraction = vm.timeLeftMs.toFloat() / vm.timeTotalMs.coerceAtLeast(1L),
                timeText = if (vm.part == 1 && secs >= 60) "%d:%02d".format(secs / 60, secs % 60) else "$secs",
                showTimer = vm.phase == Phase.PLAY && vm.timeTotalMs > 1L,
                modifier = Modifier.fillMaxWidth().height(250.dp),
            )
            Box(Modifier.fillMaxWidth().heightIn(min = 34.dp).padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
                if (vm.banner.isNotEmpty()) {
                    Text(
                        vm.banner,
                        color = GoldLight,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                    )
                }
            }
            val q = vm.question
            if (q != null) {
                QuestionCard(
                    no = vm.qNo,
                    total = vm.qTotal,
                    field = FIELD_NAMES[q.field] ?: q.field,
                    text = q.text,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                )
                Spacer(Modifier.height(10.dp))
                val enabled = vm.canAnswer()
                AnswerGrid(
                    options = q.options,
                    answer = q.answer,
                    chosen = vm.humanChoice,
                    reveal = vm.reveal,
                    enabled = enabled,
                    onChoose = { vm.choose(it) },
                )
                if (vm.part == 1) {
                    Spacer(Modifier.height(8.dp))
                    BlueButton(
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        onClick = { vm.skip() },
                    ) {
                        Text("Bỏ qua  »", color = TextMain, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
                Spacer(Modifier.height(10.dp))
            } else {
                Spacer(Modifier.weight(1f))
            }
        }

        AnimatedVisibility(
            visible = vm.phase == Phase.INTRO && vm.part > 0,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            IntroOverlay(part = vm.part, left = vm.introLeft, solo = vm.solo)
        }
    }

    if (askExit) {
        AlertDialog(
            onDismissRequest = { askExit = false },
            containerColor = Navy1,
            title = { Text("Rời trận?", color = TextMain, fontWeight = FontWeight.Bold) },
            text = { Text("Kết quả trận này sẽ không được lưu.", color = TextMuted) },
            confirmButton = {
                TextButton(onClick = {
                    askExit = false
                    vm.stop()
                    onHome()
                }) { Text("Rời trận", color = RedBad, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { askExit = false }) { Text("Thi tiếp", color = Gold, fontWeight = FontWeight.Bold) }
            },
        )
    }
}

@Composable
private fun Header(vm: MatchViewModel, onClose: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color(0x22FFFFFF))
                .clickable(onClick = onClose),
            contentAlignment = Alignment.Center,
        ) {
            Text("✕", color = TextMain, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(10.dp))
        if (vm.part > 0) {
            Box(
                Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(GoldBrush)
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            ) {
                Text("PHẦN ${vm.part}", color = Ink, fontSize = 11.sp, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.width(8.dp))
            Text(
                PART_NAMES[vm.part].uppercase(),
                color = Gold,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )
        } else {
            Spacer(Modifier.weight(1f))
        }
        if (vm.roundText.isNotEmpty()) {
            Text(vm.roundText, color = TextMuted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun QuestionCard(no: Int, total: Int, field: String, text: String, modifier: Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFF16357A), Color(0xFF0C2257))))
            .border(1.5.dp, Color(0x80FFD45E), RoundedCornerShape(18.dp))
            .padding(14.dp),
    ) {
        Column(Modifier.fillMaxSize()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(GoldBrush)
                        .padding(horizontal = 10.dp, vertical = 2.dp),
                ) {
                    Text("Câu $no / $total", color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Black)
                }
                Spacer(Modifier.weight(1f))
                Box(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .border(1.dp, Color(0x558CC3FF), RoundedCornerShape(50))
                        .padding(horizontal = 10.dp, vertical = 2.dp),
                ) {
                    Text(field, color = SkyLine, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.height(10.dp))
            Box(
                Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text,
                    color = TextMain,
                    fontSize = if (text.length > 160) 15.sp else if (text.length > 90) 17.sp else 19.sp,
                    lineHeight = if (text.length > 160) 21.sp else 25.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun AnswerGrid(
    options: List<String>,
    answer: Int,
    chosen: Int?,
    reveal: Boolean,
    enabled: Boolean,
    onChoose: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (row in 0..1) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (col in 0..1) {
                    val i = row * 2 + col
                    AnswerButton(
                        letter = "ABCD"[i].toString(),
                        text = options[i],
                        state = when {
                            reveal && i == answer -> AnsState.RIGHT
                            reveal && i == chosen -> AnsState.WRONG
                            i == chosen -> AnsState.PICKED
                            reveal -> AnsState.DIM
                            else -> AnsState.NORMAL
                        },
                        enabled = enabled,
                        onClick = { onChoose(i) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

private enum class AnsState { NORMAL, PICKED, RIGHT, WRONG, DIM }

@Composable
private fun AnswerButton(
    letter: String,
    text: String,
    state: AnsState,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    val bg: Brush = when (state) {
        AnsState.RIGHT -> Brush.verticalGradient(listOf(Color(0xFF34D399), Color(0xFF15803D)))
        AnsState.WRONG -> Brush.verticalGradient(listOf(Color(0xFFFB7185), Color(0xFFBE123C)))
        AnsState.PICKED -> GoldBrush
        AnsState.DIM -> Brush.verticalGradient(listOf(Color(0xFF15295A), Color(0xFF0E1E46)))
        AnsState.NORMAL -> Brush.verticalGradient(listOf(Color(0xFF1E4590), Color(0xFF122C66)))
    }
    val fg = when (state) {
        AnsState.PICKED -> Ink
        AnsState.DIM -> TextMuted.copy(alpha = 0.6f)
        else -> TextMain
    }
    val border = when (state) {
        AnsState.NORMAL -> Color(0x668CB4FF)
        AnsState.DIM -> Color(0x22FFFFFF)
        else -> Color(0x99FFFFFF)
    }
    Row(
        modifier
            .heightIn(min = 58.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(14.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(if (state == AnsState.PICKED) Color(0x33000000) else Color(0x33FFFFFF)),
            contentAlignment = Alignment.Center,
        ) {
            Text(letter, color = if (state == AnsState.PICKED) Ink else GoldLight, fontWeight = FontWeight.Black, fontSize = 13.sp)
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text,
            color = fg,
            fontSize = if (text.length > 40) 12.sp else 14.sp,
            lineHeight = 17.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 3,
        )
    }
}

@Composable
private fun IntroOverlay(part: Int, left: Int, solo: Boolean) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xE6040A1E))
            .clickable(enabled = false) {},
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier.padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (n in 1..3) {
                    Box(
                        Modifier
                            .width(46.dp)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (n <= part) Gold else Color(0x33FFFFFF)),
                    )
                }
            }
            Spacer(Modifier.height(22.dp))
            Text("PHẦN THI THỨ ${part}", color = TextMuted, fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
            Spacer(Modifier.height(6.dp))
            Text(
                PART_NAMES[part].uppercase(),
                color = Gold,
                fontSize = 44.sp,
                lineHeight = 48.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(18.dp))
            Text(
                rulesFor(part, solo),
                color = TextMain,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(28.dp))
            Box(
                Modifier
                    .size(84.dp)
                    .clip(CircleShape)
                    .background(GoldBrush),
                contentAlignment = Alignment.Center,
            ) {
                Text(if (left > 0) "$left" else "▶", color = Ink, fontSize = 36.sp, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.height(8.dp))
            Text("Bắt đầu sau", color = TextMuted, fontSize = 13.sp)
        }
    }
}

private fun rulesFor(part: Int, solo: Boolean): String = when (part) {
    1 -> "2 phút trả lời liên tục.\nĐúng +10 điểm · được bỏ qua · sai không bị trừ."
    2 -> if (solo) {
        "10 câu, mỗi câu 15 giây.\nĐúng trong 5 giây +30 · trong 10 giây +20 · sau đó +10."
    } else {
        "10 câu, mỗi câu 15 giây.\nĐúng nhanh nhất +30 · nhì +20 · ba +10.\nSai không bị trừ."
    }
    else -> if (solo) {
        "5 câu, mỗi câu 10 giây.\nĐúng +10 · sai −1."
    } else {
        "Mỗi người lần lượt 1 câu, 5 vòng, 10 giây/câu.\nĐúng +10 · sai −1.\nNgười khác có thể chọn trước để giành quyền khi người chính trả lời sai."
    }
}
