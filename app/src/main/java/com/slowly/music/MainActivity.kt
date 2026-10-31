package com.slowly.music

import android.os.Bundle
import android.graphics.BitmapFactory
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.*
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.slowly.lyrics.*
import kotlinx.coroutines.*

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class MainActivity:ComponentActivity(){
    override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);androidx.core.view.WindowCompat.getInsetsController(window,window.decorView).apply{isAppearanceLightStatusBars=true;isAppearanceLightNavigationBars=true};setContent{App()}}
    @Composable private fun App(){
        val preferences=remember{getSharedPreferences("display",MODE_PRIVATE)}
        val connectionStore=remember{ConnectionStore(this@MainActivity)}
        val saved=remember{connectionStore.read()}
        val initialAccount=remember{saved?.let{it.address.trimEnd('/')+"|"+it.user}?:preferences.getString("account","local")?:"local"}
        val store=remember{LyricStore(this@MainActivity).apply{accountScope=initialAccount}}
        var library by remember{mutableStateOf(LibraryStore(this@MainActivity,initialAccount))}
        var musicFiles by remember{mutableStateOf(library.songs())};var playlists by remember{mutableStateOf(library.lists())}
        var page by remember{mutableStateOf("library")};var showPlayer by remember{mutableStateOf(false)};var showQueue by remember{mutableStateOf(false)}
        var settings by remember{mutableStateOf(false)};var addCurrent by remember{mutableStateOf(false)};var libraryOverlay by remember{mutableStateOf(false)};var libraryForm by remember{mutableStateOf(false)}
        var theme by remember{mutableIntStateOf(preferences.getInt("theme",0).coerceIn(0,4))};var lyricMode by remember{mutableStateOf(preferences.getBoolean("lyricMode",true))}
        var size by remember{mutableFloatStateOf(preferences.getFloat("size",18f))};var blurred by remember{mutableStateOf(preferences.getBoolean("blurred",false))}
        var playMode by remember{mutableIntStateOf(preferences.getInt("playMode",0).coerceIn(0,2))}
        var address by remember{mutableStateOf(saved?.address?:initialAccount.substringBefore('|').takeIf{it.startsWith("https://")}?:"https://dav.jianguoyun.com/dav/")}
        var user by remember{mutableStateOf(saved?.user?:initialAccount.substringAfter('|',""))};var password by remember{mutableStateOf(saved?.password?:"")};var rememberConnection by remember{mutableStateOf(true)}
        var dav by remember{mutableStateOf(saved?.let{Dav(it.address,it.user,it.password)}?:PlaybackEngine.connection)}
        var chooseMusic by remember{mutableStateOf(false)};var folder by remember{mutableStateOf("")};var files by remember{mutableStateOf(emptyList<RemoteFile>())}
        val initialRoot=initialAccount.substringBefore('|').trimEnd('/')+"/"
        var lyricFolder by remember{mutableStateOf(store.folder(initialRoot))};var lyricFiles by remember{mutableStateOf(store.index(initialRoot))}
        var candidates by remember{mutableStateOf(emptyList<RemoteFile>())};var matchingTarget by remember{mutableStateOf<RemoteFile?>(null)};var rawPreview by remember{mutableStateOf<String?>(null)}
        var song by remember{mutableStateOf<RemoteFile?>(PlaybackEngine.current)};var pendingSong by remember{mutableStateOf<RemoteFile?>(null)};var queue by remember{mutableStateOf(PlaybackEngine.queue)}
        var lines by remember{mutableStateOf(emptyList<Line>())};var player by remember{mutableStateOf<ExoPlayer?>(PlaybackEngine.player)};var loadJob by remember{mutableStateOf<Job?>(null)}
        val clock=remember{PlaybackClock()};var probe by remember{mutableStateOf<PlaybackProbe?>(null)};var tagGeneration by remember{mutableIntStateOf(0)};var playing by remember{mutableStateOf(false)};var buffering by remember{mutableStateOf(false)}
        var playbackError by remember{mutableStateOf<String?>(null)};var artwork by remember{mutableStateOf<ImageBitmap?>(null)};var artworkBytes by remember{mutableStateOf<ByteArray?>(null)};var softArtwork by remember{mutableStateOf<ImageBitmap?>(null)};var artJob by remember{mutableStateOf<Job?>(null)}
        var artist by remember{mutableStateOf("")};var album by remember{mutableStateOf("")};var cacheBytes by remember{mutableLongStateOf(0)}
        var busy by remember{mutableStateOf(false)};var message by remember{mutableStateOf("")}
        var qqBackdrop by remember{mutableStateOf<ImageBitmap?>(null)}
        val scope=rememberCoroutineScope()
        var uiListener by remember{mutableStateOf<Player.Listener?>(null)}
        DisposableEffect(Unit){onDispose{loadJob?.cancel();artJob?.cancel();uiListener?.let{player?.removeListener(it)};probe?.let{player?.removeAnalyticsListener(it)}}}
        LaunchedEffect(PlaybackEngine.error){playbackError=PlaybackEngine.error}
        LaunchedEffect(message){if(message.isNotEmpty()){delay(4500);message=""}}
        LaunchedEffect(player){while(isActive){player?.let{clock.position=it.currentPosition;clock.duration=it.duration.coerceAtLeast(0);probe?.sample(it)};delay(200)}}
        LaunchedEffect(Unit){while(isActive){cacheBytes=withContext(Dispatchers.IO){MusicCache.get(this@MainActivity).cacheSpace};delay(3000)}}
        LaunchedEffect(dav,musicFiles.map{it.url},playing,buffering,tagGeneration){
            val connection=dav
            if(connection!=null&&!playing&&!buffering){
                val account=store.accountScope;val repository=TagRepository(this@MainActivity,account)
                for(file in musicFiles.filter{!it.tagsRead}){
                    val tagged=withContext(Dispatchers.IO){runCatching{repository.read(connection,file)}.getOrNull()}
                    ensureActive()
                    if(tagged!=null&&store.accountScope==account){musicFiles=musicFiles.map{if(it.url==file.url)tagged else it};library.saveSongs(musicFiles)}
                }
            }
        }
        fun browse(path:String){scope.launch{busy=true;try{files=withContext(Dispatchers.IO){(dav?:error("请先连接网盘")).list(path)};folder=path}catch(e:CancellationException){throw e}catch(e:Exception){message=e.message?:"目录读取失败"}finally{busy=false}}}
        fun start(file:RemoteFile,activate:Boolean=true){
            val connection=dav?:PlaybackEngine.connection
            if(connection==null){pendingSong=file;page="webdav";showPlayer=false;message="请连接网盘，连接后会继续播放这首歌";return}
            QqBridge.pause(this@MainActivity)
            pendingSong=null;candidates=emptyList();matchingTarget=null;showQueue=false;if(activate)showPlayer=true;song=file;artist=file.artist;album=file.album;artwork=null;artworkBytes=null;softArtwork=null;lines=emptyList();clock.position=0;clock.duration=0;buffering=true;playbackError=null
            loadJob?.cancel();artJob?.cancel();uiListener?.let{player?.removeListener(it)};probe?.let{player?.removeAnalyticsListener(it)}
            val active=if(activate)PlaybackEngine.select(this@MainActivity,file,queue,connection.authorization,store.accountScope,playMode)else PlaybackEngine.player?:return
            PlaybackEngine.connection=connection
            player=active
            val stats=PlaybackProbe();probe=stats;active.addAnalyticsListener(stats)
            val cache=MusicCache.get(this@MainActivity);val cacheKey=store.accountScope+"|"+file.url
            val cachedLength=androidx.media3.datasource.cache.ContentMetadata.getContentLength(cache.getContentMetadata(cacheKey))
            android.util.Log.i("SlowlyPlayback","start fullCache=${cachedLength>0&&cache.isCached(cacheKey,0,cachedLength)} bytes=$cachedLength")
            if(file.artwork.isNotEmpty())artJob=scope.launch{val pair=withContext(Dispatchers.Default){runCatching{ArtworkTools.decode(java.io.File(file.artwork).readBytes())}.getOrNull()};if(player===active&&active.currentMediaItem?.mediaId==file.url){artwork=pair?.cover;softArtwork=pair?.background}}
            val listener=object:Player.Listener{
                override fun onIsPlayingChanged(isPlaying:Boolean){if(player===active)playing=isPlaying}
                override fun onPlaybackStateChanged(state:Int){if(player===active)buffering=state==Player.STATE_BUFFERING}
                override fun onMediaMetadataChanged(metadata:MediaMetadata){
                    if(player!==active||active.currentMediaItem?.mediaId!=file.url)return
                    val currentFile=musicFiles.firstOrNull{it.url==file.url}?:file
                    artist=metadata.artist?.toString()?.takeIf{it.isNotBlank()}?:currentFile.artist
                    album=metadata.albumTitle?.toString()?.takeIf{it.isNotBlank()}?:currentFile.album
                    val title=metadata.title?.toString()?.takeIf{it.isNotBlank()}?:currentFile.title
                    val bytes=metadata.artworkData
                    val repository=TagRepository(this@MainActivity,store.accountScope)
                    artJob?.cancel()
                    artJob=scope.launch{
                        val pair=if(bytes!=null)withContext(Dispatchers.Default){ArtworkTools.decode(bytes)}else null
                        val path=if(bytes!=null)withContext(Dispatchers.IO){repository.coverPath(file.url,bytes)}else currentFile.artwork
                        ensureActive();if(player!==active||active.currentMediaItem?.mediaId!=file.url)return@launch
                        if(pair!=null){artwork=pair.cover;softArtwork=pair.background;artworkBytes=bytes}
                        val tagged=currentFile.copy(title=title,artist=artist,album=album,artwork=path,tagsRead=true)
                        song=tagged;musicFiles=musicFiles.map{if(it.url==file.url)tagged else it};queue=queue.map{if(it.url==file.url)tagged else it};library.saveSongs(musicFiles)
                    }
                }
            }
            uiListener=listener;active.addListener(listener)
            playing=active.isPlaying;buffering=active.playbackState==Player.STATE_BUFFERING
            listener.onMediaMetadataChanged(active.mediaMetadata)
            loadJob=scope.launch{
                try{
                    val match=matchLyrics(file.name,lyricFiles.map{java.net.URI(it.url).path},store.manual(file.url))
                    if(match is Match.Selected){val remote=lyricFiles.first{java.net.URI(it.url).path==match.path};val parsed=withContext(Dispatchers.IO){val raw=try{connection.read(remote.url).also{store.cache(remote.url,it)}}catch(e:Exception){store.cached(remote.url)?:throw e};Lrc.parse(raw)};ensureActive();lines=parsed}
                    // Conflicts remain manual; fallback never chooses an arbitrary external file.
                    if(lines.isEmpty()){val embedded=withContext(Dispatchers.IO){runCatching{connection.embedded(file.url)}.getOrDefault(emptyList())};ensureActive();lines=embedded}
                }catch(e:CancellationException){throw e}catch(_:Exception){/* A lyric failure must never stop audio playback. */}
            }
        }
        fun toggle(){val p=player?:return;if(p.playerError!=null){song?.let{start(it)}}else if(p.isPlaying)p.pause()else{QqBridge.pause(this@MainActivity);if(p.playbackState==Player.STATE_ENDED)p.seekTo(0);p.play()}}
        fun step(direction:Int){val index=queue.indexOfFirst{it.url==song?.url};val next=if(playMode==1)queue.filter{it.url!=song?.url}.randomOrNull()?:queue.firstOrNull()else queue.getOrNull(Math.floorMod(index+direction,queue.size.coerceAtLeast(1)));next?.let{start(it)}}
        fun scan(){scope.launch{busy=true;try{val connection=dav?:error("请先连接网盘");if(library.musicFolder.isNotEmpty()){musicFiles=withContext(Dispatchers.IO){connection.scan(library.musicFolder).filter{it.name.endsWith(".mp3",true)}};library.saveSongs(musicFiles);tagGeneration++};if(lyricFolder.isNotEmpty()){lyricFiles=withContext(Dispatchers.IO){connection.scan(lyricFolder).filter{it.name.endsWith(".lrc",true)}};store.saveIndex(connection.root,lyricFolder,lyricFiles)};message="扫描完成"}catch(e:CancellationException){throw e}catch(e:Exception){message=e.message?:"扫描失败"}finally{busy=false}}}
        fun pick(music:Boolean){chooseMusic=music;if(dav==null){page="webdav";message="请先连接网盘"}else{page="browser";browse(dav!!.root)}}
        fun connect(){scope.launch{busy=true;try{
            val root=address.trim().trimEnd('/')+"/";require(java.net.URI(root).scheme=="https"){"请使用 HTTPS 地址"};require(user.isNotBlank()&&password.isNotBlank()){"请输入账号和应用密码"}
            val connection=Dav(root,user.trim(),password);withContext(Dispatchers.IO){connection.list(root)}
            val account=root.trimEnd('/')+"|"+user.trim();val sameAccount=account==store.accountScope
            if(rememberConnection)withContext(Dispatchers.IO){connectionStore.save(SavedConnection(root,user.trim(),password))}else connectionStore.clear()
            preferences.edit().putString("account",account).apply();store.accountScope=account;dav=connection;address=root;library=LibraryStore(this@MainActivity,account);musicFiles=library.songs();playlists=library.lists();lyricFolder=store.folder(root);lyricFiles=store.index(root);page="manage";message="网盘连接成功"
            val pending=pendingSong;pendingSong=null;if(pending!=null&&sameAccount)start(pending)else if(!sameAccount){PlaybackEngine.release();player=null;song=null;queue=emptyList()}
        }catch(e:CancellationException){throw e}catch(e:Exception){message=e.message?:"连接失败，请检查网络或应用密码"}finally{busy=false}}}
        LaunchedEffect(PlaybackEngine.current?.url){PlaybackEngine.current?.let{if(song?.url!=it.url||uiListener==null){queue=PlaybackEngine.queue;start(it,false)}}}
        LaunchedEffect(playMode){PlaybackEngine.mode(playMode)}
        val globalOverlay=showQueue||settings||addCurrent||matchingTarget!=null||rawPreview!=null
        BackHandler(!showPlayer&&page !in listOf("library","lists","settings")){if(page=="browser"&&folder.isNotEmpty()&&folder!=dav?.root)browse(java.net.URI(folder).resolve("../").toString())else page="settings"}
        BackHandler(showPlayer){showPlayer=false}
        BackHandler(globalOverlay){when{rawPreview!=null->rawPreview=null;matchingTarget!=null->{candidates=emptyList();matchingTarget=null};addCurrent->addCurrent=false;settings->settings=false;else->showQueue=false}}
        val palette=gentleThemes[theme]
        MaterialTheme(typography=slowlyTypography,colorScheme=lightColorScheme(primary=palette.accent,onPrimary=Color.White,primaryContainer=palette.soft,onPrimaryContainer=palette.accent,secondary=palette.accent,secondaryContainer=palette.coverLight,onSecondaryContainer=palette.ink,surfaceVariant=palette.soft,outline=palette.line,surface=palette.surface,background=palette.start,onBackground=palette.ink,onSurface=palette.ink,onSurfaceVariant=palette.muted)){
            CompositionLocalProvider(LocalContentColor provides palette.ink){
            Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(palette.start,palette.background)))){
                if(page=="qq"&&qqBackdrop!=null){Image(qqBackdrop!!,null,Modifier.fillMaxSize().alpha(.4f),contentScale=ContentScale.Crop);Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(palette.surface.copy(alpha=.5f),palette.surface.copy(alpha=.45f),palette.surface.copy(alpha=.7f)))))}
                if(showPlayer&&!globalOverlay&&blurred&&artwork!=null){Image(softArtwork?:artwork!!,null,Modifier.fillMaxSize().alpha(.4f),contentScale=ContentScale.Crop);Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(palette.surface.copy(alpha=.5f),palette.surface.copy(alpha=.45f),palette.surface.copy(alpha=.7f)))))}
                Column(Modifier.fillMaxSize().systemBarsPadding().imePadding()){
                    val standalone=showQueue||settings||addCurrent||rawPreview!=null||libraryOverlay||page=="browser"
                    if((!showPlayer&&page!="qq")||globalOverlay)Text("SLOWLY MUSIC",fontSize=10.sp,letterSpacing=3.sp,color=palette.muted,modifier=Modifier.padding(start=22.dp,top=10.dp,bottom=4.dp))
                    Box(Modifier.weight(1f).padding(horizontal=22.dp).padding(top=if(showPlayer&&!globalOverlay)10.dp else 0.dp)){
                        when{
                            rawPreview!=null->Column(Modifier.fillMaxSize()){PageHeader("原始歌词 · 只读",{rawPreview=null});Text(rawPreview!!,fontSize=13.sp,lineHeight=22.sp,modifier=Modifier.weight(1f).verticalScroll(rememberScrollState()))}
                            matchingTarget!=null->Column(Modifier.fillMaxSize()){
                                PageHeader("异常详情",{candidates=emptyList();matchingTarget=null})
                                Text("音乐文件：${displayPath(matchingTarget?.url.orEmpty())}",fontSize=12.sp,color=palette.muted)
                                Text(if(candidates.isEmpty())"未找到可选歌词，请先选择并扫描歌词目录。"else"不会自动选择同名冲突文件",fontSize=13.sp,modifier=Modifier.padding(vertical=12.dp))
                                if(candidates.isEmpty())SettingCard("统一歌词目录",displayPath(lyricFolder).ifEmpty{"尚未设置"}){matchingTarget=null;pick(false)}
                                LazyColumn(Modifier.weight(1f)){items(candidates){candidate->CardSurface{Column(Modifier.padding(16.dp)){
                                    Text(displayPath(candidate.url),fontSize=12.sp,color=palette.muted)
                                    store.cached(candidate.url)?.let{Text(it.lineSequence().take(2).joinToString("\n"),fontSize=12.sp,lineHeight=20.sp,color=palette.muted,modifier=Modifier.padding(top=12.dp))}
                                    Row{
                                        TextButton({scope.launch{rawPreview=withContext(Dispatchers.IO){runCatching{dav?.read(candidate.url)?.also{store.cache(candidate.url,it)}?:store.cached(candidate.url)?:"暂无缓存"}.getOrDefault("读取失败")}}}){Text("只读预览")}
                                        TextButton({val track=matchingTarget?:return@TextButton;store.select(track.url,java.net.URI(candidate.url).path);candidates=emptyList();matchingTarget=null;loadJob?.cancel();if(song?.url==track.url)loadJob=scope.launch{lines=withContext(Dispatchers.IO){runCatching{Lrc.parse(dav?.read(candidate.url)?.also{store.cache(candidate.url,it)}?:store.cached(candidate.url).orEmpty())}.getOrDefault(emptyList())}}}){Text("使用此歌词")}
                                    }
                                }}}}
                                SoftButton({val track=matchingTarget?:return@SoftButton;val selected=matchLyrics(track.name,lyricFiles.map{java.net.URI(it.url).path},store.manual(track.url));val file=if(selected is Match.Selected)lyricFiles.firstOrNull{java.net.URI(it.url).path==selected.path}else null;if(file==null)message="请先选择对应歌词"else scope.launch{busy=true;try{val raw=withContext(Dispatchers.IO){dav?.read(file.url)?.also{store.cache(file.url,it)}?:store.cached(file.url)?:error("请先连接网盘")};val parsed=Lrc.parse(raw);if(song?.url==track.url)lines=parsed;message=if(parsed.isEmpty())"格式异常：未找到同步歌词"else"已重新解析 ${parsed.size} 行歌词"}catch(e:Exception){message=e.message?:"解析失败"}finally{busy=false}}},Modifier.fillMaxWidth().padding(top=12.dp,bottom=18.dp),!busy){Text("重新解析")}
                            }
                            addCurrent&&song!=null->PlaylistChoices("加入歌单",song!!,playlists,playlists.filter{song!!.url in it.songs}.map{it.id}.toSet(),{addCurrent=false}){choices->val url=song!!.url;playlists=playlists.map{it.copy(songs=if(it.id in choices)(it.songs+url).distinct()else it.songs-url)};library.saveLists(playlists);addCurrent=false;message="已保存歌单选择"}
                            settings->LyricSettingsScreen(size,{size=it;preferences.edit().putFloat("size",it).apply()},blurred,{blurred=it;preferences.edit().putBoolean("blurred",it).apply()},softArtwork,{settings=false})
                            showQueue->QueueScreen(queue,song,playing,playMode,{showQueue=false},{start(it)},{toggle()})
                            showPlayer&&song!=null->PlayerScreen(song!!,artist,album,artwork,lines,lyricMode,size,clock,playing,buffering,playbackError,playMode,{showPlayer=false},{lyricMode=!lyricMode;preferences.edit().putBoolean("lyricMode",lyricMode).apply()},{settings=true},{addCurrent=true},{probe?.userSeek=true;player?.seekTo(it)},{toggle()},{step(-1)},{step(1)},{playMode=(playMode+1)%3;preferences.edit().putInt("playMode",playMode).apply();PlaybackEngine.mode(playMode)},{showQueue=true},{song?.let{start(it)}})
                            page=="qq"->QqMusicScreen({page="library"},{player?.pause()},{qqBackdrop=it})
                            page=="webdav"->Column(Modifier.fillMaxSize()){
                                PageHeader("WebDav 连接",{page="settings"})
                                Column(Modifier.weight(1f).verticalScroll(rememberScrollState())){
                                    Text("服务器地址",fontSize=12.sp,color=palette.accent);OutlinedTextField(address,{address=it},singleLine=true,shape=RoundedCornerShape(12.dp),colors=fieldColors(),modifier=Modifier.fillMaxWidth().padding(top=8.dp,bottom=14.dp))
                                    Text("用户名",fontSize=12.sp,color=palette.accent);OutlinedTextField(user,{user=it},singleLine=true,shape=RoundedCornerShape(12.dp),colors=fieldColors(),modifier=Modifier.fillMaxWidth().padding(top=8.dp,bottom=14.dp))
                                    Text("应用专用密码",fontSize=12.sp,color=palette.accent);OutlinedTextField(password,{password=it},singleLine=true,visualTransformation=PasswordVisualTransformation(),shape=RoundedCornerShape(12.dp),colors=fieldColors(),modifier=Modifier.fillMaxWidth().padding(top=8.dp,bottom=6.dp))
                                    Row(Modifier.fillMaxWidth().clickable{rememberConnection=!rememberConnection},verticalAlignment=Alignment.CenterVertically){Checkbox(rememberConnection,{rememberConnection=it});Text("记住连接（仅在本机加密保存）",fontSize=12.sp)}
                                    if(dav!=null)TextButton({connectionStore.clear();dav=null;PlaybackEngine.connection=null;password="";player?.pause();message="已断开连接"}){Text("断开并忘记连接")}
                                }
                                SoftButton({connect()},Modifier.fillMaxWidth().padding(top=12.dp,bottom=18.dp),!busy){Text(if(busy)"正在连接…"else"测试并保存连接")}
                            }
                            page=="browser"->Column(Modifier.fillMaxSize()){
                                PageHeader("选择${if(chooseMusic)"音乐"else"歌词"}目录",{if(folder.isNotEmpty()&&folder!=dav?.root)browse(java.net.URI(folder).resolve("../").toString())else page="manage"})
                                Text(displayPath(folder).ifEmpty{"正在读取目录…"},fontSize=12.sp,color=palette.muted,modifier=Modifier.padding(bottom=12.dp))
                                LazyColumn(Modifier.weight(1f)){items(files.filter{it.directory},key={it.url}){file->SettingCard(file.name,"文件夹"){if(!busy)browse(file.url)}}}
                                SoftButton({val root=folder;scope.launch{busy=true;try{val connection=dav?:error("请先连接网盘");val found=withContext(Dispatchers.IO){connection.scan(root)};if(chooseMusic){musicFiles=found.filter{it.name.endsWith(".mp3",true)};library.musicFolder=root;library.saveSongs(musicFiles);tagGeneration++}else{lyricFiles=found.filter{it.name.endsWith(".lrc",true)};lyricFolder=root;store.saveIndex(connection.root,root,lyricFiles)};page="manage";message="扫描完成"}catch(e:CancellationException){throw e}catch(e:Exception){message=e.message?:"目录扫描失败"}finally{busy=false}}},Modifier.fillMaxWidth().padding(top=12.dp,bottom=18.dp),!busy&&folder.isNotEmpty()){Text("使用当前目录")}
                            }
                            else->LibraryScreens(page,{page=it},musicFiles,playlists,{playlists=it;library.saveLists(it)},{file,tracks->queue=tracks;start(file)},{pick(true)},{pick(false)},library.musicFolder,lyricFolder,{scan()},dav!=null,cacheBytes,{scope.launch{player?.pause();withContext(Dispatchers.IO){MusicCache.clear(this@MainActivity)};cacheBytes=MusicCache.get(this@MainActivity).cacheSpace;message="音频缓存已清理"}},theme,{theme=it;preferences.edit().putInt("theme",it).apply()},lyricFiles,{file->scope.launch{rawPreview=withContext(Dispatchers.IO){runCatching{dav?.read(file.url)?.also{store.cache(file.url,it)}?:store.cached(file.url)?:"暂无缓存"}.getOrDefault("读取失败")}}},{file->when(matchLyrics(file.name,lyricFiles.map{java.net.URI(it.url).path},store.manual(file.url))){is Match.Selected->{val matched=matchLyrics(file.name,lyricFiles.map{java.net.URI(it.url).path},store.manual(file.url)) as Match.Selected;val remote=lyricFiles.firstOrNull{java.net.URI(it.url).path==matched.path};val raw=remote?.let{store.cached(it.url)};if(raw!=null&&Lrc.parse(raw).isEmpty())"格式异常"else"已匹配"};is Match.Conflict->"待选择";else->"缺少歌词"}},{file->matchingTarget=file;val match=matchLyrics(file.name,lyricFiles.map{java.net.URI(it.url).path},store.manual(file.url));candidates=if(match is Match.Conflict)lyricFiles.filter{java.net.URI(it.url).path in match.paths}else lyricFiles},{libraryOverlay=it},{libraryForm=it},song?.url,playing,onToggleCurrent={toggle()})
                        }
                    }
                    if(busy)LinearProgressIndicator(Modifier.fillMaxWidth().padding(horizontal=22.dp).height(2.dp))
                    if(!showPlayer&&!standalone&&page!="qq"){
                        if(song!=null&&!libraryForm&&page in listOf("library","lists"))FrostedPanel(Modifier.fillMaxWidth().padding(horizontal=16.dp).padding(bottom=8.dp)){
                            Row(Modifier.padding(start=14.dp,end=10.dp,top=6.dp,bottom=6.dp),verticalAlignment=Alignment.CenterVertically){Row(Modifier.weight(1f).heightIn(min=44.dp).clickable{showPlayer=true},verticalAlignment=Alignment.CenterVertically){UiIcon("music",null,Modifier.size(18.dp));Spacer(Modifier.width(10.dp));Column{Text(trackTitle(song!!),fontSize=12.sp);Text(artist.ifEmpty{"未知歌手"},fontSize=12.sp,color=palette.muted)}};UiAction(if(playing)"pause"else"play",if(playing)"暂停"else"播放",{toggle()})}
                        }
                        Spacer(Modifier.height(8.dp));BottomNavigation(page,{page=it})
                    }
                }
                if(message.isNotEmpty())Surface(Modifier.align(Alignment.BottomCenter).systemBarsPadding().padding(horizontal=24.dp).padding(bottom=if(showPlayer||globalOverlay)100.dp else 160.dp),shape=RoundedCornerShape(12.dp),color=palette.surface,border=BorderStroke(1.dp,palette.line),shadowElevation=3.dp){Text(message,Modifier.padding(horizontal=16.dp,vertical=10.dp),fontSize=13.sp)}
            }
            }
        }
    }
}
