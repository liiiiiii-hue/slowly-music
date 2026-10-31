package com.slowly.music

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class MusicList(val id:String=UUID.randomUUID().toString(), val name:String, val songs:List<String> = emptyList(),val cover:String="")
class LibraryStore(context:Context, account:String) {
    private val prefs=context.getSharedPreferences("library-"+account.hashCode(),Context.MODE_PRIVATE)
    var musicFolder:String
        get()=prefs.getString("folder","")?:""
        set(value){prefs.edit().putString("folder",value).apply()}
    fun songs():List<RemoteFile> = runCatching {
        val a=JSONArray(prefs.getString("songs","[]")); (0 until a.length()).map { val o=a.getJSONObject(it); RemoteFile(o.getString("url"),o.getString("name"),false,o.optString("title"),o.optString("artist"),o.optString("album"),o.optString("artwork"),o.optBoolean("tagsRead")) }
    }.getOrDefault(emptyList())
    fun saveSongs(files:List<RemoteFile>) {val a=JSONArray();files.forEach{a.put(JSONObject().put("url",it.url).put("name",it.name).put("title",it.title).put("artist",it.artist).put("album",it.album).put("artwork",it.artwork).put("tagsRead",it.tagsRead))};prefs.edit().putString("songs",a.toString()).apply()}
    fun lists():List<MusicList> = runCatching {
        val a=JSONArray(prefs.getString("lists","[]"));(0 until a.length()).map { val o=a.getJSONObject(it);val tracks=o.getJSONArray("songs");MusicList(o.getString("id"),o.getString("name"),(0 until tracks.length()).map{tracks.getString(it)},o.optString("cover")) }
    }.getOrDefault(emptyList())
    fun saveLists(lists:List<MusicList>) {val a=JSONArray();lists.forEach{a.put(JSONObject().put("id",it.id).put("name",it.name).put("songs",JSONArray(it.songs)).put("cover",it.cover))};prefs.edit().putString("lists",a.toString()).apply()}
}
