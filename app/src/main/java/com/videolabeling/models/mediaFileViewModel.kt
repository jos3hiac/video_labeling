package com.videolabeling.models

import android.graphics.Color
import android.graphics.Matrix
import android.graphics.PointF
import android.view.Menu
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MediaFileViewModel : ViewModel() {
    val files: MutableLiveData<List<MediaFile>> = MutableLiveData()
    //val files: MutableLiveData<MutableMap<Uri, MediaFile>> = MutableLiveData()
    var file: MediaFile? = null
    var fileIndex = 0

    var menu: Menu? = null

    var fileType = "image"
    var isVideoVisible = true
    var videoPosition = 0L
    var frameIndex = 0
    var labeledFrameIndex: Int? = null
    var imageInfo: ImageInfo? = null

    var actionType = "edit"
    private var shapeType = ""
    var shape: Shape? = null
    var selectedShape:Shape? = null
    var selectedPointIndex:Int? = null
    var selectedItemId: Int? = null

    val COLORS = generateColors(256)
    private val defaultColor = Color.rgb(173, 255, 47)
    //val GREEN1 = Color.parseColor("#FFA500")

    fun getColorByLabel(label: String,labels: List<String>): Int {
        val index = labels.indexOf(label)
        return if(index != -1) COLORS[(index) % COLORS.size] else defaultColor
    }
    fun setMediaFileAndIndex(file: MediaFile, index: Int) {
        this.file = file
        this.fileIndex = index
    }
    fun setShapeType(shapeType: String){
        this.shapeType = shapeType
        actionType = "create"
    }
    fun createShape(matrix: Matrix): Shape? {
        shape = Shape.create(shapeType,matrix)
        shape!!.vertexSide.value = 50f
        shape!!.setColorAndAlpha(defaultColor)//0,128,0 124,252,0
        return shape
    }
    private fun addPoint(point: PointF){
        shape!!.addPoint(point)
    }
    fun addPointAndValidateShape(point: PointF,addShape: Boolean = false): Boolean {
        when(shapeType){
            Shape.RECT -> {
                addPoint(point)
                if(shape!!.points.size == 2){
                    return true
                }
            }
            Shape.POLYGON -> {
                addPoint(point)
                if(addShape && shape!!.points.size > 2){
                    return true
                }
            }
        }
        return false
    }
    fun addShape(){
        imageInfo!!.shapes.add(shape!!)
        removeShape()
    }
    fun getLabel(): String {
        return selectedShape!!.getLabel()
    }
    fun initImageInfo(imageInfo: ImageInfo,labels: List<String>){
        for(shape in imageInfo.shapes){
            shape.vertexSide.value = 50f
            updateShapeColor(shape,labels)
        }
        this.imageInfo = imageInfo
    }
    fun isSelectedShapeNull(): Boolean {
        return selectedShape == null
    }
    fun isSelectedShapeNotNull(): Boolean {
        return selectedShape != null
    }
    fun isShapeNull(): Boolean {
        return shape == null
    }
    fun isShapeNotNull(): Boolean {
        return shape != null
    }
    fun moveSelectedShape(dx: Float,dy: Float){
        selectedShape?.let { shape ->
            selectedPointIndex?.let {
                shape.movePoint(it,dx,dy)
            } ?:run {
                shape.move(dx,dy)
            }
        }
    }
    fun undoLastPoint(){
        return if(shape!!.points.size == 1){
            removeShape()
        }
        else{
            removePoint(shape!!.points.size-1)
        }
    }
    fun removePoint(index: Int){
        shape!!.removePoint(index)
    }
    fun removeShape(){
        shape = null
    }
    fun removeSelectedShape(){
        imageInfo!!.shapes.remove(selectedShape)
        resetSelectedShape()
    }
    fun resetSelectedShape(): Boolean {
        return if (selectedShape != null) {
            selectedShape!!.setSelected(false,selectedPointIndex)
            selectedShape = null
            selectedPointIndex = null
            true
        } else false
    }
    fun selectShape(point: PointF,includeVertices: Boolean = true): Boolean {
        resetSelectedShape()
        for(shape in imageInfo!!.shapes.reversed()){
            val pair = shape.contains(point,includeVertices)
            if(pair.first){
                selectedShape = shape.setSelected(true,pair.second)
                selectedPointIndex = pair.second
                return true
            }
        }
        return false
    }
    fun selectShape(shape: Shape){
        resetSelectedShape()
        selectedShape = shape.setSelected(true,null)
        selectedPointIndex = null
    }
    fun setLabel(label: String,labels: List<String>){
        shape!!.setLabel(label)
        updateShapeColor(shape!!,labels)
    }
    fun editLabel(label: String,labels: List<String>){
        selectedShape!!.setLabel(label)
        updateShapeColor(selectedShape!!,labels)
    }
    fun updateSelectedShape(point: PointF,includeVertices: Boolean = true): Boolean {
        if (selectedShape != null) {
            selectedShape!!.setSelected(false,selectedPointIndex)
            val pair = selectedShape!!.contains(point,includeVertices)
            if(pair.first){
                selectedShape = selectedShape!!.setSelected(true,pair.second)
                selectedPointIndex = pair.second
                return true
            }
        }
        resetSelectedShape()
        return false
    }
    /*fun updateShapeColors(imageInfo: ImageInfo,labels: List<String>){
        for(shape in imageInfo.shapes) updateShapeColor(shape,labels)
    }*/
    private fun updateShapeColor(shape: Shape,labels: List<String>){
        shape.setColor(getColorByLabel(shape.getLabel(),labels))
    }
}