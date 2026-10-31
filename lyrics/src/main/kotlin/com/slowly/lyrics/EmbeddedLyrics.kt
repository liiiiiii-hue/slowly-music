package com.slowly.lyrics

/** Reads ID3v2.3/2.4 SYLT with millisecond timing, and timestamped USLT. */
object EmbeddedLyrics {
    fun parse(bytes: ByteArray): List<Line> = runCatching {
        if(bytes.size<10 || String(bytes,0,3,Charsets.ISO_8859_1)!="ID3") return emptyList()
        val version=bytes[3].toInt()
        if(version !in 3..4 || bytes[5].toInt() and 0xC0 != 0) return emptyList()
        fun size(at:Int,sync:Boolean):Int {
            var n=0
            repeat(4){ val b=bytes[at+it].toInt() and 255; require(!sync || b<128); n=(n shl if(sync)7 else 8) or b }
            return n
        }
        val end=(10L+size(6,true)).coerceAtMost(bytes.size.toLong()).toInt()
        var at=10
        while(at+10<=end) {
            val id=String(bytes,at,4,Charsets.ISO_8859_1)
            val length=size(at+4,version==4)
            if(length<=0 || at+10L+length>end) break
            val data=bytes.copyOfRange(at+10,at+10+length)
            if(bytes[at+9].toInt()==0 && id in listOf("SYLT","USLT")) {
                val encoding=data[0].toInt()
                val charset=when(encoding){0->Charsets.ISO_8859_1;1->Charsets.UTF_16;2->Charsets.UTF_16BE;3->Charsets.UTF_8;else->throw IllegalArgumentException()}
                val width=if(encoding in 1..2)2 else 1
                fun terminated(start:Int):Pair<String,Int> {
                    var stop=start
                    while(stop+width<=data.size && !(data[stop].toInt()==0 && (width==1 || data[stop+1].toInt()==0))) stop+=width
                    require(stop+width<=data.size)
                    return String(data,start,stop-start,charset) to (stop+width)
                }
                if(id=="USLT" && data.size>4) {
                    val start=terminated(4).second
                    val parsed=Lrc.parse(String(data,start,data.size-start,charset))
                    if(parsed.isNotEmpty()) return parsed
                }
                if(id=="SYLT" && data.size>6 && data[4].toInt()==2 && data[5].toInt()==1) {
                    var cursor=terminated(6).second
                    val lines=mutableListOf<Line>()
                    while(cursor<data.size) {
                        val (text,next)=terminated(cursor); cursor=next
                        require(cursor+4<=data.size)
                        var time=0L
                        repeat(4){time=(time shl 8) or (data[cursor++].toLong() and 255)}
                        lines.add(Line(time,text))
                    }
                    if(lines.isNotEmpty()) return lines.sortedBy{it.timeMs}
                }
            }
            at+=10+length
        }
        emptyList<Line>()
    }.getOrDefault(emptyList())
}
