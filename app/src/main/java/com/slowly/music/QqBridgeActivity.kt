package com.slowly.music

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.service.notification.NotificationListenerService
import android.provider.Settings
import android.widget.*

class QqSessionAccess: NotificationListenerService()

object QqBridge {
    const val PACKAGE = "com.tencent.qqmusic"
    fun controller(context: Context): MediaController? = try {
        val manager=context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
        manager.getActiveSessions(ComponentName(context,QqSessionAccess::class.java))
            .filter { it.packageName==PACKAGE }
            .sortedByDescending { it.playbackState?.state==PlaybackState.STATE_PLAYING }.firstOrNull()
    } catch (_: SecurityException) { null }
    fun pause(context: Context) { controller(context)?.transportControls?.pause() }
}

