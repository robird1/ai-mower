package com.ulsee.mower.ui.schedule

import java.io.Serializable
import java.lang.IllegalArgumentException

data class Calendar(
    val utc: Int,
    val calendarRawData: List<Byte>
) : Serializable {
    var schedules: ArrayList<ArrayList<ScheduleEvent>> = ArrayList()
    init {
        if(calendarRawData.size != 70) throw IllegalArgumentException("Calendar::calendarRawData size must be 70")
        for(weekday in 1..7) {
            val schedulesOfTheWeekDay : ArrayList<ScheduleEvent> = ArrayList()
            for(scheduleIdx in 1..5) {
                val beginIdx = (weekday-1)*10 + (scheduleIdx-1)*2
                val durationIdx = (weekday-1)*10 + (scheduleIdx-1)*2+1
                val beginAt = calendarRawData[beginIdx].toUByte().toInt()
                val duration = calendarRawData[durationIdx].toUByte().toInt()
                if (beginAt < 0 || beginAt > 47) throw IllegalArgumentException("Calendar::beginAt must in [0, 47]")
                if (duration < 0 || duration > 48) throw IllegalArgumentException("Calendar::duration must in [0, 48]")
                if (duration == 0) continue // ignore
                schedulesOfTheWeekDay.add(ScheduleEvent(
                    beginAt,
                    duration
                ))
            }
            schedules.add(schedulesOfTheWeekDay)
        }
    }
}
data class ScheduleEvent(
    var beginAt: Int, // half hours
    var duration: Int, // half hours
)