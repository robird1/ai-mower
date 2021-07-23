package com.ulsee.mower.ui.map

import androidx.core.view.isVisible
import com.ulsee.mower.data.RecordBoundary

class ActionFinishPointMode(val subject: Int, val result: Int, fragment: SetupMapFragment): ActionRecordBoundary(fragment) {
    override fun execute() {
        when (subject) {
            RecordBoundary.Subject.GRASS -> {
//                                            binding.driveModeBtn.isChecked = true
                binding.setPointBtnDisabled.isVisible = true
                binding.setPointTextDisabled.isVisible = true
                binding.setPointButton.isVisible = false
                binding.setPointText.isVisible = false
                binding.instructionText.text = "Drive mode: Drive route will be saved as the mowing area boundary\u200B"
                binding.mapView.mode = SetupMapView.Mode.Drive
            }
            RecordBoundary.Subject.OBSTACLE -> {
                binding.setPointBtnDisabled.isVisible = true
                binding.setPointTextDisabled.isVisible = true
                binding.setPointButton.isVisible = false
                binding.setPointText.isVisible = false
                binding.instructionText.text = "Drive mode: Drive route will be saved as the obstacle area boundary\u200B"
                binding.mapView.mode = SetupMapView.Mode.Drive
            }
            RecordBoundary.Subject.CHARGING -> {
                binding.setPointBtnDisabled.isVisible = true
                binding.setPointTextDisabled.isVisible = true
                binding.setPointButton.isVisible = false
                binding.setPointText.isVisible = false
                binding.instructionText.text = "Drive mode: Drive route will be saved as the charging path\u200B"
                binding.mapView.mode = SetupMapView.Mode.Drive
            }
            RecordBoundary.Subject.GRASS_PATH -> {
                binding.setPointBtnDisabled.isVisible = true
                binding.setPointTextDisabled.isVisible = true
                binding.setPointButton.isVisible = false
                binding.setPointText.isVisible = false
                binding.instructionText.text = "Drive mode: Drive route will be saved as the grass path\u200B"
                binding.mapView.mode = SetupMapView.Mode.Drive
            }
        }
    }

}
