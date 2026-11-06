package com.brain.gym

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

fun seqGen(n: Int, t: Int, r: Int): List<Int> {
    val base = bagList(r, t).toMutableList()
    for (i in n until t) if ((0..2).random() == 0) base[i] = base[i - n]
    return base
}
val CL = listOf("RED" to Color.Red, "GREEN" to Color(0xFF2ECC71), "BLUE" to Color(0xFF4DA3FF), "YELLOW" to Color.Yellow, "ORANGE" to Color(0xFFFF9800), "PURPLE" to Color(0xFFBA68C8))
val WORDS = "apple river chair cloud tiger pencil window garden bridge candle mirror ocean violin rocket basket forest anchor pillow lantern button castle dragon engine feather guitar hammer island jungle kettle ladder magnet needle orange planet quilt ribbon saddle tunnel umbrella valley wagon yogurt zipper".split(" ")

// ---------- MEMORY ----------
@Composable fun Matrix(d: Int, done: (Double) -> Unit) {
    var lv by remember { mutableStateOf(1) }
    val gs = minOf(6, 3 + (lv - 1) / 3 + d / 2)
    val n = minOf(gs * gs / 2, gs - 1 + (lv - 1) % 3 + d / 2)
    val tg = remember(lv) { (0 until gs * gs).shuffled().take(n) }
    var show by remember(lv) { mutableStateOf(true) }; var hit by remember(lv) { mutableStateOf(setOf<Int>()) }
    LaunchedEffect(lv) { wait(1200L + n * 100L); show = false }
    Status("Level $lv · ${gs}×$gs grid · $n tiles")
    Grid(gs, gs) { i -> Cell(bg = if (((show || Ended.on) && i in tg) || i in hit) Color.White else Color.Black) {
        if (!show && !Ended.on && i !in hit) { if (i in tg) { hit = hit + i; if (hit.size == n) { Sfx.good(); lv++ } } else done(lv - 1.0) }
    } }
}

@Composable fun Words(d: Int, done: (Double) -> Unit) {
    var lv by remember { mutableStateOf(1) }
    val k = spanLen(d, lv, intArrayOf(3, 4, 6, 8), intArrayOf(4, 6, 8, 12))
    val all = remember(lv) { WORDS.shuffled() }; val seen = all.take(k); val opts = remember(lv) { all.take(k + 4).shuffled() }
    var ph by remember(lv) { mutableStateOf(0) }; var sel by remember(lv) { mutableStateOf(setOf<String>()) }
    LaunchedEffect(lv) { wait(k * 1000L); ph = 1 }
    Status("Level $lv · $k words")
    if (ph == 0) Text(seen.joinToString("\n"), fontSize = 24.sp, fontWeight = FontWeight.Medium) else {
        opts.forEach { w -> Opt((if (Ended.on && w in seen) "✓ " else if (w in sel) "● " else "") + w) { if (!Ended.on) sel = if (w in sel) sel - w else sel + w } }
        Btn("SUBMIT") { if (sel == seen.toSet()) { Sfx.good(); lv++ } else done(lv - 1.0) }
    }
}

@Composable fun Dual(d: Int, done: (Double) -> Unit) {
    val n = d + 1; val L = "CHKLQRST"
    val ps = remember { seqGen(n, 20 + n, 9) }; val ls = remember { seqGen(n, 20 + n, 8) }
    var i by remember { mutableStateOf(0) }; var tp by remember { mutableStateOf(false) }; var tl by remember { mutableStateOf(false) }; var ok by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        for (k in ps.indices) { i = k; tp = false; tl = false; wait(2000)
            if ((k >= n && ps[k] == ps[k - n]) == tp) ok++; if ((k >= n && ls[k] == ls[k - n]) == tl) ok++ }
        done(ok * 50.0 / ps.size)
    }
    Status("$n-back · letter ${L[ls[i]]} · trial ${i + 1}/${ps.size}")
    Grid(3, 3) { Cell(bg = if (ps[i] == it) (if (i % 2 == 0) Color.White else Color(0xFFFFB74D)) else Color.Black) }
    Row { Btn("POSITION MATCH") { tp = true }; Btn("LETTER MATCH") { tl = true } }
}

@Composable fun Corsi(d: Int, done: (Double) -> Unit) {
    val pool = remember { bagList(16, 60) }
    var n by remember { mutableStateOf(1) }; var lit by remember { mutableStateOf(-1) }
    var pos by remember { mutableStateOf(0) }; var show by remember { mutableStateOf(true) }
    val seq = pool.take(n)
    LaunchedEffect(n) {
        show = true; pos = 0; wait(600)
        for (i in seq) { lit = i; wait(maxOf(220L, 750L - d * 200)); lit = -1; wait(250) }
        show = false
    }
    Status(if (show) "Watch... · length $n" else "Repeat · length $n")
    Grid(4, 4) { i -> Cell(bg = if (lit == i) Color.White else Color.Black) {
        if (!show) { if (i == seq[pos]) { pos++; if (pos == seq.size) { Sfx.good(); n++ } } else done(n - 1.0) }
    } }
}

@Composable fun RevSpan(d: Int, done: (Double) -> Unit) {
    var lv by remember { mutableStateOf(1) }; var s by remember { mutableStateOf("") }
    var ph by remember { mutableStateOf(0) }; var inp by remember { mutableStateOf("") }
    val len = spanLen(d, lv, intArrayOf(2, 3, 4, 6), intArrayOf(3, 5, 7, 10))
    LaunchedEffect(ph, lv) { if (ph == 0) { s = rndStr("0123456789", len); wait(800L + len * 450L); ph = 1 } }
    Status("Level $lv · $len digits · type it BACKWARDS")
    if (ph == 0 || Ended.on) { if (Ended.on) Text("It was (backwards: ${s.reversed()})", color = Color.Gray); BigText(s) }
    if (ph == 1) {
        TypedBox(inp)
        if (!Ended.on) Keypad("num") { k -> when (k) {
            "⌫" -> { inp = inp.dropLast(1) }
            "✓" -> { if (inp == s.reversed()) { Sfx.good(); lv++; inp = ""; ph = 0 } else done(lv - 1.0) }
            else -> { if (inp.length < s.length) inp += k }
        } }
    }
}

fun dk(s: String) = s.split(",").map { val p = it.split(":"); p[0] to p[1] }
fun roman(n: Int): String {
    var x = n; val v = intArrayOf(50, 40, 10, 9, 5, 4, 1); val s = arrayOf("L", "XL", "X", "IX", "V", "IV", "I"); val sb = StringBuilder()
    for (i in v.indices) while (x >= v[i]) { sb.append(s[i]); x -= v[i] }
    return sb.toString()
}
val DECKS: Map<String, List<Pair<String, String>>> = mapOf(
    "Capitals" to dk("France:Paris,Japan:Tokyo,Italy:Rome,Egypt:Cairo,Canada:Ottawa,Brazil:Brasília,India:New Delhi,Spain:Madrid,Kenya:Nairobi,Peru:Lima,Germany:Berlin,Australia:Canberra,China:Beijing,Russia:Moscow,Turkey:Ankara,Argentina:Buenos Aires,Mexico:Mexico City,Nigeria:Abuja,Thailand:Bangkok,Vietnam:Hanoi,Greece:Athens,Portugal:Lisbon,Norway:Oslo,Sweden:Stockholm,Poland:Warsaw,Chile:Santiago,Colombia:Bogotá,Morocco:Rabat,Ghana:Accra,Iran:Tehran,Iraq:Baghdad,Pakistan:Islamabad,Nepal:Kathmandu,Sri Lanka:Colombo,Indonesia:Jakarta,Philippines:Manila,South Korea:Seoul,Cuba:Havana,Austria:Vienna,Ireland:Dublin"),
    "Elements" to dk("H:Hydrogen,He:Helium,Li:Lithium,Be:Beryllium,B:Boron,C:Carbon,N:Nitrogen,O:Oxygen,F:Fluorine,Ne:Neon,Na:Sodium,Mg:Magnesium,Al:Aluminium,Si:Silicon,P:Phosphorus,S:Sulfur,Cl:Chlorine,Ar:Argon,K:Potassium,Ca:Calcium,Fe:Iron,Cu:Copper,Zn:Zinc,Ag:Silver,Sn:Tin,Au:Gold,Hg:Mercury,Pb:Lead,U:Uranium,I:Iodine"),
    "Spanish" to dk("hello:hola,goodbye:adiós,please:por favor,thank you:gracias,yes:sí,no:no,water:agua,food:comida,house:casa,dog:perro,cat:gato,book:libro,friend:amigo,mother:madre,father:padre,red:rojo,blue:azul,green:verde,one:uno,two:dos,three:tres,four:cuatro,five:cinco,day:día,night:noche,sun:sol,moon:luna,city:ciudad,big:grande,small:pequeño"),
    "Word power" to dk("ephemeral:short-lived,benevolent:kind and generous,ubiquitous:found everywhere,candid:honest and open,resilient:able to recover quickly,meticulous:very careful,pragmatic:practical,eloquent:fluent and persuasive,ambiguous:unclear in meaning,diligent:hardworking,frugal:careful with money,lucid:clear and easy to understand,obsolete:no longer used,reluctant:unwilling,scarce:in short supply,tranquil:calm and peaceful,vivid:bright and clear,zealous:very enthusiastic,abundant:plentiful,inevitable:certain to happen"),
    "Science" to dk("Closest planet to the Sun:Mercury,Largest planet:Jupiter,Gas plants absorb:Carbon dioxide,Gas we breathe in:Oxygen,Powerhouse of the cell:Mitochondria,Chemical symbol for water:H2O,Hardest natural substance:Diamond,Force that pulls objects down:Gravity,Speed of light (km/s):300000,Boiling point of water (°C):100,Bones in an adult body:206,The Red Planet:Mars,Largest organ of the body:Skin,Center of an atom:Nucleus,Unit of electric current:Ampere,Study of living things:Biology,Closest star to Earth:The Sun,Chemical symbol for gold:Au,Layer of air around Earth:Atmosphere,Liquid metal at room temperature:Mercury"),
    "Times tables" to (2..12).flatMap { a -> (2..12).map { b -> "$a × $b" to "${a * b}" } },
    "Squares" to (3..30).map { "$it²" to "${it * it}" },
    "Roman numerals" to (4..50).map { "$it" to roman(it) },
    "Binary" to (3..63).map { "$it" to Integer.toBinaryString(it) }
)

@Composable fun Flash(m: String, d: Int, done: (Double) -> Unit) {
    val base = remember { DECKS[m]!!.shuffled().take(listOf(10, 20, 30, 40)[d]) }
    val q = remember { mutableStateListOf<Pair<String, String>>().apply { addAll(base) } }
    val seen = remember { mutableSetOf<String>() }
    var sh by remember { mutableStateOf(false) }; var fk by remember { mutableStateOf(0) }
    val ac = LocalAccent.current
    fun answer(y: Boolean) {
        val c = q.removeAt(0)
        if (seen.add(c.first) && y) fk++
        if (!y) q.add(c)
        sh = false
        if (q.isEmpty()) done(fk * 100.0 / base.size)
    }
    if (q.isNotEmpty()) {
        Status("$m · ${q.size} left · known first try: $fk")
        Box(Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(24.dp)).background(Color(0xFF1E1E24)).border(1.dp, ac.copy(.6f), RoundedCornerShape(24.dp)).fast(null) { Sfx.click(); sh = true }, Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(q[0].first, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                if (sh) { Spacer(Modifier.height(14.dp)); Text(q[0].second, fontSize = 26.sp, color = ac) }
                else { Spacer(Modifier.height(14.dp)); Text("Think of the answer, then tap to flip", fontSize = 12.sp, color = Color.Gray) }
            }
        }
        Spacer(Modifier.height(12.dp))
        if (sh) Row { Btn("✓  Knew it") { answer(true) }; Btn("↻  Again") { answer(false) } }
    }
}

// ---------- REACTION ----------
@Composable fun ColorMatch(d: Int, done: (Double) -> Unit) {
    val k = if (d == 3) 6 else 4; val N = if (d == 3) 30 else 20
    val al = remember { bagList(k, N) }; val mf = remember { List(N) { it % 2 == 0 }.shuffled() }
    val bl = remember { al.mapIndexed { i, a -> if (mf[i]) a else ((0 until k) - a).random() } }
    var n by remember { mutableStateOf(0) }; var ok by remember { mutableStateOf(0) }; val t0 = remember { now() }
    val a = al[minOf(n, N - 1)]; val b = bl[minOf(n, N - 1)]
    fun ans(y: Boolean) { if ((a == b) == y) { ok++; Sfx.good() } else Sfx.bad(); n++; if (n >= N) done(speedScore(ok, N, t0)) }
    Status("Correct $ok · ${minOf(n + 1, N)}/$N · does the WORD match its ink?")
    Text(CL[a].first, color = CL[b].second, fontSize = 56.sp, fontWeight = FontWeight.Black)
    Spacer(Modifier.height(16.dp))
    Row { Opt("YES") { ans(true) }; Opt("NO") { ans(false) } }
}

@Composable fun Whack(d: Int, done: (Double) -> Unit) {
    val order = remember { bagList(9, 80) }
    var m by remember { mutableStateOf(-1) }; var hit by remember { mutableStateOf(0) }; var left by remember { mutableStateOf(30) }
    val step = longArrayOf(850, 650, 480, 340)[d]
    LaunchedEffect(Unit) {
        val cnt = (30000 / step).toInt()
        for (k in 0 until cnt) { m = order[k]; left = ((cnt - k) * step / 1000).toInt(); wait(step) }
        done(hit.toDouble())
    }
    Status("Hits: $hit · ${left}s left")
    Grid(3, 3) { i -> Box(Modifier.padding(7.dp)) { Cell(bg = if (m == i) Color.White else Color.Black) { if (m == i) { hit++; Sfx.good(); m = -1 } } } }
}

val OL = listOf("O" to "Q", "E" to "F", "M" to "N", "b" to "d", "p" to "q", "C" to "G", "5" to "S", "8" to "B", "W" to "V", "H" to "N", "U" to "V", "R" to "K", "T" to "Y", "2" to "Z", "0" to "D")
val OS = listOf("▲" to "△", "●" to "○", "■" to "□", "◆" to "◇", "★" to "☆")
val OD = listOf("↑" to "↗", "→" to "↘", "←" to "↖", "↓" to "↙")
val ON = listOf("68" to "86", "19" to "91", "37" to "73", "25" to "52", "46" to "64", "58" to "85")

@Composable fun Odd(d: Int, done: (Double) -> Unit) {
    val s = 4 + d; var r by remember { mutableStateOf(0) }; var pen by remember { mutableStateOf(0) }; val t0 = remember { now() }
    val types = remember { bagList(5, 12) }
    val lp = remember { OL.shuffled() }; val sp = remember { OS.shuffled() }; val dp = remember { OD.shuffled() }; val np = remember { ON.shuffled() }
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

@Composable fun Flanker(d: Int, done: (Double) -> Unit) {
    val N = if (d == 3) 30 else 20
    val cs = remember { bagList(2, N) }; val fs = remember { List(N) { if (d == 3) 1 else it % 2 }.shuffled() }
    var n by remember { mutableStateOf(0) }; var ok by remember { mutableStateOf(0) }; val t0 = remember { now() }; val ch = listOf("<", ">")
    val c = cs[minOf(n, N - 1)]; val f = if (fs[minOf(n, N - 1)] == 0) c else 1 - c
    fun ans(x: Int) { if (x == c) { ok++; Sfx.good() } else Sfx.bad(); n++; if (n >= N) done(speedScore(ok, N, t0)) }
    Status("Correct $ok · ${minOf(n + 1, N)}/$N · middle arrow only")
    Text(ch[f].repeat(2) + ch[c] + ch[f].repeat(2), fontSize = 56.sp, fontWeight = FontWeight.Black, letterSpacing = 6.sp)
    Spacer(Modifier.height(16.dp))
    Row { Opt("◀  LEFT") { ans(0) }; Opt("RIGHT  ▶") { ans(1) } }
}

// ---------- LOGIC ----------
fun slide(l: List<Int>): Pair<List<Int>, Int> {
    val t = l.filter { it > 0 }.toMutableList(); var sc = 0; var i = 0
    while (i < t.size - 1) { if (t[i] == t[i + 1]) { t[i] *= 2; sc += t[i]; t.removeAt(i + 1) }; i++ }
    while (t.size < 4) t.add(0); return t to sc
}
fun move(b: List<Int>, d: Int): Pair<List<Int>, Int> {
    val o = b.toMutableList(); var sc = 0
    for (k in 0..3) {
        val idx = (0..3).map { when (d) { 0 -> k * 4 + it; 1 -> k * 4 + 3 - it; 2 -> it * 4 + k; else -> (3 - it) * 4 + k } }
        val (r, s) = slide(idx.map { b[it] }); sc += s; idx.forEachIndexed { j, p -> o[p] = r[j] }
    }
    return o to sc
}

@Composable fun G2048(d: Int, done: (Double) -> Unit) {
    fun spawn(b: List<Int>): List<Int> {
        val e = b.indices.filter { b[it] == 0 }; if (e.isEmpty()) return b
        val o = b.toMutableList(); o[e.random()] = if ((0..99).random() < 10 + d * 15) 4 else 2; return o
    }
    var b by remember { mutableStateOf(spawn(spawn(List(16) { 0 }))) }; var sc by remember { mutableStateOf(0) }
    fun go(dir: Int) {
        val (nb, s) = move(b, dir); if (nb == b) return
        if (s > 0) Sfx.good() else Sfx.tick()
        sc += s; val n2 = spawn(nb); b = n2
        if ((0..3).all { move(n2, it).first == n2 }) done(sc.toDouble())
    }
    Status("Score: $sc · swipe the board")
    Box(Modifier.clip(RoundedCornerShape(18.dp)).background(Color(0xFF16161A)).padding(4.dp).pointerInput(Unit) {
        var tx = 0f; var ty = 0f
        detectDragGestures(
            onDragStart = { tx = 0f; ty = 0f },
            onDragEnd = { if (abs(tx) > abs(ty)) { if (abs(tx) > 40) go(if (tx < 0) 0 else 1) } else if (abs(ty) > 40) go(if (ty < 0) 2 else 3) },
            onDrag = { c, a -> c.consume(); tx += a.x; ty += a.y })
    }) {
        Grid(4, 4) { i ->
            val v = b[i]; val s = LocalCell.current
            Box(Modifier.padding(3.dp).size((s - 6).dp).clip(RoundedCornerShape(12.dp))
                .background(if (v == 0) Color(0xFF26262B) else Color.hsv((Integer.numberOfTrailingZeros(v) * 32f) % 360f, .55f, .9f)), Alignment.Center) {
                if (v > 0) Text("$v", color = Color(0xFF111111), fontWeight = FontWeight.Black, fontSize = (s / 3.2).sp)
            }
        }
    }
}

fun press(l: List<Boolean>, i: Int): List<Boolean> {
    val o = l.toMutableList(); val x = i % 5; val y = i / 5
    for ((dx, dy) in listOf(0 to 0, 1 to 0, -1 to 0, 0 to 1, 0 to -1)) { val nx = x + dx; val ny = y + dy; if (nx in 0..4 && ny in 0..4) o[ny * 5 + nx] = !o[ny * 5 + nx] }
    return o
}

@Composable fun Lights(d: Int, done: (Double) -> Unit) {
    var b by remember { var l = List(25) { false }; repeat(4 + d * 4) { l = press(l, (0..24).random()) }; mutableStateOf(if (l.none { it }) press(l, 0) else l) }
    var mv by remember { mutableStateOf(0) }
    Status("Turn all lights off · Moves: $mv")
    Grid(5, 5) { i -> Cell(bg = if (b[i]) Color.White else Color.Black) { val nb = press(b, i); b = nb; mv++; if (nb.none { it }) done(mv.toDouble()) } }
}

fun sudoku(): List<Int> {
    val rr = (0..2).shuffled().flatMap { b -> (0..2).shuffled().map { b * 3 + it } }; val cc = (0..2).shuffled().flatMap { b -> (0..2).shuffled().map { b * 3 + it } }
    val dg = (1..9).shuffled(); return rr.flatMap { r -> cc.map { c -> dg[(3 * (r % 3) + r / 3 + c) % 9] } }
}
fun sOk(b: List<Int>) = (0..8).all { k -> listOf((0..8).map { b[k * 9 + it] }, (0..8).map { b[it * 9 + k] }, (0..8).map { b[(k / 3 * 3 + it / 3) * 9 + (k % 3) * 3 + it % 3] }).all { g -> g.toSet().size == 9 && !g.contains(0) } }

@Composable fun Sudoku(d: Int, done: (Double) -> Unit) {
    val sol = remember { sudoku() }; val rm = remember { (0..80).shuffled().take(listOf(36, 45, 52, 58)[d]).toSet() }; val t0 = remember { now() }
    var b by remember { mutableStateOf(sol.indices.map { if (it in rm) 0 else sol[it] }) }; var sel by remember { mutableStateOf(-1) }
    fun put(n: Int) { if (sel >= 0 && sel in rm) { b = b.toMutableList().also { it[sel] = n }; if (sOk(b)) done((now() - t0) / 1000.0) } }
    Status("Tap a square, then a number")
    Grid(9, 9) { i -> Cell(if (b[i] > 0) "${b[i]}" else "", if (sel == i) Color.White else if (i in rm) Color.Black else Color.DarkGray) { sel = i } }
    Spacer(Modifier.height(10.dp))
    Keypad("sud") { k -> put(if (k == "⌫") 0 else k.toInt()) }
}

@Composable fun Mines(d: Int, done: (Double) -> Unit) {
    val N = 8; val M = listOf(8, 12, 16, 20)[d]
    var mines by remember { mutableStateOf(setOf<Int>()) }; var op by remember { mutableStateOf(setOf<Int>()) }
    var fl by remember { mutableStateOf(setOf<Int>()) }
    fun nb(i: Int) = (-1..1).flatMap { dy -> (-1..1).map { dx -> dx to dy } }.filter { it != (0 to 0) }
        .map { (i % N + it.first) to (i / N + it.second) }.filter { it.first in 0 until N && it.second in 0 until N }.map { it.second * N + it.first }
    Status("TAP = dig · HOLD = flag · Mines $M · Flags ${fl.size}")
    Grid(N, N) { i ->
        val label = if (i in op) { val c = nb(i).count { it in mines }; if (c > 0) "$c" else "" } else if (i in fl) "⚑" else if (Ended.on && i in mines) "✹" else ""
        Cell(label, if (i in op) Color.DarkGray else if (Ended.on && i in mines) ROSE else Color.Black, onLong = { if (i !in op && !Ended.on) { fl = if (i in fl) fl - i else fl + i } }) {
            if (i !in fl && i !in op && !Ended.on) {
                var mm = mines
                if (mm.isEmpty()) { mm = ((0 until N * N) - (listOf(i) + nb(i)).toSet()).shuffled().take(M).toSet(); mines = mm }
