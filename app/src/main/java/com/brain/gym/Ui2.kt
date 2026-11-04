package com.brain.gym

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

fun fmt(s: Long) = if (s >= 3600) "${s / 3600}h ${"%02d".format((s % 3600) / 60)}m" else "${s / 60}:${"%02d".format(s % 60)}"

@Composable fun Sp(h: Int) = Spacer(Modifier.height(h.dp))

@Composable fun Chip(t: String, sel: Boolean, ac: Color, m: Modifier = Modifier, small: Boolean = false, f: () -> Unit) =
    Button({ Sfx.tick(); f() }, m, shape = RoundedCornerShape(12.dp), contentPadding = PaddingValues(horizontal = if (small) 2.dp else 10.dp),
        colors = ButtonDefaults.buttonColors(containerColor = if (sel) ac else Color(0xFF1C1C20), contentColor = if (sel) Color.Black else Color.White)) {
        Text(t, fontSize = if (small) 12.sp else 13.sp, maxLines = 1)
    }

@Composable fun Big(t: String, ac: Color, f: () -> Unit) =
    Button({ Sfx.tick(); f() }, Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = ac, contentColor = Color.Black)) { Text(t, fontWeight = FontWeight.Bold) }

@Composable fun Tile(x: G, ac: Color, st: Store, m: Modifier, on: () -> Unit) {
    val b = if (x.asmr) null else st.hist(x.id).map { it[3].toDouble() }.let { if (x.lower) it.minOrNull() else it.maxOrNull() }
    Box(m.aspectRatio(1f).clip(RoundedCornerShape(20.dp))
        .background(Brush.verticalGradient(listOf(Color(0xFF26262B), Color(0xFF111114))))
        .border(1.dp, ac.copy(alpha = 0.35f), RoundedCornerShape(20.dp)).clickable { Sfx.tick(); on() }.padding(6.dp), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(42.dp).clip(CircleShape).background(ac.copy(.16f)), Alignment.Center) { Icon(icon(x.id), null, tint = ac, modifier = Modifier.size(25.dp)) }
            Spacer(Modifier.height(5.dp))
            Text(x.name, fontSize = 11.sp, textAlign = TextAlign.Center, maxLines = 2, lineHeight = 13.sp)
            if (b != null) Text("★ ${"%.0f".format(b)}", color = ac, fontSize = 11.sp)
        }
    }
}

@Composable fun Play(c: G, m: String, d: Int, f: (Double) -> Unit) {
    when (c.id) {
        "num" -> NumberMem(m, d, f); "seq" -> SeqMem2(m, d, f); "chimp" -> Chimp2(d, f); "pairs" -> Pairs(m, d, f)
        "nback" -> NBack2(d, f); "matrix" -> Matrix2(d, f); "words" -> Words(d, f); "dual" -> Dual(d, f); "corsi" -> Corsi2(d, f)
        "rev" -> RevSpan(d, f); "flash" -> Flash(m, d, f); "react" -> Reaction2(m, d, f); "gonogo" -> GoNoGo2(d, f)
        "stroop" -> Stroop(d, f); "schulte" -> Schulte(d, f); "cmatch" -> ColorMatch(d, f); "whack" -> Whack2(d, f)
        "odd" -> Odd2(d, f); "flank" -> Flanker(d, f); "g2048" -> G2048(d, f); "lights" -> Lights(d, f)
        "sudoku" -> Sudoku(d, f); "mines" -> Mines2(d, f); "chess" -> ChessBoard(m, d, f)
        "ttt" -> TicTacToe(m, d, f); "c4" -> ConnectFour(m, d, f)
        "flappy" -> FlappyBall(d, f); "snake" -> Snake(m, d, f); "stack" -> StackTower2(d, f); "dodge" -> Dodge(d, f); "tiles" -> TapTiles(d, f)
        "balloon" -> BalloonPop(d, f)
        else -> Asmr(c.id)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable fun AppV2(st: Store) {
    var tab by remember { mutableStateOf(0) }
    var g by remember { mutableStateOf<G?>(null) }
    var step by remember { mutableStateOf(0) }
    var mode by remember { mutableStateOf("") }
    var diff by remember { mutableStateOf(1) }
    var score by remember { mutableStateOf(0.0) }
    var rec by remember { mutableStateOf(false) }
    var paused by remember { mutableStateOf(false) }
    var info by remember { mutableStateOf(false) }
    var ended by remember { mutableStateOf(false) }
    var run by remember { mutableStateOf(0) }
    var play by remember { mutableStateOf(0L) }
    var total by remember { mutableStateOf(st.totalSec()) }
    var fg by remember { mutableStateOf(true) }
    var home2 by remember { mutableStateOf(false) }
    var showProfile by remember { mutableStateOf(false) }
    val act = LocalContext.current as Activity
    val lo = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val ins = remember { Insights(act) }
    var asked by remember { mutableStateOf(ins.asked) }
    val prog = remember { Animatable(0f) }
    val scr = rememberScrollState()
    val portalOrigin = with(LocalDensity.current) { Offset(70.dp.toPx(), 56.dp.toPx()) }

    LaunchedEffect(Unit) { AdBrain.ins = ins; Snd.warm(act); Sfx.sound = st.bool("snd", true); Sfx.vib = st.bool("vib", true) }
    LaunchedEffect(Unit) { while (true) { delay(50); if (Pause.on) Pause.lost += 50 } }
    DisposableEffect(lo) {
        val ob = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_RESUME) { fg = true; ins.foreground() }
            if (e == Lifecycle.Event.ON_PAUSE) { fg = false; st.setTotal(total); ins.background() }
        }
        lo.lifecycle.addObserver(ob)
        onDispose { lo.lifecycle.removeObserver(ob) }
    }
    LaunchedEffect(Unit) {
        var n = 0
        while (true) {
            delay(1000)
            if (fg) {
                total++; if (step == 2 && !paused && !info && !ended) play++; n++; if (n % 10 == 0) st.setTotal(total)
                ins.tick(if (step == 2 && !paused && !info && !ended) g?.id else null)
            }
        }
    }
    SideEffect { Pause.on = paused || info; Ended.on = ended }

    val AC = listOf(Color(0xFF4DD0E1), Color(0xFFFFB74D), Color(0xFFB39DDB), Color(0xFF81C784), Color(0xFFF48FB1))
    val c = g
    val asm = c?.asmr == true
    val inGame = c != null && step >= 1
    val god = inGame && !asm && diff == 3
    val ac = if (inGame && !asm) DC[diff] else if (inGame) AC[4] else AC[tab]
    val back: () -> Unit = {
        if (step == 2 && !asm) { if (ended) { ended = false; step = 1 } else paused = true }
        else if (step == 2 || step == 1) step = 0 else step = 1
    }
    fun adThen(next: () -> Unit) { if (AdBrain.allow(play)) Ads.show(act, { AdBrain.shown() }, next) else next() }
    fun toggleHome() {
        home2 = !home2; Snd.play("portal")
        scope.launch { prog.animateTo(if (home2) 1f else 0f, tween(2400, easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f))) }
    }
    BackHandler(home2 && step == 0) { Sfx.tick(); toggleHome() }
    BackHandler(step != 0) { Sfx.tick(); back() }

    val homeFront: @Composable () -> Unit = {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).clickable { toggleHome() }) {
                    Text("BRAIN GYM", fontSize = 26.sp, fontWeight = FontWeight.Black)
                    Text("Memory · reflexes · logic · fun · asmr", color = Color.Gray, fontSize = 12.sp)
                }
                IconButton({ Sfx.sound = !Sfx.sound; st.setBool("snd", Sfx.sound); Sfx.tick() }) { Icon(if (Sfx.sound) Icons.Rounded.VolumeUp else Icons.Rounded.VolumeOff, null, tint = if (Sfx.sound) ac else Color.Gray) }
                IconButton({ Sfx.vib = !Sfx.vib; st.setBool("vib", Sfx.vib); Sfx.tick(); Sfx.buzz(act) }) { Icon(Icons.Rounded.Vibration, null, tint = if (Sfx.vib) ac else Color.Gray) }
                Row(Modifier.clip(RoundedCornerShape(12.dp)).background(Color(0xFF1C1C20)).padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Timer, null, tint = ac, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp))
                    Column { Text(fmt(total), fontSize = 14.sp, fontWeight = FontWeight.Bold); Text("total played", fontSize = 9.sp, color = Color.Gray) }
                }
            }
            Sp(14)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf("Brain", "Reaction", "Logic", "Fun", "ASMR").forEachIndexed { i, t -> Chip(t, tab == i, AC[i], Modifier.weight(1f), true) { tab = i; ins.tab(i) } }
            }
            Sp(14)
            val list = GAMES.filter { if (tab == 4) it.asmr else if (tab == 3) it.fn else if (tab == 2) it.logic else !it.logic && !it.fn && !it.asmr && it.mem == (tab == 0) }
            val tw = (LocalConfiguration.current.screenWidthDp - 32 - 20) / 3
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
                list.chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)) {
                        row.forEach { x ->
                            Tile(x, AC[tab], st, Modifier.width(tw.dp)) {
                                g = x; mode = x.modes[0]; Live.txt = ""
                                if (x.asmr) { ended = false; run++; ins.gameStarted(x.id, 0); step = 2 } else step = 1
                            }
                        }
                    }
                    Sp(10)
                }
            }
        }
    }

    CompositionLocalProvider(LocalAccent provides ac) {
        Column(Modifier.fillMaxSize().background(Brush.verticalGradient(if (god) listOf(Color(0xFF2A0E0E), Color.Black) else listOf(Color(0xFF0E0E10), Color.Black))).padding(16.dp)) {
            if (c == null || step == 0) {
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    if (home2 && prog.value >= 1f) NewHome({ toggleHome() }, { showProfile = true })
                    else if (prog.value > 0f) PortalReveal(prog.value, portalOrigin, homeFront) { NewHome({ toggleHome() }, { showProfile = true }) }
                    else homeFront()
                }
            } else {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton({ Sfx.tick(); back() }) {
                        Icon(if (step == 2 && !ended && !asm) Icons.Rounded.Pause else Icons.Rounded.ArrowBack, null, tint = Color.White)
                    }
                    Spacer(Modifier.weight(1f))
                    if (step == 2 && !asm) Text(fmt(play), fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    if (asm) Text(c.name, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    IconButton({ Sfx.tick(); info = true }) { Icon(Icons.Rounded.Info, null, tint = ac) }
                }
                if (step == 2 && !asm) Text(Live.txt, fontSize = 18.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp))
                Box(Modifier.weight(1f).fillMaxWidth().then(if (step == 2 && !asm) Modifier.hub() else Modifier), contentAlignment = Alignment.Center) {
                    Column(Modifier.then(if (asm) Modifier else Modifier.verticalScroll(scr)), horizontalAlignment = Alignment.CenterHorizontally) {
                        when (step) {
                            1 -> {
                                Box(Modifier.size(78.dp).clip(CircleShape).background(ac.copy(.16f)), Alignment.Center) { Icon(icon(c.id), null, tint = ac, modifier = Modifier.size(44.dp)) }
                                Sp(10); Text(c.name, fontSize = 24.sp, fontWeight = FontWeight.Bold); Sp(18)
                                Text("MODE", color = Color.Gray, fontSize = 12.sp); Sp(6)
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    c.modes.forEach { m -> Chip(m, m == mode, ac) { mode = m } }
                                }
                                Sp(14); Text("DIFFICULTY", color = Color.Gray, fontSize = 12.sp); Sp(6)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf("Easy", "Normal", "Hard", "God").forEachIndexed { i, n -> Chip(n, i == diff, DC[i]) { diff = i } }
                                }
                                Sp(26); Big("START", ac) { ended = false; play = 0; Live.txt = ""; run++; Sfx.base = (c.id.hashCode() and 0x7fffffff) % 5; ins.gameStarted(c.id, diff); step = 2 }
                                Sp(8); Chip("Scores and history", false, ac) { step = 4 }
                            }
                            2 -> {
                                val fin = remember(run) {
                                    { s: Double ->
                                        if (!ended) {
                                            val h = st.hist(c.id).map { it[3].toDouble() }
                                            rec = h.isEmpty() || (if (c.lower) s < h.min() else s > h.max())
                                            st.add(c.id, mode, diff, s); score = s; ended = true
                                            ins.gameFinished(c.id, s, rec)
                                            if (rec) Sfx.win() else Sfx.bad()
                                            Sfx.buzz(act)
                                        }
                                    }
                                }
                                Box(contentAlignment = Alignment.Center) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) { key(run) { Play(c, mode, diff, fin) } }
                                    if (ended) Box(Modifier.matchParentSize().pointerInput(Unit) {
                                        awaitPointerEventScope { while (true) { val e = awaitPointerEvent(); e.changes.forEach { it.consume() } } }
                                    })
                                }
                                if (ended) {
                                    Sp(18)
                                    Text(if (rec) "New best!" else "Game over", color = ac, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                                    Text("%.1f".format(score), fontSize = 52.sp, fontWeight = FontWeight.Black)
                                    Text("${c.unit} · time ${fmt(play)}", color = Color.Gray)
                                    Sp(14); Big("RETRY", ac) { adThen { ended = false; play = 0; Live.txt = ""; run++; ins.gameStarted(c.id, diff) } }
                                    Sp(8); Chip("Menu", true, ac) { adThen { ended = false; step = 1 } }
                                }
                            }
                            3 -> { }
                            else -> {
                                Text("${c.name} history", fontSize = 20.sp, fontWeight = FontWeight.Bold); Sp(8)
                                val h = st.hist(c.id)
                                h.map { it[3].toDouble() }.let { if (it.isNotEmpty()) Text("Best: ${"%.1f".format(if (c.lower) it.min() else it.max())} ${c.unit}", color = ac) }
                                Sp(8)
                                h.forEach { Text("${java.text.SimpleDateFormat("dd MMM HH:mm").format(java.util.Date(it[4].toLong()))} · ${it[1]} · ${listOf("Easy", "Normal", "Hard", "God")[it[2].toInt()]} · ${"%.1f".format(it[3].toDouble())}", fontSize = 13.sp) }
                            }
                        }
                    }
                }
            }
            Banner()
        }
        if (showProfile) ProfileSheet(ins, st) { showProfile = false }
        if (!asked) TermsGate { ins.consent(true); ins.foreground(); asked = true }
        if (info && c != null) InfoDialog(c) { info = false }
        if (paused && c != null) AlertDialog(
            onDismissRequest = { paused = false },
            title = { Text("Paused") },
            text = {
                Column {
                    Text(c.name, fontWeight = FontWeight.Bold); Sp(6)
                    Text("Score: ${Live.txt.ifEmpty { "—" }}"); Sp(4); Text("Time played: ${fmt(play)}"); Sp(10)
                    Row(verticalAlignment = Alignment.CenterVertically) { Text("Sound", Modifier.weight(1f)); Switch(Sfx.sound, { Sfx.sound = it; st.setBool("snd", it); Sfx.tick() }) }
                    Row(verticalAlignment = Alignment.CenterVertically) { Text("Vibration", Modifier.weight(1f)); Switch(Sfx.vib, { Sfx.vib = it; st.setBool("vib", it); Sfx.tick(); Sfx.buzz(act) }) }
                }
            },
            confirmButton = { TextButton({ Sfx.tick(); paused = false }) { Text("Resume") } },
            dismissButton = { TextButton({ Sfx.tick(); paused = false; step = 1 }) { Text("Quit") } }
        )
    }
}
