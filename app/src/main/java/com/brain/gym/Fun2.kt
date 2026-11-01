package com.brain.gym

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

@Composable fun FlappyBall(d: Int, done: (Double) -> Unit) {
    val ac = LocalAccent.current
    val s = remember { floatArrayOf(.5f, 0f) }
    val px = remember { floatArrayOf(1.1f, 1.8f, 2.5f) }; val gy = remember { floatArrayOf(.5f, .4f, .6f) }
    var started by remember { mutableStateOf(false) }; var frame by remember { mutableStateOf(0) }; var sc by remember { mutableStateOf(0) }
    val gap = listOf(.34f, .30f, .26f, .22f)[d]; val sp = listOf(.32f, .38f, .45f, .55f)[d]
    UseHub(down = { if (!Ended.on && !Pause.on) { started = true; s[1] = -.8f; Sfx.click() } })
    Status(if (started) "Score: $sc" else "Tap anywhere to start")
    LaunchedEffect(Unit) {
        var last = 0L
        while (true) {
            val t = withFrameNanos { it }
            if (last == 0L) { last = t; continue }
            val dt = ((t - last) / 1e9f).coerceAtMost(.04f); last = t
            if (Pause.on || Ended.on || !started) continue
            s[1] += 2.4f * dt; s[0] += s[1] * dt
            var dead = s[0] < 0f || s[0] > 1f
            for (i in 0..2) {
                val o = px[i]; px[i] = o - sp * dt
                if (o >= .25f && px[i] < .25f) { sc++; Sfx.good() }
                if (px[i] < -.1f) { px[i] += 2.1f; gy[i] = .25f + (0..50).random() / 100f }
                if (abs(px[i] - .25f) < .07f && abs(s[0] - gy[i]) > gap / 2) dead = true
            }
            frame++
            if (dead) { done(sc.toDouble()); return@LaunchedEffect }
        }
    }
    Canvas(Modifier.fillMaxWidth().height(460.dp).clip(RoundedCornerShape(20.dp)).background(Color(0xFF14202B))) {
        val f = frame
        val w = size.width; val h = size.height
        for (i in 0..2) {
            val x = px[i] * w; val pw = .14f * w
            drawRoundRect(ac, Offset(x - pw / 2, 0f), Size(pw, (gy[i] - gap / 2) * h), CornerRadius(12f))
            drawRoundRect(ac, Offset(x - pw / 2, (gy[i] + gap / 2) * h), Size(pw, h), CornerRadius(12f))
        }
        val c = Offset(.25f * w, s[0] * h)
        drawCircle(Color(0x44FFD54F), .075f * w, c)
        drawCircle(Color(0xFFFFD54F), .045f * w, c)
        drawCircle(Color(0xAAFFFFFF), .014f * w, Offset(c.x - .015f * w, c.y - .015f * w))
    }
}

@Composable fun StackTower2(d: Int, done: (Double) -> Unit) {
    val ac = LocalAccent.current
    var layers by remember { mutableStateOf(listOf(0.25f to 0.5f)) }
    var x by remember { mutableStateOf(0f) }; var dirn by remember { mutableStateOf(1f) }; var sc by remember { mutableStateOf(0) }
    val sp = listOf(.55f, .75f, .95f, 1.2f)[d]
    Status("Layers: $sc · tap anywhere to drop")
    UseHub(down = {
        if (!Ended.on && !Pause.on) {
            val (pl, pw) = layers.last()
            val l = maxOf(x, pl); val r = minOf(x + pw, pl + pw); var ow = r - l
            if (ow <= 0.015f) done(sc.toDouble()) else {
                var nl = l; if (pw - ow < 0.012f) { nl = pl; ow = pw }
                layers = layers + (nl to ow); sc++; x = 0f; dirn = 1f; Sfx.good()
            }
        }
    })
    LaunchedEffect(Unit) {
        var last = 0L
        while (true) {
            val t = withFrameNanos { it }
            if (last == 0L) { last = t; continue }
            val dt = ((t - last) / 1e9f).coerceAtMost(.04f); last = t
            if (Pause.on || Ended.on) continue
            val ww = layers.last().second
            var nx = x + dirn * sp * (1f + sc * .03f) * dt
            if (nx < 0f) { nx = 0f; dirn = 1f }
            if (nx + ww > 1f) { nx = 1f - ww; dirn = -1f }
            x = nx
        }
    }
    Canvas(Modifier.fillMaxWidth().height(460.dp).clip(RoundedCornerShape(20.dp)).background(Color(0xFF14181F))) {
        val w = size.width; val h = size.height; val lh = h / 14f
        val vis = layers.takeLast(12)
        vis.forEachIndexed { i, p ->
            drawRoundRect(Color.hsv(((sc - vis.size + i + 1) * 22f).mod(360f), .55f, .9f), Offset(p.first * w, h - (i + 1) * lh), Size(p.second * w, lh - 3f), CornerRadius(10f))
        }
        drawRoundRect(ac, Offset(x * w, h - (vis.size + 1) * lh), Size(layers.last().second * w, lh - 3f), CornerRadius(10f))
    }
}

val EP = listOf("😀" to "😃", "😄" to "😁", "🙂" to "🙃", "😐" to "😑", "😮" to "😯", "😢" to "😭", "😠" to "😡", "🤔" to "🧐",
    "😏" to "😒", "🐶" to "🐕", "🐱" to "🐈", "🌕" to "🌝", "🍎" to "🍏", "⭐" to "🌟", "❤️" to "🧡", "🔴" to "🟠")

@Composable fun EmojiSpot(d: Int, done: (Double) -> Unit) {
    val s = listOf(4, 5, 6, 6)[d]; var r by remember { mutableStateOf(0) }; var pen by remember { mutableStateOf(0) }; val t0 = remember { now() }
    val order = remember { bagList(EP.size, 10) }
    val odd = remember(r) { (0 until s * s).random() }
    val p = remember(r) { EP[order[minOf(r, 9)]].let { if ((0..1).random() == 0) it else it.second to it.first } }
    Status("Round ${minOf(r + 1, 10)}/10 · find the different emoji · +2s per miss")
    Grid(s, s) { i ->
        val o = i == odd
        Cell(if (o) p.second else p.first) { if (o) { Sfx.good(); r++; if (r >= 10) done((now() - t0) / 1000.0 + pen) } else { Sfx.bad(); pen += 2 } }
    }
}

val RID = dk("🦁👑:The Lion King,🕷️🧑:Spider-Man,❄️👸:Frozen,🚢🧊:Titanic,🐠🔎:Finding Nemo,🦇🌃:Batman,⭐⚔️:Star Wars,🍫🏭:Charlie and the Chocolate Factory,👻🚫:Ghostbusters,🐀🍳:Ratatouille,💍🌋:The Lord of the Rings,🦈🌊:Jaws,🤖🌱:WALL-E,🏠🎈:Up,🧸🤠:Toy Story,🐼🥋:Kung Fu Panda,🦖🏝️:Jurassic Park,🚗⚡:Cars,🧞🪔:Aladdin,🐘🎪:Dumbo,🧙⚡:Harry Potter,🐢🍕:Ninja Turtles")

@Composable fun EmojiRiddle(d: Int, done: (Double) -> Unit) {
    val qs = remember { RID.shuffled().take(10) }
    var i by remember { mutableStateOf(0) }; var ok by remember { mutableStateOf(0) }
    val q = qs[minOf(i, 9)]
    val opts = remember(i) { (listOf(q.second) + (RID.map { it.second } - q.second).shuffled().take(2 + d)).shuffled() }
    Status("Question ${minOf(i + 1, 10)}/10 · correct $ok")
    Text(q.first, fontSize = 64.sp)
    Spacer(Modifier.height(16.dp))
    opts.forEach { o ->
        Opt(o) { if (o == q.second) { ok++; Sfx.good() } else Sfx.bad(); i++; if (i >= 10) done(ok.toDouble()) }
    }
}

@Composable fun BalloonPop(d: Int, done: (Double) -> Unit) {
    val bl = remember { mutableListOf<FloatArray>() }
    var frame by remember { mutableStateOf(0) }; var sc by remember { mutableStateOf(0) }; var lives by remember { mutableStateOf(3) }
    Status("Popped $sc · Lives $lives")
    LaunchedEffect(Unit) {
        var last = 0L; var acc = 0f; var el = 0f
        while (true) {
            val t = withFrameNanos { it }
            if (last == 0L) { last = t; continue }
            val dt = ((t - last) / 1e9f).coerceAtMost(.04f); last = t
            if (Pause.on || Ended.on) continue
            el += dt; acc += dt
            if (acc > listOf(1.0f, .8f, .65f, .5f)[d] / (1f + el * .02f)) {
                acc = 0f
                bl.add(floatArrayOf((10..90).random() / 100f, 1.1f, .07f + (0..4).random() / 100f, (0..359).random().toFloat(), .16f + d * .04f + el * .003f + (0..8).random() / 100f))
            }
            val iter = bl.iterator()
            while (iter.hasNext()) {
                val b = iter.next(); b[1] -= b[4] * dt
                if (b[1] < -.12f) { iter.remove(); lives--; Sfx.bad(); if (lives <= 0) { done(sc.toDouble()); return@LaunchedEffect } }
            }
            frame++
        }
    }
    Canvas(Modifier.fillMaxWidth().height(460.dp).clip(RoundedCornerShape(20.dp)).background(Color(0xFF14202B)).pointerInput(Unit) {
        awaitEachGesture {
            val dn = awaitFirstDown(); dn.consume()
            if (Ended.on || Pause.on) return@awaitEachGesture
            val w = size.width.toFloat(); val h = size.height.toFloat()
            for (k in bl.indices.reversed()) {
                val b = bl[k]; val rw = b[2] * w
                val dx = (dn.position.x - b[0] * w) / (rw * 1.5f); val dy = (dn.position.y - b[1] * h) / (rw * 1.9f)
                if (dx * dx + dy * dy <= 1f) { bl.removeAt(k); sc++; Snd.play("pop", (0..7).random()); break }
            }
        }
    }) {
        val f = frame
        val w = size.width; val h = size.height
        for (b in bl) {
            val rw = b[2] * w; val c = Offset(b[0] * w, b[1] * h)
            drawLine(Color(0x88FFFFFF), Offset(c.x, c.y + rw * 1.25f), Offset(c.x, c.y + rw * 2.6f), 2f)
            drawOval(Color.hsv(b[3], .6f, .95f), Offset(c.x - rw, c.y - rw * 1.25f), Size(rw * 2, rw * 2.5f))
            drawCircle(Color(0x55FFFFFF), rw * .25f, Offset(c.x - rw * .4f, c.y - rw * .6f))
        }
    }
}
