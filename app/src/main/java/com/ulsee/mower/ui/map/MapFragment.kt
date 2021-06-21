package com.ulsee.mower.ui.map

import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.*
import com.ulsee.mower.MainActivity
import com.ulsee.mower.R
import com.ulsee.mower.data.*
import com.ulsee.mower.data.BLEBroadcastAction.Companion.ACTION_STATUS_RESPONSE
import com.ulsee.mower.databinding.FragmentMapBinding
import kotlin.math.abs

private val TAG = MapFragment::class.java.simpleName

class MapFragment: Fragment(), OnMapReadyCallback, GoogleMap.OnPolylineClickListener, GoogleMap.OnPolygonClickListener {
    private lateinit var binding: FragmentMapBinding
    private lateinit var viewModel: MapFragmentViewModel
    private lateinit var bluetoothService: BluetoothLeService
    private lateinit var bleRepository: BluetoothLeRepository

    private val positionList = arrayListOf<Position>()

    data class Position (
        var x: Double,
        var y: Double
    )

//    override fun onServiceConnected(service: BluetoothLeService) {
//        bluetoothService = service
//        bleRepository.setBleService(bluetoothService!!)
//    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        (activity as MainActivity).registerServiceCallback(this)
        bluetoothService = (activity as MainActivity).bluetoothService!!
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentMapBinding.inflate(inflater, container, false)

        initViewModel()

        binding.recyclerview.adapter = TempAdapter(viewModel)
        binding.recyclerview.layoutManager = LinearLayoutManager(context)
        registerBLEReceiver()
        viewModel.status.observe(viewLifecycleOwner) {
            (binding.recyclerview.adapter as TempAdapter).bind(it)
        }

        initJoyStick()


//        binding.moveButton.setOnClickListener {
//            viewModel.moveRobot(0, 0.0)
//        }


        // Get the SupportMapFragment and request notification
        // when the map is ready to be used.
//        val map = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
//        map!!.getMapAsync(this)

        return binding.root
    }

    private fun registerBLEReceiver() {
        val filter = IntentFilter()
        filter.addAction(ACTION_STATUS_RESPONSE)
        requireActivity().registerReceiver(viewModel.gattUpdateReceiver, filter)
    }


    override fun onMapReady(googleMap: GoogleMap) {
        showLocation(googleMap, 25.073875, 121.569744, 3000)
        showLocation(googleMap, 25.073783, 121.569009, 6000)
        showLocation(googleMap, 25.073229, 121.568784,9000)
        showLocation(googleMap, 25.072816, 121.569160,12000)
        showLocation(googleMap, 25.072933, 121.569836,15000)
        showLocation(googleMap, 25.073409, 121.569916,18000)

        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed({

            googleMap.clear()
            showPolyLine(googleMap)

        }, 21000)

        handler.postDelayed({

            googleMap.clear()
            showPolygon(googleMap)

        }, 24000)


//        // Position the map's camera near Alice Springs in the center of Australia,
//        // and set the zoom factor so most of Australia shows on the screen.
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(25.073342477002086, 121.56866865971409), 17.5f))
        googleMap.mapType = GoogleMap.MAP_TYPE_NORMAL
//        // Set listeners for click events.
//        googleMap.setOnPolylineClickListener(this)
//        googleMap.setOnPolygonClickListener(this)

    }

    override fun onPolylineClick(p0: Polyline?) {
        TODO("Not yet implemented")
    }

    override fun onPolygonClick(p0: Polygon?) {
        TODO("Not yet implemented")
    }

    private fun showLocation(googleMap: GoogleMap, x: Double, y: Double, delay: Long) {
        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed({

            googleMap.clear()
            for (p in positionList) {
                googleMap.addMarker(
                    MarkerOptions().icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_setmap_beacon))
                        .position(LatLng(p.x, p.y))
                        .title("Robot Location")
                )
            }

            val place = LatLng(x, y)
            googleMap.addMarker(
                MarkerOptions().icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_ladybug_64))
                    .position(place)
                    .title("Robot Location")
            )

            positionList.add(Position(x, y))

        }, delay)
    }

    private fun showPolyLine(googleMap: GoogleMap) {
        val polyline1 = googleMap.addPolyline(
            PolylineOptions()
            .clickable(true)
            .add(
                LatLng(25.073875, 121.569744),
                LatLng(25.073783, 121.569009),
                LatLng(25.073229, 121.568784),
                LatLng(25.072816, 121.569160),
                LatLng(25.072933, 121.569836),
                LatLng(25.073409, 121.569916)
            ))
        polyline1.tag = "A"
        stylePolyline(polyline1)
    }

    private fun showPolygon(googleMap: GoogleMap) {
        val polygon1 = googleMap.addPolygon(PolygonOptions()
            .clickable(true)
            .add(
                LatLng(25.073875, 121.569744),
                LatLng(25.073783, 121.569009),
                LatLng(25.073229, 121.568784),
                LatLng(25.072816, 121.569160),
                LatLng(25.072933, 121.569836),
                LatLng(25.073409, 121.569916)
            ))
// Store a data object with the polygon, used here to indicate an arbitrary type.
        polygon1.tag = "alpha"
        stylePolygon(polygon1)
    }

    private val COLOR_BLACK_ARGB = -0x1000000
    private val POLYLINE_STROKE_WIDTH_PX = 12

    /**
     * Styles the polyline, based on type.
     * @param polyline The polyline object that needs styling.
     */
    private fun stylePolyline(polyline: Polyline) {
        // Get the data object stored with the polyline.
        val type = polyline.tag?.toString() ?: ""
        when (type) {
            "A" -> {
                // Use a custom bitmap as the cap at the start of the line.
                polyline.startCap = CustomCap(
                    BitmapDescriptorFactory.fromResource(R.drawable.ic_setmap_beacon), 10f)
            }
            "B" -> {
                // Use a round cap at the start of the line.
                polyline.startCap = RoundCap()
            }
        }
        polyline.endCap = RoundCap()
        polyline.width = POLYLINE_STROKE_WIDTH_PX.toFloat()
        polyline.color = COLOR_BLACK_ARGB
        polyline.jointType = JointType.ROUND
    }



    private val PATTERN_GAP_LENGTH_PX = 20
    private val DOT: PatternItem = Dot()
    private val GAP: PatternItem = Gap(PATTERN_GAP_LENGTH_PX.toFloat())

    private val COLOR_WHITE_ARGB = -0x1
    private val COLOR_GREEN_ARGB = -0xc771c4
    private val COLOR_PURPLE_ARGB = -0x7e387c
    private val COLOR_ORANGE_ARGB = -0xa80e9
    private val COLOR_BLUE_ARGB = -0x657db
    private val POLYGON_STROKE_WIDTH_PX = 8
    private val PATTERN_DASH_LENGTH_PX = 20

    private val DASH: PatternItem = Dash(PATTERN_DASH_LENGTH_PX.toFloat())

    // Create a stroke pattern of a gap followed by a dash.
    private val PATTERN_POLYGON_ALPHA = listOf(GAP, DASH)

    // Create a stroke pattern of a dot followed by a gap, a dash, and another gap.
    private val PATTERN_POLYGON_BETA = listOf(DOT, GAP, DASH, GAP)

    /**
     * Styles the polygon, based on type.
     * @param polygon The polygon object that needs styling.
     */
    private fun stylePolygon(polygon: Polygon) {
        // Get the data object stored with the polygon.
        val type = polygon.tag?.toString() ?: ""
        var pattern: List<PatternItem>? = null
        var strokeColor = COLOR_BLACK_ARGB
        var fillColor = COLOR_WHITE_ARGB
        when (type) {
            "alpha" -> {
                // Apply a stroke pattern to render a dashed line, and define colors.
                pattern = PATTERN_POLYGON_ALPHA
                strokeColor = COLOR_GREEN_ARGB
                fillColor = COLOR_PURPLE_ARGB
            }
            "beta" -> {
                // Apply a stroke pattern to render a line of dots and dashes, and define colors.
                pattern = PATTERN_POLYGON_BETA
                strokeColor = COLOR_ORANGE_ARGB
                fillColor = COLOR_BLUE_ARGB
            }
        }
        polygon.strokePattern = pattern
        polygon.strokeWidth = POLYGON_STROKE_WIDTH_PX.toFloat()
        polygon.strokeColor = strokeColor
        polygon.fillColor = fillColor
    }

    private fun initViewModel() {
        bleRepository = BluetoothLeRepository(bluetoothService)
        viewModel = ViewModelProvider(this, MapFragmentFactory(bleRepository)).get(MapFragmentViewModel::class.java)
    }

    private fun initJoyStick() {
        val isStop = booleanArrayOf(false)
        val movement = doubleArrayOf(0.0)
        val rotation = doubleArrayOf(0.0)

        binding.joystick.setOnMoveListener { intAngle: Int, intStrength: Int ->
//            Log.d(TAG, "[Enter] onMoveListener() intAngle: "+ intAngle+ "intStrength: "+intStrength);
//            joystickOnMove.setText("angle: $intAngle strength: $intStrength")
//            if (!mRobotClient_sendCommand.isConnected()) {
//                debugView.setText("!mRobotClient_sendCommand.isConnected() in onMoveListener()")
//                return@setOnMoveListener
//            }
            // TODO

            val angle = intAngle.toDouble()
            val strength = intStrength.toDouble()
            rotation[0] = 0.0
            movement[0] = strength / 100.0
            isStop[0] = intAngle == 0 && intStrength == 0
            //            isStop[0] = intStrength == 0;
            if (isStop[0]) {
//                stopMove()
                // TODO
                return@setOnMoveListener
            }
            // calculate movement forward & backward
            /* 0~ 1*/if (angle > 0 && angle <= 90) movement[0] = movement[0] * angle / 90.0
            /* 1~ 0*/if (angle > 90 && angle <= 180) movement[0] = movement[0] * (1.0 - (angle - 90.0) / 90.0)
            /* 0~-1*/if (angle > 180 && angle <= 270) movement[0] = movement[0] * (-(angle - 180.0) / 90.0)
            /*-1~ 0*/if (angle > 270 && angle < 360) movement[0] = movement[0] * (-1 + (angle - 270.0) / 90.0)
            // calculate rotate
//            /*-1~ 0*/if (angle > 0 && angle <= 90) rotation[0] = -(90.0 - angle) / 90.0
//            /* 0~ 1*/if (angle > 90 && angle <= 180) rotation[0] = (angle - 90.0) / 90.0
//            /* 1~ 0*/if (angle > 180 && angle <= 270) rotation[0] = 1 - (angle - 180.0) / 90.0
//            /* 0~-1*/if (angle > 270 && angle < 360) rotation[0] = -(angle - 270.0) / 90
            // 靠近上下時不轉彎，靠近左右時不前後移動
            if (angle > 70 && angle < 110 || angle > 250 && angle < 290) rotation[0] = 0.0
            if (angle > 340 || angle < 20 || angle > 160 && angle < 200) movement[0] = 0.0
            // 下方，靠近正左正右時，轉彎方向不變
//            if (angle > 340 || angle > 180 && angle < 200) rotation[0] = -rotation[0];


//            Log.d(TAG, String.format("angle=%f, strength=%f, rotation[0]=%f, movement[0]=%f", angle, strength, rotation[0], movement[0]));
            if (isStop[0]) return@setOnMoveListener
//            movement[0] *= getSpeedRatio()

            // TODO
//            mRobotClient_sendCommand.requestMove(movement[0], rotation[0])
//                .subscribe({ Void -> }) { t ->
//                    t.printStackTrace()
//                    Log.d(TAG, "[Failed] mRobotClient_sendCommand.requestMove()")
//                }

            rotation[0] = angle - 90
            if (rotation[0] < 0)
                rotation[0] = rotation[0] + 360

            if (rotation[0] > 0)
                rotation[0] = 360 - rotation[0]

            movement[0] = abs(movement[0]) * 50

            Log.d(TAG, "angle: ${rotation[0].toInt()} movement: ${movement[0]}")
            viewModel.moveRobot(rotation[0].toInt(), movement[0])

        }
    }

//    private fun getSpeedRatio(): Double {
//        return seekBar.getProgress() / 100.0
//    }


}