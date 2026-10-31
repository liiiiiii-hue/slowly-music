package com.slowly.music

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import java.io.File
import java.security.MessageDigest
import com.slowly.lyrics.TrackTags

data class ArtworkPair(val cover:ImageBitmap,val background:ImageBitmap)
object ArtworkTools {
    fun decode(bytes:ByteArray):ArtworkPair? {
        val bounds=BitmapFactory.Options().apply{inJustDecodeBounds=true};BitmapFactory.decodeByteArray(bytes,0,bytes.size,bounds)
        if(bounds.outWidth<=0||bounds.outHeight<=0)return null
        var sample=1;while(maxOf(bounds.outWidth,bounds.outHeight)/sample>1024)sample*=2
        val cover=BitmapFactory.decodeByteArray(bytes,0,bytes.size,BitmapFactory.Options().apply{inSampleSize=sample})?:return null
        return fromBitmap(cover)
    }
    fun fromBitmap(cover:Bitmap):ArtworkPair {
        // Precompute a strong blur once, instead of filtering a full-screen layer on every UI frame.
        val tiny=Bitmap.createScaledBitmap(cover,96,96,true)
        var pixels=IntArray(96*96);tiny.getPixels(pixels,0,96,0,0,96,96)
        fun pass(source:IntArray,horizontal:Boolean):IntArray {
            val output=IntArray(source.size);val radius=6
            for(y in 0 until 96)for(x in 0 until 96){var r=0;var g=0;var b=0
                for(delta in -radius..radius){val xx=if(horizontal)(x+delta).coerceIn(0,95)else x;val yy=if(horizontal)y else(y+delta).coerceIn(0,95);val c=source[yy*96+xx];r+=c shr 16 and 255;g+=c shr 8 and 255;b+=c and 255}
                val n=radius*2+1;output[y*96+x]=(255 shl 24)or((r/n)shl 16)or((g/n)shl 8)or(b/n)
            };return output
        }
        repeat(3){pixels=pass(pass(pixels,true),false)}
        val soft=Bitmap.createBitmap(pixels,96,96,Bitmap.Config.ARGB_8888)
        return ArtworkPair(cover.asImageBitmap(),soft.asImageBitmap())
    }
    fun thumbnail(path:String):ImageBitmap? {
        if(path.isEmpty())return null
        val bounds=BitmapFactory.Options().apply{inJustDecodeBounds=true};BitmapFactory.decodeFile(path,bounds)
        var sample=1;while(maxOf(bounds.outWidth,bounds.outHeight)/sample>160)sample*=2
        return BitmapFactory.decodeFile(path,BitmapFactory.Options().apply{inSampleSize=sample})?.asImageBitmap()
    }
}
class TagRepository(private val context:Context,private val account:String){
    fun coverPath(url:String,bytes:ByteArray):String {
        val digest=MessageDigest.getInstance("SHA-256").digest((account+"\n"+url+"\n"+bytes.contentHashCode()).toByteArray()).joinToString(""){"%02x".format(it)}
        val folder=File(context.filesDir,"artwork").apply{mkdirs()};val file=File(folder,"$digest.jpg")
        if(!file.exists()){
            val decoded=ArtworkTools.decode(bytes)?:return ""
            file.outputStream().use{decoded.cover.asAndroidBitmap().compress(Bitmap.CompressFormat.JPEG,85,it)}
        }
        return file.absolutePath
    }
    fun read(connection:Dav,file:RemoteFile):RemoteFile {
        val tags=TrackTags.parse(connection.tagBytes(file.url))
        return file.copy(title=tags.title,artist=tags.artist,album=tags.album,artwork=tags.picture?.let{coverPath(file.url,it)}.orEmpty(),tagsRead=true)
    }
}

// MediaSession may parcel a new Bitmap object for unchanged artwork on every poll.
class ArtworkRevision {
    private var snapshot:Bitmap?=null
    private var revision=0L
    fun update(bitmap:Bitmap?):Long {
        val old=snapshot
        if(old==null&&bitmap==null)return revision
        if(old!=null&&bitmap!=null&&old.sameAs(bitmap))return revision
        snapshot=bitmap?.copy(Bitmap.Config.ARGB_8888,false)
        revision++
        return revision
    }
}
