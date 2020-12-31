package net.babys_care.app.utils

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.view.LayoutInflater
import android.widget.Button
import android.widget.TextView
import net.babys_care.app.R
import net.babys_care.app.scene.login.LoginActivity

class DialogHelper {

    fun showDialog(
        activity: Activity,
        message: String,
        transitToLogin: Boolean = false,
        cancelable: Boolean = false,
        okButtonText: String = "OK",
        cancelText: String? = null
    ) {
        val alertDialog = AlertDialog.Builder(activity)
        alertDialog.setCancelable(cancelable)
            .setMessage(message)
            .setPositiveButton(okButtonText) { dialog, _ ->
                dialog.dismiss()
                if (transitToLogin) {
                    val intent = Intent(activity, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                    activity.startActivity(intent)
                    activity.finishAffinity()
                }
            }
        if (cancelText != null) {
            alertDialog.setNegativeButton(cancelText, null)
        }

        alertDialog.show()
    }

    fun showLoginTransitionDialog(
        activity: Activity,
        message: String,
        isBackFromLogin: Boolean = false
    ) {
        val dialog = AlertDialog.Builder(activity)
        val layout = LayoutInflater.from(activity).inflate(R.layout.layout_ok_dialog_without_title, null, false)
        dialog.setView(layout)
        dialog.setCancelable(false)

        val alert = dialog.create()
        layout.findViewById<TextView>(R.id.message).text = message
        layout.findViewById<Button>(R.id.ok_button).setOnClickListener {
            alert.dismiss()
            val intent = Intent(activity, LoginActivity::class.java)
            if (isBackFromLogin) {
                activity.startActivity(intent)
            } else {
                intent.putExtra("isFromInitial", true)
                activity.startActivity(intent)
                activity.finishAffinity()
            }
        }

        alert.show()
        alert.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    fun showMaintenanceDialog(activity: Activity) {
        val dialog = AlertDialog.Builder(activity)
        val layout = LayoutInflater.from(activity).inflate(R.layout.layout_ok_dialog_without_title, null, false)
        dialog.setView(layout)
        dialog.setCancelable(false)

        val alert = dialog.create()
        layout.findViewById<TextView>(R.id.message).text = activity.getString(R.string.currently_under_maintenance_ea004)
        layout.findViewById<Button>(R.id.ok_button).setOnClickListener {
            alert.dismiss()
            activity.finishAffinity()
        }

        alert.show()
        alert.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }
}