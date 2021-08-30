package com.ulsee.mower.ui.map

import androidx.core.view.isVisible
import com.ulsee.mower.data.RecordBoundary.Subject.Companion.GRASS
import com.ulsee.mower.data.RecordBoundary.Subject.Companion.OBSTACLE
import com.ulsee.mower.data.RecordBoundary.Subject.Companion.CHARGING
import com.ulsee.mower.data.RecordBoundary.Subject.Companion.GRASS_PATH

class ActionStartPointMode(val subject: Int, val result: Int, fragment: SetupMapFragment): ActionRecordBoundary(fragment) {
    override fun execute() {
        when (subject) {
            GRASS -> {
                doAction()
            }
            OBSTACLE -> {
                doAction()
            }
            CHARGING -> {
                doAction()
            }
            GRASS_PATH -> {
                doAction()
            }
        }
    }

    private fun doAction() {
        binding.setPointBtnDisabled.isVisible = false
        binding.setPointTextDisabled.isVisible = false
        binding.setPointButton.isVisible = true
        binding.setPointText.isVisible = true
        binding.instructionText.text =
            "Dot mode: Set points will be connected by straight lines and saved as the mowing area boundary\u200B"
        binding.mapView.mode = SetupMapView.Mode.SetPoint
        binding.mapView.setPoint()
    }

}
