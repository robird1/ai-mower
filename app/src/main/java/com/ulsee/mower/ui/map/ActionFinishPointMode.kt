package com.ulsee.mower.ui.map

import androidx.core.view.isVisible
import com.ulsee.mower.data.RecordBoundary.Subject.Companion.GRASS
import com.ulsee.mower.data.RecordBoundary.Subject.Companion.OBSTACLE
import com.ulsee.mower.data.RecordBoundary.Subject.Companion.CHARGING
import com.ulsee.mower.data.RecordBoundary.Subject.Companion.GRASS_PATH

class ActionFinishPointMode(val subject: Int, val result: Int, fragment: SetupMapFragment): ActionRecordBoundary(fragment) {
    override fun execute() {
        when (subject) {
            GRASS -> {
                doAction()
                binding.instructionText.text = "Drive mode: Drive route will be saved as the mowing area boundary\u200B"
            }
            OBSTACLE -> {
                doAction()
                binding.instructionText.text = "Drive mode: Drive route will be saved as the obstacle area boundary\u200B"
            }
            CHARGING -> {
                doAction()
                binding.instructionText.text = "Drive mode: Drive route will be saved as the charging path\u200B"
            }
            GRASS_PATH -> {
                doAction()
                binding.instructionText.text = "Drive mode: Drive route will be saved as the grass path\u200B"
            }
        }
    }

    private fun doAction() {
        binding.setPointBtnDisabled.isVisible = true
        binding.setPointTextDisabled.isVisible = true
        binding.setPointButton.isVisible = false
        binding.setPointText.isVisible = false
        binding.mapView.mode = SetupMapView.Mode.Drive
    }

}
