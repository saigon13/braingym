package com.brain.gym

import android.app.Activity
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

class Snap(val p: ChPos, val last: Pair<Int, Int>, val clock: Int)

@Composable fun ChessBoard(m: String, d: Int, done: (Double) -> Unit) {
    val act = LocalContext.current as Activity
    val ac = LocalAccent.current
    val scope = rememberCoroutineScope()
    val human = remember { when (m) { "Play White" -> true; "Play Black" -> false; else -> (0..1).random() == 0 } }
    val h = remember { mutableStateListOf(Snap(chInit(), -1 to -1, 0)) }
    var ix by remember { mutableStateOf(0) }
    var sel by remember { mutableStateOf(-1) }
    var msg by remember { mutableStateOf("") }
    var over by remember { mutableStateOf(false) }
    var think by remember { mutableStateOf(false) }
    var res by remember { mutableStateOf(0.0) }
    var hint by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var t0 by remember { mutableStateOf(now()) }
    var endT by remember { mutableStateOf(0L) }
    var popup by remember { mutableStateOf(false) }
    val cur = h[ix]; val pos = cur.p; val last = cur.last
    val moves = remember(pos) { chGen(pos) }

    fun finish() { over = true; endT = (now() - t0) / 1000; popup = true }

    fun play(mv: Int) {
        val f = mv and 63; val t = (mv shr 6) and 63
        val np = chMake(pos, mv)
        val reset = abs(pos.b[f]) == 1 || np.b.count { it != 0 } < pos.b.count { it != 0 }
        val ck = if (reset) 0 else cur.clock + 1
        while (h.size > ix + 1) h.removeAt(h.size - 1)
        h.add(Snap(np, f to t, ck)); ix = h.size - 1; sel = -1; hint = null
        val chk = chAtt(np.b, np.b.indexOf(if (np.w) 6 else -6), !np.w)
        if (chGen(np).isEmpty()) {
            if (chk) { val win = (!np.w) == human; res = if (win) 100.0 * (d + 1) else 0.0; msg = if (win) "Checkmate! You win" else "Checkmate. You lose" }
            else { res = 50.0 * (d + 1); msg = "Stalemate. Draw" }
            finish()
        } else if (ck >= 100 || np.b.count { it != 0 } == 2) { res = 50.0 * (d + 1); msg = "Draw"; finish() }
        else msg = if (chk) "Check!" else ""
        if (!over) { if (abs(pos.b[t]) != 0) Sfx.tick() else Sfx.click() }
    }

    fun replay() {
        h.clear(); h.add(Snap(chInit(), -1 to -1, 0)); ix = 0; sel = -1; msg = ""; over = false; think = false; hint = null; popup = false; t0 = now()
    }

    LaunchedEffect(ix, h.size, over) {
        if (!over && pos.w != human) {
            think = true; delay(300)
            val mv = withContext(Dispatchers.Default) { chBest(pos, listOf(1, 2, 3)[d], listOf(200, 30, 0)[d]) }
            think = false; play(mv)
        }
    }

    fun tap(q: Int) {
        if (over || think || pos.w != human) return
        if (sel >= 0) { val mv = moves.firstOrNull { (it and 63) == sel && ((it shr 6) and 63) == q }; if (mv != null) { play(mv); return } }
        val v = pos.b[q]; sel = if (v != 0 && (v > 0) == pos.w) q else -1; hint = null
    }
    fun undo() {
        if (over || think) return
        var t = ix - 1; while (t >= 0 && h[t].p.w != human) t--
        if (t >= 0) { ix = t; sel = -1; hint = null; msg = "" }
    }
    fun redo() {
        if (over || think) return
        var t = ix + 1; while (t < h.size && h[t].p.w != human) t++
        if (t < h.size) { ix = t; sel = -1; hint = null; msg = "" }
    }
    fun getHint() {
        if (over || think || pos.w != human) return
        Ads.reward(act) {
            think = true
            scope.launch {
                val mv = withContext(Dispatchers.Default) { chBest(pos, 3, 0) }
                hint = (mv and 63) to ((mv shr 6) and 63); think = false
            }
        }
    }

    val cs = ((LocalConfiguration.current.screenWidthDp - 32) / 8).coerceIn(30, 50)
    Status(if (over) msg else if (think) "Bot is thinking…" else if (pos.w == human) "Your move" + (if (msg.isNotEmpty()) " · $msg" else "") else "Bot to move")
    Grid(8, 8) { di ->
        val q = if (human) di else 63 - di
        val v = pos.b[q]
        val target = sel >= 0 && moves.any { (it and 63) == sel && ((it shr 6) and 63) == q }
        val base = if ((q / 8 + q % 8) % 2 == 0) Color(0xFF3F3F46) else Color(0xFF26262B)
        val hn = hint
        val bg = if (sel == q) Color(0xFF7E57C2) else if (hn != null && (hn.first == q || hn.second == q)) Color(0xFF2E7D32)
            else if (target && v != 0) Color(0xFF8E3B3B) else if (q == last.first || q == last.second) Color(0xFF4A3F6B) else base
        Box(Modifier.size(cs.dp).background(bg).clickable { tap(q) }, Alignment.Center) {
            if (v != 0) Text(CH_SYM[abs(v)], color = if (v > 0) Color.White else Color(0xFFFFB74D), fontSize = (cs * 0.7).sp)
            else if (target) Text("●", color = ac, fontSize = 14.sp)
        }
    }
    Spacer(Modifier.height(10.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Chip("Undo", false, ac) { undo() }; Chip("Redo", false, ac) { redo() }
        Chip("Hint (ad)", false, ac) { getHint() }
        if (over) Chip("Finish", true, ac) { done(res) } else Chip("Resign", false, ac) { done(0.0) }
    }
    if (popup) AlertDialog(
        onDismissRequest = { popup = false },
        title = { Text(msg, fontWeight = FontWeight.Bold) },
        text = { Column { Text("Time played: ${fmt(endT)}"); Text("Score: ${"%.0f".format(res)} points") } },
        confirmButton = { TextButton({ replay() }) { Text("Replay") } },
        dismissButton = { TextButton({ popup = false; done(res) }) { Text("Finish") } }
    )
}
