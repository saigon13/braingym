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
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import kotlin.math.*

private const val SR = 44100

// ---------- tiny DSP toolkit used to build the sounds ----------
private class Bq(val kind: Int, f: Double, q: Double) {   // 0 = band-pass, 1 = low-pass, 2 = high-pass
    var b0 = 0.0; var b1 = 0.0; var b2 = 0.0; var a1 = 0.0; var a2 = 0.0
    var x1 = 0.0; var x2 = 0.0; var y1 = 0.0; var y2 = 0.0
    init { set(f, q) }
    fun set(f: Double, q: Double) {
        val w = 2 * PI * min(f, SR * .45) / SR; val al = sin(w) / (2 * q); val c = cos(w); val a0 = 1 + al
        when (kind) {
            0 -> { b0 = al; b1 = 0.0; b2 = -al }
            1 -> { b0 = (1 - c) / 2; b1 = 1 - c; b2 = (1 - c) / 2 }
            else -> { b0 = (1 + c) / 2; b1 = -(1 + c); b2 = (1 + c) / 2 }
        }
        b0 /= a0; b1 /= a0; b2 /= a0; a1 = -2 * c / a0; a2 = (1 - al) / a0
    }
    fun p(x: Double): Double { val y = b0 * x + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2; x2 = x1; x1 = x; y2 = y1; y1 = y; return y }
}

private class B(ms: Int, val r: java.util.Random) {
    val n = SR * ms / 1000
    val d = DoubleArray(n)
    fun nz() = r.nextDouble() * 2 - 1
    // short filtered-noise burst with exponential decay (clicks, snaps, thumps)
    fun burst(st: Double, amp: Double, tau: Double, kind: Int, f: Double, q: Double) {
        val bq = Bq(kind, f, q); val s = (st * SR).toInt(); val len = (tau * 6 * SR).toInt()
        val comp = if (kind == 0) sqrt((SR / 2.0) / (f / q)) * .5 else 1.2
        for (i in 0 until len) { val j = s + i; if (j >= n) break; d[j] += bq.p(nz() * exp(-i / (tau * SR))) * amp * comp }
    }
    // decaying sine = a resonating body (plastic, wood, membrane). Pitch glides from f0 to f1.
    fun ring(st: Double, amp: Double, tau: Double, f0: Double, f1: Double = f0, gl: Double = .02) {
        val s = (st * SR).toInt(); val len = (tau * 6 * SR).toInt(); var ph = 0.0
        for (i in 0 until len) { val j = s + i; if (j >= n) break; val t = i.toDouble() / SR; ph += 2 * PI * (f1 + (f0 - f1) * exp(-t / gl)) / SR; d[j] += sin(ph) * exp(-t / tau) * amp }
    }
    // longer shaped noise (rustle, squelch, rumble)
    fun noise(st: Double, dur: Double, amp: Double, kind: Int, f: Double, q: Double, e: (Double) -> Double) {
        val bq = Bq(kind, f, q); val s = (st * SR).toInt(); val len = (dur * SR).toInt()
        for (i in 0 until len) { val j = s + i; if (j >= n) break; d[j] += bq.p(nz()) * e(i.toDouble() / len) * amp }
    }
    fun out(peak: Double): ShortArray {
        var m = 1e-9; for (x in d) m = max(m, abs(x)); val g = peak / m; val fade = SR / 200
        return ShortArray(n) { i -> (d[i] * g * (if (i > n - fade) (n - i).toDouble() / fade else 1.0) * 32767).toInt().toShort() }
    }
}

private fun mk(r: java.util.Random, ms: Int, peak: Double, f: B.() -> Unit): ShortArray { val b = B(ms, r); b.f(); return b.out(peak) }

private fun build(name: String, v: Int): ShortArray {
    val r = java.util.Random(name.hashCode() * 31L + v)
    val j = 1.0 + ((v % 5) - 2) * .035 + (r.nextDouble() - .5) * .03
    return when (name) {
        "c" -> mk(r, 110, .5) { ring(0.0, 1.0, .025, listOf(523.25, 587.33, 659.25, 783.99, 880.0)[v % 5]) }
        "tick" -> mk(r, 60, .4) { ring(0.0, 1.0, .01, 740.0) }
        "good" -> mk(r, 260, .55) { ring(0.0, 1.0, .05, 659.25); ring(.075, 1.0, .09, 880.0) }
        "bad" -> mk(r, 330, .55) { ring(0.0, 1.0, .06, 220.0); ring(.09, 1.0, .11, 174.6) }
        "win" -> mk(r, 700, .55) { listOf(523.25, 659.25, 783.99, 1046.5).forEachIndexed { i, f -> ring(i * .1, 1.0, .12, f) } }
        // mechanical keyboard: clicky = click leaf + bottom-out thud + plate ping, release click
        "kdown_clicky" -> mk(r, 260, .9) { burst(0.0, 1.0, .0016, 0, 3400 * j, 2.0); ring(0.0, .25, .004, 4100 * j); burst(.006, .7, .0018, 0, 4600 * j, 2.5); burst(.034, .8, .012, 0, 650 * j, 1.1); ring(.034, .75, .045, 150 * j, 120 * j, .01); ring(.034, .12, .02, 2150 * j) }
        "kup_clicky" -> mk(r, 120, .6) { burst(0.0, .8, .0014, 0, 3900 * j, 2.5); ring(0.0, .12, .004, 3300 * j); burst(.004, .35, .01, 0, 420 * j, 1.0); ring(.004, .3, .02, 230 * j) }
        // thocky = deep lubed linear switch, no click
        "kdown_thock" -> mk(r, 260, .9) { burst(0.0, .35, .004, 0, 1250 * j, 1.2); ring(0.0, .9, .05, 118 * j, 98 * j, .012); burst(0.0, .7, .018, 0, 480 * j, .9); ring(0.0, .18, .03, 310 * j) }
        "kup_thock" -> mk(r, 140, .5) { burst(0.0, .5, .006, 0, 950 * j, 1.2); ring(0.0, .35, .03, 190 * j) }
        // silicone pop-it: membrane flips in / out
        "pop_in" -> mk(r, 220, .9) { ring(0.0, .9, .024, 560 * j, 400 * j, .02); burst(0.0, .35, .003, 0, 1500 * j, 1.0); ring(.004, .3, .05, 230 * j) }
        "pop_out" -> mk(r, 180, .75) { ring(0.0, .8, .018, 700 * j, 540 * j, .02); burst(0.0, .25, .002, 0, 1900 * j, 1.0); ring(.003, .2, .035, 290 * j) }
        // bubble wrap: sharp crack with a couple of tiny follow-up crackles
        "wrap" -> mk(r, 140, .95) { burst(0.0, 1.0, .0025, 2, 1600 * j, .7); burst(0.0, .55, .006, 0, 3500 * j, 3.0); ring(0.0, .35, .008, 420 * j, 330 * j, .01); burst(.008 + r.nextDouble() * .006, .6, .002, 2, 2200 * j, .7); burst(.022 + r.nextDouble() * .012, .3, .0018, 2, 2600 * j, .7) }
        "pop" -> mk(r, 220, 1.0) { burst(0.0, 1.0, .004, 2, 900 * j, .7); burst(0.0, .6, .02, 0, 300 * j, .8); ring(0.0, .4, .03, 210 * j, 140 * j, .01) }
        // toggle switch: plastic clack with a snap
        "sw_on" -> mk(r, 160, .95) { burst(0.0, .9, .003, 0, 2800 * j, 3.0); ring(0.0, .6, .022, 215 * j, 190 * j, .01); burst(0.0, .45, .009, 0, 900 * j, 1.0); burst(.013, .5, .003, 0, 1900 * j, 3.0) }
        "sw_off" -> mk(r, 160, .9) { burst(0.0, .8, .003, 0, 2300 * j, 3.0); ring(0.0, .6, .025, 175 * j, 155 * j, .01); burst(0.0, .45, .009, 0, 800 * j, 1.0); burst(.013, .45, .003, 0, 1500 * j, 3.0) }
        // clicky pen: click out (spring + latch) and click back
        "pen_in" -> mk(r, 200, .95) { burst(0.0, .9, .002, 0, 3100 * j, 2.5); ring(.004, .18, .016, 1850 * j); ring(.022, .55, .016, 520 * j, 470 * j, .01); burst(.022, .5, .002, 0, 4400 * j, 2.5) }
        "pen_out" -> mk(r, 220, .9) { burst(0.0, .8, .0022, 0, 2400 * j, 2.5); ring(.004, .15, .02, 1400 * j); ring(.035, .6, .02, 390 * j, 350 * j, .01); burst(.035, .4, .0025, 0, 3300 * j, 2.0) }
        // one zipper tooth
        "zipt" -> mk(r, 40, .8) { burst(0.0, 1.0, .0011, 0, 4300 * j, 2.5); burst(.0015, .6, .002, 0, 2500 * j, 3.0); ring(0.0, .12, .003, 3300 * j) }
        // spinner bearing whir (a short grain, played over and over)
        "whir" -> mk(r, 110, .6) { val f = 150.0 * (1 + v * .5); noise(0.0, .11, 1.0, 0, f, 5.0) { x -> sin(PI * x).pow(2) } }
        // slime: wet squelch with little bubbles
        "slime" -> mk(r, 300, .8) { noise(0.0, .25, .5, 1, 550.0, .8) { x -> sin(PI * x) }; repeat(3 + r.nextInt(3)) { val f0 = 180 + r.nextDouble() * 300; ring(r.nextDouble() * .16, .55, .03, f0, f0 * 1.9, .025) }; burst(r.nextDouble() * .12, .25, .002, 2, 1800.0, .7) }
        // rotary knob detent
        "detent" -> mk(r, 70, .7) { burst(0.0, .8, .0014, 0, 2300 * j, 3.0); ring(0.0, .3, .004, 1150 * j); ring(0.0, .4, .012, 135 * j) }
        // stress buttons
        "sq_boing" -> mk(r, 450, .8) { var ph = 0.0; for (i in 0 until n) { val t = i.toDouble() / SR; val f = 130 * j + 170 * exp(-t / .2) + 18 * sin(2 * PI * 11 * t) * exp(-t / .25); ph += 2 * PI * f / SR; d[i] += (sin(ph) + .35 * sin(2 * ph + 1)) * exp(-t / .17) } }
        "sq_squeak" -> mk(r, 190, .7) { var ph = 0.0; for (i in 0 until n) { val t = i.toDouble() / SR; val u = min(1.0, t / .16); val f = 1000 * j + 1500 * sin(PI * u); ph += 2 * PI * f / SR; val env = sin(PI * u); d[i] += (sin(ph) + .5 * sin(2 * ph)) * env + nz() * .04 * env } }
        "sq_bell" -> mk(r, 800, .7) { val bs = 880 * j; listOf(1.0 to .6, 2.76 to .4, 5.4 to .25, 8.93 to .15).forEach { (m, a) -> ring(0.0, a, .24 / m, bs * m) } }
        "sq_drop" -> mk(r, 320, .8) { ring(0.0, .9, .07, 420 * j, 1250 * j, .02); ring(.13, .4, .05, 520 * j, 1500 * j, .02) }
        "sq_squish" -> mk(r, 300, .8) { noise(0.0, .25, .6, 1, 500.0, .8) { x -> sin(PI * x) }; ring(0.0, .7, .09, 170 * j, 95 * j, .05) }
        "sq_pop" -> mk(r, 150, .9) { ring(0.0, .9, .03, 720 * j, 340 * j, .015); burst(0.0, .4, .004, 0, 1250 * j, 1.0) }
        // home screen split: crack, deep rumble, rising whoosh
        "split" -> mk(r, 1100, .9) {
            burst(0.0, 1.0, .006, 2, 700.0, .7); ring(0.0, .9, .25, 70.0, 48.0, .05)
            noise(0.0, .9, .7, 1, 110.0, .7) { x -> sin(PI * x).pow(2) }
            val bq = Bq(0, 300.0, 1.2)
            for (i in (.05 * SR).toInt() until (.85 * SR).toInt()) {
                val x = (i.toDouble() / SR - .05) / .8
                if (i % 64 == 0) bq.set(250 + 1900 * x * x, 1.2)
                d[i] += bq.p(nz()) * sin(PI * x) * 1.4
            }
        }
        else -> mk(r, 100, .5) { ring(0.0, 1.0, .03, 660.0) }
    }
}

private val VARS = mapOf("c" to 5, "kdown_clicky" to 8, "kup_clicky" to 8, "kdown_thock" to 8, "kup_thock" to 8, "pop_in" to 6, "pop_out" to 6,
    "wrap" to 8, "sw_on" to 4, "sw_off" to 4, "pen_in" to 3, "pen_out" to 3, "zipt" to 6, "whir" to 8, "slime" to 6, "detent" to 3, "pop" to 8)
private val ONE = listOf("tick", "good", "bad", "win", "sq_boing", "sq_squeak", "sq_bell", "sq_drop", "sq_squish", "sq_pop", "split")

// ---------- One mixer, one audio stream: no limit on sounds, very low delay ----------
object Snd {
    private class V(val pcm: ShortArray, val gain: Float) { var pos = 0 }
    private const val CH = 256
    private val cache = ConcurrentHashMap<String, ShortArray>()
    private val ex = Executors.newSingleThreadExecutor()
    private val q = LinkedBlockingQueue<V>()
    @Volatile private var started = false
    private var ctx: Context? = null

    private fun nv(n: String) = VARS[n] ?: 1

    private fun get(n: String, v: Int): ShortArray = cache.getOrPut("$n#$v") { load(n) ?: build(n, v) }

    fun play(name: String, v: Int = 0, gain: Float = 1f) {
        if (!Sfx.sound) return
        start()
        val n = nv(name); val vv = ((v % n) + n) % n
        val hit = cache["$name#$vv"]
        if (hit != null) q.offer(V(hit, gain)) else ex.execute { q.offer(V(get(name, vv), gain)) }
    }

    // builds every sound ahead of time so the first tap is instant
    fun warm(c: Context) {
        ctx = c.applicationContext; start()
        ex.execute { for ((n, k) in VARS) for (v in 0 until k) get(n, v); for (n in ONE) get(n, 0) }
    }

    private fun start() {
        if (started) return
        started = true
        Thread {
            try {
                val minBuf = AudioTrack.getMinBufferSize(SR, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
                val tr = AudioTrack(AudioManager.STREAM_MUSIC, SR, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, maxOf(minBuf, 2048), AudioTrack.MODE_STREAM)
                tr.play()
                val act = ArrayList<V>(); val mix = IntArray(CH); val out = ShortArray(CH)
                while (true) {
                    if (act.isEmpty()) act.add(q.take())
                    q.drainTo(act)
                    java.util.Arrays.fill(mix, 0)
                    val iter = act.iterator()
                    while (iter.hasNext()) {
                        val s = iter.next(); val n = minOf(CH, s.pcm.size - s.pos)
                        for (i in 0 until n) mix[i] += (s.pcm[s.pos + i] * s.gain).toInt()
                        s.pos += n; if (s.pos >= s.pcm.size) iter.remove()
                    }
                    for (i in 0 until CH) { val x = mix[i]; out[i] = (if (x > 32767) 32767 else if (x < -32768) -32768 else x).toShort() }
                    tr.write(out, 0, CH)
                }
            } catch (e: Exception) { started = false }
        }.also { it.isDaemon = true; it.start() }
    }

    // Optional real recordings: put snd_<name>.wav (16-bit PCM) in res/raw and it replaces the made-up sound
    private fun load(name: String): ShortArray? {
        try {
            val c = ctx ?: return null
            val id = c.resources.getIdentifier("snd_$name", "raw", c.packageName); if (id == 0) return null
            val b = c.resources.openRawResource(id).use { it.readBytes() }
            fun u16(o: Int) = (b[o].toInt() and 255) or ((b[o + 1].toInt() and 255) shl 8)
            fun u32(o: Int) = u16(o) or (u16(o + 2) shl 16)
            var p = 12; var ch = 1; var sr = SR; var bits = 16; var off = -1; var len = 0
            while (p + 8 <= b.size) {
                val tag = String(b, p, 4, Charsets.US_ASCII); val l = u32(p + 4)
                if (tag == "fmt ") { ch = u16(p + 10); sr = u32(p + 12); bits = u16(p + 22) }
                else if (tag == "data") { off = p + 8; len = minOf(l, b.size - off); break }
                p += 8 + l + (l and 1)
            }
            if (off < 0 || bits != 16 || ch < 1) return null
            val n = len / (2 * ch)
            val src = FloatArray(n) { i -> u16(off + i * 2 * ch).toShort().toFloat() }
            val m = (n.toLong() * SR / sr).toInt()
            return ShortArray(m) { i ->
                val x = i.toDouble() * sr / SR; val i0 = x.toInt().coerceAtMost(n - 1); val i1 = minOf(i0 + 1, n - 1)
                (src[i0] + (src[i1] - src[i0]) * (x - i0).toFloat()).toInt().toShort()
            }
        } catch (e: Exception) { return null }
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
