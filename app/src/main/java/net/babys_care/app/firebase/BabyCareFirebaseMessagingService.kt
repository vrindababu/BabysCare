package net.babys_care.app.firebase

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import net.babys_care.app.AppSetting
import net.babys_care.app.R
import net.babys_care.app.scene.MainActivity
import net.babys_care.app.utils.NotificationBootReceiver
import net.babys_care.app.utils.debugLogInfo
import java.text.SimpleDateFormat
import java.util.*

class BabyCareFirebaseMessagingService: FirebaseMessagingService() {

    override fun onNewToken(p0: String) {
        debugLogInfo("FCM Token: $p0")
        AppSetting(this).fcmToken = p0
    }

    override fun onMessageReceived(p0: RemoteMessage) {
        debugLogInfo("Notification: ${p0.from}, ${p0.notification}")
        if (p0.data.isNotEmpty()) {
            if (!isNotificationScheduled(p0)) {
                sendNotification(this, p0)
            }
        }
    }

    private fun isNotificationScheduled(p0: RemoteMessage): Boolean {
        var isScheduled = false
        val notifiedAt = p0.data["notified_at"]
        if (notifiedAt != null) {
            val notificationTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.JAPAN).parse(notifiedAt)
            val now = Calendar.getInstance()
            if (notificationTime?.after(now.time) == true) {
                val alarmManager = getSystemService(Context.ALARM_SERVICE) as? AlarmManager
                if (alarmManager != null) {
                    val intent = Intent(this, NotificationBootReceiver::class.java)
                    val pendingIntent = PendingIntent.getBroadcast(this, 0, intent,
                        PendingIntent.FLAG_UPDATE_CURRENT)

                    if (pendingIntent != null) {
                        alarmManager.cancel(pendingIntent)
                    }

                    now.time = notificationTime

                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, now.timeInMillis, pendingIntent)
                    AppSetting(this).notificationMessage = p0.data["message"]
                    isScheduled = true
                }
            }
        }
        
        return isScheduled
    }

    private fun sendNotification(context: Context, p0: RemoteMessage) {
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
        val builder = getNotificationBuilder(context, channelId, notificationId)
        builder.setContentTitle(p0.data["title"] ?: context.getString(R.string.app_name))
        builder.setContentText(p0.data["message"] ?: context.getString(R.string.app_name))
        notificationManager.notify(notificationId, builder.build())
    }

    private fun getNotificationBuilder(context: Context, channelId: String, notificationId: Int): NotificationCompat.Builder {
        val contentIntent = Intent(context, MainActivity::class.java)
        contentIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        val pendingIntent = PendingIntent.getActivity(context, notificationId, contentIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        return NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notifications_24)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
    }
}