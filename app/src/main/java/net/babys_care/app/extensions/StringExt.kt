package net.babys_care.app.extensions

import java.text.SimpleDateFormat
import java.util.*

fun String.toDateJapaneseWithYear(): String? {
    return try {
        var dateFormat = if (this.contains("/")) {
            SimpleDateFormat("yyyy/MM/dd", Locale.JAPAN)
        } else {
            SimpleDateFormat("yyyy-MM-dd", Locale.JAPAN)
        }
        val date = dateFormat.parse(this) ?: return null
        dateFormat = SimpleDateFormat("yyyy年MM月dd日", Locale.JAPAN)
        dateFormat.format(date)
    }catch (ex: Exception) {
        null
    }
}

fun String.toDotDateFormat(): String? {
    return try {
        var dateFormat = if (this.contains("/")) {
            SimpleDateFormat("yyyy/MM/dd", Locale.JAPAN)
        } else {
            SimpleDateFormat("yyyy-MM-dd", Locale.JAPAN)
        }
        val date = dateFormat.parse(this) ?: return null
        dateFormat = SimpleDateFormat("yyyy.MM.dd", Locale.JAPAN)
        dateFormat.format(date)
    }catch (ex: Exception) {
        null
    }
}

fun String.toMonthDateDayFormat(): String? {
    return try {
        var dateFormat = if (this.contains("/")) {
            SimpleDateFormat("yyyy/MM/dd", Locale.JAPAN)
        } else {
            SimpleDateFormat("yyyy-MM-dd", Locale.JAPAN)
        }
        val date = dateFormat.parse(this) ?: return null
        dateFormat = SimpleDateFormat("MM/dd (EEE)", Locale.JAPAN)
        dateFormat.format(date)
    }catch (ex: Exception) {
        null
    }
}

fun String.toDate() : Date? {
    return try {
        val formatter = when {
            this.contains("/") && this.contains("T") -> {
                SimpleDateFormat("yyyy/MM/dd'T'HH:mm:ss", Locale.JAPAN)
            }
            this.contains("-") && this.contains("T") -> {
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.JAPAN)
            }
            this.contains("/") -> {
                SimpleDateFormat("yyyy/MM/dd", Locale.JAPAN)
            }
            else -> {
                SimpleDateFormat("yyyy-MM-dd", Locale.JAPAN)
            }
        }
        formatter.parse(this)
    } catch (ex: Exception) {
        null
    }
}