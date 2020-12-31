package net.babys_care.app.utils

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import net.babys_care.app.R

class LoadingHelper {

    fun getLoadingDialog(context: Context): AlertDialog {
        val dialog = AlertDialog.Builder(context)
        val dialogView = LayoutInflater.from(context).inflate(R.layout.layout_loading_dialog, null, false)
        dialog.setView(dialogView)
        dialog.setCancelable(false)

        return dialog.create()
    }
}