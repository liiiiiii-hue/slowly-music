package com.slowly.music

import android.content.Intent
import android.os.SystemClock
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

@RunWith(AndroidJUnit4::class)
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class BackgroundPlaybackTest {
    @Test fun closesActivityAndStillAdvancesPlaylist(){
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        val app=instrumentation.targetContext
        fun wave(name:String):File{
            val pcm=ByteArray(8000*2*2)
            val data=ByteBuffer.allocate(44+pcm.size).order(ByteOrder.LITTLE_ENDIAN)
            data.put("RIFF".toByteArray()).putInt(36+pcm.size).put("WAVEfmt ".toByteArray()).putInt(16).putShort(1).putShort(1).putInt(8000).putInt(16000).putShort(2).putShort(16).put("data".toByteArray()).putInt(pcm.size).put(pcm)
            return File(app.cacheDir,name).apply{writeBytes(data.array())}
        }
        val a=wave("background-first.wav");val b=wave("background-second.wav")
        val tracks=listOf(a,b).map{RemoteFile(it.toURI().toString(),it.name,false,title=it.name)}
        val scenario=ActivityScenario.launch(MainActivity::class.java)
        var original:androidx.media3.exoplayer.ExoPlayer?=null
        try{
            scenario.onActivity{original=PlaybackEngine.select(it,tracks[0],tracks,"","background-test",0)}
            scenario.recreate()
            scenario.onActivity{assertSame(original,PlaybackEngine.player)}
            scenario.close()
            val deadline=SystemClock.elapsedRealtime()+15000
            var advanced=false
            while(SystemClock.elapsedRealtime()<deadline){
                instrumentation.runOnMainSync{advanced=PlaybackEngine.current?.url==tracks[1].url&&PlaybackEngine.player?.playerError==null}
                if(advanced)break
                SystemClock.sleep(200)
            }
            assertTrue("Playlist must advance after Activity is destroyed",advanced)
            instrumentation.runOnMainSync{assertSame(original,PlaybackEngine.player);assertNull(PlaybackEngine.error)}
        }finally{
            scenario.close()
            instrumentation.runOnMainSync{app.stopService(Intent(app,PlaybackService::class.java));PlaybackEngine.release()}
            a.delete();b.delete()
        }
    }

    @Test fun transientIoFailureRecoversWithoutReopeningActivity(){
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        val app=instrumentation.targetContext
        val missing=File(app.cacheDir,"retry-${System.nanoTime()}.wav")
        val track=RemoteFile(missing.toURI().toString(),missing.name,false,title="Synthetic retry fixture")
        val scenario=ActivityScenario.launch(MainActivity::class.java)
        try{
            scenario.onActivity{PlaybackEngine.select(it,track,listOf(track),"","retry-test",0)}
            var failed=false
            var deadline=SystemClock.elapsedRealtime()+10000
            while(SystemClock.elapsedRealtime()<deadline){
                instrumentation.runOnMainSync{failed=PlaybackEngine.error?.contains("正在自动重试")==true}
                if(failed)break
                SystemClock.sleep(100)
            }
            assertTrue("Transient read failure must schedule a retry",failed)
            scenario.close()
            val pcm=ByteArray(8000*2*5)
            val data=ByteBuffer.allocate(44+pcm.size).order(ByteOrder.LITTLE_ENDIAN)
            data.put("RIFF".toByteArray()).putInt(36+pcm.size).put("WAVEfmt ".toByteArray()).putInt(16).putShort(1).putShort(1).putInt(8000).putInt(16000).putShort(2).putShort(16).put("data".toByteArray()).putInt(pcm.size).put(pcm)
            missing.writeBytes(data.array())
            var recovered=false
            deadline=SystemClock.elapsedRealtime()+15000
            while(SystemClock.elapsedRealtime()<deadline){
                instrumentation.runOnMainSync{recovered=PlaybackEngine.player?.isPlaying==true&&PlaybackEngine.error==null}
                if(recovered)break
                SystemClock.sleep(100)
            }
            assertTrue("Must recover by itself while Activity is closed",recovered)
        }finally{
            scenario.close()
            instrumentation.runOnMainSync{app.stopService(Intent(app,PlaybackService::class.java));PlaybackEngine.release()}
            instrumentation.waitForIdleSync()
            missing.delete()
        }
    }
}
