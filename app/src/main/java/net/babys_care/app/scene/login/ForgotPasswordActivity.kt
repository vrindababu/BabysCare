package net.babys_care.app.scene.login

import android.app.AlertDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.LayoutInflater
import android.widget.Button
import android.widget.TextView
import jp.winas.android.foundation.scene.BaseActivity
import jp.winas.android.foundation.scene.ViewModelable
import kotlinx.android.synthetic.main.activity_forgot_password.*
import net.babys_care.app.R
import net.babys_care.app.utils.LoadingHelper
import net.babys_care.app.utils.Toaster.showToast

class ForgotPasswordActivity : BaseActivity(), ViewModelable<ForgotPasswordViewModel> {

    override val layout = R.layout.activity_forgot_password
    override val viewModelClass = ForgotPasswordViewModel::class

    private var loadingDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        send_mail_button.setOnClickListener {
            val email = viewModel?.emailAddress ?: ""
            if (isValidEmail(email)) {
                requestForPasswordResetLink(email)
            }
        }

        email_input_field.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel?.emailAddress = email_input_field.text.toString().trim()
                send_mail_button.isEnabled = !TextUtils.isEmpty(viewModel?.emailAddress)
            }
        })

        toolbar_back_button.setOnClickListener {
            onBackPressed()
        }
    }

    private fun requestForPasswordResetLink(email: String) {
        showLoadingDialog(true)
        viewModel?.requestPasswordReset(email) {response, errorMessage ->
            showLoadingDialog(false)
            if (errorMessage != null) {
                showToast(errorMessage)
            } else {
                response?.data?.message?.let {
                    showCompletionDialog(getString(R.string.sent_mail), it)
                }
            }
        }
    }

    private fun isValidEmail(email: String): Boolean {
        return !TextUtils.isEmpty(email) && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun showLoadingDialog(isShow: Boolean) {
        if (loadingDialog != null) {
            loadingDialog?.dismiss()
            loadingDialog = null
        }

        if (isShow) {
            loadingDialog = LoadingHelper().getLoadingDialog(this)
            loadingDialog?.show()
        }
    }

    private fun showCompletionDialog(title: String, message: String) {
        val dialog = AlertDialog.Builder(this)
        val layout = LayoutInflater.from(this).inflate(R.layout.layout_ok_dialog_with_title, null, false)
        dialog.setView(layout)
        dialog.setCancelable(false)

        val alert = dialog.create()
        layout.findViewById<TextView>(R.id.title).text = title
        layout.findViewById<TextView>(R.id.message).text = message
        layout.findViewById<Button>(R.id.ok_button).setOnClickListener {
            alert.dismiss()
            finish()
        }

        alert.show()
        alert.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    override fun onStop() {
        if (loadingDialog != null) {
            loadingDialog?.dismiss()
            loadingDialog = null
        }
        super.onStop()
    }
}