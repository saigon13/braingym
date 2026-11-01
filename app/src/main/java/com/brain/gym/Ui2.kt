package com.brain.gym

import android.app.Activity
import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.delay

fun fmt(s: Long) = if (s >= 3600) "${s / 3600}h ${"%02d".format((s % 3600) / 60)}m" else "${s / 60}:${"%02d".format(s % 60)}"

@Composable fun Sp(h: Int) = Spacer(Modifier.height(h.dp))

@Composable fun Chip(t: String, sel: Boolean, ac: Color, m: Modifier = Modifier, f: () -> Unit) =
    Button({ Sfx.tick(); f() }, m, shape = RoundedCornerShape(12.dp), contentPadding = PaddingValues(horizontal = 10.dp),
        colors = ButtonDefaults.buttonColors(containerColor = if (sel) ac else Color(0xFF1C1C20), contentColor = if (sel) Color.Black else Color.White)) {
        Text(t, fontSize = 13.sp, maxLines = 1)
    }

@Composable fun Big(t: String, ac: Color, f: () -> Unit) =
    Button({ Sfx.tick(); f() }, Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = ac, contentColor = Color.Black)) { Text(t, fontWeight = FontWeight.Bold) }

@Composable fun Tile(x: G, ac: Color, st: Store, m: Modifier, on: () -> Unit) {
    val b = st.hist(x.id).map { it[3].toDouble() }.let { if (x.lower) it.minOrNull() else it.maxOrNull() }
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
        "num" -> NumberMem(m, d, f); "seq" -> SeqMem(m, d, f); "chimp" -> Chimp(d, f); "pairs" -> Pairs(m, d, f)
        "nback" -> NBack(d, f); "matrix" -> Matrix(d, f); "words" -> Words(d, f); "dual" -> Dual(d, f); "corsi" -> Corsi(d, f)
        "rev" -> RevSpan(d, f); "flash" -> Flash(m, d, f); "react" -> Reaction(m, f); "gonogo" -> GoNoGo(d, f)
        "stroop" -> Stroop(f); "schulte" -> Schulte(d, f); "cmatch" -> ColorMatch(f); "whack" -> Whack(d, f)
        "odd" -> Odd(d, f); "flank" -> Flanker(f); "g2048" -> G2048(d, f); "lights" -> Lights(d, f)
        "sudoku" -> Sudoku(d, f); "mines" -> Mines(d, f); "chess" -> ChessBoard(m, d, f)
        "flappy" -> Flappy(d, f); "snake" -> Snake(d, f)
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
    val act = LocalContext.current as Activity
    val lo = LocalLifecycleOwner.current
    LaunchedEffect(Unit) { Sfx.sound = st.bool("snd", true); Sfx.vib = st.bool("vib", true) }
    DisposableEffect(lo) {
        val ob = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_RESUME) fg = true
            if (e == Lifecycle.Event.ON_PAUSE) { fg = false; st.setTotal(total) }
        }
        lo.lifecycle.addObserver(ob)
        onDispose { lo.lifecycle.removeObserver(ob) }
    }
    LaunchedEffect(Unit) {
        var n = 0
        while (true) {
            delay(1000)
            if (fg) { total++; if (step == 2 && !paused && !info && !ended) play++; n++; if (n % 10 == 0) st.setTotal(total) }
        }
    }
    SideEffect { Pause.on = paused || info; Ended.on = ended }
    BackHandler(step != 0) {
        if (step == 2) { if (ended) { ended = false; step = 1 } else paused = true } else if (step == 1) step = 0 else step = 1
    }

    val AC = listOf(Color(0xFF4DD0E1), Color(0xFFFFB74D), Color(0xFFB39DDB), Color(0xFF81C784))
    val c = g
    val god = c != null && step >= 1 && diff == 3 && c.id in GODS
    val ac = if (god) Color(0xFFFF5252) else if (c == null) AC[tab] else if (c.fn) AC[3] else if (c.logic) AC[2] else if (c.mem) AC[0] else AC[1]
    val dd = if (c != null && diff >= 3 && c.id !in GODS) 2 else diff
    CompositionLocalProvider(LocalAccent provides ac) {
        Column(Modifier.fillMaxSize().background(Brush.verticalGradient(if (god) listOf(Color(0xFF2A0E0E), Color.Black) else listOf(Color(0xFF0E0E10), Color.Black))).padding(16.dp)) {
            if (c == null || step == 0) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("BRAIN GYM", fontSize = 26.sp, fontWeight = FontWeight.Black)
                        Text("Memory · reflexes · logic · fun", color = Color.Gray, fontSize = 12.sp)
                    }
                    IconButton({ Sfx.sound = !Sfx.sound; st.setBool("snd", Sfx.sound) }) { Icon(if (Sfx.sound) Icons.Rounded.VolumeUp else Icons.Rounded.VolumeOff, null, tint = if (Sfx.sound) ac else Color.Gray) }
                    IconButton({ Sfx.vib = !Sfx.vib; st.setBool("vib", Sfx.vib) }) { Icon(Icons.Rounded.Vibration, null, tint = if (Sfx.vib) ac else Color.Gray) }
                    Row(Modifier.clip(RoundedCornerShape(12.dp)).background(Color(0xFF1C1C20)).padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Timer, null, tint = ac, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp))
                        Column { Text(fmt(total), fontSize = 14.sp, fontWeight = FontWeight.Bold); Text("total played", fontSize = 9.sp, color = Color.Gray) }
                    }
                }
                Sp(14)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Brain", "Reaction", "Logic", "Fun").forEachIndexed { i, t -> Chip(t, tab == i, AC[i], Modifier.weight(1f)) { tab = i } }
                }
                Sp(14)
                val list = GAMES.filter { if (tab == 3) it.fn else if (tab == 2) it.logic else !it.logic && !it.fn && it.mem == (tab == 0) }
                val tw = (LocalConfiguration.current.screenWidthDp - 32 - 20) / 3
                Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
                    list.chunked(3).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)) {
                            row.forEach { x -> Tile(x, AC[tab], st, Modifier.width(tw.dp)) { g = x; mode = x.modes[0]; if (diff > 2 && x.id !in GODS) diff = 1; step = 1 } }
                        }
                        Sp(10)
                    }
                }
                Sp(8)
            } else {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton({ if (step == 2) { if (ended) { ended = false; step = 1 } else paused = true } else if (step == 1) step = 0 else step = 1 }) {
                        Icon(if (step == 2 && !ended) Icons.Rounded.Pause else Icons.Rounded.ArrowBack, null, tint = Color.White)
                    }
                    Spacer(Modifier.weight(1f))
                    if (step == 2) Text(fmt(play), fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    IconButton({ info = true }) { Icon(Icons.Rounded.Info, null, tint = ac) }
                }
                if (step == 2) Text(Live.txt, fontSize = 18.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp))
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(Modifier.verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
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
                                    listOf("Easy", "Normal", "Hard", "God").take(if (c.id in GODS) 4 else 3).forEachIndexed { i, n -> Chip(n, i == diff, if (i == 3) Color(0xFFFF5252) else ac) { diff = i } }
                                }
                                Sp(26); Big("START", ac) { ended = false; play = 0; Live.txt = ""; run++; Sfx.base = (c.id.hashCode() and 0x7fffffff) % 10; step = 2 }
                                Sp(8); Chip("Scores and history", false, ac) { step = 4 }
                            }
                            2 -> {
                                val fin = { s: Double ->
                                    if (!ended) {
                                        val h = st.hist(c.id).map { it[3].toDouble() }
                                        rec = h.isEmpty() || (if (c.lower) s < h.min() else s > h.max())
                                        st.add(c.id, mode, diff, s); score = s; ended = true
                                        if (rec) Sfx.win() else { Sfx.bad(); Sfx.buzz(act) }
                                    }
                                }
                                Box(contentAlignment = Alignment.Center) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) { key(run) { Play(c, mode, dd, fin) } }
                                    if (ended) Box(Modifier.matchParentSize().pointerInput(Unit) {
                                        awaitPointerEventScope { while (true) { val e = awaitPointerEvent(); e.changes.forEach { it.consume() } } }
                                    })
                                }
                                if (ended) {
                                    Sp(18)
                                    Text(if (rec) "New best!" else "Game over", color = ac, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                                    Text("%.1f".format(score), fontSize = 52.sp, fontWeight = FontWeight.Black)
                                    Text("${c.unit} · time ${fmt(play)}", color = Color.Gray)
                                    Sp(14); Big("RETRY", ac) { Ads.maybe(act) { ended = false; play = 0; Live.txt = ""; run++ } }
                                    Sp(8); Chip("Menu", false, ac) { Ads.maybe(act) { ended = false; step = 1 } }
                                }
                            }
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
        if (info && c != null) InfoDialog(c) { info = false }
        if (paused && c != null) AlertDialog(
            onDismissRequest = { paused = false },
            title = { Text("Paused") },
            text = {
                Column {
                    Text(c.name, fontWeight = FontWeight.Bold); Sp(6)
                    Text("Score: ${Live.txt.ifEmpty { "—" }}"); Sp(4); Text("Time played: ${fmt(play)}"); Sp(10)
                    Row(verticalAlignment = Alignment.CenterVertically) { Text("Sound", Modifier.weight(1f)); Switch(Sfx.sound, { Sfx.sound = it; st.setBool("snd", it) }) }
                    Row(verticalAlignment = Alignment.CenterVertically) { Text("Vibration", Modifier.weight(1f)); Switch(Sfx.vib, { Sfx.vib = it; st.setBool("vib", it) }) }
                }
            },
            confirmButton = { TextButton({ paused = false }) { Text("Resume") } },
            dismissButton = { TextButton({ paused = false; step = 1 }) { Text("Quit") } }
        )
    }
}
