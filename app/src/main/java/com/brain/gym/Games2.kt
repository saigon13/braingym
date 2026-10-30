package com.brain.gym

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

fun seqGen(n: Int, t: Int, r: Int): List<Int> { val l = mutableListOf<Int>(); repeat(t) { i -> l.add(if (i >= n && (0..2).random() == 0) l[i - n] else (0 until r).random()) }; return l }
val CL = listOf("RED" to Color.Red, "GREEN" to Color(0xFF2ECC71), "BLUE" to Color(0xFF4DA3FF), "YELLOW" to Color.Yellow)
fun now() = System.currentTimeMillis()

// ---------- MEMORY ----------
@Composable fun Matrix(d: Int, done: (Double) -> Unit) {
    var n by remember { mutableStateOf(3 + d) }
    val tg = remember(n) { (0..24).shuffled().take(n) }
    var show by remember(n) { mutableStateOf(true) }; var hit by remember(n) { mutableStateOf(setOf<Int>()) }
    LaunchedEffect(n) { delay(1500); show = false }
    Text("Level ${n - 2 - d}")
    Grid(5, 5) { i -> Cell(bg = if ((show && i in tg) || i in hit) Color.White else Color.Black) {
        if (!show && i !in hit) { if (i in tg) { hit = hit + i; if (hit.size == n) n++ } else done(n - 3.0 - d) }
    } }
}

@Composable fun Words(d: Int, done: (Double) -> Unit) {
    val W = listOf("apple", "river", "chair", "cloud", "tiger", "pencil", "window", "garden", "bridge", "candle", "mirror", "ocean", "violin", "rocket", "basket", "forest", "anchor", "pillow", "lantern", "button")
    val k = 6 + d * 2; val all = remember { W.shuffled() }; val seen = all.take(k); val opts = remember { all.take(k + 4).shuffled() }
    var ph by remember { mutableStateOf(0) }; var sel by remember { mutableStateOf(setOf<String>()) }
    LaunchedEffect(Unit) { delay(k * 900L); ph = 1 }
    if (ph == 0) Text("Memorise:\n" + seen.joinToString("\n"), fontSize = 22.sp) else {
        Text("Select the words you saw")
        opts.forEach { w -> Btn((if (w in sel) "● " else "") + w) { sel = if (w in sel) sel - w else sel + w } }
        Btn("SUBMIT") { done(maxOf(0, sel.count { it in seen } - sel.count { it !in seen }).toDouble()) }
    }
}

@Composable fun Dual(d: Int, done: (Double) -> Unit) {
    val n = d + 1; val L = "CHKLQRST"
    val ps = remember { seqGen(n, 20 + n, 9) }; val ls = remember { seqGen(n, 20 + n, 8) }
    var i by remember { mutableStateOf(0) }; var tp by remember { mutableStateOf(false) }; var tl by remember { mutableStateOf(false) }; var ok by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        for (k in ps.indices) { i = k; tp = false; tl = false; delay(2000)
            if ((k >= n && ps[k] == ps[k - n]) == tp) ok++; if ((k >= n && ls[k] == ls[k - n]) == tl) ok++ }
        done(ok * 50.0 / ps.size)
    }
    Text("$n-back  |  letter: ${L[ls[i]]}", fontSize = 20.sp)
    Grid(3, 3) { Cell(bg = if (ps[i] == it) Color.White else Color.Black) }
    Row { Btn("POSITION MATCH") { tp = true }; Btn("LETTER MATCH") { tl = true } }
}

// ---------- REACTION ----------
@Composable fun ColorMatch(done: (Double) -> Unit) {
    var a by remember { mutableStateOf(0) }; var b by remember { mutableStateOf(1) }; var n by remember { mutableStateOf(0) }; var ok by remember { mutableStateOf(0) }
    fun ans(y: Boolean) { if ((a == b) == y) ok++; n++; if (n >= 20) done(ok.toDouble()) else { a = (0..3).random(); b = if ((0..1).random() == 0) a else (0..3).random() } }
    Text("Does the WORD match its ink colour? ($n/20)")
    Text(CL[a].first, color = CL[b].second, fontSize = 48.sp)
    Row { Btn("YES") { ans(true) }; Btn("NO") { ans(false) } }
}

@Composable fun Whack(d: Int, done: (Double) -> Unit) {
    var m by remember { mutableStateOf(-1) }; var hit by remember { mutableStateOf(0) }; val step = 900L - d * 250
    LaunchedEffect(Unit) { repeat((30000 / step).toInt()) { m = (0..8).random(); delay(step) }; done(hit.toDouble()) }
    Text("30 seconds. Hits: $hit")
    Grid(3, 3) { i -> Cell(bg = if (m == i) Color.White else Color.Black, sz = 80) { if (m == i) { hit++; m = -1 } } }
}

@Composable fun Odd(d: Int, done: (Double) -> Unit) {
    val s = 4 + d; var r by remember { mutableStateOf(0) }; var pen by remember { mutableStateOf(0) }; val t0 = remember { now() }
    val odd = remember(r) { (0 until s * s).random() }
    Text("Find the Q (round ${r + 1}/10). Wrong tap = +2s")
    Grid(s, s) { i -> Cell(if (i == odd) "Q" else "O") { if (i == odd) { r++; if (r >= 10) done((now() - t0) / 1000.0 + pen) } else pen += 2 } }
}

@Composable fun Flanker(done: (Double) -> Unit) {
    var c by remember { mutableStateOf((0..1).random()) }; var f by remember { mutableStateOf((0..1).random()) }
    var n by remember { mutableStateOf(0) }; var ok by remember { mutableStateOf(0) }; val ch = listOf("<", ">")
    fun ans(x: Int) { if (x == c) ok++; n++; if (n >= 20) done(ok * 5.0) else { c = (0..1).random(); f = (0..1).random() } }
    Text("Direction of the MIDDLE arrow ($n/20)")
    Text(ch[f].repeat(2) + ch[c] + ch[f].repeat(2), fontSize = 40.sp)
    Row { Btn("◀ LEFT") { ans(0) }; Btn("RIGHT ▶") { ans(1) } }
}

// ---------- LOGIC ----------
fun slide(l: List<Int>): Pair<List<Int>, Int> { val t = l.filter { it > 0 }.toMutableList(); var sc = 0; var i = 0
    while (i < t.size - 1) { if (t[i] == t[i + 1]) { t[i] *= 2; sc += t[i]; t.removeAt(i + 1) }; i++ }
    while (t.size < 4) t.add(0); return t to sc }
fun move(b: List<Int>, d: Int): Pair<List<Int>, Int> { val o = b.toMutableList(); var sc = 0
    for (k in 0..3) { val idx = (0..3).map { when (d) { 0 -> k * 4 + it; 1 -> k * 4 + 3 - it; 2 -> it * 4 + k; else -> (3 - it) * 4 + k } }
        val (r, s) = slide(idx.map { b[it] }); sc += s; idx.forEachIndexed { j, p -> o[p] = r[j] } }
    return o to sc }

@Composable fun G2048(d: Int, done: (Double) -> Unit) {
    fun spawn(b: List<Int>): List<Int> { val e = b.indices.filter { b[it] == 0 }; if (e.isEmpty()) return b
        val o = b.toMutableList(); o[e.random()] = if ((0..99).random() < 10 + d * 15) 4 else 2; return o }
    var b by remember { mutableStateOf(spawn(spawn(List(16) { 0 }))) }; var sc by remember { mutableStateOf(0) }
    fun go(dir: Int) { val (nb, s) = move(b, dir); if (nb == b) return; sc += s; val n2 = spawn(nb); b = n2
        if ((0..3).all { move(n2, it).first == n2 }) done(sc.toDouble()) }
    Text("Score: $sc")
    Grid(4, 4) { i -> Cell(if (b[i] > 0) "${b[i]}" else "", if (b[i] > 0) Color.DarkGray else Color.Black, 64) }
    Btn("▲") { go(2) }; Row { Btn("◀") { go(0) }; Btn("▼") { go(3) }; Btn("▶") { go(1) } }
}

fun press(l: List<Boolean>, i: Int): List<Boolean> { val o = l.toMutableList(); val x = i % 5; val y = i / 5
    for ((dx, dy) in listOf(0 to 0, 1 to 0, -1 to 0, 0 to 1, 0 to -1)) { val nx = x + dx; val ny = y + dy; if (nx in 0..4 && ny in 0..4) o[ny * 5 + nx] = !o[ny * 5 + nx] }
    return o }

@Composable fun Lights(d: Int, done: (Double) -> Unit) {
    var b by remember { var l = List(25) { false }; repeat(4 + d * 4) { l = press(l, (0..24).random()) }; mutableStateOf(if (l.none { it }) press(l, 0) else l) }
    var mv by remember { mutableStateOf(0) }
    Text("Turn all lights off. Moves: $mv")
    Grid(5, 5) { i -> Cell(bg = if (b[i]) Color.White else Color.Black) { val nb = press(b, i); b = nb; mv++; if (nb.none { it }) done(mv.toDouble()) } }
}

fun sudoku(): List<Int> { val rr = (0..2).shuffled().flatMap { b -> (0..2).shuffled().map { b * 3 + it } }; val cc = (0..2).shuffled().flatMap { b -> (0..2).shuffled().map { b * 3 + it } }
    val dg = (1..9).shuffled(); return rr.flatMap { r -> cc.map { c -> dg[(3 * (r % 3) + r / 3 + c) % 9] } } }
fun sOk(b: List<Int>) = (0..8).all { k -> listOf((0..8).map { b[k * 9 + it] }, (0..8).map { b[it * 9 + k] }, (0..8).map { b[(k / 3 * 3 + it / 3) * 9 + (k % 3) * 3 + it % 3] }).all { g -> g.toSet().size == 9 && !g.contains(0) } }

@Composable fun Sudoku(d: Int, done: (Double) -> Unit) {
    val sol = remember { sudoku() }; val rm = remember { (0..80).shuffled().take(listOf(36, 45, 52)[d]).toSet() }; val t0 = remember { now() }
    var b by remember { mutableStateOf(sol.indices.map { if (it in rm) 0 else sol[it] }) }; var sel by remember { mutableStateOf(-1) }
    fun put(n: Int) { if (sel >= 0 && sel in rm) { b = b.toMutableList().also { it[sel] = n }; if (sOk(b)) done((now() - t0) / 1000.0) } }
    Grid(9, 9) { i -> Cell(if (b[i] > 0) "${b[i]}" else "", if (sel == i) Color.White else if (i in rm) Color.Black else Color.DarkGray, 34) { sel = i } }
    Row { (1..5).forEach { n -> Btn("$n") { put(n) } } }; Row { (6..9).forEach { n -> Btn("$n") { put(n) } }; Btn("⌫") { put(0) } }
}

@Composable fun Mines(d: Int, done: (Double) -> Unit) {
    val N = 8; val M = listOf(8, 12, 16)[d]
    var mines by remember { mutableStateOf(setOf<Int>()) }; var op by remember { mutableStateOf(setOf<Int>()) }
    var fl by remember { mutableStateOf(setOf<Int>()) }; var fm by remember { mutableStateOf(false) }
    fun nb(i: Int) = (-1..1).flatMap { dy -> (-1..1).map { dx -> dx to dy } }.filter { it != (0 to 0) }
        .map { (i % N + it.first) to (i / N + it.second) }.filter { it.first in 0 until N && it.second in 0 until N }.map { it.second * N + it.first }
    Btn(if (fm) "● FLAG MODE" else "FLAG MODE") { fm = !fm }
    Grid(N, N) { i ->
        Cell(if (i in op) { val c = nb(i).count { it in mines }; if (c > 0) "$c" else "·" } else if (i in fl) "⚑" else "", if (i in op) Color.DarkGray else Color.Black, 38) {
            if (fm) fl = if (i in fl) fl - i else fl + i
            else if (i !in fl && i !in op) {
                var mm = mines
                if (mm.isEmpty()) { mm = ((0 until N * N) - (listOf(i) + nb(i)).toSet()).shuffled().take(M).toSet(); mines = mm }
                if (i in mm) done(op.size * 100.0 / (N * N - M)) else {
                    val o = op.toMutableSet(); val q = ArrayDeque(listOf(i))
                    while (q.isNotEmpty()) { val x = q.removeFirst(); if (o.add(x) && nb(x).none { it in mm }) q.addAll(nb(x).filter { it !in o }) }
                    op = o; if (o.size == N * N - M) done(100.0)
                }
            }
        }
    }
}
