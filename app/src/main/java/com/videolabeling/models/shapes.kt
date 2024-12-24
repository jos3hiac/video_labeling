package com.videolabeling.models

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import androidx.core.graphics.values

fun generateColors(nLabel: Int): List<Int> {
    // Crear índices iniciales
    val indices = (0 until nLabel).map { it.toUByte() }
    val bitShifts = (0 until 24 step 3).toList()
    // Función auxiliar para obtener el bit en una posición específica
    fun bitGet(value: UByte, bitPosition: Int): Boolean {
        return (value.toInt() shr bitPosition and 1) == 1
    }
    // Crear el mapa de colores
    val colorMap = indices.map { index ->
        val expandedIndex = bitShifts.map { (index.toInt() shr it).toUByte() }
        val bitIndices = (7 downTo 0).toList()
        // Generar valores R, G, B
        val r = bitIndices.fold(0) { acc, bitIdx ->
            acc or ((if (bitGet(expandedIndex[0], 0)) 1 else 0) shl bitIdx)
        }
        val g = bitIndices.fold(0) { acc, bitIdx ->
            acc or ((if (bitGet(expandedIndex[0], 1)) 1 else 0) shl bitIdx)
        }
        val b = bitIndices.fold(0) { acc, bitIdx ->
            acc or ((if (bitGet(expandedIndex[0], 2)) 1 else 0) shl bitIdx)
        }
        // Combinar R, G, B en un solo entero (0xRRGGBB)
        //(r shl 16) or (g shl 8) or b
        Color.rgb(r,g,b)
    }
    return colorMap.drop(1) // Retornar desde el segundo color en adelante
}

fun isPointInRect(targetPoint: PointF,startPoint: PointF, endPoint: PointF): Boolean {
    val left = minOf(startPoint.x, endPoint.x)
    val right = maxOf(startPoint.x, endPoint.x)
    val top = minOf(startPoint.y, endPoint.y)
    val bottom = maxOf(startPoint.y, endPoint.y)
    return targetPoint.x in left..right && targetPoint.y in top..bottom
}
fun isPointInPolygon(point: PointF, polygon: List<PointF>): Boolean {
    var intersections = 0
    val size = polygon.size

    // Variables del punto
    val px = point.x
    val py = point.y

    for (i in 0 until size) {
        val p1 = polygon[i]
        val p2 = polygon[(i + 1) % size]

        // Coordenadas de los vértices del lado actual
        val p1x = p1.x
        val p1y = p1.y
        val p2x = p2.x
        val p2y = p2.y

        // Comprobamos si el rayo cruza el lado del polígono
        if (p1y > py != p2y > py) {
            val intersectionX = p1x + (py - p1y) * (p2x - p1x) / (p2y - p1y)
            if (px < intersectionX) {
                intersections++
            }
        }
    }
    return intersections % 2 != 0
}
fun PointF.move(dx:Float, dy:Float): PointF {
    this.x += dx
    this.y += dy
    return this
}
fun PointF.moveNew(dx:Float, dy:Float): PointF {
    return PointF(this.x+dx,this.y+dy)
}
fun PointF.moveTo(x:Float, y:Float): PointF {
    this.x = x
    this.y = y
    return this
}

val Matrix.scaleX: Float
    get() = this.values()[Matrix.MSCALE_X]
val Matrix.scaleY: Float
    get() = this.values()[Matrix.MSCALE_Y]
val Matrix.transX: Float
    get() = this.values()[Matrix.MTRANS_X]
val Matrix.transY: Float
    get() = this.values()[Matrix.MTRANS_Y]
fun Matrix.getInverse(): Matrix {
    val inverse = Matrix()
    invert(inverse)
    return inverse
}
fun Matrix.transformOnX(number: Float): Float {
    val newMatrix = Matrix()
    newMatrix.setScale(scaleX, scaleX)
    val point = floatArrayOf(number, number)
    newMatrix.mapPoints(point)
    return point[0]
}
fun Matrix.transformOnY(number: Float): Float {
    val newMatrix = Matrix()
    newMatrix.setScale(scaleY, scaleY)
    val point = floatArrayOf(number, number)
    newMatrix.mapPoints(point)
    return point[0]
}
fun Matrix.transformPoint(point: PointF): PointF {
    return transformPoint(point.x,point.y)
}
fun Matrix.transformPoint(x: Float, y: Float): PointF {
    val array = floatArrayOf(x,y)
    this.mapPoints(array)
    return PointF(array[0],array[1])
}
fun Matrix.untransformOnX(number: Float): Float {
    return getInverse().transformOnX(number)
}
fun Matrix.untransformOnY(number: Float): Float {
    return getInverse().transformOnY(number)
}
fun Matrix.untransformPoint(point: PointF): PointF {
    return untransformPoint(point.x,point.y)
}
fun Matrix.untransformPoint(x: Float, y: Float): PointF {
    return getInverse().transformPoint(x,y)
}


class ScalarFloat(
    var matrix: Matrix,
    value: Float,
    fixed: Boolean = false,
    var onChange: ((Float,Boolean) -> Unit)? = null
){
    private var _value = value
    private var _fixed = fixed
    var value
        get() = getValue(_value)
        set(value) {
            _value = value
            onChange?.invoke(value,fixed)
        }
    var fixed
        get() = _fixed
        set(fixed) {
            _fixed = fixed
            onChange?.invoke(value,fixed)
        }

    init {
        //onChange?.invoke(_value,_fixed)
    }
    /*fun change(){
        onChange?.invoke(value,fixed)
    }*/
    private fun getValue(value: Float): Float {
        return if(fixed) matrix.untransformOnX(value) else value
    }
}

abstract class Paintable(
    protected var matrix: Matrix
){
    var visible = true

    protected val fillPaint = Paint().apply {
        style = Paint.Style.FILL
    }
    protected val strokePaint = Paint().apply {
        style = Paint.Style.STROKE
    }

    private var alpha = 1f
    private var bgColor = 0
    private var borderColor = 0
    private var color = 0
    private var selected = false

    val strokeWidth = ScalarFloat(matrix,5f,false)
    //var drawFill = true
    //var points: MutableList<PointF> = mutableListOf()
    protected var _onDrawListener: ((Canvas) -> Unit)? = null
    //private var fixedSizeValue: ((Float, Matrix) -> Unit)? = null

    abstract fun draw(canvas: Canvas)

    fun getBgColor(): Int {
        return bgColor
    }
    fun getBorderColor(): Int {
        return borderColor
    }
    fun getColor(): Int {
        return color
    }
    fun getSelected(): Boolean {
        return selected
    }
    fun setAlpha(alpha: Float = 0.5f){
        this.alpha = alpha
        setBgColor(color)
    }
    open fun setBgColor(bgColor: Int): Paintable {
        this.bgColor = bgColor
        fillPaint.color = getColorWithAlpha(bgColor,alpha)
        return this
    }
    open fun setBorderColor(borderColor: Int): Paintable {
        this.borderColor = borderColor
        strokePaint.color = borderColor
        return this
    }
    open fun setColor(color: Int): Paintable {
        this.color = color
        setBorderColor(color)
        setBgColor(color)
        return this
    }
    open fun setColorAndAlpha(color: Int, alpha: Float = 0.5f): Paintable {
        this.alpha = alpha
        setColor(color)
        return this
    }
    fun setSelected(selected: Boolean): Paintable {
        this.selected = selected
        return this
    }
    open fun setMatrix(matrix: Matrix): Paintable {
        this.matrix = matrix
        strokeWidth.matrix = matrix
        return this
    }
    fun setOnDrawListener(onDrawListener: ((Canvas) -> Unit)?){
        _onDrawListener = onDrawListener
    }

    open fun handleOnDrawListener(canvas: Canvas){
        _onDrawListener?.invoke(canvas)
    }
    private fun getColorWithAlpha(color: Int, alpha: Float): Int {
        val factor = (Color.alpha(color) * alpha).toInt()
        val red = Color.red(color)
        val green = Color.green(color)
        val blue = Color.blue(color)
        return Color.argb(factor, red, green, blue)
    }
}

class Vertex(matrix: Matrix,var center: PointF): Paintable(matrix) {
    val side = ScalarFloat(matrix,30f)

    fun contains(point: PointF): Boolean {
        val pts = getBbox()
        return isPointInRect(point,pts[0],pts[1])
    }
    fun getBbox(): List<PointF> {
        val halfside = getHalfSide()
        val start = PointF(center.x-halfside,center.y-halfside)
        val end = PointF(center.x+halfside,center.y+halfside)
        return listOf(start,end)
    }
    private fun getHalfSide(): Float {
        return 0.5f * side.value
    }
    private fun getRadius(): Float {
        return 0.4f * side.value
    }
    fun move(dx: Float,dy: Float): PointF {
        return center.move(dx,dy)
    }
    fun moveTo(x: Float,y: Float): PointF {
        return center.moveTo(x,y)
    }
    override fun draw(canvas: Canvas) {
        if(!visible) return
        val pts = getBbox()
        val start = pts[0]
        val end = pts[1]
        strokePaint.strokeWidth = strokeWidth.value
        if(!getSelected()){
            canvas.drawCircle(center.x,center.y,getRadius(),strokePaint)
            canvas.drawCircle(center.x,center.y,getRadius(),fillPaint)
        }
        else{
            canvas.drawRect(start.x,start.y,end.x,end.y,strokePaint)
            canvas.drawRect(start.x,start.y,end.x,end.y,fillPaint)
        }
    }
    override fun handleOnDrawListener(canvas: Canvas){
        super.handleOnDrawListener(canvas)
        _onDrawListener?.invoke(canvas)
    }
}
class Axis(matrix: Matrix): Paintable(matrix) {
    var points = mutableListOf<PointF>()
    init {
        setColor(Color.DKGRAY)
        //strokeWidth.value = 2f
    }
    override fun draw(canvas: Canvas) {
        if(!visible) return
        val center = points[0]
        val end = points[1]
        strokePaint.strokeWidth = strokeWidth.value
        canvas.drawLine(0f, center.y, end.x, center.y, strokePaint)
        canvas.drawLine(center.x, 0f, center.x, end.y , strokePaint)
    }
}
abstract class Shape(matrix: Matrix): Paintable(matrix){
    abstract val type: String
    private var label = ""
    val vertices = mutableListOf<Vertex>()
    val points = mutableListOf<PointF>()
    val vertexSide = ScalarFloat(matrix,30f,false){ value,fixed ->
        for(vertex in vertices){
            vertex.side.fixed = fixed
            vertex.side.value = value
        }
    }
    var showVertices = true
        get() = field
        set(value) {
            field = value
            for(vertex in vertices) vertex.visible = value
        }

    abstract fun contains(point: PointF, includeVertices: Boolean = true): Pair<Boolean, Int?>
    fun addPoint(point: PointF){
        points.add(point)
        val vertex = Vertex(matrix,point)
        vertex.side.fixed = vertexSide.fixed
        vertex.side.value = vertexSide.value
        vertex.visible = showVertices
        vertex.setColorAndAlpha(getColor(),1f)
        vertices.add(vertex)
    }

    fun getLabel(): String {
        return label
    }
    fun move(dx: Float = 0f,dy: Float = 0f){
        for(vertex in vertices) vertex.move(dx,dy)
    }
    fun movePoint(index: Int,dx: Float = 0f,dy: Float = 0f){
        vertices[index].move(dx,dy)
    }
    fun movePointTo(index: Int,point: PointF){
        vertices[index].moveTo(point.x,point.y)
    }
    fun removePoint(index: Int){
        points.removeAt(index)
        vertices.removeAt(index)
    }
    fun setLabel(label: String): Shape {
        this.label = label
        return this
    }
    /*override fun setMatrix(matrix: Matrix): Shape {
        super.setMatrix(matrix)
        vertexSide.matrix = matrix
        for(vertex in vertices) vertex.setMatrix(matrix)
        return this
    }*/
    fun setPoints(points: List<PointF>){
        clearPoints()
        for(point in points) addPoint(point)
    }
    fun setSelected(selected: Boolean,vertex:Int? = null): Shape {
        super.setSelected(selected)
        vertex?.let{
            vertices[it].setSelected(selected)
        }
        val borderColor = if(selected && vertex == null) Color.WHITE else getColor()
        val vertexBgColor = if(selected && vertex != null) Color.WHITE else getColor()
        setBorderColor(borderColor)
        for(vt in vertices){
            vt.setBorderColor(borderColor)
            vt.setBgColor(vertexBgColor)
        }
        return this
    }
    override fun setColor(color: Int): Shape {
        super.setColor(color)
        for(vt in vertices) vt.setColor(color)
        if(getSelected()) setSelected(true)
        return this
    }
    /*override fun setColorAndAlpha(color: Int, alpha: Float = 0.5f): Shape {
        super.setColorAndAlpha(color,alpha)
        for(vt in vertices) vt.setColorAndAlpha(color,alpha)
        if(getSelected()) setSelected(true)
        return this
    }*/
    private fun clearPoints(): Shape {
        points.clear()
        vertices.clear()
        return this
    }
    companion object {
        val RECT = "rectangle"
        val POLYGON = "polygon"
        val CIRCLE = "circle"
        fun create(type: String,matrix: Matrix): Shape? {
            return when (type) {
                RECT -> Rect(matrix)
                POLYGON -> Polygon(matrix)
                else -> null
            }
        }
    }
}
class Rect(matrix: Matrix): Shape(matrix) {
    override val type = RECT
    override fun contains(point: PointF, includeVertices: Boolean): Pair<Boolean, Int?> {
        if(includeVertices){
            for ((i,vertex) in vertices.withIndex()) {
                if(vertex.contains(point)) return Pair(true, i)
            }
        }
        return Pair(if(points.size == 2) isPointInRect(point,points[0],points[1]) else false,null)
    }
    override fun draw(canvas: Canvas) {
        if(visible && points.size == 2){
            val start = points[0]
            val end = points[1]
            strokePaint.strokeWidth = strokeWidth.value
            canvas.drawRect(start.x,start.y,end.x,end.y,strokePaint)
            if(getSelected()) canvas.drawRect(start.x,start.y,end.x,end.y,fillPaint)
        }
        for(vertex in vertices) vertex.draw(canvas)
    }
}

class Polygon(matrix: Matrix): Shape(matrix) {
    override val type = POLYGON
    override fun contains(point: PointF, includeVertices: Boolean): Pair<Boolean, Int?> {
        if(includeVertices){
            for ((i,vertex) in vertices.withIndex()) {
                if(vertex.contains(point)) return Pair(true, i)
            }
        }
        return Pair(isPointInPolygon(point,points),null)
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
            strokePaint.strokeWidth = strokeWidth.value
            canvas.drawPath(path,strokePaint)
            if(getSelected()) canvas.drawPath(path, fillPaint)
        }
        for(vertex in vertices) vertex.draw(canvas)
    }
}