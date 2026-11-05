package com.brain.gym

import android.content.Context
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlin.random.Random

object DeviceId {
    private const val CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789" // no 0/O/1/I ambiguity
    fun get(c: Context): String {
        val p = c.getSharedPreferences("profile", 0)
        var id = p.getString("id", null)
        if (id == null) { id = (1..8).map { CHARS.random(Random) }.joinToString(""); p.edit().putString("id", id).apply() }
        return id
    }
}

// PENDING: a local-only record meaning "I added this ID" — nothing has actually been sent anywhere yet.
// There is no transport layer built that can deliver a request or an accept/reject between two devices.
enum class ReqStatus { PENDING, ACCEPTED }
class Contact(val id: String, var nickname: String, var status: ReqStatus = ReqStatus.PENDING) {
    fun label() = if (status == ReqStatus.PENDING) "Request sent" else "Added"
}

object ContactsStore {
    fun load(c: Context): MutableList<Contact> {
        val p = c.getSharedPreferences("profile", 0)
        return (p.getString("contacts", "") ?: "").split(";").filter { it.contains(":") }
            .map {
                val q = it.split(":")
                val st = q.getOrNull(2)?.let { s -> runCatching { ReqStatus.valueOf(s) }.getOrDefault(ReqStatus.PENDING) } ?: ReqStatus.PENDING
                Contact(q[0], q.getOrElse(1) { q[0] }, st)
            }.toMutableList()
    }
    fun save(c: Context, list: List<Contact>) {
        c.getSharedPreferences("profile", 0).edit()
            .putString("contacts", list.joinToString(";") { "${it.id}:${it.nickname}:${it.status}" }).apply()
    }
}

object ProfileStore {
    fun name(c: Context): String = c.getSharedPreferences("profile", 0).getString("name", "") ?: ""
    fun setName(c: Context, n: String) { c.getSharedPreferences("profile", 0).edit().putString("name", n).apply() }
    fun photoUri(c: Context): String? = c.getSharedPreferences("profile", 0).getString("photo", null)
    fun setPhotoUri(c: Context, u: String?) { c.getSharedPreferences("profile", 0).edit().putString("photo", u).apply() }
}

fun loadBitmap(c: Context, uriStr: String?): ImageBitmap? {
    if (uriStr == null) return null
    return try {
        val uri = Uri.parse(uriStr)
        c.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it)?.asImageBitmap() }
    } catch (e: Exception) { null }
}

// Reflects THIS PHONE's own internet connection only. There is no chat server, so this is not
// presence with another person — just whether this device currently has internet access.
object NetMonitor {
    var online by mutableStateOf(false)
    private var started = false
    fun start(c: Context) {
        if (started) return
        started = true
        try {
            val cm = c.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            fun check(): Boolean {
                val n = cm.activeNetwork ?: return false
                val cap = cm.getNetworkCapabilities(n) ?: return false
                return cap.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            }
            online = check()
            cm.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(n: Network) { online = true }
                override fun onLost(n: Network) { online = check() }
                override fun onCapabilitiesChanged(n: Network, nc: NetworkCapabilities) { online = check() }
            })
        } catch (e: Exception) { }
    }
}