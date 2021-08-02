package com.ulsee.mower.ui.schedule

//import android.app.TimePickerDialog
import com.wdullaer.materialdatetimepicker.time.TimePickerDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ulsee.mower.R


data class SingleSchedule (
    var beginAt: Int,//0~47
    var duration: Int//1~48
)
class ScheduleEditorAdapter(private val fragmentManager: FragmentManager, private val onClick: (SingleSchedule) -> Unit, private val onAdd: () -> Unit) :
        ListAdapter<SingleSchedule, RecyclerView.ViewHolder>(DeviceDiffCallback) {

    class SingleScheduleViewHolder(itemView: View, val fragmentManager: FragmentManager, val onClick: (SingleSchedule) -> Unit) :
            RecyclerView.ViewHolder(itemView) {
        private val titleTV: TextView = itemView.findViewById(R.id.textView_title)
        private val beginAtTV: TextView = itemView.findViewById(R.id.textView_beginAt)
        private val amButton: Button = itemView.findViewById(R.id.button_am)
        private val pmButton: Button = itemView.findViewById(R.id.button_pm)
        private val lawnsLayout: LinearLayout = itemView.findViewById(R.id.linearLayout_lawns)
        private var currentSingleSchedule: SingleSchedule? = null

        init {
            itemView.setOnClickListener {
                currentSingleSchedule?.let {
                    onClick(it)
                }
            }
            amButton.setOnClickListener {
                currentSingleSchedule?.let {
                    val isAm = it.beginAt < 24
                    if (isAm) return@let
                    currentSingleSchedule!!.beginAt -= 24

                    amButton.isSelected = true
                    pmButton.isSelected = false
                }
            }
            pmButton.setOnClickListener {
                currentSingleSchedule?.let {
                    val isAm = it.beginAt < 24
                    if (!isAm) return@let
                    currentSingleSchedule!!.beginAt += 24

                    amButton.isSelected = false
                    pmButton.isSelected = true
                }
            }
            beginAtTV.setOnClickListener {
//                val context = it.context

                val hour = currentSingleSchedule!!.beginAt/2
                val minute = if(currentSingleSchedule!!.beginAt%2 == 0) 0 else 30

                val timePickerDialog = TimePickerDialog.newInstance({ view, hourOfDay, min, second ->
                    currentSingleSchedule?.beginAt = hourOfDay*2 + if(min==0)0 else 1
                    displayTime(currentSingleSchedule!!)
                }, hour, minute, false)
                timePickerDialog.setTimeInterval(1, 30, 60)
                timePickerDialog.show(fragmentManager, null)
            }
        }

        /* Bind flower name and image. */
        fun bind(data: SingleSchedule) {
            currentSingleSchedule = data

            titleTV.text = "Schedule"
            displayTime(data)

            lawnsLayout.removeAllViews()
            appendLawn("lawn1")
            appendLawn("lawn2")
        }

        fun displayTime(data: SingleSchedule) {
            val isAm = data.beginAt < 24
            val hour = data.beginAt/2 + (if(isAm)0 else -12)
            val minute = if(data.beginAt%2 == 0) 0 else 30
            beginAtTV.text = String.format("%02d:%02d", hour, minute)
            amButton.isSelected = isAm
            pmButton.isSelected = !isAm
        }

        fun appendLawn(title: String) {
            val context = itemView.context

            val v: View = (context?.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater).inflate(R.layout.item_schedule_editor_lawn_checkbox, null)
            val checkBox = v.findViewById<CheckBox>(R.id.checkbox)
            checkBox.text = title

            val layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            lawnsLayout.addView(v, layoutParams)

//            val density = context.resources.displayMetrics.density
//            val v: CheckBox = CheckBox(context)
//
//            val height = 44f * density
//            val layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height.toInt())
//            lawnsLayout.addView(v, layoutParams)
        }
    }

    class AddScheduleViewHolder(view: View) : RecyclerView.ViewHolder(view)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        if (viewType == 1) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_schedule_editor, parent, false)
            return SingleScheduleViewHolder(view, fragmentManager, onClick)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_schedule_editor_add_schedule, parent, false)
            return AddScheduleViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder:  RecyclerView.ViewHolder, position: Int) {
        if(currentList.size > position) {
            (holder as SingleScheduleViewHolder).bind(getItem(position))
        } else {
            holder.itemView.setOnClickListener {
                onAdd()
            }
        }
    }

    override fun getItemCount(): Int {
        return currentList.size + 1
    }

    override fun getItemViewType(position: Int): Int {
        return if(currentList.size > position) {
            1
        } else {
            -1
        }
    }
}

object DeviceDiffCallback : DiffUtil.ItemCallback<SingleSchedule>() {
    override fun areItemsTheSame(oldItem: SingleSchedule, newItem: SingleSchedule): Boolean {
        return oldItem == newItem
    }

    override fun areContentsTheSame(oldItem: SingleSchedule, newItem: SingleSchedule): Boolean {
        return oldItem.beginAt == newItem.beginAt && oldItem.duration == newItem.duration
    }
}
