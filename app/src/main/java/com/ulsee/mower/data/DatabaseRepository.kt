package com.ulsee.mower.data

import com.ulsee.mower.data.model.Device
import com.ulsee.mower.data.model.RealmDevice
import io.realm.Realm
import io.realm.kotlin.where
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DatabaseRepository {
    fun saveDevice(serialNumber: String, md5:String) {
        val realm = Realm.getDefaultInstance()
        realm.beginTransaction()
        val device: RealmDevice = realm.createObject(RealmDevice::class.java)
        device.setSerialNumber(serialNumber)
        device.setSnMD5(md5)
        realm.commitTransaction()
        realm.close()
    }

    suspend fun isSerialNumberDuplicated(sn: String): Boolean = withContext(Dispatchers.IO) {
        val realm = Realm.getDefaultInstance()
        realm.beginTransaction()
        val realmDevice = realm.where(RealmDevice::class.java).equalTo("mSN", sn).findFirst()
        realm.commitTransaction()
        realm.close()
        return@withContext realmDevice != null
    }

    suspend fun getDevices(): List<Device> = withContext(Dispatchers.IO) {
        val realm = Realm.getDefaultInstance()
        val results = realm.where<RealmDevice>().findAll()
        val deviceList = ArrayList<Device>()
        for (realmDevice in results) {
            val device = Device.clone(realmDevice)
            deviceList.add(device)
        }
        realm.close()
        return@withContext deviceList
    }

    fun clearDevices() {
        val realm = Realm.getDefaultInstance()
        realm.beginTransaction()
        realm.delete(RealmDevice::class.java)
        realm.commitTransaction()
        realm.close()
    }
}