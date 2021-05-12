package com.ulsee.mower

import android.bluetooth.le.ScanResult
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView

private val TAG = ScanListAdapter::class.java.simpleName

class ScanListAdapter(private val viewModel: ConnectDeviceActivityViewModel, private val progressBar: ProgressBar)  : RecyclerView.Adapter<ScanListAdapter.ViewHolder>() {

    var scanResult : List<ScanResult> = ArrayList()
    fun setList(list: List<ScanResult>) {
        scanResult = list
        notifyDataSetChanged()
    }
    fun getList():List<ScanResult> {
        return scanResult
    }

    override fun getItemCount(): Int = this.scanResult.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(scanResult[position])
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_list_wifi, parent, false)
        return ViewHolder(view)
    }

    inner class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        private val nameTV = itemView?.findViewById<TextView>(R.id.textView_ssid)
        var scanInfo : ScanResult? = null

        init {
            itemView.setOnClickListener {
                Log.d(TAG, "[Enter] onClick()")
                scanInfo?.let { it1 ->
                    progressBar.isVisible = true
                    viewModel.connectBLEDevice(itemView.context, "JCF20210302H0000001")
                }
            }
        }

        fun bind(scanInfo: ScanResult) {
            this.scanInfo = scanInfo
            nameTV?.text = scanInfo.device.address
        }
    }
}