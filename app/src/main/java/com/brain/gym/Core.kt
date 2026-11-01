package com.brain.gym

import android.app.Activity
import android.content.Context
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
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
    G("gonogo", "Go / No-Go", false, listOf("Classic"), false, "points"),
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
    G("snake", "Snake", false, listOf("Classic", "Wrap", "Obstacles", "Fast"), false, "points", false, true),
    G("stack", "Stack Tower", false, listOf("Classic"), false, "layers", false, true),
    G("dodge", "Dodge", false, listOf("Classic"), false, "sec", false, true),
    G("tiles", "Tap Tiles", false, listOf("Classic"), false, "taps", false, true)
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
    "flappy" -> Icons.Rounded.Flight; "snake" -> Icons.Rounded.Timeline; "stack" -> Icons.Rounded.Layers
    "dodge" -> Icons.Rounded.Security; "tiles" -> Icons.Rounded.MusicNote
    else -> Icons.Rounded.SportsEsports
}

val HOW = mapOf(
    "num" to "A string of characters flashes on screen. Memorise it, then type it back with the on-screen keypad. Every 3 levels the string gets one character longer. Easy runs from 2 to 3 characters, Normal 3 to 5, Hard 5 to 7 and God 7 to 10. When you get one wrong, the correct answer is shown in purple and your answer in rose. Tip: group characters into chunks of two or three.",
    "seq" to "Tiles light up one after another. Repeat the same order by tapping the tiles. Each round adds one more tile. In Reverse mode you must tap them in the opposite order. When you slip, the tile you tapped turns rose and the tile you should have tapped turns purple.",
    "chimp" to "Numbers appear on the grid. Tap number 1: all the others turn into blank tiles. Now tap the rest in order from memory. Every success adds one more number. One mistake ends the run: the wrong tile turns rose and the numbers you had left are shown in purple.",
    "pairs" to "Flip two cards at a time and find the matching pairs. Remember where each picture is. You can tap a new card straight away without waiting. Fewer moves is a better score. Easy has 6 pairs, Normal 8, Hard 10 and God 12.",
    "nback" to "Tiles light up one at a time. For each tile ask yourself: is it in the SAME place it was N steps ago? If yes, press MATCH before the next tile. If not, do nothing. N is 1 on Easy, 2 on Normal, 3 on Hard and 4 on God. After each tile you see a tick or a cross. On Easy a dim purple square shows where the tile was N steps ago. The tile colour alternates so you can tell when it moved. Your score is the percentage of right decisions.",
    "matrix" to "A pattern flashes on the grid for a moment. Tap exactly those tiles. As you level up the grid gets bigger (up to 6 by 6) and more tiles appear. One wrong tap ends the game: the wrong tile turns rose and the tiles you missed turn purple.",
    "words" to "A list of words is shown for a few seconds. Then you see a mix of those words and new ones. Select exactly the words you saw. Every 3 levels the list gets longer. When you fail, missed words turn purple and wrong picks turn rose.",
    "dual" to "Two tasks at once: the tile position and a letter. Press POSITION MATCH if the tile is where it was N steps ago, and LETTER MATCH if the letter is the same as N steps ago. The tile colour alternates so you can tell when it moves.",
    "corsi" to "Like Sequence Memory on a bigger 4 by 4 board. Watch the blocks light up, then repeat the order. A mistake shows the wrong block in rose and the right one in purple.",
    "rev" to "Digits flash on screen. Then type them BACKWARDS using the keypad. 4-1-9 becomes 9-1-4. Every 3 levels you get one more digit. When you fail you see the correct answer and your answer.",
    "flash" to "Pick a deck. A card shows a question: think of the answer, tap the card to flip it, then press Knew it or Again. Cards you miss come back until you know them. Your score is the percentage you knew on the first try. Difficulty sets 10, 20, 30 or 40 cards.",
    "react" to "Single: tap as soon as the box turns green. Average of 5: five rounds are averaged. Race lights: five red lights come on one by one at a steady beat. After the fifth red light, wait exactly one more beat and all lights turn green. Tap then. Tapping early is a false start. On God a yellow decoy flash can appear: do not tap it. Lower milliseconds is better.",
    "gonogo" to "Tap the circle when it is GREEN and do nothing when it is RED. There are 20 rounds. You earn points for each right decision plus a speed bonus for fast taps on green. Each light stays for less time on harder difficulties. Your live score is shown at the top.",
    "stroop" to "A colour word is printed in a different ink colour. Tap the button that matches the INK colour, not the word. Example: the word BLUE written in red means you tap red. You earn points for correct answers and extra points for speed. God has 6 colours and 30 rounds.",
    "schulte" to "Find and tap the numbers in order, 1, 2, 3 and so on, as fast as you can. Keep your eyes near the centre and use your side vision. Lower time is better. The grid is 3 by 3 on Easy and 6 by 6 on God.",
    "cmatch" to "A colour word is shown in some ink colour. Press YES if the meaning of the word matches the ink colour, NO if it does not. You earn points for correct answers and extra points for speed. God has 6 colours and 30 rounds.",
    "whack" to "Tiles light up for a moment. Tap each lit tile before it moves. The game lasts 30 seconds and the tiles move faster on harder difficulties.",
    "odd" to "Find the one tile that is different. The puzzle type changes every round: similar letters, a slightly different shade, filled versus outline shapes, arrows pointing a different way, and swapped digits. Each wrong tap adds 2 seconds. There are 12 rounds and a lower time is better.",
    "flank" to "Look only at the MIDDLE arrow and press the direction it points. The arrows around it may point the opposite way to distract you. Correct answers and speed both add points. On God the side arrows always point the wrong way.",
    "g2048" to "Swipe in any direction to slide all the tiles. Equal numbers merge into their sum. Build big numbers and avoid filling the board. Harder difficulties spawn more 4s.",
    "lights" to "Tapping a tile flips it and its four neighbours. Turn every light off in as few moves as you can.",
    "sudoku" to "Fill the 9 by 9 grid so every row, column and 3 by 3 box contains the numbers 1 to 9. Tap an empty tile, then a number. Dark grey tiles are fixed. Lower time is better.",
    "mines" to "TAP a tile to dig it. LONG-PRESS (hold) a tile to place or remove a flag. Each number shows how many mines touch that tile. Clear every safe tile to win. Your first tap is always safe. When you hit a mine, all mines are shown.",
    "chess" to "You play against a bot. Tap a piece, then a highlighted square to move it. Undo and Redo take moves back and forward. Hint shows a strong move after a short ad. A win scores 100 times (difficulty + 1), a draw scores half. On God the bot looks four moves ahead and may take a few seconds to move.",
    "flappy" to "Tap to flap and fly through the gaps between the pipes. Each pipe you pass is one point. Touching a pipe or the edge ends the game. The first tap starts the game.",
    "snake" to "Swipe to steer the snake. Eat the food to grow and score. Classic: walls and your own tail are deadly. Wrap: you come out the other side of the wall. Obstacles: grey blocks are deadly. Fast: the snake moves much faster. The snake speeds up as it grows.",
    "stack" to "A block slides left and right. Tap to drop it on the tower. Only the part that overlaps the block below stays, so the tower gets narrower if you are off. Land it perfectly to keep the full width. The game ends when you miss completely.",
    "dodge" to "Drag your finger left and right to move the player at the bottom. Dodge the falling blocks. Your score is how many seconds you survive. Blocks come faster the longer you last.",
    "tiles" to "Tap the lit tile in the bottom row. Each correct tap brings the next row down and refills the time bar a little. The time bar keeps draining, faster as your score grows. A wrong tap or an empty bar ends the game."
)

// ---------- State holders ----------
object Live { var txt by mutableStateOf("") }
object Pause { var on by mutableStateOf(false); var lost = 0L }
object Ended { var on by mutableStateOf(false) }
val LocalAccent = compositionLocalOf { Color(0xFF4DD0E1) }
val LocalCell = compositionLocalOf { 56 }
// Game clock: real time minus the time the game spent paused
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

// ---------- Soft synthesized sounds + vibration ----------
object Sfx {
    var sound by mutableStateOf(true)
    var vib by mutableStateOf(true)
    var base = 0
    private var ctr = 0
    private const val RATE = 22050
    private fun mk(freqs: List<Double>, ms: Int, vol: Double): AudioTrack? = try {
        val n = RATE * ms / 1000; val seg = n / freqs.size; val buf = ShortArray(n)
        for (i in 0 until n) {
            val j = i % seg; val f = freqs[minOf(i / seg, freqs.size - 1)]
            val env = minOf(1.0, j / (RATE * 0.006)) * Math.exp(-5.0 * j / seg)
            buf[i] = (Math.sin(2 * Math.PI * f * i / RATE) * env * vol * 32767).toInt().toShort()
        }
        val t = AudioTrack(AudioManager.STREAM_MUSIC, RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, n * 2, AudioTrack.MODE_STATIC)
        t.write(buf, 0, n); t
    } catch (e: Exception) { null }
    private val pent = listOf(523.25, 587.33, 659.25, 783.99, 880.0)
    private val clicks by lazy { pent.map { mk(listOf(it), 70, .16) } }
    private val tk by lazy { mk(listOf(740.0), 35, .10) }
    private val gd by lazy { mk(listOf(659.25, 880.0), 190, .18) }
    private val bd by lazy { mk(listOf(220.0, 174.6), 260, .18) }
    private val wn by lazy { mk(listOf(523.25, 659.25, 783.99, 1046.5), 520, .18) }
    private fun p(t: AudioTrack?) { if (!sound || t == null) return; try { t.stop(); t.reloadStaticData(); t.play() } catch (e: Exception) { } }
    fun click() { ctr++; p(clicks[(base + ctr) % 5]) }
    fun tick() = p(tk)
    fun good() = p(gd)
    fun bad() = p(bd)
    fun win() = p(wn)
    fun buzz(c: Context) {
        if (!vib) return
        try {
            val v = if (Build.VERSION.SDK_INT >= 31) c.getSystemService(VibratorManager::class.java).defaultVibrator else c.getSystemService(Vibrator::class.java)
            if (Build.VERSION.SDK_INT >= 26) v.vibrate(VibrationEffect.createOneShot(250, VibrationEffect.DEFAULT_AMPLITUDE)) else v.vibrate(250)
        } catch (e: Exception) { }
    }
}

// ---------- Ads: change these switches to control ads ----------
object AdCfg {
    const val ENABLED = true            // master switch: false = no ads at all
    const val BANNER = true             // small banner at the bottom
    const val INTERSTITIAL_EVERY = 3    // full-screen ad after every Nth finished game (0 = never)
    const val REWARDED_HINTS = true     // chess hint needs a rewarded ad
    // TEST ids from Google. Replace with your own ad unit ids before release
    const val BANNER_ID = "ca-app-pub-3940256099942544/6300978111"
    const val INTER_ID = "ca-app-pub-3940256099942544/1033173712"
    const val REWARD_ID = "ca-app-pub-3940256099942544/5224354917"
}

object Ads {
    private var inter: InterstitialAd? = null
    private var rew: RewardedAd? = null
    private var n = 0
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
    // Shown only when the player leaves the game-over panel, never during play
    fun maybe(act: Activity, next: () -> Unit) {
        n++; val a = inter
        if (!AdCfg.ENABLED || AdCfg.INTERSTITIAL_EVERY <= 0 || n % AdCfg.INTERSTITIAL_EVERY != 0 || a == null) { next(); return }
        a.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() { inter = null; load(act); next() }
            override fun onAdFailedToShowFullScreenContent(e: AdError) { inter = null; next() }
        }
        a.show(act)
    }
    // Rewarded: used for chess hints. If no ad is ready the reward is given for free.
    fun reward(act: Activity, ok: () -> Unit) {
        val r = rew
        if (!AdCfg.ENABLED || !AdCfg.REWARDED_HINTS || r == null) { ok(); load(act); return }
        r.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() { rew = null; load(act) }
            override fun onAdFailedToShowFullScreenContent(e: AdError) { rew = null; ok() }
        }
        r.show(act) { ok() }
    }
}

// ---------- UI helpers ----------
@Composable fun Banner() {
    if (AdCfg.ENABLED && AdCfg.BANNER) AndroidView(
        factory = { c -> AdView(c).apply { setAdSize(AdSize.BANNER); adUnitId = AdCfg.BANNER_ID; loadAd(AdRequest.Builder().build()) } },
        modifier = Modifier.fillMaxWidth().height(50.dp))
}

// Filled action button (Submit, Match, Retry ...)
@Composable fun Btn(t: String, f: () -> Unit) =
    Button({ Sfx.tick(); f() }, Modifier.padding(4.dp), shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = LocalAccent.current, contentColor = Color.Black)) { Text(t, fontWeight = FontWeight.Bold) }

// Outlined option button (answers and choices that are part of the game)
@Composable fun Opt(t: String, c: Color = Color.Unspecified, f: () -> Unit) {
    val col = if (c == Color.Unspecified) LocalAccent.current else c
    OutlinedButton({ Sfx.tick(); f() }, Modifier.padding(4.dp), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, col.copy(.7f)),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = if (c == Color.Unspecified) Color.White else c)) { Text(t) }
}

// The status text is shown big at the top of the game screen by the main UI
@Composable fun Status(t: String) { SideEffect { Live.txt = t } }

@Composable fun BigText(s: String, c: Color = LocalAccent.current) = Text(s, fontSize = 44.sp, letterSpacing = 6.sp, fontWeight = FontWeight.Bold, color = c)

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
