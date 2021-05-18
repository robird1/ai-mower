package com.ulsee.mower.data.model

class Device {

    private var mSN: String = ""
    fun setSerialNumber(value: String) {
        mSN = value
    }

    fun getSerialNumber(): String {
        return mSN
    }

    private var mSnMD5: String = ""
    fun setSnMD5(value: String) {
        mSnMD5 = value
    }

    fun getSnMD5(): String {
        return mSnMD5
    }

    companion object {
        fun clone (realmDevice: RealmDevice) : Device {
            val device = Device()
            device.setSerialNumber(realmDevice.getSerialNumber())
            device.setSnMD5(realmDevice.getSnMD5())
            return device
        }
    }

}