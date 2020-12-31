package net.babys_care.app.scene.settings

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.realm.Realm
import io.realm.kotlin.where
import jp.winas.android.foundation.scene.BaseFragment
import jp.winas.android.foundation.scene.ViewModelable
import kotlinx.android.synthetic.main.fragment_notification_settings.*
import net.babys_care.app.AppManager
import net.babys_care.app.R
import net.babys_care.app.api.responses.CreateUserResponseData
import net.babys_care.app.api.responses.NotificationEditResponse
import net.babys_care.app.models.realmmodels.ChildrenModel
import net.babys_care.app.models.realmmodels.UserModel
import net.babys_care.app.utils.LoadingHelper
import net.babys_care.app.utils.debugLogInfo

class NotificationSettingsFragment : BaseFragment(), ViewModelable<NotificationSettingsViewModel> {

    override val layout = R.layout.fragment_notification_settings
    override val viewModelClass = NotificationSettingsViewModel::class

    lateinit var realm: Realm
    var user: UserModel? = null
    var loadingDialog: AlertDialog? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        realm = Realm.getDefaultInstance()
        user = realm.where<UserModel>().findFirst()
        updateUI(true)

        local_push_notification_switch.setOnCheckedChangeListener { _, isChecked ->
            val local = if (isChecked) 1 else 0
            val remote = if (recommended_article_push_notification_switch.isChecked) 1 else 0
            updateNotificationSettings(local, remote)
        }

        recommended_article_push_notification_switch.setOnCheckedChangeListener { _, isChecked ->
            val local = if (local_push_notification_switch.isChecked) 1 else 0
            val remote = if (isChecked) 1 else 0
            updateNotificationSettings(local, remote)
        }
    }

    private fun updateNotificationSettings(notifiableLocal: Int, notifiableRemote: Int) {
        showLoadingDialog(true)
        viewModel?.updateNotificationSettings(AppManager.apiToken, notifiableLocal, notifiableRemote) {response, error ->
            showLoadingDialog(false)
            if (response != null && response.result) {
                showCompletedDialog(response)
                updateLocalDatabase(response.data)
            } else {
                updateUI()
                debugLogInfo("Error: $error")
            }
        }
    }

    private fun updateUI(isInitial: Boolean = false) {
        if (isInitial) {
            user?.let {
                local_push_notification_switch.isChecked = it.isNotifiableLocal == 1
                recommended_article_push_notification_switch.isChecked = it.isNotifiableRemote == 1
            } ?: kotlin.run {
                local_push_notification_switch.isChecked = false
                recommended_article_push_notification_switch.isChecked = false
            }
        } else {
            if (user?.isNotifiableLocal == 1 && !local_push_notification_switch.isChecked) {
                local_push_notification_switch.isChecked = true
            } else if (user?.isNotifiableLocal == 0 && local_push_notification_switch.isChecked) {
                local_push_notification_switch.isChecked = false
            } else if (user?.isNotifiableRemote == 1 && !recommended_article_push_notification_switch.isChecked) {
                recommended_article_push_notification_switch.isChecked = true
            } else if (user?.isNotifiableRemote == 0 && recommended_article_push_notification_switch.isChecked) {
                recommended_article_push_notification_switch.isChecked = false
            }
        }
    }

    private fun showCompletedDialog(response: NotificationEditResponse) {
        MaterialAlertDialogBuilder(requireContext())
            .setMessage(response.data.message)
            .setCancelable(false)
            .setPositiveButton(getString(R.string.ok)) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun showLoadingDialog(isShow: Boolean) {
        if (loadingDialog != null) {
            loadingDialog?.dismiss()
            loadingDialog = null
        }

        if (isShow) {
            loadingDialog = LoadingHelper().getLoadingDialog(requireContext())
            loadingDialog?.show()
        }
    }

    private fun updateLocalDatabase(data: CreateUserResponseData) {
        val childrenList = mutableListOf<ChildrenModel>()
        for (child in data.children) {
            val children = ChildrenModel(child.childId, child.last_name, child.first_name,
                child.last_name_kana, child.first_name_kana, child.image, child.gender,
            child.birth_day, child.birth_order, child.sibling_order)

            childrenList.add(children)
        }

        if (childrenList.isNotEmpty()) {
            realm.executeTransaction {
                user?.apiToken = data.apiToken
                user?.isNotifiableLocal = data.isNotifiableLocal
                user?.isNotifiableRemote = data.isNotifiableRemote
                realm.insertOrUpdate(childrenList)
            }
        } else {
            realm.executeTransaction {
                user?.apiToken = data.apiToken
                user?.isNotifiableLocal = data.isNotifiableLocal
                user?.isNotifiableRemote = data.isNotifiableRemote
            }
        }
    }

    override fun onStop() {
        (activity as? AppSettingsActivity)?.showNotificationSettingsView(false)
        super.onStop()
    }

    override fun onDestroyView() {
        realm.close()
        if (loadingDialog != null) {
            loadingDialog?.dismiss()
            loadingDialog = null
        }
        super.onDestroyView()
    }
}