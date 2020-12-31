package jp.winas.android.foundation.storage

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

interface Storable {
    fun clear()
}

open class StoredMap(context: Context) : Storable {

    val shared: SharedPreferences = context.getSharedPreferences(this::class.qualifiedName, Context.MODE_PRIVATE)

    inline fun <reified T : Any> storedMap(
        defaultValue: T? = when (T::class) {
            Int::class -> 0
            Long::class -> 0L
            Float::class -> 0F
            Boolean::class -> false
            String::class -> ""
            else -> null
        } as T?
    ) = shared.delegate(defaultValue, T::class)

    override fun clear() = shared.edit().clear().apply()

}

fun <T : Any> SharedPreferences.delegate(defaultValue: T?, kClass: KClass<T>) = object : ReadWriteProperty<Any, T?> {

    val getter: SharedPreferences.(key: String, defaultValue: T) -> T? = when (kClass) {
        Int::class -> SharedPreferences::getInt
        Long::class -> SharedPreferences::getLong
        Float::class -> SharedPreferences::getFloat
        Boolean::class -> SharedPreferences::getBoolean
        else -> SharedPreferences::getString
    } as SharedPreferences.(key: String, defaultValue: T) -> T?

    val setter: SharedPreferences.Editor.(key: String, value: T) -> SharedPreferences.Editor = when (kClass) {
        Int::class -> SharedPreferences.Editor::putInt
        Long::class -> SharedPreferences.Editor::putLong
        Float::class -> SharedPreferences.Editor::putFloat
        Boolean::class -> SharedPreferences.Editor::putBoolean
        else -> SharedPreferences.Editor::putString
    } as SharedPreferences.Editor.(key: String, value: T) -> SharedPreferences.Editor

    override fun getValue(thisRef: Any, property: KProperty<*>): T? {
        val target = property.name
        return if (contains(target)) when (kClass) {
            Int::class, Long::class, Float::class, Boolean::class, String::class -> getter(target, defaultValue!!)
            else -> Gson().fromJson(getString(target, ""), kClass.java)
        } else null
    }

    override fun setValue(thisRef: Any, property: KProperty<*>, value: T?) {
        val target = property.name
        if (value == null) {
            edit().remove(target).apply()
        } else {
            when (kClass) {
                Int::class, Long::class, Float::class, Boolean::class, String::class -> edit().setter(target, value).apply()
                else -> edit().putString(target, Gson().toJson(value)).apply()
            }
        }
    }

}