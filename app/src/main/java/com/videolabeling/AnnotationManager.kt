package com.videolabeling

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.videolabeling.models.ImageData
import com.videolabeling.models.LabelFile
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AnnotationManager(root: File) {
    private var annotationsDir: File = File(root, "annotations").apply {
        if (!exists()) mkdirs()
    }
    private var projectMappingFile: File = File(annotationsDir, "project_mapping.json")
    private var projectMappingData: ProjectMappingData
    private var projectPath: String
    private val labelFile = LabelFile("labels.json")

    var annotationDir: String = ""
        private set
    var mediaDirUri: String = ""
        set(value) {
            field = value
            annotationDir = getAnnotationDirByUri(value)
            Log.d("test annotationDir",annotationDir)
        }
    var labels = mutableListOf<String>()
        private set

    init {
        if (!projectMappingFile.exists()) {
            saveProjectMappingData(ProjectMappingData("", mutableListOf()))
        }
        projectMappingData = getProjectMappingData()
        projectPath = findProject(getCurrentProjectName())?.path ?:run { "" }
        Log.d("test projectPath",projectPath)
        update()
    }
    fun generateUniqueName(prefix: String = ""): String {
        val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US)
        val currentTime = System.currentTimeMillis()
        val date = Date(currentTime)
        val formattedDate = dateFormat.format(date)
        //val milliseconds = formattedDate.takeLast(3)
        //return "unique_${formattedDate}_$milliseconds"
        return if(prefix == "") formattedDate else "${prefix}_${formattedDate}"
    }
    fun getAnnotationDirByUri(mediaDirUri: String): String {
        return getAnnotationDirByProject(projectMappingData.projectName,mediaDirUri)
    }
    fun getCurrentProjectName(): String {
        return projectMappingData.projectName
    }
    fun getProjects(): MutableList<String> {
        return projectMappingData.projects.filter { it.visible }.map { it.name }.toMutableList()
    }
    fun removeProject(projectName: String){
        val projectData = findProject(projectName)
        if(projectData != null){
            projectData.visible = false
            saveProjectMapping()
            if(projectData.name == projectMappingData.projectName) setProject("")
        }
        //val wasRemoved = projectMappingData.projects.removeIf { it.name.equals(projectName, ignoreCase = true) }
    }
    fun saveLabels(){
        labelFile.saveLabels(projectPath)
        Log.d("test save labels",labelFile.labels.toString())
    }
    fun setProject(projectName: String){
        if(projectName != ""){
            val projectData = findProject(projectName) ?:run { createProject(projectName) }
            projectMappingData.projectName = projectData.name
            projectPath = projectData.path
        }
        else{
            projectMappingData.projectName = ""
            projectPath = ""
        }
        saveProjectMapping()
        update()
    }
    fun update(){
        if(projectPath != ""){
            labels = labelFile.loadLabels(projectPath)
            if(mediaDirUri != "") annotationDir = getAnnotationDirByUri(mediaDirUri)
        }
    }
    private fun createAnnotationData(projectData: ProjectData,mediaDirUri: String): AnnotationData {
        Log.d("test lastPathSegment",Uri.parse(mediaDirUri).lastPathSegment.toString())
        val name = Uri.parse(mediaDirUri).lastPathSegment?.substringAfterLast("/") ?:run { "" }
        val annotationDir = File(projectData.path,generateUniqueName(name))
        if(!annotationDir.exists()) annotationDir.mkdir()
        val annotationData = AnnotationData(mediaDirUri,annotationDir.path)
        return annotationData
    }
    fun saveProjectAnnotations(context: Context,selectedFolderUri: Uri,labels: MutableList<String>): Pair<Boolean,String> {
        val projectName = projectMappingData.projectName
        val newFolderName = generateUniqueName("${projectName}_yolo")
        return try {
            val pickedDir = DocumentFile.fromTreeUri(context,selectedFolderUri)
            val newFolder = pickedDir?.createDirectory(newFolderName)
            if (newFolder != null) {
                findProject(projectName)?.let { projectData ->
                    val labelMap = labels.withIndex().associate { it.value to it.index }
                    val newFile = newFolder.createFile("text/plain","labels")
                    newFile?.let {
                        context.contentResolver.openOutputStream(newFile.uri)?.use { outputStream ->
                            val text = labels.joinToString(separator = "\n")
                            outputStream.write(text.toByteArray())
                            outputStream.flush()
                        }
                    }
                    for(annotationData in projectData.annotations){
                        val annotationDir = File(annotationData.annotationDir)
                        saveJsonFile(context,annotationDir,newFolder,labelMap)
                    }
                }
            }
            Pair(true,newFolderName)
        } catch (e: Exception) {
            e.printStackTrace()
            Pair(false,newFolderName)
        }
    }
    private fun saveJsonFile(context: Context,sourceFile:File,targetDocumentFile: DocumentFile,labelMap: Map<String, Int>){
        if(sourceFile.isFile){
            if(sourceFile.extension == "json"){
                val imageData: ImageData = Json.decodeFromString(sourceFile.readText())
                val newFile = targetDocumentFile.createFile("text/plain",sourceFile.nameWithoutExtension)
                newFile?.let {
                    context.contentResolver.openOutputStream(newFile.uri)?.use { outputStream ->
                        var text = ""
                        val width = imageData.width
                        val height = imageData.height
                        for((index,shape) in imageData.shapes.withIndex()){
                            val labelIndex = labelMap[shape.label]
                            val points = when(shape.type){
                                "rectangle" -> toRectangleFormat(normalizePoints(shape.points,width,height))
                                "polygon" -> normalizePoints(shape.points,width,height)
                                else -> DoubleArray(0)
                            }
                            if(points.isNotEmpty()){
                                text += if(index == 0) "$labelIndex ${points.joinToString(separator = " ")}"
                                else "\n$labelIndex ${points.joinToString(separator = " ")}"
                            }
                        }
                        outputStream.write(text.toByteArray())
                        outputStream.flush()
                    }
                }
            }
            else if(sourceFile.extension == "jpeg"){
                val newFile = targetDocumentFile.createFile("image/jpeg", sourceFile.nameWithoutExtension)
                newFile?.let {
                    val inputStream: InputStream = sourceFile.inputStream()
                    val outputStream: OutputStream? = context.contentResolver.openOutputStream(newFile.uri)
                    if (outputStream != null) {
                        inputStream.use { input ->
                            outputStream.use { output ->
                                input.copyTo(output)
                            }
                        }
                        Log.d("test jpeg","Imagen ${sourceFile.name} copiado exitosamente")
                    } else {
                        Log.d("test jpeg","No se pudo abrir el OutputStream proveniente de ${sourceFile.name}")
                    }
                }
            }
        }
        else if(sourceFile.isDirectory){
            val files = sourceFile.listFiles()
            if (files != null) {
                val newDirectory = targetDocumentFile.createDirectory(sourceFile.name)
                if (newDirectory != null) {
                    for(file in files) {
                        saveJsonFile(context,file,newDirectory,labelMap)
                    }
                }
            }
        }
    }
    private fun createProject(projectName: String): ProjectData {
        val projectFile = File(annotationsDir,generateUniqueName(projectName))
        projectFile.mkdir()
        val projectData = ProjectData(projectName,true,projectFile.path,mutableListOf())
        projectMappingData.projects.add(projectData)
        return projectData
    }
    private fun findProject(projectName: String): ProjectData? {
        return if(projectName == "") null
            else projectMappingData.projects.filter { it.visible }.find { it.name.equals(projectName, ignoreCase = true) }
    }
    private fun getAnnotationDirByProject(projectName: String,mediaDirUri: String): String {
        return findProject(projectName)?.let { projectData ->
            projectData.annotations.find { it.mediaDirUri == mediaDirUri }?.annotationDir ?:run {
                val annotationData = createAnnotationData(projectData,mediaDirUri)
                projectData.annotations.add(annotationData)
                saveProjectMapping()
                annotationData.annotationDir
            }
        } ?:run {
            val projectData = ProjectData(projectName,true,annotationsDir.path, mutableListOf())
            val annotationData = createAnnotationData(projectData,mediaDirUri)
            projectData.annotations.add(annotationData)
            projectMappingData.projects.add(projectData)
            saveProjectMapping()
            annotationData.annotationDir
        }
    }
    private fun getProjectMappingData(): ProjectMappingData{
        return Json.decodeFromString(projectMappingFile.readText())
    }
    private fun saveProjectMapping(){
        saveProjectMappingData(projectMappingData)
    }
    private fun saveProjectMappingData(projectMappingData: ProjectMappingData){
        val json = Json {
            prettyPrint = true
            prettyPrintIndent = "  "
        }
        val jsonString = json.encodeToString(ProjectMappingData.serializer(),projectMappingData)
        projectMappingFile.writeText(jsonString)
    }
    private fun normalizePoints(pointList: List<List<Float>>, width: Int, height: Int): DoubleArray {
        val points = DoubleArray(2 * pointList.size)
        for (i in pointList.indices) {
            points[2 * i] = (pointList[i][0] / width).toDouble()
        }
        for (i in pointList.indices) {
            points[2 * i + 1] = (pointList[i][1] / height).toDouble()
        }
        return points
    }
    private fun toRectangleFormat(points: DoubleArray,isBbox: Boolean = false): DoubleArray {
        val xValues = points.filterIndexed { index, _ -> index % 2 == 0 }
        val yValues = points.filterIndexed { index, _ -> index % 2 != 0 }
        val xMin = xValues.min()
        val xMax = xValues.max()
        val yMin = yValues.min()
        val yMax = yValues.max()
        val w = xMax - xMin
        val h = yMax - yMin
        return if(isBbox) doubleArrayOf(xMin + w/2,yMin + h/2,w,h)
        else doubleArrayOf(xMin, yMin, xMax, yMin, xMax, yMax, xMin, yMax)
    }
}

@Serializable
data class AnnotationData(
    @SerialName("uri") val mediaDirUri: String,
    @SerialName("directory") val annotationDir: String
)
@Serializable
data class ProjectData(
    @SerialName("name") val name: String,
    @SerialName("visible") var visible: Boolean,
    @SerialName("path") val path: String,
    @SerialName("annotations") val annotations: MutableList<AnnotationData>
)
@Serializable
data class ProjectMappingData(
    @SerialName("project") var projectName: String,
    @SerialName("projects") val projects: MutableList<ProjectData>
)