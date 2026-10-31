package com.slowly.lyrics

import kotlin.test.*
class TrackTagsTest {
    private fun sync(n:Int)=byteArrayOf(((n shr 21)and 127).toByte(),((n shr 14)and 127).toByte(),((n shr 7)and 127).toByte(),(n and 127).toByte())
    private fun frame(id:String,data:ByteArray,v:Int=3):ByteArray=id.toByteArray()+ (if(v==4)sync(data.size)else java.nio.ByteBuffer.allocate(4).putInt(data.size).array())+byteArrayOf(0,0)+data
    private fun tag(frames:ByteArray,v:Int=3)="ID3".toByteArray()+byteArrayOf(v.toByte(),0,0)+sync(frames.size)+frames
    @Test fun readsUtf8TagsRatherThanInferringAFileName(){val parsed=TrackTags.parse(tag(frame("TIT2",byteArrayOf(3)+"真正的歌名".toByteArray())+frame("TPE1",byteArrayOf(3)+"演唱者".toByteArray())+frame("TALB",byteArrayOf(3)+"专辑".toByteArray())));assertEquals("真正的歌名",parsed.title);assertEquals("演唱者",parsed.artist);assertEquals("专辑",parsed.album)}
    @Test fun readsVersionFourUtf16(){val parsed=TrackTags.parse(tag(frame("TIT2",byteArrayOf(1)+"歌曲".toByteArray(Charsets.UTF_16),4),4));assertEquals("歌曲",parsed.title)}
    @Test fun prefersFrontCover(){val back=byteArrayOf(3)+"image/jpeg".toByteArray()+byteArrayOf(0,4,0,1,2);val front=byteArrayOf(3)+"image/png".toByteArray()+byteArrayOf(0,3,0,9,8,7);val parsed=TrackTags.parse(tag(frame("APIC",back)+frame("APIC",front)));assertContentEquals(byteArrayOf(9,8,7),parsed.picture)}
    @Test fun malformedAndUntaggedInputsRemainUnknown(){assertEquals("",TrackTags.parse(ByteArray(100)).title);assertEquals("",TrackTags.parse(tag(frame("TIT2",byteArrayOf(3,65))).copyOf(15)).artist)}
    @Test fun readsId3VersionOneWithoutGuessingFileNames(){val data=ByteArray(128);"TAG".toByteArray().copyInto(data);"Title from tag".toByteArray().copyInto(data,3);"Artist from tag".toByteArray().copyInto(data,33);val parsed=TrackTags.parse(data);assertEquals("Title from tag",parsed.title);assertEquals("Artist from tag",parsed.artist)}
}
