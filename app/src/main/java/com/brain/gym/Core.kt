package com.brain.gym

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
class G(val id: String, val name: String, val mem: Boolean, val modes: List<String>, val lower: Boolean, val unit: String, val logic: Boolean = false, val fn: Boolean = false)
val GAMES = listOf(
    G("num", "Number Memory", true, listOf("Digits", "Letters", "Mixed"), false, "levels"),
    G("seq", "Sequence Memory", true, listOf("Forward", "Reverse"), false, "length"),
    G("chimp", "Chimp Test", true, listOf("Classic"), false, "numbers"),
    G("pairs", "Card Pairs", true, listOf("Icons", "Numbers"), true, "moves"),
    G("nback", "N-Back", true, listOf("Position"), false, "% correct"),
    G("matrix", "Memory Matrix", true, listOf("Classic"), false, "levels"),
    G("words", "Word Recall", true, listOf("Classic"), false, "levels"),
    G("dual", "Dual N-Back", true, listOf("Classic"), false, "% correct"),
    G("corsi", "Corsi Blocks", true, listOf("Classic"), false, "length"),
    G("rev", "Reverse Span", true, listOf("Classic"), false, "levels"),
    G("flash", "Flash Cards", true, DECKS.keys.toList(), false, "% known"),
    G("react", "Reaction Time", false, listOf("Single", "Average of 5", "Race lights"), true, "ms"),
    G("gonogo", "Go / No-Go", false, listOf("Classic"), false, "% correct"),
    G("stroop", "Stroop Test", false, listOf("Classic"), false, "points"),
    G("schulte", "Schulte Table", false, listOf("Classic"), true, "sec"),
    G("cmatch", "Color Match", false, listOf("Classic"), false, "points"),
    G("whack", "Whack-a-Mole", false, listOf("Classic"), false, "hits"),
    G("odd", "Odd One Out", false, listOf("Classic"), true, "sec"),
    G("flank", "Flanker Test", false, listOf("Classic"), false, "points"),
    G("g2048", "2048", false, listOf("Classic"), false, "points", true),
    G("lights", "Lights Out", false, listOf("Classic"), true, "moves", true),
    G("sudoku", "Sudoku", false, listOf("Classic"), true, "sec", true),
    G("mines", "Minesweeper", false, listOf("Classic"), false, "% cleared", true),
    G("chess", "Chess vs Bot", false, listOf("Play White", "Play Black", "Random side"), false, "points", true),
    G("flappy", "Flappy Bird", false, listOf("Classic"), false, "points", false, true),
    G("snake", "Snake", false, listOf("Classic"), false, "points", false, true)
)
val GODS = setOf("num", "words", "matrix", "seq", "chimp", "nback", "corsi", "gonogo", "whack", "rev", "dual")

fun icon(id: String): ImageVector = when (id) {
    "num" -> Icons.Rounded.Dialpad; "seq" -> Icons.Rounded.GridView; "chimp" -> Icons.Rounded.Pets; "pairs" -> Icons.Rounded.Style
    "nback" -> Icons.Rounded.Psychology; "matrix" -> Icons.Rounded.Apps; "words" -> Icons.Rounded.TextFields; "dual" -> Icons.Rounded.Extension
    "corsi" -> Icons.Rounded.ViewModule; "rev" -> Icons.Rounded.Replay; "flash" -> Icons.Rounded.MenuBook
    "react" -> Icons.Rounded.Bolt; "gonogo" -> Icons.Rounded.Traffic; "stroop" -> Icons.Rounded.Palette; "schulte" -> Icons.Rounded.Search
    "cmatch" -> Icons.Rounded.Brush; "whack" -> Icons.Rounded.Gavel; "odd" -> Icons.Rounded.Visibility; "flank" -> Icons.Rounded.SwapHoriz
    "g2048" -> Icons.Rounded.Calculate; "lights" -> Icons.Rounded.Lightbulb; "sudoku" -> Icons.Rounded.Tag; "mines" -> Icons.Rounded.Flag
    "flappy" -> Icons.Rounded.Flight; "snake" -> Icons.Rounded.Timeline
    else -> Icons.Rounded.SportsEsports
}

val HOW = mapOf(
    "num" to "A string of characters flashes on screen. Memorise it, then type it back with the on-screen keypad. Every 3 levels the string gets one character longer. Easy runs from 2 to 3 characters, Normal 3 to 5, Hard 5 to 7 and God 7 to 10. When you get one wrong, the right answer is shown. Tip: group characters into chunks of two or three.",
    "seq" to "Tiles light up one after another. Repeat the same order by tapping the tiles. Each round adds one more tile. In Reverse mode you must tap them in the opposite order. Higher difficulty means faster flashes.",
    "chimp" to "Numbers appear on the grid. Tap number 1: all the others turn into blank tiles. Now tap the rest in order from memory. Every success adds one more number. One mistake ends the run and shows where the numbers were.",
    "pairs" to "Flip two cards at a time and find the matching pairs. Remember where each picture is. You can tap a new card straight away without waiting. Fewer moves is a better score. Easy has 6 pairs, Normal 8, Hard 10.",
    "nback" to "Tiles light up one by one, and the colour alternates so you can see when a tile moved. Press MATCH when the lit tile is in the same place as N steps earlier (N is 1 to 4 depending on difficulty). Your score is the percentage of correct decisions.",
    "matrix" to "A pattern flashes on the grid for a moment. Tap exactly those tiles. As you level up the grid gets bigger (up to 6 by 6) and more tiles appear. One wrong tap ends the game.",
    "words" to "A list of words is shown for a few seconds. Then you see a mix of those words and new ones. Select exactly the words you saw. Every 3 levels the list gets longer. When you fail, the right words are marked.",
    "dual" to "Two tasks at once: the tile position and a letter. Press POSITION MATCH if the tile is where it was N steps ago, and LETTER MATCH if the letter is the same as N steps ago. The tile colour alternates so you can tell when it moves.",
    "corsi" to "Like Sequence Memory on a bigger 4 by 4 board. Watch the blocks light up, then repeat the order. It trains spatial working memory.",
    "rev" to "Digits flash on screen. Then type them BACKWARDS using the keypad. 4-1-9 becomes 9-1-4. Every 3 levels you get one more digit.",
    "flash" to "Pick a deck. A card shows a question: think of the answer, tap the card to flip it, then press Knew it or Again. Cards you miss come back until you know them. Your score is the percentage you knew on the first try. Difficulty sets 10, 20 or 30 cards.",
    "react" to "Single: tap as soon as the box turns green. Average of 5: five rounds are averaged. Race lights: five red lights come on one by one, then they turn green one by one at the same speed. Tap when the fifth light turns green. Tapping early is a false start. Lower milliseconds is better.",
    "gonogo" to "Tap the circle when it is GREEN and do nothing when it is RED. There are 20 rounds and your live score is shown at the top. On harder difficulties each light stays for less time.",
    "stroop" to "A colour word is printed in a different ink colour. Tap the button that matches the INK colour, not the word. Example: the word BLUE written in red means you tap red. You earn points for correct answers and extra points for speed.",
    "schulte" to "Find and tap the numbers in order, 1, 2, 3 and so on, as fast as you can. Keep your eyes near the centre and use your side vision. Lower time is better.",
    "cmatch" to "A colour word is shown in some ink colour. Press YES if the meaning of the word matches the ink colour, NO if it does not. You earn points for correct answers and extra points for speed.",
    "whack" to "Tiles light up for a moment. Tap each lit tile before it moves. The game lasts 30 seconds and the tiles move faster on harder difficulties.",
    "odd" to "Find the one tile that is different. The puzzle type changes every round: similar letters, a slightly different shade, filled versus outline shapes, arrows pointing a different way, and swapped digits. Each wrong tap adds 2 seconds. There are 12 rounds and a lower time is better.",
    "flank" to "Look only at the MIDDLE arrow and press the direction it points. The arrows around it may point the opposite way to distract you. Correct answers and speed both add points.",
    "g2048" to "Swipe in any direction to slide all the tiles. Equal numbers merge into their sum. Build big numbers and avoid filling the board.",
    "lights" to "Tapping a tile flips it and its four neighbours. Turn every light off in as few moves as you can.",
    "sudoku" to "Fill the 9 by 9 grid so every row, column and 3 by 3 box contains the numbers 1 to 9. Tap an empty tile, then a number. Dark grey tiles are fixed. Lower time is better.",
    "mines" to "TAP a tile to dig it. LONG-PRESS (hold) a tile to place or remove a flag. Each number shows how many mines touch that tile. Clear every safe tile to win. Your first tap is always safe.",
    "chess" to "You play against a bot. Tap a piece, then a highlighted square to move it. Undo and Redo take moves back and forward. Hint shows a strong move after a short ad. A win scores 100 times (difficulty + 1), a draw scores half.",
    "flappy" to "Tap to flap and fly through the gaps between the pipes. Each pipe you pass is one point. Touching a pipe or the edge ends the game. The first tap starts the game.",
    "snake" to "Swipe to steer the snake. Eat the food to grow and score. Hitting the wall or your own tail ends the game. The snake speeds up as it grows."
)

// ---------- State holders ----------
object Live { var txt by mutableStateOf("") }
object Pause { var on by mutableStateOf(false) }
object Ended { var on by mutableStateOf(false) }
val LocalAccent = compositionLocalOf { Color(0xFF4DD0E1) }
val LocalCell = compositionLocalOf { 56 }
fun now() = System.currentTimeMillis()
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

// ---------- Sound and vibration ----------
object Sfx {
    var sound by mutableStateOf(true)
    var vib by mutableStateOf(true)
    var base = 1
    private val tg: ToneGenerator? = try { ToneGenerator(AudioManager.STREAM_MUSIC, 70) } catch (e: Exception) { null }
    private fun t(tone: Int, ms: Int) { if (sound) try { tg?.startTone(tone, ms) } catch (e: Exception) { } }
    fun click() = t(ToneGenerator.TONE_DTMF_0 + base, 45)
    fun tick() = t(ToneGenerator.TONE_PROP_BEEP, 60)
    fun good() = t(ToneGenerator.TONE_PROP_ACK, 160)
    fun bad() = t(ToneGenerator.TONE_PROP_NACK, 350)
    fun win() = t(ToneGenerator.TONE_PROP_PROMPT, 400)
    fun buzz(c: Context) {
        if (!vib) return
        try {
            val v = if (Build.VERSION.SDK_INT >= 31) c.getSystemService(VibratorManager::class.java).defaultVibrator else c.getSystemService(Vibrator::class.java)
            if (Build.VERSION.SDK_INT >= 26) v.vibrate(VibrationEffect.createOneShot(250, VibrationEffect.DEFAULT_AMPLITUDE)) else v.vibrate(250)
        } catch (e: Exception) { }
    }
}

// ---------- Ads (test IDs: replace before release) ----------
object Ads {
    private var inter: InterstitialAd? = null
    private var rew: RewardedAd? = null
    private var n = 0
    fun load(c: Context) {
        InterstitialAd.load(c, "ca-app-pub-3940256099942544/1033173712", AdRequest.Builder().build(), object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(a: InterstitialAd) { inter = a }
            override fun onAdFailedToLoad(e: LoadAdError) { inter = null }
        })
        RewardedAd.load(c, "ca-app-pub-3940256099942544/5224354917", AdRequest.Builder().build(), object : RewardedAdLoadCallback() {
            override fun onAdLoaded(a: RewardedAd) { rew = a }
            override fun onAdFailedToLoad(e: LoadAdError) { rew = null }
        })
    }
    // Shown at most every 3rd finished game, only when leaving the game-over panel
    fun maybe(act: Activity, next: () -> Unit) {
        n++; val a = inter
        if (n % 3 != 0 || a == null) { next(); return }
        a.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() { inter = null; load(act); next() }
            override fun onAdFailedToShowFullScreenContent(e: AdError) { inter = null; next() }
        }
        a.show(act)
    }
    // Rewarded: used for chess hints. If no ad is ready the reward is given for free.
    fun reward(act: Activity, ok: () -> Unit) {
        val r = rew
        if (r == null) { ok(); load(act); return }
        r.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() { rew = null; load(act) }
            override fun onAdFailedToShowFullScreenContent(e: AdError) { rew = null; ok() }
        }
        r.show(act) { ok() }
    }
}

// ---------- UI helpers ----------
@Composable fun Banner() = AndroidView(
    factory = { c -> AdView(c).apply { setAdSize(AdSize.BANNER); adUnitId = "ca-app-pub-3940256099942544/6300978111"; loadAd(AdRequest.Builder().build()) } },
    modifier = Modifier.fillMaxWidth().height(50.dp))

@Composable fun Btn(t: String, f: () -> Unit) =
    OutlinedButton({ Sfx.tick(); f() }, Modifier.padding(4.dp), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, LocalAccent.current.copy(.6f))) { Text(t) }

// The status text is shown big at the top of the game screen by the main UI
@Composable fun Status(t: String) { SideEffect { Live.txt = t } }

@Composable fun BigText(s: String) = Text(s, fontSize = 44.sp, letterSpacing = 6.sp, fontWeight = FontWeight.Bold, color = LocalAccent.current)

@Composable fun TypedBox(s: String) {
    val ac = LocalAccent.current
    Box(Modifier.padding(vertical = 10.dp).fillMaxWidth(.9f).height(60.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFF1B1B1F)).border(1.dp, ac.copy(.5f), RoundedCornerShape(14.dp)), Alignment.Center) {
        Text(if (s.isEmpty()) "…" else s, fontSize = 28.sp, letterSpacing = 4.sp, fontWeight = FontWeight.Bold)
    }
}

fun rndStr(pool: String, n: Int): String {
    val sb = StringBuilder()
    repeat(n) { var c: Char; do { c = pool.random() } while (sb.isNotEmpty() && c == sb.last()); sb.append(c) }
    return sb.toString()
}

@Composable fun Keypad(kind: String, onKey: (String) -> Unit) {
    val ac = LocalAccent.current
    val rows = when (kind) {
        "num" -> listOf("123", "456", "789", "⌫0✓")
        "sud" -> listOf("12345", "6789⌫")
        "alpha" -> listOf("QWERTYUIOP", "ASDFGHJKL", "⌫ZXCVBNM✓")
        else -> listOf("1234567890", "QWERTYUIOP", "ASDFGHJKL", "⌫ZXCVBNM✓")
    }
    val kw = when (kind) { "num" -> 80; "sud" -> 58; else -> ((LocalConfiguration.current.screenWidthDp - 40) / 10).coerceAtMost(40) }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        rows.forEach { r -> Row { r.forEach { ch ->
            val k = ch.toString()
            Box(Modifier.padding(2.dp).size(kw.dp, if (kind == "num" || kind == "sud") 56.dp else 48.dp).clip(RoundedCornerShape(10.dp))
                .background(if (k == "✓") ac else Color(0xFF26262B)).clickable { Sfx.click(); onKey(k) }, Alignment.Center) {
                Text(k, color = if (k == "✓") Color.Black else Color.White, fontSize = 18.sp)
            }
        } } }
    }
}

@Composable fun Grid(r: Int, c: Int, f: @Composable (Int) -> Unit) {
    val s = ((LocalConfiguration.current.screenWidthDp - 56) / c).coerceIn(30, 92)
    CompositionLocalProvider(LocalCell provides s) { Column { repeat(r) { y -> Row { repeat(c) { x -> f(y * c + x) } } } } }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable fun Cell(t: String = "", bg: Color = Color.Black, sz: Int = 0, onLong: (() -> Unit)? = null, on: () -> Unit = {}) {
    val s = LocalCell.current; val ac = LocalAccent.current; val lit = bg == Color.White
    val fill by animateColorAsState(if (lit) ac else if (bg == Color.Black) Color(0xFF1B1B1F) else bg, label = "cell")
    val sh = RoundedCornerShape((s / 5).dp)
    Box(Modifier.padding(3.dp).size((s - 6).dp).clip(sh).background(Brush.verticalGradient(listOf(fill, fill.copy(alpha = .78f))))
        .border(1.dp, if (lit) Color.White.copy(.8f) else ac.copy(.25f), sh)
        .combinedClickable(onClick = { Sfx.click(); on() }, onLongClick = if (onLong != null) ({ Sfx.tick(); onLong() }) else null), Alignment.Center) {
        Text(t, color = if (lit) Color.Black else Color.White, fontSize = (s / 3).sp, fontWeight = FontWeight.Medium)
    }
}

@Composable fun Demo(id: String) {
    val ac = LocalAccent.current; val dim = Color(0xFF2A2A30)
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        when (id) {
            "stroop", "cmatch" -> { Text("BLUE", color = Color.Red, fontSize = 40.sp, fontWeight = FontWeight.Black); Text("Ink is red, so the answer is red", color = Color.Gray, fontSize = 12.sp) }
            "react", "gonogo" -> {
                Row { listOf(Color(0xFFE53935), Color(0xFF1B8A3A)).forEach { Box(Modifier.padding(6.dp).size(40.dp).clip(CircleShape).background(it)) } }
                Text("Red means wait, green means tap", color = Color.Gray, fontSize = 12.sp)
            }
            "num", "rev", "words", "flash" -> { Text("7 4 1 9 2", fontSize = 32.sp, letterSpacing = 6.sp, fontWeight = FontWeight.Bold, color = ac); Text("Memorise it, then answer from memory", color = Color.Gray, fontSize = 12.sp) }
            else -> {
                repeat(3) { y -> Row { repeat(3) { x -> Box(Modifier.padding(3.dp).size(30.dp).clip(RoundedCornerShape(8.dp)).background(if (x == 1 && y == 1) ac else dim)) } } }
                Text("Example: the bright tile is the one to remember or tap", color = Color.Gray, fontSize = 12.sp)
            }
        }
    }
}

@Composable fun InfoDialog(g: G, onClose: () -> Unit) = AlertDialog(
    onDismissRequest = onClose,
    confirmButton = { TextButton(onClose) { Text("Got it") } },
    title = { Text("How to play: ${g.name}") },
    text = { Column(Modifier.verticalScroll(rememberScrollState())) { Demo(g.id); Spacer(Modifier.height(12.dp)); Text(HOW[g.id] ?: "", fontSize = 15.sp) } }
)
