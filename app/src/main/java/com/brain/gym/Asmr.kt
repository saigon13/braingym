package com.brain.gym

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.*

@Composable fun hap(): () -> Unit {
    val v = LocalView.current
    return { if (Sfx.vib) v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY) }
}

// Reacts to finger DOWN and finger UP separately (real keys sound different on press and release)
@Composable fun Modifier.press(onDown: () -> Unit, onUp: () -> Unit): Modifier {
    val dn by rememberUpdatedState(onDown); val up by rememberUpdatedState(onUp)
    return pointerInput(Unit) { awaitEachGesture { val d = awaitFirstDown(); d.consume(); dn(); waitForUpOrCancellation(); up() } }
}

@Composable fun Asmr(id: String) {
    when (id) {
        "keys" -> MechKeys(); "popit" -> BubbleGrid(6, 5, false); "wrap" -> BubbleGrid(9, 7, true); "switches" -> Switches()
        "pen" -> ClickyPen(); "zip" -> Zipper(); "spinner" -> Spinner(); "slime" -> Slime(); "knob" -> Knob(); "buttons" -> StressButtons()
    }
}

@Composable fun Keycap(t: String, w: Int, h: Int, v: Int, clicky: Boolean, hp: () -> Unit) {
    var down by remember { mutableStateOf(false) }
    val base = Color.hsv((v * 38f + 200f) % 360f, .3f, .36f)
    Box(Modifier.padding(5.dp).size(w.dp, h.dp)
        .press({ down = true; Snd.play(if (clicky) "kdown_clicky" else "kdown_thock", v); hp() },
               { down = false; Snd.play(if (clicky) "kup_clicky" else "kup_thock", v) })
        .padding(top = if (down) 4.dp else 0.dp)) {
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
        if (wrap) { if (!st[i]) { st[i] = true; Snd.play("wrap", (0..7).random()); hp() } }
        else { st[i] = !st[i]; Snd.play(if (st[i]) "pop_in" else "pop_out", row); hp() }
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
                val pos = ch.position; ch.consume(); at(pos)
            }
        }
    }) {
        val w = size.width / cols; val h = size.height / rows
        for (r in 0 until rows) for (c in 0 until cols) {
            val p = st[r * cols + c]; val ctr = Offset(c * w + w / 2, r * h + h / 2); val rad = min(w, h) * .42f
            if (wrap) {
                if (p) drawCircle(Color(0x33FFFFFF), rad, ctr, style = Stroke(2f))
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
        Box(Modifier.padding(10.dp).size(116.dp, 56.dp).fast(null) { on[k] = !on[k]; Snd.play(if (on[k]) "sw_on" else "sw_off", k); hp() }) {
            Box(Modifier.fillMaxSize().clip(RoundedCornerShape(28.dp)).background(if (on[k]) Color.hsv(k * 40f, .6f, .85f) else Color(0xFF26262B)))
            Box(Modifier.align(Alignment.CenterStart).offset(x = ox).size(48.dp).clip(CircleShape).background(Color.White))
        }
    } }
}

@Composable fun ClickyPen() {
    val ac = LocalAccent.current; val hp = hap()
    var out by remember { mutableStateOf(false) }; var n by remember { mutableStateOf(0) }
    Text("Clicks: $n", color = Color.Gray)
    Canvas(Modifier.size(200.dp, 420.dp).fast(null) { out = !out; n++; Snd.play(if (out) "pen_in" else "pen_out", n); hp() }) {
        val cx = size.width / 2; val h = size.height
        drawRoundRect(ac, Offset(cx - 28f, h * .12f), Size(56f, h * .66f), CornerRadius(26f))
        drawRoundRect(Color(0xFF2A2A30), Offset(cx - 20f, h * .04f), Size(40f, h * .12f), CornerRadius(12f))
        drawRect(Color(0xFFBDBDBD), Offset(cx - 6f, h * .78f), Size(12f, h * (if (out) .14f else .05f)))
        drawRect(Color(0x33FFFFFF), Offset(cx - 18f, h * .14f), Size(8f, h * .6f))
    }
}

@Composable fun Zipper() {
    val ac = LocalAccent.current; val hp = hap()
    var pos by remember { mutableStateOf(.12f) }
    Canvas(Modifier.size(200.dp, 460.dp).pointerInput(Unit) {
        awaitEachGesture {
            val d = awaitFirstDown(); d.consume()
            val tooth = size.height / 36f
            val off = pos * size.height - d.position.y     // grab the slider where it is, no jump
            var last = pos
            fun move(y: Float) {
                val np = ((y + off) / size.height).coerceIn(.04f, .96f)
                val crossed = (abs(np - last) * size.height / tooth).toInt()
                if (crossed > 0) {
                    repeat(minOf(crossed, 4)) { Snd.play("zipt", (0..5).random(), (.55f + .08f * crossed).coerceAtMost(1f)) }
                    hp(); last = np
                }
                pos = np
            }
            while (true) {
                val ev = awaitPointerEvent(); val ch = ev.changes.firstOrNull() ?: break
                if (!ch.pressed) break
                val y = ch.position.y; ch.consume(); move(y)
            }
        }
    }) {
        val cx = size.width / 2; val h = size.height; val tooth = h / 36f; val sy = pos * h
        for (i in 0 until 36) {
            val y = i * tooth
            val gap = if (y < sy - 30f) 2f else min(70f, 4f + (y - sy + 30f) * .45f)
            val col = if (i % 2 == 0) Color(0xFFC9C9D1) else Color(0xFF8C8C96)
            drawRoundRect(col, Offset(cx - gap - 16f, y), Size(16f, tooth * .78f), CornerRadius(4f))
            drawRoundRect(col, Offset(cx + gap, y + tooth * .39f), Size(16f, tooth * .78f), CornerRadius(4f))
        }
        drawRoundRect(ac, Offset(cx - 30f, sy - 40f), Size(60f, 90f), CornerRadius(18f))
        drawCircle(Color(0xFF14141A), 11f, Offset(cx, sy + 24f))
    }
}

@Composable fun Spinner() {
    val ac = LocalAccent.current; val hp = hap()
    var ang by remember { mutableStateOf(0f) }; var w by remember { mutableStateOf(0f) }; var held by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        var last = 0L; var grain = 0f
        while (true) {
            val t = withFrameNanos { it }
            if (last == 0L) { last = t; continue }
            val dt = ((t - last) / 1e9f).coerceAtMost(.05f); last = t
            if (!held) ang = (ang + w * dt) % 360f
            if (abs(w) > 30f) {
                grain += dt
                if (grain > .07f) { grain = 0f; val a = abs(w); Snd.play("whir", min(7, (a / 140f).toInt()), (a / 700f).coerceIn(.12f, 1f)) }
            }
            if (!held) { w *= (1f - .45f * dt); if (abs(w) < 1f) w = 0f }
        }
    }
    Canvas(Modifier.size(300.dp).pointerInput(Unit) {
        awaitEachGesture {
            val d = awaitFirstDown(); d.consume(); held = true; hp()
            val c = Offset(size.width / 2f, size.height / 2f); val pi = PI.toFloat()
            var la = atan2(d.position.y - c.y, d.position.x - c.x); var lt = d.uptimeMillis
            while (true) {
                val ev = awaitPointerEvent(); val ch = ev.changes.firstOrNull() ?: break
                if (!ch.pressed) break
                val a = atan2(ch.position.y - c.y, ch.position.x - c.x)
                var dl = a - la; if (dl > pi) dl -= 2 * pi; if (dl < -pi) dl += 2 * pi
                val deg = dl * 180f / pi; val dtm = maxOf(1L, ch.uptimeMillis - lt)
                ang += deg; w = w * .5f + (deg / dtm * 1000f) * .5f
                la = a; lt = ch.uptimeMillis; ch.consume()
            }
            held = false
            if (abs(w) < 60f) w += 360f
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
    var sq by remember { mutableStateOf(0f) }; var ox by remember { mutableStateOf(0f) }; var oy by remember { mutableStateOf(0f) }
    LaunchedEffect(Unit) {
        var last = 0L
        while (true) {
            val t = withFrameNanos { it }
            if (last == 0L) { last = t; continue }
            val dt = ((t - last) / 1e9f).coerceAtMost(.05f); last = t
            sq *= (1f - 7f * dt).coerceAtLeast(0f); ox *= (1f - 6f * dt).coerceAtLeast(0f); oy *= (1f - 6f * dt).coerceAtLeast(0f)
        }
    }
    Canvas(Modifier.size(320.dp).pointerInput(Unit) {
        awaitEachGesture {
            val d = awaitFirstDown(); d.consume(); sq = 1f; Snd.play("slime", (0..5).random()); hp()
            var acc = 0f
            while (true) {
                val ev = awaitPointerEvent(); val ch = ev.changes.firstOrNull() ?: break
                if (!ch.pressed) break
                val p = ch.positionChange(); ch.consume()
                ox = (ox + p.x * .6f).coerceIn(-70f, 70f); oy = (oy + p.y * .6f).coerceIn(-70f, 70f); sq = min(1f, sq + .05f)
                acc += abs(p.x) + abs(p.y); if (acc > 70f) { acc = 0f; Snd.play("slime", (0..5).random(), .8f) }
            }
        }
    }) {
        val c = Offset(size.width / 2 + ox, size.height / 2 + oy); val base = size.width * .3f
        val rx = base * (1f + .22f * sq + abs(ox) / 500f); val ry = base * (1f - .18f * sq + abs(oy) / 500f)
        drawOval(ac.copy(alpha = .9f), Offset(c.x - rx, c.y - ry + base * .1f), Size(rx * 2, ry * 2))
        drawOval(Color(0x66FFFFFF), Offset(c.x - rx * .55f, c.y - ry * .6f), Size(rx * .6f, ry * .3f))
    }
}

@Composable fun Knob() {
    val ac = LocalAccent.current; val hp = hap()
    var ang by remember { mutableStateOf(0f) }
    Canvas(Modifier.size(290.dp).pointerInput(Unit) {
        awaitEachGesture {
            val d = awaitFirstDown(); d.consume()
            val c = Offset(size.width / 2f, size.height / 2f); val pi = PI.toFloat()
            var last = atan2(d.position.y - c.y, d.position.x - c.x); var acc = 0f
            while (true) {
                val ev = awaitPointerEvent(); val ch = ev.changes.firstOrNull() ?: break
                if (!ch.pressed) break
                val a = atan2(ch.position.y - c.y, ch.position.x - c.x); ch.consume()
                var dl = a - last; if (dl > pi) dl -= 2 * pi; if (dl < -pi) dl += 2 * pi; last = a
                val deg = dl * 180f / pi; ang += deg; acc += deg
                while (abs(acc) >= 15f) { acc -= 15f * sign(acc); Snd.play("detent", (0..2).random()); hp() }
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
    val sn = listOf("sq_boing", "sq_squeak", "sq_bell", "sq_drop", "sq_squish", "sq_pop")
    for (r in 0..1) Row { for (c in 0..2) { val k = r * 3 + c; StressBtn(k, sz, sn[k], hp) } }
}
