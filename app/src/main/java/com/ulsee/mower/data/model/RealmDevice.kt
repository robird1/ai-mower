package com.ulsee.mower.data.model

import io.realm.RealmObject

open class RealmDevice : RealmObject() {

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

}