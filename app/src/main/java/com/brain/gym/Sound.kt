package com.brain.gym

import android.content.Context
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.concurrent.Executors
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

// All sounds are generated here. They are played on a background thread so taps never lag.
object Snd {
    private const val R = 22050
    private val ex = Executors.newSingleThreadExecutor()
    private val tracks = HashMap<String, Array<AudioTrack?>>()
    private val rr = HashMap<String, Int>()

    fun play(name: String, v: Int = 0) {
        if (!Sfx.sound) return
        ex.execute {
            try {
                val k = "$name#$v"
                val arr = tracks.getOrPut(k) { val pcm = build(name, v); Array(2) { mk(pcm) } }
                val i = ((rr[k] ?: 0) + 1) % 2; rr[k] = i
                arr[i]?.let { it.stop(); it.reloadStaticData(); it.play() }
            } catch (e: Exception) { }
        }
    }

    private fun mk(pcm: ShortArray): AudioTrack? = try {
        val t = AudioTrack(AudioManager.STREAM_MUSIC, R, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, pcm.size * 2, AudioTrack.MODE_STATIC)
        t.write(pcm, 0, pcm.size); t
    } catch (e: Exception) { null }

    private fun synth(ms: Int, vol: Double, f: (Double) -> Double): ShortArray {
        val n = R * ms / 1000; val fade = R / 200
        return ShortArray(n) { i ->
            val x = f(i.toDouble() / R) * vol * (if (i > n - fade) (n - i).toDouble() / fade else 1.0)
            (x.coerceIn(-1.0, 1.0) * 30000).toInt().toShort()
        }
    }

    private fun tones(fs: List<Double>, ms: Int, vol: Double): ShortArray {
        val seg = R * ms / 1000 / fs.size
        return synth(ms, vol) { t ->
            val i = (t * R).toInt(); val j = i % seg; val f = fs[minOf(i / seg, fs.size - 1)]
            sin(2 * PI * f * t) * minOf(1.0, j / (R * .006)) * exp(-5.0 * j / seg)
        }
    }

    private fun build(name: String, v: Int): ShortArray {
        val rnd = java.util.Random(name.hashCode().toLong() * 31 + v)
        var lp = 0.0; var prev = 0.0; var ph = 0.0
        fun nz() = rnd.nextDouble() * 2 - 1
        return when (name) {
            "c" -> tones(listOf(listOf(523.25, 587.33, 659.25, 783.99, 880.0)[v % 5]), 70, .16)
            "tick" -> tones(listOf(740.0), 35, .10)
            "good" -> tones(listOf(659.25, 880.0), 190, .18)
            "bad" -> tones(listOf(220.0, 174.6), 260, .18)
            "win" -> tones(listOf(523.25, 659.25, 783.99, 1046.5), 520, .18)
            "thock" -> synth(190, .9) { t -> lp += .1 * (nz() - lp); lp * 3.2 * exp(-t * 80) + sin(2 * PI * (92.0 + v * 13) * t) * exp(-t * 26) * .85 + nz() * exp(-t * 700) * .3 }
            "clack" -> synth(150, .8) { t -> val r = if (t > .04) nz() * exp(-(t - .04) * 800) * .35 else 0.0; nz() * exp(-t * 900) * .8 + sin(2 * PI * (2300.0 + v * 90) * t) * exp(-t * 330) * .35 + sin(2 * PI * (170.0 + v * 9) * t) * exp(-t * 55) * .6 + r }
            "pop" -> synth(120, .9) { t -> ph += 2 * PI * maxOf(200.0, 520.0 + v * 55 - 2400 * t) / R; sin(ph) * exp(-t * 38) + nz() * exp(-t * 600) * .25 }
            "snap" -> synth(110, .85) { t -> val x = nz(); val h = x - prev; prev = x; h * exp(-t * 260) * .7 + (if (t > .018 + (v % 3) * .006) h * exp(-(t - .018) * 300) * .5 else 0.0) + sin(2 * PI * 1700 * t) * exp(-t * 220) * .25 }
            "bloop" -> synth(170, .9) { t -> val f = if (v % 2 == 0) 420.0 + (v / 2) * 45 - 1800 * t else 170.0 + (v / 2) * 45 + 1700 * t; ph += 2 * PI * maxOf(120.0, f) / R; sin(ph) * exp(-t * 24) + sin(ph * 2) * exp(-t * 40) * .2 }
            "squish" -> synth(280, .9) { t -> lp += .06 * (nz() - lp); ph += 2 * PI * maxOf(40.0, 150.0 + v * 12 - 330 * t) / R; sin(ph) * exp(-t * 11) * .8 + lp * 2.5 * exp(-t * 18) }
            "zip" -> synth(70, .55) { t -> val x = nz(); val h = x - prev; prev = x; h * (.6 + .4 * sin(2 * PI * 110 * t)) * .8 }
            "spin" -> synth(260, .5) { t -> lp += .08 * (nz() - lp); lp * 4 * sin(PI * t / .26) + sin(2 * PI * (190.0 + v * 20) * t) * .12 * sin(PI * t / .26) }
            "boing" -> synth(420, .8) { t -> ph += 2 * PI * (150.0 + v * 30 + 55 * sin(2 * PI * 13 * t)) / R; sin(ph) * exp(-t * 7) }
            "squeak" -> synth(150, .6) { t -> ph += 2 * PI * (1500.0 + v * 150 + 2600 * t) / R; sin(ph) * exp(-t * 17) * (.7 + .3 * sin(2 * PI * 38 * t)) }
            "switch" -> synth(100, .85) { t -> val r = if (t > .022) nz() * exp(-(t - .022) * 700) * .5 else 0.0; sin(2 * PI * (140.0 + v * 10) * t) * exp(-t * 60) * .7 + nz() * exp(-t * 800) * .5 + r + sin(2 * PI * 950 * t) * exp(-t * 180) * .3 }
            "pen" -> synth(120, .8) { t -> val c2 = if (t > .045) nz() * exp(-(t - .045) * 1100) * .5 + sin(2 * PI * 2600 * t) * exp(-(t - .045) * 400) * .25 else 0.0; nz() * exp(-t * 1200) * .7 + sin(2 * PI * 3000 * t) * exp(-t * 500) * .4 + c2 }
            "detent" -> synth(45, .7) { t -> nz() * exp(-t * 1500) * .6 + sin(2 * PI * 1300 * t) * exp(-t * 450) * .35 }
            "drop" -> synth(220, .8) { t -> ph += 2 * PI * (560.0 + v * 70 + 1500 * t) / R; sin(ph) * exp(-t * 22) }
            else -> synth(450, .6) { t -> (sin(2 * PI * (760.0 + v * 110) * t) + .5 * sin(2 * PI * (760.0 + v * 110) * 2.76 * t)) * exp(-t * 7) * .6 }
        }
    }
}

object Sfx {
    var sound by mutableStateOf(true)
    var vib by mutableStateOf(true)
    var base = 0
    private var ctr = 0
    fun click() { ctr++; Snd.play("c", base + ctr) }
    fun tick() = Snd.play("tick")
    fun good() = Snd.play("good")
    fun bad() = Snd.play("bad")
    fun win() = Snd.play("win")
    fun buzz(c: Context) {
        if (!vib) return
        try {
            val v = if (Build.VERSION.SDK_INT >= 31) c.getSystemService(VibratorManager::class.java).defaultVibrator else c.getSystemService(Vibrator::class.java)
            if (!v.hasVibrator()) return
            if (Build.VERSION.SDK_INT >= 33) v.vibrate(VibrationEffect.createOneShot(300, 255), VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ALARM))
            else if (Build.VERSION.SDK_INT >= 26) v.vibrate(VibrationEffect.createOneShot(300, 255))
            else v.vibrate(300)
        } catch (e: Exception) { }
    }
}
