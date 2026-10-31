package com.slowly.music

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slowly.lyrics.Line
import com.slowly.lyrics.Lrc

fun clockText(value:Long)="${value.coerceAtLeast(0)/60000}:${(value.coerceAtLeast(0)/1000%60).toString().padStart(2,'0')}"
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun PlayerScreen(track:RemoteFile,artist:String,album:String,artwork:ImageBitmap?,lines:List<Line>,lyricMode:Boolean,size:Float,clock:PlaybackClock,playing:Boolean,loading:Boolean,error:String?,mode:Int,onCollapse:()->Unit,onView:()->Unit,onSettings:()->Unit,onAdd:()->Unit,onSeek:(Long)->Unit,onToggle:()->Unit,onPrevious:()->Unit,onNext:()->Unit,onMode:()->Unit,onQueue:()->Unit,onRetry:()->Unit,external:Boolean=false,rawLyrics:String="",seekEnabled:Boolean=true){
    val colors=MaterialTheme.colorScheme
    Column(Modifier.fillMaxSize()){
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){UiAction("down","收起播放器",onCollapse);Spacer(Modifier.weight(1f));UiAction(if(lyricMode)"album"else"lyrics","切换封面歌词",onView);UiAction("settings","歌词设置",onSettings)}
        Row(Modifier.fillMaxWidth().padding(top=8.dp),verticalAlignment=Alignment.CenterVertically){Text(trackTitle(track),Modifier.weight(1f),fontSize=26.sp,lineHeight=34.sp,fontWeight=FontWeight.Normal);UiAction(if(external)"music"else"add",if(external)"打开 QQ 音乐"else"加入歌单",onAdd)}
        Text(listOf(artist.ifEmpty{"未知歌手"},album.ifEmpty{"未知专辑"}).joinToString(" · "),fontSize=12.sp,lineHeight=20.sp,color=colors.onSurfaceVariant,modifier=Modifier.padding(top=6.dp,bottom=10.dp))
        if(error!=null)CardSurface{Column(Modifier.padding(12.dp)){Text(error,fontSize=13.sp,color=colors.onSurface);TextButton(onRetry){Text("重试播放")}}}
        if(loading)LinearProgressIndicator(Modifier.fillMaxWidth().height(2.dp))
        if(lyricMode&&lines.isEmpty()&&rawLyrics.isNotBlank())Box(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())){Text(rawLyrics,Modifier.padding(vertical=20.dp),fontSize=size.sp,lineHeight=(size+10).sp)}else if(lyricMode)LyricsClockViewport(lines,clock,size,onSeek,Modifier.weight(1f))else BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()){
            val coverSize=minOf(maxWidth,310.dp,maxHeight*.72f)
            Column(Modifier.fillMaxSize(),verticalArrangement=Arrangement.SpaceEvenly,horizontalAlignment=Alignment.CenterHorizontally){
                Box(Modifier.size(coverSize).clip(RoundedCornerShape(20.dp)).background(Brush.linearGradient(listOf(colors.secondaryContainer,colors.primary.copy(alpha=.65f)))),contentAlignment=Alignment.Center){if(artwork!=null)Image(artwork,null,Modifier.fillMaxSize(),contentScale=ContentScale.Crop)else UiIcon("music",null,Modifier.size(68.dp),Color.White)}
                AlbumLyric(lines,clock)
            }
        }
        PlaybackProgress(clock,onSeek,seekEnabled)
        Row(Modifier.fillMaxWidth().padding(top=10.dp,bottom=18.dp),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){if(external)Box(Modifier.size(48.dp))else UiAction(when(mode){1->"shuffle";2->"repeatOne";else->"mode"},"播放模式："+listOf("顺序播放","随机播放","单曲循环")[mode],onMode);UiAction("prev","上一首",onPrevious);FilledIconButton(onToggle,Modifier.size(54.dp),shape=CircleShape){UiIcon(if(playing)"pause"else"play",if(playing)"暂停"else"播放",Modifier.size(24.dp),Color.White)};UiAction("next","下一首",onNext);if(external)Box(Modifier.size(48.dp))else UiAction("queue","播放队列",onQueue)}
    }
}
@Composable fun LyricsViewport(lines:List<Line>,position:Long,size:Float,onSeek:(Long)->Unit,modifier:Modifier=Modifier){
    val current=Lrc.current(lines,position).coerceAtLeast(0)
    var follow by remember{mutableStateOf(true)}
    val state=rememberLazyListState()
    LaunchedEffect(lines){follow=true}
    LaunchedEffect(state){state.interactionSource.interactions.collect{if(it is DragInteraction.Start)follow=false}}
    LaunchedEffect(current,follow,lines){if(follow&&lines.isNotEmpty())state.animateScrollToItem(current.coerceAtMost(lines.lastIndex))}
    BoxWithConstraints(modifier.fillMaxWidth()){
        if(lines.isEmpty())Text("暂无同步歌词",Modifier.align(Alignment.Center),fontSize=14.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
        else{
            val rowHeight=(maxHeight/11).coerceAtLeast(40.dp)
            LazyColumn(state=state,modifier=Modifier.fillMaxSize(),contentPadding=PaddingValues(vertical=((maxHeight-rowHeight)/2).coerceAtLeast(0.dp))){itemsIndexed(lines){i,line->
                val distance=kotlin.math.abs(i-current).coerceAtMost(3)
                val fade=listOf(1f,.85f,.65f,.4f)[distance]
                val colors=MaterialTheme.colorScheme
                Text(buildAnnotatedString{if(i==current&&line.words.isNotEmpty())line.words.forEach{word->withStyle(SpanStyle(color=if(position>=word.timeMs)colors.onSurface else colors.onSurfaceVariant.copy(alpha=.65f))){append(word.text)}}else append(line.text)},fontSize=(size-listOf(0,2,3,4)[distance]).sp,lineHeight=(size+10).sp,fontWeight=if(distance==0)FontWeight.Medium else FontWeight.Normal,color=if(distance==0)colors.onSurface else colors.onSurfaceVariant.copy(alpha=fade),textAlign=TextAlign.Center,modifier=Modifier.fillMaxWidth().height(rowHeight).wrapContentHeight(Alignment.CenterVertically).clickable{onSeek(line.timeMs);follow=true}.padding(vertical=10.dp))
            }}
            if(!follow)TextButton({follow=true},Modifier.align(Alignment.BottomCenter)){Text("回到当前歌词",fontSize=12.sp)}
        }
    }
}
@Composable fun QueueScreen(queue:List<RemoteFile>,current:RemoteFile?,playing:Boolean,mode:Int,onBack:()->Unit,onPlay:(RemoteFile)->Unit,onToggle:()->Unit){Column(Modifier.fillMaxSize()){PageHeader("播放队列",onBack);Text("${listOf("顺序播放","随机播放","单曲循环")[mode]} · ${queue.size} 首",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.padding(bottom=8.dp));LocatedTracks(queue,current?.url,Modifier.weight(1f)){track->val isCurrent=track.url==current?.url;SongCard(track,if(isCurrent){if(playing)"正在播放"else"已暂停"}else"待播放",{onPlay(track)},if(isCurrent&&playing)"pause"else"play","播放或暂停 ${track.name}",current=isCurrent,playing=playing,onAction={if(isCurrent)onToggle()else onPlay(track)})}}}
@Composable fun LyricSettingsScreen(size:Float,onSize:(Float)->Unit,blurred:Boolean,onBackground:(Boolean)->Unit,artwork:ImageBitmap?,onBack:()->Unit){
    Column(Modifier.fillMaxSize()){
        PageHeader("歌词设置",onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())){
            Text("字号：${size.toInt()}sp",fontSize=12.sp,color=MaterialTheme.colorScheme.primary)
            GentleSlider(size,onSize,12f..24f,Modifier.padding(vertical=10.dp))
            Text("时间在此刻，慢了下来",fontSize=size.sp,textAlign=TextAlign.Center,modifier=Modifier.fillMaxWidth().padding(vertical=12.dp))
            HorizontalDivider(Modifier.padding(vertical=18.dp),color=MaterialTheme.colorScheme.outline)
            Text("跟随系统字体；逐字 / 逐行根据文件自动识别",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
            Text("播放背景",fontSize=20.sp,fontWeight=FontWeight.Medium,modifier=Modifier.padding(top=22.dp,bottom=14.dp))
            Box(Modifier.fillMaxWidth().height(110.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.primaryContainer),contentAlignment=Alignment.Center){
                if(blurred&&artwork!=null)Image(artwork,null,Modifier.fillMaxSize().alpha(.45f),contentScale=ContentScale.Crop)
                Text(if(blurred&&artwork==null)"当前歌曲暂无专辑封面"else"歌词显示效果预览",fontSize=14.sp)
            }
            Spacer(Modifier.height(12.dp))
            listOf(false to "浅色渐变",true to "专辑模糊").forEach{(value,label)->Surface(onClick={onBackground(value)},shape=RoundedCornerShape(14.dp),border=BorderStroke(1.dp,if(blurred==value)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),color=MaterialTheme.colorScheme.primaryContainer.copy(alpha=if(blurred==value)1f else .48f),modifier=Modifier.fillMaxWidth().padding(vertical=5.dp)){Row(Modifier.padding(horizontal=17.dp).heightIn(min=52.dp),verticalAlignment=Alignment.CenterVertically){Text(label,Modifier.weight(1f),fontSize=14.sp);if(blurred==value)Text("✓",color=MaterialTheme.colorScheme.primary)}}}
        }
        SoftOutlinedButton(onBack,Modifier.fillMaxWidth().padding(top=12.dp,bottom=18.dp)){Text("完成")}
    }
}

@Composable private fun LyricsClockViewport(lines:List<Line>,clock:PlaybackClock,size:Float,onSeek:(Long)->Unit,modifier:Modifier){LyricsViewport(lines,clock.position,size,onSeek,modifier)}
@Composable private fun PlaybackProgress(clock:PlaybackClock,onSeek:(Long)->Unit,enabled:Boolean=true){
    val duration=clock.duration
    var draft by remember(clock,duration){mutableStateOf<Float?>(null)}
    var committed by remember(clock,duration){mutableStateOf(false)}
    LaunchedEffect(committed){if(committed){kotlinx.coroutines.delay(900);draft=null;committed=false}}
    val position=draft?.toLong()?:clock.position
    GentleSlider(position.toFloat().coerceIn(0f,duration.coerceAtLeast(1).toFloat()),{draft=it;committed=false},0f..duration.coerceAtLeast(1).toFloat(),enabled=enabled&&duration>0,onFinished={draft?.let{onSeek(it.toLong());committed=true}})
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(clockText(position),fontSize=10.sp,color=MaterialTheme.colorScheme.onSurfaceVariant);Text(clockText(duration),fontSize=10.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}
}

@Composable private fun AlbumLyric(lines:List<Line>,clock:PlaybackClock){val i=Lrc.current(lines,clock.position);Text(if(lines.isEmpty())"暂无同步歌词"else lines.getOrNull(i.coerceAtLeast(0))?.text.orEmpty(),textAlign=TextAlign.Center,fontSize=16.sp,lineHeight=28.sp,color=MaterialTheme.colorScheme.onSurface,modifier=Modifier.fillMaxWidth().padding(horizontal=8.dp,vertical=12.dp))}
