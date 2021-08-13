package com.ulsee.mower.ui.schedule

import android.content.Context
import android.content.DialogInterface
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ulsee.mower.R
import com.wdullaer.materialdatetimepicker.time.TimePickerDialog


class ScheduleEditorAdapter(
    private val fragmentManager: FragmentManager,
    private val onClick: (ScheduleEvent) -> Unit,
    private val onDelete: (ScheduleEvent) -> Unit,
    private val onAdd: () -> Unit
) :
        ListAdapter<ScheduleEvent, RecyclerView.ViewHolder>(DeviceDiffCallback) {

    class ScheduleEventViewHolder(
        itemView: View,
        val fragmentManager: FragmentManager,
        val onClick: (ScheduleEvent) -> Unit,
        val onRemove: (ScheduleEvent) -> Unit
    ) :
            RecyclerView.ViewHolder(itemView) {
        private val titleTV: TextView = itemView.findViewById(R.id.textView_title)
        private val beginAtTV: TextView = itemView.findViewById(R.id.textView_beginAt)
        private val durationTV: TextView = itemView.findViewById(R.id.textView_duration)
        private val durationLayout: RelativeLayout = itemView.findViewById(R.id.layout_duration)
        private val amButton: Button = itemView.findViewById(R.id.button_am)
        private val pmButton: Button = itemView.findViewById(R.id.button_pm)
        private val lawnsLayout: LinearLayout = itemView.findViewById(R.id.linearLayout_lawns)
        private val moreBtn: ImageButton = itemView.findViewById(R.id.button_more)
        private lateinit var mPopup: PopupMenu
        private var currentScheduleEvent: ScheduleEvent? = null

        init {
            mPopup = PopupMenu(itemView.context, moreBtn)
            mPopup.menu.add("a").setTitle("Remove")

            mPopup.setOnMenuItemClickListener { item: MenuItem? ->
                when (item!!.title) {
                    "Remove" -> {
                        currentScheduleEvent?.let {
                            onRemove(it)
                        }
                    }
                }

                true
            }

            moreBtn.setOnClickListener {
                mPopup.show()
            }

            itemView.setOnClickListener {
                currentScheduleEvent?.let {
                    onClick(it)
                }
            }
            amButton.setOnClickListener {
                currentScheduleEvent?.let {
                    val isAm = it.beginAt < 24
                    if (isAm) return@let
                    currentScheduleEvent!!.beginAt -= 24

                    amButton.isSelected = true
                    pmButton.isSelected = false
                }
            }
            pmButton.setOnClickListener {
                currentScheduleEvent?.let {
                    val isAm = it.beginAt < 24
                    if (!isAm) return@let
                    currentScheduleEvent!!.beginAt += 24

                    amButton.isSelected = false
                    pmButton.isSelected = true
                }
            }
            beginAtTV.setOnClickListener {
//                val context = it.context

                val hour = currentScheduleEvent!!.beginAt / 2
                val minute = if (currentScheduleEvent!!.beginAt % 2 == 0) 0 else 30

                val timePickerDialog =
                    TimePickerDialog.newInstance({ view, hourOfDay, min, second ->
                        currentScheduleEvent?.beginAt = hourOfDay * 2 + if (min == 0) 0 else 1
                        displayTime(currentScheduleEvent!!)
                    }, hour, minute, false)
                timePickerDialog.setTimeInterval(1, 30, 60)
                timePickerDialog.show(fragmentManager, null)
            }
            durationLayout.setOnClickListener {
                val items = arrayOf<CharSequence>(
                    "30 mins",
                    "1 hour",
                    "1 hour 30 mins",
                    "2 hours",
                    "2 hour 30 mins",
                    "3 hours",
                    "3 hour 30 mins",
                    "4 hours",
                    "4 hour 30 mins",
                    "5 hours",
                    "5 hour 30 mins",
                    "6 hours",
                    "6 hour 30 mins",
                    "7 hours",
                    "7 hour 30 mins",
                    "8 hours",
                    "8 hour 30 mins",
                    "9 hours",
                    "9 hour 30 mins",
                    "10 hours",
                    "10 hour 30 mins",
                    "11 hours",
                    "11 hour 30 mins",
                    "12 hours",
                    "12 hour 30 mins",
                    "13 hours",
                    "13 hour 30 mins",
                    "14 hours",
                    "14 hour 30 mins",
                    "15 hours",
                    "15 hour 30 mins",
                    "16 hours",
                    "16 hour 30 mins",
                    "17 hours",
                    "17 hour 30 mins",
                    "18 hours",
                    "18 hour 30 mins",
                    "19 hours",
                    "19 hour 30 mins",
                    "20 hours",
                    "20 hour 30 mins",
                    "21 hours",
                    "21 hour 30 mins",
                    "22 hours",
                    "22 hour 30 mins",
                    "23 hours",
                    "23 hour 30 mins"
                )

                var duration = 0
                val builder: AlertDialog.Builder = AlertDialog.Builder(itemView.context)
                builder.setTitle("Duration")
                builder.setItems(items) { dialog, item -> // Do something with the selection
                    duration = item + 1
                    currentScheduleEvent!!.duration = duration
                    displayDuration(currentScheduleEvent!!)
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                }
                val alert: AlertDialog = builder.create()
                alert.show()
            }
        }

        /* Bind flower name and image. */
        fun bind(data: ScheduleEvent) {
            currentScheduleEvent = data

            titleTV.text = "Schedule"
            displayTime(data)

            lawnsLayout.removeAllViews()
            appendLawn("lawn1")
            appendLawn("lawn2")

            displayDuration(data)
        }

        fun displayTime(data: ScheduleEvent) {
            val isAm = data.beginAt < 24
            val hour = data.beginAt/2 + (if(isAm)0 else -12)
            val minute = if(data.beginAt%2 == 0) 0 else 30
            beginAtTV.text = String.format("%02d:%02d", hour, minute)
            amButton.isSelected = isAm
            pmButton.isSelected = !isAm
        }

        fun displayDuration(data: ScheduleEvent) {
            val hours = data.duration / 2
            var mins = if(data.duration % 2 == 1) 30 else 0
            if (mins == 0) {
                durationTV.text = String.format("%d hours", hours)
            } else {
                durationTV.text = String.format("%d hours %02d mins", hours, mins)
            }
        }

        fun appendLawn(title: String) {
            val context = itemView.context

            val v: View = (context?.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater).inflate(
                R.layout.item_schedule_editor_lawn_checkbox,
                null
            )
            val checkBox = v.findViewById<CheckBox>(R.id.checkbox)
            checkBox.text = title

            val layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            lawnsLayout.addView(v, layoutParams)

//            val density = context.resources.displayMetrics.density
//            val v: CheckBox = CheckBox(context)
//
//            val height = 44f * density
//            val layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height.toInt())
//            lawnsLayout.addView(v, layoutParams)
        }
    }

    class AddScheduleViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tv: TextView = itemView.findViewById(R.id.textView)
        private val iconIV: ImageView = itemView.findViewById(R.id.imageView_add)

        fun setAddEnabled(isAddable: Boolean) {
            if(isAddable) {
                tv.text = "Add Schedule"
                iconIV.visibility = View.VISIBLE
            } else {
                tv.text = "Up to 5 schedules in a day"
                iconIV.visibility = View.GONE
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        if (viewType == 1) {
            val view = LayoutInflater.from(parent.context).inflate(
                R.layout.item_schedule_editor,
                parent,
                false
            )
            return ScheduleEventViewHolder(view, fragmentManager, onClick, onDelete)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(
                R.layout.item_schedule_editor_add_schedule,
                parent,
                false
            )
            return AddScheduleViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if(currentList.size > position) {
            (holder as ScheduleEventViewHolder).bind(getItem(position))
        } else {
            (holder as AddScheduleViewHolder).setAddEnabled(position < 5)
            holder.itemView.setOnClickListener {
                if (position<5) onAdd()
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

object DeviceDiffCallback : DiffUtil.ItemCallback<ScheduleEvent>() {
    override fun areItemsTheSame(oldItem: ScheduleEvent, newItem: ScheduleEvent): Boolean {
        return oldItem == newItem
    }

    override fun areContentsTheSame(oldItem: ScheduleEvent, newItem: ScheduleEvent): Boolean {
        return oldItem.beginAt == newItem.beginAt && oldItem.duration == newItem.duration
    }
}
