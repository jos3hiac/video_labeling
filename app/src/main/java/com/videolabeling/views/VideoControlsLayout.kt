package com.videolabeling.views

import android.content.Context
import android.util.AttributeSet
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
    val layoutVideo: LinearLayout
    val layoutPlayerView: LinearLayout
    val layoutFrame: LinearLayout
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
        layoutVideo = findViewById(R.id.layoutVideo)
        layoutPlayerView = findViewById(R.id.layoutPlayerView)
        layoutFrame = findViewById(R.id.layoutFrame)
        layoutCapture = findViewById(R.id.layoutCapture)
        layoutLabeledFrameIndex = findViewById(R.id.layoutLabeledFrameIndex)
        layoutFrameIndex = findViewById(R.id.layoutFrameIndex)

        controlCapture = findViewById(R.id.controlCapture)
        controlCapture.apply {
            setIcon(R.drawable.baseline_photo_camera_36)
            setText("Tomar captura")
        }
        controlHideVideo = findViewById(R.id.controlHideVideo)
        controlHideVideo.apply {
            setIcon(R.drawable.baseline_videocam_off_36)
            setText("Ocultar video")
        }

        inputLabeledFrameIndex = findViewById(R.id.inputLabeledFrameIndex)
        inputFrameIndex = findViewById(R.id.inputFrameIndex)
    }
    fun getVideoLayoutParams(): LayoutParams {
        return layoutVideo.layoutParams as LayoutParams
    }
    fun getPlayerViewLayoutParams(): LayoutParams {
        return layoutPlayerView.layoutParams as LayoutParams
    }
    fun getFrameLayoutParams(): LayoutParams {
        return layoutFrame.layoutParams as LayoutParams
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
        //Log.d("test orientation","$orientation")
        //super.setOrientation(orientation)
        val videoLayoutParams = getVideoLayoutParams()
        val playerViewLayoutParams = getPlayerViewLayoutParams()
        val frameLayoutParams = getFrameLayoutParams()
        val playerLayoutParams = getPlayerLayoutParams()

        if(orientation == VERTICAL){
            layoutCapture.columnCount = 4

            layoutVideo.orientation = VERTICAL
            layoutPlayerView.orientation = VERTICAL
            layoutLabeledFrameIndex.orientation = HORIZONTAL
            layoutFrameIndex.orientation = HORIZONTAL

            setVerticalSize(videoLayoutParams)
            setVerticalSize(playerViewLayoutParams)
            setVerticalSize(playerLayoutParams,350.dpToPx())
            setVerticalSize(frameLayoutParams)
        }
        else if(orientation == HORIZONTAL){
            layoutCapture.columnCount = 2

            layoutVideo.orientation = HORIZONTAL
            layoutPlayerView.orientation = HORIZONTAL
            layoutLabeledFrameIndex.orientation = VERTICAL
            layoutFrameIndex.orientation = VERTICAL

            setHorizontalSize(videoLayoutParams,LayoutParams.MATCH_PARENT)
            setHorizontalSize(playerViewLayoutParams)
            setHorizontalSize(playerLayoutParams,320.dpToPx())
            setHorizontalSize(frameLayoutParams,250.dpToPx())
        }
    }
    private fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }
    private fun setHorizontalSize(layoutParams: LayoutParams,width: Int = LayoutParams.WRAP_CONTENT){
        layoutParams.apply {
            this.width = width
            height = LayoutParams.MATCH_PARENT
        }
    }
    private fun setVerticalSize(layoutParams: LayoutParams,height: Int = LayoutParams.WRAP_CONTENT){
        layoutParams.apply {
            width = LayoutParams.MATCH_PARENT
            this.height = height
        }
    }
}