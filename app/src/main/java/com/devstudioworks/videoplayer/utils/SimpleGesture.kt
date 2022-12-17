package com.devstudioworks.videoplayer.utils

import android.view.ScaleGestureDetector
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.ui.PlayerView

class SimpleGesture(private val player: PlayerView) : ScaleGestureDetector.SimpleOnScaleGestureListener() {
    private var scaleFactor = 0f
    override fun onScale(detector: ScaleGestureDetector): Boolean {
        scaleFactor = detector.scaleFactor
        return super.onScale(detector)
    }

    override fun onScaleEnd(detector: ScaleGestureDetector) {
        if (scaleFactor > 1) {
            player.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
        } else {
            player.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
        }
    }

}