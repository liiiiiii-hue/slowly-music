package com.slowly.music

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.database.StandaloneDatabaseProvider

@RunWith(AndroidJUnit4::class)
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class LibraryPersistenceTest {
    private val context=InstrumentationRegistry.getInstrumentation().targetContext
    @Test fun songMayBelongToMultiplePersistentLists() {
        val account="test-${java.util.UUID.randomUUID()}"
        val store=LibraryStore(context,account)
        val song=RemoteFile("https://example.com/Music/test.mp3","test.mp3",false,title="Tag title",artist="Tag artist",album="Tag album",artwork="/private/test-cover.jpg",tagsRead=true)
        store.saveSongs(listOf(song))
        val a=MusicList(name="A",songs=listOf(song.url))
        val b=MusicList(name="B",songs=listOf(song.url))
        store.saveLists(listOf(a,b))
        val reopened=LibraryStore(context,account)
        assertEquals(listOf(song),reopened.songs())
        assertEquals(2,reopened.lists().count{song.url in it.songs})
        reopened.saveLists(listOf(a.copy(songs=emptyList()),b))
        assertEquals(listOf(song.url),LibraryStore(context,account).lists()[1].songs)
    }
    @Test fun audioCacheEvictsOldestWithoutRemovingLibrary() {
        val directory=java.io.File(context.cacheDir,"cache-test-${java.util.UUID.randomUUID()}")
        val db=StandaloneDatabaseProvider(context)
        val cache=SimpleCache(directory,LeastRecentlyUsedCacheEvictor(32),db)
        try {
            fun write(key:String){val hole=cache.startReadWrite(key,0,20);try{val file=cache.startFile(key,0,20);file.writeBytes(ByteArray(20));cache.commitFile(file,20)}finally{cache.releaseHoleSpan(hole)}}
            write("old");write("new")
            assertTrue(cache.cacheSpace<=32)
            assertTrue(cache.getCachedSpans("old").isEmpty())
            assertFalse(cache.getCachedSpans("new").isEmpty())
            assertEquals(1073741824L,MusicCache.LIMIT)
        } finally {cache.release();db.close();directory.deleteRecursively()}
    }
}

