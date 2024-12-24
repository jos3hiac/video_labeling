package com.videolabeling

import android.net.Uri
import android.util.Log
import com.videolabeling.models.LabelFile
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
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
        projectPath = findProject(getCurrentProject())?.path ?:run { "" }
        Log.d("test projectPath",projectPath)
        update()
    }
    fun generateUniqueName(): String {
        val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US)
        val currentTime = System.currentTimeMillis()
        val date = Date(currentTime)
        val formattedDate = dateFormat.format(date)
        //val milliseconds = formattedDate.takeLast(3)
        //return "unique_${formattedDate}_$milliseconds"
        return formattedDate
    }
    fun getAnnotationDirByUri(mediaDirUri: String): String {
        return getAnnotationDirByProject(projectMappingData.project,mediaDirUri)
    }
    fun getCurrentProject(): String {
        return projectMappingData.project
    }
    fun getProjects(): MutableList<String> {
        return projectMappingData.projects.filter { it.visible }.map { it.name }.toMutableList()
    }
    fun removeProject(projectName: String){
        val projectData = findProject(projectName)
        if(projectData != null){
            projectData.visible = false
            saveProjectMapping()
            if(projectData.name == projectMappingData.project) setProject("")
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
            projectMappingData.project = projectData.name
            projectPath = projectData.path
        }
        else{
            projectMappingData.project = ""
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
        val annotationDir = File(projectData.path,"${name}_${generateUniqueName()}")
        if(!annotationDir.exists()) annotationDir.mkdir()
        val annotationData = AnnotationData(mediaDirUri,annotationDir.path)
        return annotationData
    }
    private fun createProject(projectName: String): ProjectData {
        val projectFile = File(annotationsDir,"${projectName}_${generateUniqueName()}")
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
}

/*@Serializable
data class AnnotationMappingItem(
    @SerialName("uri") val mediaDirUri: String,
    @SerialName("directory") val annotationDir: String
)

@Serializable
data class AnnotationMapping(
    @SerialName("annotation_mapping") val annotationMappingList: MutableList<AnnotationMappingItem>
)*/
/*class Annotation {
    var mediaDirUri = ""
    var directory = ""
}
class Project {
    var name = ""
    var visible = true
    var path = ""
    val annotations = mutableListOf<Annotation>()
}
class ProjectMapping {
    var project = ""
    val projects = mutableListOf<Project>()

    fun loadProject(projectDir: String){

    }
}*/

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
    @SerialName("project") var project: String,
    @SerialName("projects") val projects: MutableList<ProjectData>
)