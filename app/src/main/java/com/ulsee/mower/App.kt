package com.ulsee.mower

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import com.ulsee.mower.ble.BluetoothLeService
import io.realm.Realm
import io.realm.RealmConfiguration

private val TAG = App::class.java.simpleName

class App: Application() {
    var bluetoothService: BluetoothLeService? = null

    override fun onCreate() {
        Log.d(TAG, "[Enter] onCreate()")
        super.onCreate()
        Realm.init(this)
        val config = RealmConfiguration.Builder()
            .name("appv2.realm")
            .schemaVersion(1) // Must be bumped when the schema changes
//            .migration { realm, oldVersion, newVersion ->
//                if (oldVersion == 0L) {
//                    val schema = realm.schema
//                    schema.create("RealmDevice")
//                        ?.addField("mID", String.javaClass)
//                        ?.addField("mIP", String.javaClass)
//                        ?.addField("mName", String.javaClass)
//                        ?.addField("mCreatedAt", Long.javaClass)
//                }
//            } // Migration to run
            .deleteRealmIfMigrationNeeded()
            .build()
        Realm.setDefaultConfiguration(config)

        bindService()
    }

    private val serviceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(
            componentName: ComponentName,
            service: IBinder
        ) {
            Log.d(TAG, "[Enter] onServiceConnected")
            bluetoothService = (service as BluetoothLeService.LocalBinder).getService()
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            bluetoothService = null
        }
    }

    private fun bindService() {
        Log.d(TAG, "[Enter] bindService()")
        val gattServiceIntent = Intent(this, BluetoothLeService::class.java)
        bindService(gattServiceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    //    suspend fun bindService() = suspendCoroutine<BluetoothLeService?> {
//        Log.d(TAG, "[Enter] bindService()")
//        val gattServiceIntent = Intent(this, BluetoothLeService::class.java)
//
//        val serviceConnection = object : ServiceConnection {
//            override fun onServiceConnected(
//                componentName: ComponentName,
//                service: IBinder
//            ) {
//                Log.d(TAG, "[Enter] onServiceConnected")
//                bluetoothService = (service as BluetoothLeService.LocalBinder).getService()
//                it.resume(bluetoothService)
//            }
//
//            override fun onServiceDisconnected(componentName: ComponentName) {
//                bluetoothService = null
//                it.resume(null)
//            }
//        }
//
//        bindService(gattServiceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
//    }

}