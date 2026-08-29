@file:OptIn(androidx.media3.common.util.UnstableApi::class)

package app.anisora

import android.graphics.Color
import android.net.Uri
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView

object ExtPlayer {

    @JvmStatic
    fun play(
        app: MainActivity,
        title: String,
        url: String,
        headers: Map<String, String>,
        onEnded: Runnable,
    ) {
        val ctx = app
        val overlay = FrameLayout(ctx)
        overlay.setBackgroundColor(0xF2000000.toInt())
        overlay.isClickable = true

        val playerView = PlayerView(ctx)
        playerView.setBackgroundColor(Color.BLACK)
        playerView.useController = true

        val headerMap = HashMap<String, String>(headers)
        val factory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(20_000)
            .setReadTimeoutMs(30_000)
        if (headerMap.isNotEmpty()) factory.setDefaultRequestProperties(headerMap)

        val player = ExoPlayer.Builder(ctx)
            .setMediaSourceFactory(DefaultMediaSourceFactory(factory))
            .build()
        playerView.player = player

        val top = LinearLayout(ctx)
        top.orientation = LinearLayout.HORIZONTAL
        top.setPadding(Ui.dp(14f), Ui.dp(12f), Ui.dp(12f), Ui.dp(12f))
        top.setBackgroundColor(0x99000000.toInt())
        val t = TextView(ctx)
        t.text = title
        t.setTextColor(Color.WHITE)
        t.textSize = 14f
        t.maxLines = 1
        val tp = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        top.addView(t, tp)

        val close = TextView(ctx)
        close.text = "  ✕  "
        close.setTextColor(Color.WHITE)
        close.textSize = 18f
        top.addView(close)

        val root = LinearLayout(ctx)
        root.orientation = LinearLayout.VERTICAL
        root.addView(top, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        val pvLp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        root.addView(playerView, pvLp)
        overlay.addView(root, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

        fun dismiss() {
            try { player.release() } catch (_: Throwable) {}
            (overlay.parent as? ViewGroup)?.removeView(overlay)
        }
        close.setOnClickListener { dismiss() }

        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) {
                    dismiss()
                    onEnded.run()
                }
            }
            override fun onPlayerError(error: PlaybackException) {
                app.toast("Playback failed — ${error.errorCodeName}", "info")
            }
        })

        player.setMediaItem(MediaItem.fromUri(Uri.parse(url)))
        player.prepare()
        player.playWhenReady = true

        app.overlayRoot().addView(
            overlay,
            ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT),
        )
    }
}
