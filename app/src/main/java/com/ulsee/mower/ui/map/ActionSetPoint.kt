package com.ulsee.mower.ui.map

import com.ulsee.mower.data.RecordBoundary

class ActionSetPoint(val subject: Int, val result: Int, fragment: SetupMapFragment): ActionRecordBoundary(fragment) {
    override fun execute() {
        when (subject) {
            RecordBoundary.Subject.GRASS -> {
                binding.mapView.setPoint()
            }
            RecordBoundary.Subject.OBSTACLE -> {
                binding.mapView.setPoint()
            }
            RecordBoundary.Subject.CHARGING -> {
                binding.mapView.setPoint()
            }
            RecordBoundary.Subject.GRASS_PATH -> {
                binding.mapView.setPoint()
            }
        }
    }

}
