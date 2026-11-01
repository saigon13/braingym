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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable fun NumberMem(m: String, d: Int, done: (Double) -> Unit) {
    var lv by remember { mutableStateOf(1) }; var s by remember { mutableStateOf("") }
    var ph by remember { mutableStateOf(0) }; var inp by remember { mutableStateOf("") }
    val len = spanLen(d, lv, intArrayOf(2, 3, 5, 7), intArrayOf(3, 5, 7, 10))
    val pool = when (m) { "Letters" -> "ABCDEFGHJKLMNPQRSTUVWXYZ"; "Mixed" -> "ABCDEFGHJKLMNPQRSTUVWXYZ0123456789"; else -> "0123456789" }
    LaunchedEffect(ph, lv) { if (ph == 0) { s = rndStr(pool, len); wait(900L + len * 450L); ph = 1 } }
    Status("Level $lv · $len characters")
    if (ph == 0) BigText(s)
    if (ph == 1) {
        if (Ended.on) {
            Text("Correct answer", color = Color.Gray); BigText(s, PURP)
            Spacer(Modifier.height(10.dp))
            Text("Your answer", color = Color.Gray); BigText(inp.ifEmpty { "—" }, ROSE)
        } else {
            TypedBox(inp)
            Keypad(if (m == "Digits") "num" else if (m == "Letters") "alpha" else "mix") { k ->
                when (k) {
                    "⌫" -> { inp = inp.dropLast(1) }
                    "✓" -> { if (inp == s) { Sfx.good(); lv++; inp = ""; ph = 0 } else done(lv - 1.0) }
                    else -> { if (inp.length < s.length) inp += k }
                }
            }
        }
    }
}

@Composable fun SeqMem(m: String, d: Int, done: (Double) -> Unit) {
    val pool = remember { bagList(9, 120) }
    var n by remember { mutableStateOf(1) }; var lit by remember { mutableStateOf(-1) }
    var pos by remember { mutableStateOf(0) }; var show by remember { mutableStateOf(true) }
    var wrong by remember { mutableStateOf(-1) }; var miss by remember { mutableStateOf(-1) }
    val seq = pool.take(n)
    LaunchedEffect(n) {
        show = true; pos = 0; wait(600)
        for (i in seq) { lit = i; wait(maxOf(180L, 700L - d * 200)); lit = -1; wait(200) }
        show = false
    }
    Status(if (show) "Watch... · length $n" else "Repeat · length $n")
    Grid(3, 3) { i ->
        val bg = if (Ended.on && i == wrong) ROSE else if (Ended.on && i == miss) PURP else if (lit == i) Color.White else Color.Black
        Cell(bg = bg) {
            if (!show && !Ended.on) {
                val exp = if (m == "Reverse") seq.reversed()[pos] else seq[pos]
                if (i == exp) { pos++; if (pos == seq.size) { Sfx.good(); n++ } } else { wrong = i; miss = exp; done(n - 1.0) }
            }
        }
    }
}

@Composable fun Chimp(d: Int, done: (Double) -> Unit) {
    var n by remember { mutableStateOf(4 + d) }
    val pos = remember(n) { (0..24).shuffled().take(n) }
    var nx by remember(n) { mutableStateOf(1) }; var wrong by remember { mutableStateOf(-1) }
    Status("Numbers: $n")
    Grid(5, 5) { i ->
        val k = pos.indexOf(i) + 1
        val bg = if (k > 0 && Ended.on) { if (i == wrong) ROSE else if (k >= nx) PURP else Color.Black } else if (k > 0 && k >= nx) Color.White else Color.Black
        Cell(if (k > 0 && (nx == 1 || Ended.on)) "$k" else "", bg) {
            if (k > 0 && !Ended.on) { if (k == nx) { nx++; if (nx > n) { Sfx.good(); n++ } } else { wrong = i; done(n - 1.0) } }
        }
    }
}

val FACES: List<ImageVector> = listOf(Icons.Rounded.Favorite, Icons.Rounded.Star, Icons.Rounded.Bolt, Icons.Rounded.Cloud, Icons.Rounded.Pets,
    Icons.Rounded.Eco, Icons.Rounded.Anchor, Icons.Rounded.Diamond, Icons.Rounded.MusicNote, Icons.Rounded.Public, Icons.Rounded.Home, Icons.Rounded.Face)

@Composable fun FaceCell(f: Int, nums: Boolean, ok: Boolean, on: () -> Unit) {
    val s = LocalCell.current; val ac = LocalAccent.current; val sh = RoundedCornerShape((s / 5).dp)
    Box(Modifier.padding(3.dp).size((s - 6).dp).clip(sh)
        .background(if (ok) ac.copy(.22f) else if (f >= 0) Color(0xFF2A2A30) else Color(0xFF1B1B1F))
        .border(1.dp, ac.copy(if (f >= 0) .8f else .25f), sh).clickable { Sfx.click(); on() }, Alignment.Center) {
        if (f >= 0) {
            if (nums) Text("${f + 1}", fontSize = (s / 2.4).sp, fontWeight = FontWeight.Bold)
            else Icon(FACES[f], null, tint = ac, modifier = Modifier.size((s / 2).dp))
        } else Icon(Icons.Rounded.QuestionMark, null, tint = Color(0xFF55555C), modifier = Modifier.size((s / 3).dp))
    }
}

@Composable fun Pairs(m: String, d: Int, done: (Double) -> Unit) {
    val n = listOf(6, 8, 10, 12)[d]
    val items = remember { (0 until 12).shuffled().take(n).let { (it + it).shuffled() } }
    var open by remember { mutableStateOf(listOf<Int>()) }; var ma by remember { mutableStateOf(setOf<Int>()) }
    var mv by remember { mutableStateOf(0) }
    LaunchedEffect(open) { if (open.size == 2) { wait(900); open = listOf() } }
    Status("Moves: $mv · Pairs ${ma.size / 2}/$n")
    Grid(n / 2, 4) { i ->
        val up = i in open || i in ma
        FaceCell(if (up) items[i] else -1, m == "Numbers", i in ma) {
            if (!up) {
                var o: List<Int> = if (open.size == 2) listOf() else open
                o = o + i
                if (o.size == 2) {
                    mv++
                    if (items[o[0]] == items[o[1]]) { ma = ma + o; Sfx.good(); o = listOf() }
                }
                open = o
                if (ma.size == items.size) done(mv.toDouble())
            }
        }
    }
}

@Composable fun NBack(d: Int, done: (Double) -> Unit) {
    val n = d + 1
    val st = remember { seqGen(n, 20 + n, 9) }
    var i by remember { mutableStateOf(0) }; var tap by remember { mutableStateOf(false) }; var ok by remember { mutableStateOf(0) }
    var lock by remember { mutableStateOf(false) }; var fb by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        for (k in st.indices) {
            i = k; tap = false; lock = false; fb = ""; wait(1800)
            lock = true
            val mt = k >= n && st[k] == st[k - n]
            if (mt == tap) { ok++; Sfx.good(); fb = if (mt) "✓ Match caught" else "✓ Correct, no match" }
            else fb = if (mt) "✗ You missed a match" else "✗ That was not a match"
            wait(600)
        }
        done(ok * 100.0 / st.size)
    }
    val gh = if (d == 0 && i >= n) st[i - n] else -1
    Status("Tile ${i + 1}/${st.size} · correct $ok")
    Text("Is the glowing tile in the SAME place as $n step${if (n > 1) "s" else ""} ago?", fontSize = 14.sp, color = Color(0xFFB0B0B8))
    if (d == 0) Text("Easy: the dim purple square is where it was $n step ago", fontSize = 12.sp, color = PURP)
    Spacer(Modifier.height(8.dp))
    Grid(3, 3) { Cell(bg = if (st[i] == it) (if (i % 2 == 0) Color.White else Color(0xFFFFB74D)) else if (it == gh) Color(0xFF3D3563) else Color.Black) }
    Text(fb, color = if (fb.startsWith("✓")) Color(0xFF66BB6A) else ROSE, fontSize = 15.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 8.dp))
    Btn("MATCH") { if (!lock) tap = true }
}

@Composable fun Reaction(m: String, d: Int, done: (Double) -> Unit) {
    val total = if (m == "Single") 1 else 5; val race = m == "Race lights"
    var ph by remember { mutableStateOf(0) }; var t0 by remember { mutableStateOf(0L) }
    var res by remember { mutableStateOf(listOf<Long>()) }; var round by remember { mutableStateOf(0) }
    var lit by remember { mutableStateOf(0) }; var gr by remember { mutableStateOf(0) }; var msg by remember { mutableStateOf("") }
    var dec by remember { mutableStateOf(false) }
    val T = longArrayOf(1100, 1000, 850, 700)[d]
    LaunchedEffect(round) {
        ph = 0; lit = 0; gr = 0; dec = false
        if (race) {
            repeat(5) { wait(T); lit++; Sfx.tick() }
            wait(T); gr = 5
        } else {
            if (d == 3 && (0..1).random() == 0) { wait((600..1500).random().toLong()); dec = true; wait(500); dec = false }
            wait((1500..4500).random().toLong())
        }
        t0 = now(); ph = 1
    }
    Status(if (msg.isEmpty()) "Round ${res.size + 1}/$total" else "Round ${res.size + 1}/$total · $msg")
    if (race) Row { repeat(5) { i ->
        Box(Modifier.padding(6.dp).size(46.dp).clip(CircleShape)
            .background(if (i < gr) Color(0xFF2ECC71) else if (i < lit) Color(0xFFE53935) else Color(0xFF2A2A2E))
            .border(2.dp, Color(0xFF444444), CircleShape))
    } }
    Spacer(Modifier.height(12.dp))
    Box(Modifier.fillMaxWidth().height(240.dp).clip(RoundedCornerShape(28.dp))
        .background(if (ph == 1) Color(0xFF1B8A3A) else if (dec) Color(0xFFFFC107) else Color(0xFF26262B)).clickable {
            if (ph == 1) { val ms = now() - t0; val r = res + ms; res = r; msg = "$ms ms"; Sfx.click(); if (r.size >= total) done(r.average()) else round++ }
            else { msg = "False start!"; Sfx.bad(); round++ }
        }, Alignment.Center) {
        Text(if (ph == 1) "TAP!" else if (dec) "Fake! Do not tap" else if (race && lit > 0) "Wait for all green…" else "Wait…", fontSize = 28.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable fun GoNoGo(d: Int, done: (Double) -> Unit) {
    val seq = remember { (List(14) { true } + List(6) { false }).shuffled() }
    var go by remember { mutableStateOf(true) }; var tap by remember { mutableStateOf(false) }
    var shown by remember { mutableStateOf(false) }; var sc by remember { mutableStateOf(0.0) }; var i by remember { mutableStateOf(0) }
    var at by remember { mutableStateOf(0L) }; var rt by remember { mutableStateOf(0L) }
    val win = 1000L - d * 220
    LaunchedEffect(Unit) {
        for (k in seq.indices) {
            go = seq[k]; tap = false; shown = true; at = now(); wait(win); shown = false
            if (go == tap) sc += 100.0 + (if (go) maxOf(0.0, 60.0 * (1.0 - rt / win.toDouble())) else 0.0)
            wait(350L + (0..300).random()); i = k + 1
        }
        done(sc)
    }
    Status("Tap GREEN only · Round ${minOf(i + 1, 20)}/20 · Score ${sc.toInt()}")
    Box(Modifier.size(230.dp).clip(CircleShape).background(if (!shown) Color(0xFF1B1B1F) else if (go) Color(0xFF1B8A3A) else Color(0xFFD32F2F)).border(2.dp, Color(0xFF444444), CircleShape).clickable {
        if (!tap) { tap = true; rt = now() - at; Sfx.click() }
    })
}

@Composable fun Stroop(d: Int, done: (Double) -> Unit) {
    val k = if (d == 3) 6 else 4; val N = if (d == 3) 30 else 20
    val wl = remember { bagList(k, N) }
    val il = remember { wl.mapIndexed { i, w -> if (d != 3 && i % 4 == 0) w else ((0 until k) - w).random() } }
    var n by remember { mutableStateOf(0) }; var ok by remember { mutableStateOf(0) }; val t0 = remember { now() }
    val a = wl[minOf(n, N - 1)]; val b = il[minOf(n, N - 1)]
    val bs = ((LocalConfiguration.current.screenWidthDp - 48) / k - 8).coerceIn(40, 76)
    Status("Correct $ok · ${minOf(n + 1, N)}/$N")
    Text(CL[a].first, color = CL[b].second, fontSize = 56.sp, fontWeight = FontWeight.Black)
    Spacer(Modifier.height(20.dp))
    Row { for (i in 0 until k) {
        val c = CL[i]
        Box(Modifier.padding(4.dp).size(bs.dp).clip(RoundedCornerShape(18.dp)).background(c.second).clickable {
            if (i == b) { ok++; Sfx.good() } else Sfx.bad()
            n++
            if (n >= N) done(speedScore(ok, N, t0))
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
