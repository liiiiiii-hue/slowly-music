package com.slowly.lyrics

/** ID3 values only. File names are deliberately not consulted. */
data class TrackTag(val title:String="",val artist:String="",val album:String="",val picture:ByteArray?=null)
object TrackTags {
    fun parse(bytes:ByteArray):TrackTag {
        if(bytes.size<10||String(bytes,0,3,Charsets.ISO_8859_1)!="ID3"){
            val at=bytes.size-128
            if(at>=0&&String(bytes,at,3,Charsets.ISO_8859_1)=="TAG"){
                fun field(offset:Int)=String(bytes,at+offset,30,Charsets.ISO_8859_1).trim('\u0000',' ')
                return TrackTag(field(3),field(33),field(63))
            }
            return TrackTag()
        }
        val version=bytes[3].toInt() and 255
        if(version !in 2..4)return TrackTag()
        fun number(data:ByteArray,at:Int,length:Int,sync:Boolean=false):Int {var result=0L;repeat(length){val b=data[at+it].toInt() and 255;require(!sync||b<128);result=(result shl if(sync)7 else 8) or b.toLong()};require(result<=Int.MAX_VALUE);return result.toInt()}
        return runCatching {
            val length=number(bytes,6,4,true).coerceAtMost(bytes.size-10)
            var data=bytes.copyOfRange(10,10+length)
            fun deunsync(source:ByteArray):ByteArray {val out=java.io.ByteArrayOutputStream();var i=0;while(i<source.size){val b=source[i++];out.write(b.toInt());if(b.toInt() and 255==255&&i<source.size&&source[i].toInt()==0)i++};return out.toByteArray()}
            if(bytes[5].toInt() and 128!=0)data=deunsync(data)
            var at=if(version>=3&&bytes[5].toInt() and 64!=0){if(version==3)number(data,0,4)+4 else number(data,0,4,true)}else 0
            var title="";var artist="";var album="";var picture:ByteArray?=null;var pictureType=-1
            fun charset(encoding:Int)=when(encoding){0->Charsets.ISO_8859_1;1->Charsets.UTF_16;2->Charsets.UTF_16BE;3->Charsets.UTF_8;else->throw IllegalArgumentException()}
            fun text(frame:ByteArray):String {if(frame.isEmpty())return "";return String(frame,1,frame.size-1,charset(frame[0].toInt())).trim('\u0000',' ','\n','\r').replace('\u0000',' ').trim()}
            val header=if(version==2)6 else 10
            while(at+header<=data.size){
                val id=String(data,at,if(version==2)3 else 4,Charsets.ISO_8859_1)
                if(id.all{it=='\u0000'})break
                if(!id.all{it in 'A'..'Z'||it in '0'..'9'})break
                val count=number(data,at+if(version==2)3 else 4,if(version==2)3 else 4,version==4)
                if(count<=0||at.toLong()+header+count>data.size)break
                val flags=if(version==2)0 else data[at+9].toInt() and 255
                var frame=data.copyOfRange(at+header,at+header+count);at+=header+count
                if((version==3&&flags and 0xE0!=0)||(version==4&&flags and 0x4C!=0))continue
                if(version==4&&flags and 2!=0)frame=deunsync(frame)
                if(version==4&&flags and 1!=0){if(frame.size<4)continue;frame=frame.copyOfRange(4,frame.size)}
                when(id){
                    "TIT2","TT2"->title=text(frame)
                    "TPE1","TP1"->artist=text(frame)
                    "TALB","TAL"->album=text(frame)
                    "APIC","PIC"->runCatching {
                        val encoding=frame[0].toInt();val width=if(encoding in 1..2)2 else 1
                        var cursor=if(id=="PIC")4 else frame.indexOf(0.toByte(),1)+1
                        require(cursor>1&&cursor<frame.size)
                        val type=frame[cursor++].toInt() and 255
                        while(cursor+width<=frame.size&&!(frame[cursor].toInt()==0&&(width==1||frame[cursor+1].toInt()==0)))cursor+=width
                        cursor+=width
                        if(cursor<frame.size&&(picture==null||type==3&&pictureType!=3)){picture=frame.copyOfRange(cursor,frame.size);pictureType=type}
                    }
                }
            }
            TrackTag(title,artist,album,picture)
        }.getOrDefault(TrackTag())
    }
    private fun ByteArray.indexOf(value:Byte,from:Int):Int {for(i in from until size)if(this[i]==value)return i;return -1}
}
