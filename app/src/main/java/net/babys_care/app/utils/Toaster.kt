package net.babys_care.app.utils

import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import android.widget.Toast
import net.babys_care.app.AppManager
import net.babys_care.app.R
import net.babys_care.app.utils.Toaster.Companion.toast

internal object Toaster {

    fun showToast(message: String, length: Int = Toast.LENGTH_SHORT) {
        dismissExistingToast()
        toast = Toast.makeText(AppManager.context, message, length)
        toast?.show()
    }

    fun showCustomToast(message: String, length: Int = Toast.LENGTH_SHORT, view: View? = null, horizontalGravity: Int = Gravity.FILL_HORIZONTAL, verticalGravity: Int = Gravity.BOTTOM, xOffset: Int = 0, yOffset: Int = 0) {
        dismissExistingToast()
        toast = Toast(AppManager.context)
        toast?.duration = length
        toast?.setGravity(horizontalGravity or verticalGravity, xOffset, yOffset)
        if (view != null) {
            toast?.view = view
        } else {
            val defaultView = LayoutInflater.from(AppManager.context).inflate(R.layout.custom_toast_default_layout, null)
            defaultView.findViewById<TextView>(R.id.toast_message).text = message
            toast?.view = defaultView
        }

        toast?.show()
    }

    private fun dismissExistingToast() {
        if (toast != null) {
            toast?.cancel()
        }
    }

    object Companion{
        var toast: Toast? = null
    }
}