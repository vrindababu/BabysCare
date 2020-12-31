package net.babys_care.app.extensions

import java.text.SimpleDateFormat
import java.util.*

fun Date.toDateFormatWithDay(): String? {
    return try {
        val dateFormat = SimpleDateFormat("yyyy/MM/dd (EEE)", Locale.JAPAN)
        dateFormat.format(this)
    }catch (ex: Exception) {
        null
    }
}

fun Date.toDateFormatYMD(): String? {
    return try {
        val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.JAPAN)
        dateFormat.format(this)
    } catch (ex: Exception) {
        null
    }
}

fun Date.toDayFormat(): String {
    return try {
        val dateFormat = SimpleDateFormat("dd", Locale.JAPAN)
        dateFormat.format(this)
    } catch (ex: Exception) {
        "0"
    }
}

fun Date.toDotDateFormatYMD(): String? {
    return try {
        val dateFormat = SimpleDateFormat("yyyy.MM.dd", Locale.JAPAN)
        dateFormat.format(this)
    } catch (ex: Exception) {
        null
    }
}

fun Date.toMonthDay(): String? {
    return try {
        val dateFormat = SimpleDateFormat("MM.dd", Locale.JAPAN)
        dateFormat.format(this)
    } catch (ex: Exception) {
        null
    }
}