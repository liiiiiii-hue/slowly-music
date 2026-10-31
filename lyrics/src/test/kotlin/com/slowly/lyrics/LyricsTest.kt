package com.slowly.lyrics
import kotlin.test.*
class LyricsTest {
    @Test fun timestampsAndOffset() {
        val lines = Lrc.parse("\uFEFF[offset:500]\r\n[00:02.5][00:03.00]hello\r\n[00:01]first\n[00:99]bad")
        assertEquals(listOf(500L,2000L,2500L), lines.map { it.timeMs })
        assertEquals(-1,Lrc.current(lines,0))
    }
    @Test fun strictMatchAndConflict() {
        assertEquals(Match.Missing, matchLyrics("Song A.mp3",listOf("SongA.lrc")))
        assertIs<Match.Conflict>(matchLyrics("Song A.mp3",listOf("a/song a.LRC","b/Song A.lrc")))
        assertEquals(Match.Selected("b.lrc"),matchLyrics("x.mp3", listOf("b.lrc"),"b.lrc"))
    }
    @Test fun enhancedAndInvalid() {
        assertEquals(listOf(1000L,1500L), Lrc.parse("[00:01]<00:01>one<00:01.5>two").single().words.map { it.timeMs })
        assertTrue(Lrc.parse("plain text").isEmpty())
    }
}
