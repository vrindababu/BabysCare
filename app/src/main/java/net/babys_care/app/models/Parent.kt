package net.babys_care.app.models

data class Parent(
    var first_name: String,
    var last_name: String,
    var first_name_kana: String,
    var last_name_kana: String,
    var gender: String,
    var birth_day: String,
    var postal_code: String,
    var prefecture: Int,
    var city: String,
    var building: String? = null,
    var is_premama: Int = 0,
    var fcm_token: String = "",
    val device_os: String = "Android"
)