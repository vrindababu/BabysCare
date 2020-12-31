package jp.winas.android.foundation.scene

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlin.reflect.KClass

interface ViewModelable<T : ViewModel> {

    // Implement
    val viewModelClass: KClass<T>

    val viewModel: T?
        get() = when (this) {
            is Fragment -> ViewModelProvider(this, factory).get(viewModelClass.java)
            is FragmentActivity -> ViewModelProvider(this, factory).get(viewModelClass.java)
            else -> null
        }

    // retrieve ViewModel of Activity (from Fragment)
    val activityViewModel: T?
        get() = if (this is Fragment) when (val a = activity) {
            null -> null
            else -> ViewModelProvider(this, factory).get(viewModelClass.java)
        } else viewModel

    private val factory: ViewModelProvider.Factory get() = object : ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T = viewModelClass.java.getConstructor().newInstance() as T
    }

}