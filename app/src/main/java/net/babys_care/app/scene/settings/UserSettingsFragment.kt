package net.babys_care.app.scene.settings

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import io.realm.Realm
import jp.winas.android.foundation.scene.BaseFragment
import kotlinx.android.synthetic.main.fragment_user_settings.*
import net.babys_care.app.BuildConfig
import net.babys_care.app.R
import net.babys_care.app.constants.Prefecture
import net.babys_care.app.extensions.toDateJapaneseWithYear
import net.babys_care.app.models.realmmodels.UserModel
import net.babys_care.app.scene.MainActivity
import net.babys_care.app.utils.Encrypt

class UserSettingsFragment : BaseFragment() {

    override val layout = R.layout.fragment_user_settings

    private lateinit var realm: Realm
    private var user: UserModel? = null
    var onFragmentAdd: ((Fragment) -> Unit)? = null

    override fun onResume() {
        (activity as? AccountSettingsActivity)?.updateToolbarTitle(getString(R.string.confirmation_of_user_info))
        (activity as? MainActivity)?.addBackButtonAndActionToMain(true)
        super.onResume()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        realm = Realm.getDefaultInstance()

        button_edit.setOnClickListener {
            if (user != null) {
                val userEdit = UserInfoEditFragment()
                userEdit.user = user
                if (activity is MainActivity) {
                    onFragmentAdd?.invoke(userEdit)
                } else {
                    (activity as? AccountSettingsActivity)?.replaceFragment(userEdit)
                }
            }
        }

        showUserData()
    }

    @SuppressLint("SetTextI18n")
    private fun showUserData() {
        user = realm.where(UserModel::class.java).findFirst()
        user?.let { user ->
            first_name.text = user.firstName
            full_name.text = "${user.lastName} ${user.firstName}"
            full_name_kana.text = "${user.lastNameKana} ${user.firstNameKana}"
            gender.text = when (user.gender) {
                "male" -> getString(R.string.male)
                "female" -> getString(R.string.female)
                else -> getString(R.string.other)
            }
            birthday.text = user.birthday.toDateJapaneseWithYear()
            postal_code.text = if (user.postalCode.length == 7) {
                "${user.postalCode.subSequence(0, 3)}-${user.postalCode.substring(3)}"
            } else {
                user.postalCode
            }
            email.text = user.email
            password.setText(Encrypt().getDecryptedPassword(user.password, getString(R.string.encryption_pass)))
            val defaultImage = when(user.gender) {
                "male" -> ContextCompat.getDrawable(requireContext(), R.drawable.default_profile_male)
                else -> ContextCompat.getDrawable(requireContext(), R.drawable.default_profile_female)
            }
            user.image?.let {
                Glide.with(requireContext()).load("${BuildConfig.URL_IMAGE_DIRECTORY}$it").apply(RequestOptions().placeholder(defaultImage)).into(profile_image)
            } ?: kotlin.run {
                profile_image.setImageDrawable(defaultImage)
            }
            address.text = "${Prefecture.values()[user.prefecture].value}${user.city}${user.building ?: ""}"
        } ?: kotlin.run {
            (activity as? AccountSettingsActivity)?.onBackPressed()
            (activity as? MainActivity)?.onBackPressed()
        }
    }

    override fun onDestroyView() {
        realm.close()

        super.onDestroyView()
    }
}