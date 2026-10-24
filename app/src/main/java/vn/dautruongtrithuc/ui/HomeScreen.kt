package vn.dautruongtrithuc.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import vn.dautruongtrithuc.audio.SoundFx
import vn.dautruongtrithuc.data.Prefs
import vn.dautruongtrithuc.data.QuestionBank
import vn.dautruongtrithuc.game.MatchConfig

@Composable
fun HomeScreen(prefs: Prefs, fx: SoundFx, onStart: (MatchConfig) -> Unit) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var settings by rememberSaveable { mutableStateOf(false) }
    // Tăng mỗi khi đổi tên để thanh trên cùng vẽ lại
    var nameVersion by remember { mutableIntStateOf(0) }

    if (settings) {
        BackHandler { settings = false }
        SettingsScreen(prefs, fx, onBack = { settings = false })
        return
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(ScreenBrush)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        TopBar(prefs, nameVersion)
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when (tab) {
                0 -> PlayTab(onStart)
                1 -> RankTab()
                else -> MeTab(prefs, onOpenSettings = { settings = true }, onNameChanged = { nameVersion++ })
            }
        }
        BottomTabs(tab) { tab = it }
    }
}

@Composable
private fun TopBar(prefs: Prefs, nameVersion: Int) {
    Row(
        Modifier.fillMaxWidth().padding(start = 16.dp, end = 26.dp, top = 14.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text("ĐẤU TRƯỜNG", color = GoldLight, fontSize = 20.sp, fontWeight = FontWeight.Black, letterSpacing = 4.sp)
            Text("TRÍ THỨC", color = Gold, fontSize = 36.sp, lineHeight = 38.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
        }
        // nameVersion chỉ để buộc vẽ lại khi đổi tên
        Avatar(if (nameVersion >= 0) prefs.name else "", 0, size = 50.dp, ring = true, rating = "100")
    }
}

@Composable
private fun BottomTabs(selected: Int, onSelect: (Int) -> Unit) {
    val items = listOf("⚔" to "Thi đấu", "♛" to "Xếp hạng", "☺" to "Tôi")
    Row(
        Modifier
            .fillMaxWidth()
            .background(Color(0xF0071435))
            .border(1.dp, Color(0x1AFFFFFF))
            .padding(vertical = 6.dp),
    ) {
        items.forEachIndexed { i, (icon, label) ->
            val on = i == selected
            Column(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onSelect(i) }
                    .padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(icon, color = if (on) Gold else TextMuted, fontSize = 20.sp)
                Text(label, color = if (on) Gold else TextMuted, fontSize = 12.sp, fontWeight = if (on) FontWeight.Bold else FontWeight.Normal)
            }
        }
    }
}

@Composable
private fun TabColumn(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}

// ============================================================ Tab Thi đấu

@Composable
private fun PlayTab(onStart: (MatchConfig) -> Unit) {
    val ctx = LocalContext.current
    var botDialog by remember { mutableStateOf(false) }
    val soon = { Toast.makeText(ctx, "Cần đăng nhập Google · tính năng sắp có", Toast.LENGTH_SHORT).show() }

    TabColumn {
        GoldButton(modifier = Modifier.fillMaxWidth().height(84.dp), onClick = soon) {
            Row(Modifier.padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("⚡", fontSize = 30.sp, color = Ink)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("Thi ngẫu nhiên", color = Ink, fontSize = 20.sp, fontWeight = FontWeight.Black)
                    Text("Ghép với người chơi cùng trình độ", color = Ink.copy(alpha = 0.75f), fontSize = 12.sp)
                }
                Text("»", color = Ink, fontSize = 26.sp, fontWeight = FontWeight.Black)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MenuTile("＋", "Tạo phòng", "Mời bạn bè", Modifier.weight(1f), onClick = soon)
            MenuTile("✎", "Luyện tập", "Thi một mình", Modifier.weight(1f)) { onStart(MatchConfig(0, 1)) }
        }
        MenuTile("◎", "Luyện với máy", "1–3 đối thủ máy · chọn độ khó", Modifier.fillMaxWidth()) { botDialog = true }

        Text("PHÒNG ĐANG CHỜ", color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        GlassCard(Modifier.fillMaxWidth()) {
            Text(
                "Thi đấu trực tuyến sẽ mở khi có đăng nhập Google.\nTrong lúc chờ, mình luyện tập trước nhé!",
                color = TextMuted,
                fontSize = 13.sp,
                lineHeight = 19.sp,
            )
        }
        Spacer(Modifier.height(8.dp))
    }

    if (botDialog) {
        BotDialog(onDismiss = { botDialog = false }) { bots, level ->
            botDialog = false
            onStart(MatchConfig(bots, level))
        }
    }
}

@Composable
private fun MenuTile(icon: String, title: String, sub: String, modifier: Modifier, onClick: () -> Unit) {
    BlueButton(modifier = modifier.height(84.dp), onClick = onClick) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(Color(0x26FFD45E)),
                contentAlignment = Alignment.Center,
            ) { Text(icon, color = Gold, fontSize = 20.sp, fontWeight = FontWeight.Bold) }
            Spacer(Modifier.width(10.dp))
            Column {
                OneLine(title, size = 16, weight = FontWeight.Bold)
                OneLine(sub, color = TextMuted, size = 12)
            }
        }
    }
}

@Composable
private fun BotDialog(onDismiss: () -> Unit, onGo: (Int, Int) -> Unit) {
    var bots by remember { mutableIntStateOf(3) }
    var level by remember { mutableIntStateOf(1) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Navy1,
        title = { Text("Luyện với máy", color = Gold, fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Số đối thủ", color = TextMuted, fontSize = 13.sp)
                ChoiceRow(listOf("1", "2", "3"), bots - 1) { bots = it + 1 }
                Text("Độ khó", color = TextMuted, fontSize = 13.sp)
                ChoiceRow(listOf("Dễ", "Vừa", "Khó"), level) { level = it }
            }
        },
        confirmButton = {
            TextButton(onClick = { onGo(bots, level) }) { Text("Bắt đầu", color = Gold, fontWeight = FontWeight.Black) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy", color = TextMuted) }
        },
    )
}

@Composable
private fun ChoiceRow(items: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEachIndexed { i, s ->
            val on = i == selected
            Box(
                Modifier
                    .weight(1f)
                    .height(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (on) GoldBrush else BlueBtnBrush)
                    .clickable { onSelect(i) },
                contentAlignment = Alignment.Center,
            ) {
                Text(s, color = if (on) Ink else TextMain, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ============================================================ Tab Xếp hạng

@Composable
private fun RankTab() {
    var period by rememberSaveable { mutableIntStateOf(0) }
    TabColumn {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Tất cả", "Tuần", "Tháng", "Năm").forEachIndexed { i, s ->
                Box(
                    Modifier
                        .weight(1f)
                        .height(36.dp)
                        .clip(RoundedCornerShape(50))
                        .background(if (i == period) GoldBrush else BlueBtnBrush)
                        .clickable { period = i },
                    contentAlignment = Alignment.Center,
                ) { Text(s, color = if (i == period) Ink else TextMuted, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
            }
        }
        GlassCard(Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("♛", color = Gold, fontSize = 40.sp)
                Spacer(Modifier.height(6.dp))
                Text("Bảng xếp hạng", color = TextMain, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Xếp hạng sẽ hiển thị khi có đăng nhập Google và thi đấu trực tuyến.\nMỗi người khởi đầu với 100 điểm.\nBấm dấu + trên ảnh người chơi để kết bạn.",
                    color = TextMuted,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

// ============================================================ Tab Tôi

@Composable
private fun MeTab(prefs: Prefs, onOpenSettings: () -> Unit, onNameChanged: () -> Unit) {
    var name by remember { mutableStateOf(prefs.name) }
    var editing by remember { mutableStateOf(false) }
    var friends by remember { mutableStateOf(prefs.friends()) }
    var removing by remember { mutableStateOf<String?>(null) }

    TabColumn {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Hồ sơ", color = TextMain, fontSize = 20.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
            Box(
                Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color(0x22FFFFFF))
                    .clickable(onClick = onOpenSettings),
                contentAlignment = Alignment.Center,
            ) { Text("⚙", color = Gold, fontSize = 22.sp) }
        }
        GlassCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(name.ifBlank { "B" }, 0, size = 64.dp, ring = true, rating = "100")
                Spacer(Modifier.width(22.dp))
                if (editing) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it.take(16) },
                        label = { Text("Tên hiển thị") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Gold,
                            unfocusedBorderColor = Color(0x558CB4FF),
                            focusedLabelColor = Gold,
                            unfocusedLabelColor = TextMuted,
                            focusedTextColor = TextMain,
                            unfocusedTextColor = TextMain,
                            cursorColor = Gold,
                        ),
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = {
                        name = name.trim().ifBlank { "Bạn" }
                        prefs.name = name
                        editing = false
                        onNameChanged()
                    }) { Text("Lưu", color = Gold, fontWeight = FontWeight.Black) }
                } else {
                    Column(Modifier.weight(1f)) {
                        OneLine(name, size = 20, weight = FontWeight.Black)
                        Text("Khách · chưa đăng nhập", color = TextMuted, fontSize = 12.sp)
                    }
                    TextButton(onClick = { editing = true }) { Text("✎ Sửa", color = Gold, fontWeight = FontWeight.Bold) }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Stat("Điểm xếp hạng", "100", Modifier.weight(1f))
            Stat("Hạng", "—", Modifier.weight(1f))
            Stat("Thắng / Trận", "${prefs.wins}/${prefs.played}", Modifier.weight(1f))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("BẠN BÈ", color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp,
                modifier = Modifier.weight(1f))
            Text("${friends.size}", color = Gold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        if (friends.isEmpty()) {
            GlassCard(Modifier.fillMaxWidth()) {
                Text(
                    "Chưa có bạn bè.\nKhi thi trực tuyến, bấm dấu + ở góc ảnh người chơi là thêm bạn ngay.",
                    color = TextMuted,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                )
            }
        } else {
            friends.forEachIndexed { i, f ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0x14FFFFFF))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Avatar(f, i + 1, size = 40.dp, friend = true)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        OneLine(f, size = 15, weight = FontWeight.Bold)
                        Text("Ngoại tuyến", color = TextMuted, fontSize = 12.sp)
                    }
                    TextButton(onClick = { removing = f }) { Text("Bỏ", color = TextMuted) }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }

    val r = removing
    if (r != null) {
        AlertDialog(
            onDismissRequest = { removing = null },
            containerColor = Navy1,
            title = { Text("Bỏ kết bạn?", color = TextMain, fontWeight = FontWeight.Bold) },
            text = { Text("Xóa $r khỏi danh sách bạn bè.", color = TextMuted) },
            confirmButton = {
                TextButton(onClick = {
                    prefs.removeFriend(r)
                    friends = prefs.friends()
                    removing = null
                }) { Text("Bỏ", color = RedBad, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { removing = null }) { Text("Thôi", color = Gold) } },
        )
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier) {
    GlassCard(modifier) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, color = Gold, fontSize = 22.sp, fontWeight = FontWeight.Black, maxLines = 1)
            Text(label, color = TextMuted, fontSize = 11.sp, maxLines = 1)
        }
    }
}

// ============================================================ Màn Cài đặt

@Composable
private fun SettingsScreen(prefs: Prefs, fx: SoundFx, onBack: () -> Unit) {
    val ctx = LocalContext.current
    var sound by remember { mutableStateOf(prefs.soundOn) }
    var voice by remember { mutableStateOf(prefs.voiceOn) }
    var readQ by remember { mutableStateOf(prefs.readQuestions) }
    var seen by remember { mutableIntStateOf(prefs.practiceSeen().size) }
    var wrongN by remember { mutableIntStateOf(prefs.wrong().size) }

    Column(
        Modifier
            .fillMaxSize()
            .background(ScreenBrush)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0x22FFFFFF))
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) { Text("‹", color = TextMain, fontSize = 26.sp, fontWeight = FontWeight.Bold) }
            Spacer(Modifier.width(12.dp))
            Text("Cài đặt", color = Gold, fontSize = 24.sp, fontWeight = FontWeight.Black)
        }

        Text("ÂM THANH", color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        GlassCard(Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SettingRow("Âm thanh hiệu ứng", sound) { sound = it; prefs.soundOn = it }
                SettingRow("Giọng đọc chào mừng", voice) { voice = it; prefs.voiceOn = it }
                SettingRow("Đọc câu hỏi và đáp án", readQ) { readQ = it; prefs.readQuestions = it }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        (if (fx.voiceOk) "✓ " else "⚠ ") + fx.voiceStatus,
                        color = if (fx.voiceOk) GreenOk else GoldLight,
                        fontSize = 13.sp,
                        modifier = Modifier.weight(1f),
                    )
                    if (fx.voiceOk) {
                        TextButton(onClick = { fx.say("Chào mừng bạn đến với Đấu Trường Trí Thức", force = true) }) {
                            Text("Nghe thử", color = Gold, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        TextButton(onClick = { fx.openVoiceSetup(ctx) }) {
                            Text("Cài giọng", color = Gold, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                if (!fx.voiceOk) {
                    Text(
                        "Cài \"Dịch vụ lời nói của Google\" và tải gói tiếng Việt, rồi chọn nó làm bộ đọc mặc định. Quay lại app sẽ tự nhận.",
                        color = TextMuted,
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                    )
                }
            }
        }

        Text("CÂU HỎI", color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        GlassCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Câu đã gặp khi luyện tập", color = TextMain, fontWeight = FontWeight.SemiBold)
                    Text("$seen / ${QuestionBank.all.size} · cần ôn lại: $wrongN", color = TextMuted, fontSize = 13.sp)
                }
                TextButton(onClick = {
                    prefs.resetPractice()
                    seen = 0
                    wrongN = 0
                    Toast.makeText(ctx, "Đã làm mới danh sách câu hỏi", Toast.LENGTH_SHORT).show()
                }) { Text("Làm mới", color = Gold, fontWeight = FontWeight.Bold) }
            }
        }

        Spacer(Modifier.height(10.dp))
        Text(
            "Phiên bản ${appVersion(ctx)} · bản chơi thử ngoại tuyến",
            color = TextMuted.copy(alpha = 0.6f),
            fontSize = 12.sp,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
    }
}

private fun appVersion(ctx: android.content.Context): String = try {
    ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: ""
} catch (e: Exception) {
    ""
}

@Composable
private fun SettingRow(label: String, on: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = TextMain, modifier = Modifier.weight(1f))
        Switch(
            checked = on,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Ink,
                checkedTrackColor = Gold,
                uncheckedThumbColor = TextMuted,
                uncheckedTrackColor = Navy2,
            ),
        )
    }
}
