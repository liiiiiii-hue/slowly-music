package com.slowly.music

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
@RunWith(AndroidJUnit4::class)
class ArtworkToolsTest {
    @Test fun precomputesSoftBackgroundAndKeepsCoverSharp(){
        val bitmap=Bitmap.createBitmap(64,64,Bitmap.Config.ARGB_8888)
        for(y in 0..63)for(x in 0..63)bitmap.setPixel(x,y,if(x<32)android.graphics.Color.BLACK else android.graphics.Color.WHITE)
        val stream=java.io.ByteArrayOutputStream();bitmap.compress(Bitmap.CompressFormat.PNG,100,stream)
        val result=ArtworkTools.decode(stream.toByteArray())!!
        assertEquals(64,result.cover.width);assertEquals(96,result.background.width)
        assertEquals(android.graphics.Color.BLACK,result.cover.asAndroidBitmap().getPixel(20,32))
        val blurred=result.background.asAndroidBitmap().getPixel(47,48) and 255
        assertTrue(blurred in 1..254)
    }
    @Test fun unchangedParceledArtworkKeepsBackgroundRevision(){
        val tracker=ArtworkRevision()
        val bitmap=Bitmap.createBitmap(64,64,Bitmap.Config.ARGB_8888).apply{eraseColor(android.graphics.Color.RED)}
        val first=tracker.update(bitmap)
        repeat(20){assertEquals(first,tracker.update(bitmap.copy(Bitmap.Config.ARGB_8888,false)))}
        bitmap.eraseColor(android.graphics.Color.BLUE)
        val changed=tracker.update(bitmap)
        assertTrue(changed>first)
        assertEquals(changed,tracker.update(bitmap.copy(Bitmap.Config.ARGB_8888,false)))
        val cleared=tracker.update(null)
        assertTrue(cleared>changed)
        assertEquals(cleared,tracker.update(null))
    }
}
