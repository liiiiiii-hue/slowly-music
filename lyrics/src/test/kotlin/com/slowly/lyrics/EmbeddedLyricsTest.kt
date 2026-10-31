package com.slowly.lyrics

import kotlin.test.*

class EmbeddedLyricsTest {
    @Test fun readsMillisecondSylt() {
        val payload=byteArrayOf(3,101,110,103,2,1,0)+"hello".toByteArray()+byteArrayOf(0,0,0,4,(-24).toByte())
        val frame="SYLT".toByteArray()+byteArrayOf(0,0,0,payload.size.toByte(),0,0)+payload
        val tag="ID3".toByteArray()+byteArrayOf(3,0,0,0,0,0,frame.size.toByte())+frame
        assertEquals(listOf(Line(1256,"hello")),EmbeddedLyrics.parse(tag))
    }
    @Test fun rejectsDamagedTag() {
        assertTrue(EmbeddedLyrics.parse(byteArrayOf(1,2,3)).isEmpty())
        assertTrue(EmbeddedLyrics.parse("ID3broken".toByteArray()).isEmpty())
    }
}
