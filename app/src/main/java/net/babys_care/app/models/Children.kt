package net.babys_care.app.models

import com.google.gson.annotations.SerializedName

data class Children(
    @SerializedName("child_id")
    @Transient
    var childId: Int = 0,
    var last_name: String? = null,
    var first_name: String? = null,
    var last_name_kana: String? = null,
    var first_name_kana: String? = null,
    var image: String? = null,
    var gender: String? = null,
    var birth_day: String? = null,
    var birth_order: Int? = null,
    var sibling_order: Int? = null
)