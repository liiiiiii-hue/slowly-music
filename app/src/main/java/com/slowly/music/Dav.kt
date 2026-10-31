package com.slowly.music

import android.util.Base64
import java.net.URI
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

data class RemoteFile(val url:String,val name:String,val directory:Boolean,val title:String="",val artist:String="",val album:String="",val artwork:String="",val tagsRead:Boolean=false)
class Dav(val root: String, user: String, password: String) {
    val authorization = "Basic " + Base64.encodeToString("$user:$password".toByteArray(), Base64.NO_WRAP)
    private val client = OkHttpClient.Builder().followRedirects(false).followSslRedirects(false)
        .connectTimeout(15, TimeUnit.SECONDS).readTimeout(20, TimeUnit.SECONDS).build()
    private fun connection(url: String, method: String, range:String?=null): okhttp3.Response {
        val origin = URI(root)
        val target = URI(url)
        require(target.scheme == "https" && target.host == origin.host && target.port == origin.port) { "只允许访问当前 HTTPS 服务" }
        val request = Request.Builder().url(url).header("Authorization", authorization)
        if(range!=null)request.header("Range",range)
        if (method == "PROPFIND") request.header("Depth", "1").method(method, "".toRequestBody()) else request.get()
        return client.newCall(request.build()).execute()
    }
    fun list(url: String): List<RemoteFile> {
        val c = connection(url, "PROPFIND")
        try {
            check(c.code == 207) { if(c.code == 401) "账号或应用密码不正确，请重新连接" else "目录读取失败：${c.code}" }
            val parser = android.util.Xml.newPullParser()
            parser.setFeature(org.xmlpull.v1.XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
            parser.setInput(c.body!!.byteStream(), null)
            val result = mutableListOf<RemoteFile>()
            var href: String? = null
            var directory = false
            var event = parser.eventType
            while(event != org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
                if(event == org.xmlpull.v1.XmlPullParser.START_TAG && parser.namespace == "DAV:") {
                    when(parser.name) {
                        "response" -> { href = null; directory = false }
                        "href" -> href = parser.nextText()
                        "collection" -> directory = true
                    }
                }
                if(event == org.xmlpull.v1.XmlPullParser.END_TAG && parser.namespace == "DAV:" && parser.name == "response") {
                    href?.let {
                        val resolved = URI(url).resolve(it)
                        if(resolved.path.trimEnd('/') != URI(url).path.trimEnd('/')) {
                            val target = resolved.toASCIIString().let { path -> if(directory && !path.endsWith('/')) "$path/" else path }
                            result.add(RemoteFile(target, resolved.path.trimEnd('/').substringAfterLast('/'), directory))
                        }
                    }
                }
                event = parser.next()
            }
            return result.sortedWith(compareByDescending<RemoteFile> { it.directory }.thenBy { it.name })
        } finally { c.close() }
    }
    fun read(url: String): String {
        val c = connection(url,"GET")
        try { check(c.code == 200) { "歌词读取失败：${c.code}" }; return c.body!!.bytes().toString(Charsets.UTF_8) }
        finally { c.close() }
    }
    fun embedded(url: String): List<com.slowly.lyrics.Line> {
        // Limit tag reading even when a server ignores byte-range requests.
        val origin=URI(root); val target=URI(url)
        require(target.scheme=="https" && target.host==origin.host && target.port==origin.port)
        val request=Request.Builder().url(url).header("Authorization",authorization).header("Range","bytes=0-2097151").build()
        return client.newCall(request).execute().use { response ->
            check(response.code in listOf(200,206))
            val bytes=response.body!!.byteStream().use { input ->
                val out=java.io.ByteArrayOutputStream()
                val buffer=ByteArray(8192)
                while(out.size()<2097152) { val count=input.read(buffer,0,minOf(buffer.size,2097152-out.size())); if(count<0) break; out.write(buffer,0,count) }
                out.toByteArray() }
            com.slowly.lyrics.EmbeddedLyrics.parse(bytes)
        }
    }
    fun tagBytes(url:String):ByteArray {
        fun readRange(range:String,limit:Int):ByteArray=connection(url,"GET",range).use {response->
            check(response.code in listOf(200,206)){"歌曲信息读取失败：${response.code}"}
            response.body!!.byteStream().use{input->val out=java.io.ByteArrayOutputStream();val buffer=ByteArray(8192);while(out.size()<limit){val n=input.read(buffer,0,minOf(buffer.size,limit-out.size()));if(n<0)break;out.write(buffer,0,n)};out.toByteArray()}
        }
        val prefix=readRange("bytes=0-262143",262144)
        if(prefix.size<10||String(prefix,0,3,Charsets.ISO_8859_1)!="ID3")return readRange("bytes=-128",128)
        var size=0;for(i in 6..9){val n=prefix[i].toInt() and 255;require(n<128);size=(size shl 7)or n}
        require(size<=8*1024*1024){"歌曲标签过大，播放时再读取"}
        val count=size+10
        return if(count<=prefix.size)prefix.copyOf(count)else readRange("bytes=0-${count-1}",count)
    }
    fun scan(url: String): List<RemoteFile> {
        val seen = mutableSetOf<String>()
        val result = mutableListOf<RemoteFile>()
        fun visit(path: String) {
            if (!seen.add(path)) return
            check(seen.size <= 10000) { "目录过多，请选择更小的目录" }
            list(path).forEach { if(it.directory) visit(it.url) else result.add(it) }
        }
        visit(url)
        return result
    }
}
