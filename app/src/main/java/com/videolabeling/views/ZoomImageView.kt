package com.videolabeling.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.PointF
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import com.videolabeling.models.Axis
import com.videolabeling.models.Shape
import com.videolabeling.models.scaleX
import com.videolabeling.models.transX
import com.videolabeling.models.transY
import com.videolabeling.models.transformOnX
import com.videolabeling.models.transformOnY
import com.videolabeling.models.untransformOnX
import com.videolabeling.models.untransformPoint

class ZoomImageView(context: Context, attrs: AttributeSet?) : androidx.appcompat.widget.AppCompatImageView(context, attrs) {

    var scaleFactor = 1f
    private val mat = Matrix()
    private var disabled = true
    private var isPortrait = true
    private var scaleDetector = ScaleGestureDetector(context, ScaleListener())
    private var gestureDetector = GestureDetector(context, GestureListener())
    private var previous = PointF()
    private var intrinsicWidth = 0f
    private var intrinsicHeight = 0f
    private var shapes: MutableList<Shape> = mutableListOf()
    private var axis: Axis? = null
    private var shape: Shape? = null

    private var permissions: Array<Permission> = arrayOf()
    private var direction = Direction.TOUCH
    //private var isPressed = false
    private var isMouseMoved = false
    private var handler = Handler(Looper.getMainLooper())
    private var currentMotionEvent: MotionEvent? = null
    /*private val longPressRunnable = Runnable {
        if (isPressed) {
            // Manejar el evento de presión prolongada aquí
            println("Long press detected")
        }
    }*/
    private val callbackRunnable = object : Runnable {
        override fun run() {
            currentMotionEvent?.let { event ->
                // Llamar al callback con la posición actual
                if(containsAnyPermissions(arrayOf(Permission.PRESS,Permission.ALL))) {
                    onPressListener?.invoke(getPointsFromView(event.x,event.y))
                    invalidate()
                }
            }
            // Volver a programar el Runnable para ejecutarse después de un retraso
            handler.postDelayed(this, 20) // Llamar al callback cada 100 ms
        }
    }

    var onScaleChangedListener: ((Float) -> Unit)? = null
    var onPressListener: ((Array<PointF>) -> Unit)? = null
    var onClickListener: (() -> Unit)? = null
    var onMouseDownListener: ((Array<PointF>) -> Unit)? = null
    var onMouseUpListener: ((Array<PointF>) -> Unit)? = null
    var onSingleTapListener: ((Array<PointF>) -> Unit)? = null
    var onDoubleTapListener: ((Array<PointF>) -> Unit)? = null
    var onDragStartListener: ((Array<PointF>) -> Unit)? = null
    var onDragMoveListener: ((Array<PointF>) -> Unit)? = null
    var onDragEndListener: ((Array<PointF>) -> Unit)? = null

    init {
        scaleType = ScaleType.MATRIX
        setOnTouchListener { view, event ->
            if(!disabled){
                scaleDetector.onTouchEvent(event)
                gestureDetector.onTouchEvent(event)
                handleTouchEvent(event)
                if (event.action == MotionEvent.ACTION_UP) {
                    view.performClick()
                }
            }
            true
        }
    }
    fun setPermissions(permissions: Array<Permission>) {
        this.permissions = permissions
    }
    fun containsAnyPermissions(permissions: Array<Permission>): Boolean {
        return this.permissions.any { it in permissions }
    }
    fun containsAllPermissions(permissions: Array<Permission>): Boolean {
        return this.permissions.all { it in permissions }
    }
    fun containsPermission(permission: Permission): Boolean {
        return permissions.contains(permission)
    }
    fun setIsPortrait(isPortrait: Boolean){
        this.isPortrait = isPortrait
    }
    fun setDirection(direction: Direction) {
        this.direction = direction
    }
    override fun getMatrix(): Matrix {
        return mat
    }
    fun setShapes(shapes: MutableList<Shape>){
        this.shapes = shapes
    }
    fun setShape(shape: Shape?){
        this.shape = shape
    }
    fun setAxis(axis: Axis){
        this.axis = axis
    }
    /*var zoom: Float
        get(){
            return scaleFactor * 100
        }
        set(value) {
            setScale(value/100)
        }*/

    fun resetViewState(){
        mat.reset()
        scaleFactor = 1f
        //scaleDetector = ScaleGestureDetector(context, ScaleListener())
        //gestureDetector = GestureDetector(context, GestureListener())
        previous = PointF()
        shapes = mutableListOf()
        axis = null
        shape = null
        permissions = arrayOf()
        direction = Direction.TOUCH
        //handler = Handler(Looper.getMainLooper())
    }
    private fun adjustScale(imageWidth: Float,imageHeight: Float){
        intrinsicWidth = imageWidth
        intrinsicHeight = imageHeight
        axis?.apply {
            points = mutableListOf(PointF(), PointF(intrinsicWidth,intrinsicHeight))
            visible = false
        }
        //Log.e("test", "(${intrinsicWidth},${intrinsicHeight}),(${width.toFloat()},${height.toFloat()})")
        setScale(height/intrinsicHeight,0f,0f)
    }

    override fun setImageDrawable(drawable: Drawable?) {
        super.setImageDrawable(drawable)
        drawable?.let {
            this.post {
                //Log.e("test", "1(${drawable.intrinsicWidth},${drawable.intrinsicHeight}),(${width},${height})")
                if (width > 0 && height > 0 && drawable.intrinsicWidth > 0 && drawable.intrinsicHeight > 0) {
                    disabled = false
                    /*val scale = if(adjustScale) {
                        if(isPortrait) height/intrinsicHeight else width/drawable.intrinsicWidth
                    } else scaleFactor*/
                    adjustScale(drawable.intrinsicWidth.toFloat(),drawable.intrinsicHeight.toFloat())
                }
            }
        }
    }
    /*override fun setImageBitmap(bm: Bitmap?) {
        super.setImageBitmap(bm)
    }*/
    /*override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        Log.e("AttachedToWindow", "image: ${imageViewWidth},${imageViewHeight}")
        adjustImageToScreenHeight()
    }
    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        super.onSizeChanged(width, height, oldWidth, oldHeight)
        viewWidth = width
        viewHeight = height
    }*/
    private fun translateMatrix(dx: Float,dy: Float): PointF {
        val currentTransX = mat.transX
        val currentTransY = mat.transY

        val imageWidth = mat.transformOnX(intrinsicWidth)
        val imageHeight = mat.transformOnY(intrinsicHeight)

        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()

        val maxTransX = 0f
        val minTransX = viewWidth - imageWidth

        val maxTransY = 0f
        val minTransY = viewHeight - imageHeight

        // Asegurarse de que la imagen no se salga de los límites si es más grande que la vista de lo contrario moverlo al centro
        val adjustTransX = if(minTransX > 0) minTransX/2 - currentTransX else if (currentTransX + dx > maxTransX) maxTransX - currentTransX else if (currentTransX + dx < minTransX) minTransX - currentTransX else dx
        val adjustTransY = if(minTransY > 0) minTransY/2 - currentTransY else if (currentTransY + dy > maxTransY) maxTransY - currentTransY else if (currentTransY + dy < minTransY) minTransY - currentTransY else dy
        mat.postTranslate(adjustTransX, adjustTransY)
        return PointF(adjustTransX, adjustTransY)
    }
    private fun setScale(scaleFactor: Float,focusX: Float,focusY: Float) {
        if(!containsAnyPermissions(arrayOf(Permission.SCALE,Permission.ALL))) return
        onScaleChangedListener?.invoke(scaleFactor)
        val scale = mat.untransformOnX(scaleFactor)
        mat.postScale(scale, scale, focusX, focusY)
        this.scaleFactor = scaleFactor
        translateMatrix(0f,0f)
        invalidate()
    }

    fun getPointsFromView(x: Float,y: Float): Array<PointF> {
        return arrayOf(mat.untransformPoint(x,y), PointF(x,y))
    }

    private fun handleTouchEvent(event: MotionEvent) {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                previous.x = event.x
                previous.y = event.y
                //isPressed = true
                //handler.postDelayed(longPressRunnable, 500)
                handler.post(callbackRunnable)
                if(containsAnyPermissions(arrayOf(Permission.MOUSEDOWN,Permission.ALL))) {
                    onMouseDownListener?.invoke(getPointsFromView(event.x,event.y))
                    invalidate()
                }
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - previous.x
                val dy = event.y - previous.y
                if(!isMouseMoved && containsAnyPermissions(arrayOf(Permission.DRAGSTART,Permission.ALL))){
                    onDragStartListener?.invoke(getPointsFromView(previous.x,previous.y))
                }
                isMouseMoved = true
                if(containsAnyPermissions(arrayOf(Permission.DRAGMOVE,Permission.ALL))) {
                    val translate = if(direction == Direction.TOUCH) translateMatrix(dx, dy)
                    else translateMatrix(-dx, -dy)
                    if(direction == Direction.TOUCH){

                    }
                    else{
                        translate.x = if(translate.x == 0f) dx else dx-translate.x
                        translate.y = if(translate.y == 0f) dy else dy-translate.y
                    }
                    onDragMoveListener?.invoke(getPointsFromView(event.x,event.y)+ PointF(mat.untransformOnX(translate.x),mat.untransformOnX(translate.y)))
                    invalidate()
                }
                previous.x = event.x
                previous.y = event.y
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                //isPressed = false
                if(!isMouseMoved){
                    if(containsAnyPermissions(arrayOf(Permission.MOUSEUP,Permission.ALL))) {
                        onMouseUpListener?.invoke(getPointsFromView(event.x,event.y))
                        invalidate()
                    }
                }
                else{
                    if(containsAnyPermissions(arrayOf(Permission.DRAGEND,Permission.ALL))) {
                        onDragEndListener?.invoke(getPointsFromView(event.x,event.y))
                        invalidate()
                    }
                }
                isMouseMoved = false
                //handler.removeCallbacks(longPressRunnable)
                handler.removeCallbacks(callbackRunnable)
            }
        }
    }
    override fun performClick(): Boolean {
        super.performClick()
        onClickListener?.invoke()
        return true
    }

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val scaleFactor = detector.scaleFactor
            setScale(scaleFactor * this@ZoomImageView.scaleFactor,detector.focusX,detector.focusY)
            return true
        }
    }

    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            if(containsAnyPermissions(arrayOf(Permission.SINGLETAP,Permission.ALL))){
                onSingleTapListener?.invoke(getPointsFromView(e.x,e.y))
                invalidate()
            }
            return true
        }
        override fun onDoubleTap(e: MotionEvent): Boolean {
            if(containsAnyPermissions(arrayOf(Permission.DOUBLETAP,Permission.ALL))){
                onDoubleTapListener?.invoke(getPointsFromView(e.x,e.y))
                invalidate()
            }
            return true
        }
    }

    override fun onDraw(canvas: Canvas) {
        canvas.save()
        canvas.concat(mat)
        super.onDraw(canvas)

        axis?.let {
            it.handleOnDrawListener(canvas)
            it.draw(canvas)
        }
        for(shape in shapes) {
            shape.handleOnDrawListener(canvas)
            shape.draw(canvas)
        }
        shape?.let {
            it.handleOnDrawListener(canvas)
            it.draw(canvas)
        }
        canvas.restore()
    }

    enum class Direction {
        TOUCH, MOUSE
    }
    enum class Permission {
        SCALE, CLICK, MOUSEDOWN, MOUSEUP, PRESS, DRAGSTART, DRAGMOVE, DRAGEND, SINGLETAP, DOUBLETAP, ALL
    }
    /*class Shape(val type: ShapeType) {
        var points: MutableList<PointF> = mutableListOf()

        /*fun clone(): Shape {
            return Shape(type).apply {
                points = this@Shape.points.map { PointF(it.x, it.y) }.toMutableList()
            }
        }*/
        companion object {
            val RECT = 1
            val POLYGON = 2
        }
    }*/
}
/*abstract class Shape {
    abstract val type: ShapeType
    val paint = Paint().apply {
        color = Color.GREEN
        strokeWidth = 5f
        style = Paint.Style.STROKE
    }
    var drawFill = true
    var alphaFactor = 0.5f
    var visible = true
    var points: MutableList<PointF> = mutableListOf()
    var paintStrokeWidth = paint.strokeWidth

    protected var _onDrawListener: ((Canvas, Matrix) -> Unit)? = null
    //private var fixedSizeValue: ((Float, Matrix) -> Unit)? = null

    abstract fun draw(canvas: Canvas)
    //fun setStrokeWidth(strokeWidth: Float){
    //    paintStrokeWidth = strokeWidth
    //}
    open fun addPoint(point: PointF){
        points.add(point)
    }
    open fun removePointAt(index: Int){
        points.removeAt(index)
    }
    fun setOnDrawListener(onDrawListener: ((Canvas, Matrix) -> Unit)?){
        _onDrawListener = onDrawListener
    }
    open fun handleOnDrawListener(canvas: Canvas, mat: Matrix){
        paint.strokeWidth = paintStrokeWidth/ ZoomImageView.getMatrixValues(mat)[Matrix.MSCALE_X]
        _onDrawListener?.invoke(canvas,mat)
    }

    fun getTransparentColor(color: Int, alphaFactor: Float): Int {
        val alpha = (Color.alpha(color) * alphaFactor).toInt()
        val red = Color.red(color)
        val green = Color.green(color)
        val blue = Color.blue(color)
        return Color.argb(alpha, red, green, blue)
    }
    fun getFillPaint(): Paint {
        return Paint().apply {
            color = getTransparentColor(paint.color,alphaFactor)
            style = Paint.Style.FILL
        }
    }
}
abstract class BasicShape: Shape() {
    var vertices: MutableList<Vertex> = mutableListOf()
    var label = ""
    private var _vertexSide = 30f
    var _fixedVertexSide = false
    var _showVertices = false
    //var transformedVertexSide = vertexSide
    //private var fixedSizeValue: ((Float, Matrix) -> Unit)? = null
    var vertexSide: Float
        get() = _vertexSide
        set(value) {
            _vertexSide = value
            for(vertex in vertices) vertex.originalSide = value
        }
    var fixedVertexSide: Boolean
        get() = _fixedVertexSide
        set(value) {
            _fixedVertexSide = value
            for(vertex in vertices) vertex.fixedSide = value
        }
    var showVertices: Boolean
        get() = _showVertices
        set(value) {
            _showVertices = value
            for(vertex in vertices) vertex.visible = value
        }

    override fun handleOnDrawListener(canvas: Canvas, mat: Matrix){
        super.handleOnDrawListener(canvas, mat)
        _onDrawListener?.invoke(canvas,mat)
        for(vertex in vertices) vertex.handleOnDrawListener(canvas, mat)
    }
    override fun addPoint(point: PointF){
        super.addPoint(point)
        vertices.add(Vertex().apply {
            addPoint(point)
            originalSide = _vertexSide
            fixedSide = _fixedVertexSide
        })
    }
    override fun removePointAt(index: Int){
        points.removeAt(index)
        vertices.removeAt(index)
    }
}
class Axis: Shape() {
    override val type = ShapeType.AXIS
    init {
        paint.apply {
            color = Color.DKGRAY
            strokeWidth = 2f
            style = Paint.Style.STROKE
        }
        //points = mutableListOf(PointF(), PointF())
    }
    override fun draw(canvas: Canvas) {
        if(!visible) return
        val center = points[0]
        val end = points[1]
        canvas.drawLine(0f, center.y, end.x, center.y, paint)
        canvas.drawLine(center.x, 0f, center.x, end.y , paint)
    }
}
class Vertex: Shape() {
    override val type = ShapeType.VERTEX
    var side = 30f
    var originalSide = side
    var fixedSide = true

    init {
        paint.apply {
            color = Color.parseColor("#FFA500")
            strokeWidth = 3f
            style = Paint.Style.STROKE
        }
    }
    fun getRect(): List<PointF> {
        val center = points[0]
        val start = PointF(center.x-side/2,center.y-side/2)
        val end = PointF(center.x+side/2,center.y+side/2)
        return listOf(start,end)
    }
    override fun handleOnDrawListener(canvas: Canvas, mat: Matrix){
        super.handleOnDrawListener(canvas, mat)
        side = if(fixedSide) originalSide/ZoomImageView.getMatrixValues(mat)[Matrix.MSCALE_X] else originalSide
        _onDrawListener?.invoke(canvas,mat)
    }
    override fun draw(canvas: Canvas) {
        if(!visible) return
        val pts = getRect()
        val start = pts[0]
        val end = pts[1]
        canvas.drawRect(start.x,start.y,end.x,end.y,paint)
        if(drawFill) canvas.drawRect(start.x,start.y,end.x,end.y,getFillPaint())
    }
}
class Rect: BasicShape() {
    override val type = ShapeType.RECT
    init {

    }
    override fun draw(canvas: Canvas) {
        if(visible && points.size == 2){
            val start = points[0]
            val end = points[1]
            canvas.drawRect(start.x,start.y,end.x,end.y,paint)
            if(drawFill) canvas.drawRect(start.x,start.y,end.x,end.y,getFillPaint())
        }
        for(vertex in vertices) vertex.draw(canvas)
    }
}
class Polygon: BasicShape() {
    override val type = ShapeType.POLYGON
    init {

    }
    override fun draw(canvas: Canvas) {
        if(visible){
            val path = Path()
            val first = points[0]
            path.moveTo(first.x, first.y)
            for (i in 1 until points.size) {//point in points.drop(1)
                val point = points[i]
                path.lineTo(point.x, point.y)
            }
            path.close()
            canvas.drawPath(path, paint)
            if(drawFill) canvas.drawPath(path, getFillPaint())
        }
        for(vertex in vertices) vertex.draw(canvas)
    }
}

class ShapeFactory {
    companion object {
        fun create(type: ShapeType): Shape? {
            return when (type) {
                ShapeType.AXIS -> Axis()
                ShapeType.VERTEX -> Vertex()
                ShapeType.RECT -> Rect()
                ShapeType.POLYGON -> Polygon()
            }
        }
        fun createBasic(type: ShapeType): BasicShape? {
            return when (type) {
                ShapeType.RECT -> Rect()
                ShapeType.POLYGON -> Polygon()
                else -> null
            }
        }
    }
}

enum class ShapeType {
    AXIS, VERTEX, RECT, POLYGON
}*/