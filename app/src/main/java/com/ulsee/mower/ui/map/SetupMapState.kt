package com.ulsee.mower.ui.map

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import com.ulsee.mower.data.RobotStatusState

abstract class SetupMapState(private val fragment: SetupMapFragment) {
    var gattUpdateReceiver: BroadcastReceiver? = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                RobotStatusState.ACTION_STATUS_RESPONSE -> {
                    val x = intent.getIntExtra("x", 0)
                    val y = intent.getIntExtra("y", 0)
                    val angle = intent.getFloatExtra("angle", 0F)
//                    Log.d(TAG, "x: $x y: $y angle: $angle")

                    // TODO OperationType
//                    binding.mapView.updateRobotPosition(x, y, angle, MapView.OperationType.WorkingBorder)
                    binding.mapView.notifyRobotCoordinate(x, y, angle, fragment.state)
                }

                RobotStatusState.ACTION_BORDER_RECORD_RESPONSE -> {

                }
            }
        }
    }

    val binding = fragment.binding
    val bleService = fragment.bluetoothService
    val viewModel = fragment.viewModel

    fun createView() {
        setTitleHint()
        onSetupListeners()
        hideAllFooterButtons()
        onFooterButtonsView().isVisible = true
    }

    abstract fun onTitleHintText(): String
    abstract fun onFooterButtonsView(): ConstraintLayout
    abstract fun onSetupListeners()
    abstract fun onNextState(): SetupMapState

    private fun setTitleHint() {
        binding.titleHintText.text = onTitleHintText()
    }

    private fun hideAllFooterButtons() {
        binding.footerViewStep1.isVisible = false
        binding.footerViewStep2.isVisible = false
        binding.footerViewStep3.isVisible = false
        binding.footerViewStep4.isVisible = false
        binding.footerViewSetObstacle.isVisible = false
    }

    fun goToNextState() {
        fragment.state = onNextState()
        fragment.state.createView()
    }

}

class SetChargingStation(private val fragment: SetupMapFragment) : SetupMapState(fragment) {

    override fun onTitleHintText() = "STEP 1. Set the charging station and plan the route to the lawn"

    override fun onFooterButtonsView() = binding.footerViewStep1

    override fun onSetupListeners() {
        binding.chargeStationView.setOnClickListener {
            viewModel.getStatusPeriodically()
            binding.mapView.showChargingStation()
//            viewModel.startRecordChargingPath()

//            fragment.state = onNextState()
//            fragment.state.createView()
            goToNextState()
//            onNextState().createView()
        }
    }

    override fun onNextState() = SetupChargingPath(fragment)

//    fun showChargingStation() {
//        binding.mapView.showChargingStation()
//        onNextState().createView()
//    }

}


class SetupChargingPath(private val fragment: SetupMapFragment) : SetupMapState(fragment) {

    override fun onTitleHintText() = "STEP 2. Once you arrive the mowing area, tap the start button."

    override fun onFooterButtonsView() = binding.footerViewStep2

    override fun onSetupListeners() {
        binding.startWorkingBoundaryView.setOnClickListener {
            binding.mapView.showWorkingStartPoint()

//            onNextState().createView()
//            fragment.state = onNextState()
//            fragment.state.createView()
            goToNextState()
        }
    }

    override fun onNextState() = SetWorkingBoundary(fragment)

}


class SetWorkingBoundary(private val fragment: SetupMapFragment) : SetupMapState(fragment) {

    override fun onTitleHintText() = "STEP 3. Drive the lawn mower along the lawn’s boundary, " +
            "and set the boundary point when it needs.\n Tap the “END” button when this lawn area is done. "

    override fun onFooterButtonsView() = binding.footerViewStep3

    override fun onSetupListeners() {
        binding.step3EndButton.setOnClickListener {
            binding.mapView.finishWorkingBorder()
//            binding.mapView.isWithinCanvasBound2()
//            onNextState().createView()
            goToNextState()
        }
        binding.step3SetPointButton.setOnClickListener {
//            viewModel.setWorkingBorderPoint()
            binding.mapView.setWorkingBoundaryPoint()
        }
//        binding.step3SetPointButton.setOnLongClickListener {
//            true
//        }
        binding.step3SwitchButton.setOnClickListener {
            if (binding.mapView.mode == MapView.Mode.Drive) {
                binding.mapView.changeMode(MapView.Mode.SetPoint)
            } else {
                binding.mapView.changeMode(MapView.Mode.Drive)
            }

            val visible = binding.step3SetPointButton.isVisible
            binding.step3SetPointButton.isVisible = !visible
            binding.step3SetPointText.isVisible = !visible
        }
    }

    override fun onNextState() = FinishWorkingBoundary(fragment)

}


class FinishWorkingBoundary(private val fragment: SetupMapFragment) : SetupMapState(fragment) {

    override fun onTitleHintText() = "STEP 4. Tap “Edit map” to edit the boundary. Tap “Next lawn” to " +
            "set another lawn in this map. \nTap “Obstacle” to set the obstacle boundary. Tap “Done” to save this map."

    override fun onFooterButtonsView() = binding.footerViewStep4

    override fun onSetupListeners() {
        binding.step4EditButton.setOnClickListener {

        }
        binding.step4NextLawnButton.setOnClickListener {

        }
        binding.step4ObstacleButton.setOnClickListener {
//            val offsetX = arrayListOf(300, 300, -150)
//            val offsetY = arrayListOf(300, 0, -400)
//            val angles = arrayListOf(90F, 90F, -160F)
//            createFakeRoute(offsetX, offsetY, angles, MapView.OperationType.Obstacle)

            SetObstacle(fragment).createView()
        }
        binding.step4DoneButton.setOnClickListener {

        }
    }

    override fun onNextState(): SetupMapState {
        TODO("Not yet implemented")
    }

}


class SetObstacle(private val fragment: SetupMapFragment) : SetupMapState(fragment) {

    override fun onTitleHintText() = "Obstacle: Drive your lawn mower to the obstacle and " +
            "tap ”Obstacle” to set the obstacle’s boundary. \nTap “END” when it’s finished."

    override fun onFooterButtonsView() = binding.footerViewSetObstacle

    override fun onSetupListeners() {
        binding.obstacleButton.setOnClickListener {
            binding.mapView.setObstaclePoint()
        }
        binding.obstacleEndButton.setOnClickListener {
            binding.mapView.finishObstacleBorder()
            goToNextState()
        }
    }

    override fun onNextState() = FinishWorkingBoundary(fragment)

}


