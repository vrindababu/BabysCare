package jp.winas.android.foundation.extension

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.annotation.MainThread

@MainThread
fun <T> LiveData<T>.onChanged(owner: LifecycleOwner, cb: (T?) -> Unit) {
    observe(owner, object : Observer<T> {
        override fun onChanged(t: T?) = cb(t)
    })
}