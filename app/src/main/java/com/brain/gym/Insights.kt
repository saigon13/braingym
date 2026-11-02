package com.brain.gym

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// Player activity data. Everything is stored ONLY in this phone's app storage, and only if the player allowed it.
// Collected: opens, sessions, time per day / hour / minute, per-game plays, time and scores, difficulty use, ad counts.
// NOT collected: advertising ID, device ID, location, contacts, anything outside this app.
class Insights(c: Context) {
    private val p = c.getSharedPreferences("ins", 0)
    var on = p.getBoolean("on", false)
        private set
    var asked = p.getBoolean("asked", false)
        private set
    private var first = p.getLong("first", 0L)
    private var openCount = p.getInt("logins", 0)
    private var lastEnd = p.getLong("lastEnd", 0L)
    private var curStart = 0L
    private var rewards = p.getInt("rewards", 0)
    private var interTotal = p.getInt("interTotal", 0)
    private val sessions = ArrayList<LongArray>()               // [start ms, seconds]
    private val days = HashMap<String, Int>()                    // yyyyMMdd -> seconds
    private val hours = IntArray(24); private val wd = IntArray(7); private val diffs = IntArray(4); private val tabs = IntArray(5)
    private val games = HashMap<String, LongArray>()             // plays, seconds, finished, new bests, score sum x10
    private val mins = HashMap<String, BooleanArray>()           // yyyyMMdd -> which of the 1440 minutes were active
    private val adTimes = ArrayList<Long>()
    private var since = 0
    private val df = SimpleDateFormat("yyyyMMdd", Locale.US)

    init { load() }

    private fun dayKey(ms: Long = System.currentTimeMillis()) = df.format(Date(ms))
    private fun ints(s: String?, n: Int): IntArray { val a = IntArray(n); s?.split(",")?.forEachIndexed { i, x -> if (i < n) a[i] = x.toIntOrNull() ?: 0 }; return a }

    private fun load() {
        try {
            p.getString("sessions", "")!!.split(";").filter { it.contains(":") }.forEach { val q = it.split(":"); sessions.add(longArrayOf(q[0].toLong(), q[1].toLong())) }
            p.getString("days", "")!!.split(",").filter { it.contains(":") }.forEach { val q = it.split(":"); days[q[0]] = q[1].toInt() }
            ints(p.getString("hours", null), 24).copyInto(hours); ints(p.getString("wd", null), 7).copyInto(wd)
            ints(p.getString("diffs", null), 4).copyInto(diffs); ints(p.getString("tabs", null), 5).copyInto(tabs)
            p.getString("games", "")!!.split(";").filter { it.contains(":") }.forEach {
                val q = it.split(":"); games[q[0]] = LongArray(5) { i -> q.getOrNull(i + 1)?.toLongOrNull() ?: 0L }
            }
            p.getString("mins", "")!!.split(";").filter { it.contains("=") }.forEach {
                val q = it.split("="); val b = BooleanArray(1440); q[1].forEachIndexed { i, ch -> if (i < 1440) b[i] = ch == '1' }; mins[q[0]] = b
            }
            p.getString("ads", "")!!.split(",").mapNotNull { it.toLongOrNull() }.forEach { adTimes.add(it) }
        } catch (e: Exception) { }
    }

    fun save() {
        since = 0
        val kd = days.keys.sorted().takeLast(120); val km = mins.keys.sorted().takeLast(14)
        p.edit()
            .putString("sessions", sessions.joinToString(";") { "${it[0]}:${it[1]}" })
            .putString("days", kd.joinToString(",") { "$it:${days[it]}" })
            .putString("hours", hours.joinToString(",")).putString("wd", wd.joinToString(","))
            .putString("diffs", diffs.joinToString(",")).putString("tabs", tabs.joinToString(","))
            .putString("games", games.entries.joinToString(";") { (k, v) -> "$k:" + v.joinToString(":") })
            .putString("mins", km.joinToString(";") { k -> k + "=" + mins[k]!!.joinToString("") { if (it) "1" else "0" } })
            .putString("ads", adTimes.joinToString(","))
            .putLong("first", first).putInt("logins", openCount).putLong("lastEnd", lastEnd).putInt("rewards", rewards).putInt("interTotal", interTotal)
            .apply()
    }

    fun consent(v: Boolean) {
        on = v; asked = true
        p.edit().putBoolean("on", v).putBoolean("asked", true).apply()
        if (v && first == 0L) { first = System.currentTimeMillis(); save() }
    }

    // ---- recording ----
    fun foreground() {
        if (!on) return
        val t = System.currentTimeMillis(); if (first == 0L) first = t
        if (curStart == 0L) { if (t - lastEnd > 300_000L) openCount++; curStart = t }
        save()
    }
    fun background() {
        if (!on || curStart == 0L) return
        val t = System.currentTimeMillis()
        sessions.add(longArrayOf(curStart, (t - curStart) / 1000)); if (sessions.size > 300) sessions.removeAt(0)
        lastEnd = t; curStart = 0L; save()
    }
    fun tick(game: String?) {
        if (!on) return
        val t = System.currentTimeMillis(); val cal = Calendar.getInstance(); val dk = dayKey(t)
        days[dk] = (days[dk] ?: 0) + 1
        hours[cal.get(Calendar.HOUR_OF_DAY)]++; wd[(cal.get(Calendar.DAY_OF_WEEK) + 5) % 7]++
        mins.getOrPut(dk) { BooleanArray(1440) }[cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)] = true
        if (game != null) games.getOrPut(game) { LongArray(5) }[1]++
        if (++since >= 15) save()
    }
    fun gameStarted(id: String, diff: Int) { if (!on) return; games.getOrPut(id) { LongArray(5) }[0]++; diffs[diff.coerceIn(0, 3)]++ }
    fun gameFinished(id: String, score: Double, best: Boolean) {
        if (!on) return
        val g = games.getOrPut(id) { LongArray(5) }; g[2]++; if (best) g[3]++; g[4] += (score * 10).toLong(); save()
    }
    fun tab(i: Int) { if (on && i in 0..4) tabs[i]++ }
    fun adShown() { adTimes.add(System.currentTimeMillis()); if (adTimes.size > 60) adTimes.removeAt(0); interTotal++; if (on) save() }
    fun rewardUsed() { rewards++; if (on) save() }
    fun clear() {
        sessions.clear(); days.clear(); hours.fill(0); wd.fill(0); diffs.fill(0); tabs.fill(0); games.clear(); mins.clear(); adTimes.clear()
        first = if (on) System.currentTimeMillis() else 0L; openCount = 0; lastEnd = 0L; rewards = 0; interTotal = 0; save()
    }

    // ---- numbers used by AdBrain and the Player data screen ----
    fun opens() = openCount
    fun totalSec(): Long = days.values.sum().toLong()
    fun avgSession(): Long = if (sessions.isEmpty()) 0L else sessions.sumOf { it[1] } / sessions.size
    fun activeDays7(): Int = (0..6).count { (days[dayKey(System.currentTimeMillis() - it * 86400000L)] ?: 0) > 0 }
    fun adsToday(): Int = adTimes.count { dayKey(it) == dayKey() }
    private fun weekSec(): Int = (0..6).sumOf { days[dayKey(System.currentTimeMillis() - it * 86400000L)] ?: 0 }
    private fun streak(): Int {
        var s = 0; var t = System.currentTimeMillis()
        if ((days[dayKey(t)] ?: 0) == 0) t -= 86400000L
        while ((days[dayKey(t)] ?: 0) > 0) { s++; t -= 86400000L }
        return s
    }
    private fun longest(): Int {
        var best = 0; var cur = 0; var prev = -1L
        for (k in days.keys.sorted()) {
            val ms = try { df.parse(k)!!.time } catch (e: Exception) { continue }
            cur = if (prev >= 0 && ms - prev <= 129_600_000L) cur + 1 else 1
            if (cur > best) best = cur; prev = ms
        }
        return best
    }

    fun summary(): List<String> {
        if (!on && days.isEmpty()) return listOf("Activity data is switched off.")
        val L = ArrayList<String>()
        val nm = { id: String -> GAMES.firstOrNull { it.id == id }?.name ?: id }
        val dt = SimpleDateFormat("d MMM yyyy, HH:mm", Locale.getDefault())
        L += "First opened: " + (if (first > 0) dt.format(Date(first)) else "—")
        L += "App opens (logins): $openCount · sessions: ${sessions.size}"
        L += "Average session: ${fmt(avgSession())} · longest: ${fmt(sessions.maxOfOrNull { it[1] } ?: 0L)}"
        L += "Time in app: ${fmt(totalSec())} · today: ${fmt((days[dayKey()] ?: 0).toLong())} · last 7 days: ${fmt(weekSec().toLong())}"
        L += "Active days: ${days.count { it.value > 0 }} · current streak: ${streak()} · longest streak: ${longest()}"
        val ph = hours.indices.maxByOrNull { hours[it] }
        L += "Usual time of day: " + (if (ph == null || hours[ph] == 0) "—" else "%02d:00–%02d:00".format(ph, (ph + 1) % 24))
        val bd = wd.indices.maxByOrNull { wd[it] }
        L += "Busiest weekday: " + (if (bd == null || wd[bd] == 0) "—" else listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")[bd])
        L += "Active minutes in the last 14 days: ${mins.values.sumOf { b -> b.count { it } }}"
        val pl = games.values.sumOf { it[0] }; val fi = games.values.sumOf { it[2] }
        L += "Games started: $pl · finished: $fi"
        val dm = diffs.indices.maxByOrNull { diffs[it] }
        L += "Favourite difficulty: " + (if (dm == null || diffs[dm] == 0) "—" else listOf("Easy", "Normal", "Hard", "God")[dm])
        games.entries.sortedByDescending { it.value[1] }.take(5).forEach { (k, v) -> L += "  ${nm(k)}: ${v[0]} plays · ${fmt(v[1])} · ${v[3]} new bests" }
        L += "Ads: $interTotal full-screen ($adsToday today) · $rewards rewarded · pacing tier: ${AdBrain.tier()}"
        return L
    }
}

// Uses the player's own activity to decide WHEN full-screen ads appear. It never changes which ads Google picks.
object AdBrain {
    var ins: Insights? = null
    private var sessionAds = 0; private var lastAd = 0L; private var games = 0

    fun tier(): String {
        val i = ins
        if (i == null || !i.on) return "default"
        return when {
            i.totalSec() < 600 || i.opens() < 3 -> "new"
            i.avgSession() < 240 -> "casual"
            i.activeDays7() >= 5 -> "heavy"
            else -> "regular"
        }
    }

    // gameSec = how long the game that just ended lasted
    fun allow(gameSec: Long): Boolean {
        val t = tier(); games++
        if (t == "new") return false                                    // first minutes: let people enjoy the app
        if (gameSec < 20) return false                                  // quick restarts are never interrupted
        if (System.currentTimeMillis() - lastAd < 180_000L) return false // at least 3 minutes between ads
        if (sessionAds >= (if (t == "heavy") 5 else 3)) return false    // per-session cap
        if ((ins?.adsToday() ?: 0) >= 12) return false                  // per-day cap
        val n = when (t) { "casual" -> 4; "default" -> AdCfg.INTERSTITIAL_EVERY; else -> 3 }
        return n > 0 && games % n == 0
    }

    fun shown() { sessionAds++; lastAd = System.currentTimeMillis(); ins?.adShown() }
}
