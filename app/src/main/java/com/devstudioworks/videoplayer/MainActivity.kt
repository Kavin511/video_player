package com.devstudioworks.videoplayer

import android.app.PictureInPictureParams
import android.content.Intent
import android.content.pm.PackageManager.FEATURE_PICTURE_IN_PICTURE
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.ViewModelProvider
import com.devstudioworks.videoplayer.databinding.ActivityMainBinding
import com.google.android.exoplayer2.C.VIDEO_SCALING_MODE_SCALE_TO_FIT
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.ui.PlayerView


class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding
    lateinit var simpleGestureListener: ScaleGestureDetector
    lateinit var playerView: PlayerView
    var player: ExoPlayer? = null
    lateinit var playerViewModel: PlayerViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        playerViewModel = ViewModelProvider(this).get(PlayerViewModel::class.java)
        setContentView(binding.root)
        val launchIntent = intent
        playerView = binding.player
        player = playerView.player as ExoPlayer?
        binding.selectVideoToPlay.visibility = View.VISIBLE
        binding.selectVideoToPlay.setOnClickListener {
            selectVideoFileToPlay()
        }
        launchIntent.data.apply {
            if (this != null) {
                binding.selectVideoToPlay.visibility = GONE
                playerViewModel.selectedVideoUri.value = this
            }
        }
        playerViewModel.selectedVideoUri.observe(this) {
            it?.initialiseVideoFileToPlay()
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
                    playerViewModel.selectedVideoUri.value = this
                    this?.initialiseVideoFileToPlay()
                }
            }
        }


    override fun onTouchEvent(event: MotionEvent): Boolean {
        super.onTouchEvent(event)
        simpleGestureListener.onTouchEvent(event)
        return true
    }

    override fun onStop() {
        super.onStop()
        binding.player.onPause()
        setScreenOn(keepScreenOn = false)
    }

    override fun onPause() {
        super.onPause()
        enterPipMode()
    }

    private fun enterPipMode() {
        if (applicationContext.packageManager.hasSystemFeature(FEATURE_PICTURE_IN_PICTURE)) {
            val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                PictureInPictureParams.Builder()
            } else {
                null
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                builder?.setAutoEnterEnabled(true)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                builder?.build()
            }
        } else {
            resetPlayer()
        }
    }

    private fun setScreenOn(keepScreenOn: Boolean) {
        if (keepScreenOn) {
            window.addFlags(FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(FLAG_KEEP_SCREEN_ON)
        }
    }

    private fun Uri?.initialiseVideoFileToPlay() {
        resetPlayer()
        val mediaItemBuilder = MediaItem.Builder()
            .setUri(this)
            .build()
        val playerBuilder: ExoPlayer = ExoPlayer.Builder(applicationContext)
            .setVideoScalingMode(VIDEO_SCALING_MODE_SCALE_TO_FIT)
            .build()
        playerBuilder.setMediaItem(mediaItemBuilder, 0)
        simpleGestureListener = ScaleGestureDetector(this@MainActivity, SimpleGesture(playerView))
        playerView.player = playerBuilder
        binding.selectVideoToPlay.visibility = GONE
        playerView.setControllerHideDuringAds(true)
        playerView.controllerHideOnTouch = true
        playerView.setKeepContentOnPlayerReset(true)
        player?.playWhenReady = true
        setScreenOn(keepScreenOn = true)
        hideSystemBars()
    }

    private fun resetPlayer() {
        player?.stop()
        player?.clearMediaItems()
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
    override fun onScale(detector: ScaleGestureDetector): Boolean {
        scaleFactor = detector?.scaleFactor ?: 0f
        return super.onScale(detector)
    }

    override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
        return super.onScaleBegin(detector)
    }

    override fun onScaleEnd(detector: ScaleGestureDetector) {
        if (scaleFactor > 1) {
            player.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
        } else {
            player.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
        }
    }

}