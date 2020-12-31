package net.babys_care.app.scene.home

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnFocusChangeListener
import android.widget.Button
import android.widget.DatePicker
import android.widget.TextView
import androidx.core.content.ContextCompat
import io.realm.Realm
import io.realm.kotlin.where
import jp.winas.android.foundation.extension.onChanged
import jp.winas.android.foundation.scene.BaseFragment
import jp.winas.android.foundation.scene.ViewModelable
import kotlinx.android.synthetic.main.fragment_growth_data_input.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.babys_care.app.AppManager
import net.babys_care.app.R
import net.babys_care.app.api.responses.GrowthHistory
import net.babys_care.app.constants.AppConstants
import net.babys_care.app.extensions.hideKeyBoard
import net.babys_care.app.extensions.toDateFormatWithDay
import net.babys_care.app.models.realmmodels.GrowthHistories
import net.babys_care.app.scene.MainActivity
import net.babys_care.app.utils.DialogHelper
import net.babys_care.app.utils.LoadingHelper
import net.babys_care.app.utils.Toaster.showToast
import net.babys_care.app.utils.debugLogInfo
import java.text.SimpleDateFormat
import java.util.*
import kotlin.properties.Delegates

class GrowthDataInputFragment : BaseFragment(), ViewModelable<GrowthInputViewModel> {

    override val layout = R.layout.fragment_growth_data_input
    override val viewModelClass = GrowthInputViewModel::class

    private var loadingDialog: AlertDialog? = null
    private var childID by Delegates.notNull<Int>()
    private lateinit var calendar: Calendar

    override fun onResume() {
        (activity as? MainActivity)?.addBackButtonAndActionToMain(true)
        (activity as? MainActivity)?.updateToolbarTitle(getString(R.string.daily_record))
        super.onResume()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        childID = arguments?.getInt(AppConstants.CHILD_ID) ?: -1
        if (childID < 1) {
            debugLogInfo("ChildId is invalid. Returning to previous screen")
            activity?.onBackPressed()
            return
        }
        setDate()
        addTextWatchers()
        button_record.setOnClickListener {
            validateInputAndProceed()
        }
        date_value.setOnClickListener {
            showDatePicker()
        }
        viewModel?.growthResponseLiveData?.onChanged(viewLifecycleOwner){response ->
            showLoadingDialog(false)
            if (!AppManager.isLoggedIn) {
                showDialogAndTransitToLogin(getString(R.string.authentication_error_ea002))
                return@onChanged
            }
            if (response?.data?.growth_histories?.isNotEmpty() == true) {
                saveDataToLocalDB(response.data.growth_histories)
            }

            response?.data?.message?.let {
                showToast(it)
            }
        }

        viewModel?.errorResponse?.onChanged(viewLifecycleOwner){error ->
            showLoadingDialog(false)
            if (!AppManager.isLoggedIn) {
                showDialogAndTransitToLogin(getString(R.string.authentication_error_ea002))
                return@onChanged
            }
            debugLogInfo("Error: $error")
        }
    }

    private fun setDate() {
        calendar = Calendar.getInstance()
        date_value.text = "${calendar.time.toDateFormatWithDay()}"
    }

    private fun addTextWatchers() {
        height_value.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun afterTextChanged(s: Editable?) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                enableSignUpButton()
                if (isNotNullAndBlank(s.toString())) height_unit.setTextColor(ContextCompat.getColor(requireContext(), R.color.brownishGrey))
                else height_unit.setTextColor(ContextCompat.getColor(requireContext(), R.color.pinkishGrey))
            }
        })
        height_value.onFocusChangeListener = OnFocusChangeListener { _, hasFocus ->
            if (!hasFocus && isNotNullAndBlank(height_value.text.toString())) {
                height_value.setText(formatInput(height_value.text.toString()))
            }
        }

        weight_value.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun afterTextChanged(s: Editable?) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                enableSignUpButton()
                if (isNotNullAndBlank(s.toString())) weight_unit.setTextColor(ContextCompat.getColor(requireContext(), R.color.brownishGrey))
                else weight_unit.setTextColor(ContextCompat.getColor(requireContext(), R.color.pinkishGrey))
            }
        })
        weight_value.onFocusChangeListener = OnFocusChangeListener { _, hasFocus ->
            if (!hasFocus && isNotNullAndBlank(weight_value.text.toString())) {
                weight_value.setText(formatInput(weight_value.text.toString()))
            }
        }
    }

    private fun validateInputAndProceed() {
        val height = height_value.text.toString().trim()
        val weight = weight_value.text.toString().trim()
        if (!isValidHeight(height)) {
            return
        }
        if (!isValidWeight(weight)) {
            return
        }
        if (!isValidDate()) {
            return
        }

        hideKeyBoard()

        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.JAPAN)
        val dateString = formatter.format(calendar.time) ?: return

        if (!isSameDateDataExists(Date(calendar.timeInMillis))) {
            showConfirmationDialog(dateString, height.toDouble(), weight.toDouble())
        } else {
            createGrowthData(dateString, height.toDouble(), weight.toDouble())
        }
    }

    private fun isSameDateDataExists(date: Date): Boolean {
        val realm = Realm.getDefaultInstance()
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = date.time
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val date1 = Date(calendar.timeInMillis)
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        val growthHistory = realm.where<GrowthHistories>()
            .equalTo("childId", childID)
            .greaterThanOrEqualTo("measuredAt", date1)
            .lessThan("measuredAt", date)
            .findFirst()
        if (growthHistory != null) {
            return false
        }

        return true
    }

    private fun enableSignUpButton() {
        val height = height_value.text.toString().trim()
        val weight = weight_value.text.toString().trim()
        button_record.isEnabled = !(TextUtils.isEmpty(height) || TextUtils.isEmpty(weight))
    }

    private fun isValidHeight(height: String): Boolean {
        if (TextUtils.isEmpty(height)) {
            height_error.text = getString(R.string.must_enter_height_weight_value)
            height_error.visibility = View.VISIBLE
            return false
        }

        if (height.toDouble() < 0) {
            height_error.text = getString(R.string.enter_zero_or_more)
            height_error.visibility = View.VISIBLE
            return false
        }

        if (height.toDouble() >= 1000) {
            height_error.text = getString(R.string.enter_less_than_thousand)
            height_error.visibility = View.VISIBLE
            return false
        }

        height_error.visibility = View.GONE

        return true
    }

    private fun isValidWeight(weight: String): Boolean {
        if (TextUtils.isEmpty(weight)) {
            weight_error.text = getString(R.string.must_enter_height_weight_value)
            weight_error.visibility = View.VISIBLE
            return false
        }

        if (weight.toDouble() < 0) {
            weight_error.text = getString(R.string.enter_zero_or_more)
            weight_error.visibility = View.VISIBLE
            return false
        }

        if (weight.toDouble() >= 1000) {
            weight_error.text = getString(R.string.enter_less_than_thousand)
            weight_error.visibility = View.VISIBLE
            return false
        }

        weight_error.visibility = View.GONE

        return true
    }

    private fun isValidDate(): Boolean {
        val currentDate = Calendar.getInstance()
        if (currentDate.before(calendar)) {
            date_error.text = getString(R.string.enter_valid_date)
            date_error.visibility = View.VISIBLE
            return false
        }

        date_error.visibility = View.GONE

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
                this.calendar.set(year, month, dayOfMonth)
                date_value.text = this.calendar.time.toDateFormatWithDay()
                enableSignUpButton()
            }, defaultYear, defaultMonth, defaultDay
        )

        datePickerDialog.datePicker.maxDate = calendar.timeInMillis
        datePickerDialog.show()
    }

    private fun createGrowthData(measuredAt: String, height: Double, weight: Double) {
        showLoadingDialog(true)
        viewModel?.createGrowthData(AppManager.apiToken, childID, measuredAt, height, weight)
    }

    private fun saveDataToLocalDB(growthHistories: List<GrowthHistory>) {
        launch(Dispatchers.Default) {
            val formatter1 = SimpleDateFormat("yyyy/MM/dd", Locale.JAPAN)
            val formatter2 = SimpleDateFormat("yyyy-MM-dd", Locale.JAPAN)
            val growthDataList: MutableList<GrowthHistories> = mutableListOf()
            for (history in growthHistories) {
                val dateFormatter = if (history.measuredAt.contains("/")) {
                    formatter1
                } else {
                    formatter2
                }
                val growth = GrowthHistories("${history.childId}${history.measuredAt}", history.childId, dateFormatter.parse(history.measuredAt) ?: Date(), history.height, history.weight)
                growthDataList.add(growth)
            }

            val realm = Realm.getDefaultInstance()
            realm.executeTransaction { bgRealm ->
                bgRealm.insertOrUpdate(growthDataList)
            }
            realm.close()
            launch(Dispatchers.Main) {
                activity?.onBackPressed()
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

    private fun showDialogAndTransitToLogin(message: String) {
        val activity = activity ?: return
        DialogHelper().showLoginTransitionDialog(activity, message, true)
    }

    private fun showConfirmationDialog(measuredAt: String, height: Double, weight: Double) {
        val dialog = AlertDialog.Builder(requireContext())
        val layout = LayoutInflater.from(requireContext()).inflate(R.layout.layout_custom_dialog, null, false)
        dialog.setView(layout)

        val alert = dialog.create()
        layout.findViewById<TextView>(R.id.title).text = getString(R.string.title_same_day_data_already_exists)
        layout.findViewById<TextView>(R.id.message).text = getString(R.string.want_to_override)
        layout.findViewById<Button>(R.id.ok_button).setOnClickListener {
            alert.dismiss()
            createGrowthData(measuredAt, height, weight)
        }
        layout.findViewById<Button>(R.id.cancel_button).setOnClickListener {
            alert.dismiss()
        }

        alert.show()
        alert.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    override fun onStop() {
        (activity as? MainActivity)?.addBackButtonAndActionToMain(false)
        if (loadingDialog != null) {
            loadingDialog?.dismiss()
            loadingDialog = null
        }
        super.onStop()
    }

    private fun formatInput(input: String): String {
        return "%.3f".format(input.toDouble())
    }

    private fun isNotNullAndBlank(input: String?): Boolean {
        return input != null && input.isNotBlank()
    }
}