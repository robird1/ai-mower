package com.ulsee.mower.ui.connect

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.ulsee.mower.R
import com.ulsee.mower.data.model.Device
import java.lang.Exception

private val TAG = RobotListAdapter::class.java.simpleName

class RobotListAdapter(private val viewModel: RobotListFragmentViewModel, private val progressBar: ConstraintLayout)  : RecyclerView.Adapter<RobotListAdapter.ViewHolder>() {

    var deviceList : List<Device> = ArrayList()
    fun setList(list: List<Device>) {
        deviceList = list
        notifyDataSetChanged()
    }
    fun getList():List<Device> {
        return deviceList
    }

    override fun getItemCount(): Int = this.deviceList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(deviceList[position])
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_list_device, parent, false)
        return ViewHolder(view)
    }

    inner class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        private val nameTV = itemView.findViewById<TextView>(R.id.text_sn)
        var device : Device? = null

        init {
            itemView.setOnClickListener {
//                Log.d(TAG, "[Enter] onClick()")
                device?.let { it1 ->
                    progressBar.isVisible = true
                    viewModel.connectBLEDevice(device!!.getSerialNumber())
                }
            }
        }

        fun bind(device: Device) {
            this.device = device
            nameTV?.text = device.getSerialNumber()
        }
    }
}