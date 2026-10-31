package com.slowly.music

import android.content.Intent
import android.provider.Settings
import android.media.MediaMetadata
import android.media.session.PlaybackState
import android.os.SystemClock
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable fun QqMusicScreen(back:()->Unit,beforePlay:()->Unit,onBackdrop:(androidx.compose.ui.graphics.ImageBitmap?)->Unit){
    val context=LocalContext.current
    val prefs=remember{context.getSharedPreferences("display",0)}
    var tick by remember{mutableIntStateOf(0)}
    var lyricMode by remember{mutableStateOf(true)}
    var blurred by remember{mutableStateOf(prefs.getBoolean("blurred",false))}
    var settings by remember{mutableStateOf(false)}
    var size by remember{mutableFloatStateOf(prefs.getFloat("size",18f))}
    var notice by remember{mutableStateOf<String?>(null)}
    val clock=remember{PlaybackClock()}
    LaunchedEffect(Unit){while(true){tick++;delay(200)}}
    val c=remember(tick){QqBridge.controller(context)}
    val m=c?.metadata;val s=c?.playbackState
    val title=m?.getString(MediaMetadata.METADATA_KEY_TITLE).orEmpty().ifBlank{"QQ 音乐"}
    val artist=m?.getString(MediaMetadata.METADATA_KEY_ARTIST).orEmpty()
    val album=m?.getString(MediaMetadata.METADATA_KEY_ALBUM).orEmpty()
    val bytes=m?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)?:m?.getBitmap(MediaMetadata.METADATA_KEY_ART)?:m?.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
    val revisions=remember{ArtworkRevision()}
    val artworkRevision=revisions.update(bytes)
    val cover=remember(artworkRevision){bytes?.asImageBitmap()}
    val soft by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null,artworkRevision){
        value=if(bytes==null)null else kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default){ArtworkTools.fromBitmap(bytes).background}
    }
    LaunchedEffect(soft,blurred){onBackdrop(if(blurred)soft else null)}
    DisposableEffect(Unit){onDispose{onBackdrop(null)}}
    val raw=runCatching{m?.getText("ucar.media.metadata.LYRICS_WHOLE")?.toString()}.getOrNull().orEmpty()
    val lines=remember(raw){com.slowly.lyrics.Lrc.parse(raw)}
    clock.duration=m?.getLong(MediaMetadata.METADATA_KEY_DURATION)?:0
    clock.position=((s?.position?:0)+if(s?.state==PlaybackState.STATE_PLAYING)((SystemClock.elapsedRealtime()-s.lastPositionUpdateTime)*s.playbackSpeed).toLong()else 0).coerceIn(0,clock.duration.coerceAtLeast(0))
    fun openQq(){beforePlay();val intent=context.packageManager.getLaunchIntentForPackage(QqBridge.PACKAGE);if(intent!=null)context.startActivity(intent)else notice="请先安装 QQ 音乐"}
    fun command(flag:Long,action:(android.media.session.MediaController.TransportControls)->Unit){
        val active=QqBridge.controller(context)
        notice=when{active==null->"请先开启播放控制权限，并在 QQ 中播放歌曲";((active.playbackState?.actions?:0) and flag)==0L->"QQ 暂未提供此操作";else->runCatching{action(active.transportControls);null}.getOrElse{"操作暂未完成，请重试"}}
    }
    androidx.activity.compose.BackHandler{if(settings)settings=false else back()}
    if(settings){LyricSettingsScreen(size,{size=it;prefs.edit().putFloat("size",it).apply()},blurred,{blurred=it;prefs.edit().putBoolean("blurred",it).apply()},cover,{settings=false});return}
    Box(Modifier.fillMaxSize()){
    Column(Modifier.fillMaxSize()){
        if(c==null)TextButton({context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))}){Text("连接 QQ 播放控制")}
        Box(Modifier.weight(1f)){
            PlayerScreen(RemoteFile("qq://current",title,false,title,artist,album),artist,album,cover,lines,lyricMode,size,clock,s?.state==PlaybackState.STATE_PLAYING,s?.state==PlaybackState.STATE_BUFFERING,notice,0,back,{lyricMode=!lyricMode},{settings=true},{openQq()},{position->command(PlaybackState.ACTION_SEEK_TO){it.seekTo(position)}},{if(s?.state==PlaybackState.STATE_PLAYING)command(PlaybackState.ACTION_PAUSE){it.pause()}else{beforePlay();command(PlaybackState.ACTION_PLAY){it.play()}}},{command(PlaybackState.ACTION_SKIP_TO_PREVIOUS){it.skipToPrevious()}},{command(PlaybackState.ACTION_SKIP_TO_NEXT){it.skipToNext()}},{},{},{openQq()},external=true,rawLyrics=raw,seekEnabled=((s?.actions?:0) and PlaybackState.ACTION_SEEK_TO)!=0L)
        }
    }
    }
}
