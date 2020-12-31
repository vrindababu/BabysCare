package net.babys_care.app.scene.registration

import android.app.DatePickerDialog
import android.content.res.ColorStateList
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.DatePicker
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import jp.winas.android.foundation.scene.BaseFragment
import jp.winas.android.foundation.scene.ViewModelable
import kotlinx.android.synthetic.main.fragment_parent_registration.*
import net.babys_care.app.R
import net.babys_care.app.constants.Prefecture
import net.babys_care.app.extensions.enableErrorColor
import net.babys_care.app.extensions.hideKeyBoard
import net.babys_care.app.models.Parent
import net.babys_care.app.models.User
import java.util.*

class ParentRegistrationFragment : BaseFragment(), ViewModelable<ParentRegistrationViewModel> {

    override val layout = R.layout.fragment_parent_registration
    override val viewModelClass = ParentRegistrationViewModel::class
    private var isPasswordVisible: Boolean = false
    private var isConfirmPasswordVisible: Boolean = false

    override fun onResume() {
        (activity as? UserRegistrationActivity)?.changeToolbarTitle(getString(R.string.user_info_entry_title))
        if (!viewModel?.parentUser?.birth_day.isNullOrEmpty()) {
            viewModel?.parentUser?.birth_day?.split("/")?.let { birthDate ->
                try {
                    birthday_text_view.text = String.format(getString(R.string.year_month_day_format), birthDate[0].toInt(), birthDate[1].toInt(), birthDate[2].toInt())
                } catch (ex: Exception) {
                    birthday_text_view.text = null
                }
            }
        }
        super.onResume()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        button_next_page.setOnClickListener {
            validateInputAndProceed()
        }

        birthday_text_view.setOnClickListener {
            showDatePicker()
        }

        setGenderSpinner()
        setPrefectureSpinner()

        addTextWatchers()

        password_toggle.setOnClickListener {
            togglePasswordVisibility()
        }

        password_toggle_confirm.setOnClickListener {
            toggleConfirmPasswordVisibility()
        }
    }

    private fun addTextWatchers() {
        last_name_input_field.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun afterTextChanged(s: Editable?) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel?.parentUser?.last_name = last_name_input_field.text.toString().trim()
                enableNextButton()
            }
        })

        first_name_input_field.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun afterTextChanged(s: Editable?) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel?.parentUser?.first_name = first_name_input_field.text.toString().trim()
                enableNextButton()
            }
        })

        last_name_kana_input_field.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun afterTextChanged(s: Editable?) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel?.parentUser?.last_name_kana = last_name_kana_input_field.text.toString().trim()
                enableNextButton()
            }
        })

        first_name_kana_input_field.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun afterTextChanged(s: Editable?) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel?.parentUser?.first_name_kana = first_name_kana_input_field.text.toString().trim()
                enableNextButton()
            }
        })

        zip_code_input_field.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun afterTextChanged(s: Editable?) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel?.parentUser?.postal_code = zip_code_input_field.text.toString().trim()
                enableNextButton()
            }
        })

        city_street_input_field.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel?.parentUser?.city = city_street_input_field.text.toString()
                enableNextButton()
            }
        })

        email_address_input_field.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel?.email = email_address_input_field.text.toString()
                enableNextButton()
            }
        })

        password_input_field.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel?.password = password_input_field.text.toString()
                enableNextButton()
            }
        })

        confirm_password_input_field.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                enableNextButton()
            }
        })
    }

    private fun enableNextButton() {
        val user = viewModel?.parentUser ?: return
        val email = viewModel?.email ?: return
        val password = viewModel?.password ?: return
        val confirmPassword = confirm_password_input_field.text.toString().trim()
        button_next_page.isEnabled = !(TextUtils.isEmpty(user.last_name)
                || TextUtils.isEmpty(user.first_name)
                || TextUtils.isEmpty(user.last_name_kana)
                || TextUtils.isEmpty(user.first_name_kana)
                || TextUtils.isEmpty(user.birth_day)
                || TextUtils.isEmpty(user.postal_code)
                || TextUtils.isEmpty(user.city)
                || TextUtils.isEmpty(email)
                || TextUtils.isEmpty(password)
                || TextUtils.isEmpty(confirmPassword))
    }

    private fun validateInputAndProceed() {
        val lastNameKanji = last_name_input_field.text.toString().trim()
        if (!isLastNameKanjiValid(lastNameKanji)) {
            return
        }

        val firstNameKanji = first_name_input_field.text.toString().trim()
        if (!isFirstNameKanjiValid(firstNameKanji)) {
            return
        }

        val lastNameKana = last_name_kana_input_field.text.toString().trim()
        if (!isLastNameKanaValid(lastNameKana)) {
            return
        }

        val firstNameKana = first_name_kana_input_field.text.toString().trim()
        if (!isFirstNameKanaValid(firstNameKana)) {
            return
        }

        val birthDay = viewModel?.parentUser?.birth_day ?: ""
        if (!isBirthdayValid(birthDay)) {
            return
        }

        val zipCode = zip_code_input_field.text.toString().trim()
        if (!isValidZipCode(zipCode)) {
            return
        }

        val cityName = city_street_input_field.text.toString().trim()
        if (!isCityNameValid(cityName)) {
            return
        }

        val gender = viewModel?.parentUser?.gender ?: return
        if (!isGenderValid(gender)) {
            return
        }

        val prefecture = viewModel?.parentUser?.prefecture ?: return
        if (!isPrefectureValid(prefecture)) {
            return
        }

        val buildingName = building_name_input_field.text.toString().trim()

        val email = email_address_input_field.text.toString().trim()
        if (!isValidEmail(email)) {
            return
        }

        val password = password_input_field.text.toString().trim()
        if (!isValidPassword(password)) {
            return
        }

        val confirmPassword = confirm_password_input_field.text.toString().trim()
        if (!isConfirmPasswordValid(confirmPassword, password)) {
            return
        }

        confirm_password_input_field.hideKeyBoard()

        val parent = Parent(firstNameKanji, lastNameKanji, firstNameKana, lastNameKana, gender,
            birthDay, zipCode, prefecture, cityName, buildingName, 0)
        val user = User(email, password)

        transitToBabyInfoAddPage(parent, user)
    }

    private fun transitToBabyInfoAddPage(parent: Parent, user: User) {
        (activity as? UserRegistrationActivity)?.showBabyRegistrationFragment(parent, user)
    }

    private fun isValidEmail(email: String): Boolean {
        if (TextUtils.isEmpty(email)) {
            email_error.visibility = View.VISIBLE
            email_error.text = String.format(getString(R.string.enter_at_least_n_character), 6)
            email_address_input_field.enableErrorColor(true)
            return false
        } else if (email.length > 255) {
            email_error.visibility = View.VISIBLE
            email_error.text = String.format(getString(R.string.enter_at_most_n_character), 255)
            email_address_input_field.enableErrorColor(true)
            return false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            email_error.visibility = View.VISIBLE
            email_error.text = getString(R.string.enter_in_email_address_format)
            email_address_input_field.enableErrorColor(true)
            return false
        }

        email_address_input_field.enableErrorColor(false)
        email_error.visibility = View.GONE
        return true
    }

    private fun isValidPassword(password: String): Boolean {
        if (TextUtils.isEmpty(password) || password.length < 6) {
            password_error.visibility = View.VISIBLE
            password_error.text = String.format(getString(R.string.enter_at_least_n_character), 6)
            password_input_field.enableErrorColor(true)
            return false
        } else if (password.length > 255) {
            password_error.visibility = View.VISIBLE
            password_error.text = String.format(getString(R.string.enter_at_most_n_character), 255)
            password_input_field.enableErrorColor(true)
            return false
        }

        password_input_field.enableErrorColor(false)
        password_error.visibility = View.GONE
        return true
    }

    private fun isConfirmPasswordValid(confirmPassword: String, password: String): Boolean {
        if (confirmPassword != password) {
            confirm_password_error.visibility = View.VISIBLE
            confirm_password_error.text = getString(R.string.enter_same_password_for_confirmation)
            confirm_password_input_field.enableErrorColor(true)
            return false
        }

        confirm_password_input_field.enableErrorColor(false)
        confirm_password_error.visibility = View.GONE
        return true
    }

    private fun isFirstNameKanjiValid(name: String): Boolean {
        if (TextUtils.isEmpty(name)) {
            first_name_kanji_error.visibility = View.VISIBLE
            first_name_kanji_error.text = String.format(getString(R.string.enter_at_least_n_character), 1)
            first_name_input_field.enableErrorColor(true)
            return false
        } else if (name.length > 100) {
            first_name_kanji_error.visibility = View.VISIBLE
            first_name_kanji_error.text = String.format(getString(R.string.enter_at_most_n_character), 100)
            first_name_input_field.enableErrorColor(true)
            return false
        }

        first_name_input_field.enableErrorColor(false)
        first_name_kanji_error.visibility = View.GONE
        return true
    }

    private fun isLastNameKanjiValid(name: String): Boolean {
        if (TextUtils.isEmpty(name)) {
            last_name_kanji_error.visibility = View.VISIBLE
            last_name_kanji_error.text = String.format(getString(R.string.enter_at_least_n_character), 1)
            last_name_input_field.enableErrorColor(true)
            return false
        } else if (name.length > 100) {
            last_name_kanji_error.visibility = View.VISIBLE
            last_name_kanji_error.text = String.format(getString(R.string.enter_at_most_n_character), 100)
            last_name_input_field.enableErrorColor(true)
            return false
        }

        last_name_input_field.enableErrorColor(false)
        last_name_kanji_error.visibility = View.GONE
        return true
    }

    private fun isFirstNameKanaValid(name: String): Boolean {
        if (TextUtils.isEmpty(name)) {
            first_name_kana_error.visibility = View.VISIBLE
            first_name_kana_error.text = String.format(getString(R.string.enter_at_least_n_character), 1)
            first_name_kana_input_field.enableErrorColor(true)
            return false
        } else if (name.length > 100) {
            first_name_kana_error.visibility = View.VISIBLE
            first_name_kana_error.text = String.format(getString(R.string.enter_at_most_n_character), 100)
            first_name_kana_input_field.enableErrorColor(true)
            return false
        }

        first_name_kana_input_field.enableErrorColor(false)
        first_name_kana_error.visibility = View.GONE
        return true
    }

    private fun isLastNameKanaValid(name: String): Boolean {
        if (TextUtils.isEmpty(name)) {
            last_name_kana_error.visibility = View.VISIBLE
            last_name_kana_error.text = String.format(getString(R.string.enter_at_least_n_character), 1)
            last_name_kana_input_field.enableErrorColor(true)
            return false
        } else if (name.length > 100) {
            last_name_kana_error.visibility = View.VISIBLE
            last_name_kana_error.text = String.format(getString(R.string.enter_at_most_n_character), 100)
            last_name_kana_input_field.enableErrorColor(true)
            return false
        }

        last_name_kana_input_field.enableErrorColor(false)
        last_name_kana_error.visibility = View.GONE
        return true
    }

    private fun isBirthdayValid(birthDay: String): Boolean {
        if (birthDay.isEmpty()) {
            birthday_error.visibility = View.VISIBLE
            birthday_error.text = getString(R.string.enter_correct_value)
            return false
        }

        birthday_error.visibility = View.GONE
        return true
    }

    private fun isValidZipCode(zipCode: String): Boolean {
        if (TextUtils.isEmpty(zipCode) || zipCode.length < 7) {
            zip_code_error.visibility =  View.VISIBLE
            zip_code_error.text = String.format(getString(R.string.enter_at_least_n_character), 7)
            zip_code_input_field.enableErrorColor(true)
            return false
        } else if (zipCode.length > 7){
            zip_code_error.visibility =  View.VISIBLE
            zip_code_error.text = String.format(getString(R.string.enter_at_most_n_character), 7)
            zip_code_input_field.enableErrorColor(true)
            return false
        }

        zip_code_input_field.enableErrorColor(false)
        zip_code_error.visibility =  View.GONE
        return true
    }

    private fun isCityNameValid(name: String): Boolean {
        if (TextUtils.isEmpty(name)) {
            city_name_error.visibility = View.VISIBLE
            city_name_error.text = String.format(getString(R.string.enter_at_least_n_character), 1)
            city_street_input_field.enableErrorColor(true)
            return false
        } else if (name.length > 100) {
            city_name_error.visibility = View.VISIBLE
            city_name_error.text = String.format(getString(R.string.enter_at_most_n_character), 100)
            city_street_input_field.enableErrorColor(true)
            return false
        }

        city_street_input_field.enableErrorColor(false)
        city_name_error.visibility = View.GONE
        return true
    }

    private fun isGenderValid(gender: String): Boolean {
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

    private fun setGenderSpinner() {
        val genderArray = arrayOf(getString(R.string.select_gender), getString(R.string.male), getString(R.string.female), getString(R.string.other))
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
                viewModel?.parentUser?.gender = when(gender_spinner.selectedItem.toString()) {
                    "男性" -> "male"
                    "女性" -> "female"
                    "その他" -> "other"
                    else -> ""
                }
                if (position == 0) {
                    (parent?.getChildAt(position) as? TextView)?.setTextColor(ContextCompat.getColor(parent.context, R.color.pinkishGrey))
                }
            }
        }
    }

    private fun setPrefectureSpinner() {
        val prefectureArray = mutableListOf<String>()
        Prefecture.values().forEach {
            prefectureArray.add(it.value)
        }
        val adapter = ArrayAdapter(requireContext(), R.layout.custom_spinner, prefectureArray)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        prefecture_spinner.adapter = adapter
        prefecture_spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {}

            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                viewModel?.parentUser?.prefecture = position
                if (position == 0) {
                    (parent?.getChildAt(position) as? TextView)?.setTextColor(ContextCompat.getColor(parent.context, R.color.pinkishGrey))
                }
            }
        }
    }

    private fun isPrefectureValid(prefecture: Int): Boolean {
        if (prefecture < 1) {
            prefecture_error.visibility = View.VISIBLE
            prefecture_error.text = getString(R.string.enter_correct_value)
            return false
        }

        prefecture_error.visibility = View.GONE
        return true
    }

    private fun showDatePicker() {
        val c = Calendar.getInstance()
        val defaultYear = 1990
        val defaultMonth = c.get(Calendar.MONTH)
        val defaultDay = c.get(Calendar.DAY_OF_MONTH)

        Locale.setDefault(Locale.JAPAN)
        val datePickerDialog = DatePickerDialog(requireContext(),
            { _: DatePicker, year: Int, month: Int, dayOfMonth: Int ->
                val currentYear = c.get(Calendar.YEAR)
                val birthYear = if (year < 1960) 1960 else if (year > currentYear) currentYear else year
                birthday_text_view.text = String.format(getString(R.string.year_month_day_format), birthYear, month + 1, dayOfMonth)
                viewModel?.parentUser?.birth_day = "$birthYear/${month + 1}/$dayOfMonth"
                enableNextButton()
            }, defaultYear, defaultMonth, defaultDay
        )
        val minDate =Calendar.getInstance()
        minDate.set(1960, 0, 1)
        datePickerDialog.datePicker.minDate = minDate.timeInMillis
        datePickerDialog.datePicker.maxDate = c.timeInMillis
        datePickerDialog.show()
    }

    private fun togglePasswordVisibility() {
        isPasswordVisible = !isPasswordVisible
        if (isPasswordVisible) {
            ImageViewCompat.setImageTintList(password_toggle, ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.azure)))
            password_input_field.transformationMethod = null
            password_input_field.setSelection(password_input_field.text.length)
        } else {
            ImageViewCompat.setImageTintList(password_toggle, ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.whiteFour)))
            password_input_field.transformationMethod = PasswordTransformationMethod()
            password_input_field.setSelection(password_input_field.text.length)
        }
    }

    private fun toggleConfirmPasswordVisibility() {
        isConfirmPasswordVisible = !isConfirmPasswordVisible
        if (isConfirmPasswordVisible) {
            ImageViewCompat.setImageTintList(password_toggle_confirm, ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.azure)))
            confirm_password_input_field.transformationMethod = null
            confirm_password_input_field.setSelection(confirm_password_input_field.text.length)
        } else {
            ImageViewCompat.setImageTintList(password_toggle_confirm, ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.whiteFour)))
            confirm_password_input_field.transformationMethod = PasswordTransformationMethod()
            confirm_password_input_field.setSelection(confirm_password_input_field.text.length)
        }
    }
}