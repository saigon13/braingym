package com.brain.gym

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import kotlin.math.abs

@Composable fun Flappy(d: Int, done: (Double) -> Unit) {
    val ac = LocalAccent.current
    var y by remember { mutableStateOf(.5f) }; var v by remember { mutableStateOf(0f) }
    var started by remember { mutableStateOf(false) }
    val px = remember { mutableStateListOf(1.1f, 1.8f, 2.5f) }; val gy = remember { mutableStateListOf(.5f, .4f, .6f) }
    var sc by remember { mutableStateOf(0) }
    val gap = listOf(.34f, .30f, .26f, .22f)[d]; val sp = listOf(.32f, .38f, .45f, .55f)[d]
    Status(if (started) "Score: $sc" else "Tap to start")
    LaunchedEffect(Unit) {
        var last = 0L
        while (true) {
            val t = withFrameNanos { it }
            if (last == 0L) { last = t; continue }
            val dt = ((t - last) / 1e9f).coerceAtMost(.04f); last = t
            if (Pause.on || Ended.on || !started) continue
            v += 2.4f * dt; y += v * dt
            var dead = y < 0f || y > 1f
            for (i in 0..2) {
                val o = px[i]; px[i] = o - sp * dt
                if (o >= .25f && px[i] < .25f) { sc++; Sfx.good() }
                if (px[i] < -.1f) { px[i] = px[i] + 2.1f; gy[i] = .25f + (0..50).random() / 100f }
                if (abs(px[i] - .25f) < .07f && abs(y - gy[i]) > gap / 2) dead = true
            }
            if (dead) { done(sc.toDouble()); return@LaunchedEffect }
        }
    }
    Canvas(Modifier.fillMaxWidth().height(440.dp).clip(RoundedCornerShape(20.dp)).background(Color(0xFF14202B))
        .pointerInput(Unit) { detectTapGestures { if (!Ended.on && !Pause.on) { started = true; v = -.8f; Sfx.click() } } }) {
        val w = size.width; val h = size.height
        for (i in 0..2) {
            val x = px[i] * w; val pw = .14f * w
            drawRoundRect(ac, Offset(x - pw / 2, 0f), Size(pw, (gy[i] - gap / 2) * h), CornerRadius(12f))
            drawRoundRect(ac, Offset(x - pw / 2, (gy[i] + gap / 2) * h), Size(pw, h), CornerRadius(12f))
        }
        drawCircle(Color(0xFFFFD54F), .04f * w, Offset(.25f * w, y * h))
    }
}

@Composable fun Snake(m: String, d: Int, done: (Double) -> Unit) {
    val ac = LocalAccent.current; val W = 12; val H = 16
    val wrap = m == "Wrap"
    val obs = remember {
        if (m == "Obstacles") (0 until W * H).map { it % W to it / W }.filter { it.second in 3 until H - 2 && it.second != 8 && it.first in 1 until W - 1 }.shuffled().take(10).toSet()
        else emptySet<Pair<Int, Int>>()
    }
    var snake by remember { mutableStateOf(listOf(6 to 8, 5 to 8, 4 to 8)) }
    var dir by remember { mutableStateOf(1 to 0) }; var nd by remember { mutableStateOf(1 to 0) }
    var food by remember { mutableStateOf(9 to 8) }
    val base = (longArrayOf(260, 200, 150, 110)[d] * (if (m == "Fast") 0.6 else 1.0)).toLong()
    Status("Score: ${snake.size - 3} · swipe to steer")
    LaunchedEffect(Unit) {
        while (true) {
            wait(maxOf(60L, base - minOf(70L, (snake.size - 3) * 3L)))
            dir = nd; val h = snake[0]
            var nx = h.first + dir.first; var ny = h.second + dir.second
            if (wrap) { nx = (nx + W) % W; ny = (ny + H) % H }
            val n = nx to ny
            if (nx !in 0 until W || ny !in 0 until H || n in snake.dropLast(1) || n in obs) { done(snake.size - 3.0); return@LaunchedEffect }
            val eat = n == food
            snake = listOf(n) + (if (eat) snake else snake.dropLast(1))
            if (eat) { Sfx.good(); food = (0 until W * H).map { it % W to it / W }.filter { it !in snake && it !in obs }.random() }
        }
    }
    Canvas(Modifier.fillMaxWidth(.9f).aspectRatio(W.toFloat() / H).clip(RoundedCornerShape(16.dp)).background(Color(0xFF14201A)).pointerInput(Unit) {
        var tx = 0f; var ty = 0f
        detectDragGestures(
            onDragStart = { tx = 0f; ty = 0f },
            onDragEnd = {
                val c = if (abs(tx) > abs(ty)) (if (tx < 0) -1 to 0 else 1 to 0) else (if (ty < 0) 0 to -1 else 0 to 1)
                if (abs(tx) + abs(ty) > 30 && (c.first != -dir.first || c.second != -dir.second)) { nd = c; Sfx.tick() }
            },
            onDrag = { c, a -> c.consume(); tx += a.x; ty += a.y })
    }) {
        val cs = size.width / W
        obs.forEach { p -> drawRoundRect(Color.Gray, Offset(p.first * cs + 1, p.second * cs + 1), Size(cs - 2, cs - 2), CornerRadius(cs / 5)) }
        snake.forEachIndexed { i, p -> drawRoundRect(if (i == 0) Color.White else ac, Offset(p.first * cs + 1, p.second * cs + 1), Size(cs - 2, cs - 2), CornerRadius(cs / 3)) }
        drawCircle(Color(0xFFFF5252), cs / 2.6f, Offset(food.first * cs + cs / 2, food.second * cs + cs / 2))
    }
}

@Composable fun StackGame(d: Int, done: (Double) -> Unit) {
    val ac = LocalAccent.current
    var layers by remember { mutableStateOf(listOf(0.25f to 0.5f)) }
    var x by remember { mutableStateOf(0f) }; var dirn by remember { mutableStateOf(1f) }; var sc by remember { mutableStateOf(0) }
    val sp = listOf(.55f, .75f, .95f, 1.2f)[d]
    Status("Layers: $sc · tap to drop the block")
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
    Canvas(Modifier.fillMaxWidth().height(440.dp).clip(RoundedCornerShape(20.dp)).background(Color(0xFF14181F))
        .pointerInput(Unit) { detectTapGestures {
            if (Ended.on || Pause.on) return@detectTapGestures
            val (pl, pw) = layers.last()
            val l = maxOf(x, pl); val r = minOf(x + pw, pl + pw); var ow = r - l
            if (ow <= 0.015f) { done(sc.toDouble()) } else {
                var nl = l
                if (pw - ow < 0.012f) { nl = pl; ow = pw }
                layers = layers + (nl to ow); sc++; x = 0f; dirn = 1f; Sfx.good()
            }
        } }) {
        val w = size.width; val h = size.height; val lh = h / 14f
        val vis = layers.takeLast(12)
        vis.forEachIndexed { i, p ->
            drawRoundRect(Color.hsv(((sc - vis.size + i + 1) * 22f).mod(360f), .55f, .9f), Offset(p.first * w, h - (i + 1) * lh), Size(p.second * w, lh - 3f), CornerRadius(10f))
        }
        drawRoundRect(ac, Offset(x * w, h - (vis.size + 1) * lh), Size(layers.last().second * w, lh - 3f), CornerRadius(10f))
    }
}

@Composable fun Dodge(d: Int, done: (Double) -> Unit) {
    val ac = LocalAccent.current
    var px by remember { mutableStateOf(.5f) }
    val bl = remember { mutableListOf<FloatArray>() }
    var frame by remember { mutableStateOf(0) }; var sc by remember { mutableStateOf(0) }
    val rate = listOf(.9f, .65f, .5f, .38f)[d]
    Status("Survived: ${sc}s · drag to move")
    LaunchedEffect(Unit) {
        var last = 0L; var acc = 0f; var el = 0f
        while (true) {
            val t = withFrameNanos { it }
            if (last == 0L) { last = t; continue }
            val dt = ((t - last) / 1e9f).coerceAtMost(.04f); last = t
            if (Pause.on || Ended.on) continue
            el += dt; sc = el.toInt(); acc += dt
            if (acc > rate / (1f + el * .02f)) { acc = 0f; bl.add(floatArrayOf((0..88).random() / 100f, -.06f, .08f + (0..10).random() / 100f)) }
            val spd = .4f + el * .01f + d * .08f
            var dead = false
            for (b in bl) { b[1] += spd * dt; if (b[1] + .06f > .9f && b[1] < .96f && px + .04f > b[0] && px - .04f < b[0] + b[2]) dead = true }
            bl.removeAll { it[1] > 1.1f }
            frame++
            if (dead) { done(sc.toDouble()); return@LaunchedEffect }
        }
    }
    Canvas(Modifier.fillMaxWidth().height(440.dp).clip(RoundedCornerShape(20.dp)).background(Color(0xFF14181F))
        .pointerInput(Unit) { detectDragGestures { c, a -> c.consume(); px = (px + a.x / size.width).coerceIn(.04f, .96f) } }) {
        val f = frame
        val w = size.width; val h = size.height
        for (b in bl) drawRoundRect(Color(0xFFFF5252), Offset(b[0] * w, b[1] * h), Size(b[2] * w, .06f * h), CornerRadius(10f))
        drawRoundRect(ac, Offset((px - .04f) * w, .9f * h), Size(.08f * w, .06f * h), CornerRadius(12f))
    }
}

@Composable fun TapTiles(d: Int, done: (Double) -> Unit) {
    val ac = LocalAccent.current
    val rows = remember { mutableStateListOf<Int>().apply { var p = -1; repeat(5) { val n = ((0..3) - p).random(); add(n); p = n } } }
    var sc by remember { mutableStateOf(0) }; var bar by remember { mutableStateOf(1f) }; var wrong by remember { mutableStateOf(-1) }
    val drain = listOf(.22f, .32f, .45f, .6f)[d]
    Status("Taps: $sc · tap the lit tile in the bottom row")
    LaunchedEffect(Unit) {
        var last = 0L
        while (true) {
            val t = withFrameNanos { it }
            if (last == 0L) { last = t; continue }
            val dt = ((t - last) / 1e9f).coerceAtMost(.05f); last = t
            if (Pause.on || Ended.on || sc == 0) continue
            bar -= (drain + sc * .004f) * dt
            if (bar <= 0f) { done(sc.toDouble()); return@LaunchedEffect }
        }
    }
    Box(Modifier.fillMaxWidth(.9f).height(8.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFF26262B))) {
        Box(Modifier.fillMaxWidth(bar.coerceIn(0f, 1f)).fillMaxHeight().background(ac))
    }
    Spacer(Modifier.height(10.dp))
    Grid(5, 4) { i ->
        val r = 4 - i / 4; val c = i % 4
        val lit = rows[r] == c
        val bg = if (Ended.on && r == 0 && c == wrong) ROSE else if (Ended.on && r == 0 && lit) PURP
            else if (lit && r == 0) Color.White else if (lit) Color(0xFF3A3A55) else Color.Black
        Cell(bg = bg) {
            if (r == 0 && !Ended.on) {
                if (c == rows[0]) {
                    sc++; bar = minOf(1f, bar + .12f)
                    val prev = rows.last(); rows.removeAt(0); rows.add(((0..3) - prev).random())
                } else { wrong = c; done(sc.toDouble()) }
            }
        }
    }
}
