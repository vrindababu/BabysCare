package net.babys_care.app.models.realmmodels

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import io.realm.annotations.RealmClass
import io.realm.annotations.RealmNamingPolicy

@RealmClass(name = "children", fieldNamingPolicy = RealmNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
open class ChildrenModel (
    @PrimaryKey var childId: Int = 0,
    var lastName: String = "",
    var firstName: String = "",
    var lastNameKana: String = "",
    var firstNameKana: String = "",
    var image: String? = null,
    var gender: String = "",
    var birthDay: String = "",
    var birthOrder: Int = 0,
    var siblingOrder: Int = 0
): RealmObject()