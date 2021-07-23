package com.ulsee.mower.ui.map

import android.widget.Toast
import com.ulsee.mower.data.RecordBoundary

class ActionStartRecord(val subject: Int, val result: Int, fragment: SetupMapFragment): ActionRecordBoundary(fragment) {
    override fun execute() {
        when (subject) {
            RecordBoundary.Subject.GRASS -> {
                binding.mapView.setGrassStartPoint()
                binding.mapView.mode = SetupMapView.Mode.Drive
                fragment.state.onNextState()

                Toast.makeText(context, "[Success] START_RECORD", Toast.LENGTH_SHORT).show()

            }
            RecordBoundary.Subject.OBSTACLE -> {
                binding.mapView.mode = SetupMapView.Mode.Drive
                fragment.state.onNextState()

                Toast.makeText(context, "[Success] START_RECORD", Toast.LENGTH_SHORT).show()

            }
            RecordBoundary.Subject.CHARGING -> {
                binding.mapView.setChargingStation()
                binding.mapView.mode = SetupMapView.Mode.Drive
                fragment.state.onNextState()

                Toast.makeText(context, "[Success] START_RECORD", Toast.LENGTH_SHORT).show()

            }
            RecordBoundary.Subject.GRASS_PATH -> {
                binding.mapView.mode = SetupMapView.Mode.Drive
                fragment.state.onNextState()

                Toast.makeText(context, "[Success] START_RECORD", Toast.LENGTH_SHORT).show()

            }
        }
    }

}
