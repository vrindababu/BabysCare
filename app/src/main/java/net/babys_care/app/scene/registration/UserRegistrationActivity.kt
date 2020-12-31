package net.babys_care.app.scene.registration

import android.content.Intent
import android.os.Bundle
import jp.winas.android.foundation.scene.BaseActivity
import kotlinx.android.synthetic.main.activity_user_registration.*
import net.babys_care.app.R
import net.babys_care.app.models.Parent
import net.babys_care.app.models.User
import net.babys_care.app.scene.login.LoginActivity

class UserRegistrationActivity : BaseActivity() {

    override val layout= R.layout.activity_user_registration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        toolbar_back_button.setOnClickListener {
            onBackPressed()
        }

        showParentRegistrationFragment()
    }

    private fun showParentRegistrationFragment() {
        navigate(ParentRegistrationFragment(), R.id.registration_fragment_container, false)
        toolbar_title_text.text = getString(R.string.user_info_entry_title)
    }

    /**
     * Method to replace existing fragment in activity with #{BabyRegistrationFragment}
     * @param parent Optional parentUser object to pre-initialize baby's parent data
     */
    fun showBabyRegistrationFragment(parent: Parent?, user: User) {
        val babyRegistrationFragment = BabyRegistrationFragment()
        babyRegistrationFragment.parent = parent
        babyRegistrationFragment.user = user
        navigate(babyRegistrationFragment, R.id.registration_fragment_container)
    }

    /**
     * Change the title of toolbar according to screen
     * @param title that will be displayed
     */
    fun changeToolbarTitle(title: String) {
        toolbar_title_text.text = title
    }

    /**
     * Functions to return to MainActivity by cleaning view and fragment back stack
     */
    fun transitToLoginActivity() {
        navigateAsNewTask(Intent(this, LoginActivity::class.java).putExtra("isFromInitial", true))
    }
}