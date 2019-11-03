package com.elyonut.wow.alerts

import android.app.*
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import androidx.core.app.NotificationCompat
import com.elyonut.wow.view.MainActivity
import android.content.Intent
import com.elyonut.wow.R
import android.graphics.*
import android.provider.Settings
import com.elyonut.wow.Constants
import com.elyonut.wow.NotificationReceiver
import com.elyonut.wow.viewModel.MapViewModel
import kotlin.random.Random


class AlertsManager(var context: Context) {

    private var mNotifyManager: NotificationManager? = null
    private var notificationIds = HashMap<String, Int>()
//    val notificationReceiver = NotificationReceiver()

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        mNotifyManager = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager?
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {

            // Create a NotificationChannel
            val notificationChannel = NotificationChannel(
                Constants.PRIMARY_CHANNEL_ID,
                "Notification", NotificationManager
                    .IMPORTANCE_HIGH
            )

            notificationChannel.enableLights(true)
            notificationChannel.lightColor = Color.RED
            notificationChannel.enableVibration(true)
//            notificationChannel.description = "Notification from Mascot"
            mNotifyManager?.createNotificationChannel(notificationChannel)
        }
    }

    fun sendNotification(
        notificationTitle: String,
        notificationText: String,
        notificationIcon: Int,
        threatID: String
    ) {
        var notificationID = notificationIds[threatID]
        if (notificationID == null) {
            notificationID = generateNotificationID(threatID)
        }

        val notificationIntent = Intent(context, MainActivity::class.java)
        val notificationPendingIntent = PendingIntent.getActivity(
            context,
            notificationID, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT
        )

        val zoomLocationIntent = Intent("ZOOM_LOCATION").apply {

            putExtra("threatID", threatID)
        }

        val zoomLocationPendingIntent: PendingIntent =
            PendingIntent.getBroadcast(context, notificationID, zoomLocationIntent, 0)

        val notifyBuilder = NotificationCompat.Builder(context, Constants.PRIMARY_CHANNEL_ID)
            .setContentTitle(notificationTitle)
            .setContentText(notificationText)
            .setSmallIcon(notificationIcon)
            .setContentIntent(notificationPendingIntent)
            .setAutoCancel(true)
            .addAction(
                R.drawable.ic_check_black,
                context.getString(R.string.notification_accepted),
                notificationPendingIntent
            )
            .addAction(
                R.drawable.ic_gps_fixed_black,
                context.getString(R.string.goto_location),
                zoomLocationPendingIntent
            )
            .setLargeIcon(
                getCircleBitmap(
                    BitmapFactory.decodeResource(
                        context.resources,
                        R.drawable.sunflower
                    )
                )
            )

        notifyBuilder.build().flags.and(Notification.FLAG_AUTO_CANCEL)

        mNotifyManager?.notify(notificationID, notifyBuilder.build())
    }

    private fun getCircleBitmap(bitmap: Bitmap): Bitmap {
        val output = Bitmap.createBitmap(
            bitmap.width,
            bitmap.height, Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(output)

        val color = Color.RED
        val paint = Paint()
        val rect = Rect(0, 0, bitmap.width, bitmap.height)
        val rectF = RectF(rect)

        paint.isAntiAlias = true
        canvas.drawARGB(0, 0, 0, 0)
        paint.color = color
        canvas.drawOval(rectF, paint)

        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(bitmap, rect, rect, paint)

        bitmap.recycle()

        return output
    }

    private fun generateNotificationID(threatID: String): Int {
        var newID = Random.nextInt()
        while (notificationIds.containsValue(newID)) {
            newID = Random.nextInt()
        }

        notificationIds[threatID] = newID
        return newID
    }
}