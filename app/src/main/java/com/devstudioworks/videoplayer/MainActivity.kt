package com.devstudioworks.videoplayer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener
import android.view.View
import android.view.View.GONE
import android.view.WindowManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.devstudioworks.videoplayer.databinding.ActivityMainBinding
import com.google.android.exoplayer2.C.VIDEO_SCALING_MODE_SCALE_TO_FIT
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.ui.PlayerView


class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding
    lateinit var simpleGestureListener: ScaleGestureDetector
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val launchIntent = intent
        binding.selectVideoToPlay.visibility = View.VISIBLE
        binding.selectVideoToPlay.setOnClickListener {
            selectVideoFileToPlay()
        }
        launchIntent.data.apply {
            if (this != null) {
                binding.selectVideoToPlay.visibility = GONE
                this.initialiseVideoFileToPlay()
            }
        }
    }

    private fun selectVideoFileToPlay() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
        activityResultLauncher.launch(intent)
    }

    private val activityResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                it.data?.data.apply {
                    initialiseVideoFileToPlay()
                }
            }
        }


    override fun onTouchEvent(event: MotionEvent?): Boolean {
        super.onTouchEvent(event)
        simpleGestureListener.onTouchEvent(event)
        return true
    }

    private fun Uri?.initialiseVideoFileToPlay() {
        val mediaItemBuilder = MediaItem.Builder()
            .setUri(this)
            .build()
        val playerBuilder: ExoPlayer = ExoPlayer.Builder(applicationContext)
            .setVideoScalingMode(VIDEO_SCALING_MODE_SCALE_TO_FIT)
            .build()
        playerBuilder.setMediaItem(mediaItemBuilder, 0)
        val pl = binding.player
        simpleGestureListener = ScaleGestureDetector(this@MainActivity, SimpleGesture(pl))
        pl.player = playerBuilder
        binding.selectVideoToPlay.visibility = GONE
        pl.setControllerHideDuringAds(true)
        pl.controllerHideOnTouch = true
        pl.setKeepContentOnPlayerReset(true)
        (pl.player as ExoPlayer).playWhenReady = true
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        hideSystemBars()
    }

    private fun hideSystemBars() {
        val windowInsetsController =
            WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
    }

}

class SimpleGesture(private val player: PlayerView) : SimpleOnScaleGestureListener() {
    private var scaleFactor = 0f
    override fun onScale(detector: ScaleGestureDetector?): Boolean {
        scaleFactor = detector?.scaleFactor ?: 0f
        return super.onScale(detector)
    }

    override fun onScaleBegin(detector: ScaleGestureDetector?): Boolean {
        return super.onScaleBegin(detector)
    }

    override fun onScaleEnd(detector: ScaleGestureDetector?) {
        if (scaleFactor > 1) {
            player.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
        } else {
            player.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
        }
    }

}