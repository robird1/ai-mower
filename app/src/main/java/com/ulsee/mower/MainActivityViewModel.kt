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
import com.ulsee.mower.ble.BluetoothLeService
import com.ulsee.mower.data.BLEBroadcastAction
import com.ulsee.mower.data.MowerStatus
import kotlinx.coroutines.launch
import java.util.*
import com.amplifyframework.kotlin.core.Amplify as CoroutineAmplify

private val TAG = MainActivityViewModel::class.java.simpleName

class MainActivityViewModel(private var bleService: BluetoothLeService): ViewModel() {

    var mAWSIotMqttManager: AWSIotMqttManager? = null
    var isAWSIotMqttManagerConnected = false
    var isDeviceConnected = false
    var isIotInitialized = false

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

    // 每分鐘上傳資料
    fun keepUploadingStatus() {
        var isLogined = false
        var lastUploadAt = 0L

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
                if (now - lastUploadAt < 60000) {
                    Log.i(TAG, "upload skip just uploaded ${(now - lastUploadAt)/1000}s ago")
                    return@Runnable
                }
                lastUploadAt = now

                Log.i(TAG, "upload.... isAWSIotMqttManagerConnected=$isAWSIotMqttManagerConnected status = ${if(status == null) "null" else "exist"}")
                if (isAWSIotMqttManagerConnected) {
                    status?.let {
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
        val dataString = Gson().toJson(status.genPayload())

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
            Log.i(TAG, "gattUpdateReceiver.onReceive ${intent.action}")
            when (intent.action){
                BLEBroadcastAction.ACTION_GATT_DISCONNECTED -> {
                    unsubscribeIotMQTT(bleService.robotSerialNumber!!)
                    isDeviceConnected = false
                    isAWSIotMqttManagerConnected = false
                    isIotInitialized = false
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
                        signalQuality
                    )
                }
            }
        }
    }
}


class MainActivityViewModelFactory(private var bleService: BluetoothLeService) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainActivityViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainActivityViewModel(bleService) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}