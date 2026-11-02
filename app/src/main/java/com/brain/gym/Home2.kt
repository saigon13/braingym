package com.brain.gym

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.sin

// The home screen splits down the middle: left half slides left, right half slides right, the new screen is revealed behind.
@Composable fun SplitReveal(p: Float, front: @Composable () -> Unit, back: @Composable () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val w = maxWidth; val ac = LocalAccent.current
        Box(Modifier.fillMaxSize().graphicsLayer { val s = .94f + .06f * p; scaleX = s; scaleY = s; alpha = .35f + .65f * p }) { back() }
        Box(Modifier.width(w / 2).fillMaxHeight().align(Alignment.CenterStart).offset(x = -(w / 2) * p).clipToBounds()) {
            Box(Modifier.requiredWidth(w).fillMaxHeight().align(Alignment.CenterStart).background(Color(0xFF0B0B0D))) { front() }
        }
        Box(Modifier.width(w / 2).fillMaxHeight().align(Alignment.CenterEnd).offset(x = (w / 2) * p).clipToBounds()) {
            Box(Modifier.requiredWidth(w).fillMaxHeight().align(Alignment.CenterEnd).background(Color(0xFF0B0B0D))) { front() }
        }
        Box(Modifier.align(Alignment.Center).fillMaxHeight().width(3.dp).graphicsLayer { alpha = sin(PI.toFloat() * p) }.background(ac))
        Box(Modifier.fillMaxSize().pointerInput(Unit) { awaitPointerEventScope { while (true) { awaitPointerEvent().changes.forEach { it.consume() } } } })
    }
}

// Placeholder: tell me what should go here
@Composable fun NewHome(ins: Insights, ac: Color, onBack: () -> Unit, onData: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF1A1030), Color(0xFF05050A)))).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
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

@Composable fun ConsentDialog(onAllow: () -> Unit, onNo: () -> Unit) = AlertDialog(
    onDismissRequest = { },
    title = { Text("Your activity data") },
    text = {
        Text("Brain Gym can keep play statistics on this phone: when you open the app, how long you play, which games you play and your scores.\n\n" +
            "The data stays on your phone. It is only used to decide when ads appear, for example no full-screen ads while you are new, and fewer ads if you play in short bursts.\n\n" +
            "You can see it, switch it off or delete it any time: tap BRAIN GYM, then Player data.", fontSize = 14.sp)
    },
    confirmButton = { TextButton({ Sfx.tick(); onAllow() }) { Text("Allow") } },
    dismissButton = { TextButton({ Sfx.tick(); onNo() }) { Text("No thanks") } }
)

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
