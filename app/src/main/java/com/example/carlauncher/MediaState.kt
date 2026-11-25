package com.example.carlauncher

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.view.KeyEvent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationManagerCompat

class MediaState(private val ctx: Context) {
    var title by mutableStateOf("Nothing playing")
    var artist by mutableStateOf("Play something in a music app")
    var playing by mutableStateOf(false)
    var position by mutableLongStateOf(0L)
    var duration by mutableLongStateOf(0L)
    var art by mutableStateOf<Bitmap?>(null)
    var access by mutableStateOf(false)

    private var started = false
    private var controller: MediaController? = null
    private val msm = ctx.getSystemService(MediaSessionManager::class.java)
    private val audio = ctx.getSystemService(AudioManager::class.java)
    private val cn = ComponentName(ctx, MediaListenerService::class.java)

    private val callback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) = update()
        override fun onPlaybackStateChanged(state: PlaybackState?) = update()
    }

    private val sessionsListener =
        MediaSessionManager.OnActiveSessionsChangedListener { attach(it) }

    fun hasAccess(): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(ctx).contains(ctx.packageName)

    fun start() {
        access = hasAccess()
        if (!access || started) return
        try {
            msm.addOnActiveSessionsChangedListener(sessionsListener, cn)
            started = true
            attach(msm.getActiveSessions(cn))
        } catch (_: SecurityException) { }
    }

    fun stop() {
        if (started) {
            try { msm.removeOnActiveSessionsChangedListener(sessionsListener) } catch (_: Exception) { }
        }
        controller?.unregisterCallback(callback)
        started = false
    }

    private fun attach(list: List<MediaController>?) {
        controller?.unregisterCallback(callback)
        controller = list?.firstOrNull()
        controller?.registerCallback(callback)
        update()
    }

    private fun update() {
        val c = controller
        if (c == null) {
            title = "Nothing playing"
            artist = if (access) "Play something in a music app" else "Tap here to enable music info"
            playing = false
            art = null
            return
        }
        val m = c.metadata
        title = m?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: "Unknown"
        artist = m?.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: ""
        duration = m?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L
        art = m?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: m?.getBitmap(MediaMetadata.METADATA_KEY_ART)
        playing = c.playbackState?.state == PlaybackState.STATE_PLAYING
        position = c.playbackState?.position ?: 0L
    }

    /** Called every second from the UI. Also reconnects after access is granted. */
    fun tick() {
        if (!started) {
            start()
            if (!access) { update(); return }
        } else if (controller == null) {
            try { attach(msm.getActiveSessions(cn)) } catch (_: Exception) { }
        }
        val s = controller?.playbackState ?: return
        position = s.position.coerceAtLeast(0L)
        playing = s.state == PlaybackState.STATE_PLAYING
    }

    private fun sendKey(code: Int) {
        audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, code))
        audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, code))
    }

    fun toggle() {
        val t = controller?.transportControls
        if (t != null) { if (playing) t.pause() else t.play() }
        else sendKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
    }

    fun next() {
        val t = controller?.transportControls
        if (t != null) t.skipToNext() else sendKey(KeyEvent.KEYCODE_MEDIA_NEXT)
    }

    fun previous() {
        val t = controller?.transportControls
        if (t != null) t.skipToPrevious() else sendKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
    }
}