package com.ulsee.mower.ui.map

import com.ulsee.mower.data.RecordBoundary.Subject.Companion.GRASS
import com.ulsee.mower.data.RecordBoundary.Subject.Companion.OBSTACLE
import com.ulsee.mower.data.RecordBoundary.Subject.Companion.CHARGING
import com.ulsee.mower.data.RecordBoundary.Subject.Companion.GRASS_PATH

class ActionSetPoint(val subject: Int, val result: Int, fragment: SetupMapFragment): ActionRecordBoundary(fragment) {
    override fun execute() {
        when (subject) {
            GRASS, OBSTACLE, CHARGING, GRASS_PATH -> {
                binding.mapView.setPoint()
            }
        }
    }

}
