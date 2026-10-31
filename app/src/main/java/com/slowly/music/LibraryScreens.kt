package com.slowly.music

import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class GentleTheme(val name:String,val start:Color,val background:Color,val surface:Color,val accent:Color,val ink:Color,val muted:Color,val line:Color,val soft:Color,val coverLight:Color,val coverDark:Color)
val gentleThemes=listOf(
    GentleTheme("奶油杏",Color(0xFFFAF7EF),Color(0xFFE6D5B0),Color(0xFFFFFDF6),Color(0xFF806D45),Color(0xFF403A2D),Color(0xFF756B59),Color(0xFFDED5C3),Color(0xFFEEE4CE),Color(0xFFDDD0AE),Color(0xFFA38C60)),
    GentleTheme("樱花粉",Color(0xFFFFF4F4),Color(0xFFEDBFCA),Color(0xFFFFF9FA),Color(0xFF975B70),Color(0xFF49333D),Color(0xFF806570),Color(0xFFE4CBD5),Color(0xFFF2DCE5),Color(0xFFE8C6D3),Color(0xFFAD7C91)),
    GentleTheme("鼠尾草绿",Color(0xFFF4F8EE),Color(0xFFB6C9A0),Color(0xFFFBFDF7),Color(0xFF587447),Color(0xFF303D2C),Color(0xFF68775E),Color(0xFFD2DDC7),Color(0xFFE1EBD7),Color(0xFFCEDBBB),Color(0xFF869D72)),
    GentleTheme("雾霭蓝",Color(0xFFF2F7FC),Color(0xFFADC8DF),Color(0xFFF9FCFF),Color(0xFF4D7191),Color(0xFF2D3B49),Color(0xFF61758A),Color(0xFFCBD9E6),Color(0xFFDCE8F3),Color(0xFFC5D9E8),Color(0xFF7897B1)),
    GentleTheme("薰衣草紫",Color(0xFFF8F3FC),Color(0xFFC9B4DF),Color(0xFFFDFAFF),Color(0xFF7B5B99),Color(0xFF40334D),Color(0xFF776582),Color(0xFFDBCEE6),Color(0xFFEADEF2),Color(0xFFDDCDEA),Color(0xFFA28AB8))
)
@Composable fun ActionIcon(icon:ImageVector,label:String,action:()->Unit){IconButton(action,Modifier.size(48.dp)){Icon(icon,label,Modifier.size(21.dp))}}
@Composable fun MusicRow(file:RemoteFile,onPlay:()->Unit,onAdd:()->Unit,current:Boolean=false,playing:Boolean=false,onToggle:()->Unit={}){SongCard(file,trackArtist(file),onPlay,"add","加入歌单",current,playing,onCurrentToggle=onToggle,onAction=onAdd)}
@Composable fun SongCard(file:RemoteFile,subtitle:String,onPlay:()->Unit,action:String,actionLabel:String,current:Boolean=false,playing:Boolean=false,onCurrentToggle:()->Unit={},onAction:()->Unit){
    CardSurface(selected=current){Row(Modifier.padding(start=16.dp,end=10.dp,top=12.dp,bottom=12.dp),verticalAlignment=Alignment.CenterVertically){
        Row(Modifier.weight(1f).clickable(onClick=onPlay),verticalAlignment=Alignment.CenterVertically){SongArtwork(file,Modifier.size(48.dp));Spacer(Modifier.width(12.dp));Column(Modifier.weight(1f)){Text(trackTitle(file),fontSize=15.sp,lineHeight=22.sp,color=if(current)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface);if(subtitle.isNotEmpty())Text(subtitle,fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.padding(top=4.dp))}}
        if(current&&action!="play"&&action!="pause")UiAction(if(playing)"pause"else"play",if(playing)"暂停"else"播放",onCurrentToggle)
        UiAction(action,actionLabel,onAction)
    }}
}
@Composable fun PlaylistChoices(title:String,track:RemoteFile,lists:List<MusicList>,initial:Set<String>,onBack:()->Unit,onSave:(Set<String>)->Unit){
    var choices by remember(track.url){mutableStateOf(initial)}
    Column(Modifier.fillMaxSize()){
        PageHeader(title,onBack)
        CardSurface(Modifier.padding(top=20.dp),corner=22){Column(Modifier.padding(20.dp)){Text("加入歌单",fontSize=20.sp,fontWeight=FontWeight.Medium);Text("可同时加入多个歌单",fontSize=14.sp,lineHeight=23.sp,color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.padding(vertical=16.dp));
            if(lists.isEmpty())Text("请先在歌单页新建歌单",fontSize=14.sp)
            LazyColumn{items(lists,key={it.id}){list->Row(Modifier.fillMaxWidth().heightIn(min=62.dp).toggleable(value=list.id in choices,role=Role.Checkbox,onValueChange={choices=if(it)choices+list.id else choices-list.id}),verticalAlignment=Alignment.CenterVertically){val checked=list.id in choices;Box(Modifier.size(22.dp).clip(RoundedCornerShape(6.dp)).background(if(checked)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface).border(1.dp,if(checked)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,RoundedCornerShape(6.dp)),contentAlignment=Alignment.Center){if(checked)Text("✓",fontSize=16.sp,color=Color.White)};Spacer(Modifier.width(14.dp));Text(list.name,fontSize=16.sp)};if(list!=lists.last())HorizontalDivider(color=MaterialTheme.colorScheme.outline.copy(alpha=.5f))}}
        }}
        Spacer(Modifier.weight(1f));SoftButton({onSave(choices)},Modifier.fillMaxWidth().padding(top=12.dp,bottom=18.dp)){Text("保存选择")}
    }
}
@Composable fun LibraryScreens(
    page:String,navigate:(String)->Unit,songs:List<RemoteFile>,lists:List<MusicList>,saveLists:(List<MusicList>)->Unit,play:(RemoteFile,List<RemoteFile>)->Unit,
    chooseMusic:()->Unit,chooseLyrics:()->Unit,musicFolder:String,lyricFolder:String,scan:()->Unit,connected:Boolean,cacheBytes:Long,clearCache:()->Unit,
    theme:Int,setTheme:(Int)->Unit,lyricFiles:List<RemoteFile>,openLyrics:(RemoteFile)->Unit,matching:(RemoteFile)->String,manual:(RemoteFile)->Unit,hideBottom:(Boolean)->Unit={},hideMini:(Boolean)->Unit={},currentUrl:String?=null,isPlaying:Boolean=false,onToggleCurrent:()->Unit={}
){
    val context=LocalContext.current
    var cover by remember{mutableStateOf("")};var query by remember{mutableStateOf("")};var selected by remember{mutableStateOf<String?>(null)}
    var editing by remember{mutableStateOf<String?>(null)};var nameError by remember{mutableStateOf(false)};var listName by remember{mutableStateOf("")};var adding by remember{mutableStateOf<RemoteFile?>(null)}
    var issues by remember{mutableStateOf(false)}
    val coverPicker=rememberLauncherForActivityResult(ActivityResultContracts.GetContent()){uri->if(uri!=null)runCatching{val target=java.io.File(context.filesDir,"cover-${java.util.UUID.randomUUID()}");context.contentResolver.openInputStream(uri)?.use{input->target.outputStream().use{input.copyTo(it)}};cover=target.absolutePath}}
    LaunchedEffect(adding){hideBottom(adding!=null)}
    LaunchedEffect(editing){hideMini(editing!=null);nameError=false}
    DisposableEffect(Unit){onDispose{hideBottom(false);hideMini(false)}}
    BackHandler(editing!=null||adding!=null||(page=="lists"&&selected!=null)||issues){when{adding!=null->adding=null;editing!=null->editing=null;issues->issues=false;else->selected=null}}
    if(adding!=null){val track=adding!!;PlaylistChoices("加入歌单",track,lists,lists.filter{track.url in it.songs}.map{it.id}.toSet(),{adding=null}){choices->saveLists(lists.map{it.copy(songs=if(it.id in choices)(it.songs+track.url).distinct()else it.songs-track.url)});adding=null};return}
    if(editing!=null){Column(Modifier.fillMaxSize().imePadding()){
        PageHeader(if(editing=="")"新建歌单" else "编辑歌单",{editing=null})
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())){
            Text("歌单名称",fontSize=12.sp,color=MaterialTheme.colorScheme.primary)
            OutlinedTextField(listName,{listName=it.take(40);nameError=false},isError=nameError,supportingText=if(nameError)({Text("请输入歌单名称")})else null,placeholder={Text("为这一刻取个名字")},singleLine=true,shape=RoundedCornerShape(12.dp),colors=fieldColors(),modifier=Modifier.fillMaxWidth().padding(top=8.dp,bottom=14.dp))
            CardSurface{Column(Modifier.padding(16.dp)){Text("封面（可选）",fontSize=12.sp,color=MaterialTheme.colorScheme.primary)
                Row(Modifier.fillMaxWidth().clickable{coverPicker.launch("image/*")}.padding(vertical=14.dp),verticalAlignment=Alignment.CenterVertically){
                    val bitmap=remember(cover){if(cover.isEmpty())null else BitmapFactory.decodeFile(cover)}
                    Box(Modifier.size(54.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primaryContainer),contentAlignment=Alignment.Center){if(bitmap!=null)Image(bitmap.asImageBitmap(),null,Modifier.fillMaxSize(),contentScale=ContentScale.Crop)else UiIcon("album")}
                    Spacer(Modifier.width(12.dp));Column(Modifier.weight(1f)){Text(if(cover.isEmpty())"选择封面" else "更换封面",fontSize=15.sp);Text("点击选取图片，可随时更换",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.padding(top=6.dp))};Text("＋",fontSize=16.sp)
                }
            }}
        }
        SoftButton({val n=listName.trim();if(n.isEmpty())nameError=true else{saveLists(if(editing=="")lists+MusicList(name=n,cover=cover)else lists.map{if(it.id==editing)it.copy(name=n,cover=cover)else it});editing=null}},Modifier.fillMaxWidth().padding(top=12.dp,bottom=18.dp)){Text(if(editing=="")"创建歌单" else "保存修改")}
    };return}
    val chosen=if(page=="lists")lists.firstOrNull{it.id==selected}else null
    val title=when(page){"library"->"音乐库";"lists"->if(chosen==null)"我的歌单"else"歌单详情";"settings"->"设置";"manage"->"音乐库管理";"theme"->"外观主题";"cache"->"缓存管理";"lyrics"->if(issues)"异常歌词"else"歌词管理";"about"->"关于 Slowly";else->"Slowly Music"}
    Column(Modifier.fillMaxSize()){
        val back:(()->Unit)?=when{chosen!=null->({selected=null});issues->({issues=false});page !in listOf("library","lists","settings")->({navigate("settings")});else->null}
        PageHeader(title,back,if(back==null)"把喜欢的声音，好好收藏。" else "留一点时间，给音乐。")
        when(page){
            "library"->{OutlinedTextField(query,{query=it},placeholder={Text("搜索歌曲或歌手")},singleLine=true,shape=RoundedCornerShape(12.dp),colors=fieldColors(),modifier=Modifier.fillMaxWidth());Text("全部歌曲 · ${songs.size} 首",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.padding(top=12.dp,bottom=5.dp));if(songs.isEmpty()){Text("选择云盘音乐目录，建立你的音乐库。",fontSize=14.sp,modifier=Modifier.weight(1f).padding(top=18.dp));SoftButton(chooseMusic,Modifier.fillMaxWidth().padding(bottom=18.dp)){Text("选择音乐目录")}}else LocatedTracks(songs.filter{(it.title+" "+it.artist+" "+it.name).contains(query,true)},currentUrl,Modifier.weight(1f),reveal={query=""},canLocate=songs.any{it.url==currentUrl}){file->MusicRow(file,{play(file,songs)},{adding=file},currentUrl==file.url,isPlaying,onToggleCurrent)}}
            "lists"->if(chosen==null){SoftOutlinedButton({editing="";listName="";cover=""},Modifier.fillMaxWidth().padding(bottom=8.dp)){Text("＋ 新建歌单")};LazyColumn(Modifier.weight(1f),contentPadding=PaddingValues(bottom=18.dp)){items(lists,key={it.id}){list->CardSurface(onClick={selected=list.id}){Row(Modifier.padding(16.dp),verticalAlignment=Alignment.CenterVertically){PlaylistCover(list.cover,Modifier.size(48.dp));Spacer(Modifier.width(12.dp));Column(Modifier.weight(1f)){Text(list.name,fontSize=15.sp);Text("${list.songs.size} 首歌曲",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.padding(top=4.dp))};Text("›",color=MaterialTheme.colorScheme.primary)}}}}}else{
                val tracks=songs.filter{it.url in chosen.songs}
                LocatedTracks(tracks,currentUrl,Modifier.weight(1f),header={PlaylistCover(chosen.cover,Modifier.fillMaxWidth().height(230.dp).clip(RoundedCornerShape(20.dp)));Text(chosen.name,fontSize=20.sp,fontWeight=FontWeight.Medium,modifier=Modifier.padding(top=18.dp,bottom=14.dp));SoftButton({tracks.firstOrNull()?.let{play(it,tracks)}},Modifier.fillMaxWidth(),tracks.isNotEmpty()){UiIcon("play",null,Modifier.size(18.dp),Color.White);Text("播放歌单")};Row{TextButton({editing=chosen.id;listName=chosen.name;cover=chosen.cover}){Text("编辑歌单")};TextButton({saveLists(lists.filterNot{it.id==chosen.id});selected=null}){Text("删除歌单")}}}){file->SongCard(file,trackArtist(file),{play(file,tracks)},"remove","从歌单移除",current=currentUrl==file.url,playing=isPlaying,onCurrentToggle=onToggleCurrent){saveLists(lists.map{if(it.id==chosen.id)it.copy(songs=it.songs-file.url)else it})}}
            }
            "settings"->LazyColumn(Modifier.weight(1f),contentPadding=PaddingValues(bottom=18.dp)){items(listOf(Triple("webdav","WebDav 连接",if(connected)"已连接 · 私人网盘" else "连接你的音乐空间"),Triple("manage","音乐库管理","目录、扫描与歌词"),Triple("theme","外观主题","五种浅色主题"),Triple("cache","缓存管理","自动缓存 · 上限 5GB"),Triple("about","关于播放器","Slowly Music"))){(route,label,subtitle)->SettingCard(label,subtitle){navigate(route)}}}
            "manage"->{Column(Modifier.weight(1f).verticalScroll(rememberScrollState())){SettingCard("音乐目录",displayPath(musicFolder).ifEmpty{"尚未设置"},chooseMusic);SettingCard("统一歌词目录",displayPath(lyricFolder).ifEmpty{"尚未设置"},chooseLyrics);Text("递归扫描所选目录及全部子目录",fontSize=13.sp,color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.padding(vertical=14.dp));Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Stat(songs.size,"已扫描 MP3",Modifier.weight(1f));Stat(songs.count{matching(it)=="已匹配"},"正常匹配歌词",Modifier.weight(1f))};SettingCard("歌词管理","匹配统计与异常处理"){navigate("lyrics")};if(!connected)Text("连接网盘后可重新扫描。",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)};SoftButton(scan,Modifier.fillMaxWidth().padding(top=12.dp,bottom=18.dp),connected&&musicFolder.isNotEmpty()){Text("重新扫描音乐库")}}
            "lyrics"->if(issues){if(songs.none{matching(it)!="已匹配"})Text("当前没有异常歌词",fontSize=14.sp,color=MaterialTheme.colorScheme.onSurfaceVariant);LazyColumn(Modifier.weight(1f)){items(songs.filter{matching(it)!="已匹配"},key={it.url}){file->CardSurface(onClick={manual(file)}){Column(Modifier.padding(16.dp)){Text(trackTitle(file),fontSize=15.sp);Text(matching(file),fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}}}}}else{Column(Modifier.weight(1f).verticalScroll(rememberScrollState())){SettingCard("统一歌词目录",displayPath(lyricFolder).ifEmpty{"尚未设置"},chooseLyrics);val statuses=songs.groupingBy{matching(it)}.eachCount();Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Stat(statuses["已匹配"]?:0,"正常匹配",Modifier.weight(1f));Stat(statuses["缺少歌词"]?:0,"缺少歌词",Modifier.weight(1f))};Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Stat(statuses["待选择"]?:0,"待选择",Modifier.weight(1f));Stat(statuses["格式异常"]?:0,"格式异常",Modifier.weight(1f))};SettingCard("异常歌词列表","查看路径、只读预览、手动指定"){issues=true};lyricFiles.forEach{file->TextButton({openLyrics(file)}){Text("只读预览：${file.name}",fontSize=12.sp)}}};SoftButton(scan,Modifier.fillMaxWidth().padding(top=12.dp,bottom=18.dp),connected){Text("重新扫描歌词")}}
            "theme"->Column(Modifier.weight(1f).verticalScroll(rememberScrollState())){Text("选择后立即应用到整个应用，切换页面也会保留。",fontSize=13.sp,lineHeight=23.sp,color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.padding(top=8.dp,bottom=14.dp));gentleThemes.forEachIndexed{i,t->Surface(onClick={setTheme(i)},shape=RoundedCornerShape(14.dp),border=BorderStroke(1.dp,if(i==theme)t.accent else MaterialTheme.colorScheme.outline),color=if(i==theme)t.soft else MaterialTheme.colorScheme.surface,modifier=Modifier.fillMaxWidth().padding(vertical=5.dp)){Row(Modifier.padding(16.dp).heightIn(min=46.dp),verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(46.dp).clip(RoundedCornerShape(13.dp)).background(Brush.linearGradient(listOf(t.start,t.background))).border(1.dp,t.accent.copy(alpha=.35f),RoundedCornerShape(13.dp)));Text(t.name,Modifier.weight(1f).padding(start=16.dp),fontSize=16.sp);if(i==theme)Text("✓",fontSize=22.sp,color=t.accent)}}}}
            "cache"->{Column(Modifier.weight(1f).verticalScroll(rememberScrollState())){CardSurface{Column(Modifier.padding(16.dp)){Text("音乐缓存使用量",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant);Row(verticalAlignment=Alignment.Bottom){Text("${cacheBytes/1024/1024} MB",fontSize=28.sp);Text(" / 5GB",fontSize=12.sp,modifier=Modifier.padding(bottom=5.dp))};LinearProgressIndicator(progress={(cacheBytes.toFloat()/MusicCache.LIMIT).coerceIn(0f,1f)},modifier=Modifier.fillMaxWidth().padding(top=14.dp))}};CardSurface{Row(Modifier.padding(16.dp)){Text("在线播放自动缓存",Modifier.weight(1f));Text("已开启",color=MaterialTheme.colorScheme.primary)}};CardSurface{Row(Modifier.padding(16.dp)){Text("默认缓存上限",Modifier.weight(1f));Text("5GB",color=MaterialTheme.colorScheme.primary)}};Text("达到上限后，自动淘汰最久未使用的音乐缓存。清理仅作用于本机音频缓存，保留歌单、歌词及云盘原文件。",fontSize=13.sp,lineHeight=23.sp,color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.padding(top=12.dp))};SoftButton(clearCache,Modifier.fillMaxWidth().padding(top=12.dp,bottom=18.dp)){Text("清理音频缓存")}}
            "about"->Column(Modifier.weight(1f).verticalScroll(rememberScrollState())){CoverTile(Modifier.fillMaxWidth().height(230.dp).clip(RoundedCornerShape(20.dp)));Text("Slowly Music",fontSize=20.sp,fontWeight=FontWeight.Medium,modifier=Modifier.padding(top=20.dp,bottom=8.dp));Text("留一点时间，给音乐。",fontSize=14.sp,color=MaterialTheme.colorScheme.onSurfaceVariant);CardSurface(Modifier.padding(top=14.dp)){Column(Modifier.padding(16.dp)){Text("个人音乐空间");Text("WebDav · MP3 · LRC\nAndroid 原生应用 · 0.4.0",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}}}
        }
    }
}
@Composable private fun PlaylistCover(path:String,modifier:Modifier){val bitmap=remember(path){if(path.isEmpty())null else BitmapFactory.decodeFile(path)};if(bitmap==null)CoverTile(modifier)else Image(bitmap.asImageBitmap(),"歌单封面",modifier.clip(RoundedCornerShape(18.dp)),contentScale=ContentScale.Crop)}
@Composable private fun Stat(number:Int,label:String,modifier:Modifier){Surface(modifier.padding(vertical=4.dp),shape=RoundedCornerShape(16.dp),color=MaterialTheme.colorScheme.surface,border=BorderStroke(1.dp,MaterialTheme.colorScheme.outline)){Column(Modifier.padding(14.dp)){Text(number.toString(),fontSize=26.sp,color=MaterialTheme.colorScheme.primary);Text(label,fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}}}
@Composable fun fieldColors()=OutlinedTextFieldDefaults.colors(unfocusedContainerColor=MaterialTheme.colorScheme.surface,focusedContainerColor=MaterialTheme.colorScheme.surface,unfocusedBorderColor=MaterialTheme.colorScheme.outline)
fun displayPath(url:String)=runCatching{java.net.URI(url).path}.getOrDefault(url)


fun trackTitle(file:RemoteFile)=file.title.ifBlank{if(file.tagsRead)"未知歌曲"else"正在读取歌曲信息"}
fun trackArtist(file:RemoteFile)=file.artist.ifBlank{"未知歌手"}
@Composable fun SongArtwork(file:RemoteFile,modifier:Modifier){
    val image by produceState<ImageBitmap?>(null,file.artwork){value=withContext(Dispatchers.IO){ArtworkTools.thumbnail(file.artwork)}}
    if(image==null)CoverTile(modifier)else Image(image!!,"歌曲封面",modifier.clip(RoundedCornerShape(12.dp)),contentScale=ContentScale.Crop)
}
@Composable private fun NowPlayingMark(playing:Boolean){
    val color=MaterialTheme.colorScheme.primary
    Canvas(Modifier.padding(start=8.dp).size(16.dp).semantics{contentDescription=if(playing)"正在播放"else"当前歌曲已暂停"}){
        if(playing)listOf(.4f,.85f,.6f).forEachIndexed{i,h->val x=size.width*(i+1)/4;drawLine(color,Offset(x,size.height*(1-h)/2),Offset(x,size.height*(1+h)/2),strokeWidth=2.dp.toPx(),cap=StrokeCap.Round)}
        else listOf(.35f,.65f).forEach{x->drawLine(color,Offset(size.width*x,size.height*.25f),Offset(size.width*x,size.height*.75f),strokeWidth=2.dp.toPx(),cap=StrokeCap.Round)}
    }
}

@Composable fun LocatedTracks(tracks:List<RemoteFile>,current:String?,modifier:Modifier=Modifier,reveal:()->Unit={},canLocate:Boolean=tracks.any{it.url==current},header:(@Composable ()->Unit)?=null,row:@Composable (RemoteFile)->Unit){
    val state=androidx.compose.foundation.lazy.rememberLazyListState()
    var request by remember{mutableIntStateOf(0)}
    LaunchedEffect(request,tracks.map{it.url}){if(request>0){val index=tracks.indexOfFirst{it.url==current};if(index>=0)state.animateScrollToItem(index+if(header!=null)1 else 0)}}
    Box(modifier.fillMaxWidth()){
        LazyColumn(state=state,modifier=Modifier.fillMaxSize(),contentPadding=PaddingValues(bottom=88.dp)){
            if(header!=null)item{header()}
            items(tracks,key={it.url}){row(it)}
            if(tracks.isEmpty())item{Text("暂无歌曲",Modifier.padding(vertical=20.dp),color=MaterialTheme.colorScheme.onSurfaceVariant)}
        }
        if(canLocate)SmallFloatingActionButton({reveal();request++},Modifier.align(Alignment.BottomEnd).padding(10.dp),containerColor=MaterialTheme.colorScheme.primaryContainer,contentColor=MaterialTheme.colorScheme.primary){UiIcon("music","定位当前歌曲")}
    }
}
