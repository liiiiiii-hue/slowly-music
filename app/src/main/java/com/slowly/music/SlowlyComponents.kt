package com.slowly.music

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val slowlyTypography=Typography(
    bodyLarge=TextStyle(fontSize=14.sp,lineHeight=22.sp),bodyMedium=TextStyle(fontSize=14.sp,lineHeight=22.sp),
    bodySmall=TextStyle(fontSize=12.sp,lineHeight=20.sp),labelLarge=TextStyle(fontSize=15.sp,lineHeight=20.sp,fontWeight=FontWeight.Medium),
    labelMedium=TextStyle(fontSize=12.sp,lineHeight=18.sp),titleLarge=TextStyle(fontSize=26.sp,lineHeight=34.sp,fontWeight=FontWeight.Medium))

@Composable fun UiIcon(name:String,label:String?=null,modifier:Modifier=Modifier.size(22.dp),tint:Color=MaterialTheme.colorScheme.primary){
    val id=when(name){"library"->R.drawable.ic_library;"music"->R.drawable.ic_music;"settings"->R.drawable.ic_settings;"play"->R.drawable.ic_play;"pause"->R.drawable.ic_pause;"prev"->R.drawable.ic_prev;"next"->R.drawable.ic_next;"add"->R.drawable.ic_add;"remove"->R.drawable.ic_remove;"back"->R.drawable.ic_back;"down"->R.drawable.ic_down;"lyrics"->R.drawable.ic_lyrics;"album"->R.drawable.ic_album;"queue"->R.drawable.ic_queue;"shuffle"->R.drawable.ic_shuffle;"repeatOne"->R.drawable.ic_repeatone;else->R.drawable.ic_mode}
    Icon(painterResource(id),label,modifier,tint=tint)
}
@Composable fun UiAction(name:String,label:String,onClick:()->Unit){IconButton(onClick,Modifier.size(48.dp)){UiIcon(name,label,Modifier.size(21.dp))}}
@Composable fun SoftButton(onClick:()->Unit,modifier:Modifier=Modifier,enabled:Boolean=true,content:@Composable RowScope.()->Unit){
    Button(onClick,modifier.heightIn(min=48.dp),enabled,shape=RoundedCornerShape(15.dp),contentPadding=PaddingValues(horizontal=18.dp,vertical=12.dp),content=content)
}
@Composable fun SoftOutlinedButton(onClick:()->Unit,modifier:Modifier=Modifier,enabled:Boolean=true,content:@Composable RowScope.()->Unit){
    OutlinedButton(onClick,modifier.heightIn(min=48.dp),enabled,shape=RoundedCornerShape(15.dp),border=BorderStroke(1.dp,MaterialTheme.colorScheme.outline),colors=ButtonDefaults.outlinedButtonColors(containerColor=MaterialTheme.colorScheme.primaryContainer),contentPadding=PaddingValues(horizontal=18.dp,vertical=12.dp),content=content)
}
@Composable fun PageHeader(title:String,back:(()->Unit)?=null,subtitle:String="留一点时间，给音乐。"){
    if(back!=null)Row(Modifier.offset(x=(-12).dp).height(48.dp).clickable(onClick=back).padding(horizontal=12.dp),verticalAlignment=Alignment.CenterVertically){UiIcon("back",null,Modifier.size(18.dp));Spacer(Modifier.width(4.dp));Text("返回",fontSize=13.sp,color=MaterialTheme.colorScheme.primary)}
    Text(title,fontSize=26.sp,fontWeight=FontWeight.Normal,letterSpacing=.4.sp,modifier=Modifier.padding(top=if(back==null)12.dp else 6.dp,bottom=6.dp))
    if(subtitle.isNotEmpty())Text(subtitle,fontSize=12.sp,lineHeight=20.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(18.dp))
}
@Composable fun CardSurface(modifier:Modifier=Modifier,corner:Int=18,onClick:(()->Unit)?=null,selected:Boolean=false,content:@Composable ()->Unit){
    val shape=RoundedCornerShape(corner.dp)
    Surface(modifier=modifier.fillMaxWidth().padding(vertical=5.dp).then(if(onClick!=null)Modifier.clickable(onClick=onClick)else Modifier),shape=shape,border=BorderStroke(1.dp,MaterialTheme.colorScheme.outline),color=if(selected)MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,content=content)
}
@Composable fun CoverTile(modifier:Modifier=Modifier){Box(modifier.clip(RoundedCornerShape(12.dp)).background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.secondaryContainer,MaterialTheme.colorScheme.primary.copy(alpha=.65f)))),contentAlignment=Alignment.Center){UiIcon("music",null,Modifier.size(22.dp),Color.White)}}
@Composable fun SettingCard(title:String,value:String,onClick:()->Unit){CardSurface(onClick=onClick){Row(Modifier.padding(16.dp),verticalAlignment=Alignment.CenterVertically){CoverTile(Modifier.size(48.dp));Spacer(Modifier.width(12.dp));Column(Modifier.weight(1f)){Text(title,fontSize=15.sp);if(value.isNotEmpty())Text(value,fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.padding(top=4.dp))};UiIcon("back",null,Modifier.size(16.dp).rotate(180f),MaterialTheme.colorScheme.primary)}}}
@Composable fun FrostedPanel(modifier:Modifier=Modifier,content:@Composable ()->Unit){
    // The layer below this panel is the app's smooth gradient, with no sharp texture to blur.
    Surface(modifier,shape=RoundedCornerShape(20.dp),color=MaterialTheme.colorScheme.surface.copy(alpha=.48f),border=BorderStroke(1.dp,Color.White.copy(alpha=.65f))){Box(Modifier.background(Brush.verticalGradient(listOf(Color.White.copy(alpha=.18f),Color.Transparent)))){content()}}
}
@Composable fun BottomNavigation(page:String,onNavigate:(String)->Unit){
    FrostedPanel(Modifier.fillMaxWidth().padding(horizontal=10.dp).padding(bottom=4.dp)){
        Row(Modifier.fillMaxWidth().padding(horizontal=10.dp,vertical=6.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)){
            listOf(Triple("library","library","音乐库"),Triple("lists","music","歌单"),Triple("qq","music","QQ 音乐"),Triple("settings","settings","设置")).forEach{(route,glyph,label)->
                val selected=if(route=="settings")page !in listOf("library","lists","qq") else page==route
                val color=if(selected)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                Column(Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(if(selected)MaterialTheme.colorScheme.primary.copy(alpha=.10f)else Color.Transparent).clickable{onNavigate(route)}.padding(vertical=6.dp),horizontalAlignment=Alignment.CenterHorizontally){Box(Modifier.size(50.dp,28.dp),contentAlignment=Alignment.Center){UiIcon(glyph,label,Modifier.size(22.dp),color)};Spacer(Modifier.height(5.dp));Text(label,fontSize=11.sp,lineHeight=14.sp,color=color,fontWeight=if(selected)FontWeight.Medium else FontWeight.Normal)}
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun GentleSlider(value:Float,onValue:(Float)->Unit,range:ClosedFloatingPointRange<Float>,modifier:Modifier=Modifier,enabled:Boolean=true,onFinished:()->Unit={}){
    val colors=MaterialTheme.colorScheme
    Slider(value.coerceIn(range.start,range.endInclusive),onValue,onValueChangeFinished=onFinished,valueRange=range,enabled=enabled,modifier=modifier.fillMaxWidth().height(28.dp),thumb={Box(Modifier.size(14.dp).background(colors.primary,androidx.compose.foundation.shape.CircleShape))},track={state->Box(Modifier.fillMaxWidth().height(3.dp).clip(androidx.compose.foundation.shape.CircleShape).background(colors.outline.copy(alpha=.65f))){Box(Modifier.fillMaxWidth(((state.value-range.start)/(range.endInclusive-range.start)).coerceIn(0f,1f)).fillMaxHeight().background(colors.primary))}})
}
