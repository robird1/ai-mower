package com.ulsee.mower.ui.map

import androidx.core.view.isVisible
import com.ulsee.mower.data.RecordBoundary

class ActionStartPointMode(val subject: Int, val result: Int, fragment: SetupMapFragment): ActionRecordBoundary(fragment) {
    override fun execute() {
        when (subject) {
            RecordBoundary.Subject.GRASS -> {
//                                            binding.driveModeBtn.isChecked = false
                binding.setPointBtnDisabled.isVisible = false
                binding.setPointTextDisabled.isVisible = false
                binding.setPointButton.isVisible = true
                binding.setPointText.isVisible = true
                binding.instructionText.text =
                    "Dot mode: Set points will be connected by straight lines and saved as the mowing area boundary\u200B"
                binding.mapView.mode = SetupMapView.Mode.SetPoint

                binding.mapView.setPoint()

            }
            RecordBoundary.Subject.OBSTACLE -> {
                binding.setPointBtnDisabled.isVisible = false
                binding.setPointTextDisabled.isVisible = false
                binding.setPointButton.isVisible = true
                binding.setPointText.isVisible = true
                binding.instructionText.text =
                    "Dot mode: Set points will be connected by straight lines and saved as the mowing area boundary\u200B"
                binding.mapView.mode = SetupMapView.Mode.SetPoint

                binding.mapView.setPoint()

            }
            RecordBoundary.Subject.CHARGING -> {
                binding.setPointBtnDisabled.isVisible = false
                binding.setPointTextDisabled.isVisible = false
                binding.setPointButton.isVisible = true
                binding.setPointText.isVisible = true
                binding.instructionText.text =
                    "Dot mode: Set points will be connected by straight lines and saved as the mowing area boundary\u200B"
                binding.mapView.mode = SetupMapView.Mode.SetPoint

                binding.mapView.setPoint()
            }
            RecordBoundary.Subject.GRASS_PATH -> {
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
    }

}
