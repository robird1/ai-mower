package com.ulsee.mower

import android.app.Application
import io.realm.Realm
import io.realm.RealmConfiguration

class App: Application() {
    override fun onCreate() {
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


    }

}