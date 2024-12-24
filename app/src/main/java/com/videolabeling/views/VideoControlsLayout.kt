package com.videolabeling.views

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.GridLayout
import android.widget.LinearLayout
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.videolabeling.R

class VideoControlsLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {
    val exoPlayer: ExoPlayer
    val playerView: PlayerView
    val layoutCapture: GridLayout
    val layoutLabeledFrameIndex: LinearLayout
    val layoutFrameIndex: LinearLayout
    val controlCapture: ButtonControl
    val controlHideVideo: ButtonControl
    val inputLabeledFrameIndex: NumberInput
    val inputFrameIndex: NumberInput

    init {
        LayoutInflater.from(context).inflate(R.layout.video_controls_layout, this, true)
        exoPlayer = ExoPlayer.Builder(context).build()
        playerView = findViewById(R.id.playerView)
        playerView.player = exoPlayer

        layoutCapture = findViewById(R.id.layoutCapture)
        layoutLabeledFrameIndex = findViewById(R.id.layoutLabeledFrameIndex)
        layoutFrameIndex = findViewById(R.id.layoutFrameIndex)

        controlCapture = layoutCapture.findViewById(R.id.controlCapture)
        controlCapture.apply {
            setIcon(R.drawable.baseline_photo_camera_36)
            setText("Tomar captura")
        }
        controlHideVideo = layoutCapture.findViewById(R.id.controlHideVideo)
        controlHideVideo.apply {
            setIcon(R.drawable.baseline_videocam_off_36)
            setText("Ocultar video")
        }

        inputLabeledFrameIndex = findViewById(R.id.inputLabeledFrameIndex)
        inputFrameIndex = findViewById(R.id.inputFrameIndex)
    }
    fun getPlayerLayoutParams(): LayoutParams {
        return playerView.layoutParams as LayoutParams
    }
    fun setVideoPosition(position: Long){
        exoPlayer.seekTo(position)
    }
    fun setVisibility(isVisible: Boolean){
        val visibility = if(isVisible) View.VISIBLE else View.GONE
        setVisibility(visibility)
    }
    override fun setOrientation(orientation: Int){
        super.setOrientation(orientation)
        layoutLabeledFrameIndex.orientation = orientation
        layoutFrameIndex.orientation = orientation
        if(orientation == HORIZONTAL){
            layoutCapture.columnCount = 4
            layoutLabeledFrameIndex.gravity = Gravity.CENTER_VERTICAL
            layoutFrameIndex.gravity = Gravity.CENTER_VERTICAL
        }
        else if(orientation == VERTICAL){
            layoutCapture.columnCount = 2
            layoutLabeledFrameIndex.gravity = Gravity.CENTER_HORIZONTAL
            layoutFrameIndex.gravity = Gravity.CENTER_HORIZONTAL
        }
    }
}