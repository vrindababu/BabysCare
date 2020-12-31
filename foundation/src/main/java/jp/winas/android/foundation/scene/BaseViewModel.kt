package jp.winas.android.foundation.scene

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel

open class BaseViewModel : ViewModel()

open class BaseAndroidViewModel(app: Application) : AndroidViewModel(app)