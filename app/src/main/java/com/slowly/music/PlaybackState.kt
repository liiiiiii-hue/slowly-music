package com.slowly.music

import android.util.Log
import androidx.compose.runtime.*
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener

@Stable class PlaybackClock {
    var position by mutableLongStateOf(0L)
    var duration by mutableLongStateOf(0L)
}
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class PlaybackProbe:AnalyticsListener {
    private var ready=false
    private var wasBuffering=false
    var userSeek=false
    private var rebuffers=0
    private var underruns=0
    private var lastSample=0L
    override fun onPlaybackStateChanged(eventTime:AnalyticsListener.EventTime,state:Int){
        if(state==Player.STATE_BUFFERING&&!wasBuffering&&ready&&!userSeek)rebuffers++
        if(state==Player.STATE_READY){ready=true;userSeek=false}
        wasBuffering=state==Player.STATE_BUFFERING
        Log.i("SlowlyPlayback","state=$state rebuffers=$rebuffers underruns=$underruns")
    }
    override fun onAudioUnderrun(eventTime:AnalyticsListener.EventTime,bufferSize:Int,bufferSizeMs:Long,elapsedSinceLastFeedMs:Long){underruns++;Log.w("SlowlyPlayback","audioUnderrun count=$underruns elapsedMs=$elapsedSinceLastFeedMs")}
    fun sample(player:ExoPlayer){val now=android.os.SystemClock.elapsedRealtime();if(now-lastSample>=5000){lastSample=now;Log.i("SlowlyPlayback","positionMs=${player.currentPosition} bufferedMs=${player.totalBufferedDuration} playing=${player.isPlaying} rebuffers=$rebuffers underruns=$underruns")}}
}
