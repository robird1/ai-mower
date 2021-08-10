package com.ulsee.mower.ui.map

import android.widget.Toast
import com.ulsee.mower.data.RecordBoundary

class ActionDiscardBoundary(val subject: Int, val result: Int, fragment: SetupMapFragment): ActionRecordBoundary(fragment) {
    override fun execute() {
        when (subject) {
            RecordBoundary.Subject.GRASS -> {
                Toast.makeText(context, "discard successfully", Toast.LENGTH_SHORT).show()
                binding.mapView.resetCurrentWork()
//                fragment.state.onNextState()

                fragment.isSaveOrDiscardAppeared = false
            }
        }

        fragment.isTestOrSaveAppeared = false
    }

}
