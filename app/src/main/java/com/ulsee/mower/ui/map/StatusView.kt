package com.ulsee.mower.ui.map

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.View
import com.ulsee.mower.R
import com.ulsee.mower.data.MapData


private const val LAWN_WIDTH = 3000         // unit: cm
private const val LAWN_HEIGHT = 1500

private val TAG = SetupMapView::class.java.simpleName

class StatusView@JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    // charging station
    private lateinit var chargingStationBitmap: Bitmap
    private var chargingStationPixelOffset = PointF(0f, 0f)
    private var chargingStationPosition : PointF? = null
    private var chargingStationCoordinate : PointF? = null

    // working start point
    private lateinit var workingStartPointBitmap: Bitmap
    private var workingStartPointPixelOffset = PointF(0f, 0f)
    private var workingStartPointPosition : PointF? = null

    // working border point
    private lateinit var grassPointBitmap: Bitmap
    private var grassPointBitmapPixelOffset = PointF(0f, 0f)

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

    // 已存於割草機中的地圖數據或建圖過程中已確認的數據 (座標)
    private var confirmedGrass = HashMap<String, ArrayList<PointF>>()
    private var confirmedObstacle = HashMap<String, ArrayList<PointF>>()
    private var confirmedGrassRoute = HashMap<String, ArrayList<PointF>>()
    private var confirmedChargingRoute = HashMap<String, ArrayList<PointF>>()

    // 建圖過程中尚未結束記錄的座標數據
    private var mowingData = ArrayList<PointF>()


    private var state: SetupMapState? = null


    private var confirmedPathGrass = Path()
    private var confirmedPathObstacle = Path()
    private var confirmedPathGrassRoute = Path()
    private var confirmedPathCharging = Path()
    private var mowingAreaPath = Path()

    private lateinit var paintConfirmedGrass: Paint
    private lateinit var paintConfirmedCharging: Paint
    private lateinit var paintConfirmedObstacle: Paint
    private lateinit var paintConfirmedGrassRoute: Paint
    private lateinit var paintMowingArea: Paint

    private var xAxisOffset = 0F
    private var yAxisOffset = 0F

    private var graphCenter = PointF()

    init {
        initChargingStationBitmap()
        initWorkingStartPointBitmap()
        initGrassPointBitmap()
        initObstacleBitmap()
        initRobotBitmap()

        initGrassConfirmedPaint()
        initObstacleConfirmedPaint()
        initGrassRouteConfirmedPaint()
        initChargingConfirmedPaint()

        initData()
    }

    fun initData() {
        Log.d(TAG, "[Enter] initData()")
        confirmedGrass = MapData.grassData
        confirmedObstacle = MapData.obstacleData
        confirmedGrassRoute = MapData.grassPathData
        confirmedChargingRoute = MapData.chargingPathData

        Log.d("654", "confirmedGrass.size: ${confirmedGrass.size}")
        postInvalidate()
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        Log.d(TAG, "[Enter] onLayout()")
        super.onLayout(changed, left, top, right, bottom)

        calculateScale()
        robotPixelPosition.x = getStartPositionX()
        robotPixelPosition.y = getStartPositionY()
    }

    override fun onDraw(canvas: Canvas?) {
//        Log.d(TAG, "[Enter] onDraw()")
        if (confirmedGrass.size > 0) {
            graphCenter = getCenterOfGrass()
            xAxisOffset = graphCenter.x - width / 2
            yAxisOffset = graphCenter.y - height / 2
//            Log.d("123", "graphCenter.x: ${graphCenter.x} graphCenter.y: ${graphCenter.y}")
        }

        canvas?.apply {
            drawGrass()
            drawObstacle()
            drawGrassRoute()
            drawChargingRoute()
//            drawWorkingStartPoint()
            drawChargingStation()
            drawRobotPosition()
            drawMowingArea()
        }
    }

    fun notifyRobotCoordinate(x: Int, y: Int, angle: Float = 0f) {
//        Log.d("123", "[Enter] notifyRobotCoordinate() x: $x y: $y")
        val point = getPixelPosition(PointF(x.toFloat(), y.toFloat()))
        robotPixelPosition.x = point.x
        robotPixelPosition.y = point.y

        robotRotatedBitmap = rotateBitmap(robotBitmap, angle)
        robotCoordinateX = x
        robotCoordinateY = y

        postInvalidate()
    }

    fun updateMowingArea(data: ArrayList<PointF>) {
        mowingData = data
        postInvalidate()
    }

    private fun getPixelPosition(coordinatePoint: PointF) = PointF(
        (coordinatePoint.x) * xScale - xAxisOffset,
        (-coordinatePoint.y) * yScale - yAxisOffset
    )

    private fun calculateScale() {
//        xScale = width.toFloat() / (LAWN_WIDTH * scaleFactor)
        yScale = height.toFloat() / (LAWN_HEIGHT * scaleFactor)
        xScale = yScale
//        Log.d(TAG, "[Enter] calculateScale() xScale: $xScale yScale: $yScale")
    }

    private fun Canvas.drawMowingArea() {
        if (mowingData.size > 0) {
            addPath(mowingAreaPath, mowingData)
        }
        drawPath(mowingAreaPath, paintConfirmedObstacle)
    }

    private fun Canvas.drawGrass() {
        confirmedGrass.forEach { (key, pointList) ->
            addPath(confirmedPathGrass, pointList)

//            trashCanList[key] = pointList[0]
        }

        drawPath(confirmedPathGrass, paintConfirmedGrass)
    }

    private fun Canvas.drawObstacle() {
        confirmedObstacle.forEach { (key, pointList) ->
            addPath(confirmedPathObstacle, pointList)

//            trashCanList[key] = pointList[0]
        }

        drawPath(confirmedPathObstacle, paintConfirmedObstacle)
    }

    private fun Canvas.drawGrassRoute() {
        confirmedGrassRoute.forEach { (key, pointList) ->
            addPath(confirmedPathGrassRoute, pointList)

//            trashCanList[key] = pointList[0]
        }

        drawPath(confirmedPathGrassRoute, paintConfirmedGrassRoute)
    }

    private fun Canvas.drawChargingRoute() {
        confirmedChargingRoute.forEach { (key, pointList) ->
            addPath(confirmedPathCharging, pointList)

//            trashCanList[key] = pointList[0]

            val position = getPixelPosition(pointList[pointList.size-1])
            drawBitmap(
                chargingStationBitmap,
                position.x - chargingStationPixelOffset.x,
                position.y - chargingStationPixelOffset.y,
                Paint()
            )
        }

        drawPath(confirmedPathCharging, paintConfirmedCharging)
    }

    private fun Canvas.drawRobotPosition() {
        val point = getPixelPosition(PointF(robotCoordinateX.toFloat(), robotCoordinateY.toFloat()))
        drawBitmap(
            robotRotatedBitmap,
            robotPixelPosition.x - robotPixelOffset.x,
            robotPixelPosition.y - robotPixelOffset.y,
            Paint()
        )
    }

    private fun Canvas.drawChargingStation() {
        chargingStationCoordinate?.let {
            val position = getPixelPosition(it)
            drawBitmap(
                chargingStationBitmap,
                position.x - chargingStationPixelOffset.x,
                position.y - chargingStationPixelOffset.y,
                Paint()
            )
        }
    }

    private fun addPath(path: Path, pointList: ArrayList<PointF>) {
        val pathStart = getPixelPosition(pointList[0])
        path.moveTo(
            pathStart.x,
            pathStart.y)

        for (idx in 0 until pointList.size) {
            val point = getPixelPosition(pointList[idx])
            path.lineTo(
                point.x,
                point.y
            )
        }

//        path.setLastPoint(
//            pointList[0].x - xAxisOffset,
//            pointList[0].y - yAxisOffset
//        )
    }

    private fun getCenterOfGrass(): PointF {
        val grassCenterList = ArrayList<PointF>()
        confirmedGrass.forEach { (key, pointList) ->
            if (pointList.size > 0) {
                confirmedPathGrass.reset()
                confirmedPathGrass.moveTo(pointList[0].x * xScale, -pointList[0].y * yScale)
                for (idx in 0 until pointList.size) {
                    confirmedPathGrass.lineTo(pointList[idx].x * xScale, -pointList[idx].y * yScale)
                }

                grassCenterList.add(getPathCenter(confirmedPathGrass))
            }
        }
        confirmedPathGrass.reset()

        var temp = PointF()
        for (i in grassCenterList) {
            temp.x += i.x
            temp.y += i.y
        }
        val graphCenter = PointF()
        graphCenter.x = temp.x / grassCenterList.size
        graphCenter.y = temp.y / grassCenterList.size
        return graphCenter
    }

    private fun getPathCenter(path: Path): PointF {
        // mPath is your path. Must contain more than 1 path point
        val bounds = RectF()
        path.computeBounds(bounds, false) // fills rect with bounds

        val center = PointF(
            (bounds.left + bounds.right) / 2,
            (bounds.top + bounds.bottom) / 2
        )
        return center
    }

    fun setChargingStation() {
        chargingStationCoordinate = PointF()
        chargingStationCoordinate!!.x = robotCoordinateX.toFloat()
        chargingStationCoordinate!!.y = robotCoordinateY.toFloat()
    }

    private fun rotateBitmap(origin: Bitmap, alpha: Float): Bitmap {
        val width = origin.width
        val height = origin.height
        val matrix = Matrix()
        matrix.setRotate(alpha)
        return Bitmap.createBitmap(origin, 0, 0, width, height, matrix, true)
    }

    private fun getStartPositionY() = height / 2f

    private fun getStartPositionX() = width / 2f

    private fun initGrassConfirmedPaint() {
        paintConfirmedGrass = Paint()
//        workingBorderPaint.style = Paint.Style.STROKE
        paintConfirmedGrass.style = Paint.Style.FILL_AND_STROKE
        paintConfirmedGrass.strokeWidth = dp2px(3)
        paintConfirmedGrass.isAntiAlias = true
        paintConfirmedGrass.color = Color.parseColor("#00D91D")
        paintConfirmedGrass.alpha = 150
    }

    private fun initObstacleConfirmedPaint() {
        paintConfirmedObstacle = Paint()
        paintConfirmedObstacle.style = Paint.Style.FILL_AND_STROKE
        paintConfirmedObstacle.strokeWidth = dp2px(3)
        paintConfirmedObstacle.isAntiAlias = true
        paintConfirmedObstacle.color = Color.parseColor("#802A2A")
        paintConfirmedObstacle.alpha = 150
    }

    // TODO use only one paint for grass route?
    private fun initGrassRouteConfirmedPaint() {
        paintConfirmedGrassRoute = Paint()
        paintConfirmedGrassRoute.style = Paint.Style.STROKE
        paintConfirmedGrassRoute.strokeWidth = dp2px(6)
        paintConfirmedGrassRoute.isAntiAlias = true
        paintConfirmedGrassRoute.color = Color.parseColor("#00D91D")
//        paintConfirmedGrassRoute.alpha = 150
    }

    // TODO use only one paint for charging route?
    private fun initChargingConfirmedPaint() {
        paintConfirmedCharging = Paint()
        paintConfirmedCharging.style = Paint.Style.STROKE
        paintConfirmedCharging.strokeWidth = dp2px(6)
        paintConfirmedCharging.isAntiAlias = true
        paintConfirmedCharging.color = Color.parseColor("#00D91D")
//        paintConfirmedCharging.alpha = 150
    }

    private fun dp2px(dp:Int):Float= dp * context.resources.displayMetrics.density

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

    private fun initGrassPointBitmap() {
        grassPointBitmap = BitmapFactory.decodeResource(context.resources, R.drawable. ic_boundary_point_s)
        grassPointBitmapPixelOffset.x = grassPointBitmap.width / 2f
        grassPointBitmapPixelOffset.y = grassPointBitmap.height / 2f
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


}