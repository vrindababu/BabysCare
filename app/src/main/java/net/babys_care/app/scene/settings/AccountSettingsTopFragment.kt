package net.babys_care.app.scene.settings

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import jp.winas.android.foundation.scene.BaseFragment
import jp.winas.android.foundation.scene.ViewModelable
import kotlinx.android.synthetic.main.fragment_account_settings_top.*
import net.babys_care.app.AppManager
import net.babys_care.app.R
import net.babys_care.app.scene.login.LoginActivity
import net.babys_care.app.utils.LoadingHelper

class AccountSettingsTopFragment : BaseFragment(), ViewModelable<AccountSettingsTopViewModel> {

    override val layout = R.layout.fragment_account_settings_top
    override val viewModelClass = AccountSettingsTopViewModel::class

    private lateinit var settingsDataList: List<String>
    private var loadingDialog: AlertDialog? = null

    override fun onResume() {
        (activity as? AccountSettingsActivity)?.updateToolbarTitle(getString(R.string.account_settings))
        super.onResume()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        settingsDataList = getSettingsList()
        setRecyclerView()
    }

    private fun setRecyclerView() {
        settings_top_recycler_view.layoutManager = LinearLayoutManager(requireContext())
        settings_top_recycler_view.adapter = AccountSettingsAdapter().apply {
            onItemClick = {position ->
                when(position) {
                    0 -> {
                        val userSettings = UserSettingsFragment()
                        (activity as? AccountSettingsActivity)?.replaceFragment(userSettings)
                    }
                    1 -> {
                        val userSettings = BabyInfoCheckFragment()
                        (activity as? AccountSettingsActivity)?.replaceFragment(userSettings)
                    }
                    2 -> showDeleteConfirmationDialog()
                }
            }
        }
        settings_top_recycler_view.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
    }

    private fun getSettingsList(): List<String> {
        return listOf(
            getString(R.string.setting_user_info), getString(R.string.setting_and_check_baby_info),
            getString(R.string.unsubscribe)
        )
    }

    private fun showDeleteConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setMessage(getString(R.string.really_want_to_unsubscribe))
            .setPositiveButton(getString(R.string.withdraw)) { dialog, _ ->
                dialog.dismiss()
                deleteUser()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun deleteUser() {
        if (loadingDialog != null) {
            loadingDialog?.dismiss()
            loadingDialog = null
        }
        loadingDialog = LoadingHelper().getLoadingDialog(requireContext())
        loadingDialog?.show()
        viewModel?.unsubscribeUser(AppManager.apiToken) { response, error ->
            loadingDialog?.dismiss()
            if (response != null) {
                showError(response.message ?: "エラー", true)
            } else {
                showError(error?.message ?: "エラー")
            }
        }
    }

    private fun showError(message: String, transitToLogin: Boolean = false) {
        AlertDialog.Builder(requireContext())
            .setCancelable(false)
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
                if (transitToLogin) {
                    val intent = Intent(requireContext(), LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                    activity?.startActivity(intent)
                    activity?.finishAffinity()
                }
            }
            .show()
    }

    inner class AccountSettingsAdapter : RecyclerView.Adapter<AccountSettingsViewHolder>() {

        var onItemClick: ((Int) -> Unit)? = null

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): AccountSettingsViewHolder {
            return AccountSettingsViewHolder(LayoutInflater.from(parent.context)
                .inflate(R.layout.item_account_settings_recycler, parent, false))
        }

        override fun onBindViewHolder(holder: AccountSettingsViewHolder, position: Int) {
            val item = settingsDataList[position]
            holder.settingsItemName.text = item
            holder.settingsItemName.setOnClickListener {
                onItemClick?.invoke(position)
            }
        }

        override fun getItemCount(): Int {
            return settingsDataList.size
        }

    }

    inner class AccountSettingsViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val settingsItemName: TextView = itemView.findViewById(R.id.settings_item_name)
    }
}