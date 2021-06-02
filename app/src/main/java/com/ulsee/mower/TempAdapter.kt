package com.ulsee.mower

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ulsee.mower.data.model.Device

class TempAdapter(private val viewModel: MapFragmentViewModel)  : RecyclerView.Adapter<TempAdapter.ViewHolder>() {

    var statusList : ArrayList<String> = ArrayList()
//    fun setList(list: List<String>) {
//        statusList = list
//        notifyDataSetChanged()
//    }
//    fun getList():List<String> {
//        return statusList
//    }
    fun bind(msg: String) {
        statusList.add(msg)
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = this.statusList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(statusList[position])
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_list_device, parent, false)
        return ViewHolder(view)
    }

    inner class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        private val nameTV = itemView.findViewById<TextView>(R.id.text_sn)
        var status : String? = null

        fun bind(status: String) {
            this.status = status
            nameTV?.text = status
        }
    }
}