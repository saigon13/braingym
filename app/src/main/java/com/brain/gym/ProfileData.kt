package com.brain.gym

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
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

class Contact(val id: String, var nickname: String)

object ContactsStore {
    fun load(c: Context): MutableList<Contact> {
        val p = c.getSharedPreferences("profile", 0)
        return (p.getString("contacts", "") ?: "").split(";").filter { it.contains(":") }
            .map { val q = it.split(":", limit = 2); Contact(q[0], q.getOrElse(1) { q[0] }) }.toMutableList()
    }
    fun save(c: Context, list: List<Contact>) {
        c.getSharedPreferences("profile", 0).edit().putString("contacts", list.joinToString(";") { "${it.id}:${it.nickname}" }).apply()
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
