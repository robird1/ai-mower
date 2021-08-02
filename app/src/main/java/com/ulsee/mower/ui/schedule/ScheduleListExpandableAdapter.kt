package com.ulsee.mower.ui.schedule

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseExpandableListAdapter
import android.widget.CheckBox
import android.widget.TextView
import androidx.core.view.isVisible
import com.ulsee.mower.R

class ScheduleListExpandableAdapter(val context: Context, val weeks: List<String>, val details: List<String>): BaseExpandableListAdapter() {
    override fun getGroupCount(): Int {
        return weeks.size
    }

    override fun getChildrenCount(groupPosition: Int): Int {
        return if (details[groupPosition].isNotEmpty()) 1 else 0
    }

    override fun getGroup(groupPosition: Int): Any {
        return weeks[groupPosition]
    }

    override fun getChild(groupPosition: Int, childPosition: Int): Any {
        return details[groupPosition]
    }

    override fun getGroupId(groupPosition: Int): Long {
        return groupPosition.toLong()
    }

    override fun getChildId(groupPosition: Int, childPosition: Int): Long {
        return groupPosition.toLong()
    }

    override fun hasStableIds(): Boolean {
        return true//?
    }

    override fun getGroupView(groupPosition: Int, isExpanded: Boolean, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_list_schedulelist_weekday, null)
        val textView = view.findViewById<TextView>(R.id.textView)
        textView.text = weeks[groupPosition]

        val checkBox = view.findViewById<CheckBox>(R.id.checkbox)
        checkBox.isChecked = !details[groupPosition].isEmpty()

        val divider = view.findViewById<View>(R.id.view_divider)
        divider.isVisible = groupPosition != 0
        return view
    }

    override fun getChildView(groupPosition: Int, childPosition: Int, isLastChild: Boolean, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_list_schedulelist_detail, null)
        val textView = view.findViewById<TextView>(R.id.textView)
        textView.text = details[groupPosition]
        return view
    }

    override fun isChildSelectable(groupPosition: Int, childPosition: Int): Boolean {
        return false
    }
}