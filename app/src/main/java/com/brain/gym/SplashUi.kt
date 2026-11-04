package com.brain.gym

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable fun SplashScreen(onDone: () -> Unit) {
    val scale = remember { Animatable(0.55f) }
    val alpha = remember { Animatable(1f) }
    LaunchedEffect(Unit) {
        scale.animateTo(1f, tween(600, easing = CubicBezierEasing(.22f, 1f, .36f, 1f)))
        delay(420)
        launch { scale.animateTo(5.5f, tween(620, easing = CubicBezierEasing(.55f, 0f, 1f, 1f))) }
        alpha.animateTo(0f, tween(560, delayMillis = 140))
        delay(560)
        onDone()
    }
    Box(Modifier.fillMaxSize().background(Color.Black), Alignment.Center) {
        Image(
            painter = painterResource(R.drawable.ic_launcher),
            contentDescription = null,
            modifier = Modifier.size(140.dp).graphicsLayer {
                scaleX = scale.value; scaleY = scale.value; this.alpha = alpha.value
            }
        )
    }
}
