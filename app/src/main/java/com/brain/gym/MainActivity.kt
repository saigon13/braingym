package com.brain.gym

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ---------- Game registry: add a game = add a line here + a composable below ----------
class G(val id: String, val name: String, val mem: Boolean, val modes: List<String>, val lower: Boolean, val unit: String, val logic: Boolean = false)
val GAMES = listOf(
    G("num", "Number Memory", true, listOf("Digits", "Letters"), false, "level"),
    G("seq", "Sequence Memory", true, listOf("Forward", "Reverse"), false, "length"),
    G("chimp", "Chimp Test", true, listOf("Classic"), false, "numbers"),
    G("pairs", "Card Pairs", true, listOf("Emoji", "Numbers"), true, "moves"),
    G("nback", "N-Back", true, listOf("Position"), false, "% correct"),
    G("react", "Reaction Time", false, listOf("Single", "Average of 5"), true, "ms"),
    G("gonogo", "Go / No-Go", false, listOf("Classic"), false, "% correct"),
    G("stroop", "Stroop Test", false, listOf("Classic"), false, "/ 20"),
    G("schulte", "Schulte Table", false, listOf("Classic"), true, "sec"),
    G("matrix", "Memory Matrix", true, listOf("Classic"), false, "levels"),
    G("words", "Word Recall", true, listOf("Classic"), false, "points"),
    G("dual", "Dual N-Back", true, listOf("Classic"), false, "% correct"),
    G("cmatch", "Color Match", false, listOf("Classic"), false, "/ 20"),
    G("whack", "Whack-a-Mole", false, listOf("Classic"), false, "hits"),
    G("odd", "Odd One Out", false, listOf("Classic"), true, "sec"),
    G("flank", "Flanker Test", false, listOf("Classic"), false, "% correct"),
    G("g2048", "2048", false, listOf("Classic"), false, "points", true),
    G("lights", "Lights Out", false, listOf("Classic"), true, "moves", true),
    G("sudoku", "Sudoku", false, listOf("Classic"), true, "sec", true),
    G("mines", "Minesweeper", false, listOf("Classic"), false, "% cleared", true)
)

// ---------- Local storage: scores + history ----------
class Store(c: Context) {
    private val p = c.getSharedPreferences("s", 0)
    fun all() = (p.getString("l", "") ?: "").lines().filter { it.isNotBlank() }.map { it.split("|") }
    fun add(g: String, m: String, d: Int, s: Double) {
        val old = (p.getString("l", "") ?: "")
        p.edit().putString("l", (old + "\n$g|$m|$d|$s|${System.currentTimeMillis()}").trim()).apply()
    }
    fun hist(g: String) = all().filter { it[0] == g }.reversed()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        MobileAds.initialize(this)
        val st = Store(this)
        setContent {
            MaterialTheme(darkColorScheme(background = Color.Black, surface = Color.Black, primary = Color.White, onBackground = Color.White, onSurface = Color.White)) {
                Surface(Modifier.fillMaxSize(), color = Color.Black) { App(st) }
            }
        }
    }
}

@Composable fun Btn(t: String, f: () -> Unit) = OutlinedButton(onClick = f, modifier = Modifier.padding(4.dp)) { Text(t) }

@Composable fun Banner() = AndroidView(
    factory = { c -> AdView(c).apply { setAdSize(AdSize.BANNER); adUnitId = "ca-app-pub-3940256099942544/6300978111"; loadAd(AdRequest.Builder().build()) } }, // TEST banner id
    modifier = Modifier.fillMaxWidth().height(50.dp))

@Composable fun App(st: Store) {
    var tab by remember { mutableStateOf(0) }
    var g by remember { mutableStateOf<G?>(null) }
    var step by remember { mutableStateOf(0) }
    var mode by remember { mutableStateOf("") }
    var diff by remember { mutableStateOf(1) }
    var score by remember { mutableStateOf(0.0) }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
            val c = g
            if (c == null || step == 0) {
                Text("BRAIN GYM", fontSize = 28.sp)
                Row {
                    Btn((if (tab == 0) "● " else "") + "MEMORY") { tab = 0 }
                    Btn((if (tab == 1) "● " else "") + "REACTION") { tab = 1 }
                    Btn((if (tab == 2) "● " else "") + "LOGIC") { tab = 2 }
                }
                GAMES.filter { if (tab == 2) it.logic else !it.logic && it.mem == (tab == 0) }.forEach { x -> Btn(x.name) { g = x; mode = x.modes[0]; step = 1 } }
            } else when (step) {
                1 -> {
                    Text(c.name, fontSize = 22.sp)
                    Text("Mode"); Row { c.modes.forEach { m -> Btn((if (m == mode) "● " else "") + m) { mode = m } } }
                    Text("Difficulty"); Row { listOf("Easy", "Normal", "Hard").forEachIndexed { i, n -> Btn((if (i == diff) "● " else "") + n) { diff = i } } }
                    Btn("START") { step = 2 }; Btn("SCORES & HISTORY") { step = 4 }; Btn("BACK") { step = 0 }
                }
                2 -> {
                    val fin = { s: Double -> st.add(c.id, mode, diff, s); score = s; step = 3 }
                    when (c.id) {
                        "num" -> NumberMem(mode, diff, fin); "seq" -> SeqMem(mode, diff, fin); "chimp" -> Chimp(diff, fin)
                        "pairs" -> Pairs(mode, diff, fin); "nback" -> NBack(diff, fin); "react" -> Reaction(mode, fin)
                        "gonogo" -> GoNoGo(diff, fin); "stroop" -> Stroop(fin); "schulte" -> Schulte(diff, fin)
                        "matrix" -> Matrix(diff, fin); "words" -> Words(diff, fin); "dual" -> Dual(diff, fin); "cmatch" -> ColorMatch(fin)
                        "whack" -> Whack(diff, fin); "odd" -> Odd(diff, fin); "flank" -> Flanker(fin); "g2048" -> G2048(diff, fin)
                        "lights" -> Lights(diff, fin); "sudoku" -> Sudoku(diff, fin); "mines" -> Mines(diff, fin)
                    }
                }
                3 -> {
                    val best = st.hist(c.id).map { it[3].toDouble() }.let { if (c.lower) it.minOrNull() else it.maxOrNull() }
                    Text("Result", fontSize = 22.sp); Text("${"%.1f".format(score)} ${c.unit}", fontSize = 32.sp)
                    Text("Best: ${"%.1f".format(best ?: score)} ${c.unit}")
                    Btn("PLAY AGAIN") { step = 2 }; Btn("MENU") { step = 1 }
                }
                else -> {
                    Text("${c.name} history", fontSize = 20.sp)
                    val h = st.hist(c.id)
                    h.map { it[3].toDouble() }.let { if (it.isNotEmpty()) Text("Best: ${"%.1f".format(if (c.lower) it.min() else it.max())} ${c.unit}") }
                    h.forEach { Text("${java.text.SimpleDateFormat("dd MMM HH:mm").format(java.util.Date(it[4].toLong()))} · ${it[1]} · ${listOf("Easy", "Normal", "Hard")[it[2].toInt()]} · ${"%.1f".format(it[3].toDouble())}") }
                    Btn("BACK") { step = 1 }
                }
            }
        }
        Banner()
    }
}

// ---------- UI helpers ----------
@Composable fun Grid(r: Int, c: Int, f: @Composable (Int) -> Unit) = Column { repeat(r) { y -> Row { repeat(c) { x -> f(y * c + x) } } } }
@Composable fun Cell(t: String = "", bg: Color = Color.Black, sz: Int = 56, on: () -> Unit = {}) =
    Box(Modifier.padding(2.dp).size(sz.dp).background(bg).border(1.dp, Color.White).clickable { on() }, Alignment.Center) {
        Text(t, color = if (bg == Color.White) Color.Black else Color.White)
    }

// ---------- MEMORY GAMES ----------
@Composable fun NumberMem(m: String, d: Int, done: (Double) -> Unit) {
    var lvl by remember { mutableStateOf(2 + d) }; var s by remember { mutableStateOf("") }
    var ph by remember { mutableStateOf(0) }; var inp by remember { mutableStateOf("") }
    LaunchedEffect(ph, lvl) {
        if (ph == 0) { s = (1..lvl).map { if (m == "Letters") ('A'..'Z').random() else (0..9).random() }.joinToString(""); delay(800L + lvl * 400L); ph = 1 }
    }
    Text("Level ${lvl - 1 - d}")
    if (ph == 0) Text(s, fontSize = 40.sp) else {
        OutlinedTextField(inp, { inp = it }, singleLine = true)
        Btn("OK") { if (inp.trim().uppercase() == s) { lvl++; inp = ""; ph = 0 } else done((lvl - 2 - d).toDouble()) }
    }
}

@Composable fun SeqMem(m: String, d: Int, done: (Double) -> Unit) {
    var seq by remember { mutableStateOf(listOf<Int>()) }; var lit by remember { mutableStateOf(-1) }
    var pos by remember { mutableStateOf(0) }; var show by remember { mutableStateOf(true) }
    LaunchedEffect(seq.size) {
        if (seq.isEmpty()) { seq = listOf((0..8).random()); return@LaunchedEffect }
        show = true; pos = 0; delay(600)
        for (i in seq) { lit = i; delay(700L - d * 200); lit = -1; delay(200) }
        show = false
    }
    Text(if (show) "Watch..." else "Repeat (${seq.size})")
    Grid(3, 3) { i ->
        Cell(bg = if (lit == i) Color.White else Color.Black) {
            if (!show) {
                val exp = if (m == "Reverse") seq.reversed()[pos] else seq[pos]
                if (i == exp) { pos++; if (pos == seq.size) seq = seq + (0..8).random() } else done(seq.size - 1.0)
            }
        }
    }
}

@Composable fun Chimp(d: Int, done: (Double) -> Unit) {
    var n by remember { mutableStateOf(4 + d) }
    val pos = remember(n) { (0..24).shuffled().take(n) }
    var nx by remember(n) { mutableStateOf(1) }
    Text("Numbers: $n")
    Grid(5, 5) { i ->
        val k = pos.indexOf(i) + 1
        Cell(if (k > 0 && nx == 1) "$k" else "", if (k > 0 && k >= nx) Color.White else Color.Black) {
            if (k > 0) { if (k == nx) { nx++; if (nx > n) n++ } else done(n.toDouble()) }
        }
    }
}

@Composable fun Pairs(m: String, d: Int, done: (Double) -> Unit) {
    val n = listOf(6, 8, 10)[d]
    val deck = if (m == "Numbers") (1..10).map { "$it" } else listOf("🍎", "🍌", "🍇", "🍒", "🍋", "🍉", "🥝", "🍑", "🍓", "🍍")
    val items = remember { (deck.take(n) + deck.take(n)).shuffled() }
    var open by remember { mutableStateOf(listOf<Int>()) }; var ma by remember { mutableStateOf(setOf<Int>()) }
    var mv by remember { mutableStateOf(0) }; val sc = rememberCoroutineScope()
    Text("Moves: $mv")
    Grid(n / 2, 4) { i ->
        Cell(if (i in open || i in ma) items[i] else "") {
            if (i !in open && i !in ma && open.size < 2) {
                open = open + i
                if (open.size == 2) sc.launch {
                    mv++; delay(600)
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
    val st = remember { val l = mutableListOf<Int>(); repeat(20 + n) { i -> l.add(if (i >= n && (0..2).random() == 0) l[i - n] else (0..8).random()) }; l }
    var i by remember { mutableStateOf(0) }; var tap by remember { mutableStateOf(false) }; var ok by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        for (k in st.indices) { i = k; tap = false; delay(1800); if ((k >= n && st[k] == st[k - n]) == tap) ok++ }
        done(ok * 100.0 / st.size)
    }
    Text("$n-back: tap MATCH if the square equals the one $n step(s) ago")
    Grid(3, 3) { Cell(bg = if (st[i] == it) Color.White else Color.Black) }
    Btn("MATCH") { tap = true }
}

// ---------- REACTION GAMES ----------
@Composable fun Reaction(m: String, done: (Double) -> Unit) {
    val total = if (m == "Single") 1 else 5
    var ph by remember { mutableStateOf(0) }; var t0 by remember { mutableStateOf(0L) }
    var res by remember { mutableStateOf(listOf<Long>()) }; var round by remember { mutableStateOf(0) }
    LaunchedEffect(round) { ph = 0; delay((1500..4000).random().toLong()); t0 = System.currentTimeMillis(); ph = 1 }
    Text("Tap when green (${res.size}/$total). Early tap restarts.")
    Box(Modifier.fillMaxWidth().height(300.dp).background(if (ph == 1) Color(0xFF1B8A3A) else Color.DarkGray).clickable {
        if (ph == 1) { val r = res + (System.currentTimeMillis() - t0); res = r; if (r.size >= total) done(r.average()) else round++ } else round++
    })
}

@Composable fun GoNoGo(d: Int, done: (Double) -> Unit) {
    var go by remember { mutableStateOf(true) }; var tap by remember { mutableStateOf(false) }
    var shown by remember { mutableStateOf(false) }; var ok by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        repeat(20) { go = (0..2).random() > 0; tap = false; shown = true; delay(1000L - d * 250); shown = false; if (go == tap) ok++; delay(400) }
        done(ok * 5.0)
    }
    Text("Tap on GREEN only. Never tap RED.")
    Box(Modifier.fillMaxWidth().height(300.dp).background(if (!shown) Color.Black else if (go) Color(0xFF1B8A3A) else Color.Red).border(1.dp, Color.White).clickable { tap = true })
}

@Composable fun Stroop(done: (Double) -> Unit) {
    val C = listOf("RED" to Color.Red, "GREEN" to Color(0xFF2ECC71), "BLUE" to Color(0xFF4DA3FF), "YELLOW" to Color.Yellow)
    var a by remember { mutableStateOf((0..3).random()) }; var b by remember { mutableStateOf((0..3).random()) }
    var n by remember { mutableStateOf(0) }; var ok by remember { mutableStateOf(0) }
    Text("Pick the INK colour (${n}/20)")
    Text(C[a].first, color = C[b].second, fontSize = 48.sp)
    Row { C.forEachIndexed { i, c -> Btn(c.first) {
        if (i == b) ok++; n++
        if (n >= 20) done(ok.toDouble()) else { a = (0..3).random(); b = (0..3).random() }
    } } }
}

@Composable fun Schulte(d: Int, done: (Double) -> Unit) {
    val s = 3 + d
    val nums = remember { (1..s * s).shuffled() }
    var nx by remember { mutableStateOf(1) }; val t0 = remember { System.currentTimeMillis() }
    Text("Tap 1 → ${s * s} in order. Next: $nx")
    Grid(s, s) { i -> Cell("${nums[i]}") { if (nums[i] == nx) { nx++; if (nx > s * s) done((System.currentTimeMillis() - t0) / 1000.0) } } }
}
