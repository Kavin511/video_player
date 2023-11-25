package com.devstudioworks.videoplayer

import android.app.PictureInPictureParams
import android.content.Intent
import android.content.pm.PackageManager.FEATURE_PICTURE_IN_PICTURE
import android.media.browse.MediaBrowser
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.ViewModelProvider
import androidx.media3.common.AudioAttributes
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.devstudioworks.videoplayer.databinding.ActivityMainBinding
import com.devstudioworks.videoplayer.preferences.VideoPreferences
import com.devstudioworks.videoplayer.utils.SimpleGesture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding
    lateinit var simpleGestureListener: ScaleGestureDetector
    lateinit var videoPreferences: VideoPreferences
    val coroutineScope = CoroutineScope(Dispatchers.Main)
    lateinit var playerView: PlayerView
    var player: ExoPlayer? = null
    lateinit var mediaItem: MediaItem
    lateinit var playerViewModel: PlayerViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        playerViewModel = ViewModelProvider(this).get(PlayerViewModel::class.java)
        setContentView(binding.root)
        val launchIntent = intent
        playerView = binding.player
        binding.selectVideoToPlay.visibility = View.VISIBLE
        binding.selectVideoToPlay.setOnClickListener {
            selectVideoFileToPlay()
        }
        videoPreferences = VideoPreferences(applicationContext)

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

    override fun onStart() {
        super.onStart()
        coroutineScope.launch {
            videoPreferences.getVideoUri.collectLatest {
                if (it?.isNotEmpty() == true) {
                    it.toUri().initialiseVideoFileToPlay()
                }
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
                    if (this != null) {
                        coroutineScope.launch {
                            videoPreferences.setVideoUri(this@apply.toString())
                        }
                    }
                    playerViewModel.selectedVideoUri.value = this
                    initialiseVideoFileToPlay()
                }
            }
        }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        super.onTouchEvent(event)
        event.let { simpleGestureListener.onTouchEvent(it) }
        return true
    }

    override fun onStop() {
        super.onStop()
        setScreenOn(keepScreenOn = false)
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.launch {
            videoPreferences.setVideoUri("")
        }//todo uri stored in db is not working when reloaded. Need to fix that and remove this reset
    }

    override fun onPause() {
        super.onPause()
        releasePlayer()
        enterPipMode()
    }

    private fun enterPipMode() {
        if (applicationContext.packageManager.hasSystemFeature(FEATURE_PICTURE_IN_PICTURE)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val builder = PictureInPictureParams.Builder()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    builder.setAutoEnterEnabled(true)
                }
                enterPictureInPictureMode(builder.build())
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                enterPictureInPictureMode()
            } else {
                releasePlayer()
            }
        }
    }

    private fun setScreenOn(keepScreenOn: Boolean) {
        if (keepScreenOn) {
            window.addFlags(FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(FLAG_KEEP_SCREEN_ON)
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        releasePlayer()
    }

    private fun Uri?.initialiseVideoFileToPlay() {
        releasePlayer()
        if (player == null) {
            player = ExoPlayer.Builder(applicationContext)
                .build()
            mediaItem = MediaItem.Builder().setUri(this)
                .setMediaMetadata(MediaMetadata.Builder().setTitle(title).build()).build()
            simpleGestureListener =
                ScaleGestureDetector(this@MainActivity, SimpleGesture(playerView))
            player?.setMediaItem(mediaItem, 0)
            player?.setAudioAttributes(AudioAttributes.DEFAULT,  /* handleAudioFocus= */true)
            player?.playWhenReady = true
            playerView.player = player
        }
        binding.selectVideoToPlay.visibility = GONE
        setScreenOn(keepScreenOn = true)
        hideSystemBars()
    }

    private fun releasePlayer() {
        if (player != null) {
            player?.release()
            player?.stop()
            player?.clearMediaItems()
            player = null
            binding.selectVideoToPlay.visibility = VISIBLE
        }
    }

    private fun hideSystemBars() {
        val windowInsetsController =
            WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
    }

}