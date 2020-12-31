package net.babys_care.app.extensions

import android.content.res.ColorStateList
import android.graphics.Color
import android.widget.EditText
import androidx.core.view.ViewCompat

fun EditText.enableErrorColor(isEnabled: Boolean) {
    val colorState = ColorStateList.valueOf(Color.parseColor(if (isEnabled) "#e41400" else "#dddddd"))
    ViewCompat.setBackgroundTintList(this, colorState)
}