package com.ulsee.mower.ui.map

import android.widget.Toast
import com.ulsee.mower.data.RecordBoundary.Subject.Companion.GRASS
import com.ulsee.mower.data.RecordBoundary.Subject.Companion.OBSTACLE
import com.ulsee.mower.data.RecordBoundary.Subject.Companion.CHARGING
import com.ulsee.mower.data.RecordBoundary.Subject.Companion.GRASS_PATH

class ActionStartRecord(val subject: Int, val result: Int, fragment: SetupMapFragment): ActionRecordBoundary(fragment) {
    override fun execute() {
        when (subject) {
            GRASS -> {
                binding.mapView.setGrassStartPoint()
                doAction()
            }
            OBSTACLE -> {
                doAction()
            }
            CHARGING -> {
                binding.mapView.setChargingStation()
                doAction()
            }
            GRASS_PATH -> {
                doAction()
            }
        }
    }

    private fun doAction() {
        binding.mapView.mode = SetupMapView.Mode.Drive
        fragment.state.onNextState()
        Toast.makeText(context, "[Success] START_RECORD", Toast.LENGTH_SHORT).show()
    }

}
