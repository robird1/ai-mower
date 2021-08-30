package com.ulsee.mower

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.*
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.amazonaws.mobile.client.AWSMobileClient
import com.amazonaws.mobileconnectors.iot.AWSIotMqttClientStatusCallback
import com.amazonaws.mobileconnectors.iot.AWSIotMqttManager
import com.amazonaws.mobileconnectors.iot.AWSIotMqttQos
import com.amazonaws.regions.Region
import com.amazonaws.services.iot.AWSIotClient
import com.amazonaws.services.iot.model.AttachPolicyRequest
import com.google.gson.Gson
import com.ulsee.mower.ble.BluetoothLeRepository
import com.ulsee.mower.ble.BluetoothLeService
import com.ulsee.mower.ble.CommandSettings
import com.ulsee.mower.data.BLEBroadcastAction
import com.ulsee.mower.data.MowerStatus
import com.ulsee.mower.data.StartStop
import com.ulsee.mower.data.StatusFragmentBroadcast
import com.ulsee.mower.data.model.DesiredAction
import com.ulsee.mower.data.model.DesiredMode
import com.ulsee.mower.data.model.IotCoreShadowPayload
import com.ulsee.mower.data.model.ReportedState
import com.ulsee.mower.ui.map.StatusFragment
import com.ulsee.mower.ui.settings.mower.MowerSettings
import com.ulsee.mower.ui.settings.mower.MowerWorkingMode
import kotlinx.coroutines.launch
import java.util.*
import com.amplifyframework.kotlin.core.Amplify as CoroutineAmplify

private val TAG = MainActivityViewModel::class.java.simpleName

class MainActivityViewModel(private var bleService: BluetoothLeService, private var bleRepository: BluetoothLeRepository): ViewModel() {

    var mAWSIotMqttManager: AWSIotMqttManager? = null
    var isAWSIotMqttManagerConnected = false
    var isDeviceConnected = false
    var isIotInitialized = false
//    var lastUploadStatusAt = 0L
    var lastUploadingStatusAt = 0L
    var lastUploadStatusIsError = false

    private lateinit var statusHandler: StatusHandler
    private inner class StatusHandler(looper: Looper) : Handler(looper) {
        override fun handleMessage(msg: Message) {
            try {
                Thread.sleep(5000)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }
    }
    private var uploadStatusTask: Runnable? = null
    private var status: MowerStatus? = null

    private var isStatusFragmentActive = false
    // MowingState copy from status fragment
    private var state = MowingState.Stop
    enum class MowingState {
        Mowing, Pause, Stop
    }

    // 每分鐘上傳資料
    fun keepUploadingStatus() {
        var isLogined = false

        HandlerThread("keepUploadStatusThread", Process.THREAD_PRIORITY_BACKGROUND).apply {
            start()
            statusHandler = StatusHandler(looper)
        }

        uploadStatusTask = Runnable {
            uploadStatusTask?.let { statusHandler.postDelayed(it, 5000) }
            // 1. login
            if(!isLogined) {
                viewModelScope.launch {
                    isLogined = login()
                }
            } else if(!isIotInitialized) {
                Log.i(TAG, "try to init iot, sn = ${bleService.robotSerialNumber}, isDeviceConnected = $isDeviceConnected")
                if (isDeviceConnected && bleService.robotSerialNumber != null) {
                    Thread{
                        try {
                            initIot()
                            isIotInitialized = true
                        } catch(e: Exception) {
                            e.printStackTrace()
                        }
                    }.start()
                }
            } else {
                val now = System.currentTimeMillis()
                val isError = status?.isError == true
                val delay = if (isError && lastUploadStatusIsError) 5000 else 1000
                if (now - lastUploadingStatusAt < delay) { // 每1 or 5秒傳
                    Log.i(TAG, "upload skip just uploaded ${(now - lastUploadingStatusAt)/1000}s ago")
                    return@Runnable
                }

                Log.i(TAG, "upload.... isAWSIotMqttManagerConnected=$isAWSIotMqttManagerConnected status = ${if(status == null) "null" else "exist"}")
                if (isAWSIotMqttManagerConnected) {
                    status?.let {
                        lastUploadingStatusAt = System.currentTimeMillis()
                        lastUploadStatusIsError = isError
                        upload(it)
                        status = null
                    }
                }
            }
        }
        statusHandler.post(uploadStatusTask!!)
    }

    // required in thread
    private fun initIot() {
        val mqttClientID = UUID.randomUUID().toString()
        val endpoint = "ag9gcnyhp6jrs-ats.iot.us-east-1.amazonaws.com" // https://console.aws.amazon.com/iot/home?region=us-east-1#/settings
        val regionString = "us-east-1"
        val iotPolicyName = "my_iot_policy"
        mAWSIotMqttManager = AWSIotMqttManager(mqttClientID, endpoint);

        val attachPolicyReq = AttachPolicyRequest()
        attachPolicyReq.policyName = iotPolicyName // name of your IOT AWS policy
        val mIotAndroidClient = AWSIotClient(AWSMobileClient.getInstance())
        attachPolicyReq.target = AWSMobileClient.getInstance().identityId
        mIotAndroidClient.setRegion(Region.getRegion(regionString)) // name of your IoT Region such as "us-east-1"
        mIotAndroidClient.attachPolicy(attachPolicyReq)

        mAWSIotMqttManager?.connect(
            AWSMobileClient.getInstance()
        ) { status, throwable ->
            isAWSIotMqttManagerConnected = status == AWSIotMqttClientStatusCallback.AWSIotMqttClientStatus.Connected
            if (isAWSIotMqttManagerConnected) subscribeIotMQTT(bleService.robotSerialNumber!!)
            Log.i(
                TAG,
                "mqttManager.connect(AWSMobileClient.getInstance(), object: AWSIotMqttClientStatusCallback: status=$status"
            )
            throwable?.printStackTrace()
        }
    }

    private fun subscribeIotMQTT(deviceMacAddress: String) {
        try {
            val SHADOW_PREFIX = "\$aws/things/"
            val thingName = deviceMacAddress
            val SHADOW_OP_UPDATE = "/shadow/update"

            val updateTopic = "$SHADOW_PREFIX$thingName$SHADOW_OP_UPDATE"
            val acceptedTopic = "$SHADOW_PREFIX$thingName$SHADOW_OP_UPDATE/accepted"
            val rejectedTopic = "$SHADOW_PREFIX$thingName$SHADOW_OP_UPDATE/rejected"
            Log.i(TAG, "subscribeUpdateResult $deviceMacAddress, $acceptedTopic, $rejectedTopic")
            mAWSIotMqttManager?.subscribeToTopic(acceptedTopic, AWSIotMqttQos.QOS0 /* Quality of Service */
            ) { topic, data ->
                try {
                    val message = String(data, Charsets.UTF_8)
                    Log.i(TAG, "IotCore acceptedTopic Message received: $message")

                    val payload = Gson().fromJson(message, IotCoreShadowPayload::class.java)
                    if (payload.state.reported != null) {
//                        lastUploadStatusAt = System.currentTimeMillis()
                    }
                    payload.state.desired?.let {
                        if (it.mode != null) {
                            Log.i(TAG, "desired mode ${it.mode}")
                            val value : Byte = when(it.mode) {
                                DesiredMode.learning -> 0x00
                                DesiredMode.working -> 0x01
                                DesiredMode.learnthenwork -> 0x02
                                DesiredMode.gradual -> 0x03
                                DesiredMode.explosive -> 0x04
                                else -> 0x00
                            }
                            bleRepository.configSettings(CommandSettings.INSTRUCTION_WORKING_MODE/*0x88*/, value)
                        }
                        if (it.blade != null) {
                            Log.i(TAG, "desired blade ${it.blade}")
                            bleRepository.configSettings(CommandSettings.INSTRUCTION_BLADE_HEIGHT/*0x88*/, it.blade.toByte())
                        }
                        if (it.action != null) {
                            Log.i(TAG, "desired action ${it.action}")
                            when(it.action) {
                                DesiredAction.go -> {
                                    Log.i(TAG, "desired action TODO: go, state =$state")
                                    if (!isStatusFragmentActive) {
                                        Log.i(TAG, "desired action but status fragment not active")
                                    }
                                    when (state) {
                                        MowingState.Mowing -> {
                                        }
                                        MowingState.Pause -> {
                                            bleRepository.startStop(StartStop.Command.RESUME_MOWING)
                                        }
                                        MowingState.Stop -> {
                                            bleRepository.startStop(StartStop.Command.START_MOWING)
                                        }
                                    }
                                }
                                DesiredAction.pause -> {
                                    Log.i(TAG, "desired action TODO: pause, state=$state")
                                    if (!isStatusFragmentActive) {
                                        Log.i(TAG, "desired action but status fragment not active")
                                    }
                                    when (state) {
                                        MowingState.Mowing -> {
                                            bleRepository.startStop(StartStop.Command.PAUSE_MOWING)
                                        }
                                        MowingState.Pause -> {
                                            bleRepository.startStop(StartStop.Command.RESUME_MOWING)
                                        }
                                        MowingState.Stop -> {
                                            // do nothing
                                        }
                                    }
                                }
                                DesiredAction.charge -> {
                                    Log.i(TAG, "desired action TODO: charge, state=$state")
                                    if (!isStatusFragmentActive) {
                                        Log.i(TAG, "desired action but status fragment not active")
                                    }
                                    when (state) {
                                        MowingState.Pause -> {
                                            bleRepository.startStop(StartStop.Command.BACK_CHARGING_STATION)
                                            bleRepository.startStop(StartStop.Command.RESUME_MOWING)
                                        }
                                        MowingState.Mowing -> {
                                            bleRepository.startStop(StartStop.Command.BACK_CHARGING_STATION)
                                        }
                                        MowingState.Stop -> {
                                            // do nothing
                                        }
                                    }
                                }
                            }
                        }
                        if (it.rainmode != null) {
                            Log.i(TAG, "desired rainmode ${it.rainmode}")
                            val value : Byte = if(it.rainmode.isTrue) 0x01 else 0x00
                            bleRepository.configSettings(CommandSettings.INSTRUCTION_RAIN_MODE/*0x83*/, value)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "IotCore acceptedTopic Message encoding error: ", e)
                }
            }
            mAWSIotMqttManager?.subscribeToTopic(rejectedTopic, AWSIotMqttQos.QOS0 /* Quality of Service */
            ) { topic, data ->
                try {
                    val message = String(data, Charsets.UTF_8)
                    Log.i(TAG, "IotCore rejectedTopic Message received: $message")
                } catch (e: Exception) {
                    Log.e(TAG, "IotCore rejectedTopic Message encoding error: ", e)
                }
            }
//            mAWSIotMqttManager?.subscribeToTopic(updateTopic, AWSIotMqttQos.QOS0 /* Quality of Service */
//            ) { topic, data ->
//                try {
//                    val message = String(data, Charsets.UTF_8)
//                    Log.i(TAG, "IotCore updateTopic Message received: $message")
//                } catch (e: Exception) {
//                    Log.e(TAG, "IotCore updateTopic Message encoding error: ", e)
//                }
//            }
        } catch(e: Exception) {
            e.printStackTrace()
        }
    }

    private fun unsubscribeIotMQTT(deviceMacAddress: String) {
        Log.i(TAG, "unsubscribeIotMQTT($deviceMacAddress)")
        val SHADOW_PREFIX = "\$aws/things/"
        val thingName = deviceMacAddress
        val SHADOW_OP_UPDATE = "/shadow/update"

        val acceptedTopic = "$SHADOW_PREFIX$thingName$SHADOW_OP_UPDATE/accepted"
        val rejectedTopic = "$SHADOW_PREFIX$thingName$SHADOW_OP_UPDATE/rejected"
        mAWSIotMqttManager?.unsubscribeTopic(acceptedTopic)
        mAWSIotMqttManager?.unsubscribeTopic(rejectedTopic)
    }

    override fun onCleared() {
        uploadStatusTask?.let {
            statusHandler.removeCallbacks(it)
        }
        super.onCleared()
    }

    private fun upload(status: MowerStatus) {
        val payload = IotCoreShadowPayload.createStatus(status)
        if (!isStatusFragmentActive) payload.state.reported?.state = ReportedState.setting
        val dataString = Gson().toJson(payload)

        val SHADOW_PREFIX = "\$aws/things/"
        val thingName = bleService.robotSerialNumber!!
        val SHADOW_OP_UPDATE = "/shadow/update"

        val topic = "$SHADOW_PREFIX$thingName$SHADOW_OP_UPDATE"
        Log.i(TAG, "upload $topic, $dataString")
        if (isAWSIotMqttManagerConnected) {
            mAWSIotMqttManager?.publishString(dataString, topic, AWSIotMqttQos.QOS0)
        }
    }
    private fun upload(settings: MowerSettings) {
        val dataString = Gson().toJson(IotCoreShadowPayload.createSettings(settings))

        val SHADOW_PREFIX = "\$aws/things/"
        val thingName = bleService.robotSerialNumber!!
        val SHADOW_OP_UPDATE = "/shadow/update"

        val topic = "$SHADOW_PREFIX$thingName$SHADOW_OP_UPDATE"
        Log.i(TAG, "upload $topic, $dataString")
        if (isAWSIotMqttManagerConnected) {
            mAWSIotMqttManager?.publishString(dataString, topic, AWSIotMqttQos.QOS0)
        }
    }

    private suspend fun login() : Boolean {
        val username = "codus.hsu@ulsee.com"
        val password = "assa4415"
        return CoroutineAmplify.Auth.signIn(username, password).isSignInComplete
    }

    val gattUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
//            Log.i(TAG, "gattUpdateReceiver.onReceive ${intent.action}")
            when (intent.action){
                BLEBroadcastAction.ACTION_ON_DISCONNECT_DEVICE -> {
                    Log.i(TAG, "ACTION_ON_DISCONNECT_DEVICE...")
                    isDeviceConnected = false
                    isAWSIotMqttManagerConnected = false
                    isIotInitialized = false
                    unsubscribeIotMQTT(bleService.robotSerialNumber!!)
                    mAWSIotMqttManager?.disconnect()
                    mAWSIotMqttManager = null
                }
                BLEBroadcastAction.ACTION_GATT_CONNECTED -> {
                    isDeviceConnected = true
                }
                BLEBroadcastAction.ACTION_STATUS -> {
                    val x = intent.getIntExtra("x", 0)
                    val y = intent.getIntExtra("y", 0)
                    val angle = intent.getFloatExtra("angle", 0F)
                    val power = intent.getIntExtra("power", -1)
                    val workingMode = intent.getIntExtra("working_mode", -1)
                    val errorCode = intent.getIntExtra("working_error_code", -1)
                    val robotStatus = intent.getStringExtra("robot_status") ?: ""
                    val isCharging = intent.getBooleanExtra("charging_status", false)
                    val isMowingStatus = intent.getBooleanExtra("mowing_status", false)
                    val interruptionCode = intent.getStringExtra("interruption_code") ?: ""
                    val testingBoundaryState = intent.getIntExtra("testing_boundary", -1)
                    val signalQuality = intent.getIntExtra("signal_quality", -1)
                    val estimatedTime = intent.getStringExtra("estimated_time")
                    val elapsedTime = intent.getStringExtra("elapsed_time")
                    val totalArea = intent.getShortExtra("total_area", 0)
                    val finishedArea = intent.getShortExtra("finished_area", 0)
                    Log.i(TAG, "errorCode=$errorCode, robotStatus=$robotStatus, interruptionCode=$interruptionCode")
                    status = MowerStatus(
                        x,
                        y,
                        angle,
                        power,
                        workingMode,
                        errorCode,
                        robotStatus,
                        isCharging,
                        isMowingStatus,
                        interruptionCode,
                        testingBoundaryState,
                        signalQuality,
                        estimatedTime,
                        elapsedTime,
                        totalArea,
                        finishedArea
                    )
                }

                BLEBroadcastAction.ACTION_SETTINGS -> {
                    try {
                        val result = intent.getIntExtra("result", -1) // 1 for ok, 0 for error
                        val operation_mode = intent.getIntExtra("operation_mode", -1)
                        val operationString = if (operation_mode == 1) "fetch" else "write"
                        val working_mode = intent.getIntExtra("working_mode", -1)
                        val rain_mode = intent.getIntExtra("rain_mode", -1)
                        val mower_count = intent.getIntExtra("mower_count", -1)
                        val knife_height = intent.getIntExtra("knife_height", -1)

                        if (result == 1) {
                            val settings = MowerSettings(
                                MowerWorkingMode(working_mode),
                                rain_mode,
                                mower_count,
                                knife_height
                            )
                            upload(settings)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "gattUpdateReceiver.onReceive exception: ${e.message}")
                        e.printStackTrace()
                    }
                }

                StatusFragmentBroadcast.LIFECYCLE_ONRESUME -> {
                    isStatusFragmentActive = true
                }
                StatusFragmentBroadcast.LIFECYCLE_ONPAUSE -> {
                    isStatusFragmentActive = false
                }
                StatusFragmentBroadcast.MOWER_STATUS_MOWING -> {
                    state = MowingState.Mowing
                }
                StatusFragmentBroadcast.MOWER_STATUS_PAUSE -> {
                    state = MowingState.Pause
                }
                StatusFragmentBroadcast.MOWER_STATUS_STOP -> {
                    state = MowingState.Stop
                }
            }
        }
    }
}


class MainActivityViewModelFactory(private var bleService: BluetoothLeService, private var bleRepository: BluetoothLeRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainActivityViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainActivityViewModel(bleService, bleRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}