package com.slowly.music

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest

/** App-private persistence; never writes to the remote drive. */
class LyricStore(context: Context) {
    var accountScope: String = ""
    private val prefs = context.getSharedPreferences("lyric-library", Context.MODE_PRIVATE)
    private fun key(url: String) = MessageDigest.getInstance("SHA-256").digest((accountScope+"\n"+url).toByteArray()).joinToString("") { "%02x".format(it) }
    fun cached(url: String): String? = prefs.getString("raw:${key(url)}", null)
    fun cache(url: String, raw: String) { prefs.edit().putString("raw:${key(url)}", raw).apply() }
    fun manual(song: String): String? = prefs.getString("match:${key(song)}",null)
    fun select(song: String, lyric: String) { prefs.edit().putString("match:${key(song)}",lyric).apply() }
    fun saveIndex(root: String, folder: String, files: List<RemoteFile>) {
        val json=JSONArray()
        files.forEach { json.put(JSONObject().put("url",it.url).put("name",it.name)) }
        prefs.edit().putString("folder:${key(root)}",folder).putString("index:${key(root)}",json.toString()).apply()
    }
    fun folder(root: String) = prefs.getString("folder:${key(root)}", "") ?: ""
    fun index(root: String): List<RemoteFile> = runCatching {
        val json=JSONArray(prefs.getString("index:${key(root)}","[]"))
        (0 until json.length()).map { val obj=json.getJSONObject(it); RemoteFile(obj.getString("url"),obj.getString("name"),false) }
    }.getOrDefault(emptyList())
}
