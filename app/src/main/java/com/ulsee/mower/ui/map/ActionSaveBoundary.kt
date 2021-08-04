package com.ulsee.mower.ui.map

import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.core.view.isVisible
import com.ulsee.mower.data.RecordBoundary

private val TAG = ActionSaveBoundary::class.java.simpleName

class ActionSaveBoundary(val subject: Int, val result: Int, fragment: SetupMapFragment, val intent: Intent): ActionRecordBoundary(fragment) {
    override fun execute() {
        when (subject) {
            RecordBoundary.Subject.GRASS -> {
                Toast.makeText(context, "save successfully", Toast.LENGTH_SHORT).show()

                binding.progressView.isVisible = true
                binding.mapView.resetData()
                viewModel.getMapGlobalParameters()
                Log.d(TAG, "[Enter] getMapGlobalParameters()")
                Log.d("123", "[Enter] getMapGlobalParameters()")

                fragment.isTestOrSaveAppeared = false
                fragment.isSaveOrDiscardAppeared = false

            }
            RecordBoundary.Subject.OBSTACLE -> {
                binding.progressView.isVisible = false

                val grassNumber = intent.getIntExtra("grass_number", -1)
                val obstacleNumber = intent.getIntExtra("obstacle_number", -1)

                val key = "obstacle.$grassNumber.$obstacleNumber"
                Log.d("789", "[Success] save boundary. obstacle number: ${key}")

                binding.mapView.saveBoundary(SetupMapView.WorkType.OBSTACLE, key)

                Toast.makeText(context, "save successfully", Toast.LENGTH_SHORT).show()

                fragment.state.onNextState()

            }
            RecordBoundary.Subject.CHARGING -> {
                binding.progressView.isVisible = false

                val grassNumber = intent.getIntExtra("grass_number", -1)
                val pathNumber = intent.getIntExtra("path_number", -1)

                val key = "charging.$grassNumber.$pathNumber"
                Log.d("789", "[Success] save boundary. charging path number: ${key}")

                binding.mapView.saveBoundary(SetupMapView.WorkType.CHARGING_ROUTE, key)


                Toast.makeText(context, "save successfully", Toast.LENGTH_SHORT).show()

                fragment.state.onNextState()

            }
            RecordBoundary.Subject.GRASS_PATH -> {
                binding.progressView.isVisible = false

                val grassNumber = intent.getIntExtra("grass_number", -1)
                val targetGrassNumber = intent.getIntExtra("target_grass_number", -1)
                val pathNumber = intent.getIntExtra("path_number", -1)

                val key = "route.$grassNumber.$targetGrassNumber.$pathNumber"
                Log.d("789", "[Success] save boundary. grass route number: ${key}")

                binding.mapView.saveBoundary(SetupMapView.WorkType.GRASS_ROUTE, key)


                Toast.makeText(context, "save successfully", Toast.LENGTH_SHORT).show()

                fragment.state.onNextState()

            }
        }
    }

}
