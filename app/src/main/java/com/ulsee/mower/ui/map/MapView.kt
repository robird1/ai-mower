package com.ulsee.mower.ui.map

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.core.view.isVisible
import com.ulsee.mower.R

private val TAG = MapView::class.java.simpleName

private const val LAWN_WIDTH = 3000         // unit: cm
private const val LAWN_HEIGHT = 2500

class MapView@JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // charging station
    private lateinit var chargingStationBitmap: Bitmap
    private var chargingStationPixelOffset = PointF(0f, 0f)
    private var chargingStationPosition : PointF? = null

    // working start point
    private lateinit var workingStartPointBitmap: Bitmap
    private var workingStartPointPixelOffset = PointF(0f, 0f)
    private var workingStartPointPosition : PointF? = null

    // working border point
    private lateinit var workingBorderPointBitmap: Bitmap
    private var workingBorderPointPixelOffset = PointF(0f, 0f)

    // obstacle
    private lateinit var obstacleBitmap: Bitmap
    private var obstacleBitmapPixelOffset = PointF(0f, 0f)

    // robot
    private lateinit var robotBitmap: Bitmap
    private lateinit var robotRotatedBitmap: Bitmap
    private var robotPixelOffset = PointF(0f, 0f)
    private var robotPixelPosition = PointF(0f, 0f)
    private var robotCoordinateX = 0
    private var robotCoordinateY = 0
//    private var robotAngle = 0F
    private var xScale = 0F
    private var yScale = 0F
    private var scaleFactor = 1F
    private val pixelOrigin = PointF(0f, 0f)
    private val axisOffset = PointF(0f, 0f)

    private val borderPathPixels = ArrayList<PointF>()
    private val borderPointPixels = ArrayList<PointF>()
    private val chargingPathPixels = ArrayList<PointF>()
    private val obstaclePointPixels = ArrayList<PointF>()

    private val borderPathCoordinates = ArrayList<PointF>()
    private val borderPointCoordinates = ArrayList<PointF>()
    private val chargingPathCoordinates = ArrayList<PointF>()
    private val obstaclePointCoordinates = ArrayList<PointF>()
    private var workingStartPointCoordinate = PointF(0f, 0f)


    private var workingPath = Path()
    private var chargingPath = Path()
    private var obstaclePath = Path()
    private lateinit var workingBorderPaint: Paint
    private lateinit var chargingPathPaint: Paint
    private lateinit var obstacleBorderPaint: Paint

    var mode = Mode.Drive
    enum class Mode {
        SetPoint, Drive
    }

//    enum class OperationType {
//        None, ChargingPath, WorkingBorder, Obstacle
//    }

    init {
        // Setting the view's isClickable property to true enables that view to accept user input.
//        isClickable = true

        initChargingStationBitmap()
        initWorkingStartPointBitmap()
        initWorkingBorderPointBitmap()
        initObstacleBitmap()
        initRobotBitmap()
        initWorkingBorderPaint()
        initChargingPathPaint()
        initObstacleBorderPaint()
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)

        calculateScale()
        robotPixelPosition.x = getStartPositionX()
        robotPixelPosition.y = getStartPositionY()

//        Log.d(TAG, "[Enter] onLayout() robotPixelLocation.x: ${robotPixelLocation.x} robotPixelLocation.y: ${robotPixelLocation.y}")
    }

    override fun onDraw(canvas: Canvas?) {
//        Log.d(TAG, "[Enter] onDraw() robotPixelLocation.x: ${robotPixelLocation.x} robotPixelLocation.y: ${robotPixelLocation.y}")

        canvas?.apply {
            drawChargingPath()
            drawBorderPath()
            drawBorderPoint()
            drawObstaclePoint()
            drawObstacleBorder()
            drawRobotPosition()
            drawChargingStation()
            drawWorkingStartPoint()
        }
    }

    private fun Canvas.drawWorkingStartPoint() {
        workingStartPointPosition?.let { location ->
            drawBitmap(
                workingStartPointBitmap,
                location.x - workingStartPointPixelOffset.x,
                location.y - workingStartPointPixelOffset.y,
                Paint()
            )
        }
    }

    private fun Canvas.drawChargingPath() {
        chargingStationPosition?.let {
            chargingPath.reset()

            chargingPath.moveTo(it.x, it.y)

            for (idx in 0 until chargingPathPixels.size) {
                chargingPath.lineTo(chargingPathPixels[idx].x, chargingPathPixels[idx].y)
            }
            drawPath(chargingPath, chargingPathPaint)
        }
    }

    private fun Canvas.drawBorderPath() {
        workingStartPointPosition?.let {
            workingPath.reset()

            workingPath.moveTo(it.x, it.y)
//            Log.d(TAG, "[Enter] drawBorderPath() workingPath.moveTo(it.x, it.y) x: ${it.x} y: ${it.y}")
//
//            Log.d(TAG, "[Enter] drawBorderPath(): ${borderPathData.size}")
            for (idx in 0 until borderPathPixels.size) {
//                Log.d(TAG, "[Enter] workingPath.lineTo() x: ${borderPathData[idx].x} y: ${borderPathData[idx].y}")

                workingPath.lineTo(borderPathPixels[idx].x, borderPathPixels[idx].y)
            }

            drawPath(workingPath, workingBorderPaint)
        }
    }

    private fun Canvas.drawBorderPoint() {
        for (idx in 0 until borderPointPixels.size) {
            drawBitmap(
                workingBorderPointBitmap,
                borderPointPixels[idx].x - workingBorderPointPixelOffset.x,
                borderPointPixels[idx].y - workingBorderPointPixelOffset.y * 2,
                Paint()
            )
        }
    }

    private fun Canvas.drawObstaclePoint() {
        for (idx in 0 until obstaclePointPixels.size) {
            drawBitmap(
                obstacleBitmap,
                obstaclePointPixels[idx].x - obstacleBitmapPixelOffset.x,
                obstaclePointPixels[idx].y - obstacleBitmapPixelOffset.y * 2,
                Paint()
            )
        }
    }

    private fun Canvas.drawObstacleBorder() {
//        workingStartPointPosition?.let {
        if (obstaclePointPixels.size > 0) {
            obstaclePath.moveTo(obstaclePointPixels[0].x, obstaclePointPixels[0].y)
            for (idx in 0 until obstaclePointPixels.size) {
                obstaclePath.lineTo(obstaclePointPixels[idx].x, obstaclePointPixels[idx].y)
            }
            drawPath(obstaclePath, obstacleBorderPaint)
        }
//        }

    }

    private fun Canvas.drawRobotPosition() {
        drawBitmap(
            robotRotatedBitmap,
            robotPixelPosition.x - robotPixelOffset.x,
            robotPixelPosition.y - robotPixelOffset.y,
            Paint()
        )
    }

    private fun Canvas.drawChargingStation() {
        chargingStationPosition?.let { location ->
            drawBitmap(
                chargingStationBitmap,
                location.x - chargingStationPixelOffset.x,
                location.y  - chargingStationPixelOffset.y * 2,
                Paint()
            )
        }
    }

    private fun getStartPoint() = PointF(getStartPositionX(), getStartPositionY())

    private fun calculateScale() {
        xScale = width.toFloat() / (LAWN_WIDTH * scaleFactor)
        yScale = height.toFloat() / (LAWN_HEIGHT * scaleFactor)
        Log.d(TAG, "[Enter] calculateScale() xScale: $xScale yScale: $yScale")
    }

    private fun getStartPositionY() = height / 2f

    private fun getStartPositionX() = width / 2f

    /**
     * pixelOrigin : 自定義的座標原點 (0, 0)
     * robotCoordinate : 機器人停泊在充電站上的座標 (機器人本身的座標系)
     */
    private fun calculateAxisOffset() {
        axisOffset.x = pixelOrigin.x - robotCoordinateX
        axisOffset.y = pixelOrigin.y - robotCoordinateY
    }

    private fun getPixelPosition(coordinateX: Float, coordinateY: Float) = Pair(
        (coordinateX + axisOffset.x) * xScale + width / 2f,
        -(coordinateY + axisOffset.y) * yScale + height / 2f
    )

    private fun isWithinCanvasBound(): Boolean {
        return !(robotPixelPosition.x >= width || robotPixelPosition.x <= 0 ||
                robotPixelPosition.y >= height || robotPixelPosition.y <= 0)
    }

    fun changeMode(mode: Mode) {
        this.mode = mode
    }

    fun notifyRobotCoordinate(x: Int, y: Int, angle: Float = 0f, state: SetupMapState) {
//        Log.d(TAG, "[Enter] notifyRobotCoordinate()")
        val pair = getPixelPosition(x.toFloat(), y.toFloat())
        robotPixelPosition.x = pair.first
        robotPixelPosition.y = pair.second

        if (!isWithinCanvasBound()) {
            clearCanvasData()

            scaleFactor *= 1.2F
            calculateScale()

            recalculateCanvasData()

            return
        }

        if (robotPixelPosition.y > (height - dp2px(123))) {
//            state.binding.footerViewStep3.alpha = 0.2f
            state.binding.step3Instruction.isVisible = false
        }

        if (mode == Mode.Drive) {
            if (state is SetWorkingBoundary) {
                borderPathPixels.add(PointF(robotPixelPosition.x, robotPixelPosition.y))
                borderPathCoordinates.add(PointF(x.toFloat(), y.toFloat()))
//            Log.d(TAG, "[Enter] updateRobotPosition() robotPixelLocation.x: ${robotPixelLocation.x} robotPixelLocation.y: ${robotPixelLocation.y}")

            } else if (state is SetObstacle) {
                obstaclePointPixels.add(PointF(robotPixelPosition.x, robotPixelPosition.y))
                obstaclePointCoordinates.add(PointF(x.toFloat(), y.toFloat()))

            } else if (state is FinishWorkingBoundary) {

            } else {
                // do nothing
            }
        }
        robotRotatedBitmap = rotateBitmap(robotBitmap, angle)

        robotCoordinateX = x
        robotCoordinateY = y

        postInvalidate()
    }

    private fun recalculateCanvasData() {
        for (data in borderPathCoordinates) {
            val pair = getPixelPosition(data.x, data.y)
    //                Log.d(TAG, "[Enter] point.x: ${pair.first} point.y: ${pair.second}")
            borderPathPixels.add(PointF(pair.first, pair.second))
        }
        for (data in borderPointCoordinates) {
            val pair = getPixelPosition(data.x, data.y)
            borderPointPixels.add(PointF(pair.first, pair.second))
        }
        for (data in chargingPathCoordinates) {
            val pair = getPixelPosition(data.x, data.y)
            chargingPathPixels.add(PointF(pair.first, pair.second))
        }
        for (data in obstaclePointCoordinates) {
            val pair = getPixelPosition(data.x, data.y)
            obstaclePointPixels.add(PointF(pair.first, pair.second))
        }
        workingStartPointPosition?.let { it ->
            val pair = getPixelPosition(workingStartPointCoordinate.x, workingStartPointCoordinate.y)
            it.x = pair.first
            it.y = pair.second
        }
    }

    private fun clearCanvasData() {
        borderPathPixels.clear()
        borderPointPixels.clear()
        chargingPathPixels.clear()
        obstaclePointPixels.clear()
        workingPath.reset()
        chargingPath.reset()
        obstaclePath.reset()
    }

    fun showChargingStation() {
        chargingStationPosition = PointF(
            width / 2f,
            height / 2f
        )

        // place robot at the default position of charging station
        robotPixelPosition.x = width / 2f
        robotPixelPosition.y = height / 2f - robotPixelOffset.y * 2

        calculateAxisOffset()

        postInvalidate()
    }

    fun showWorkingStartPoint() {
        workingStartPointPosition = PointF(
            robotPixelPosition.x,
            robotPixelPosition.y
        )
        chargingPathPixels.add(PointF(workingStartPointPosition!!.x, workingStartPointPosition!!.y))
        chargingPathCoordinates.add(PointF(robotCoordinateX.toFloat(), robotCoordinateY.toFloat()))
        workingStartPointCoordinate = PointF(robotCoordinateX.toFloat(), robotCoordinateY.toFloat())
        postInvalidate()
    }

    fun setWorkingBoundaryPoint() {
        borderPointPixels.add(PointF(robotPixelPosition.x, robotPixelPosition.y))
        borderPointCoordinates.add(PointF(robotCoordinateX.toFloat(), robotCoordinateY.toFloat()))
        borderPathPixels.add(PointF(robotPixelPosition.x, robotPixelPosition.y))
        borderPathCoordinates.add(PointF(robotCoordinateX.toFloat(), robotCoordinateY.toFloat()))
        postInvalidate()
    }

    fun finishWorkingBorder() {
        workingStartPointPosition?.let {
            workingPath.setLastPoint(it.x, it.y)
            workingBorderPaint.style = Paint.Style.FILL_AND_STROKE
            workingBorderPaint.alpha = 150
            borderPointPixels.clear()
            postInvalidate()
        }
    }

    fun setObstaclePoint() {
        obstaclePointPixels.add(PointF(robotPixelPosition.x, robotPixelPosition.y))
        obstaclePointCoordinates.add(PointF(robotCoordinateX.toFloat(), robotCoordinateY.toFloat()))
        postInvalidate()
    }

    fun finishObstacleBorder() {
//        workingStartPointPosition?.let {
            obstaclePath.setLastPoint(obstaclePointPixels[0].x, obstaclePointPixels[0].y)
            obstacleBorderPaint.style = Paint.Style.FILL_AND_STROKE
            obstacleBorderPaint.alpha = 150
//            borderPointData.clear()
            postInvalidate()
//        }
    }

    private fun initChargingStationBitmap() {
        chargingStationBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.ic_chargingstation_s)
        chargingStationPixelOffset.x = chargingStationBitmap.width / 2f
        chargingStationPixelOffset.y = chargingStationBitmap.height / 2f
    }

    private fun initWorkingStartPointBitmap() {
        workingStartPointBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.ic_mowing_point_s)
        workingStartPointPixelOffset.x = workingStartPointBitmap.width / 2f
        workingStartPointPixelOffset.y = workingStartPointBitmap.height / 2f
    }

    private fun initWorkingBorderPointBitmap() {
        workingBorderPointBitmap = BitmapFactory.decodeResource(context.resources, R.drawable. ic_boundary_point_s)
        workingBorderPointPixelOffset.x = workingStartPointBitmap.width / 2f
        workingBorderPointPixelOffset.y = workingStartPointBitmap.height / 2f
    }

    private fun initObstacleBitmap() {
        obstacleBitmap = BitmapFactory.decodeResource(context.resources, R.drawable. ic_mowing_obstacle_s)
        obstacleBitmapPixelOffset.x = obstacleBitmap.width / 2f
        obstacleBitmapPixelOffset.y = obstacleBitmap.height / 2f
    }

    private fun initRobotBitmap() {
        robotBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.ic_map_handyman)
        robotRotatedBitmap = robotBitmap
        robotPixelOffset.x = robotBitmap.width / 2f
        robotPixelOffset.y = robotBitmap.height / 2f
    }

    private fun initWorkingBorderPaint() {
        workingBorderPaint = Paint()
        workingBorderPaint.style = Paint.Style.STROKE
        workingBorderPaint.strokeWidth = dp2px(3)
        workingBorderPaint.isAntiAlias = true
        workingBorderPaint.color = Color.parseColor("#00D91D")
    }

    private fun initChargingPathPaint() {
        chargingPathPaint = Paint()
        chargingPathPaint.style = Paint.Style.STROKE
        chargingPathPaint.strokeWidth = dp2px(3)
        chargingPathPaint.isAntiAlias = true
        chargingPathPaint.color = Color.parseColor("#00D91D")
    }

    private fun initObstacleBorderPaint() {
        obstacleBorderPaint = Paint()
        obstacleBorderPaint.style = Paint.Style.STROKE
        obstacleBorderPaint.strokeWidth = dp2px(2)
        obstacleBorderPaint.isAntiAlias = true
        obstacleBorderPaint.color = Color.parseColor("#AB0A0A")
    }

    private fun dp2px(dp:Int):Float= dp * context.resources.displayMetrics.density

    private fun rotateBitmap(origin: Bitmap, alpha: Float): Bitmap {
        val width = origin.width
        val height = origin.height
        val matrix = Matrix()
        matrix.setRotate(alpha)
        return Bitmap.createBitmap(origin, 0, 0, width, height, matrix, true)
    }

    //    fun isWithinCanvasBound2() {
//        borderPathData.clear()
//        borderPointData.clear()
//        chargingPathData.clear()
//        obstaclePointData.clear()
//
//        xScale = width.toFloat() / (LAWN_WIDTH * 1.5f)
//        yScale = height.toFloat() / (LAWN_HEIGHT * 1.5f)
//
//        Log.d(TAG, "[Enter] isWithinCanvasBound() xScale: $xScale yScale: $yScale")
//
////            var point = PointF(0f, 0f)
//        for (data in borderPathDataX) {
//            val point = PointF(0f, 0f)
//
//            point.x = (data.x + axisOffset.x) * xScale + width / 2f
//            point.y = -(data.y + axisOffset.y) * yScale + height / 2f
//
//            Log.d(TAG, "[Enter] point.x: ${point.x} point.y: ${point.y}")
//
//            borderPathData.add(point)
//        }
//        for (data in borderPointDataX) {
//            val point = PointF(0f, 0f)
//
//            point.x = (data.x + axisOffset.x) * xScale + width / 2f
//            point.y = -(data.y + axisOffset.y) * yScale + height / 2f
//            borderPointData.add(point)
//        }
//        for (data in chargingPathDataX) {
//            val point = PointF(0f, 0f)
//
//            point.x = (data.x + axisOffset.x) * xScale + width / 2f
//            point.y = -(data.y + axisOffset.y) * yScale + height / 2f
//            chargingPathData.add(point)
//        }
//        for (data in obstaclePointDataX) {
//            val point = PointF(0f, 0f)
//
//            point.x = (data.x + axisOffset.x) * xScale + width / 2f
//            point.y = -(data.y + axisOffset.y) * yScale + height / 2f
//            obstaclePointData.add(point)
//        }
//        workingStartPointPosition?.let { it ->
//            it.x = (workingStartPointX.x + axisOffset.x) * xScale + width / 2f
//            it.y = -(workingStartPointX.y + axisOffset.y) * yScale + height / 2f
//        }
//        workingPath.reset()
//        chargingPath.reset()
//        obstaclePath.reset()
//
//    }


//    fun updateRobotPosition2(x: Int, y: Int, angle: Float = 0f, type: OperationType) {
//        robotPixelLocation.x += x * xScale
//        robotPixelLocation.y -= y * yScale
//
//        when (type) {
//            OperationType.WorkingBorder -> borderPathData.add(PointF(robotPixelLocation.x, robotPixelLocation.y))
//            OperationType.Obstacle -> obstaclePointData.add(PointF(robotPixelLocation.x, robotPixelLocation.y))
//        }
//
//        robotRotatedBitmap = rotateBitmap(robotBitmap, angle)
//
//        postInvalidate()
//    }


//    override fun performClick(): Boolean {
//        if (super.performClick()) return true
//
//        fanSpeed = fanSpeed.next()
//        contentDescription = resources.getString(fanSpeed.label)
//
//        invalidate()
//        return true
//    }


}