package net.babys_care.app.models.realmmodels

import com.google.gson.annotations.SerializedName
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import io.realm.annotations.RealmClass
import io.realm.annotations.RealmNamingPolicy
import io.realm.annotations.Required
import java.util.*

@RealmClass(name = "growth_histories", fieldNamingPolicy = RealmNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
open class GrowthHistories(
    @PrimaryKey @Required
    var id: String = "",
    @SerializedName("child_id")
    var childId: Int = 0,
    @SerializedName("measured_at")
    var measuredAt: Date = Date(),
    var height: Float = 0.0f,
    var weight: Float = 0.0f
): RealmObject()