package net.babys_care.app.scene.registration

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.messaging.FirebaseMessaging
import io.realm.Realm
import jp.winas.android.foundation.extension.onChanged
import jp.winas.android.foundation.scene.BaseFragment
import jp.winas.android.foundation.scene.ViewModelable
import kotlinx.android.synthetic.main.fragment_baby_registration.*
import net.babys_care.app.AppManager
import net.babys_care.app.AppSetting
import net.babys_care.app.BuildConfig
import net.babys_care.app.R
import net.babys_care.app.constants.AppConstants
import net.babys_care.app.extensions.hideKeyBoard
import net.babys_care.app.extensions.makeLinks
import net.babys_care.app.models.Children
import net.babys_care.app.models.Parent
import net.babys_care.app.models.User
import net.babys_care.app.utils.JapaneseCharacterUtils
import net.babys_care.app.utils.LoadingHelper
import java.util.*

class BabyRegistrationFragment : BaseFragment(), ViewModelable<BabyRegistrationViewModel> {

    override val layout = R.layout.fragment_baby_registration
    override val viewModelClass = BabyRegistrationViewModel::class

    var parent: Parent? = null
    var user: User? = null
    private lateinit var children: Children
    private lateinit var realm: Realm
    private var loadingDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel?.userLiveData?.onChanged(this) {response ->
            showLoadingDialog(false)
            if (response != null) {
                if (response.result == true) {
                    showCompletionDialog()
                } else {
                    val message = response.message ?: response.data?.message
                    message?.let {
                        showErrorDialog(it)
                    }
                }
            }
        }
    }

    override fun onResume() {
        (activity as? UserRegistrationActivity)?.changeToolbarTitle(getString(R.string.baby_info_entry_title))
        super.onResume()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        realm = Realm.getDefaultInstance()

        children = Children()

        privacy_policy_text_view.makeLinks(Pair(getString(R.string.terms), View.OnClickListener {
            val intent = Intent(requireActivity(), PrivacyPolicyActivity::class.java)
            intent.putExtra(AppConstants.URL, BuildConfig.URL_AGREEMENT)
            intent.putExtra("title", getString(R.string.terms))
            startActivity(intent)
        }), Pair(getString(R.string.privacy_policy), View.OnClickListener {
            val intent = Intent(requireActivity(), PrivacyPolicyActivity::class.java)
            intent.putExtra(AppConstants.URL, BuildConfig.URL_PRIVACY_POLICY)
            intent.putExtra("title", getString(R.string.privacy_policy))
            startActivity(intent)
        }))

        pre_mama_check_box.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                baby_info_input_container.visibility = View.GONE
                parent?.is_premama = 1
            } else {
                baby_info_input_container.visibility = View.VISIBLE
                parent?.is_premama = 0
            }
            enableSignUpButton()
        }

        privacy_policy_check_box.setOnCheckedChangeListener { _, _ ->
            enableSignUpButton()
        }

        button_sign_up.setOnClickListener {
            validateInputAndProceed()
        }

        birthday_text_view.setOnClickListener {
            showDatePicker()
        }

        setGenderSpinner()
        setChildBirthOrderSpinner()
        addTextWatchers()
    }

    private fun addTextWatchers() {
        last_name_input_field.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun afterTextChanged(s: Editable?) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                children.last_name = last_name_input_field.text.toString().trim()
                enableSignUpButton()
            }
        })

        first_name_input_field.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun afterTextChanged(s: Editable?) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                children.first_name = first_name_input_field.text.toString().trim()
                enableSignUpButton()
            }
        })

        last_name_kana_input_field.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun afterTextChanged(s: Editable?) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                children.last_name_kana = last_name_kana_input_field.text.toString().trim()
                enableSignUpButton()
            }
        })

        first_name_kana_input_field.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun afterTextChanged(s: Editable?) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                children.first_name_kana = first_name_kana_input_field.text.toString().trim()
                enableSignUpButton()
            }
        })
    }

    private fun validateInputAndProceed() {
        if (!pre_mama_check_box.isChecked) {
            if (!isFirstNameKanjiValid(children.first_name)) {
                return
            }

            if (!isLastNameKanjiValid(children.last_name)) {
                return
            }

            if (!isFirstNameKanaValid(children.first_name_kana)) {
                return
            }

            if (!isLastNameKanaValid(children.last_name_kana)) {
                return
            }

            if (!isGenderValid(children.gender)) {
                return
            }

            if (!isBirthdayValid(children.birth_day)) {
                return
            }

            if (!isBirthOrderValid(children.birth_order)) {
                return
            }

            if (!isSiblingOrderValid(children.sibling_order)) {
                return
            }

            if (!privacy_policy_check_box.isChecked) {
                return
            }

        } else if (!privacy_policy_check_box.isChecked){
            return
        } else {
            children = Children()
        }

        first_name_kana_input_field.hideKeyBoard()

        signUpUser()
    }

    private fun enableSignUpButton() {
        if (pre_mama_check_box.isChecked) {
            button_sign_up.isEnabled = privacy_policy_check_box.isChecked
        } else {
            button_sign_up.isEnabled = !(TextUtils.isEmpty(children.last_name)
                    || TextUtils.isEmpty(children.first_name)
                    || TextUtils.isEmpty(children.last_name_kana)
                    || TextUtils.isEmpty(children.first_name_kana)
                    || TextUtils.isEmpty(children.gender)
                    || TextUtils.isEmpty(children.birth_day)
                    || !privacy_policy_check_box.isChecked)
        }
    }

    private fun setChildBirthOrderSpinner() {
        val japaneseCounting = JapaneseCharacterUtils().getJapaneseNumbers(1, 10)
        val birthOrderArray = mutableListOf<String>()
        birthOrderArray.add(0, getString(R.string.select_birth_order))
        japaneseCounting.forEach { item ->
            birthOrderArray.add(String.format(getString(R.string.child_birth_order), item))
        }

        val adapter = ArrayAdapter(requireContext(), R.layout.custom_spinner, birthOrderArray)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        birth_order_spinner.adapter = adapter
        birth_order_spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {}

            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                if (position > 0) {
                    children.birth_order = position
                } else {
                    children.birth_order = null
                    (parent?.getChildAt(position) as? TextView)?.setTextColor(ContextCompat.getColor(parent.context, R.color.pinkishGrey))
                }
                enableSignUpButton()
            }
        }
    }

    private fun isBirthOrderValid(birthOrder: Int?): Boolean {
        if (birthOrder == null || birthOrder < 1) {
            birth_order_error.visibility = View.VISIBLE
            birth_order_error.text = getString(R.string.enter_correct_value)
            return false
        }

        birth_order_error.visibility = View.GONE
        return true
    }

    private fun setGenderSpinner() {
        val genderArray = arrayOf(getString(R.string.select_gender), getString(R.string.baby_boy), getString(R.string.baby_girl), getString(R.string.other))
        val adapter = ArrayAdapter(requireContext(), R.layout.custom_spinner, genderArray)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        gender_spinner.adapter = adapter
        gender_spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {}

            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                if (position > 0) {
                    children.gender = when(gender_spinner.selectedItem.toString()) {
                        "男の子" -> "male"
                        "女の子" -> "female"
                        "その他" -> "other"
                        else -> ""
                    }
                } else {
                    children.gender = null
                    (parent?.getChildAt(position) as? TextView)?.setTextColor(ContextCompat.getColor(parent.context, R.color.pinkishGrey))
                }
                setSiblingOrderSpinner(children.gender)
                enableSignUpButton()
            }
        }
    }

    private fun isGenderValid(gender: String?): Boolean {
        gender_error.visibility = View.GONE
        return when(gender) {
            "male" -> true
            "female" -> true
            "other" -> true
            else -> {
                gender_error.visibility = View.VISIBLE
                gender_error.text = getString(R.string.enter_correct_value)
                false
            }
        }
    }

    private fun setSiblingOrderSpinner(gender: String?) {
        val siblingOrderData = arrayListOf<String>()
        val japaneseNumbers = JapaneseCharacterUtils().getJapaneseNumbers(3, 5)
        siblingOrderData.add(0, getString(R.string.select_relationship))
        when (gender) {
            "male" -> {
                siblingOrderData.add(1, getString(R.string.eldest_son))
                siblingOrderData.add(2, getString(R.string.second_son))
                for (number in japaneseNumbers) {
                    siblingOrderData.add(String.format(getString(R.string.n_son), number))
                }
            }
            "female" -> {
                siblingOrderData.add(1, getString(R.string.eldest_daughter))
                siblingOrderData.add(2, getString(R.string.second_daughter))
                for (number in japaneseNumbers) {
                    siblingOrderData.add(String.format(getString(R.string.n_daughter), number))
                }
            }
            else -> {
                siblingOrderData.add(1, getString(R.string.eldest_son))
                siblingOrderData.add(2, getString(R.string.second_son))
                for (number in japaneseNumbers) {
                    siblingOrderData.add(String.format(getString(R.string.n_son), number))
                }

                siblingOrderData.add(getString(R.string.eldest_daughter))
                siblingOrderData.add(getString(R.string.second_daughter))
                for (number in japaneseNumbers) {
                    siblingOrderData.add(String.format(getString(R.string.n_daughter), number))
                }
            }
        }

        val adapter = ArrayAdapter(requireContext(), R.layout.custom_spinner, siblingOrderData)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        sibling_order_spinner.adapter = adapter

        sibling_order_spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {}
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                if (position == 0) {
                    children.sibling_order = null
                    (parent?.getChildAt(position) as? TextView)?.setTextColor(ContextCompat.getColor(parent.context, R.color.pinkishGrey))
                } else {
                    children.sibling_order = position
                }
                enableSignUpButton()
            }
        }
    }

    private fun isSiblingOrderValid(siblingOrder: Int?): Boolean {
        if (siblingOrder == null || siblingOrder < 1) {
            relationship_error.visibility = View.VISIBLE
            relationship_error.text = getString(R.string.enter_correct_value)
            return false
        }

        relationship_error.visibility = View.GONE
        return true
    }

    private fun isFirstNameKanjiValid(name: String?): Boolean {
        if (name == null || TextUtils.isEmpty(name)) {
            first_name_kanji_error.visibility = View.VISIBLE
            first_name_kanji_error.text = String.format(getString(R.string.enter_at_least_n_character), 1)
            return false
        } else if (name.length > 100) {
            first_name_kanji_error.visibility = View.VISIBLE
            first_name_kanji_error.text = String.format(getString(R.string.enter_at_most_n_character), 100)
            return false
        }

        first_name_kanji_error.visibility = View.GONE
        return true
    }

    private fun isLastNameKanjiValid(name: String?): Boolean {
        if (name == null || TextUtils.isEmpty(name)) {
            last_name_kanji_error.visibility = View.VISIBLE
            last_name_kanji_error.text = String.format(getString(R.string.enter_at_least_n_character), 1)
            return false
        } else if (name.length > 100) {
            last_name_kanji_error.visibility = View.VISIBLE
            last_name_kanji_error.text = String.format(getString(R.string.enter_at_most_n_character), 100)
            return false
        }

        last_name_kanji_error.visibility = View.GONE
        return true
    }

    private fun isFirstNameKanaValid(name: String?): Boolean {
        if (name == null || TextUtils.isEmpty(name)) {
            first_name_kana_error.visibility = View.VISIBLE
            first_name_kana_error.text = String.format(getString(R.string.enter_at_least_n_character), 1)
            return false
        } else if (name.length > 100) {
            first_name_kana_error.visibility = View.VISIBLE
            first_name_kana_error.text = String.format(getString(R.string.enter_at_most_n_character), 100)
            return false
        }

        first_name_kana_error.visibility = View.GONE
        return true
    }

    private fun isLastNameKanaValid(name: String?): Boolean {
        if (name == null || TextUtils.isEmpty(name)) {
            last_name_kana_error.visibility = View.VISIBLE
            last_name_kana_error.text = String.format(getString(R.string.enter_at_least_n_character), 1)
            return false
        } else if (name.length > 100) {
            last_name_kana_error.visibility = View.VISIBLE
            last_name_kana_error.text = String.format(getString(R.string.enter_at_most_n_character), 100)
            return false
        }

        last_name_kana_error.visibility = View.GONE
        return true
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val defaultYear = calendar.get(Calendar.YEAR)
        val defaultMonth = calendar.get(Calendar.MONTH)
        val defaultDay = calendar.get(Calendar.DAY_OF_MONTH)

        Locale.setDefault(Locale.JAPAN)
        val datePickerDialog = DatePickerDialog(requireContext(),
            { _: DatePicker, year: Int, month: Int, dayOfMonth: Int ->
                val currentYear = calendar.get(Calendar.YEAR)
                val birthYear = if (year < 2010) 2010 else if (year > currentYear) currentYear else year
                birthday_text_view.text = String.format(getString(R.string.year_month_day_format), birthYear, month + 1, dayOfMonth)
                children.birth_day = "$birthYear/${month + 1}/$dayOfMonth"
                enableSignUpButton()
            }, defaultYear, defaultMonth, defaultDay
        )
        val minDate = Calendar.getInstance()
        minDate.set(2010, 0, 1)
        datePickerDialog.datePicker.minDate = minDate.timeInMillis
        datePickerDialog.datePicker.maxDate = calendar.timeInMillis
        datePickerDialog.show()
    }

    private fun isBirthdayValid(birthDay: String?): Boolean {
        if (birthDay.isNullOrEmpty()) {
            birthday_error.visibility = View.VISIBLE
            birthday_error.text = getString(R.string.enter_correct_value)
            return false
        }

        birthday_error.visibility = View.GONE
        return true
    }

    private fun signUpUser() {
        val parent = this.parent ?: return
        val user = this.user ?: return
        AppSetting(AppManager.context).fcmToken?.let {fcmToken ->
            showLoadingDialog(true)
            parent.fcm_token = fcmToken
            viewModel?.createUser(parent, if (parent.is_premama == 1) null else children, user) {
                showLoadingDialog(false)
                showErrorDialog(it)
            }
        } ?: kotlin.run {
            FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                showLoadingDialog(true)
                parent.fcm_token = token
                AppSetting(AppManager.context).fcmToken = token
                viewModel?.createUser(parent, if (parent.is_premama == 1) null else children, user) {
                    showLoadingDialog(false)
                    showErrorDialog(it)
                }
            }
        }
    }

    private fun showCompletionDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setMessage(getString(R.string.registration_complete_and_mail_sent))
            .setCancelable(false)
            .setPositiveButton(getString(R.string.ok)) { dialog, _ ->
                dialog.dismiss()
                (activity as? UserRegistrationActivity)?.transitToLoginActivity()
            }
            .show()
    }

    private fun showErrorDialog(message: String) {
        val dialog = AlertDialog.Builder(requireContext())
        val layout = LayoutInflater.from(requireContext()).inflate(R.layout.layout_ok_dialog_with_title, null, false)
        dialog.setView(layout)

        val alert = dialog.create()
        layout.findViewById<TextView>(R.id.message).text = message
        layout.findViewById<Button>(R.id.ok_button).setOnClickListener {
            alert.dismiss()
        }

        alert.show()
        alert.window?.setBackgroundDrawableResource(android.R.color.transparent)
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

    override fun onStop() {
        if (loadingDialog != null) {
            loadingDialog?.dismiss()
            loadingDialog = null
        }

        super.onStop()
    }


    override fun onDestroyView() {
        realm.close()
        super.onDestroyView()
    }
}