package com.ulsee.mower.data

import com.ulsee.mower.data.model.ReportedState

data class MowerStatus(
    val x: Int,
    val y: Int,
    val angle: Float,
    val power: Int,
    val workingMode: Int,
    var errorCode: Int,
    val robotStatus: String?,
    val isCharging: Boolean,
    val isMowingStatus: Boolean,
    val interruptionCode: String?,
    val testingBoundaryState: Int,
    val signalQuality: Int,
    val estimatedTime: String?,
    val elapsedTime: String?,
    val totalArea: Short?,
    val finishedArea: Short?,
) {
    val workingPercentage: Int?
        get() {
            if (totalArea == null) return null
            if (finishedArea == null) return null
            if (totalArea.toInt() == 0) return null
            return (100 * finishedArea) / totalArea
        }
    val isError: Boolean?
        get() {
            if (errorCode>0) { // error
                return true
            } else if ((robotStatus?.indexOf('1') ?: 0) >= 0) { // robotStatus emergency stop
                return true
            } else if ((interruptionCode?.indexOf('1') ?: 0) >= 0) { // robotStatus emergency stop
                return true
            }
            return false
        }
    fun hideError() {
        errorCode = 0
    }
}