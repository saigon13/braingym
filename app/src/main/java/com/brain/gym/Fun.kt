package com.brain.gym

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
    val gap = listOf(.34f, .30f, .26f)[d]; val sp = listOf(.32f, .38f, .45f)[d]
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
        .pointerInput(Unit) { detectTapGestures { started = true; v = -.8f; Sfx.click() } }) {
        val w = size.width; val h = size.height
        for (i in 0..2) {
            val x = px[i] * w; val pw = .14f * w
            drawRoundRect(ac, Offset(x - pw / 2, 0f), Size(pw, (gy[i] - gap / 2) * h), CornerRadius(12f))
            drawRoundRect(ac, Offset(x - pw / 2, (gy[i] + gap / 2) * h), Size(pw, h), CornerRadius(12f))
        }
        drawCircle(Color(0xFFFFD54F), .04f * w, Offset(.25f * w, y * h))
    }
}

@Composable fun Snake(d: Int, done: (Double) -> Unit) {
    val ac = LocalAccent.current; val W = 12; val H = 16
    var snake by remember { mutableStateOf(listOf(6 to 8, 5 to 8, 4 to 8)) }
    var dir by remember { mutableStateOf(1 to 0) }; var nd by remember { mutableStateOf(1 to 0) }
    var food by remember { mutableStateOf(9 to 8) }
    val sp = longArrayOf(260, 200, 150)[d]
    Status("Score: ${snake.size - 3} · swipe to steer")
    LaunchedEffect(Unit) {
        while (true) {
            wait(sp - minOf(70L, (snake.size - 3) * 3L))
            dir = nd; val h = snake[0]; val n = (h.first + dir.first) to (h.second + dir.second)
            if (n.first !in 0 until W || n.second !in 0 until H || n in snake.dropLast(1)) { done(snake.size - 3.0); return@LaunchedEffect }
            val eat = n == food
            snake = listOf(n) + (if (eat) snake else snake.dropLast(1))
            if (eat) { Sfx.good(); food = (0 until W * H).map { it % W to it / W }.filter { it !in snake }.random() }
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
        snake.forEachIndexed { i, p -> drawRoundRect(if (i == 0) Color.White else ac, Offset(p.first * cs + 1, p.second * cs + 1), Size(cs - 2, cs - 2), CornerRadius(cs / 3)) }
        drawCircle(Color(0xFFFF5252), cs / 2.6f, Offset(food.first * cs + cs / 2, food.second * cs + cs / 2))
    }
}
