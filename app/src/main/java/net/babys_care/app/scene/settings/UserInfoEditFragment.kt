package net.babys_care.app.scene.settings

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.CustomViewTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.realm.Realm
import jp.winas.android.foundation.scene.BaseFragment
import jp.winas.android.foundation.scene.ViewModelable
import kotlinx.android.synthetic.main.fragment_user_info_edit.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.babys_care.app.AppManager
import net.babys_care.app.AppSetting
import net.babys_care.app.BuildConfig
import net.babys_care.app.R
import net.babys_care.app.api.requests.UserInfoUpdateRequest
import net.babys_care.app.api.responses.ErrorResponse
import net.babys_care.app.api.responses.UserImageUploadResponse
import net.babys_care.app.api.responses.UserInfoUpdateResponse
import net.babys_care.app.constants.AppConstants.Companion.CAMERA_REQUEST_CODE
import net.babys_care.app.constants.Prefecture
import net.babys_care.app.extensions.hideKeyBoard
import net.babys_care.app.extensions.resized
import net.babys_care.app.extensions.toDateJapaneseWithYear
import net.babys_care.app.models.Parent
import net.babys_care.app.models.User
import net.babys_care.app.models.realmmodels.ChildrenModel
import net.babys_care.app.models.realmmodels.UserModel
import net.babys_care.app.utils.DialogHelper
import net.babys_care.app.utils.Encrypt
import net.babys_care.app.utils.LoadingHelper
import net.babys_care.app.utils.Toaster.showToast
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.*

class UserInfoEditFragment : BaseFragment(), ViewModelable<UserInfoEditViewModel> {

    override val layout = R.layout.fragment_user_info_edit
    override val viewModelClass = UserInfoEditViewModel::class

    var user: UserModel? = null
    private var loadingDialog: AlertDialog? = null
    private var uri: Uri? = null
    private var file: File? =null
    private var isPasswordVisible: Boolean = false
    private var isConfirmPasswordVisible: Boolean = false

    override fun onResume() {
        (activity as? AccountSettingsActivity)?.updateToolbarTitle(getString(R.string.edit_user_info))
        super.onResume()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        button_sign_up.setOnClickListener {
            validateInputAndProceed()
        }

        birthday_text_view.setOnClickListener {
            showDatePicker()
        }

        setGenderSpinner()
        setPrefectureSpinner()

        addTextWatchers()

        profile_image_change_button.setOnClickListener {
            if (checkStoragePermission()) {
                choosePicture()
            } else {
                grantPermission()
            }
        }

        password_toggle.setOnClickListener {
            togglePasswordVisibility()
        }

        password_toggle_confirm.setOnClickListener {
            toggleConfirmPasswordVisibility()
        }

        setUserData()
    }

    private fun addTextWatchers() {
        last_name_input_field.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun afterTextChanged(s: Editable?) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel?.parentUser?.last_name = last_name_input_field.text.toString().trim()
                enableSignUpButton()
            }
        })

        first_name_input_field.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun afterTextChanged(s: Editable?) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel?.parentUser?.first_name = first_name_input_field.text.toString().trim()
                enableSignUpButton()
            }
        })

        last_name_kana_input_field.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun afterTextChanged(s: Editable?) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel?.parentUser?.last_name_kana =
                    last_name_kana_input_field.text.toString().trim()
                enableSignUpButton()
            }
        })

        first_name_kana_input_field.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun afterTextChanged(s: Editable?) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel?.parentUser?.first_name_kana =
                    first_name_kana_input_field.text.toString().trim()
                enableSignUpButton()
            }
        })

        postal_code_input_field.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun afterTextChanged(s: Editable?) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel?.parentUser?.postal_code = postal_code_input_field.text.toString().trim()
                enableSignUpButton()
            }
        })

        city_street_input_field.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel?.parentUser?.city = city_street_input_field.text.toString()
                enableSignUpButton()
            }
        })

        email_address_input_field.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                enableSignUpButton()
            }
        })

        password_input_field.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                enableSignUpButton()
            }
        })

        confirm_password_input_field.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                enableSignUpButton()
            }
        })
    }

    private fun enableSignUpButton() {
        val user = viewModel?.parentUser ?: return
        val email = email_address_input_field.text.toString().trim()
        val password = password_input_field.text.toString().trim()
        val confirmPassword = confirm_password_input_field.text.toString().trim()
        button_sign_up.isEnabled = !(TextUtils.isEmpty(user.last_name)
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

        val postalCode = postal_code_input_field.text.toString().trim()
        if (!isPostalCodeValid(postalCode)) {
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

        val parent = Parent(
            firstNameKanji, lastNameKanji, firstNameKana, lastNameKana, gender,
            birthDay, postalCode, prefecture, cityName, buildingName, this.user?.isPremama ?: 0
        )
        val user = User(email, password)
        val request = UserInfoUpdateRequest(AppManager.apiToken, parent, user)
        updateUserInfo(request)
    }

    private fun updateUserInfo(request: UserInfoUpdateRequest) {
        if (loadingDialog != null) {
            loadingDialog?.dismiss()
            loadingDialog = null
        }

        loadingDialog = LoadingHelper().getLoadingDialog(requireContext())
        loadingDialog?.show()
        viewModel?.updateUserInfo(request) { response, error ->
            if (response != null) {

                if (uri != null) {
                    uploadUserImage(response.data.parent_id)
                } else {
                    loadingDialog?.dismiss()
                    loadingDialog = null
                }

                saveUserInfoIntoLocalDb(response)

            } else {

                loadingDialog?.dismiss()
                loadingDialog = null

                error?.message?.let {
                    val activity = activity ?: return@let
                    DialogHelper().showDialog(activity, it)
                }
            }
        }
    }

    private fun saveUserInfoIntoLocalDb(response: UserInfoUpdateResponse) {
        val realm = Realm.getDefaultInstance()
        realm.beginTransaction()

        val user = this.user ?: UserModel()
        val data = response.data

        user.lastName = data.last_name
        user.firstName = data.first_name
        user.lastNameKana = data.last_name_kana
        user.firstNameKana = data.first_name_kana
        user.birthday = data.birth_day
        user.postalCode = data.postal_code
        user.city = data.city
        user.building = data.building
        user.userType = data.user_type
        user.status = data.status
        user.isPremama = data.is_premama
        user.email = data.email
        user.password = this.user?.password ?: ""
        user.parentId = data.parent_id
        user.gender = data.gender
        user.prefecture = data.prefecture
        user.image = data.image
        user.isNotifiableLocal = data.is_notifiable_local
        user.isNotifiableRemote = data.is_notifiable_remote

        this.user?.userId?.let { id ->
            if (id != data.user_id) {
                user.userId = data.user_id
                realm.delete(UserModel::class.java)
            }
        }

        realm.insertOrUpdate(user)

        val childList = mutableListOf<ChildrenModel>()
        for (childUser in data.children) {
            val id = childUser.childId
            val firstName = childUser.first_name
            val lastName = childUser.last_name
            val firstNameKana = childUser.first_name_kana
            val lastNameKana = childUser.last_name_kana
            val image = childUser.image
            val gender = childUser.gender
            val birthday = childUser.birth_day
            val birthOrder = childUser.birth_order
            val siblingOrder = childUser.sibling_order

            val child = ChildrenModel(id, lastName, firstName, lastNameKana, firstNameKana, image, gender, birthday, birthOrder, siblingOrder)
            childList.add(child)
        }

        if (childList.size > 0) {
            realm.insertOrUpdate(childList)
        }

        realm.commitTransaction()
        realm.close()

        if (uri == null) {
            showCompletionDialog(response.data.message ?: getString(R.string.edit_completed))
        }
        AppSetting(requireContext()).userInfoUpdated = true
    }

    private fun isValidEmail(email: String): Boolean {
        if (TextUtils.isEmpty(email)) {
            email_error.visibility = View.VISIBLE
            email_error.text = String.format(getString(R.string.enter_at_least_n_character), 6)
            return false
        } else if (email.length > 255) {
            email_error.visibility = View.VISIBLE
            email_error.text = String.format(getString(R.string.enter_at_most_n_character), 255)
            return false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            email_error.visibility = View.VISIBLE
            email_error.text = getString(R.string.enter_in_email_address_format)
            return false
        }

        email_error.visibility = View.GONE
        return true
    }

    private fun isValidPassword(password: String): Boolean {
        if (TextUtils.isEmpty(password) || password.length < 6) {
            password_error.visibility = View.VISIBLE
            password_error.text = String.format(getString(R.string.enter_at_least_n_character), 6)
            return false
        } else if (password.length > 255) {
            password_error.visibility = View.VISIBLE
            password_error.text = String.format(getString(R.string.enter_at_most_n_character), 255)
            return false
        }

        password_error.visibility = View.GONE
        return true
    }

    private fun isConfirmPasswordValid(confirmPassword: String, password: String): Boolean {
        if (confirmPassword != password) {
            confirm_password_error.visibility = View.VISIBLE
            confirm_password_error.text = getString(R.string.enter_same_password_for_confirmation)
            return false
        }

        confirm_password_error.visibility = View.GONE
        return true
    }

    private fun isFirstNameKanjiValid(name: String): Boolean {
        if (TextUtils.isEmpty(name)) {
            first_name_kanji_error.visibility = View.VISIBLE
            first_name_kanji_error.text = String.format(
                getString(R.string.enter_at_least_n_character),
                1
            )
            return false
        } else if (name.length > 100) {
            first_name_kanji_error.visibility = View.VISIBLE
            first_name_kanji_error.text = String.format(
                getString(R.string.enter_at_most_n_character),
                100
            )
            return false
        }

        first_name_kanji_error.visibility = View.GONE
        return true
    }

    private fun isLastNameKanjiValid(name: String): Boolean {
        if (TextUtils.isEmpty(name)) {
            last_name_kanji_error.visibility = View.VISIBLE
            last_name_kanji_error.text = String.format(
                getString(R.string.enter_at_least_n_character),
                1
            )
            return false
        } else if (name.length > 100) {
            last_name_kanji_error.visibility = View.VISIBLE
            last_name_kanji_error.text = String.format(
                getString(R.string.enter_at_most_n_character),
                100
            )
            return false
        }

        last_name_kanji_error.visibility = View.GONE
        return true
    }

    private fun isFirstNameKanaValid(name: String): Boolean {
        if (TextUtils.isEmpty(name)) {
            first_name_kana_error.visibility = View.VISIBLE
            first_name_kana_error.text = String.format(
                getString(R.string.enter_at_least_n_character),
                1
            )
            return false
        } else if (name.length > 100) {
            first_name_kana_error.visibility = View.VISIBLE
            first_name_kana_error.text = String.format(
                getString(R.string.enter_at_most_n_character),
                100
            )
            return false
        }

        first_name_kana_error.visibility = View.GONE
        return true
    }

    private fun isLastNameKanaValid(name: String): Boolean {
        if (TextUtils.isEmpty(name)) {
            last_name_kana_error.visibility = View.VISIBLE
            last_name_kana_error.text = String.format(
                getString(R.string.enter_at_least_n_character),
                1
            )
            return false
        } else if (name.length > 100) {
            last_name_kana_error.visibility = View.VISIBLE
            last_name_kana_error.text = String.format(
                getString(R.string.enter_at_most_n_character),
                100
            )
            return false
        }

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

    private fun isPostalCodeValid(postalCode: String): Boolean {
        if (TextUtils.isEmpty(postalCode) || postalCode.length < 7) {
            postal_code_error.visibility =  View.VISIBLE
            postal_code_error.text = String.format(
                getString(R.string.enter_at_least_n_character),
                7
            )
            return false
        } else if (postalCode.length > 7){
            postal_code_error.visibility =  View.VISIBLE
            postal_code_error.text = String.format(getString(R.string.enter_at_most_n_character), 7)
            return false
        }

        postal_code_error.visibility =  View.GONE
        return true
    }

    private fun isCityNameValid(name: String): Boolean {
        if (TextUtils.isEmpty(name)) {
            city_name_error.visibility = View.VISIBLE
            city_name_error.text = String.format(getString(R.string.enter_at_least_n_character), 1)
            return false
        } else if (name.length > 100) {
            city_name_error.visibility = View.VISIBLE
            city_name_error.text = String.format(getString(R.string.enter_at_most_n_character), 100)
            return false
        }

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
        val genderArray = arrayOf(
            getString(R.string.select_gender), getString(R.string.male), getString(
                R.string.female
            ), getString(R.string.other)
        )
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
        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _: DatePicker, year: Int, month: Int, dayOfMonth: Int ->
                val currentYear = c.get(Calendar.YEAR)
                val birthYear =
                    if (year < 1960) 1960 else if (year > currentYear) currentYear else year
                birthday_text_view.text = String.format(
                    getString(R.string.year_month_day_format),
                    birthYear,
                    month + 1,
                    dayOfMonth
                )
                viewModel?.parentUser?.birth_day = "$birthYear/${month + 1}/$dayOfMonth"
                enableSignUpButton()
            }, defaultYear, defaultMonth, defaultDay
        )
        val minDate = Calendar.getInstance()
        minDate.set(1960, 0, 1)
        datePickerDialog.datePicker.minDate = minDate.timeInMillis
        datePickerDialog.datePicker.maxDate = c.timeInMillis
        datePickerDialog.show()
    }

    private fun setUserData() {
        viewModel?.parentUser?.birth_day = user?.birthday ?: ""
        viewModel?.parentUser?.is_premama = user?.isPremama ?: 0
        display_name.text = user?.firstName
        last_name_input_field.setText(user?.lastName)
        first_name_input_field.setText(user?.firstName)
        last_name_kana_input_field.setText(user?.lastNameKana)
        first_name_kana_input_field.setText(user?.firstNameKana)
        birthday_text_view.text = user?.birthday?.toDateJapaneseWithYear()
        postal_code_input_field.setText(user?.postalCode)
        city_street_input_field.setText(user?.city)
        building_name_input_field.setText(user?.building)
        email_address_input_field.setText(user?.email)
        val decryptedPassword = Encrypt().getDecryptedPassword(
            user?.password ?: "", getString(R.string.encryption_pass)
        )
        password_input_field.setText(decryptedPassword)
        confirm_password_input_field.setText(decryptedPassword)
        val gender = when(user?.gender) {
            "male" -> 1
            "female" -> 2
            "other" -> 3
            else -> 0
        }
        gender_spinner.setSelection(gender)
        prefecture_spinner.setSelection(user?.prefecture ?: 0)

        val defaultImage = when(user?.gender) {
            "male" -> ContextCompat.getDrawable(requireContext(), R.drawable.default_profile_male)
            else -> ContextCompat.getDrawable(requireContext(), R.drawable.default_profile_female)
        }
        user?.image?.let {
            Glide.with(requireContext()).load("${BuildConfig.URL_IMAGE_DIRECTORY}$it").apply(RequestOptions().placeholder(defaultImage)).into(profile_image)
        } ?: kotlin.run {
            profile_image.setImageDrawable(defaultImage)
        }
    }

    private fun checkStoragePermission() = PackageManager.PERMISSION_GRANTED ==
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE)

    private fun grantPermission() =
        ActivityCompat.requestPermissions(requireActivity(),
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
            CAMERA_REQUEST_CODE
        )

    private fun choosePicture() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).apply {
            type = "image/*"
        }
        startActivityForResult(intent, CAMERA_REQUEST_CODE)
    }

    private fun getFileFromBitmap(bitmap: Bitmap) {
        file = File(requireContext().cacheDir, "baby_care_image.png")
        file?.createNewFile()

        //Convert bitmap to byte array
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 0, baos)
        val byte = baos.toByteArray()

        //Write bites to file
        val fos = FileOutputStream(file)
        fos.write(byte)
        fos.flush()
        fos.close()
    }

    private fun uploadUserImage(parentId: Int) {
        val requestFile = file?.asRequestBody("multipart/form-data".toMediaTypeOrNull()) ?: return
        val body = MultipartBody.Part.createFormData("image", file?.name, requestFile)

        launch {
            viewModel?.uploadUserImage(parentId, body) {response, error ->
                handleImageUploadResponse(response, error)
            }
        }
    }

    private fun handleImageUploadResponse(
        response: UserImageUploadResponse?,
        error: ErrorResponse?
    ) {
        loadingDialog?.dismiss()
        loadingDialog = null
        if (response != null) {
            updateUserImageUrlToLocalDb(response)
        } else {
            error?.let {
                showToast(it.message)
            }
        }
    }

    private fun updateUserImageUrlToLocalDb(response: UserImageUploadResponse) {
        val realm = Realm.getDefaultInstance()
        val user = realm.where(UserModel::class.java).findFirst()
        user?.let {
            realm.beginTransaction()
            user.image = response.imagePath
            realm.insertOrUpdate(user)
            realm.commitTransaction()
        }

        realm.close()
        uri = null
        showCompletionDialog(response.message ?: getString(R.string.edit_completed))
    }

    private fun showCompletionDialog(message: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton(getString(R.string.ok)) { dialog, _ ->
                dialog.dismiss()
                activity?.onBackPressed()
            }
            .show()
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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == CAMERA_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                choosePicture()
            } else {
                showToast("許可が必要です。")
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == CAMERA_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                uri = data?.data
                Glide.with(requireContext()).asBitmap().load(uri).into(object : CustomViewTarget<ImageView, Bitmap>(profile_image){
                    override fun onResourceReady(
                        resource: Bitmap,
                        transition: Transition<in Bitmap>?
                    ) {
                        profile_image.setImageBitmap(resource)
                        CoroutineScope(Dispatchers.Default).launch {
                            getFileFromBitmap(resource.resized(400))
                        }
                    }

                    override fun onLoadFailed(errorDrawable: Drawable?) {
                        uri = null
                    }

                    override fun onResourceCleared(placeholder: Drawable?) {}
                })
            } else {
                uri = null
            }
        }

        super.onActivityResult(requestCode, resultCode, data)
    }
}