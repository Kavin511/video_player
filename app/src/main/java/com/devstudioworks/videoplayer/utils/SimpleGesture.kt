package com.devstudioworks.videoplayer.utils

import android.view.ScaleGestureDetector
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView

class SimpleGesture(private val player: PlayerView) : ScaleGestureDetector.SimpleOnScaleGestureListener() {
    private var scaleFactor = 0f
    override fun onScale(detector: ScaleGestureDetector): Boolean {
        scaleFactor = detector.scaleFactor
        return super.onScale(detector)
    }

    @OptIn(UnstableApi::class) override fun onScaleEnd(detector: ScaleGestureDetector) {
        if (scaleFactor > 1) {
            player.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
        } else {
            player.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
        }
    }

}