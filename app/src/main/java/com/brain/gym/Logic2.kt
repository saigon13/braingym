package com.brain.gym

import androidx.compose.runtime.*
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.abs

val TL = listOf(listOf(0, 1, 2), listOf(3, 4, 5), listOf(6, 7, 8), listOf(0, 3, 6), listOf(1, 4, 7), listOf(2, 5, 8), listOf(0, 4, 8), listOf(2, 4, 6))
fun tttWin(b: List<Int>): Int { for (l in TL) if (b[l[0]] != 0 && b[l[0]] == b[l[1]] && b[l[1]] == b[l[2]]) return b[l[0]]; return 0 }
fun tttMin(b: List<Int>, turn: Int): Int {
    val w = tttWin(b); if (w == 2) return 1; if (w == 1) return -1; if (b.none { it == 0 }) return 0
    val sc = b.indices.filter { b[it] == 0 }.map { i -> tttMin(b.toMutableList().also { x -> x[i] = turn }, 3 - turn) }
    return if (turn == 2) sc.max() else sc.min()
}
fun tttBot(b: List<Int>, d: Int): Int {
    val e = b.indices.filter { b[it] == 0 }
    if ((0..99).random() < listOf(100, 50, 15, 0)[d]) return e.random()
    val sc = e.map { i -> i to tttMin(b.toMutableList().also { x -> x[i] = 2 }, 1) }
    val mx = sc.maxOf { it.second }; return sc.filter { it.second == mx }.random().first
}

@Composable fun TicTacToe(m: String, d: Int, done: (Double) -> Unit) {
    val two = m == "2 Players"
    var b by remember { mutableStateOf(List(9) { 0 }) }; var turn by remember { mutableStateOf(1) }
    var over by remember { mutableStateOf(false) }; var msg by remember { mutableStateOf("") }
    val line = TL.firstOrNull { l -> b[l[0]] != 0 && b[l[0]] == b[l[1]] && b[l[1]] == b[l[2]] }
    fun place(i: Int) {
        val nb = b.toMutableList().also { it[i] = turn }; b = nb
        val w = tttWin(nb)
        if (w != 0 || nb.none { it == 0 }) {
            over = true
            msg = if (w == 0) "Draw" else if (two) "${if (w == 1) "X" else "O"} wins" else if (w == 1) "You win!" else "You lose"
            done(if (two) 1.0 else if (w == 1) 100.0 * (d + 1) else if (w == 2) 0.0 else 50.0 * (d + 1))
        } else turn = 3 - turn
    }
    LaunchedEffect(turn, over) {
        if (!two && turn == 2 && !over) { delay(350); place(withContext(Dispatchers.Default) { tttBot(b, d) }) }
    }
    Status(if (over) msg else if (two) "${if (turn == 1) "X" else "O"} to move" else if (turn == 1) "Your move (X)" else "Bot is thinking…")
    Grid(3, 3) { i ->
        Cell(if (b[i] == 1) "X" else if (b[i] == 2) "O" else "", if (line != null && i in line) Color.White else Color.Black) {
            if (!over && b[i] == 0 && (two || turn == 1)) place(i)
        }
    }
}

fun c4Row(b: IntArray, c: Int): Int { for (r in 5 downTo 0) if (b[r * 7 + c] == 0) return r; return -1 }
fun c4Win(b: IntArray, p: Int): Boolean {
    for (r in 0..5) for (c in 0..6) for ((dr, dc) in listOf(0 to 1, 1 to 0, 1 to 1, 1 to -1)) {
        val er = r + dr * 3; val ec = c + dc * 3; if (er !in 0..5 || ec !in 0..6) continue
        if ((0..3).all { b[(r + dr * it) * 7 + c + dc * it] == p }) return true
    }
    return false
}
fun c4Eval(b: IntArray): Int {
    var s = 0
    for (r in 0..5) { if (b[r * 7 + 3] == 2) s += 3 else if (b[r * 7 + 3] == 1) s -= 3 }
    for (r in 0..5) for (c in 0..6) for ((dr, dc) in listOf(0 to 1, 1 to 0, 1 to 1, 1 to -1)) {
        val er = r + dr * 3; val ec = c + dc * 3; if (er !in 0..5 || ec !in 0..6) continue
        var a = 0; var o = 0
        for (k in 0..3) { val v = b[(r + dr * k) * 7 + c + dc * k]; if (v == 2) a++ else if (v == 1) o++ }
        if (o == 0) s += when (a) { 3 -> 6; 2 -> 2; else -> 0 } else if (a == 0) s -= when (o) { 3 -> 7; 2 -> 2; else -> 0 }
    }
    return s
}
fun c4Search(b: IntArray, d: Int, al: Int, be: Int, bot: Boolean): Int {
    if (c4Win(b, 2)) return 100000 + d; if (c4Win(b, 1)) return -100000 - d
    val cols = (0..6).filter { b[it] == 0 }; if (cols.isEmpty()) return 0; if (d == 0) return c4Eval(b)
    var a = al; var bt = be; val order = cols.sortedBy { abs(it - 3) }
    if (bot) {
        var best = -10000000
        for (c in order) { val nb = b.clone(); nb[c4Row(nb, c) * 7 + c] = 2; best = maxOf(best, c4Search(nb, d - 1, a, bt, false)); a = maxOf(a, best); if (a >= bt) break }
        return best
    }
    var best = 10000000
    for (c in order) { val nb = b.clone(); nb[c4Row(nb, c) * 7 + c] = 1; best = minOf(best, c4Search(nb, d - 1, a, bt, true)); bt = minOf(bt, best); if (a >= bt) break }
    return best
}
fun c4Bot(b: IntArray, d: Int): Int {
    val cols = (0..6).filter { b[it] == 0 }
    if ((0..99).random() < listOf(45, 10, 0, 0)[d]) return cols.random()
    val dep = listOf(1, 3, 4, 6)[d]
    val sc = cols.map { c -> val nb = b.clone(); nb[c4Row(nb, c) * 7 + c] = 2; c to c4Search(nb, dep - 1, -10000000, 10000000, false) }
    val mx = sc.maxOf { it.second }; return sc.filter { it.second == mx }.random().first
}

@Composable fun ConnectFour(m: String, d: Int, done: (Double) -> Unit) {
    val two = m == "2 Players"
    var b by remember { mutableStateOf(IntArray(42)) }; var turn by remember { mutableStateOf(1) }
    var over by remember { mutableStateOf(false) }; var msg by remember { mutableStateOf("") }
    fun drop(c: Int) {
        val r = c4Row(b, c); if (r < 0) return
        val nb = b.clone(); nb[r * 7 + c] = turn; b = nb
        if (c4Win(nb, turn)) {
            over = true
            msg = if (two) (if (turn == 1) "Red wins" else "Yellow wins") else if (turn == 1) "You win!" else "You lose"
            done(if (two) 1.0 else if (turn == 1) 100.0 * (d + 1) else 0.0)
        } else if (nb.none { it == 0 }) { over = true; msg = "Draw"; done(if (two) 1.0 else 50.0 * (d + 1)) }
        else turn = 3 - turn
    }
    LaunchedEffect(turn, over) {
        if (!two && turn == 2 && !over) { delay(300); drop(withContext(Dispatchers.Default) { c4Bot(b, d) }) }
    }
    Status(if (over) msg else if (two) "${if (turn == 1) "Red" else "Yellow"} to move" else if (turn == 1) "Your move (red)" else "Bot is thinking…")
    Grid(6, 7) { i ->
        val v = b[i]
        Cell("", if (v == 1) Color(0xFFE53935) else if (v == 2) Color(0xFFFFC107) else Color.Black) {
            if (!over && (two || turn == 1)) drop(i % 7)
        }
    }
}
