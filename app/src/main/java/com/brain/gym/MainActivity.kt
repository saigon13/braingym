package com.brain.gym

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.abs

// Board: index 0 = a8 ... 63 = h1. White pieces positive, black negative. 1=P 2=N 3=B 4=R 5=Q 6=K
// c = castling rights bits: 1 = white kingside, 2 = white queenside, 4 = black kingside, 8 = black queenside
class ChPos(val b: IntArray, val w: Boolean, val c: Int, val ep: Int)

val CH_DIAG = listOf(1 to 1, 1 to -1, -1 to 1, -1 to -1)
val CH_ORTH = listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1)
val CH_ALL = CH_DIAG + CH_ORTH
val CH_KN = listOf(1 to 2, 2 to 1, -1 to 2, -2 to 1, 1 to -2, 2 to -1, -1 to -2, -2 to -1)
val CH_VAL = intArrayOf(0, 100, 320, 330, 500, 900, 0)
val CH_SYM = arrayOf("", "\u265F\uFE0E", "\u265E", "\u265D", "\u265C", "\u265B", "\u265A")

fun chInit(): ChPos {
    val b = IntArray(64); val back = intArrayOf(4, 2, 3, 5, 6, 3, 2, 4)
    for (x in 0..7) { b[x] = -back[x]; b[8 + x] = -1; b[48 + x] = 1; b[56 + x] = back[x] }
    return ChPos(b, true, 15, -1)
}

fun chAtt(b: IntArray, sq: Int, byW: Boolean): Boolean {
    val x = sq % 8; val y = sq / 8; val sg = if (byW) 1 else -1
    val py = if (byW) y + 1 else y - 1
    for (dx in intArrayOf(-1, 1)) { val nx = x + dx; if (nx in 0..7 && py in 0..7 && b[py * 8 + nx] == sg) return true }
    for ((dx, dy) in CH_KN) { val nx = x + dx; val ny = y + dy; if (nx in 0..7 && ny in 0..7 && b[ny * 8 + nx] == 2 * sg) return true }
    for ((dx, dy) in CH_ALL) { val nx = x + dx; val ny = y + dy; if (nx in 0..7 && ny in 0..7 && b[ny * 8 + nx] == 6 * sg) return true }
    for ((i, d) in CH_ALL.withIndex()) {
        var nx = x + d.first; var ny = y + d.second
        while (nx in 0..7 && ny in 0..7) {
            val o = b[ny * 8 + nx]
            if (o != 0) {
                if ((o > 0) == byW) { val a = abs(o); if (a == 5 || (a == 3 && i < 4) || (a == 4 && i >= 4)) return true }
                break
            }
            nx += d.first; ny += d.second
        }
    }
    return false
}

fun chCm(sq: Int) = when (sq) { 0 -> 8; 7 -> 4; 4 -> 12; 56 -> 2; 63 -> 1; 60 -> 3; else -> 0 }

fun chMake(p: ChPos, m: Int): ChPos {
    val f = m and 63; val t = (m shr 6) and 63; val pr = (m shr 12) and 7
    val b = p.b.clone(); val v = b[f]; val a = abs(v); var ep = -1
    b[f] = 0
    if (a == 1 && t == p.ep && b[t] == 0 && f % 8 != t % 8) b[if (p.w) t + 8 else t - 8] = 0
    b[t] = if (pr > 0) (if (p.w) pr else -pr) else v
    if (a == 1 && abs(t - f) == 16) ep = (f + t) / 2
    if (a == 6 && abs(t - f) == 2) { if (t > f) { b[f + 1] = b[f + 3]; b[f + 3] = 0 } else { b[f - 1] = b[f - 4]; b[f - 4] = 0 } }
    return ChPos(b, !p.w, p.c and (chCm(f) or chCm(t)).inv(), ep)
}

fun chGen(p: ChPos): List<Int> {
    val out = ArrayList<Int>(48); val b = p.b; val w = p.w
    for (s in 0..63) {
        val v = b[s]; if (v == 0 || (v > 0) != w) continue
        val t = abs(v); val x = s % 8; val y = s / 8
        when (t) {
            1 -> {
                val dy = if (w) -1 else 1; val ny = y + dy
                if (ny in 0..7) {
                    val one = ny * 8 + x; val promo = if (ny == 0 || ny == 7) (5 shl 12) else 0
                    if (b[one] == 0) {
                        out.add(s or (one shl 6) or promo)
                        if (y == (if (w) 6 else 1) && b[one + dy * 8] == 0) out.add(s or ((one + dy * 8) shl 6))
                    }
                    for (dx in intArrayOf(-1, 1)) {
                        val nx = x + dx
                        if (nx in 0..7) { val to = ny * 8 + nx; if ((b[to] != 0 && (b[to] > 0) != w) || to == p.ep) out.add(s or (to shl 6) or promo) }
                    }
                }
            }
            2, 6 -> {
                for ((dx, dy) in if (t == 2) CH_KN else CH_ALL) {
                    val nx = x + dx; val ny = y + dy
                    if (nx in 0..7 && ny in 0..7) { val o = b[ny * 8 + nx]; if (o == 0 || (o > 0) != w) out.add(s or ((ny * 8 + nx) shl 6)) }
                }
                if (t == 6) {
                    val r = if (w) 7 else 0; val rk = if (w) 4 else -4
                    if (s == r * 8 + 4 && !chAtt(b, s, !w)) {
                        if ((p.c and (if (w) 1 else 4)) != 0 && b[s + 1] == 0 && b[s + 2] == 0 && b[s + 3] == rk && !chAtt(b, s + 1, !w) && !chAtt(b, s + 2, !w)) out.add(s or ((s + 2) shl 6))
                        if ((p.c and (if (w) 2 else 8)) != 0 && b[s - 1] == 0 && b[s - 2] == 0 && b[s - 3] == 0 && b[s - 4] == rk && !chAtt(b, s - 1, !w) && !chAtt(b, s - 2, !w)) out.add(s or ((s - 2) shl 6))
                    }
                }
            }
            else -> {
                val dirs = if (t == 3) CH_DIAG else if (t == 4) CH_ORTH else CH_ALL
                for ((dx, dy) in dirs) {
                    var nx = x + dx; var ny = y + dy
                    while (nx in 0..7 && ny in 0..7) {
                        val to = ny * 8 + nx; val o = b[to]
                        if (o == 0) out.add(s or (to shl 6)) else { if ((o > 0) != w) out.add(s or (to shl 6)); break }
                        nx += dx; ny += dy
                    }
                }
            }
        }
    }
    return out.filter { val q = chMake(p, it); !chAtt(q.b, q.b.indexOf(if (w) 6 else -6), !w) }
}

fun chEval(p: ChPos): Int {
    var s = 0
    for (i in 0..63) {
        val v = p.b[i]; if (v == 0) continue
        val a = abs(v); val x = i % 8; val y = i / 8
        val cen = 14 - (abs(2 * x - 7) + abs(2 * y - 7))
        var sc = CH_VAL[a]
        if (a == 1) sc += (if (v > 0) 6 - y else y - 1) * 6 + cen
        else if (a == 2 || a == 3) sc += cen * 3
        else if (a == 5) sc += cen
        s += if (v > 0) sc else -sc
    }
    return if (p.w) s else -s
}

fun chOrd(p: ChPos, m: Int) = abs(p.b[(m shr 6) and 63]) * 10 - abs(p.b[m and 63]) + (if (((m shr 12) and 7) > 0) 50 else 0)

fun chSearch(p: ChPos, d: Int, al: Int, be: Int): Int {
    if (d == 0) return chEval(p)
    val ms = chGen(p)
    if (ms.isEmpty()) return if (chAtt(p.b, p.b.indexOf(if (p.w) 6 else -6), !p.w)) -100000 - d else 0
    var a = al
    for (m in ms.sortedByDescending { chOrd(p, it) }) {
        val sc = -chSearch(chMake(p, m), d - 1, -be, -a)
        if (sc >= be) return be
        if (sc > a) a = sc
    }
    return a
}

fun chBest(p: ChPos, d: Int, noise: Int): Int {
    val ms = chGen(p).shuffled(); var bm = ms[0]; var bs = -10000000
    for (m in ms) {
        val hi = if (noise > 0) 1000000 else -bs
        val sc = -chSearch(chMake(p, m), d - 1, -1000000, hi) + (if (noise > 0) (0..noise).random() else 0)
        if (sc > bs) { bs = sc; bm = m }
    }
    return bm
}

@Composable fun ChessGame(m: String, d: Int, done: (Double) -> Unit) {
    val human = remember { when (m) { "Play White" -> true; "Play Black" -> false; else -> (0..1).random() == 0 } }
    var pos by remember { mutableStateOf(chInit()) }
    var sel by remember { mutableStateOf(-1) }
    var last by remember { mutableStateOf(-1 to -1) }
    var msg by remember { mutableStateOf("") }
    var clock by remember { mutableStateOf(0) }
    var over by remember { mutableStateOf(false) }
    var think by remember { mutableStateOf(false) }
    var res by remember { mutableStateOf(0.0) }
    val moves = remember(pos) { chGen(pos) }

    fun play(mv: Int) {
        val before = pos; val f = mv and 63; val t = (mv shr 6) and 63
        val np = chMake(before, mv)
        val reset = abs(before.b[f]) == 1 || np.b.count { it != 0 } < before.b.count { it != 0 }
        clock = if (reset) 0 else clock + 1
        last = f to t; pos = np; sel = -1
        val chk = chAtt(np.b, np.b.indexOf(if (np.w) 6 else -6), !np.w)
        if (chGen(np).isEmpty()) {
            over = true
            if (chk) { val win = (!np.w) == human; res = if (win) 100.0 * (d + 1) else 0.0; msg = if (win) "Checkmate! You win" else "Checkmate. You lose" }
            else { res = 50.0 * (d + 1); msg = "Stalemate. Draw" }
        } else if (clock >= 100 || np.b.count { it != 0 } == 2) { over = true; res = 50.0 * (d + 1); msg = "Draw" }
        else msg = if (chk) "Check!" else ""
    }

    LaunchedEffect(pos) {
        if (!over && pos.w != human) {
            think = true; delay(300)
            val mv = withContext(Dispatchers.Default) { chBest(pos, listOf(1, 2, 3)[d], listOf(200, 30, 0)[d]) }
            think = false; play(mv)
        }
    }

    fun tap(q: Int) {
        if (over || think || pos.w != human) return
        if (sel >= 0) { val mv = moves.firstOrNull { (it and 63) == sel && ((it shr 6) and 63) == q }; if (mv != null) { play(mv); return } }
        val v = pos.b[q]; sel = if (v != 0 && (v > 0) == pos.w) q else -1
    }

    Text("You are ${if (human) "White" else "Black"}", color = Color.Gray, fontSize = 13.sp)
    Text(if (over) msg else if (think) "Bot is thinking…" else if (pos.w == human) "Your move" + (if (msg.isNotEmpty()) " · $msg" else "") else "", fontSize = 16.sp)
    Spacer(Modifier.height(8.dp))
    Grid(8, 8) { di ->
        val q = if (human) di else 63 - di
        val v = pos.b[q]
        val target = sel >= 0 && moves.any { (it and 63) == sel && ((it shr 6) and 63) == q }
        val base = if ((q / 8 + q % 8) % 2 == 0) Color(0xFF3A3A3A) else Color(0xFF242424)
        val bg = if (sel == q) Color(0xFF7E57C2) else if (target && v != 0) Color(0xFF8E3B3B) else if (q == last.first || q == last.second) Color(0xFF4A3F6B) else base
        Box(Modifier.size(40.dp).background(bg).clickable { tap(q) }, Alignment.Center) {
            if (v != 0) Text(CH_SYM[abs(v)], color = if (v > 0) Color.White else Color(0xFFFFB74D), fontSize = 26.sp)
            else if (target) Text("●", color = Color(0xFFB39DDB), fontSize = 14.sp)
        }
    }
    Spacer(Modifier.height(8.dp))
    if (over) Btn("SEE RESULT") { done(res) } else Btn("Resign") { done(0.0) }
}
