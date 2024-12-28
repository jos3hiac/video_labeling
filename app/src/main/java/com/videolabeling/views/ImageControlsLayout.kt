package com.videolabeling.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.GridLayout
import android.widget.LinearLayout
import com.videolabeling.R

class ImageControlsLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {
    val layoutImage: LinearLayout
    val zoomImageView: ZoomImageView
    val layoutShape: GridLayout
    val controlCreateRect: ButtonControl
    val controlCreatePolygon: ButtonControl
    val controlEditShape: ButtonControl
    val controlShowVideo: ButtonControl
    val controlUndoLastPoint: ButtonControl
    val controlRemoveShape: ButtonControl
    val controlEditLabel: ButtonControl

    val labelDialog: LabelDialog
    var selectedButtonControl: ButtonControl? = null
    //var onButtonCreateRectClick: (() -> Unit)? = null
    //var onButtonCreatePolygonClick: (() -> Unit)? = null
    //var onButtonEditShapeClick: (() -> Unit)? = null

    init {
        LayoutInflater.from(context).inflate(R.layout.image_controls_layout, this, true)
        layoutImage = findViewById(R.id.layoutImage)
        zoomImageView = findViewById(R.id.zoomImageView)
        layoutShape = findViewById(R.id.layoutShape)
        labelDialog = LabelDialog(context)

        controlCreateRect = findViewById(R.id.controlCreateRect)
        controlCreatePolygon = findViewById(R.id.controlCreatePolygon)
        controlEditShape = findViewById(R.id.controlEditShape)
        controlShowVideo = findViewById(R.id.controlShowVideo)
        controlUndoLastPoint = findViewById(R.id.controlUndoLastPoint)
        controlRemoveShape = findViewById(R.id.controlRemoveShape)
        controlEditLabel = findViewById(R.id.controlEditLabel)

        controlCreateRect.apply {
            setIcon(R.drawable.baseline_rectangle_36)
            setText("Crear rectángulo")
        }
        controlCreatePolygon.apply {
            setIcon(R.drawable.baseline_hexagon_36)
            setText("Crear polígono")
        }
        controlEditShape.apply {
            setIcon(R.drawable.baseline_edit_36)
            setText("Editar forma")
        }
        controlShowVideo.apply {
            setIcon(R.drawable.baseline_videocam_36)
            setText("Mostrar video")
        }
        controlUndoLastPoint.apply {
            setIcon(R.drawable.baseline_undo_36)
            setText("Deshacer último punto")
            setEnabled(false)
        }
        controlRemoveShape.apply {
            setIcon(R.drawable.baseline_close_36)
            setText("Eliminar forma")
            setEnabled(false)
        }
        controlEditLabel.apply {
            setIcon(R.drawable.baseline_edit_36)
            setText("Editar etiqueta")
            setEnabled(false)
        }

    }
    fun getImageLayoutParams(): LayoutParams {
        return zoomImageView.layoutParams as LayoutParams
    }
    fun getZoomImageViewLayoutParams(): LayoutParams {
         return zoomImageView.layoutParams as LayoutParams
    }

    /*fun isDefaultText(buttonControl: ButtonControl?): Boolean {
        if(buttonControl == controlCreateRect){
            return buttonControl.getText() == "Crear rectángulo"
        }
        else if(buttonControl == controlCreatePolygon){
            return buttonControl.getText() == "Crear polígono"
        }
        return true
    }*/
    fun handleSelectionButtonControl(buttonControl: ButtonControl){
        //toogleSelectedButtonControl()
        setSelectedButtonEnabled(true)
        selectedButtonControl = buttonControl
    }
    /*fun resetButtonControl(buttonControl: ButtonControl?){
        if(!isDefaultText(buttonControl)) toogleButtonControl(buttonControl)
        setSelectedButtonEnabled(true)
    }*/
    fun resetSelectedButtonControl(){
        //resetButtonControl(selectedButtonControl)
        setSelectedButtonEnabled(true)
        selectedButtonControl = null
    }
    fun setSelectedButtonEnabled(enabled: Boolean){
        selectedButtonControl?.setEnabled(enabled)
    }
    fun setVisibility(isVisible: Boolean){
        val visibility = if(isVisible) View.VISIBLE else View.GONE
        setVisibility(visibility)
        //zoomImageView.visibility = visibility
    }
    /*fun toogleButtonControl(buttonControl: ButtonControl?){
        if(buttonControl == controlCreateRect){
            buttonControl.setText(if(buttonControl.getText() == "Crear rectángulo") "Quitar rectángulo" else "Crear rectángulo")
        }
        else if(buttonControl == controlCreatePolygon){
            buttonControl.setText(if(buttonControl.getText() == "Crear polígono") "Quitar polígono" else "Crear polígono")
        }
    }
    fun toogleSelectedButtonControl(){
        selectedButtonControl?.let(::toogleButtonControl)
    }*/
    override fun setOrientation(orientation: Int){
        super.setOrientation(orientation)
        val imageLayoutParams = getImageLayoutParams()

        if(orientation == VERTICAL){
            layoutImage.orientation = VERTICAL
            layoutShape.columnCount = 4
            setVerticalSize(imageLayoutParams)
        }
        else if(orientation == HORIZONTAL){
            layoutImage.orientation = HORIZONTAL
            layoutShape.columnCount = 3
            setHorizontalSize(imageLayoutParams,LayoutParams.MATCH_PARENT)
        }
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