package net.babys_care.app.scene.settings

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.CustomViewTarget
import com.bumptech.glide.request.transition.Transition
import io.realm.Realm
import io.realm.kotlin.where
import jp.winas.android.foundation.scene.BaseFragment
import jp.winas.android.foundation.scene.ViewModelable
import kotlinx.android.synthetic.main.fragment_baby_info_edit.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.babys_care.app.AppManager
import net.babys_care.app.AppSetting
import net.babys_care.app.BuildConfig
import net.babys_care.app.R
import net.babys_care.app.api.requests.BabyInfoUpdateRequest
import net.babys_care.app.api.responses.ChildData
import net.babys_care.app.api.responses.ErrorResponse
import net.babys_care.app.api.responses.UserInfoUpdateResponse
import net.babys_care.app.constants.AppConstants.Companion.CAMERA_REQUEST_CODE
import net.babys_care.app.extensions.hideKeyBoard
import net.babys_care.app.extensions.resized
import net.babys_care.app.extensions.toDateJapaneseWithYear
import net.babys_care.app.models.Children
import net.babys_care.app.models.realmmodels.ChildrenModel
import net.babys_care.app.models.realmmodels.UserModel
import net.babys_care.app.scene.MainActivity
import net.babys_care.app.utils.JapaneseCharacterUtils
import net.babys_care.app.utils.LoadingHelper
import net.babys_care.app.utils.Toaster.showToast
import net.babys_care.app.utils.debugLogInfo
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.*

class BabyInfoEditFragment : BaseFragment(), ViewModelable<BabyInfoEditViewModel> {

    override val layout = R.layout.fragment_baby_info_edit
    override val viewModelClass = BabyInfoEditViewModel::class

    var childId: Int? = null
    private lateinit var realm: Realm
    private var loadingDialog: AlertDialog? = null
    private var uri: Uri? = null
    private var file: File? = null

    override fun onResume() {
        when(val activity = activity) {
            is MainActivity -> {
                activity.updateToolbarTitle(getString(R.string.confirm_baby_info))
            }
        }
        super.onResume()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        realm = Realm.getDefaultInstance()

        button_sign_up.setOnClickListener {
            validateInputAndProceed()
        }
        profile_image_change_button.setOnClickListener {
            if (checkStoragePermission()) {
                choosePicture()
            } else {
                grantPermission()
            }
        }
        birthday_text_view.setOnClickListener {
            showDatePicker()
        }

        addTextWatchers()
        setGenderSpinner()
        setBirthOrderSpinner()

        childId?.let { id ->
            button_sign_up.text = getString(R.string.to_edit)
            getChildInfo(id)
        } ?: kotlin.run {
            button_sign_up.text = getString(R.string.sign_up)
            profile_image.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.baby_girl_default_image))
        }
    }

    private fun getChildInfo(childId: Int) {
        val child = realm.where<ChildrenModel>().equalTo("childId", childId).findFirst() ?: return
        debugLogInfo("Child: $child")
        viewModel?.child?.birth_day = child.birthDay
        viewModel?.child?.gender = child.gender
        viewModel?.child?.birth_order = child.birthOrder
        viewModel?.child?.childId = child.childId
        display_name.text = child.firstName
        last_name_input_field.setText(child.lastName)
        first_name_input_field.setText(child.firstName)
        last_name_kana_input_field.setText(child.lastNameKana)
        first_name_kana_input_field.setText(child.firstNameKana)
        birthday_text_view.text = child.birthDay.toDateJapaneseWithYear()
        val gender = when(child.gender) {
            "male" -> 1
            "female" -> 2
            "other" -> 3
            else -> 0
        }
        gender_spinner.setSelection(gender)

        val defaultImage = when(child.gender) {
            "male" -> ContextCompat.getDrawable(requireContext(), R.drawable.baby_boy_default_image)
            else -> ContextCompat.getDrawable(requireContext(), R.drawable.baby_girl_default_image)
        }
        val defaultHonorificTitle = when(child.gender) {
            "male" -> getString(R.string.honorific_title_boy)
            else -> getString(R.string.honorific_title_girl)
        }
        honorific_title.text = defaultHonorificTitle
        child.image?.let {
            Glide.with(requireContext()).load("${BuildConfig.URL_IMAGE_DIRECTORY}$it").apply(RequestOptions().placeholder(defaultImage)).into(profile_image)
        } ?: kotlin.run {
            profile_image.setImageDrawable(defaultImage)
        }

        setSiblingOrderSpinner(child.gender)
        viewModel?.child?.sibling_order = child.siblingOrder
        Handler().postDelayed({
            birth_order_spinner.setSelection(if (child.birthOrder > 5) 5 else child.birthOrder)
            sibling_order_spinner.setSelection(child.siblingOrder)
        }, 500)
    }

    private fun addTextWatchers() {
        last_name_input_field.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun afterTextChanged(s: Editable?) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel?.child?.last_name = last_name_input_field.text.toString().trim()
                enableSignUpButton()
            }
        })

        first_name_input_field.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun afterTextChanged(s: Editable?) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel?.child?.first_name = first_name_input_field.text.toString().trim()
                enableSignUpButton()
            }
        })

        last_name_kana_input_field.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun afterTextChanged(s: Editable?) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel?.child?.last_name_kana = last_name_kana_input_field.text.toString().trim()
                enableSignUpButton()
            }
        })

        first_name_kana_input_field.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun afterTextChanged(s: Editable?) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel?.child?.first_name_kana = first_name_kana_input_field.text.toString().trim()
                enableSignUpButton()
            }
        })
    }

    private fun validateInputAndProceed() {
        val children = viewModel?.child ?: return

        if (!isLastNameKanjiValid(children.last_name)) {
            return
        }

        if (!isFirstNameKanjiValid(children.first_name)) {
            return
        }

        if (!isLastNameKanaValid(children.last_name_kana)) {
            return
        }

        if (!isFirstNameKanaValid(children.first_name_kana)) {
            return
        }

        val gender = viewModel?.child?.gender
        if (!isGenderValid(gender)) {
            return
        }

        if (!isBirthdayValid(children.birth_day)) {
            return
        }

        val birthOrder = birth_order_spinner.selectedItemPosition
        if (!isBirthOrderValid(birthOrder)) {
            return
        }

        val siblingOrder = sibling_order_spinner.selectedItemPosition
        if (!isSiblingOrderValid(siblingOrder)) {
            return
        }

        first_name_kana_input_field.hideKeyBoard()

        children.gender = gender
        children.birth_order = birthOrder
        children.sibling_order = siblingOrder

        registerOrUpdateBabyInfo(children)
    }

    private fun enableSignUpButton() {
        val children = viewModel?.child
        if (children == null) {
            button_sign_up.isEnabled = false
            return
        }
        button_sign_up.isEnabled = !(TextUtils.isEmpty(children.last_name)
                || TextUtils.isEmpty(children.first_name)
                || TextUtils.isEmpty(children.last_name_kana)
                || TextUtils.isEmpty(children.first_name_kana)
                || TextUtils.isEmpty(children.birth_day))
    }

    private fun setBirthOrderSpinner() {
        val japaneseCounting = JapaneseCharacterUtils().getJapaneseNumbers(1, 5)
        val birthOrderArray = mutableListOf<String>()
        birthOrderArray.add(0, getString(R.string.select_birth_order))
        japaneseCounting.forEach { item ->
            birthOrderArray.add(String.format(getString(R.string.child_birth_order), item))
        }

        val adapter = ArrayAdapter(requireContext(), R.layout.custom_spinner, birthOrderArray)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        birth_order_spinner.adapter = adapter
        birth_order_spinner.onItemSelectedListener = null
        birth_order_spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {}

            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                if (position == 0) {
                    (parent?.getChildAt(position) as? TextView)?.setTextColor(ContextCompat.getColor(parent.context, R.color.pinkishGrey))
                }
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
        gender_spinner.onItemSelectedListener = null
        gender_spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {}

            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val gender = if (position > 0) {
                    when(gender_spinner.selectedItem.toString()) {
                        "男の子" -> "male"
                        "女の子" -> "female"
                        "その他" -> "other"
                        else -> ""
                    }
                } else {
                    (parent?.getChildAt(position) as? TextView)?.setTextColor(ContextCompat.getColor(parent.context, R.color.pinkishGrey))
                    ""
                }
                viewModel?.child?.gender = gender
                setBirthOrderSpinner()
                setSiblingOrderSpinner(gender)
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
        sibling_order_spinner.onItemSelectedListener = null
        sibling_order_spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {}

            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                if (position == 0) {
                    (parent?.getChildAt(position) as? TextView)?.setTextColor(ContextCompat.getColor(parent.context, R.color.pinkishGrey))
                }
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
        val birthDate = when {
            viewModel?.child?.birth_day?.isEmpty() == true -> {
                null
            }
            viewModel?.child?.birth_day?.contains("/") == true -> {
                viewModel?.child?.birth_day?.split("/")
            }
            else -> {
                viewModel?.child?.birth_day?.split("-")
            }
        }
        val defaultYear = birthDate?.get(0)?.toInt() ?: calendar.get(Calendar.YEAR)
        val defaultMonth = birthDate?.get(1)?.toInt()?.minus(1) ?: calendar.get(Calendar.MONTH)
        val defaultDay = birthDate?.get(2)?.toInt() ?: calendar.get(Calendar.DAY_OF_MONTH)

        Locale.setDefault(Locale.JAPAN)
        val datePickerDialog = DatePickerDialog(requireContext(),
            { _: DatePicker, year: Int, month: Int, dayOfMonth: Int ->
                val currentYear = calendar.get(Calendar.YEAR)
                val birthYear = if (year < 2010) 2010 else if (year > currentYear) currentYear else year
                birthday_text_view.text = String.format(getString(R.string.year_month_day_format), birthYear, month + 1, dayOfMonth)
                viewModel?.child?.birth_day = "$birthYear/${month + 1}/$dayOfMonth"
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

    private fun registerOrUpdateBabyInfo(children: Children) {
        showLoadingDialog(true)
        childId?.let {
            viewModel?.updateBabyInfo(it, BabyInfoUpdateRequest(AppManager.apiToken, children)) {response, error ->
                showLoadingDialog(false)
                handleUpdateResponse(children.childId, response, error)
            }
        } ?: kotlin.run {
            viewModel?.createBabyInfo(BabyInfoUpdateRequest(AppManager.apiToken, children)) {response, error ->
                showLoadingDialog(false)
                if (response != null) {
                    response.data?.let {
                        AppSetting(AppManager.context).babyInfoChanged = true
                        if (uri != null) {
                            saveChildDataToLocalDb(it.children, it.isPremama)
                            uploadBabyImage(children.childId)
                        } else {
                            saveChildDataToLocalDb(it.children, it.isPremama, true)
                        }
                    }
                    response.message?.let {
                        showToast(it)
                    }
                } else {
                    error?.message?.let {
                        showToast(it, Toast.LENGTH_LONG)
                    }
                }
            }
        }
    }

    private fun handleUpdateResponse(childId: Int, response: UserInfoUpdateResponse?, error: ErrorResponse?) {
        if (response != null) {
            if (response.result) {
                response.data.message?.let {
                    showToast(it)
                }
                AppSetting(AppManager.context).babyInfoChanged = true
                if (uri != null) {
                    saveChildDataToLocalDb(response.data.children, response.data.is_premama)
                    uploadBabyImage(childId)
                } else {
                    saveChildDataToLocalDb(response.data.children, response.data.is_premama, true)
                }
            } else {
                showToast(response.data.message ?: "エラー")
            }
        } else {
            error?.message?.let {
                showToast(it, Toast.LENGTH_LONG)
            }
        }
    }

    private fun saveChildDataToLocalDb(children: List<ChildData>, isPremama: Int, isBack: Boolean = false) {
        launch(Dispatchers.Default) {
            val childList = mutableListOf<ChildrenModel>()
            val realm = Realm.getDefaultInstance()
            for (childUser in children) {
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

                val child = ChildrenModel(
                    id,
                    lastName,
                    firstName,
                    lastNameKana,
                    firstNameKana,
                    image,
                    gender,
                    birthday,
                    birthOrder,
                    siblingOrder
                )
                childList.add(child)
            }

            realm.beginTransaction()

            val user = realm.where<UserModel>().equalTo("apiToken", AppManager.apiToken).findFirst()
            user?.isPremama = isPremama

            if (childList.size > 0) {
                realm.insertOrUpdate(childList)
            }

            realm.commitTransaction()
            realm.close()
            launch(Dispatchers.Main) {
                activity?.setResult(MainActivity.RESULT_UPDATED)
                if (isBack) {
                    backToPreviousScreen()
                }
            }
        }
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

    private fun checkStoragePermission() = PackageManager.PERMISSION_GRANTED ==
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.READ_EXTERNAL_STORAGE
            )

    private fun grantPermission() =
        ActivityCompat.requestPermissions(
            requireActivity(),
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
            CAMERA_REQUEST_CODE
        )

    private fun choosePicture() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).apply {
            type = "image/*"
        }
        startActivityForResult(intent, CAMERA_REQUEST_CODE)
    }

    private fun uploadBabyImage(childId: Int) {
        val requestFile = file?.asRequestBody("multipart/form-data".toMediaTypeOrNull()) ?: return
        val body = MultipartBody.Part.createFormData("image", file?.name, requestFile)
        showLoadingDialog(true)

        launch {
            viewModel?.uploadBabyImage(body, childId) { response, error ->
                activity?.runOnUiThread {
                    showLoadingDialog(false)
                    if (response != null) {
                        response.message?.let {
                            showToast(it)
                        }
                        updateBabyImageUrlToLocalDb(response.imagePath)
                    } else {
                        error?.let {
                            showToast(it.message)
                        }
                    }
                }
            } ?: kotlin.run {
                activity?.runOnUiThread {
                    showLoadingDialog(false)
                }
            }
        }
    }

    private fun updateBabyImageUrlToLocalDb(imagePath: String) {
        val child = realm.where(ChildrenModel::class.java).findFirst()
        child?.let { childModel ->
            realm.executeTransaction { realmIns ->
                childModel.image = imagePath
                realmIns.insertOrUpdate(childModel)
            }
        }

        uri = null
        backToPreviousScreen()
    }

    private fun getFileFromBitmap(bitmap: Bitmap) {
        file = File(requireContext().cacheDir, "baby_care_child_image.png")
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

    private fun backToPreviousScreen() {
        activity?.onBackPressed()
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

    override fun onStop() {
        if (loadingDialog != null) {
            loadingDialog?.dismiss()
            loadingDialog = null
        }

        super.onStop()
    }

    override fun onDestroyView() {
        if (file?.exists() == true) {
            file?.deleteRecursively()
        }
        realm.close()
        super.onDestroyView()
    }
}