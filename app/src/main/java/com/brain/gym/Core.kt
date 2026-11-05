package com.brain.gym

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.delay

// ---------- Registry ----------
class G(val id: String, val name: String, val mem: Boolean, val modes: List<String>, val lower: Boolean, val unit: String, val logic: Boolean = false, val fn: Boolean = false, val asmr: Boolean = false)
val CLS = listOf("Classic")
val BOT2 = listOf("vs Bot", "2 Players")
val GAMES = listOf(
    G("num", "Number Memory", true, listOf("Digits", "Letters", "Mixed"), false, "levels"),
    G("seq", "Sequence Memory", true, listOf("Forward", "Reverse"), false, "length"),
    G("chimp", "Chimp Test", true, CLS, false, "numbers"),
    G("pairs", "Card Pairs", true, listOf("Icons", "Numbers"), true, "moves"),
    G("nback", "N-Back", true, listOf("Position"), false, "% correct"),
    G("matrix", "Memory Matrix", true, CLS, false, "levels"),
    G("words", "Word Recall", true, CLS, false, "levels"),
    G("dual", "Dual N-Back", true, CLS, false, "% correct"),
    G("corsi", "Corsi Blocks", true, CLS, false, "length"),
    G("rev", "Reverse Span", true, CLS, false, "levels"),
    G("flash", "Flash Cards", true, DECKS.keys.toList(), false, "% known"),
    G("react", "Reaction Time", false, listOf("Single", "Average of 5", "Race lights"), true, "ms"),
    G("gonogo", "Go / No-Go", false, CLS, false, "points"),
    G("stroop", "Stroop Test", false, CLS, false, "points"),
    G("schulte", "Schulte Table", false, CLS, true, "sec"),
    G("cmatch", "Color Match", false, CLS, false, "points"),
    G("whack", "Whack-a-Mole", false, CLS, false, "hits"),
    G("odd", "Odd One Out", false, CLS, true, "sec"),
    G("flank", "Flanker Test", false, CLS, false, "points"),
    G("g2048", "2048", false, CLS, false, "points", true),
    G("lights", "Lights Out", false, CLS, true, "moves", true),
    G("sudoku", "Sudoku", false, CLS, true, "sec", true),
    G("mines", "Minesweeper", false, CLS, false, "% cleared", true),
    G("chess", "Chess", false, listOf("Play White", "Play Black", "Random side", "2 Players"), false, "points", true),
    G("ttt", "Tic Tac Toe", false, BOT2, false, "points", true),
    G("c4", "Connect Four", false, BOT2, false, "points", true),
    G("flappy", "Flappy Ball", false, CLS, false, "points", false, true),
    G("snake", "Snake", false, listOf("Classic", "Wrap", "Obstacles", "Fast"), false, "points", false, true),
    G("stack", "Stack Tower", false, CLS, false, "layers", false, true),
    G("dodge", "Dodge", false, CLS, false, "sec", false, true),
    G("tiles", "Tap Tiles", false, CLS, false, "taps", false, true),
    G("balloon", "Balloon Pop", false, CLS, false, "pops", false, true),
    G("keys", "Mechanical Keys", false, CLS, false, "", false, false, true),
    G("popit", "Pop-It", false, CLS, false, "", false, false, true),
    G("wrap", "Bubble Wrap", false, CLS, false, "", false, false, true),
    G("switches", "Toggle Switches", false, CLS, false, "", false, false, true),
    G("pen", "Clicky Pen", false, CLS, false, "", false, false, true),
    G("zip", "Zipper", false, CLS, false, "", false, false, true),
    G("spinner", "Fidget Spinner", false, CLS, false, "", false, false, true),
    G("slime", "Slime", false, CLS, false, "", false, false, true),
    G("knob", "Rotary Knob", false, CLS, false, "", false, false, true),
    G("buttons", "Stress Buttons", false, CLS, false, "", false, false, true)
)

val ROSE = Color(0xFFFF6B8B)
val PURP = Color(0xFF9C6BFF)
val DC = listOf(Color(0xFF4CAF50), Color(0xFFFFC107), Color(0xFFFF9800), Color(0xFFFF5252))

fun icon(id: String): ImageVector = when (id) {
    "num" -> Icons.Rounded.Dialpad; "seq" -> Icons.Rounded.GridView; "chimp" -> Icons.Rounded.Pets; "pairs" -> Icons.Rounded.Style
    "nback" -> Icons.Rounded.Psychology; "matrix" -> Icons.Rounded.Apps; "words" -> Icons.Rounded.TextFields; "dual" -> Icons.Rounded.Extension
    "corsi" -> Icons.Rounded.ViewModule; "rev" -> Icons.Rounded.Replay; "flash" -> Icons.Rounded.MenuBook
    "react" -> Icons.Rounded.Bolt; "gonogo" -> Icons.Rounded.Traffic; "stroop" -> Icons.Rounded.Palette; "schulte" -> Icons.Rounded.Search
    "cmatch" -> Icons.Rounded.Brush; "whack" -> Icons.Rounded.Gavel; "odd" -> Icons.Rounded.Visibility; "flank" -> Icons.Rounded.SwapHoriz
    "g2048" -> Icons.Rounded.Calculate; "lights" -> Icons.Rounded.Lightbulb; "sudoku" -> Icons.Rounded.Tag; "mines" -> Icons.Rounded.Flag
    "ttt" -> Icons.Rounded.Close; "c4" -> Icons.Rounded.Adjust
    "flappy" -> Icons.Rounded.Flight; "snake" -> Icons.Rounded.Timeline; "stack" -> Icons.Rounded.Layers
    "dodge" -> Icons.Rounded.Security; "tiles" -> Icons.Rounded.MusicNote; "balloon" -> Icons.Rounded.Star
    "keys" -> Icons.Rounded.Keyboard; "popit" -> Icons.Rounded.BubbleChart; "wrap" -> Icons.Rounded.Apps; "switches" -> Icons.Rounded.ToggleOn
    "pen" -> Icons.Rounded.Edit; "zip" -> Icons.Rounded.UnfoldMore; "spinner" -> Icons.Rounded.Autorenew; "slime" -> Icons.Rounded.Eco
    "knob" -> Icons.Rounded.RadioButtonChecked; "buttons" -> Icons.Rounded.TouchApp
    else -> Icons.Rounded.SportsEsports
}

val HOW = mapOf(
    "num" to "A string of characters flashes on screen. Memorise it, then type it back with the on-screen keypad. Every 3 levels it gets one character longer. Easy runs 2 to 3 characters, Normal 3 to 5, Hard 5 to 7, God 7 to 10. When you fail, the correct answer shows in purple and yours in rose.",
    "seq" to "Tiles light up one after another. Repeat the same order. Each round adds one tile. Reverse mode: tap them backwards. When you slip, your wrong tile turns rose and the whole correct order is shown in purple with numbers.",
    "chimp" to "Numbers appear on the grid. Tap 1: the others turn blank. Tap the rest in order from memory. Each success adds a number. On a mistake the wrong tile turns rose and all the numbers are shown again.",
    "pairs" to "Flip two cards at a time and find the pairs. You can tap the next card straight away. Fewer moves is better. Easy 6 pairs, Normal 8, Hard 10, God 12.",
    "nback" to "Tiles light up one at a time. Is the glowing tile in the SAME place as N steps ago? If yes, tap anywhere or press MATCH before the next tile. N is 1 on Easy up to 4 on God. After each tile you see a tick or a cross. On Easy a dim purple square shows where it was N steps ago.",
    "matrix" to "A pattern flashes on the grid. Tap exactly those tiles. As you level up the grid grows (up to 6 by 6) and more tiles appear. On a mistake the wrong tile is rose and the tiles you missed are purple.",
    "words" to "A list of words is shown. Then you see those words mixed with new ones. Select exactly the words you saw. Every 3 levels the list gets longer. On failure missed words turn purple and wrong picks turn rose.",
    "dual" to "Two tasks at once: tile position and a letter. Press POSITION MATCH if the tile is where it was N steps ago, and LETTER MATCH if the letter is the same as N steps ago.",
    "corsi" to "Like Sequence Memory on a 4 by 4 board. Watch, then repeat. A mistake shows your wrong block in rose and the full correct order in purple.",
    "rev" to "Digits flash on screen. Type them BACKWARDS with the keypad. Every 3 levels you get one more digit.",
    "flash" to "Pick a deck. Think of the answer, tap the card to flip it, then press Knew it or Again. Missed cards come back. Score is the percentage you knew first try. Difficulty sets 10, 20, 30 or 40 cards.",
    "react" to "Single: tap anywhere as soon as the box turns green. Average of 5: five rounds averaged. Race lights: five red lights come on one by one at a steady beat, then five green lights come on one by one at the same beat. Tap the moment the last green light comes on. Tapping early is a false start. Lower is better.",
    "gonogo" to "Tap anywhere when the circle is GREEN and do nothing when it is RED. 20 rounds. You earn points for right decisions plus a speed bonus for fast taps on green.",
    "stroop" to "A colour word is printed in a different ink colour. Tap the button matching the INK colour, not the word. Points for correct answers plus speed. God has 6 colours and 30 rounds.",
    "schulte" to "Tap the numbers in order, 1, 2, 3 and so on, as fast as you can. Lower time is better. The grid is 3 by 3 on Easy up to 6 by 6 on God.",
    "cmatch" to "Press YES if the meaning of the word matches its ink colour, NO if not. Points for correct answers plus speed. God has 6 colours and 30 rounds.",
    "whack" to "Tap each lit tile. When you hit one, it vanishes and a new one appears at once. 30 seconds. Tiles stay for less time on harder difficulties.",
    "odd" to "Find the one tile that is different. Types rotate each round: similar letters, a different shade, filled versus outline shapes, arrows and swapped digits. Each wrong tap adds 2 seconds. 12 rounds, lower time is better.",
    "flank" to "Look only at the MIDDLE arrow and press its direction. The arrows around it may point the other way. On God they always do.",
    "g2048" to "Swipe to slide all tiles. Equal numbers merge. Avoid filling the board. Harder difficulties spawn more 4s.",
    "lights" to "Tapping a tile flips it and its four neighbours. Turn every light off in as few moves as you can.",
    "sudoku" to "Fill the grid so every row, column and 3 by 3 box has 1 to 9. Tap an empty tile, then a number. Dark grey tiles are fixed.",
    "mines" to "TAP to dig. HOLD to flag. Numbers show how many mines touch a tile. Clear all safe tiles to win. Your first tap is safe. Easy is 6 by 6 with 5 mines, up to 10 by 10 with 24 on God.",
    "chess" to "Tap a piece, then a highlighted square. Play White, Black or a random side against the bot, or choose 2 Players to share one phone. Undo, Redo and Hint (short ad) are available. A win scores 100 times (difficulty + 1). On God the bot looks four moves ahead and may take a few seconds.",
    "ttt" to "Get three in a row. You are X and move first. vs Bot: the bot plays O and God never loses. 2 Players: take turns on one phone.",
    "c4" to "Tap a column to drop a disc. Connect four in a row, column or diagonal to win. You are red. Play vs the bot or with 2 Players.",
    "flappy" to "Tap anywhere to flap the ball through the gaps. Each pipe passed is one point. The first tap starts the game.",
    "snake" to "Swipe to steer. Eat food to grow. Classic: walls and your tail are deadly. Wrap: walls lead to the other side. Obstacles: grey blocks are deadly. Fast: much quicker.",
    "stack" to "A block slides left and right. Tap anywhere to drop it. Only the overlapping part stays. A perfect drop keeps the width. The game ends when you miss completely.",
    "dodge" to "Drag your finger to move the player and dodge falling blocks. Score is seconds survived.",
    "tiles" to "Tap the lit tile in the bottom row. Each correct tap refills the time bar a little. A wrong tap or an empty bar ends the game.",
    "balloon" to "Tap balloons to pop them before they float away. Three lives. Balloons get faster over time.",
    "keys" to "Eight big mechanical keys. Tap them in any rhythm. Every key makes a press sound and a release sound. Switch between Clicky and Thocky key sounds.",
    "popit" to "A silicone pop-it. Tap or slide your finger over the bubbles to push them in and out.",
    "wrap" to "Tap or slide your finger over the bubble wrap to pop it. It refills when everything is popped.",
    "switches" to "Flip the toggle switches. On and off clack differently.",
    "pen" to "Click the pen. One click pushes the tip out, the next pulls it back in.",
    "zip" to "Drag the zipper slider up and down. Every tooth makes a tiny ratchet click, so faster pulls sound faster.",
    "spinner" to "Flick around it to spin it. The bearing whir follows its speed. A plain tap does nothing.",
    "slime" to "Poke it with one finger, or pull it with two fingers to stretch it. Let go and it slowly settles back into shape.",
    "knob" to "Turn the knob with your finger. Every notch clicks.",
    "buttons" to "Six squishy stress buttons, each with its own sound."
)

// ---------- State holders ----------
object Live { var txt by mutableStateOf("") }
object Pause { var on by mutableStateOf(false); var lost = 0L }
object Ended { var on by mutableStateOf(false) }
val LocalAccent = compositionLocalOf { Color(0xFF4DD0E1) }
val LocalCell = compositionLocalOf { 56 }
fun now() = System.currentTimeMillis() - Pause.lost
suspend fun wait(ms: Long) { var left = ms; while (left > 0) { delay(50); if (!Pause.on) left -= 50 } }

fun spanLen(d: Int, lv: Int, s: IntArray, m: IntArray) = minOf(m[d], s[d] + (lv - 1) / 3)
fun speedScore(ok: Int, total: Int, t0: Long): Double { val sec = (now() - t0) / 1000.0; return ok * 100.0 + ok * maxOf(0.0, 3.0 - sec / total) * 30 }
fun bagList(n: Int, count: Int): List<Int> {
    val out = ArrayList<Int>()
    while (out.size < count) {
        var b = (0 until n).shuffled()
        if (out.isNotEmpty() && n > 1 && b[0] == out.last()) b = b.drop(1) + b[0]
        out.addAll(b)
    }
    return out.take(count)
}

class Store(c: Context) {
    private val p = c.getSharedPreferences("s", 0)
    fun all() = (p.getString("l", "") ?: "").lines().filter { it.isNotBlank() }.map { it.split("|") }
    fun add(g: String, m: String, d: Int, s: Double) {
        val old = (p.getString("l", "") ?: "")
        p.edit().putString("l", (old + "\n$g|$m|$d|$s|${System.currentTimeMillis()}").trim()).apply()
    }
    fun hist(g: String) = all().filter { it[0] == g }.reversed()
    fun totalSec(): Long = p.getLong("t", 0L)
    fun setTotal(s: Long) { p.edit().putLong("t", s).apply() }
    fun bool(k: String, def: Boolean) = p.getBoolean(k, def)
    fun setBool(k: String, v: Boolean) { p.edit().putBoolean(k, v).apply() }
}

// ---------- Touch hub: lets a game react to a touch ANYWHERE in the game area ----------
object Hub {
    var down: (() -> Unit)? = null
    var move: ((Float, Float) -> Unit)? = null
    var up: ((Float, Float) -> Unit)? = null
    var w = 1f
}
@Composable fun UseHub(down: (() -> Unit)? = null, move: ((Float, Float) -> Unit)? = null, up: ((Float, Float) -> Unit)? = null) {
    SideEffect { Hub.down = down; Hub.move = move; Hub.up = up }
    DisposableEffect(Unit) { onDispose { Hub.down = null; Hub.move = null; Hub.up = null } }
}
fun Modifier.hub(): Modifier = pointerInput(Unit) {
    awaitEachGesture {
        val d = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        Hub.w = size.width.toFloat()
        Hub.down?.invoke()
        var lx = d.position.x; var ly = d.position.y; val sx = lx; val sy = ly
        while (true) {
            val ev = awaitPointerEvent(PointerEventPass.Initial)
            val ch = ev.changes.firstOrNull() ?: break
            if (!ch.pressed) { Hub.up?.invoke(ch.position.x - sx, ch.position.y - sy); break }
            Hub.move?.invoke(ch.position.x - lx, ch.position.y - ly); lx = ch.position.x; ly = ch.position.y
        }
    }
}

// Reacts the moment the finger goes DOWN (fast). With onLong it waits to tell tap from hold.
@Composable fun Modifier.fast(onLong: (() -> Unit)?, f: () -> Unit): Modifier {
    val cb by rememberUpdatedState(f); val lb by rememberUpdatedState(onLong)
    return pointerInput(Unit) {
        awaitEachGesture {
            val d = awaitFirstDown(); d.consume()
            val lg = lb
            if (lg == null) { cb(); waitForUpOrCancellation() }
            else { val up = withTimeoutOrNull(380) { waitForUpOrCancellation() }; if (up == null) { lg(); waitForUpOrCancellation() } else cb() }
        }
    }
}

// ---------- Ads (test IDs: replace before release) ----------
object AdCfg {
    const val ENABLED = true
    const val BANNER = true
    const val INTERSTITIAL_EVERY = 3
    const val REWARDED_HINTS = true
    const val BANNER_ID = "ca-app-pub-3940256099942544/6300978111"
    const val INTER_ID = "ca-app-pub-3940256099942544/1033173712"
    const val REWARD_ID = "ca-app-pub-3940256099942544/5224354917"
}

object Ads {
    private var inter: InterstitialAd? = null
    private var rew: RewardedAd? = null
    fun load(c: Context) {
        if (!AdCfg.ENABLED) return
        InterstitialAd.load(c, AdCfg.INTER_ID, AdRequest.Builder().build(), object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(a: InterstitialAd) { inter = a }
            override fun onAdFailedToLoad(e: LoadAdError) { inter = null }
        })
        RewardedAd.load(c, AdCfg.REWARD_ID, AdRequest.Builder().build(), object : RewardedAdLoadCallback() {
            override fun onAdLoaded(a: RewardedAd) { rew = a }
            override fun onAdFailedToLoad(e: LoadAdError) { rew = null }
        })
    }
    fun show(act: Activity, onShown: () -> Unit, next: () -> Unit) {
        val a = inter
        if (!AdCfg.ENABLED || a == null) { next(); return }
        onShown()
        a.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() { inter = null; load(act); next() }
            override fun onAdFailedToShowFullScreenContent(e: AdError) { inter = null; next() }
        }
        a.show(act)
    }
    fun reward(act: Activity, ok: () -> Unit) {
        val r = rew
        if (!AdCfg.ENABLED || !AdCfg.REWARDED_HINTS || r == null) { ok(); load(act); return }
        r.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() { rew = null; load(act) }
            override fun onAdFailedToShowFullScreenContent(e: AdError) { rew = null; ok() }
        }
        r.show(act) { AdBrain.ins?.rewardUsed(); ok() }
    }
}

// ---------- UI helpers ----------
@Composable fun Banner() {
    if (AdCfg.ENABLED && AdCfg.BANNER) AndroidView(
        factory = { c -> AdView(c).apply { setAdSize(AdSize.BANNER); adUnitId = AdCfg.BANNER_ID; loadAd(AdRequest.Builder().build()) } },
        modifier = Modifier.fillMaxWidth().height(50.dp))
}

// Filled action button — reacts on finger-down like the game tiles do.
@Composable fun Btn(t: String, enabled: Boolean = true, f: () -> Unit) {
    val ac = LocalAccent.current
    Box(Modifier.padding(4.dp).clip(RoundedCornerShape(12.dp))
        .background(if (enabled) ac else ac.copy(alpha = .3f))
        .fast(null) { if (enabled) { Sfx.tick(); f() } }
        .padding(horizontal = 16.dp, vertical = 10.dp), Alignment.Center) {
        Text(t, fontWeight = FontWeight.Bold, color = if (enabled) Color.Black else Color.Black.copy(alpha = .5f))
    }
}

// Outlined option button (answers and choices that are part of the game)
@Compos