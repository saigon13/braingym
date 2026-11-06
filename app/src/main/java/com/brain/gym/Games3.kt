package com.brain.gym

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable fun SeqMem2(m: String, d: Int, done: (Double) -> Unit) {
    val pool = remember { bagList(9, 120) }
    var n by remember { mutableStateOf(1) }
    var lit by remember { mutableStateOf(-1) }
    var pos by remember { mutableStateOf(0) }
    var show by remember { mutableStateOf(true) }
    var wrong by remember { mutableStateOf(-1) }
    var over by remember { mutableStateOf(false) }
    val seq = pool.take(n)
    val want = if (m == "Reverse") seq.reversed() else seq
    LaunchedEffect(n) {
        show = true; pos = 0; wait(500)
        for (i in seq) { lit = i; wait(maxOf(160L, 650L - d * 180)); lit = -1; wait(150) }
        show = false
    }
    Status(if (show) "Watch... · length $n" else "Repeat · length $n")
    Grid(3, 3) { i ->
        val order = want.indices.filter { want[it] == i }.map { it + 1 }
        val bg = if (over && i == wrong) ROSE else if (over && order.isNotEmpty()) PURP else if (lit == i) Color.White else Color.Black
        Cell(if (over) (if (i == wrong) "✗" else order.joinToString(",")) else "", bg) {
            if (!show && !over) {
                if (i == want[pos]) { pos++; if (pos == want.size) { Sfx.good(); n++ } } else { wrong = i; over = true; done(n - 1.0) }
            }
        }
    }
}

@Composable fun Chimp2(d: Int, done: (Double) -> Unit) {
    var n by remember { mutableStateOf(4 + d) }
    val pos = remember(n) { (0..24).shuffled().take(n) }
    var nx by remember(n) { mutableStateOf(1) }
    var wrong by remember { mutableStateOf(-1) }
    var over by remember { mutableStateOf(false) }
    Status("Numbers: $n")
    Grid(5, 5) { i ->
        val k = pos.indexOf(i) + 1
        val bg = if (over && k > 0) { if (i == wrong) ROSE else if (k >= nx) PURP else Color.DarkGray } else if (k > 0 && k >= nx) Color.White else Color.Black
        Cell(if (k > 0 && (nx == 1 || over)) "$k" else "", bg) {
            if (!over && k > 0) {
                if (k == nx) { nx++; if (nx > n) { Sfx.good(); n++ } } else { wrong = i; over = true; done(n - 1.0) }
            }
        }
    }
}

@Composable fun Matrix2(d: Int, done: (Double) -> Unit) {
    var lv by remember { mutableStateOf(1) }
    val gs = minOf(6, 3 + (lv - 1) / 3 + d / 2)
    val n = minOf(gs * gs / 2, gs - 1 + (lv - 1) % 3 + d / 2)
    val tg = remember(lv) { (0 until gs * gs).shuffled().take(n) }
    var show by remember(lv) { mutableStateOf(true) }
    var hit by remember(lv) { mutableStateOf(setOf<Int>()) }
    var wrong by remember(lv) { mutableStateOf(-1) }
    var over by remember(lv) { mutableStateOf(false) }
    LaunchedEffect(lv) { wait(1200L + n * 100L); show = false }
    Status("Level $lv · ${gs}×$gs grid · $n tiles")
    Grid(gs, gs) { i ->
        val bg = if (over && i == wrong) ROSE else if (over && i in tg && i !in hit) PURP
            else if ((show && i in tg) || i in hit) Color.White else Color.Black
        Cell(bg = bg) {
            if (!show && !over && i !in hit) {
                if (i in tg) { hit = hit + i; if (hit.size == n) { Sfx.good(); lv++ } } else { wrong = i; over = true; done(lv - 1.0) }
            }
        }
    }
}

@Composable fun Corsi2(d: Int, done: (Double) -> Unit) {
    val pool = remember { bagList(16, 60) }
    var n by remember { mutableStateOf(1) }
    var lit by remember { mutableStateOf(-1) }
    var pos by remember { mutableStateOf(0) }
    var show by remember { mutableStateOf(true) }
    var wrong by remember { mutableStateOf(-1) }
    var miss by remember { mutableStateOf(-1) }
    var over by remember { mutableStateOf(false) }
    val seq = pool.take(n)
    LaunchedEffect(n) {
        show = true; pos = 0; wait(500)
        for (i in seq) { lit = i; wait(maxOf(200L, 700L - d * 180)); lit = -1; wait(200) }
        show = false
    }
    Status(if (show) "Watch... · length $n" else "Repeat · length $n")
    Grid(4, 4) { i ->
        val bg = if (over && i == wrong) ROSE else if (over && i == miss) PURP else if (lit == i) Color.White else Color.Black
        Cell(bg = bg) {
            if (!show && !over) {
                if (i == seq[pos]) { pos++; if (pos == seq.size) { Sfx.good(); n++ } } else { wrong = i; miss = seq[pos]; over = true; done(n - 1.0) }
            }
        }
    }
}

@Composable fun NBack2(d: Int, done: (Double) -> Unit) {
    val n = d + 1
    val st = remember { seqGen(n, 20 + n, 9) }
    var i by remember { mutableStateOf(0) }
    var tap by remember { mutableStateOf(false) }
    var ok by remember { mutableStateOf(0) }
    var lock by remember { mutableStateOf(false) }
    var fb by remember { mutableStateOf("") }
    UseHub(down = { if (!lock) tap = true })
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
    Text("Same place as $n step${if (n > 1) "s" else ""} ago? Tap ANYWHERE for MATCH", fontSize = 14.sp, color = Color(0xFFB0B0B8))
    if (d == 0) Text("Easy: the dim purple square is where it was $n step ago", fontSize = 12.sp, color = PURP)
    Spacer(Modifier.height(8.dp))
    Grid(3, 3) { Cell(bg = if (st[i] == it) (if (i % 2 == 0) Color.White else Color(0xFFFFB74D)) else if (it == gh) Color(0xFF3D3563) else Color.Black) }
    Text(fb, color = if (fb.startsWith("✓")) Color(0xFF66BB6A) else ROSE, fontSize = 15.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 8.dp))
    Btn("MATCH") { if (!lock) tap = true }
}

@Composable fun Reaction2(m: String, d: Int, done: (Double) -> Unit) {
    val total = if (m == "Single") 1 else 5
    val race = m == "Race lights"
    var ph by remember { mutableStateOf(0) }
    var t0 by remember { mutableStateOf(0L) }
    var res by remember { mutableStateOf(listOf<Long>()) }
    var round by remember { mutableStateOf(0) }
    var lit by remember { mutableStateOf(0) }
    var gr by remember { mutableStateOf(0) }
    var msg by remember { mutableStateOf("") }
    val T = longArrayOf(1100, 1000, 850, 700)[d]
    UseHub(down = {
        if (!Ended.on && !Pause.on) {
            if (ph == 1) {
                val ms = now() - t0; val r = res + ms; res = r; msg = "$ms ms"; Sfx.click()
                if (r.size >= total) done(r.average()) else round++
            } else { msg = "False start!"; Sfx.bad(); round++ }
        }
    })
    LaunchedEffect(round) {
        ph = 0; lit = 0; gr = 0
        if (race) { repeat(5) { wait(T); lit++; Sfx.tick() }; wait(T); gr = 5 }
        else wait((1500..4500).random().toLong())
        t0 = now(); ph = 1
    }
    Status(if (msg.isEmpty()) "Round ${res.size + 1}/$total · tap anywhere" else "Round ${res.size + 1}/$total · $msg")
    if (race) Row {
        repeat(5) { k ->
            Box(Modifier.padding(6.dp).size(46.dp)
                .background(if (k < gr) Color(0xFF2ECC71) else if (k < lit) Color(0xFFE53935) else Color(0xFF2A2A2E), CircleShape)
                .border(2.dp, Color(0xFF444444), CircleShape))
        }
    }
    Spacer(Modifier.height(12.dp))
    Box(
        Modifier.fillMaxWidth().height(260.dp).clip(RoundedCornerShape(28.dp))
            .background(if (ph == 1) Color(0xFF1B8A3A) else Color(0xFF26262B)),
        Alignment.Center
    ) {
        Text(if (ph == 1) "TAP!" else if (race && lit > 0) "Wait for all green…" else "Wait…", fontSize = 28.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable fun GoNoGo2(d: Int, done: (Double) -> Unit) {
    val seq = remember { (List(14) { true } + List(6) { false }).shuffled() }
    var go by remember { mutableStateOf(true) }
    var tap by remember { mutableStateOf(false) }
    var shown by remember { mutableStateOf(false) }
    var sc by remember { mutableStateOf(0.0) }
    var i by remember { mutableStateOf(0) }
    var at by remember { mutableStateOf(0L) }
    var rt by remember { mutableStateOf(0L) }
    val win = 1000L - d * 220
    UseHub(down = { if (!tap && !Ended.on && !Pause.on) { tap = true; rt = now() - at; Sfx.click() } })
    LaunchedEffect(Unit) {
        for (k in seq.indices) {
            go = seq[k]; tap = false; shown = true; at = now(); wait(win); shown = false
            if (go == tap) sc += 100.0 + (if (go) maxOf(0.0, 60.0 * (1.0 - rt / win.toDouble())) else 0.0)
            wait(350L + (0..300).random()); i = k + 1
        }
        done(sc)
    }
    Status("Tap anywhere on GREEN only · Round ${minOf(i + 1, 20)}/20 · Score ${sc.toInt()}")
    Box(
        Modifier.size(230.dp)
            .background(if (!shown) Color(0xFF1B1B1F) else if (go) Color(0xFF1B8A3A) else Color(0xFFD32F2F), CircleShape)
            .border(2.dp, Color(0xFF444444), CircleShape)
    )
}

@Composable fun Whack2(d: Int, done: (Double) -> Unit) {
    val order = remember { bagList(9, 400) }
    var tick by remember { mutableStateOf(0) }
    var m by remember { mutableStateOf(order[0]) }
    var hit by remember { mutableStateOf(0) }
    var left by remember { mutableStateOf(30) }
    val step = longArrayOf(820, 620, 460, 320)[d]
    LaunchedEffect(Unit) {
        var t = 0
        while (t < 300) { wait(100); t++; left = 30 - t / 10 }
        done(hit.toDouble())
    }
    LaunchedEffect(tick) { m = order[tick]; wait(step); if (!Ended.on) tick++ }
    Status("Hits: $hit · ${left}s left")
    Grid(3, 3) { i ->
        Cell(bg = if (m == i) Color.White else Color.Black, sz = 9) {
            if (m == i && !Ended.on) { hit++; Sfx.good(); tick++; m = order[tick] }
        }
    }
}

@Composable fun Odd2(d: Int, done: (Double) -> Unit) {
    val s = 4 + d
    var r by remember { mutableStateOf(0) }
    var pen by remember { mutableStateOf(0) }
    val t0 = remember { now() }
    val types = remember { bagList(5, 12) }
    val lp = remember { OL.shuffled() }
    val sp = remember { OS.shuffled() }
    val dp = remember { OD.shuffled() }
    val np = remember { ON.shuffled() }
    val ty = types[minOf(r, 11)]
    val odd = remember(r) { (0 until s * s).random() }
    val pr = remember(r) {
        val p = when (ty) { 0 -> lp[r]; 1 -> OL[0]; 2 -> sp[r % sp.size]; 3 -> dp[r % dp.size]; else -> np[r % np.size] }
        if ((0..1).random() == 0) p else p.second to p.first
    }
    val hue = remember(r) { (0..359).random().toFloat() }
    val dl = (0.34f - r * 0.018f - d * 0.02f).coerceAtLeast(0.14f)
    Status("Round ${minOf(r + 1, 12)}/12 · ${listOf("Similar letters", "Shade", "Shapes", "Direction", "Digits")[ty]} · +2s per miss")
    Grid(s, s) { i ->
        val o = i == odd
        fun tap() { if (o) { Sfx.good(); r++; if (r >= 12) done((now() - t0) / 1000.0 + pen) } else { Sfx.bad(); pen += 2 } }
        if (ty == 1) Cell("", Color.hsv(hue, 0.65f, if (o) 0.55f + dl else 0.55f)) { tap() }
        else Cell(if (o) pr.second else pr.first) { tap() }
    }
}

@Composable fun Mines2(d: Int, done: (Double) -> Unit) {
    val N = listOf(6, 8, 9, 10)[d]
    val M = listOf(5, 10, 16, 24)[d]
    var mines by remember { mutableStateOf(setOf<Int>()) }
    var op by remember { mutableStateOf(setOf<Int>()) }
    var fl by remember { mutableStateOf(setOf<Int>()) }
    var over by remember { mutableStateOf(false) }
    fun nb(i: Int): List<Int> {
        return (-1..1).flatMap { dy -> (-1..1).map { dx -> dx to dy } }.filter { it != (0 to 0) }
            .map { (i % N + it.first) to (i / N + it.second) }
            .filter { it.first in 0 until N && it.second in 0 until N }
            .map { it.second * N + it.first }
    }
    Status("TAP = dig · HOLD = flag · Mines $M · Flags ${fl.size}")
    Grid(N, N) { i ->
        val label = if (i in op) {
            val c = nb(i).count { it in mines }
            if (c > 0) "$c" else ""
        } else if (i in fl) "⚑" else if (over && i in mines) "✹" else ""
        Cell(
            label,
            if (i in op) Color.DarkGray else if (over && i in mines) ROSE else Color.Black,
            onLong = { if (i !in op && !over) { fl = if (i in fl) fl - i else fl + i } }
        ) {
            if (i !in fl && i !in op && !over) {
                var mm = mines
                if (mm.isEmpty()) {
                    mm = ((0 until N * N) - (listOf(i) + nb(i)).toSet()).shuffled().take(M).toSet()
                    mines = mm
                }
                if (i in mm) {
                    over = true
                    done(op.size * 100.0 / (N * N - M))
                } else {
                    val o = op.toMutableSet()
                    val q = ArrayDeque(listOf(i))
                    while (q.isNotEmpty()) {
                        val x = q.removeFirst()
                        if (o.add(x) && nb(x).none { it in mm }) q.addAll(nb(x).filter { it !in o })
                    }
                    op = o
                    if (o.size == N * N - M) done(100.0)
                }
            }
        }
    }
}