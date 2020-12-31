package net.babys_care.app.extensions

import java.util.*

fun Calendar.isSameMonth(targetDate: Calendar): Boolean {
    val currentMonth = this.get(Calendar.MONTH)
    val currentYear = this.get(Calendar.YEAR)
    val targetMonth = targetDate.get(Calendar.MONTH)
    val targetYear = targetDate.get(Calendar.YEAR)

    return currentMonth == targetMonth && currentYear == targetYear
}