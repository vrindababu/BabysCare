package net.babys_care.app.models.realmmodels

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import io.realm.annotations.RealmClass
import io.realm.annotations.RealmNamingPolicy

@RealmClass(name = "users", fieldNamingPolicy = RealmNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
open class UserModel (
    @PrimaryKey var userId: Int = 0,
    var email: String = "",
    var password: String = "",
    var userType: String = "",
    var status: String = "",
    var apiToken: String = "",
    var parentId: Int = 0,
    var firstName: String = "",
    var lastName: String = "",
    var firstNameKana: String = "",
    var lastNameKana: String = "",
    var image: String? = null,
    var gender: String = "",
    var birthday: String = "",
    var postalCode: String = "",
    var prefecture: Int = 0,
    var city: String = "",
    var building: String? = null,
    var isNotifiableLocal: Int = 0,
    var isNotifiableRemote: Int = 0,
    var isPremama: Int = 0
): RealmObject()