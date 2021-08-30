package com.ulsee.mower.ui.map

import android.widget.Toast
import com.ulsee.mower.data.RecordBoundary.Subject.Companion.GRASS
import com.ulsee.mower.data.RecordBoundary.Subject.Companion.OBSTACLE
import com.ulsee.mower.data.RecordBoundary.Subject.Companion.CHARGING
import com.ulsee.mower.data.RecordBoundary.Subject.Companion.GRASS_PATH

class ActionCancelRecord(val subject: Int, val result: Int, fragment: SetupMapFragment): ActionRecordBoundary(fragment) {
    override fun execute() {
        when (subject) {
            GRASS, OBSTACLE, CHARGING, GRASS_PATH -> {
                binding.mapView.resetCurrentWork()
//                        fragment.state = StateControlPanel(fragment)
//                        fragment.state.createView()
                fragment.state.onNextState()

                Toast.makeText(context, "[Success] CANCEL_RECORD", Toast.LENGTH_SHORT).show()
            }
        }
    }

}
