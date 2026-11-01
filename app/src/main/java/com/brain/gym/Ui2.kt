package com.brain.gym

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

// ---------- New games ----------
val DECKS = mapOf(
    "Capitals" to listOf("France" to "Paris", "Japan" to "Tokyo", "Italy" to "Rome", "Egypt" to "Cairo", "Canada" to "Ottawa", "Brazil" to "Brasília", "India" to "New Delhi", "Spain" to "Madrid", "Kenya" to "Nairobi", "Peru" to "Lima"),
    "Science" to listOf("H2O" to "Water", "NaCl" to "Salt", "CO2" to "Carbon dioxide", "Fe" to "Iron", "Au" to "Gold", "Closest planet to the Sun" to "Mercury", "Largest planet" to "Jupiter", "Plants take in" to "CO2", "Powerhouse of the cell" to "Mitochondria", "Speed of light (km/s)" to "300,000"),
    "Times tables" to (3..12).map { "$it × 7" to "${it * 7}" }
)

val GAMES2 = GAMES + listOf(
    G("corsi", "Corsi Blocks", true, listOf("Classic"), false, "length"),
    G("rev", "Reverse Span", true, listOf("Classic"), false, "level"),
    G("flash", "Flash Cards", true, DECKS.keys.toList(), false, "% known"),
    G("chess", "Chess vs Bot", false, listOf("Play White", "Play Black", "Random side"), false, "points", true)
)

fun ic(id: String) = when (id) {
    "num" -> "🔢"; "seq" -> "🔷"; "chimp" -> "🐒"; "pairs" -> "🃏"; "nback" -> "🧠"; "matrix" -> "🟦"; "words" -> "📝"
    "dual" -> "🧩"; "corsi" -> "🔲"; "rev" -> "🔁"; "flash" -> "📚"; "react" -> "⚡"; "gonogo" -> "🚦"; "stroop" -> "🎨"
    "schulte" -> "🔍"; "cmatch" -> "🌈"; "whack" -> "🔨"; "odd" -> "👀"; "flank" -> "↔️"; "g2048" -> "🧮"
    "lights" -> "💡"; "sudoku" -> "9️⃣"; "mines" -> "💣"; else -> "♞"
}

@Composable fun Corsi(d: Int, done: (Double) -> Unit) {
    var seq by remember { mutableStateOf(listOf<Int>()) }; var lit by remember { mutableStateOf(-1) }
    var pos by remember { mutableStateOf(0) }; var show by remember { mutableStateOf(true) }
    LaunchedEffect(seq.size) {
        if (seq.isEmpty()) { seq = listOf((0..15).random()); return@LaunchedEffect }
        show = true; pos = 0; delay(600)
        for (i in seq) { lit = i; delay(750L - d * 200); lit = -1; delay(250) }
        show = false
    }
    Text(if (show) "Watch..." else "Repeat (${seq.size})")
    Grid(4, 4) { i -> Cell(bg = if (lit == i) Color.White else Color.Black, sz = 60) {
        if (!show) { if (i == seq[pos]) { pos++; if (pos == seq.size) seq = seq + ((0..15) - seq.toSet()).random() } else done(seq.size - 1.0) }
    } }
}

@Composable fun RevSpan(d: Int, done: (Double) -> Unit) {
    var lvl by remember { mutableStateOf(2 + d) }; var s by remember { mutableStateOf("") }
    var ph by remember { mutableStateOf(0) }; var inp by remember { mutableStateOf("") }
    LaunchedEffect(ph, lvl) { if (ph == 0) { s = (1..lvl).map { (0..9).random() }.joinToString(""); delay(800L + lvl * 400L); ph = 1 } }
    Text("Type it BACKWARDS")
    if (ph == 0) Text(s, fontSize = 40.sp) else {
        OutlinedTextField(inp, { inp = it }, singleLine = true)
        Btn("OK") { if (inp.trim() == s.reversed()) { lvl++; inp = ""; ph = 0 } else done((lvl - 2 - d).toDouble()) }
    }
}

@Composable fun Flash(m: String, d: Int, done: (Double) -> Unit) {
    val cards = remember { DECKS[m]!!.shuffled().take(6 + d * 2) }
    var i by remember { mutableStateOf(0) }; var sh by remember { mutableStateOf(false) }; var k by remember { mutableStateOf(0) }
    fun nxt(y: Boolean) { if (y) k++; if (i + 1 >= cards.size) done(k * 100.0 / cards.size) else { i++; sh = false } }
    val (q, a) = cards[i]
    Text("Card ${i + 1}/${cards.size}", color = Color.Gray)
    Text(q, fontSize = 32.sp, textAlign = TextAlign.Center)
    if (!sh) Btn("REVEAL") { sh = true } else {
        Text(a, fontSize = 28.sp, color = Color(0xFF4DD0E1))
        Row { Btn("✓ KNEW IT") { nxt(true) }; Btn("✗ DIDN'T") { nxt(false) } }
    }
}

// ---------- New UI ----------
@Composable fun Sp(h: Int) = Spacer(Modifier.height(h.dp))

@Composable fun Chip(t: String, sel: Boolean, ac: Color, m: Modifier = Modifier, f: () -> Unit) =
    Button(f, m, shape = RoundedCornerShape(12.dp), contentPadding = PaddingValues(horizontal = 8.dp),
        colors = ButtonDefaults.buttonColors(containerColor = if (sel) ac else Color(0xFF1C1C1C), contentColor = if (sel) Color.Black else Color.White)) {
        Text(t, fontSize = 13.sp, maxLines = 1)
    }

@Composable fun Big(t: String, ac: Color, f: () -> Unit) =
    Button(f, Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = ac, contentColor = Color.Black)) { Text(t, fontWeight = FontWeight.Bold) }

@Composable fun Tile(x: G, ac: Color, st: Store, on: () -> Unit) {
    val b = st.hist(x.id).map { it[3].toDouble() }.let { if (x.lower) it.minOrNull() else it.maxOrNull() }
    Box(Modifier.aspectRatio(1f).clip(RoundedCornerShape(18.dp))
        .background(Brush.verticalGradient(listOf(Color(0xFF262626), Color(0xFF101010))))
        .border(1.dp, ac.copy(alpha = 0.35f), RoundedCornerShape(18.dp)).clickable { on() }.padding(6.dp), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(ic(x.id), fontSize = 28.sp)
            Text(x.name, fontSize = 11.sp, textAlign = TextAlign.Center, maxLines = 2, lineHeight = 13.sp)
            if (b != null) Text("★ ${"%.0f".format(b)}", color = ac, fontSize = 11.sp)
        }
    }
}

@Composable fun Play(c: G, m: String, d: Int, f: (Double) -> Unit) {
    when (c.id) {
        "num" -> NumberMem(m, d, f); "seq" -> SeqMem(m, d, f); "chimp" -> Chimp(d, f); "pairs" -> Pairs(m, d, f)
        "nback" -> NBack(d, f); "react" -> Reaction(m, f); "gonogo" -> GoNoGo(d, f); "stroop" -> Stroop(f); "schulte" -> Schulte(d, f)
        "matrix" -> Matrix(d, f); "words" -> Words(d, f); "dual" -> Dual(d, f); "cmatch" -> ColorMatch(f); "whack" -> Whack(d, f)
        "odd" -> Odd(d, f); "flank" -> Flanker(f); "g2048" -> G2048(d, f); "lights" -> Lights(d, f); "sudoku" -> Sudoku(d, f)
        "mines" -> Mines(d, f); "corsi" -> Corsi(d, f); "rev" -> RevSpan(d, f); "flash" -> Flash(m, d, f); "chess" -> ChessGame(m, d, f)
    }
}

@Composable fun AppV2(st: Store) {
    var tab by remember { mutableStateOf(0) }
    var g by remember { mutableStateOf<G?>(null) }
    var step by remember { mutableStateOf(0) }
    var mode by remember { mutableStateOf("") }
    var diff by remember { mutableStateOf(1) }
    var score by remember { mutableStateOf(0.0) }
    var rec by remember { mutableStateOf(false) }
    val AC = listOf(Color(0xFF4DD0E1), Color(0xFFFFB74D), Color(0xFFB39DDB))
    val c = g
    val ac = if (c == null) AC[tab] else if (c.logic) AC[2] else if (c.mem) AC[0] else AC[1]
    Column(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF0E0E0E), Color.Black))).padding(16.dp)) {
        if (c == null || step == 0) {
            Text("BRAIN GYM", fontSize = 30.sp, fontWeight = FontWeight.Black)
            Text("Train your memory, reflexes and logic", color = Color.Gray, fontSize = 13.sp)
            Sp(12)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("🧠 Brain", "⚡ Reaction", "🧩 Logic").forEachIndexed { i, t -> Chip(t, tab == i, AC[i], Modifier.weight(1f)) { tab = i } }
            }
            Sp(12)
            LazyVerticalGrid(GridCells.Fixed(3), Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(GAMES2.filter { if (tab == 2) it.logic else !it.logic && it.mem == (tab == 0) }) { x ->
                    Tile(x, AC[tab], st) { g = x; mode = x.modes[0]; step = 1 }
                }
            }
            Text("Games played: ${st.all().size}", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(vertical = 8.dp))
        } else Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
            when (step) {
                1 -> {
                    Text(ic(c.id), fontSize = 48.sp); Text(c.name, fontSize = 24.sp, fontWeight = FontWeight.Bold); Sp(16)
                    Text("MODE", color = Color.Gray, fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { c.modes.forEach { m -> Chip(m, m == mode, ac) { mode = m } } }
                    Sp(12); Text("DIFFICULTY", color = Color.Gray, fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("Easy", "Normal", "Hard").forEachIndexed { i, n -> Chip(n, i == diff, ac) { diff = i } } }
                    Sp(24); Big("▶  START", ac) { step = 2 }; Sp(8)
                    Chip("📈 Scores and history", false, ac) { step = 4 }; Chip("← Back", false, ac) { step = 0 }
                }
                2 -> {
                    val fin = { s: Double ->
                        val h = st.hist(c.id).map { it[3].toDouble() }
                        rec = h.isEmpty() || (if (c.lower) s < h.min() else s > h.max())
                        st.add(c.id, mode, diff, s); score = s; step = 3
                    }
                    Chip("✕ Quit", false, ac) { step = 1 }; Sp(8)
                    Play(c, mode, diff, fin)
                }
                3 -> {
                    val best = st.hist(c.id).map { it[3].toDouble() }.let { if (c.lower) it.minOrNull() else it.maxOrNull() }
                    Sp(24); Text(if (rec) "🏆 New best!" else "Result", color = ac, fontSize = 20.sp)
                    Text("%.1f".format(score), fontSize = 56.sp, fontWeight = FontWeight.Black)
                    Text(c.unit, color = Color.Gray); Text("Best: ${"%.1f".format(best ?: score)}", color = Color.Gray)
                    Sp(24); Big("PLAY AGAIN", ac) { step = 2 }; Sp(8); Chip("Menu", false, ac) { step = 1 }
                }
                else -> {
                    Text("${c.name} history", fontSize = 20.sp, fontWeight = FontWeight.Bold); Sp(8)
                    val h = st.hist(c.id)
                    h.map { it[3].toDouble() }.let { if (it.isNotEmpty()) Text("Best: ${"%.1f".format(if (c.lower) it.min() else it.max())} ${c.unit}", color = ac) }
                    Sp(8)
                    h.forEach { Text("${java.text.SimpleDateFormat("dd MMM HH:mm").format(java.util.Date(it[4].toLong()))} · ${it[1]} · ${listOf("Easy", "Normal", "Hard")[it[2].toInt()]} · ${"%.1f".format(it[3].toDouble())}", fontSize = 13.sp) }
                    Sp(12); Chip("← Back", false, ac) { step = 1 }
                }
            }
        }
        Banner()
    }
}
