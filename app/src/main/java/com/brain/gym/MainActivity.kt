package com.brain.gym

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.google.android.gms.ads.MobileAds

class MainActivity : ComponentActivity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        MobileAds.initialize(this)
        Ads.load(this)
        val st = Store(this)
        setContent {
            MaterialTheme(darkColorScheme(background = Color.Black, surface = Color.Black, primary = Color.White, onBackground = Color.White, onSurface = Color.White)) {
                Surface(Modifier.fillMaxSize(), color = Color.Black) {
                    var splash by remember { mutableStateOf(true) }
                    if (splash) SplashScreen { splash = false } else AppV2(st)
                }
            }
        }
    }
}
