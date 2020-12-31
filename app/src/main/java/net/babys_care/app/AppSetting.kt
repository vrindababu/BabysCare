package net.babys_care.app

import android.content.Context
import jp.winas.android.foundation.storage.StoredMap

class AppSetting(context: Context): StoredMap(context) {
    var fcmToken: String? by storedMap()
    var babyInfoChanged: Boolean? by storedMap()
    var userInfoUpdated: Boolean? by storedMap()
    var notificationMessage: String? by storedMap()
}