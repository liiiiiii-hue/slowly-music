package com.slowly.music

import android.app.*
import android.content.*
import android.os.*
import android.media.session.MediaSession
import android.media.session.PlaybackState
import androidx.compose.runtime.*
import androidx.media3.common.*
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
object PlaybackEngine {
    var player by mutableStateOf<ExoPlayer?>(null);private set
    var current by mutableStateOf<RemoteFile?>(null);private set
    var queue:List<RemoteFile> = emptyList();private set
    var error by mutableStateOf<String?>(null);private set
    var connection:Dav?=null
    private var account=""
    private val handler=Handler(Looper.getMainLooper())
    private var retry:Runnable?=null
    private var attempts=0
    fun mode(value:Int){player?.let{it.repeatMode=when(value){2->Player.REPEAT_MODE_ONE;1->Player.REPEAT_MODE_ALL;else->Player.REPEAT_MODE_OFF};it.shuffleModeEnabled=value==1}}
    fun select(context:Context,file:RemoteFile,tracks:List<RemoteFile>,authorization:String,scope:String,mode:Int):ExoPlayer {
        context.startForegroundService(Intent(context,PlaybackService::class.java))
        retry?.let{handler.removeCallbacks(it)};attempts=0;error=null
        if(player==null||account!=scope){
            release();account=scope
            val app=context.applicationContext
            val http=DefaultHttpDataSource.Factory().setConnectTimeoutMs(15000).setReadTimeoutMs(20000).setDefaultRequestProperties(mapOf("Authorization" to authorization))
            val source=DefaultMediaSourceFactory(CacheDataSource.Factory().setCache(MusicCache.get(app)).setUpstreamDataSourceFactory(androidx.media3.datasource.DefaultDataSource.Factory(app,http)).setCacheKeyFactory{spec->scope+"|"+spec.uri})
            val p=ExoPlayer.Builder(app).setMediaSourceFactory(source).setLoadControl(DefaultLoadControl.Builder().setBufferDurationsMs(30000,120000,5000,10000).setPrioritizeTimeOverSizeThresholds(true).build()).build()
            player=p
            p.setAudioAttributes(AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(),true)
            p.setWakeMode(C.WAKE_MODE_NETWORK)
            p.addListener(object:Player.Listener{
                override fun onMediaItemTransition(item:MediaItem?,reason:Int){retry?.let{handler.removeCallbacks(it)};attempts=0;error=null;current=queue.firstOrNull{it.url==item?.mediaId}}
                override fun onPlayerError(exception:PlaybackException){
                    val httpError=generateSequence<Throwable>(exception){it.cause}.filterIsInstance<HttpDataSource.InvalidResponseCodeException>().firstOrNull()?.responseCode
                    val retryable=if(httpError!=null)httpError==429||httpError>=500 else exception.errorCode in 2000..2008
                    android.util.Log.w("SlowlyPlayback","loadError code=${exception.errorCode} http=$httpError retry=$attempts")
                    if(retryable&&attempts<3&&p.playWhenReady){
                        attempts++;error="网络暂时不可用，正在自动重试（$attempts/3）"
                        val expected=p.currentMediaItem?.mediaId
                        retry=Runnable{if(player===p&&p.currentMediaItem?.mediaId==expected&&p.playWhenReady){p.prepare();p.play()}}
                        handler.postDelayed(retry!!,1000L shl attempts)
                    }else error=when(httpError){401,403->"网盘拒绝访问，请检查账号权限或应用密码。";404->"云盘中找不到这首歌曲，请重新扫描。";429->"网盘请求频繁，请稍后重试。";else->"歌曲加载失败，自动重试未恢复，请检查网络后重试。"}
                }
                override fun onPlaybackStateChanged(state:Int){if(state==Player.STATE_READY)error=null}
                override fun onPlayWhenReadyChanged(ready:Boolean,reason:Int){if(!ready){retry?.let{handler.removeCallbacks(it)}}}
            })
        }
        val p=player!!
        val list=tracks.ifEmpty{listOf(file)}
        if(queue.map{it.url}!=list.map{it.url}){
            queue=list
            p.setMediaItems(list.map{track->MediaItem.Builder().setMediaId(track.url).setUri(track.url).setMediaMetadata(MediaMetadata.Builder().setTitle(trackTitle(track)).setArtist(track.artist).setAlbumTitle(track.album).build()).build()},list.indexOfFirst{it.url==file.url}.coerceAtLeast(0),0)
        }else p.seekTo(list.indexOfFirst{it.url==file.url}.coerceAtLeast(0),0)
        current=file;this.mode(mode);p.prepare();p.play()
        return p
    }
    fun release(){retry?.let{handler.removeCallbacks(it)};retry=null;player?.release();player=null;current=null;queue=emptyList();error=null;connection=null}
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class PlaybackService:Service(){
    private lateinit var session:MediaSession
    private var attached:ExoPlayer?=null
    private val listener=object:Player.Listener{override fun onEvents(player:Player,events:Player.Events){publish()}}
    override fun onCreate(){
        super.onCreate()
        getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel("playback","音乐播放",NotificationManager.IMPORTANCE_LOW))
        session=MediaSession(this,"Slowly Music")
        session.setSessionActivity(PendingIntent.getActivity(this,0,Intent(this,MainActivity::class.java),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
        session.setCallback(object:MediaSession.Callback(){
            override fun onPlay(){PlaybackEngine.player?.let{if(it.playerError!=null)it.prepare();it.play()}}
            override fun onPause(){PlaybackEngine.player?.pause()}
            override fun onSkipToNext(){next(true)}
            override fun onSkipToPrevious(){next(false)}
            override fun onSeekTo(pos:Long){PlaybackEngine.player?.seekTo(pos)}
            override fun onStop(){PlaybackEngine.player?.stop();stopSelf()}
        })
        session.isActive=true
        startForeground(101,notification())
    }
    private fun next(forward:Boolean){PlaybackEngine.player?.let{p->if(forward){if(p.hasNextMediaItem())p.seekToNextMediaItem()else if(p.mediaItemCount>0)p.seekTo(0,0)}else{if(p.hasPreviousMediaItem())p.seekToPreviousMediaItem()else p.seekTo(0)};p.play()}}
    override fun onStartCommand(intent:Intent?,flags:Int,startId:Int):Int{
        val p=PlaybackEngine.player
        if(p==null){stopSelf();return START_NOT_STICKY}
        if(attached!==p){attached?.removeListener(listener);attached=p;p.addListener(listener)}
        when(intent?.action){"pause"->p.pause();"play"->{if(p.playerError!=null)p.prepare();p.play()};"next"->next(true);"previous"->next(false);"stop"->{p.stop();stopSelf()}}
        publish();return START_NOT_STICKY
    }
    private fun action(name:String)=PendingIntent.getService(this,name.hashCode(),Intent(this,PlaybackService::class.java).setAction(name),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    private fun notification():Notification{
        val p=PlaybackEngine.player;val track=PlaybackEngine.current
        val n=Notification.Builder(this,"playback").setSmallIcon(R.drawable.ic_music).setContentTitle(track?.let{trackTitle(it)}?:"Slowly Music").setContentText(track?.artist?:"正在准备播放")
            .setContentIntent(session.controller.sessionActivity).setVisibility(Notification.VISIBILITY_PUBLIC).setOnlyAlertOnce(true).setOngoing(p?.playWhenReady==true)
            .addAction(Notification.Action.Builder(R.drawable.ic_prev,"上一首",action("previous")).build())
            .addAction(Notification.Action.Builder(if(p?.isPlaying==true)R.drawable.ic_pause else R.drawable.ic_play,if(p?.isPlaying==true)"暂停"else"播放",action(if(p?.isPlaying==true)"pause"else"play")).build())
            .addAction(Notification.Action.Builder(R.drawable.ic_next,"下一首",action("next")).build())
            .setStyle(Notification.MediaStyle().setMediaSession(session.sessionToken).setShowActionsInCompactView(0,1,2))
        return n.build()
    }
    private fun publish(){
        val p=PlaybackEngine.player?:return
        session.setMetadata(android.media.MediaMetadata.Builder().putString(android.media.MediaMetadata.METADATA_KEY_TITLE,p.mediaMetadata.title?.toString()).putString(android.media.MediaMetadata.METADATA_KEY_ARTIST,p.mediaMetadata.artist?.toString()).putLong(android.media.MediaMetadata.METADATA_KEY_DURATION,p.duration.coerceAtLeast(0)).build())
        val state=when{p.playerError!=null->PlaybackState.STATE_ERROR;p.playbackState==Player.STATE_BUFFERING->PlaybackState.STATE_BUFFERING;p.isPlaying->PlaybackState.STATE_PLAYING;else->PlaybackState.STATE_PAUSED}
        session.setPlaybackState(PlaybackState.Builder().setActions(PlaybackState.ACTION_PLAY or PlaybackState.ACTION_PAUSE or PlaybackState.ACTION_SKIP_TO_NEXT or PlaybackState.ACTION_SKIP_TO_PREVIOUS or PlaybackState.ACTION_SEEK_TO or PlaybackState.ACTION_STOP).setState(state,p.currentPosition,if(p.isPlaying)1f else 0f).build())
        getSystemService(NotificationManager::class.java).notify(101,notification())
    }
    override fun onTaskRemoved(rootIntent:Intent?){if(PlaybackEngine.player?.playWhenReady!=true)stopSelf()}
    override fun onDestroy(){attached?.removeListener(listener);if(PlaybackEngine.player===attached)PlaybackEngine.release();session.release();super.onDestroy()}
    override fun onBind(intent:Intent?) :IBinder?=null
}
