package com.brain.gym

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

// TODO: replace with the real published Terms / Privacy Policy URL before release.
const val POLICY_URL = "https://example.com/braingym-policy"

// Portal / iris transition: the old home is punched out by a growing circle centred on `origin`,
// revealing the new home behind it, with glowing rings drawn over the seam.
@Composable fun PortalReveal(p: Float, origin: Offset, front: @Composable () -> Unit, back: @Composable () -> Unit) {
    val ac = LocalAccent.current
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wPx = with(androidx.compose.ui.platform.LocalDensity.current) { maxWidth.toPx() }
        val hPx = with(androidx.compose.ui.platform.LocalDensity.current) { maxHeight.toPx() }
        val maxR = kotlin.math.sqrt(wPx * wPx + hPx * hPx)
        val radius = maxR * p

        Box(Modifier.fillMaxSize().graphicsLayer {
            val s = .78f + .22f * p
            scaleX = s; scaleY = s
            rotationX = (1f - p) * -14f
            cameraDistance = 14f * density
            alpha = (p * 1.4f).coerceIn(0f, 1f)
        }) { back() }

        Box(Modifier.fillMaxSize().graphicsLayer {
            val s = 1f - .1f * p
            scaleX = s; scaleY = s
            rotationX = p * 10f
            cameraDistance = 14f * density
            alpha = (1f - p * 1.3f).coerceAtLeast(0f)
        }.drawWithContent {
            val path = Path().apply { addOval(Rect(center = origin, radius = radius.coerceAtLeast(1f))) }
            clipPath(path, clipOp = ClipOp.Difference) { this@drawWithContent.drawContent() }
        }) { front() }

        if (p in 0.001f..0.999f) Canvas(Modifier.fillMaxSize()) {
            for (k in 0..2) {
                val rr = radius * (1f - k * .05f)
                val a = (1f - p) * (1f - k * .3f)
                drawCircle(ac.copy(alpha = a.coerceIn(0f, .8f)), rr, origin, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 5f - k))
            }
            val n = 10
            for (i in 0 until n) {
                val ang = (i * 360f / n + p * 180f) * (Math.PI.toFloat() / 180f)
                val pr = radius * (0.92f + 0.08f * sin(p * 20f + i))
                val pt = Offset(origin.x + pr * cos(ang), origin.y + pr * sin(ang))
                drawCircle(ac.copy(alpha = (1f - p) * .9f), 5f, pt)
            }
        }

        Box(Modifier.fillMaxSize().pointerInput(p) {
            if (p in 0.001f..0.999f) awaitPointerEventScope { while (true) { awaitPointerEvent().changes.forEach { it.consume() } } }
        })
    }
}

// Placeholder: tell me what should go here
@Composable fun NewHome(ins: Insights, ac: Color, onBack: () -> Unit, onData: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF140B28), Color(0xFF05050A)))).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("NEW HOME", fontSize = 24.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
            Chip("Back", true, ac) { onBack() }
        }
        Column(Modifier.weight(1f).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Box(Modifier.size(88.dp).clip(CircleShape).background(ac.copy(.16f)), Alignment.Center) { Icon(Icons.Rounded.Dashboard, null, tint = ac, modifier = Modifier.size(48.dp)) }
            Spacer(Modifier.height(14.dp))
            Text("This space is ready", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("Tell me what to put here.", color = Color.Gray, textAlign = TextAlign.Center)
            Spacer(Modifier.height(22.dp))
            Chip("Player data", false, ac) { onData() }
        }
    }
}

@Composable fun TermsGate(onAccept: () -> Unit) {
    var checked by remember { mutableStateOf(false) }
    val uri = LocalUriHandler.current
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF120A24), Color(0xFF05050A)))), Alignment.Center) {
        Column(
            Modifier.fillMaxWidth(.88f).clip(RoundedCornerShape(22.dp)).background(Color(0xFF15151C)).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Welcome", fontSize = 22.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = checked, onCheckedChange = { checked = it; Sfx.tick() })
                Text("I accept the Terms, Conditions and Privacy Policy", fontSize = 13.sp, modifier = Modifier.weight(1f))
            }
            TextButton({ Sfx.tick(); uri.openUri(POLICY_URL) }) { Text("View Terms, Conditions & Privacy Policy", fontSize = 12.sp) }
            Spacer(Modifier.height(8.dp))
            Button(
                { Sfx.tick(); onAccept() }, enabled = checked,
                modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4DD0E1), disabledContainerColor = Color(0xFF4DD0E1).copy(alpha = .25f))
            ) { Text("OK", fontWeight = FontWeight.Bold, color = Color.Black) }
        }
    }
}

@Composable fun DataDialog(ins: Insights, onClose: () -> Unit) {
    var on by remember { mutableStateOf(ins.on) }; var tick by remember { mutableStateOf(0) }
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Player data (on this phone)") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Collect activity data", Modifier.weight(1f))
                    Switch(on, { on = it; ins.consent(it); if (it) ins.foreground(); tick++; Sfx.tick() })
                }
                Text("Stays on this device. Used only to decide when ads appear.", fontSize = 12.sp, color = Color.Gray)
                Spacer(Modifier.height(10.dp))
                key(tick) { ins.summary().forEach { Text(it, fontSize = 13.sp, modifier = Modifier.padding(vertical = 2.dp)) } }
            }
        },
        confirmButton = { TextButton({ Sfx.tick(); onClose() }) { Text("Close") } },
        dismissButton = { TextButton({ Sfx.tick(); ins.clear(); tick++ }) { Text("Delete my data") } }
    )
}