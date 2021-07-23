package com.ulsee.mower.ui.map

import androidx.core.view.isVisible
import com.ulsee.mower.data.RecordBoundary

class ActionFinishRecord(val subject: Int, val result: Int, fragment: SetupMapFragment): ActionRecordBoundary(fragment) {
    override fun execute() {
        when (subject) {
            RecordBoundary.Subject.GRASS -> {
                binding.mapView.finishGrassBorder()
                fragment.state.onNextState()

                binding.mapView.mode = SetupMapView.Mode.None
            }
            RecordBoundary.Subject.OBSTACLE -> {
                binding.progressView.isVisible = false
                binding.progressView.isVisible = true

                viewModel.recordBoundary(
                    RecordBoundary.Command.SAVE_BOUNDARY,
                    RecordBoundary.Subject.OBSTACLE
                )

                binding.mapView.finishObstacleBorder()
//                        fragment.state.goToNextState()
                binding.mapView.mode = SetupMapView.Mode.None

            }
            RecordBoundary.Subject.CHARGING -> {
                binding.progressView.isVisible = false
                binding.progressView.isVisible = true

                viewModel.recordBoundary(
                    RecordBoundary.Command.SAVE_BOUNDARY,
                    RecordBoundary.Subject.CHARGING
                )

                binding.mapView.finishChargingRoute()
//                        fragment.state.goToNextState()
                binding.mapView.mode = SetupMapView.Mode.None

            }
            RecordBoundary.Subject.GRASS_PATH -> {
                binding.progressView.isVisible = false
                binding.progressView.isVisible = true

                viewModel.recordBoundary(
                    RecordBoundary.Command.SAVE_BOUNDARY,
                    RecordBoundary.Subject.GRASS_PATH
                )

                binding.mapView.finishGrassRoute()
//                        fragment.state.goToNextState()
                binding.mapView.mode = SetupMapView.Mode.None

            }
        }
    }

}
