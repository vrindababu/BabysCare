package net.babys_care.app.utils

import java.util.*

class DateUtils {

    fun getDifferenceInMonths(startDate: Calendar, endDate: Calendar): Int {
        var monthCount = 0
        while (startDate.time.before(endDate.time)) {
            startDate.add(Calendar.MONTH, 1)
            monthCount++
        }

        return monthCount - 1
    }

    fun getDayDifference(currentDate: Calendar, birthCalender: Calendar): Int {
        val calculatedDayDiff = currentDate.get(Calendar.DAY_OF_MONTH) - birthCalender.get(Calendar.DAY_OF_MONTH)
        if (calculatedDayDiff >= 0) {
            return calculatedDayDiff
        }

        val calendar = Calendar.getInstance()
        calendar.time = currentDate.time
        calendar.add(Calendar.MONTH, -1)
        val daysOfMonth = when(calendar.get(Calendar.MONTH)) {
            0,2,4,6,7,9,11 -> 31
            1 -> if (calendar.getActualMaximum(Calendar.DAY_OF_YEAR) > 365) 29 else 28
            else -> 30
        }

        return daysOfMonth + calculatedDayDiff
    }

}