package net.babys_care.app.models.realmmodels

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import io.realm.annotations.RealmClass
import io.realm.annotations.RealmNamingPolicy

@RealmClass(name = "news", fieldNamingPolicy = RealmNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
open class NewsModel(
    @PrimaryKey var newsId: Int = 0,
    var title: String = "",
    var releaseStartAt: String = "",
    var releaseEndAt: String? = null,
    var isRelease: Int = 0,
    var listImage: String? = null,
    var isRead: Int = 0
): RealmObject()