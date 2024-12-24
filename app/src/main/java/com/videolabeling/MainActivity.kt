package com.videolabeling

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModelProvider
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.videolabeling.models.ImageFile
import com.videolabeling.models.MediaFile
import com.videolabeling.models.MediaFileViewModel
import com.videolabeling.models.Shape
import com.videolabeling.models.VideoFile
import com.videolabeling.views.ImageControlsLayout
import com.videolabeling.views.MediaAdapter
import com.videolabeling.views.ProjectDialog
import com.videolabeling.views.VideoControlsLayout
import com.videolabeling.views.ZoomImageView

class MainActivity : AppCompatActivity() {

    private lateinit var annotationManager: AnnotationManager
    private lateinit var mediaFileViewModel: MediaFileViewModel
    private lateinit var openFolderLauncher: ActivityResultLauncher<Uri?>
    private var isPortrait = true
    private lateinit var recyclerViewMedia: RecyclerView
    private lateinit var mediaAdapter: MediaAdapter
    private lateinit var videoControls: VideoControlsLayout
    private lateinit var imageControls: ImageControlsLayout
    private lateinit var projectDialog: ProjectDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        toolbar.setTitleTextColor(Color.WHITE)
        setSupportActionBar(toolbar)
        isPortrait = resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT

        annotationManager = AnnotationManager(filesDir)
        mediaFileViewModel = ViewModelProvider(this)[MediaFileViewModel::class.java]
        //menuViewModel = ViewModelProvider(this)[MenuViewModel::class.java]
        recyclerViewMedia = findViewById(R.id.recyclerViewMedia)
        videoControls = findViewById(R.id.videoControlsLayout)
        imageControls = findViewById(R.id.imageControlsLayout)

        mediaAdapter = MediaAdapter(
            onItemClick = { index,file ->
                mediaFileViewModel.apply {
                    if(fileIndex != index){
                        isVideoVisible = true
                        videoPosition = 0L
                        frameIndex = 0
                        labeledFrameIndex = null
                        selectMediaFile(index,file)
                    }
                }
            }
        )
        recyclerViewMedia.adapter = mediaAdapter

        projectDialog = ProjectDialog(this)
        projectDialog.setItems(annotationManager.getProjects())
        projectDialog.onAccept = { projectName ->
            annotationManager.setProject(projectName)
            updateMenu()
        }
        projectDialog.onRemove = { position,item ->
            /*AlertDialog.Builder(applicationContext)
                .setTitle("Confirmar eliminación")
                .setMessage("¿Estás seguro que deseas eliminar el proyecto $item ?")
                .setPositiveButton("Sí") { dialog, _ ->
                    Toast.makeText(applicationContext, "Proyecto $item eliminado", Toast.LENGTH_SHORT).show()
                    projectDialog.removeItemAt(position)
                    projectDialog.setButtonRemoveEnabled(false)
                    annotationManager.removeProject(item)
                    updateMenu()
                    dialog.dismiss()
                }
                .setNegativeButton("No") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()*/
            Toast.makeText(applicationContext, "Proyecto $item eliminado", Toast.LENGTH_SHORT).show()
            projectDialog.removeItemAt(position)
            projectDialog.setButtonRemoveEnabled(false)
            annotationManager.removeProject(item)
            updateMenu()
        }

        videoControls.apply {
            exoPlayer.addListener(object : Player.Listener {
                override fun onTracksChanged(tracks: Tracks) {
                    super.onTracksChanged(tracks)
                    mediaFileViewModel.file?.let { file ->
                        if(file is VideoFile){
                            for (group in tracks.groups) {
                                for (i in 0 until group.length) {
                                    val format = group.getTrackFormat(i)
                                    if (format.frameRate != Format.NO_VALUE.toFloat()) {
                                        val fps = format.frameRate
                                        val duration = exoPlayer.duration / 1000f
                                        val frameCount = (fps * duration).toInt()
                                        file.fps = fps
                                        file.duration = duration
                                        file.frameCount = frameCount
                                        Log.d("test fps","Video: ${file.name} tiene fps: $fps duration: $duration frames $frameCount")
                                        configureFrameLayout(file)
                                    }
                                }
                            }
                        }
                    }
                }
            })
            controlHideVideo.onButtonClick = {
                setVideoControlsVisibility(false)
            }
        }

        imageControls.controlShowVideo.onButtonClick = {
            setVideoControlsVisibility(true)
        }

        openFolderLauncher = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            uri?.let { uri ->
                //saveUriToPreferences(this, uri)
                /*contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )*/
                val files = getFilesFromFolder(this, uri)
                annotationManager.mediaDirUri = uri.toString()
                imageControls.labelDialog.setItems(annotationManager.labels)
                isPortrait = resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
                /*Toast.makeText(this, "${images.size} imágenes encontradas", Toast.LENGTH_SHORT).show()
                loadShapesFromJson(uri,images)*/
                mediaFileViewModel.files.value = files
            }
        }

        mediaFileViewModel.files.observe(this) { files ->
            mediaAdapter.submitList(files)
            if (files.isNotEmpty()) {
                selectMediaFile(mediaFileViewModel.fileIndex, files[mediaFileViewModel.fileIndex])
            }
        }

        mediaFileViewModel.apply {
            imageControls.controlCreateRect.onButtonClick = {
                actionType = "create"
                removeShape()
                resetSelectedShape()
                imageControls.zoomImageView.setShape(null)
                setShapeType(Shape.RECT)
                imageControls.handleSelectionButtonControl(imageControls.controlCreateRect)
                updateButtonsState()
                imageControls.zoomImageView.invalidate()
            }
            imageControls.controlCreatePolygon.onButtonClick = {
                actionType = "create"
                removeShape()
                resetSelectedShape()
                imageControls.zoomImageView.setShape(null)
                setShapeType(Shape.POLYGON)
                imageControls.handleSelectionButtonControl(imageControls.controlCreatePolygon)
                updateButtonsState()
                imageControls.zoomImageView.invalidate()
            }
            imageControls.controlUndoLastPoint.onButtonClick = {
                if(actionType == "create"){
                    undoLastPoint()
                    if(isShapeNull()){
                        imageControls.zoomImageView.setShape(null)
                        updateButtonsState()
                    }
                    imageControls.zoomImageView.invalidate()
                }
            }
            imageControls.controlRemoveShape.onButtonClick = {
                if(actionType == "create"){
                    removeShape()
                    imageControls.zoomImageView.setShape(null)
                }
                else if(actionType == "edit"){
                    removeSelectedShape()
                    saveFrame()
                }
                updateButtonsState()
                imageControls.zoomImageView.invalidate()
            }
            imageControls.controlEditShape.onButtonClick = {
                if(actionType == "create"){
                    actionType = "edit"
                    updateButtonsState(true)
                }
            }
            imageControls.controlEditLabel.onButtonClick = {
                if(actionType == "edit"){
                    imageControls.labelDialog.setItem(getLabel())
                    imageControls.labelDialog.show()
                    saveFrame()
                }
            }
            imageControls.labelDialog.onAccept = { label ->
                if(actionType == "create"){
                    setLabel(label,imageControls.labelDialog.getItems())
                    addShape()
                    imageControls.zoomImageView.setShape(null)
                }
                else if(actionType == "edit"){
                    editLabel(label,imageControls.labelDialog.getItems())
                }
                updateButtonsState()
                imageControls.zoomImageView.invalidate()
                saveFrame()
                annotationManager.saveLabels()
                /*Log.e("test shapes.size","$label ${imageInfo!!.shapes.size}")
                for((index,shape) in imageInfo!!.shapes.withIndex()){
                    Log.e("test shape $index","${shape.points[0]} ${shape.points[1]}")
                }*/
            }
            imageControls.labelDialog.onRemove = { position,item ->
                /*AlertDialog.Builder(applicationContext)
                    .setTitle("Confirmar eliminación")
                    .setMessage("¿Estás seguro que deseas eliminar la etiqueta $item ?")
                    .setPositiveButton("Sí") { dialog, _ ->
                        Toast.makeText(applicationContext, "Etiqueta $item eliminada", Toast.LENGTH_SHORT).show()
                        imageControls.labelDialog.removeItemAt(position)
                        if(imageControls.labelDialog.getSelectedPosition() == -1) imageControls.labelDialog.setButtonRemoveEnabled(false)
                        dialog.dismiss()
                    }
                    .setNegativeButton("No") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()*/
                Toast.makeText(applicationContext, "Etiqueta $item eliminada", Toast.LENGTH_SHORT).show()
                imageControls.labelDialog.removeItemAt(position)
                imageControls.labelDialog.setButtonRemoveEnabled(false)
                annotationManager.saveLabels()
            }
            imageControls.labelDialog.onCancel = {
                if(actionType == "create"){
                    undoLastPoint()
                    imageControls.zoomImageView.invalidate()
                }
            }
            imageControls.zoomImageView.onSingleTapListener = { pts ->
                if(actionType == "create"){
                    if(shape == null){
                        imageControls.zoomImageView.setShape(createShape(imageControls.zoomImageView.matrix))
                        updateButtonsState()
                    }
                    if(addPointAndValidateShape(pts[0])){
                        imageControls.labelDialog.show()
                    }
                }
                else if(actionType == "edit"){
                    selectShape(pts[0])
                    updateButtonsState()
                }
            }
            imageControls.zoomImageView.onDoubleTapListener = { pts ->
                if(actionType == "create"){
                    if(addPointAndValidateShape(pts[0],true)) {
                        imageControls.labelDialog.show()
                    }
                }
            }
            imageControls.zoomImageView.onDragStartListener = { pts ->
                if(actionType == "edit"){
                    if(updateSelectedShape(pts[0])) imageControls.zoomImageView.setDirection(ZoomImageView.Direction.MOUSE)
                    else imageControls.zoomImageView.setDirection(ZoomImageView.Direction.TOUCH)
                }
            }
            imageControls.zoomImageView.onDragMoveListener = { pts ->
                if(actionType == "edit"){
                    moveSelectedShape(pts[2].x,pts[2].y)
                }
            }
            imageControls.zoomImageView.onDragEndListener = { pts ->
                if(actionType == "edit"){
                    imageControls.zoomImageView.setDirection(ZoomImageView.Direction.TOUCH)
                    if(isSelectedShapeNotNull()) saveFrame()
                }
            }
        }

        setViewLayoutManager(isPortrait)
    }
    private fun updateButtonsState(resetState: Boolean = false,isSelectedButtonEnabled: Boolean = false){
        val isShapeNotNull = mediaFileViewModel.isShapeNotNull()
        val isSelectedShapeNotNull = mediaFileViewModel.isSelectedShapeNotNull()
        if(!resetState) imageControls.setSelectedButtonEnabled(isSelectedButtonEnabled)
        else imageControls.resetSelectedButtonControl()
        imageControls.controlUndoLastPoint.setEnabled(isShapeNotNull)
        if(mediaFileViewModel.actionType == "create"){
            imageControls.controlRemoveShape.setEnabled(isShapeNotNull)
            imageControls.controlEditShape.setEnabled(!isShapeNotNull)
            imageControls.controlEditLabel.setEnabled(false)
        }
        else if(mediaFileViewModel.actionType == "edit"){
            imageControls.controlUndoLastPoint.setEnabled(false)
            imageControls.controlRemoveShape.setEnabled(isSelectedShapeNotNull)
            imageControls.controlEditShape.setEnabled(false)
            imageControls.controlEditLabel.setEnabled(isSelectedShapeNotNull)

        }
    }
    private fun getFilesFromFolder(context: Context, folderUri: Uri): List<MediaFile> {
        val mediaFiles = mutableListOf<MediaFile>()
        val documentFile = DocumentFile.fromTreeUri(context, folderUri)
        documentFile?.let {
            for (file in it.listFiles().sortedBy { file -> file.name }) {
                if (file.isFile){
                    if(file.type?.startsWith("image/") == true){
                        Log.e("image", file.name!!)
                        val imageFile = ImageFile(this,file.uri,file.name!!,imageControls.zoomImageView.matrix,annotationManager.labels)
                        mediaFiles.add(imageFile)
                    }
                    else if(file.type?.startsWith("video/") == true){
                        Log.e("video", file.name!!)
                        mediaFiles.add(VideoFile(this,file.uri, file.name!!,imageControls.zoomImageView.matrix,annotationManager.labels))
                    }
                }
            }
        }
        if (documentFile == null) {
            Log.e("DocumentFile", "No se pudo obtener el DocumentFile para el URI")
        }
        return mediaFiles
    }
    private fun updateMenu(){
        val projectName = annotationManager.getCurrentProject()
        imageControls.labelDialog.setItems(annotationManager.labels)
        supportActionBar?.title = projectName
        setMenuItemEnabled(mediaFileViewModel.menu!!.findItem(R.id.open_dir),projectName != "")
        setMenuItemEnabled(mediaFileViewModel.menu!!.findItem(R.id.download),projectName != "")
        videoControls.setVisibility(false)
        imageControls.setVisibility(false)
        if(projectName != ""){
            mediaFileViewModel.file?.let {
                it.areShapesLoaded = false
                selectMediaFile(mediaFileViewModel.fileIndex,it)
            }
        }
    }
    private fun selectMediaFile(index: Int, file: MediaFile) {
        mediaFileViewModel.setMediaFileAndIndex(file,index)
        videoControls.apply {
            if(!file.areShapesLoaded){
                file.loadShapes(annotationManager.annotationDir)
            }
            if (file is ImageFile) {
                mediaFileViewModel.fileType = "image"
                exoPlayer.stop()
                exoPlayer.clearMediaItems()
                setVideoControlsVisibility(false)
                setZoomImageByImage(file)
                //imageView.setImageURI(file.uri)
            } else if (file is VideoFile) {
                mediaFileViewModel.fileType = "video"
                exoPlayer.setMediaItem(MediaItem.fromUri(file.uri))
                exoPlayer.prepare()
                //exoPlayer.play()
                setVideoPosition(mediaFileViewModel.videoPosition)
                setVideoControlsVisibility(mediaFileViewModel.isVideoVisible)
            }
        }
    }
    private fun setVideoControlsVisibility(isVisible: Boolean){
        mediaFileViewModel.apply {
            isVideoVisible = isVisible
            videoControls.setVisibility(isVisible)
            imageControls.setVisibility(!isVisible)
            file?.let { file ->
                if(!isVisible && file is VideoFile) setZoomImageByFrameIndex(file,frameIndex)
            }
        }
    }
    private fun setZoomImageByImage(file: ImageFile){
        file.image?.let { image ->
            Log.e("test","image: ${file.name}")
            //zoomImageView.setImageBitmap(frame)
            imageControls.zoomImageView.setImageDrawable(BitmapDrawable(resources,image))
        }
    }
    /*private fun setZoomImageByTime(file: VideoFile,time: Long){
        file.getFrameByMilliseconds(time)?.let { frame ->
            val frameIndex = file.getFrameIndexByMilliseconds(time)
            Log.e("test","Video: ${file.name} con time $time frame: $frameIndex")
            zoomImageView.setImageDrawable(BitmapDrawable(resources,frame))
        }
    }*/
    private fun setZoomImageByFrameIndex(file: VideoFile,frameIndex: Int){
        file.getImageInfo(frameIndex)?.let { imageInfo ->
            imageInfo.image?.let { frame ->
                mediaFileViewModel.initImageInfo(imageInfo,annotationManager.labels)
                Log.e("test zoom","Video: ${file.name} con frame: $frameIndex fps: ${file.fps} frames: ${file.frameCount} labels ${annotationManager.labels} images: ${file.videoInfo.images.size} ")
                //imageControls.zoomImageView.setImageBitmap(frame)
                imageControls.zoomImageView.apply {
                    resetViewState()
                    setPermissions(arrayOf(ZoomImageView.Permission.ALL))
                    setShapes(imageInfo.shapes)
                    setImageDrawable(BitmapDrawable(resources,frame))
                }
                mediaFileViewModel.removeShape()
                mediaFileViewModel.resetSelectedShape()
                updateButtonsState(true)
                updateFrame(file,frameIndex)

            }
        }
    }
    private fun saveFrame(){
        mediaFileViewModel.imageInfo?.let {imageInfo ->
            when(imageInfo.mediaFile){
                is ImageFile -> imageInfo.mediaFile.saveShapes(annotationManager.annotationDir)
                is VideoFile -> imageInfo.mediaFile.saveShapes(annotationManager.annotationDir,imageInfo)
            }
            Log.d("test save frame","frameIndex ${imageInfo.frameIndex} shapes size ${imageInfo.shapes.size}")
        }
    }
    private fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }
    private fun updateFrame(file: VideoFile,time: Long,newLabelFrameIndex: Int? = null){
        Log.e("test time","$time")
        file.getFrameIndexByMilliseconds(time)?.let { frameIndex ->
            updateFrame(file,if(frameIndex > 0) frameIndex+1 else frameIndex,newLabelFrameIndex)
        }
    }
    private fun updateFrame(file: VideoFile,frameIndex: Int,newLabelFrameIndex: Int? = null){
        mediaFileViewModel.apply {
            if(frameIndex < 0){
                videoControls.inputFrameIndex.value = this.frameIndex+1
            }
            else{
                file.frameCount?.let { frameCount ->
                    val index = if(frameIndex >= frameCount) frameCount-1 else frameIndex
                    this.frameIndex = index
                    videoControls.apply{
                        val newTime = file.getMillisecondsByFrameIndex(frameIndex)//time ?: file.getMillisecondsByFrameIndex(frameIndex)
                        newTime?.let {
                            Log.e("test frameIndex","frameIndex $index msec $it")
                            mediaFileViewModel.videoPosition = it
                            setVideoPosition(it)
                        }
                    }
                    this.labeledFrameIndex = newLabelFrameIndex ?: file.videoInfo.findIndex(index)
                    //Log.e("test frame", "frameIndex $index, labeledFrameIndex ${this.labeledFrameIndex}")
                    updateLayoutFrame(file)
                    /*file.getFrameByFrameIndex(index)?.let { frame ->
                        imageControls.zoomImageView.setImageDrawable(BitmapDrawable(resources,frame))
                    }*/
                }
            }
        }
    }
    private fun configureFrameLayout(file: VideoFile){
        videoControls.apply {
            controlCapture.onButtonClick = {
                updateFrame(file,exoPlayer.currentPosition)
            }
            inputLabeledFrameIndex.onDecrementClick = {
                val labelFrameIndex = mediaFileViewModel.labeledFrameIndex?.minus(1) ?: file.videoInfo.findLowerClosestIndex(mediaFileViewModel.frameIndex)
                labelFrameIndex?.let{updateFrame(file,file.videoInfo.sortedImages[it].frameIndex,it)}
            }
            inputLabeledFrameIndex.onIncrementClick = {
                val labelFrameIndex = mediaFileViewModel.labeledFrameIndex?.plus(1) ?: file.videoInfo.findUpperClosestIndex(mediaFileViewModel.frameIndex)
                labelFrameIndex?.let{updateFrame(file,file.videoInfo.sortedImages[it].frameIndex,it)}
            }
            inputLabeledFrameIndex.onDonePressed = { text ->
                val index = text.toIntOrNull()?.minus(1)
                if(index != null && index in 0..< file.videoInfo.sortedImages.size){
                    updateFrame(file,file.videoInfo.sortedImages[index].frameIndex,index)
                }
                else mediaFileViewModel.labeledFrameIndex?.let { inputLabeledFrameIndex.value = it+1 }
            }
            inputFrameIndex.onDecrementClick = {
                updateFrame(file,mediaFileViewModel.frameIndex - 1,null)
            }
            inputFrameIndex.onIncrementClick = {
                updateFrame(file,mediaFileViewModel.frameIndex + 1,null)
            }
            inputFrameIndex.onDonePressed = { text ->
                val index = text.toIntOrNull()?.minus(1)
                if(index != null) updateFrame(file,index,null)
                else inputFrameIndex.value = mediaFileViewModel.frameIndex+1
            }
            updateFrame(file,mediaFileViewModel.frameIndex)
        }
    }
    private fun updateLayoutFrame(file: VideoFile){
        val labelFrameIndex = mediaFileViewModel.labeledFrameIndex
        videoControls.apply {
            inputLabeledFrameIndex.apply {
                labelFrameIndex?.let {
                    value = it + 1
                    setDecrementButtonEnabled(it-1 >= 0)
                    setIncrementButtonEnabled(it+1 < file.videoInfo.sortedImages.size)
                } ?:run {
                    value = null
                    setDecrementButtonEnabled(file.videoInfo.findLowerClosestIndex(mediaFileViewModel.frameIndex) != null)
                    setIncrementButtonEnabled(file.videoInfo.findUpperClosestIndex(mediaFileViewModel.frameIndex) != null)
                }
            }
            val frameIndex = mediaFileViewModel.frameIndex
            inputFrameIndex.apply {
                value = frameIndex+1
                file.frameCount?.let{
                    setDecrementButtonEnabled(frameIndex-1 >= 0)
                    setIncrementButtonEnabled(frameIndex+1 < it)
                }
            }
        }
    }
    private fun setViewLayoutManager(isPortrait: Boolean) {
        val videoControlsLayoutParams = videoControls.layoutParams as RelativeLayout.LayoutParams
        val imageControlsLayoutParams = imageControls.layoutParams as RelativeLayout.LayoutParams
        val playerLayoutParams = videoControls.getPlayerLayoutParams()
        val imageLayoutParams = imageControls.getImageLayoutParams()
        //val labelLayoutParams = recyclerViewLabel.layoutParams as RelativeLayout.LayoutParams
        //recyclerViewLabel.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        if(isPortrait){
            recyclerViewMedia.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

            videoControlsLayoutParams.apply {
                addRule(RelativeLayout.BELOW, R.id.recyclerViewMedia)
                removeRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
                removeRule(RelativeLayout.END_OF)
                width = RelativeLayout.LayoutParams.MATCH_PARENT
                height = RelativeLayout.LayoutParams.WRAP_CONTENT
            }

            playerLayoutParams.apply {
                width = LinearLayout.LayoutParams.MATCH_PARENT
                height = 350.dpToPx()
            }

            videoControls.setOrientation(LinearLayout.HORIZONTAL)
            imageControls.setOrientation(LinearLayout.HORIZONTAL)

            imageControlsLayoutParams.apply {
                addRule(RelativeLayout.BELOW, R.id.videoControlsLayout)
                removeRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
                removeRule(RelativeLayout.END_OF)
                width = RelativeLayout.LayoutParams.MATCH_PARENT
                height = RelativeLayout.LayoutParams.WRAP_CONTENT
            }

            imageLayoutParams.apply {
                width = LinearLayout.LayoutParams.MATCH_PARENT
                height = playerLayoutParams.height
            }

            //imageLayoutParams.addRule(RelativeLayout.ABOVE, R.id.recyclerViewLabel)
            //labelLayoutParams.removeRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
            //imageLayoutParams.removeRule(RelativeLayout.START_OF)
            /*labelLayoutParams.width = RecyclerView.LayoutParams.MATCH_PARENT
            labelLayoutParams.height = (200 * resources.displayMetrics.density).toInt()
            labelLayoutParams.removeRule(RelativeLayout.BELOW)
            labelLayoutParams.removeRule(RelativeLayout.ALIGN_PARENT_RIGHT)
            labelLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE)*/
        }
        else{
            recyclerViewMedia.layoutManager = GridLayoutManager(this, 2)

            videoControlsLayoutParams.apply {
                addRule(RelativeLayout.BELOW,R.id.toolbar)
                addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE)
                addRule(RelativeLayout.END_OF,R.id.recyclerViewMedia)
                width = RelativeLayout.LayoutParams.WRAP_CONTENT//(200 * resources.displayMetrics.density).toInt()
                height = RelativeLayout.LayoutParams.MATCH_PARENT
            }

            playerLayoutParams.apply {
                width = 350.dpToPx()
                height = LinearLayout.LayoutParams.MATCH_PARENT
            }

            videoControls.setOrientation(LinearLayout.VERTICAL)
            imageControls.setOrientation(LinearLayout.VERTICAL)

            imageControlsLayoutParams.apply {
                addRule(RelativeLayout.BELOW,R.id.toolbar)
                addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE)
                addRule(RelativeLayout.END_OF,R.id.videoControlsLayout)
                width = RelativeLayout.LayoutParams.WRAP_CONTENT//(200 * resources.displayMetrics.density).toInt()
                height = RelativeLayout.LayoutParams.MATCH_PARENT
            }

            imageLayoutParams.apply {
                width = playerLayoutParams.width
                height = RelativeLayout.LayoutParams.MATCH_PARENT
            }

            /*
            imageLayoutParams.removeRule(RelativeLayout.ABOVE)
            imageLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE)
            imageLayoutParams.addRule(RelativeLayout.START_OF,R.id.recyclerViewLabel)
            labelLayoutParams.width = (200 * resources.displayMetrics.density).toInt()
            labelLayoutParams.height = RecyclerView.LayoutParams.MATCH_PARENT
            labelLayoutParams.addRule(RelativeLayout.BELOW, R.id.toolbar)
            labelLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE)
            labelLayoutParams.removeRule(RelativeLayout.ALIGN_PARENT_BOTTOM)*/
        }
    }
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mediaFileViewModel.videoPosition = videoControls.exoPlayer.currentPosition
    }
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        mediaFileViewModel.menu = menu
        val openDir = menu.findItem(R.id.open_dir)
        val download = menu.findItem(R.id.download)
        val createProject = menu.findItem(R.id.create_project)
        configureMenuItem(openDir, R.drawable.baseline_folder_36, "Abrir Directorio")
        configureMenuItem(download, R.drawable.baseline_download_36, "Descargar anotaciones VOC")
        configureMenuItem(createProject, R.drawable.baseline_create_new_folder_36, "Nuevo proyecto")
        updateMenu()
        return true
    }
    private fun setMenuItemEnabled(menuItem: MenuItem,enabled: Boolean){
        val actionView = menuItem.actionView
        if (actionView != null) {
            val iconView: ImageView = actionView.findViewById(R.id.icon)
            menuItem.isEnabled = enabled
            if(enabled){
                iconView.alpha = 1f
                actionView.setOnClickListener {
                    //Log.e("Click", actionView.tooltipText.toString())
                    onOptionsItemSelected(menuItem)
                }
            }
            else{
                iconView.alpha = 0.5f
                actionView.setOnClickListener(null)
            }
        }
    }
    private fun configureMenuItem(menuItem: MenuItem,iconResId: Int, tooltipText: String,enabled: Boolean = true) {
        val actionView = menuItem.actionView
        if (actionView != null) {
            actionView.setBackgroundResource(R.drawable.selector_menu_item)
            /*menuViewModel.selectedItemId?.let{
                if(menuItem.itemId == it) actionView.setBackgroundResource(R.drawable.selector_menu_item_selected)
            }*/
            val iconView: ImageView = actionView.findViewById(R.id.icon)
            iconView.setImageResource(iconResId)
            //val vectorDrawable = ContextCompat.getDrawable(this, iconResId) as VectorDrawable
            //val bitmapDrawable = vectorToBitmapDrawable(this, vectorDrawable)
            //menuItem.icon = ContextCompat.getDrawable(this,iconResId)
            menuItem.title = tooltipText
            actionView.tooltipText = tooltipText
            //iconView.isEnabled = enabled
            setMenuItemEnabled(menuItem,enabled)
            /*actionView.setOnLongClickListener { view ->
                Toast.makeText(this, view.tooltipText, Toast.LENGTH_SHORT).show()
                true
            }*/
        }
        else{
            Log.e("Menu", "actionView es nulo")
        }
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.open_dir -> {
                openFolderLauncher.launch(null)
                true
            }
            R.id.create_project -> {
                projectDialog.show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}