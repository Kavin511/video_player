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
import com.devstudioworks.videoplayer.databinding.ActivityMainBinding
import com.devstudioworks.videoplayer.preferences.VideoPreferences
import com.devstudioworks.videoplayer.utils.SimpleGesture
import com.google.android.exoplayer2.C.VIDEO_SCALING_MODE_SCALE_TO_FIT
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.ui.PlayerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

//@HiltAndroidApp
//class VideoPlayer : Application()

//@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding
    lateinit var simpleGestureListener: ScaleGestureDetector
    lateinit var videoPreferences: VideoPreferences
    val coroutineScope = CoroutineScope(Dispatchers.Main)
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
        event?.let { simpleGestureListener.onTouchEvent(it) }
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

    private fun Uri?.initialiseVideoFileToPlay() {
        releasePlayer()
        val mediaItemBuilder = MediaItem.Builder()
            .setUri(this)
            .build()
        val playerBuilder: ExoPlayer = ExoPlayer.Builder(applicationContext)
            .setVideoScalingMode(VIDEO_SCALING_MODE_SCALE_TO_FIT)
            .build()
        playerBuilder.setMediaItem(mediaItemBuilder, 0)
        val playerView = binding.player
        simpleGestureListener = ScaleGestureDetector(this@MainActivity, SimpleGesture(playerView))
        playerView.player = playerBuilder
        binding.selectVideoToPlay.visibility = GONE
        playerView.setControllerHideDuringAds(true)
        playerView.controllerHideOnTouch = true
        playerView.setKeepContentOnPlayerReset(true)
        player?.play()
        setScreenOn(keepScreenOn = true)
        hideSystemBars()
    }

    private fun releasePlayer() {
        if (binding.player.player != null) {
            binding.player.player?.release()
            binding.player.player?.stop()
            player?.clearMediaItems()
            binding.player.player = null
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