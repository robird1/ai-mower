package com.ulsee.mower.ui.map

import android.widget.Toast
import com.ulsee.mower.data.RecordBoundary

class ActionCancelRecord(val subject: Int, val result: Int, fragment: SetupMapFragment): ActionRecordBoundary(fragment) {
    override fun execute() {
        when (subject) {
            RecordBoundary.Subject.GRASS -> {
                binding.mapView.resetCurrentWork(SetupMapView.WorkType.GRASS)
//                        fragment.state = StateControlPanel(fragment)
//                        fragment.state.createView()
                fragment.state.onNextState()

                Toast.makeText(context, "[Success] CANCEL_RECORD", Toast.LENGTH_SHORT).show()

            }
            RecordBoundary.Subject.OBSTACLE -> {
                binding.mapView.resetCurrentWork(SetupMapView.WorkType.OBSTACLE)
//                        fragment.state = StateControlPanel(fragment)
//                        fragment.state.createView()
                fragment.state.onNextState()

                Toast.makeText(context, "[Success] CANCEL_RECORD", Toast.LENGTH_SHORT).show()

            }
            RecordBoundary.Subject.CHARGING -> {
                binding.mapView.resetCurrentWork(SetupMapView.WorkType.CHARGING_ROUTE)
//                        fragment.state = StateControlPanel(fragment)
//                        fragment.state.createView()
                fragment.state.onNextState()

                Toast.makeText(context, "[Success] CANCEL_RECORD", Toast.LENGTH_SHORT).show()

            }
            RecordBoundary.Subject.GRASS_PATH -> {
                binding.mapView.resetCurrentWork(SetupMapView.WorkType.GRASS_ROUTE)
//                        fragment.state = StateControlPanel(fragment)
//                        fragment.state.createView()
                fragment.state.onNextState()

                Toast.makeText(context, "[Success] CANCEL_RECORD", Toast.LENGTH_SHORT).show()

            }

        }
    }

}
