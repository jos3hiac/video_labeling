package com.videolabeling.models

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.PointF
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.Locale

abstract class MediaFile(
    val context: Context,
    val uri: Uri,
    val name: String,
    val matrix: Matrix,
    val type: String
){
    var thumbnail: Bitmap? = null
    var areShapesLoaded = false
    var onThumbnailLoaded: ((Bitmap?) -> Unit)? = null
    val nameWithoutExtension = File(name).nameWithoutExtension

    fun loadThumbnailAsync() {
        CoroutineScope(Dispatchers.IO).launch {
            val bitmap = when (this@MediaFile) {
                is ImageFile -> loadImage()?.let { Bitmap.createBitmap(it) }
                is VideoFile -> getFrameByMilliseconds(0)
                else -> null
            }
            withContext(Dispatchers.Main) {
                thumbnail = bitmap
                onThumbnailLoaded?.invoke(bitmap)
            }
        }
    }
    open fun loadShapes(annotationDir: String){
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        other as MediaFile
        return uri == other.uri && name == other.name && type == other.type
    }
    // Sobrescribir hashCode() para garantizar que objetos iguales tengan el mismo hash code
    override fun hashCode(): Int {
        return 31 * uri.hashCode() + 31 * name.hashCode() + type.hashCode()
    }
}

class ImageFile(context: Context,uri: Uri,name: String,matrix: Matrix): MediaFile(context,uri,name,matrix,"image"){
    var image: Bitmap? = null
    private var imageInfo: ImageInfo? = null
    fun loadImage(): Bitmap? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    fun getImageInfo(): ImageInfo? {
        if(image == null) image = loadImage()
        return imageInfo?.also {
            if (it.image == null) it.image = image
        } ?: if (image != null) {
            imageInfo = ImageInfo(this,image!!.width,image!!.height,0,image)
            imageInfo
        } else null
    }
    override fun loadShapes(annotationDir: String) {
        try {
            imageInfo = null
            val file = File(annotationDir, "$nameWithoutExtension.json")
            val imageData:ImageData = Json.decodeFromString(file.readText())
            imageInfo = ImageInfo(this,imageData)
            areShapesLoaded = true
        } catch (e: Exception) {
            e.printStackTrace()
            areShapesLoaded = false
        }
    }
    fun saveShapes(annotationDir: String){
        imageInfo?.save(File(annotationDir,"$nameWithoutExtension.json").path)
    }
}
class VideoFile(context: Context,uri: Uri,name: String,matrix: Matrix): MediaFile(context,uri,name,matrix,"video"){
    var fps: Float? = null
    var duration: Float? = null
    var frameCount: Int? = null
    var videoInfo = VideoInfo(this)
    /*private fun getDirectory(annotationDir: String = ""): String {
        return File(annotationDir, name).nameWithoutExtension
    }*/
    fun getFrameIndexByMilliseconds(time: Long): Int? {
        //Log.e("test frameIndex","$time ${((time / 1_000.0) * fps!!)} ${((time / 1_000.0) * fps!!).toInt()}")
        return fps?.let {
            val frame = (time / 1_000.0) * it
            val frameIndex = frame.toInt()
            frameIndex//if (frame == frameIndex.toDouble()) frameIndex - 1 else frameIndex
        }
    }
    fun getFrameByFrameIndex(frameIndex: Int): Bitmap? {
        return getMillisecondsByFrameIndex(frameIndex)?.let(::getFrameByMilliseconds)
    }
    fun getMillisecondsByFrameIndex(frameIndex: Int): Long? {
        return fps?.let { (frameIndex * (1000 / it)).toLong()}
    }
    fun getFrameByMilliseconds(time: Long): Bitmap? {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            //val added = (fps?.let{1000/it} ?:run {0}).toLong()
            return retriever.getFrameAtTime(time*1000, MediaMetadataRetriever.OPTION_CLOSEST)
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        } finally {
            retriever.release()
        }
    }
    fun getImageInfo(frameIndex: Int): ImageInfo? {
        return videoInfo.images[frameIndex]?.also { imageInfo ->
                if (imageInfo.image == null) {
                    imageInfo.image = getFrameByFrameIndex(frameIndex)
                }
            } ?: getFrameByFrameIndex(frameIndex)?.let { image ->
                val imageInfo = ImageInfo(this,image.width,image.height,frameIndex,image)
                videoInfo.addImageInfo(imageInfo)
                imageInfo
            }
    }
    override fun loadShapes(annotationDir: String) {
        try {
            videoInfo.clear()
            val dir = getVideoJsonFile(annotationDir)
            dir.listFiles()?.filter { it.extension == "json" }?.sortedBy { file -> file.name }?.forEach { file ->
                val imageData:ImageData = Json.decodeFromString(file.readText())
                videoInfo.addImageInfo(ImageInfo(this,imageData))
            }
            /*directory.listFiles()?.let { files ->
                for (file in files.sortedBy { file -> file.name }) {
                    val imageData:ImageData = Json.decofunromString(file.readText())
                }
            }*/
            areShapesLoaded = true
        } catch (e: Exception) {
            e.printStackTrace()
            areShapesLoaded = false
        }
    }
    fun saveLabeledFrames(annotationDir: String){
        videoInfo.save(getVideoJsonFile(annotationDir).path)
    }
    fun saveShapes(annotationDir: String,imageInfo: ImageInfo){
        val videoJsonPath = File(annotationDir,nameWithoutExtension).path
        videoInfo.saveImageInfo(videoJsonPath,imageInfo)
    }
    private fun getVideoJsonFile(annotationDir: String): File {
        return File(annotationDir, nameWithoutExtension)
    }
}

class ImageInfo(
    val mediaFile: MediaFile,
    val width: Int,
    val height: Int,
    val frameIndex: Int,
    var image: Bitmap? = null
){
    constructor(mediaFile: MediaFile,imageData: ImageData) : this(
        mediaFile,
        imageData.width,
        imageData.height,
        imageData.frameIndex
    ){
        for(shapeData in imageData.shapes){
            val shape:Shape? = Shape.create(shapeData.type,mediaFile.matrix)
            shape?.let { newShape ->
                for(points in shapeData.points){
                    if(points.size == 2){
                        val x = points[0]
                        val y = points[1]
                        newShape.addPoint(PointF(x,y))
                    }
                }
                newShape.setAlpha()
                newShape.setLabel(shapeData.label)
                shapes.add(newShape)
            }
        }
    }
    val shapes = mutableListOf<Shape>()

    fun save(imageJsonPath: String){
        Log.d("test imageJsonPath",imageJsonPath)
        val file = File(imageJsonPath)
        file.parentFile?.mkdirs()
        file.writeText(toJsonString())
        saveImage(imageJsonPath)
    }
    /*fun setMatrix(matrix: Matrix): ImageInfo{
        for (shape in shapes) shape.setMatrix(matrix)
        return this
    }*/
    fun toJsonString(): String {
        val shapeDataList = mutableListOf<ShapeData>()
        for(shape in shapes){
            val type = shape.type
            shapeDataList.add(ShapeData(type,shape.getLabel(),shape.points.map { point -> listOf(point.x,point.y) }))
        }
        val imageNameWithoutExtension = when(mediaFile){
            is ImageFile -> mediaFile.nameWithoutExtension
            is VideoFile -> mediaFile.videoInfo.getImageNameWithoutExtension(frameIndex)
            else -> ""
        }
        val imageData = ImageData("${imageNameWithoutExtension}.jpeg",width,height,frameIndex,shapeDataList)
        val json = Json {
            prettyPrint = true
            prettyPrintIndent = "  "
        }
        return json.encodeToString(ImageData.serializer(),imageData)
    }
    private fun saveImage(imageJsonPath: String){
        image?.let {
            val filePath = imageJsonPath.replaceAfterLast(".", "jpeg")
            if(!File(filePath).exists()) saveBitmapAsJpeg(it,filePath)
        }
    }
    private fun bitmapToString(bitmap: Bitmap?): String {
        return if(bitmap != null){
            val byteArrayOutputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
            val byteArray = byteArrayOutputStream.toByteArray()
            Base64.encodeToString(byteArray, Base64.DEFAULT)
        } else ""
    }
    private fun saveBitmapAsJpeg(bitmap: Bitmap,filePath: String): Boolean {
        return try {
            val file = File(filePath)
            val outputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            outputStream.flush()
            outputStream.close()
            true
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }
    }
}
class VideoInfo(
    val videoFile: VideoFile
) {
    val images = mutableMapOf<Int,ImageInfo>()
    var sortedImages = listOf<ImageInfo>()

    fun addImageInfo(imageInfo: ImageInfo){
        images[imageInfo.frameIndex] = imageInfo
        updateSortedImages()
    }
    fun clear(){
        images.clear()
        sortedImages = listOf()
    }
    fun findIndex(frameIndex: Int): Int? {
        return findExactIndex(sortedImages,frameIndex)
    }
    fun findLowerClosestIndex(frameIndex: Int): Int? {
        return findLowerClosestIndex(sortedImages,frameIndex)
    }
    fun findUpperClosestIndex(frameIndex: Int): Int? {
        return findUpperClosestIndex(sortedImages,frameIndex)
    }
    fun getImageNameWithoutExtension(frameIndex: Int): String {
        return "${videoFile.nameWithoutExtension}_${String.format(Locale.US,"%07d", frameIndex)}"
    }
    fun removeImageInfo(frameIndex: Int){
        images.remove(frameIndex)
        updateSortedImages()
    }
    fun removeImageInfo(imageInfo: ImageInfo){
        removeImageInfo(imageInfo.frameIndex)
    }
    fun saveImageInfo(videoJsonDir: String,frameIndex: Int){
        images[frameIndex]?.let{ (saveImageInfo(videoJsonDir,it)) }
    }
    fun saveImageInfo(videoJsonPath: String,imageInfo: ImageInfo){
        imageInfo.save(getImageJsonPath(videoJsonPath,imageInfo.frameIndex))
    }
    fun save(videoJsonPath: String){
        for (imageInfo in sortedImages) {
            saveImageInfo(videoJsonPath,imageInfo)
        }
    }
    private fun findExactIndex(images: List<ImageInfo>, target: Int): Int? {
        var low = 0
        var high = images.size - 1

        while (low <= high) {
            val mid = (low + high) / 2
            val midValue = images[mid].frameIndex

            when {
                midValue == target -> return mid // Encontrado
                midValue < target -> low = mid + 1 // Buscar en la mitad superior
                else -> high = mid - 1 // Buscar en la mitad inferior
            }
        }

        return null // No encontrado
    }
    private fun findLowerClosestIndex(images: List<ImageInfo>, target: Int): Int? {
        var low = 0
        var high = images.size - 1
        var lowerIndex: Int? = null

        while (low <= high) {
            val mid = (low + high) / 2
            val midValue = images[mid].frameIndex

            if (midValue <= target) {
                lowerIndex = mid // Actualiza el índice del menor más cercano
                low = mid + 1    // Busca en la mitad superior
            } else {
                high = mid - 1   // Busca en la mitad inferior
            }
        }
        return lowerIndex
    }
    private fun findUpperClosestIndex(images: List<ImageInfo>, target: Int): Int? {
        var low = 0
        var high = images.size - 1
        var upperIndex: Int? = null

        while (low <= high) {
            val mid = (low + high) / 2
            val midValue = images[mid].frameIndex

            if (midValue > target) {
                upperIndex = mid // Actualiza el índice del mayor más cercano
                high = mid - 1   // Busca en la mitad inferior
            } else {
                low = mid + 1    // Busca en la mitad superior
            }
        }
        return upperIndex
    }
    private fun getImageJsonPath(videoJsonPath: String,frameIndex: Int): String {
        return File(videoJsonPath,"${getImageNameWithoutExtension(frameIndex)}.json").path
    }
    private fun updateSortedImages(){
        //val filteredImages = images//images.filter { it.value.shapes.isNotEmpty() }
        sortedImages = images.toSortedMap().values.toList()
    }
}
class LabelFile(
    val filename: String,
) {
    var labels = mutableListOf<String>()
    //var areLabelsLoaded = false

    fun loadLabels(labelJsonDir: String): MutableList<String> {
        try {
            labels = mutableListOf()
            val file = File(labelJsonDir, filename)
            val labelData:LabelData = Json.decodeFromString(file.readText())
            labels = labelData.labels.toMutableList()
            //areLabelsLoaded = true
        } catch (e: Exception) {
            e.printStackTrace()
            //areLabelsLoaded = false
        }
        return labels
    }
    fun saveLabels(labelJsonDir: String){
        val file = File(labelJsonDir, filename)
        val jsonString = toJsonString()
        file.writeText(jsonString)
    }
    private fun toJsonString(): String {
        val labelData = LabelData(labels)
        val json = Json {
            prettyPrint = true
            prettyPrintIndent = "  "
        }
        return json.encodeToString(LabelData.serializer(),labelData)
    }
}

@Serializable
data class ShapeData(
    @SerialName("shape_type") val type: String,
    @SerialName("label") val label: String,
    @SerialName("points") val points: List<List<Float>>
)
@Serializable
data class ImageData(
    @SerialName("imagePath") val path: String,
    @SerialName("imageWidth") val width: Int,
    @SerialName("imageHeight") val height: Int,
    @SerialName("frameIndex") val frameIndex: Int,
    @SerialName("shapes") val shapes: List<ShapeData>
)
@Serializable
data class LabelData(
    @SerialName("labels") val labels: List<String>
)

