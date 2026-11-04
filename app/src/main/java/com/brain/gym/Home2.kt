package com.brain.gym

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.sqrt

// TODO: replace with the real published Terms / Privacy Policy URL before release.
const val POLICY_URL = "https://example.com/braingym-policy"
val IG_BLUE = Color(0xFF3897F0)

// Portal / iris transition: the old home is punched out by a growing circle centred on `origin`,
// revealing the new home behind it, with glowing rings drawn over the seam.
@Composable fun PortalReveal(p: Float, origin: Offset, front: @Composable () -> Unit, back: @Composable () -> Unit) {
    val ac = LocalAccent.current
    val density = LocalDensity.current
    val config = LocalConfiguration.current
    val wPx = remember(config, density) { with(density) { config.screenWidthDp.dp.toPx() } }
    val hPx = remember(config, density) { with(density) { config.screenHeightDp.dp.toPx() } }
    val maxR = remember(wPx, hPx) { sqrt(wPx * wPx + hPx * hPx) }
    val radius = maxR * p
    // Only true while the animation is actually running, so the touch-blocking layer below
    // only starts or stops twice total (not every frame).
    val blocking = p in 0.001f..0.999f

    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().graphicsLayer {
            val s = .8f + .2f * p
            scaleX = s; scaleY = s
            rotationX = (1f - p) * -10f
            cameraDistance = 16f * density
            alpha = (p * 1.5f).coerceIn(0f, 1f)
        }) { back() }

        Box(Modifier.fillMaxSize().graphicsLayer {
            val s = 1f - .08f * p
            scaleX = s; scaleY = s
            rotationX = p * 8f
            cameraDistance = 16f * density
            alpha = (1f - p * 1.3f).coerceAtLeast(0f)
        }.drawWithContent {
            val path = Path().apply { addOval(Rect(center = origin, radius = radius.coerceAtLeast(1f))) }
            clipPath(path, clipOp = ClipOp.Difference) { this@drawWithContent.drawContent() }
        }) { front() }

        if (blocking) Canvas(Modifier.fillMaxSize()) {
            for (k in 0..2) {
                val rr = radius * (1f - k * .04f)
                val a = (1f - p) * (1f - k * .3f)
                drawCircle(ac.copy(alpha = a.coerceIn(0f, .75f)), rr, origin, style = Stroke(width = 5f - k))
            }
        }

        Box(Modifier.fillMaxSize().pointerInput(blocking) {
            if (blocking) {
                awaitPointerEventScope {
                    while (true) { awaitPointerEvent().changes.forEach { it.consume() } }
                }
            }
        })
    }
}

@Composable fun Avatar(name: String, photo: ImageBitmap?, size: Int, bg: Color) {
    Box(Modifier.size(size.dp).clip(CircleShape).background(if (photo != null) Color.Transparent else bg), Alignment.Center) {
        if (photo != null) Image(photo, null, Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop)
        else Text(if (name.isNotBlank()) name.trim().first().uppercase() else "?", color = Color.White, fontWeight = FontWeight.Bold, fontSize = (size / 2.4).sp)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun NewHome(onBack: () -> Unit, onProfile: () -> Unit) {
    val ctx = LocalContext.current
    var query by remember { mutableStateOf("") }
    val contacts = remember { mutableStateListOf<Contact>().apply { addAll(ContactsStore.load(ctx)) } }
    var photo by remember { mutableStateOf(loadBitmap(ctx, ProfileStore.photoUri(ctx))) }
    var name by remember { mutableStateOf(ProfileStore.name(ctx)) }

    Column(Modifier.fillMaxSize().background(Color.Black).padding(16.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton({ Sfx.tick(); onBack() }) { Icon(Icons.Rounded.ArrowBack, null, tint = Color.White) }
            Spacer(Modifier.weight(1f))
            Box(Modifier.clip(CircleShape).clickable { Sfx.tick(); onProfile() }) { Avatar(name, photo, 38, IG_BLUE) }
        }
        OutlinedTextField(
            value = query, onValueChange = { query = it },
            placeholder = { Text("Search by ID", color = Color(0xFF8A8A8E)) },
            leadingIcon = { Icon(Icons.Rounded.Search, null, tint = Color(0xFF8A8A8E)) },
            singleLine = true, modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = IG_BLUE, unfocusedBorderColor = Color(0xFF2A2A2E),
                focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                cursorColor = IG_BLUE, focusedContainerColor = Color(0xFF0E0E0E), unfocusedContainerColor = Color(0xFF0E0E0E)
            )
        )
        val q = query.trim().uppercase()
        if (q.length in 1..8) {
            Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Avatar(q, null, 34, Color(0xFF3A3A3E))
                Spacer(Modifier.width(10.dp))
                Text(q, color = Color.White, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                if (q.length == 8 && contacts.none { it.id == q }) TextButton({
                    Sfx.tick(); contacts.add(Contact(q, q)); ContactsStore.save(ctx, contacts); query = ""
                }) { Text("Add", color = IG_BLUE) }
            }
            Divider(color = Color(0xFF1C1C1C))
            Spacer(Modifier.height(8.dp))
        }
        if (contacts.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.PersonSearch, null, tint = Color(0xFF3A3A3E), modifier = Modifier.size(56.dp))
                    Spacer(Modifier.height(10.dp))
                    Text("No contacts yet", color = Color(0xFF6E6E72))
                    Text("Search an 8-character ID to add someone", color = Color(0xFF4A4A4E), fontSize = 12.sp, textAlign = TextAlign.Center)
                }
            }
        } else {
            LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
                items(contacts) { ct ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Avatar(ct.nickname, null, 46, Color(0xFF2E2E32))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(ct.nickname, color = Color.White, fontWeight = FontWeight.Medium)
                            Text(ct.id, color = Color(0xFF6E6E72), fontSize = 12.sp, letterSpacing = 1.sp)
                        }
                    }
                }
            }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun ProfileSheet(ins: Insights, st: Store, onClose: () -> Unit) {
    val ctx = LocalContext.current
    var name by remember { mutableStateOf(ProfileStore.name(ctx)) }
    var photoUri by remember { mutableStateOf(ProfileStore.photoUri(ctx)) }
    var photo by remember { mutableStateOf(loadBitmap(ctx, photoUri)) }
    var showData by remember { mutableStateOf(false) }
    val id = remember { DeviceId.get(ctx) }
    val clip = LocalClipboardManager.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null) { ProfileStore.setPhotoUri(ctx, uri.toString()); photoUri = uri.toString(); photo = loadBitmap(ctx, photoUri) }
    }
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton({ Sfx.tick(); onClose() }) { Icon(Icons.Rounded.ArrowBack, null, tint = Color.White) }
                Spacer(Modifier.weight(1f))
                Text("Profile", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.width(40.dp))
            }
            Spacer(Modifier.height(18.dp))
            Box(Modifier.align(Alignment.CenterHorizontally).clip(CircleShape).clickable {
                Sfx.tick(); picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }) { Avatar(name, photo, 96, IG_BLUE) }
            Spacer(Modifier.height(8.dp))
            Text("Tap to change photo", color = Color(0xFF6E6E72), fontSize = 12.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
            Spacer(Modifier.height(20.dp))
            OutlinedTextField(
                name, { name = it; ProfileStore.setName(ctx, it) }, label = { Text("Name") }, singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = IG_BLUE, unfocusedBorderColor = Color(0xFF2A2A2E),
                    focusedTextColor = Color.White, unfocusedTextColor = Color.White, cursorColor = IG_BLUE,
                    focusedContainerColor = Color(0xFF0E0E0E), unfocusedContainerColor = Color(0xFF0E0E0E), focusedLabelColor = IG_BLUE
                )
            )
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xFF111114)).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Your ID", color = Color(0xFF6E6E72), fontSize = 11.sp)
                    Text(id, color = Color.White, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                }
                TextButton({ clip.setText(AnnotatedString(id)); Sfx.tick() }) { Text("Copy", color = IG_BLUE) }
            }
            Spacer(Modifier.height(22.dp))
            Text("SETTINGS", color = Color(0xFF6E6E72), fontSize = 11.sp)
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Sound", color = Color.White, modifier = Modifier.weight(1f)); Switch(Sfx.sound, { Sfx.sound = it; st.setBool("snd", it); Sfx.tick() })
            }
            Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Vibration", color = Color.White, modifier = Modifier.weight(1f)); Switch(Sfx.vib, { Sfx.vib = it; st.setBool("vib", it); Sfx.tick() })
            }
            Row(Modifier.fillMaxWidth().clickable { Sfx.tick(); showData = true }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Privacy & activity data", color = Color.White, modifier = Modifier.weight(1f))
                Icon(Icons.Rounded.ChevronRight, null, tint = Color(0xFF6E6E72))
            }
        }
        if (showData) DataDialog(ins) { showData = false }
    }
}
