package com.brain.gym

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sign
import kotlin.math.sin

@Composable fun hap(): () -> Unit {
    val v = LocalView.current
    return { if (Sfx.vib) v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY) }
}

@Composable fun Asmr(id: String) {
    when (id) {
        "keys" -> MechKeys(); "popit" -> BubbleGrid(6, 5, false); "wrap" -> BubbleGrid(9, 7, true); "switches" -> Switches()
        "pen" -> ClickyPen(); "zip" -> Zipper(); "spinner" -> Spinner(); "slime" -> Slime(); "knob" -> Knob(); "buttons" -> StressButtons()
    }
}

@Composable fun Keycap(t: String, w: Int, h: Int, v: Int, clicky: Boolean, hp: () -> Unit) {
    var down by remember { mutableStateOf(false) }; var n by remember { mutableStateOf(0) }
    LaunchedEffect(n) { if (n > 0) { down = true; delay(110); down = false } }
    val base = Color.hsv((v * 38f + 200f) % 360f, .3f, .36f)
    Box(Modifier.padding(5.dp).size(w.dp, h.dp).fast(null) { Snd.play(if (clicky) "clack" else "thock", v % 8); hp(); n++ }.padding(top = if (down) 4.dp else 0.dp)) {
        Box(Modifier.fillMaxSize().clip(RoundedCornerShape(14.dp)).background(Brush.verticalGradient(listOf(base, Color(0xFF16161A))))
            .border(1.dp, Color(0xFF55555C), RoundedCornerShape(14.dp)), Alignment.Center) {
            Text(t, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD8D8E0))
        }
    }
}

@Composable fun MechKeys() {
    val ac = LocalAccent.current; val hp = hap(); var clicky by remember { mutableStateOf(true) }
    val ks = ((LocalConfiguration.current.screenWidthDp - 56) / 4).coerceAtMost(84)
    val labels = "QWERASDF"
    Row { Chip("Clicky", clicky, ac) { clicky = true }; Spacer(Modifier.width(8.dp)); Chip("Thocky", !clicky, ac) { clicky = false } }
    Spacer(Modifier.height(16.dp))
    for (r in 0..1) Row { for (c in 0..3) { val k = r * 4 + c; Keycap(labels[k].toString(), ks, ks, k, clicky, hp) } }
    Keycap("SPACE", ks * 4, ks * 3 / 4, 3, clicky, hp)
}

@Composable fun BubbleGrid(rows: Int, cols: Int, wrap: Boolean) {
    val hp = hap()
    val st = remember { mutableStateListOf<Boolean>().apply { repeat(rows * cols) { add(false) } } }
    val cs = ((LocalConfiguration.current.screenWidthDp - 48) / cols).coerceAtMost(64)
    val popped = st.count { it }
    LaunchedEffect(popped) { if (wrap && popped == rows * cols) { delay(1000); for (i in st.indices) st[i] = false } }
    fun hit(i: Int, row: Int) {
        if (wrap) { if (!st[i]) { st[i] = true; Snd.play("snap", (0..5).random()); hp() } }
        else { st[i] = !st[i]; Snd.play("bloop", (if (st[i]) 0 else 1) + 2 * (row % 3)); hp() }
    }
    Canvas(Modifier.size((cs * cols).dp, (cs * rows).dp).pointerInput(Unit) {
        awaitEachGesture {
            val d = awaitFirstDown(); d.consume()
            val seen = HashSet<Int>()
            fun at(p: Offset) {
                val c = (p.x / (size.width / cols.toFloat())).toInt(); val r = (p.y / (size.height / rows.toFloat())).toInt()
                if (c in 0 until cols && r in 0 until rows && seen.add(r * cols + c)) hit(r * cols + c, r)
            }
            at(d.position)
            while (true) {
                val ev = awaitPointerEvent(); val ch = ev.changes.firstOrNull() ?: break
                if (!ch.pressed) break
                ch.consume(); at(ch.position)
            }
        }
    }) {
        val w = size.width / cols; val h = size.height / rows
        for (r in 0 until rows) for (c in 0 until cols) {
            val p = st[r * cols + c]; val ctr = Offset(c * w + w / 2, r * h + h / 2); val rad = minOf(w, h) * .42f
            if (wrap) {
                if (p) drawCircle(Color(0x33FFFFFF), rad, ctr, style = androidx.compose.ui.graphics.drawscope.Stroke(2f))
                else { drawCircle(Color(0x5590CAF9), rad, ctr); drawCircle(Color(0x88FFFFFF), rad * .35f, Offset(ctr.x - rad * .3f, ctr.y - rad * .3f)) }
            } else {
                val col = Color.hsv((r * 55f) % 360f, .55f, if (p) .45f else .92f)
                drawCircle(col, rad, ctr)
                if (p) drawCircle(Color(0x66000000), rad * .72f, Offset(ctr.x, ctr.y + rad * .08f))
                else drawCircle(Color(0x88FFFFFF), rad * .3f, Offset(ctr.x - rad * .3f, ctr.y - rad * .3f))
            }
        }
    }
}

@Composable fun Switches() {
    val hp = hap()
    val on = remember { mutableStateListOf<Boolean>().apply { repeat(8) { add(false) } } }
    for (r in 0..3) Row { for (c in 0..1) {
        val k = r * 2 + c
        val ox by animateDpAsState(if (on[k]) 58.dp else 4.dp, label = "sw")
        Box(Modifier.padding(10.dp).size(116.dp, 56.dp).fast(null) { on[k] = !on[k]; Snd.play("switch", k); hp() }) {
            Box(Modifier.fillMaxSize().clip(RoundedCornerShape(28.dp)).background(if (on[k]) Color.hsv(k * 40f, .6f, .85f) else Color(0xFF26262B)))
            Box(Modifier.align(Alignment.CenterStart).offset(x = ox).size(48.dp).clip(CircleShape).background(Color.White))
        }
    } }
}

@Composable fun ClickyPen() {
    val ac = LocalAccent.current; val hp = hap()
    var out by remember { mutableStateOf(false) }; var n by remember { mutableStateOf(0) }
    Text("Clicks: $n", color = Color.Gray)
    Canvas(Modifier.size(200.dp, 420.dp).fast(null) { out = !out; n++; Snd.play("pen"); hp() }) {
        val cx = size.width / 2; val h = size.height
        drawRoundRect(ac, Offset(cx - 28f, h * .12f), Size(56f, h * .66f), CornerRadius(26f))
        drawRoundRect(Color(0xFF2A2A30), Offset(cx - 20f, h * .04f), Size(40f, h * .12f), CornerRadius(12f))
        drawRect(Color(0xFFBDBDBD), Offset(cx - 6f, h * .78f), Size(12f, h * (if (out) .14f else .05f)))
        drawRect(Color(0x33FFFFFF), Offset(cx - 18f, h * .14f), Size(8f, h * .6f))
    }
}

@Composable fun Zipper() {
    val ac = LocalAccent.current; val hp = hap()
    var pos by remember { mutableStateOf(.15f) }; var acc by remember { mutableStateOf(0f) }
    Canvas(Modifier.size(170.dp, 440.dp).pointerInput(Unit) {
        awaitEachGesture {
            val d = awaitFirstDown(); d.consume()
            while (true) {
                val ev = awaitPointerEvent(); val ch = ev.changes.firstOrNull() ?: break
                if (!ch.pressed) break
                ch.consume(); val dy = ch.positionChange().y
                pos = (pos + dy / size.height).coerceIn(.05f, .95f); acc += abs(dy)
                if (acc > 24f) { acc = 0f; Snd.play("zip", (0..3).random()); hp() }
            }
        }
    }) {
        val cx = size.width / 2; val h = size.height; val tooth = h / 40
        for (i in 0 until 40) {
            val y = i * tooth; val off = if (y > pos * h) 16f else 3f
            val col = if (i % 2 == 0) Color(0xFFBDBDBD) else Color(0xFF888888)
            drawRect(col, Offset(cx - off - 14f, y), Size(14f, tooth * .7f))
            drawRect(col, Offset(cx + off, y + tooth * .35f), Size(14f, tooth * .7f))
        }
        drawRoundRect(ac, Offset(cx - 26f, pos * h - 34f), Size(52f, 76f), CornerRadius(16f))
    }
}

@Composable fun Spinner() {
    val ac = LocalAccent.current; val hp = hap()
    var ang by remember { mutableStateOf(0f) }; var w by remember { mutableStateOf(0f) }
    LaunchedEffect(Unit) {
        var last = 0L; var carry = 0f
        while (true) {
            val t = withFrameNanos { it }
            if (last == 0L) { last = t; continue }
            val dt = ((t - last) / 1e9f).coerceAtMost(.05f); last = t
            if (abs(w) > 1f) {
                ang = (ang + w * dt) % 360f; carry += abs(w) * dt
                if (carry > 60f) { carry = 0f; Snd.play("detent"); if (abs(w) > 300f) Snd.play("spin", (0..3).random()) }
                w *= (1f - .55f * dt)
            } else w = 0f
        }
    }
    Canvas(Modifier.size(300.dp).pointerInput(Unit) {
        awaitEachGesture {
            val d = awaitFirstDown(); d.consume(); w += if (w >= 0f) 420f else -420f; hp()
            while (true) {
                val ev = awaitPointerEvent(); val ch = ev.changes.firstOrNull() ?: break
                if (!ch.pressed) break
                ch.consume(); val p = ch.positionChange(); w += (p.x - p.y) * 2.5f
            }
        }
    }) {
        val c = Offset(size.width / 2, size.height / 2); val R = size.width * .3f
        rotate(ang, c) {
            for (k in 0..2) {
                val a = (k * 120f - 90f) * PI.toFloat() / 180f
                val p = Offset(c.x + R * cos(a), c.y + R * sin(a))
                drawLine(Color(0xFF3A3A44), c, p, 46f)
                drawCircle(ac, R * .38f, p); drawCircle(Color(0xFF14141A), R * .2f, p)
            }
            drawCircle(Color(0xFF3A3A44), R * .34f, c); drawCircle(Color(0xFFBDBDBD), R * .16f, c)
        }
    }
}

@Composable fun Slime() {
    val ac = LocalAccent.current; val hp = hap()
    var sq by remember { mutableStateOf(0f) }; var ox by remember { mutableStateOf(0f) }
    LaunchedEffect(Unit) {
        var last = 0L
        while (true) {
            val t = withFrameNanos { it }
            if (last == 0L) { last = t; continue }
            val dt = ((t - last) / 1e9f).coerceAtMost(.05f); last = t
            if (abs(sq) > .002f || abs(ox) > .5f) { sq *= (1f - 7f * dt).coerceAtLeast(0f); ox *= (1f - 6f * dt).coerceAtLeast(0f) }
        }
    }
    Canvas(Modifier.size(320.dp).pointerInput(Unit) {
        awaitEachGesture {
            val d = awaitFirstDown(); d.consume(); sq = 1f; Snd.play("squish", (0..5).random()); hp()
            var acc = 0f
            while (true) {
                val ev = awaitPointerEvent(); val ch = ev.changes.firstOrNull() ?: break
                if (!ch.pressed) break
                ch.consume(); val p = ch.positionChange(); ox = (ox + p.x * .5f).coerceIn(-60f, 60f); sq = minOf(1f, sq + .05f)
                acc += abs(p.x) + abs(p.y); if (acc > 90f) { acc = 0f; Snd.play("squish", (0..5).random()) }
            }
        }
    }) {
        val c = Offset(size.width / 2 + ox, size.height / 2); val base = size.width * .3f
        val rx = base * (1f + .22f * sq); val ry = base * (1f - .18f * sq)
        drawOval(ac.copy(alpha = .9f), Offset(c.x - rx, c.y - ry + base * .1f), Size(rx * 2, ry * 2))
        drawOval(Color(0x66FFFFFF), Offset(c.x - rx * .55f, c.y - ry * .6f), Size(rx * .6f, ry * .3f))
    }
}

@Composable fun Knob() {
    val ac = LocalAccent.current; val hp = hap()
    var ang by remember { mutableStateOf(0f) }; var acc by remember { mutableStateOf(0f) }
    Canvas(Modifier.size(290.dp).pointerInput(Unit) {
        awaitEachGesture {
            val d = awaitFirstDown(); d.consume()
            val c = Offset(size.width / 2f, size.height / 2f)
            var last = atan2(d.position.y - c.y, d.position.x - c.x)
            val pi = PI.toFloat()
            while (true) {
                val ev = awaitPointerEvent(); val ch = ev.changes.firstOrNull() ?: break
                if (!ch.pressed) break
                ch.consume()
                val a = atan2(ch.position.y - c.y, ch.position.x - c.x)
                var dl = a - last; if (dl > pi) dl -= 2 * pi; if (dl < -pi) dl += 2 * pi; last = a
                val deg = dl * 180f / pi; ang += deg; acc += deg
                while (abs(acc) >= 15f) { acc -= 15f * sign(acc); Snd.play("detent"); hp() }
            }
        }
    }) {
        val c = Offset(size.width / 2, size.height / 2); val R = size.width * .36f
        for (k in 0 until 24) {
            val a = k * 15f * PI.toFloat() / 180f
            drawLine(Color(0xFF555560), Offset(c.x + (R + 14f) * cos(a), c.y + (R + 14f) * sin(a)), Offset(c.x + (R + 30f) * cos(a), c.y + (R + 30f) * sin(a)), 4f)
        }
        rotate(ang, c) {
            drawCircle(Color(0xFF2A2A32), R, c); drawCircle(Color(0xFF3A3A44), R * .86f, c)
            drawLine(ac, Offset(c.x, c.y - R * .25f), Offset(c.x, c.y - R * .8f), 12f)
        }
    }
}

@Composable fun StressBtn(v: Int, sz: Int, snd: String, hp: () -> Unit) {
    var down by remember { mutableStateOf(false) }; var n by remember { mutableStateOf(0) }
    LaunchedEffect(n) { if (n > 0) { down = true; delay(140); down = false } }
    val col = Color.hsv(v * 58f, .6f, .9f)
    Box(Modifier.padding(8.dp).size(sz.dp).fast(null) { Snd.play(snd, v); hp(); n++ }.padding(if (down) 7.dp else 0.dp)) {
        Box(Modifier.fillMaxSize().clip(CircleShape).background(Brush.radialGradient(listOf(Color.White.copy(.5f), col, col.copy(alpha = .7f)))))
    }
}

@Composable fun StressButtons() {
    val hp = hap(); val sz = ((LocalConfiguration.current.screenWidthDp - 80) / 3).coerceAtMost(104)
    val sn = listOf("boing", "squeak", "bell", "drop", "squish", "pop")
    for (r in 0..1) Row { for (c in 0..2) { val k = r * 3 + c; StressBtn(k, sz, sn[k], hp) } }
}
