package com.ulsee.mower.ui.map

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import androidx.core.view.isVisible
import com.ulsee.mower.R
import com.ulsee.mower.data.BLEBroadcastAction
import com.ulsee.mower.data.MapData


private const val LAWN_WIDTH = 3000         // unit: cm
private const val LAWN_HEIGHT = 1500
private const val TRASH_CAN_PADDING = 30

private val TAG = SetupMapView::class.java.simpleName

class SetupMapView@JvmOverloads constructor(
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

    private lateinit var trashCanBitmap: Bitmap
    private var trashCanBitmapPixelOffset = PointF(0f, 0f)

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
    private var workingElement = ArrayList<PointF>()
    private val workingPoint = ArrayList<PointF>()

    private var trashCanList = HashMap<String, PointF>()

    private var state: SetupMapState? = null


    private var confirmedPathGrass = Path()
    private var confirmedPathObstacle = Path()
    private var confirmedPathGrassRoute = Path()
    private var confirmedPathCharging = Path()
    private var workingPathElement = Path()

    private lateinit var paintWorkingGrass: Paint
    private lateinit var paintWorkingCharging: Paint
    private lateinit var paintWorkingObstacle: Paint
    private lateinit var paintWorkingGrassRoute: Paint
    private lateinit var paintConfirmedGrass: Paint
    private lateinit var paintConfirmedCharging: Paint
    private lateinit var paintConfirmedObstacle: Paint
    private lateinit var paintConfirmedGrassRoute: Paint

    private var xAxisOffset = 0F
    private var yAxisOffset = 0F

    private var graphCenter = PointF()

    var mode = Mode.None
    enum class Mode {
        SetPoint, Drive, None
    }

    enum class WorkType {
        GRASS, OBSTACLE, CHARGING_ROUTE, GRASS_ROUTE
    }

    var deleteMode = DeleteType.NONE
    enum class DeleteType {
        NONE, GRASS, OBSTACLE, CHARGING_ROUTE, GRASS_ROUTE
    }

    init {
        initChargingStationBitmap()
        initWorkingStartPointBitmap()
        initGrassPointBitmap()
        initObstacleBitmap()
        initRobotBitmap()
        initDeleteBitmap()
        initGrassPaint()
        initChargingPathPaint()
        initObstaclePaint()
        initGrassPathPaint()
        initChargingPathPaint()

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

        postInvalidate()
    }

    // TODO check this function
    fun resetData() {
        Log.d(TAG, "[Enter] resetData()")
        MapData.clear()

        workingElement.clear()
        workingPoint.clear()

        confirmedPathObstacle.reset()
        confirmedPathCharging.reset()
        confirmedPathGrassRoute.reset()
        workingPathElement.reset()

        trashCanList.clear()

        initGrassPaint()

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
            drawWorkingPoint()
            drawWorkingElement()
            drawObstacle()
            drawGrassRoute()
            drawChargingRoute()
//            drawWorkingStartPoint()
            drawChargingStation()
            drawRobotPosition()
            drawTrashCan()
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        Log.d("789", "[Enter] onTouchEvent()")

        if (deleteMode == DeleteType.NONE)
            return false

        val x = event.x
        val y = event.y
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                //Check if the x and y position of the touch is inside the bitmap
                trashCanList.forEach { (key, coordinate) ->

                    val position = getPixelPosition(coordinate)
                    val isPressed = ((x > position.x - TRASH_CAN_PADDING) && (x < position.x + trashCanBitmap.width + TRASH_CAN_PADDING) && (y > position.y - TRASH_CAN_PADDING) && (y < position.y + trashCanBitmap.height + TRASH_CAN_PADDING))
                    val temp = key.split(".")
                    when (deleteMode) {
                        DeleteType.GRASS -> {
                            if (temp[0] == "grass" && isPressed) {
                                Log.d("789", "grass touched. key: $key")
                                val temp = key.split(".")
                                val intent = Intent(BLEBroadcastAction.ACTION_REQUEST_DELETE_MAP)
                                intent.putExtra("type", temp[0])
                                intent.putExtra("grassNumber", temp[1].toByte())
                                context.sendBroadcast(intent)

                                state!!.binding.progressView.isVisible = true

                            }
                        }
                        DeleteType.OBSTACLE -> {
                            if (key.startsWith("obstacle") && isPressed) {
                                Log.d("789", "obstacle touched. key: $key")
                                val temp = key.split(".")
                                val intent = Intent(BLEBroadcastAction.ACTION_REQUEST_DELETE_MAP)
                                intent.putExtra("type", temp[0])
                                intent.putExtra("grassNumber", temp[1].toByte())
                                intent.putExtra("obstacleNumber", temp[2].toByte())
                                context.sendBroadcast(intent)

                                state!!.binding.progressView.isVisible = true

                            }

                        }
                        DeleteType.CHARGING_ROUTE -> {
                            if (key.startsWith("charging") && isPressed) {
                                Log.d("789", "charging touched. key: $key")
                                val temp = key.split(".")
                                val intent = Intent(BLEBroadcastAction.ACTION_REQUEST_DELETE_MAP)
                                intent.putExtra("type", temp[0])
                                intent.putExtra("grassNumber", temp[1].toByte())
                                intent.putExtra("pathNumber", temp[2].toByte())
                                context.sendBroadcast(intent)

                                state!!.binding.progressView.isVisible = true

                            }

                        }
                        DeleteType.GRASS_ROUTE -> {
                            if (key.startsWith("route") && isPressed) {
                                Log.d("789", "route touched. key: $key")
                                val temp = key.split(".")
                                val intent = Intent(BLEBroadcastAction.ACTION_REQUEST_DELETE_MAP)
                                intent.putExtra("type", temp[0])
                                intent.putExtra("grassNumber", temp[1].toByte())
                                intent.putExtra("targetGrassNumber", temp[2].toByte())
                                intent.putExtra("pathNumber", temp[3].toByte())
                                context.sendBroadcast(intent)

                                state!!.binding.progressView.isVisible = true

                            }
                        }
                    }
                }
                return false
            }
        }
        return false
    }

    fun notifyRobotCoordinate(x: Int, y: Int, angle: Float = 0f, state: SetupMapState) {
//        Log.d("123", "[Enter] notifyRobotCoordinate() x: $x y: $y")
        val point = getPixelPosition(PointF(x.toFloat(), y.toFloat()))
        robotPixelPosition.x = point.x
        robotPixelPosition.y = point.y

        this.state = state
        // 機器人停留在原地，因此不做更新地圖的動作
//        if (x == robotCoordinateX && y == robotCoordinateY) {
//            return
//        }

//        if (!isWithinCanvasBound()) {
//            clearCanvasData()
//
//            scaleFactor *= 1.2F
//            calculateScale()
//
//            recalculateCanvasData()
//
//            return
//        }
//
//        if (robotPixelPosition.y > (height - dp2px(123))) {
////            state.binding.footerViewStep3.alpha = 0.2f
//            state.binding.step3Instruction.isVisible = false
//        }

        when (state) {
            is RecordObstacle, is RecordChargingPath, is RecordGrassRoute, is RecordGrass -> {

                // 只有在drive mode時, 且機器不停留在原地時，才儲存機器人所在座標
                if (mode == Mode.Drive && x != robotCoordinateX && y != robotCoordinateY) {

                    val point = PointF(x.toFloat(), y.toFloat())
                    workingElement.add(point)
                }
            }
        }

        robotRotatedBitmap = rotateBitmap(robotBitmap, angle)
        robotCoordinateX = x
        robotCoordinateY = y

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

    private fun Canvas.drawGrass() {
        confirmedGrass.forEach { (key, pointList) ->
            addPath(confirmedPathGrass, pointList)

            trashCanList[key] = pointList[0]
        }

        drawPath(confirmedPathGrass, paintConfirmedGrass)
    }

    private fun Canvas.drawWorkingElement() {
//        state?.let {
//            when (state) {
//                is RecordObstacle -> {
//                    if (workingElement.size > 0) {
//                        addPath(workingPathElement, workingElement)
//                    }
//
//                    drawPath(workingPathElement, paintWorkingObstacle)
//                }
//                is RecordChargingPath -> {
//                    if (workingElement.size > 0) {
//                        addPath(workingPathElement, workingElement)
//                    }
//
//                    drawPath(workingPathElement, paintWorkingCharging)
//                }
//                is RecordGrassRoute -> {
//                    if (workingElement.size > 0) {
//                        addPath(workingPathElement, workingElement)
//                    }
//
//                    drawPath(workingPathElement, paintWorkingGrassRoute)
//                }
//                is RecordGrass -> {
//                    if (workingElement.size > 0) {
//                        addPath(workingPathElement, workingElement)
//                    }
//
//                    drawPath(workingPathElement, paintWorkingGrass)
//                }
//                else -> {
//                    // do nothing
//                    if (workingElement.size > 0) {
//                        addPath(workingPathElement, workingElement)
//                    }
//
//                    drawPath(workingPathElement, paintWorkingGrass)
//
//                }
//            }
//        }
        if (workingElement.size > 0) {
            addPath(workingPathElement, workingElement)
        }

        drawPath(workingPathElement, paintWorkingGrass)

    }

    private fun Canvas.drawWorkingPoint() {
        for (i in workingPoint) {
            val point = getPixelPosition(i)
            drawBitmap(
                grassPointBitmap,
                point.x - grassPointBitmapPixelOffset.x,
                point.y - grassPointBitmapPixelOffset.y * 2,
                Paint()
            )
        }
    }

    private fun Canvas.drawObstacle() {
        confirmedObstacle.forEach { (key, pointList) ->
            addPath(confirmedPathObstacle, pointList)

            trashCanList[key] = pointList[0]
        }

        drawPath(confirmedPathObstacle, paintConfirmedObstacle)
    }

    private fun Canvas.drawGrassRoute() {
        confirmedGrassRoute.forEach { (key, pointList) ->
            addPath(confirmedPathGrassRoute, pointList)

            trashCanList[key] = pointList[0]
        }

        drawPath(confirmedPathGrassRoute, paintConfirmedGrassRoute)
    }

    private fun Canvas.drawChargingRoute() {
        confirmedChargingRoute.forEach { (key, pointList) ->
            addPath(confirmedPathCharging, pointList)

            trashCanList[key] = pointList[0]

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

    private fun Canvas.drawTrashCan() {
        when (deleteMode) {
            DeleteType.NONE -> {
                // do nothing
            }
            DeleteType.GRASS -> {
                drawTrashCan2("grass")
            }
            DeleteType.OBSTACLE -> {
                drawTrashCan2("obstacle")
            }
            DeleteType.CHARGING_ROUTE -> {
                drawTrashCan2("charging")
            }
            DeleteType.GRASS_ROUTE -> {
                drawTrashCan2("route")
            }
        }
    }

    private fun Canvas.drawTrashCan2(type: String) {
        trashCanList.forEach { (key, coordinate) ->
            if (key.startsWith(type)) {
                val position = getPixelPosition(coordinate)
                drawBitmap(
                    trashCanBitmap,
                    position.x - trashCanBitmapPixelOffset.x,
                    position.y - trashCanBitmapPixelOffset.y,
                    Paint()
                )
            }
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

    fun setPoint() {
        workingPoint.add(PointF(robotCoordinateX.toFloat(), robotCoordinateY.toFloat()))
        workingElement.add(PointF(robotCoordinateX.toFloat(), robotCoordinateY.toFloat()))
        postInvalidate()
    }

    fun finishGrassBorder() {
        workingStartPointPosition?.let {
            workingPathElement.setLastPoint(it.x, it.y)
            paintWorkingGrass.style = Paint.Style.FILL_AND_STROKE
            paintWorkingGrass.alpha = 150
            workingPoint.clear()
            postInvalidate()
        }
    }

    fun finishObstacleBorder() {
        val startPoint = getPixelPosition(workingElement[0])
        startPoint?.let {
            workingPathElement.setLastPoint(it.x, it.y)
            paintWorkingObstacle.style = Paint.Style.FILL_AND_STROKE
            paintWorkingObstacle.alpha = 150
            workingPoint.clear()
            postInvalidate()
        }
    }

    fun finishChargingRoute() {
        workingPoint.clear()
        postInvalidate()
    }

    fun finishGrassRoute() {
        workingPoint.clear()
        postInvalidate()
    }

    fun switchOffPointMode() {

    }

    // TODO remove type parameter
    fun resetCurrentWork(type: WorkType) {
        initGrassPaint()
        initObstaclePaint()
        workingElement.clear()
        workingPathElement.reset()
        workingPoint.clear()
        postInvalidate()
    }

    fun saveBoundary(type: WorkType, elementKey: String) {
        when (type) {
            WorkType.GRASS -> {
                // TODO
            }
            WorkType.OBSTACLE -> {
                val list = ArrayList<PointF>()
                list.addAll(workingElement)
                confirmedObstacle[elementKey] = list
                workingElement.clear()
                workingPathElement.reset()
                initObstaclePaint()
            }
            WorkType.CHARGING_ROUTE -> {
                val list = ArrayList<PointF>()
                list.addAll(workingElement)
                confirmedChargingRoute[elementKey] = list
                workingElement.clear()
                workingPathElement.reset()
            }
            WorkType.GRASS_ROUTE -> {
                val list = ArrayList<PointF>()
                list.addAll(workingElement)
                confirmedGrassRoute[elementKey] = list
                workingElement.clear()
                workingPathElement.reset()
            }
        }

    }

    // TODO　refactor this function
    fun setGrassStartPoint() {
        Log.d("123", "[Enter] setGrassStartPoint()")
        if (MapData.grassData.size == 0) {            // 無任何地圖數據的情況
            graphCenter.x = robotCoordinateX.toFloat() * xScale
            graphCenter.y = -(robotCoordinateY.toFloat()) * yScale

            xAxisOffset = graphCenter.x - width / 2
            yAxisOffset = graphCenter.y - height / 2

            workingStartPointPosition = PointF(width / 2F, height / 2F)

        } else {                // 已存在草皮，新增草皮的情況

            val point = getPixelPosition(PointF(robotCoordinateX.toFloat(), robotCoordinateY.toFloat()))
            workingStartPointPosition = PointF(point.x, point.y)
        }

        postInvalidate()
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

    private fun initGrassPaint() {
        paintWorkingGrass = Paint()
        paintWorkingGrass.style = Paint.Style.STROKE
//        workingBorderPaint.alpha = 150
        paintWorkingGrass.strokeWidth = dp2px(3)
        paintWorkingGrass.isAntiAlias = true
        paintWorkingGrass.color = Color.parseColor("#00D91D")
//        workingBorderPaint.setXfermode( PorterDuffXfermode(PorterDuff.Mode.XOR))
    }

    private fun initGrassConfirmedPaint() {
        paintConfirmedGrass = Paint()
//        workingBorderPaint.style = Paint.Style.STROKE
        paintConfirmedGrass.style = Paint.Style.FILL_AND_STROKE
        paintConfirmedGrass.strokeWidth = dp2px(3)
        paintConfirmedGrass.isAntiAlias = true
        paintConfirmedGrass.color = Color.parseColor("#00D91D")
        paintConfirmedGrass.alpha = 150
    }

    private fun initObstaclePaint() {
        paintWorkingObstacle = Paint()
        paintWorkingObstacle.style = Paint.Style.STROKE
        paintWorkingObstacle.strokeWidth = dp2px(2)
        paintWorkingObstacle.isAntiAlias = true
        paintWorkingObstacle.color = Color.parseColor("#802A2A")
    }

    private fun initObstacleConfirmedPaint() {
        paintConfirmedObstacle = Paint()
        paintConfirmedObstacle.style = Paint.Style.FILL_AND_STROKE
        paintConfirmedObstacle.strokeWidth = dp2px(3)
        paintConfirmedObstacle.isAntiAlias = true
        paintConfirmedObstacle.color = Color.parseColor("#802A2A")
        paintConfirmedObstacle.alpha = 150
    }

    private fun initGrassPathPaint() {
        paintWorkingGrassRoute = Paint()
        paintWorkingGrassRoute.style = Paint.Style.STROKE
        paintWorkingGrassRoute.strokeWidth = dp2px(6)
//        paintWorkingGrassRoute.pathEffect = DashPathEffect(
//            floatArrayOf(20F, 30F),
//            0F
//        )
        paintWorkingGrassRoute.isAntiAlias = true
        paintWorkingGrassRoute.color = Color.parseColor("#00D91D")
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

    private fun initChargingPathPaint() {
        paintWorkingCharging = Paint()
        paintWorkingCharging.style = Paint.Style.STROKE
        paintWorkingCharging.strokeWidth = dp2px(6)
//        chargingPathPaint.strokeWidth = 20F
//        paintWorkingCharging.pathEffect = DashPathEffect(
//            // array of ON and OFF distances
////            floatArrayOf(20F, 30F, 40F, 50F),
//            floatArrayOf(20F, 30F),
//            0F // phase : offset into the intervals array
//        )
        paintWorkingCharging.isAntiAlias = true
        paintWorkingCharging.color = Color.parseColor("#00D91D")
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

    private fun initDeleteBitmap() {
        trashCanBitmap = BitmapFactory.decodeResource(context.resources, R.drawable. ic_setmap_delete)
        trashCanBitmapPixelOffset.x = trashCanBitmap.width / 2f
        trashCanBitmapPixelOffset.y = trashCanBitmap.height / 2f
    }

}