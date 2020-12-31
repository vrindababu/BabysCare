package jp.winas.android.foundation.scene

import kotlin.properties.ObservableProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

inline fun <T> observe(
    initialValue: T,
    noinline before: ((property: KProperty<*>, oldValue: T, newValue: T) -> Boolean)? = null,
    crossinline after: (property: KProperty<*>, oldValue: T, newValue: T) -> Unit)
        : ReadWriteProperty<Any?, T> =
    when (before) {
        null -> object : ObservableProperty<T>(initialValue) {
            override fun afterChange(property: KProperty<*>, oldValue: T, newValue: T) = after(property, oldValue, newValue)
        }
        else -> object : ObservableProperty<T>(initialValue) {
            override fun afterChange(property: KProperty<*>, oldValue: T, newValue: T) = after(property, oldValue, newValue)
            override fun beforeChange(property: KProperty<*>, oldValue: T, newValue: T) = before(property, oldValue, newValue)
        }
    }
