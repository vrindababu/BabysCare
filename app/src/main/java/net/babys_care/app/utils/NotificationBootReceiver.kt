package net.babys_care.app.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import io.realm.Realm
import io.realm.kotlin.where
import net.babys_care.app.AppSetting
import net.babys_care.app.R
import net.babys_care.app.models.realmmodels.GrowthHistories
import net.babys_care.app.models.realmmodels.UserModel
import net.babys_care.app.scene.MainActivity
import java.util.*

class NotificationBootReceiver : BroadcastReceiver(){

    override fun onReceive(context: Context?, intent: Intent?) {
        // Set the notification here.
        context?.let {
            if (AppSetting(it).notificationMessage != null) {
                sendNotification(it, AppSetting(it).notificationMessage)
                AppSetting(it).notificationMessage = null
            } else if (isLocalNotificationOn() && !isTodayHeightWeightExists()) {
                sendNotification(it)
            }
        }
    }

    private fun isTodayHeightWeightExists(): Boolean {
        val realm = Realm.getDefaultInstance()
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val date1 = Date(calendar.timeInMillis)
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        val date2 = Date(calendar.timeInMillis)
        val growthHistory = realm.where<GrowthHistories>()
            .greaterThanOrEqualTo("measuredAt", date1)
            .lessThan("measuredAt", date2)
            .findFirst()
        realm.close()
        debugLogInfo("History: $growthHistory")

        return growthHistory != null
    }

    private fun isLocalNotificationOn(): Boolean {
        val realm = Realm.getDefaultInstance()
        val user = realm.where<UserModel>().findFirst()
        if (user != null && user.isNotifiableLocal == 1) {
            return true
        }

        return false
    }

    private fun sendNotification(context: Context, message: String? = null) {
        val notificationManager: NotificationManager = context.getSystemService(AppCompatActivity.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = context.packageName
        val notificationId = System.currentTimeMillis().toInt()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(channelId, "Notification", NotificationManager.IMPORTANCE_HIGH)
            notificationChannel.enableLights(true)
            notificationChannel.lightColor = Color.RED
            notificationChannel.enableVibration(true)
            notificationChannel.description = "Notification description"
            notificationManager.createNotificationChannel(notificationChannel)
        }
        val builder = getNotificationBuilder(context, channelId, notificationId, message)
        notificationManager.notify(notificationId, builder.build())
    }

    private fun getNotificationBuilder(context: Context, channelId: String, notificationId: Int, message: String? = null): NotificationCompat.Builder {
        val contentIntent = Intent(context, MainActivity::class.java)
        contentIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        val pendingIntent = PendingIntent.getActivity(context, notificationId, contentIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        return NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notifications_24)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(message ?: context.getString(R.string.add_baby_height_weight))
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
    }
}