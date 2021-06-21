package com.ulsee.mower.ui.map

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ulsee.mower.data.BLEBroadcastAction.Companion.ACTION_STATUS_RESPONSE
import com.ulsee.mower.data.BluetoothLeRepository
import com.ulsee.mower.utils.MD5
import com.ulsee.mower.utils.Utils
import java.math.BigInteger
import java.nio.ByteBuffer
import kotlin.experimental.and


class MapFragmentViewModel(private val bleRepository: BluetoothLeRepository): ViewModel() {
    private var _status : MutableLiveData<String> = MutableLiveData()
    val status : LiveData<String>
        get() = _status

    fun moveRobot(rotation: Int, movement: Double) {
        bleRepository.moveRobot(rotation, movement)
    }

    val gattUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            val x = intent.getByteArrayExtra("x")!!
            val y = intent.getByteArrayExtra("y")!!
            val angle = intent.getByteArrayExtra("angle")!!
//            Log.d(TAG, "action: $action message: $message")
            when (action){
                ACTION_STATUS_RESPONSE -> {
//                    val temp = message!!.split(" ")
//                    val msg = "offset: ${message!!.substring(17, 40)} angle: ${message!!.substring(44, 49)}"

                    _status.value = "x: ${BigInteger(x).toInt()} y: ${BigInteger(y).toInt()} angle: ${Utils.littleEndianConversion(angle)}"
                }
            }
        }
    }

    fun byteArrayToInt(bytes: ByteArray): Int {
        val byteArray = byteArrayOf(0, 0, 0, 1)
        return ByteBuffer.wrap(bytes).int
    }

}

class MapFragmentFactory(private val bleRepository: BluetoothLeRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MapFragmentViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MapFragmentViewModel(bleRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}