package com.ulsee.mower.ui.map

import android.app.AlertDialog
import android.util.Log
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import com.ulsee.mower.data.RecordBoundary.Command.Companion.CANCEL_RECORD
import com.ulsee.mower.data.RecordBoundary.Command.Companion.FINISH_POINT_MODE
import com.ulsee.mower.data.RecordBoundary.Command.Companion.FINISH_RECORD
import com.ulsee.mower.data.RecordBoundary.Command.Companion.SET_POINT
import com.ulsee.mower.data.RecordBoundary.Command.Companion.START_POINT_MODE
import com.ulsee.mower.data.RecordBoundary.Command.Companion.START_RECORD
import com.ulsee.mower.data.RecordBoundary.Subject.Companion.CHARGING
import com.ulsee.mower.data.RecordBoundary.Subject.Companion.GRASS
import com.ulsee.mower.data.RecordBoundary.Subject.Companion.GRASS_PATH
import com.ulsee.mower.data.RecordBoundary.Subject.Companion.OBSTACLE


private val TAG = SetupMapState::class.java.simpleName

abstract class SetupMapState(private val fragment: SetupMapFragment) {

    val binding = fragment.binding
    val bleService = fragment.bluetoothService
    val viewModel = fragment.viewModel
    val context = fragment.context

    fun createView() {
        Log.d("456", "[Enter] createView() state: ${fragment.state}")
        setTitleHint()
        onResetView()
        onSetupListeners()
        hideAllFooterButtons()
        onFooterButtonsView().isVisible = true
    }

    abstract fun onTitleHintText(): String
    abstract fun onFooterButtonsView(): ConstraintLayout
    abstract fun onSetupListeners()
    abstract fun onNextState()
    abstract fun onBackPressed()
    abstract fun onResetView()

    private fun setTitleHint() {
        binding.titleHintText.text = onTitleHintText()
    }

    private fun hideAllFooterButtons() {
        binding.footerViewStartGrass.isVisible = false
        binding.footerViewStartObstacle.isVisible = false
        binding.footerViewStartCharging.isVisible = false
        binding.footerViewStartRoute.isVisible = false
        binding.footerViewMode.isVisible = false
        binding.footerViewControlPanel.isVisible = false
        binding.footerViewSelectDeleteType.isVisible = false
        binding.footerViewDeleteElement.isVisible = false
    }

}


class StartGrass(private val fragment: SetupMapFragment) : SetupMapState(fragment) {

    override fun onTitleHintText() = "STEP 1. Drive the mower to the lawn. Tap START when arrive the boundary.\u200B\n" +
            "* If there is more than one mowing area, start from the one closest to the charging station\u200B"

    override fun onFooterButtonsView() = binding.footerViewStartGrass

    override fun onSetupListeners() {
        binding.startGrassIcon.setOnClickListener {
            if (fragment.signalQuality == 0) {
                Toast.makeText(context, "Weak satellite signal", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewModel.recordBoundary(START_RECORD, GRASS)
        }
    }

    override fun onNextState() {
        fragment.state = RecordGrass(fragment)
        fragment.state.createView()
    }

    override fun onBackPressed() {
        fragment.state = StateControlPanel(fragment)
        fragment.state.createView()
    }

    override fun onResetView() {
        binding.startGrassIcon.setOnClickListener(null)
    }

}


class StartObstacle(private val fragment: SetupMapFragment) : SetupMapState(fragment) {

    override fun onTitleHintText() = "Drive your lawn mower to the obstacle and tap ”START” to start record obstacle boundary."

    override fun onFooterButtonsView() = binding.footerViewStartObstacle

    override fun onSetupListeners() {
        binding.startObstacleIcon.setOnClickListener {
            if (fragment.signalQuality == 0) {
                Toast.makeText(context, "Weak satellite signal", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewModel.recordBoundary(START_RECORD, OBSTACLE)
        }
    }

    override fun onNextState() {
        fragment.state = RecordObstacle(fragment)
        fragment.state.createView()
    }

    override fun onBackPressed() {
        fragment.state = StateControlPanel(fragment)
        fragment.state.createView()
    }

    override fun onResetView() {
        binding.startObstacleIcon.setOnClickListener(null)
    }

}


class StartChargingPath(private val fragment: SetupMapFragment) : SetupMapState(fragment) {

    override fun onTitleHintText() = "Charging route: Tap the button \"Set Station\" after mower is parked at charging station."

    override fun onFooterButtonsView() = binding.footerViewStartCharging

    override fun onSetupListeners() {
        binding.startChargingIcon.setOnClickListener {
            if (fragment.signalQuality == 0) {
                Toast.makeText(context, "Weak satellite signal", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewModel.recordBoundary(START_RECORD, CHARGING)
        }
    }

    override fun onNextState() {
        fragment.state = RecordChargingPath(fragment)
        fragment.state.createView()
    }

    override fun onBackPressed() {
        fragment.state = StateControlPanel(fragment)
        fragment.state.createView()
    }

    override fun onResetView() {
        binding.startChargingIcon.setOnClickListener(null)
    }

}


class StartGrassRoute(private val fragment: SetupMapFragment) : SetupMapState(fragment) {

    override fun onTitleHintText() = "Tap the button \"START\" when mower is in grass area."

    override fun onFooterButtonsView() = binding.footerViewStartRoute

    override fun onSetupListeners() {
        binding.startRouteIcon.setOnClickListener {
            if (fragment.signalQuality == 0) {
                Toast.makeText(context, "Weak satellite signal", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewModel.recordBoundary(START_RECORD, GRASS_PATH)
        }
    }

    override fun onNextState() {
        fragment.state = RecordGrassRoute(fragment)
        fragment.state.createView()
    }

    override fun onBackPressed() {
        fragment.state = StateControlPanel(fragment)
        fragment.state.createView()
    }

    override fun onResetView() {
        binding.startRouteIcon.setOnClickListener(null)
    }

}


class StateControlPanel(private val fragment: SetupMapFragment) : SetupMapState(fragment) {

    override fun onTitleHintText() = ""

    override fun onFooterButtonsView() = binding.footerViewControlPanel

    override fun onSetupListeners() {
        binding.editButton.setOnClickListener {
            fragment.state = SelectDeleteType(fragment)
            fragment.state.createView()
        }
        binding.nextLawnButton.setOnClickListener {
            fragment.state = StartGrass(fragment)
            fragment.state.createView()
        }
        binding.obstacleButton.setOnClickListener {
            fragment.state = StartObstacle(fragment)
            fragment.state.createView()
        }
        binding.routeButton.setOnClickListener {
            fragment.state = StartGrassRoute(fragment)
            fragment.state.createView()
        }
        binding.chargingButton.setOnClickListener {
            fragment.state = StartChargingPath(fragment)
            fragment.state.createView()
            showInstructionDialog()
        }
        binding.finishText.setOnClickListener {
            onNextState()
        }
    }

    override fun onNextState() {
        fragment.backToStatusScreen()
    }

    override fun onBackPressed() {
        fragment.backToStatusScreen()
    }

    override fun onResetView() {
        binding.editButton.setOnClickListener(null)
        binding.nextLawnButton.setOnClickListener(null)
        binding.obstacleButton.setOnClickListener(null)
        binding.routeButton.setOnClickListener(null)
    }

    private fun showInstructionDialog() {
        val dialog = AlertDialog.Builder(context)
            .setMessage("The starting point of charging path is only allowed to be set at charging station. Please move your mower to the charging station.")
            .setCancelable(false)
            .setPositiveButton("ok") { it, _ ->
                it.dismiss()
            }
            .create()
        dialog.show()
    }

}


open class RecordGrass(private val fragment: SetupMapFragment) : SetupMapState(fragment) {

    override fun onTitleHintText() = "Use “switch button” to change mode\u200B. Drive the mower along the lawn boundary.  The drive route will be saved as the mowing area border line."

    override fun onFooterButtonsView() = binding.footerViewMode

    override fun onSetupListeners() {
        initEndButtonListener()
        initSetPointBtnListener()
        initDriveBtnListener()
        initResetBtnListener()
    }

    open fun onRecordType() = GRASS

    private fun initResetBtnListener() {
        binding.resetBtn.setOnClickListener {
            if (fragment.signalQuality == 0) {
                Toast.makeText(context, "Weak satellite signal", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewModel.recordBoundary(CANCEL_RECORD, onRecordType())
        }
    }

    private fun initDriveBtnListener() {
        binding.driveModeBtn.setOnCheckedChangeListener { _, isChecked ->
            if (fragment.signalQuality == 0) {
                Toast.makeText(context, "Weak satellite signal", Toast.LENGTH_SHORT).show()
                return@setOnCheckedChangeListener
            }
            if (isChecked) {
                // drive mode
                viewModel.recordBoundary(FINISH_POINT_MODE, onRecordType())
            } else {
                // set point mode
                viewModel.recordBoundary(START_POINT_MODE, onRecordType())
            }
        }
    }

    private fun initSetPointBtnListener() {
        binding.setPointButton.setOnClickListener {
            if (fragment.signalQuality == 0) {
                Toast.makeText(context, "Weak satellite signal", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewModel.recordBoundary(SET_POINT, onRecordType())
        }
    }

    private fun initEndButtonListener() {
        binding.endButton.setOnClickListener {
            if (fragment.signalQuality == 0) {
                Toast.makeText(context, "Weak satellite signal", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            binding.progressView.isVisible = true
            if (!binding.driveModeBtn.isChecked) {        // Point mode
    //                binding.titleHintText.text = "Step 2, use “switch button” to change mode\u200B. Drive the mower to the corners of lawn and tap Set point.  Points will be connected by straight line as the boundary of the mowing area. \u200B"
                viewModel.recordBoundary(FINISH_POINT_MODE, onRecordType())

            } else {      // Drive mode
    //                binding.titleHintText.text = "Step 2, use “switch button” to change mode\u200B. Drive the mower along the lawn boundary.  The drive route will be saved as the mowing area border line."

            }
            viewModel.recordBoundary(FINISH_RECORD, onRecordType())
        }
    }

    override fun onNextState() {
        fragment.state = StateControlPanel(fragment)
        fragment.state.createView()
    }

    override fun onBackPressed() {
        // do nothing
    }

    override fun onResetView() {
        binding.endButton.setOnClickListener(null)
        binding.setPointButton.setOnClickListener(null)
        binding.driveModeBtn.setOnCheckedChangeListener(null)
        binding.resetBtn.setOnClickListener(null)
        binding.driveModeBtn.isChecked = true
        binding.setPointButton.isVisible = false
        binding.setPointBtnDisabled.isVisible = true
        binding.setPointText.isVisible = false
        binding.setPointTextDisabled.isVisible = true
        binding.titleHintText.text = onTitleHintText()
    }
}


class RecordObstacle(fragment: SetupMapFragment): RecordGrass(fragment) {

    override fun onTitleHintText() = "Drive mower to a obstacle and tap ”Obstacle” to set obstacle’s boundary. Tap “END” when it’s finished."

    override fun onRecordType() = OBSTACLE

}


class RecordChargingPath(fragment: SetupMapFragment): RecordGrass(fragment) {

    override fun onTitleHintText() = "Drive mower to the grass area. Tap “END” when it’s finished."

    override fun onRecordType() = CHARGING

}


class RecordGrassRoute(fragment: SetupMapFragment): RecordGrass(fragment) {

    override fun onTitleHintText() = "Drive mower to the target grass area. Tap “END” when it’s finished."

    override fun onRecordType() = GRASS_PATH

}


class SelectDeleteType(private val fragment: SetupMapFragment) : SetupMapState(fragment) {

    override fun onTitleHintText() = "Select the element type you want to delete."

    override fun onFooterButtonsView() = binding.footerViewSelectDeleteType

    override fun onSetupListeners() {
        binding.grassIcon.setOnClickListener {
            if (fragment.signalQuality == 0) {
                Toast.makeText(context, "Weak satellite signal", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            binding.mapView.deleteMode = SetupMapView.DeleteType.GRASS
            onNextState()
        }
        binding.obstacleIcon.setOnClickListener {
            if (fragment.signalQuality == 0) {
                Toast.makeText(context, "Weak satellite signal", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            binding.mapView.deleteMode = SetupMapView.DeleteType.OBSTACLE
            onNextState()
        }
        binding.chargingIcon.setOnClickListener {
            if (fragment.signalQuality == 0) {
                Toast.makeText(context, "Weak satellite signal", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            binding.mapView.deleteMode = SetupMapView.DeleteType.CHARGING_ROUTE
            onNextState()
        }
        binding.routeIcon.setOnClickListener {
            if (fragment.signalQuality == 0) {
                Toast.makeText(context, "Weak satellite signal", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            binding.mapView.deleteMode = SetupMapView.DeleteType.GRASS_ROUTE
            onNextState()
        }
    }

    override fun onNextState() {
        fragment.state = DeleteElement(fragment)
        fragment.state.createView()
        binding.mapView.postInvalidate()
    }

    override fun onBackPressed() {
        fragment.state = StateControlPanel(fragment)
        fragment.state.createView()
    }

    override fun onResetView() {
        binding.grassIcon.setOnClickListener(null)
        binding.obstacleIcon.setOnClickListener(null)
        binding.chargingIcon.setOnClickListener(null)
        binding.routeIcon.setOnClickListener(null)
    }

}


class DeleteElement(private val fragment: SetupMapFragment) : SetupMapState(fragment) {

    override fun onTitleHintText() = "Tap the trash can icon to delete specified element."

    override fun onFooterButtonsView() = binding.footerViewDeleteElement

    override fun onSetupListeners() {
        // do nothing
    }

    override fun onNextState() {
        // stay at current screen
    }

    override fun onBackPressed() {
        fragment.state = SelectDeleteType(fragment)
        fragment.state.createView()
        binding.mapView.deleteMode = SetupMapView.DeleteType.NONE
        binding.mapView.postInvalidate()
    }

    override fun onResetView() {
        // do nothing
    }

}





