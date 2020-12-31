package net.babys_care.app.api.responses

import com.google.gson.annotations.SerializedName

class UserImageUploadResponse(
    val message: String?,
    @SerializedName("image_path")
    val imagePath: String
)