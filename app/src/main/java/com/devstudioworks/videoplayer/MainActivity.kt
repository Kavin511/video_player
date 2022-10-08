package com.devstudioworks.videoplayer

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.ui.PlayerView

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val launchIntent = intent
        val type = launchIntent.type
        launchIntent.data.apply {
            if (this == null) {
                Toast.makeText(
                    applicationContext,
                    "Open video from file chooser to play!",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                val mediaItemBuilder = MediaItem.Builder()
                    .setUri(this)
                    .setMimeType(type)
                    .build()
                val playerBuilder: ExoPlayer = ExoPlayer.Builder(applicationContext)
                    .build()
                playerBuilder.setMediaItem(mediaItemBuilder, 0)
                val pl = findViewById<PlayerView>(R.id.player)
                pl.player = playerBuilder
                (pl.player as ExoPlayer).prepare()
                (pl.player as ExoPlayer).playWhenReady = true
                (pl.player as ExoPlayer).setPlaybackSpeed(2F)
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            }
        }
    }

}