package com.brain.gym

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable fun NumberMem(m: String, d: Int, done: (Double) -> Unit) {
    var lvl by remember { mutableStateOf(3 + d) }; var s by remember { mutableStateOf("") }
    var ph by remember { mutableStateOf(0) }; var inp by remember { mutableStateOf("") }
    val pool = when (m) { "Letters" -> "ABCDEFGHJKLMNPQRSTUVWXYZ"; "Mixed" -> "ABCDEFGHJKLMNPQRSTUVWXYZ0123456789"; else -> "0123456789" }
    LaunchedEffect(ph, lvl) { if (ph == 0) { s = rndStr(pool, lvl); wait(900L + lvl * 450L); ph = 1 } }
    Status("Level ${lvl - 2 - d}")
    if (ph == 0) BigText(s) else {
        TypedBox(inp)
        Keypad(if (m == "Digits") "num" else if (m == "Letters") "alpha" else "mix") { k -> when (k) {
            "⌫" -> { inp = inp.dropLast(1) }
            "✓" -> { if (inp == s) { lvl++; inp = ""; ph = 0 } else done((lvl - 3 - d).toDouble()) }
            else -> { if (inp.length < s.length) inp += k }
        } }
    }
}

@Composable fun SeqMem(m: String, d: Int, done: (Double) -> Unit) {
    var seq by remember { mutableStateOf(listOf<Int>()) }; var lit by remember { mutableStateOf(-1) }
    var pos by remember { mutableStateOf(0) }; var show by remember { mutableStateOf(true) }
    fun next(s: List<Int>) = ((0..8) - listOfNotNull(s.lastOrNull()).toSet()).random()
    LaunchedEffect(seq.size) {
        if (seq.isEmpty()) { seq = listOf((0..8).random()); return@LaunchedEffect }
        show = true; pos = 0; wait(600)
        for (i in seq) { lit = i; wait(700L - d * 200); lit = -1; wait(200) }
        show = false
    }
    Status(if (show) "Watch..." else "Repeat (${seq.size})")
    Grid(3, 3) { i ->
        Cell(bg = if (lit == i) Color.White else Color.Black) {
            if (!show) {
                val exp = if (m == "Reverse") seq.reversed()[pos] else seq[pos]
                if (i == exp) { pos++; if (pos == seq.size) seq = seq + next(seq) } else done(seq.size - 1.0)
            }
        }
    }
}

@Composable fun Chimp(d: Int, done: (Double) -> Unit) {
    var n by remember { mutableStateOf(4 + d) }
    val pos = remember(n) { (0..24).shuffled().take(n) }
    var nx by remember(n) { mutableStateOf(1) }
    Status("Numbers: $n")
    Grid(5, 5) { i ->
        val k = pos.indexOf(i) + 1
        Cell(if (k > 0 && nx == 1) "$k" else "", if (k > 0 && k >= nx) Color.White else Color.Black) {
            if (k > 0) { if (k == nx) { nx++; if (nx > n) n++ } else done(n.toDouble()) }
        }
    }
}

val FACES: List<ImageVector> = listOf(Icons.Rounded.Favorite, Icons.Rounded.Star, Icons.Rounded.Bolt, Icons.Rounded.Cloud, Icons.Rounded.Pets,
    Icons.Rounded.Eco, Icons.Rounded.Anchor, Icons.Rounded.Diamond, Icons.Rounded.MusicNote, Icons.Rounded.Public)

@Composable fun FaceCell(f: Int, nums: Boolean, ok: Boolean, on: () -> Unit) {
    val s = LocalCell.current; val ac = LocalAccent.current; val sh = RoundedCornerShape((s / 5).dp)
    Box(Modifier.padding(3.dp).size((s - 6).dp).clip(sh)
        .background(if (ok) ac.copy(.22f) else if (f >= 0) Color(0xFF2A2A30) else Color(0xFF1B1B1F))
        .border(1.dp, ac.copy(if (f >= 0) .8f else .25f), sh).clickable { on() }, Alignment.Center) {
        if (f >= 0) {
            if (nums) Text("${f + 1}", fontSize = (s / 2.4).sp, fontWeight = FontWeight.Bold)
            else Icon(FACES[f], null, tint = ac, modifier = Modifier.size((s / 2).dp))
        } else Icon(Icons.Rounded.QuestionMark, null, tint = Color(0xFF55555C), modifier = Modifier.size((s / 3).dp))
    }
}

@Composable fun Pairs(m: String, d: Int, done: (Double) -> Unit) {
    val n = listOf(6, 8, 10)[d]
    val items = remember { (0 until 10).shuffled().take(n).let { (it + it).shuffled() } }
    var open by remember { mutableStateOf(listOf<Int>()) }; var ma by remember { mutableStateOf(setOf<Int>()) }
    var mv by remember { mutableStateOf(0) }; val sc = rememberCoroutineScope()
    Status("Moves: $mv · Pairs ${ma.size / 2}/$n")
    Grid(n / 2, 4) { i ->
        val up = i in open || i in ma
        FaceCell(if (up) items[i] else -1, m == "Numbers", i in ma) {
            if (!up && open.size < 2) {
                open = open + i
                if (open.size == 2) sc.launch {
                    mv++; wait(700)
                    if (items[open[0]] == items[open[1]]) ma = ma + open
                    open = listOf()
                    if (ma.size == items.size) done(mv.toDouble())
                }
            }
        }
    }
}

@Composable fun NBack(d: Int, done: (Double) -> Unit) {
    val n = d + 1
    val st = remember { seqGen(n, 20 + n, 9) }
    var i by remember { mutableStateOf(0) }; var tap by remember { mutableStateOf(false) }; var ok by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        for (k in st.indices) { i = k; tap = false; wait(1800); if ((k >= n && st[k] == st[k - n]) == tap) ok++ }
        done(ok * 100.0 / st.size)
    }
    Status("$n-back · trial ${i + 1}/${st.size}: press MATCH if it is where it was $n step(s) ago")
    Grid(3, 3) { Cell(bg = if (st[i] == it) Color.White else Color.Black) }
    Btn("MATCH") { tap = true }
}

@Composable fun Reaction(m: String, done: (Double) -> Unit) {
    val total = if (m == "Single") 1 else 5; val race = m == "Race lights"
    var ph by remember { mutableStateOf(0) }; var t0 by remember { mutableStateOf(0L) }
    var res by remember { mutableStateOf(listOf<Long>()) }; var round by remember { mutableStateOf(0) }
    var lit by remember { mutableStateOf(0) }; var msg by remember { mutableStateOf("") }
    LaunchedEffect(round) {
        ph = 0; lit = 0
        if (race) { repeat(5) { wait(850); lit++ }; wait((600..3200).random().toLong()); lit = 0 } else wait((1500..4500).random().toLong())
        t0 = now(); ph = 1
    }
    Status(if (msg.isEmpty()) "Round ${res.size + 1}/$total" else msg)
    if (race) Row { repeat(5) { i -> Box(Modifier.padding(6.dp).size(46.dp).clip(CircleShape).background(if (i < lit) Color(0xFFE53935) else Color(0xFF2A2A2E)).border(2.dp, Color(0xFF444444), CircleShape)) } }
    Spacer(Modifier.height(12.dp))
    Box(Modifier.fillMaxWidth().height(240.dp).clip(RoundedCornerShape(28.dp)).background(if (ph == 1) Color(0xFF1B8A3A) else Color(0xFF26262B)).clickable {
        if (ph == 1) { val ms = now() - t0; val r = res + ms; res = r; msg = "$ms ms"; if (r.size >= total) done(r.average()) else round++ }
        else { msg = "False start!"; round++ }
    }, Alignment.Center) {
        Text(if (ph == 1) "TAP!" else if (race && lit > 0) "Lights are coming…" else "Wait…", fontSize = 28.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable fun GoNoGo(d: Int, done: (Double) -> Unit) {
    val seq = remember { (List(14) { true } + List(6) { false }).shuffled() }
    var go by remember { mutableStateOf(true) }; var tap by remember { mutableStateOf(false) }
    var shown by remember { mutableStateOf(false) }; var ok by remember { mutableStateOf(0) }; var i by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        for (k in seq.indices) { go = seq[k]; tap = false; shown = true; wait(1000L - d * 250); shown = false; if (go == tap) ok++; wait(350L + (0..300).random()); i = k + 1 }
        done(ok * 5.0)
    }
    Status("Tap GREEN only · Round ${i + 1}/20")
    Box(Modifier.size(230.dp).clip(CircleShape).background(if (!shown) Color(0xFF1B1B1F) else if (go) Color(0xFF1B8A3A) else Color(0xFFD32F2F)).border(2.dp, Color(0xFF444444), CircleShape).clickable { tap = true })
}

@Composable fun Stroop(done: (Double) -> Unit) {
    var a by remember { mutableStateOf((0..3).random()) }; var b by remember { mutableStateOf((0..3).random()) }
    var n by remember { mutableStateOf(0) }; var ok by remember { mutableStateOf(0) }
    Status("Pick the INK colour ($n/20)")
    Text(CL[a].first, color = CL[b].second, fontSize = 56.sp, fontWeight = FontWeight.Black)
    Spacer(Modifier.height(20.dp))
    Row { CL.forEachIndexed { i, c ->
        Box(Modifier.padding(4.dp).size(76.dp).clip(RoundedCornerShape(18.dp)).background(c.second).clickable {
            if (i == b) ok++; n++
            if (n >= 20) done(ok.toDouble()) else { a = (0..3).random(); b = if ((0..4).random() == 0) a else ((0..3) - a).random() }
        }, Alignment.Center) { Text(c.first.take(3), color = Color.Black, fontWeight = FontWeight.Bold) }
    } }
}

@Composable fun Schulte(d: Int, done: (Double) -> Unit) {
    val s = 3 + d
    val nums = remember { (1..s * s).shuffled() }
    var nx by remember { mutableStateOf(1) }; val t0 = remember { now() }
    Status("Tap 1 → ${s * s} in order · Next: $nx")
    Grid(s, s) { i -> Cell("${nums[i]}", if (nums[i] < nx) Color.DarkGray else Color.Black) { if (nums[i] == nx) { nx++; if (nx > s * s) done((now() - t0) / 1000.0) } } }
}
