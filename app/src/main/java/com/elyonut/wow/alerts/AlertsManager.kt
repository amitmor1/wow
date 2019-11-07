package com.elyonut.wow.alerts

import android.app.*
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import androidx.core.app.NotificationCompat
import com.elyonut.wow.view.MainActivity
import android.content.Intent
import com.elyonut.wow.R
import android.graphics.*
import com.elyonut.wow.Constants
import kotlin.random.Random


class AlertsManager(var context: Context) {

    private var notificationManager: NotificationManager? = null
    private var notificationIds = HashMap<String, Int>()

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        notificationManager = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager?
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
            notificationManager?.createNotificationChannel(notificationChannel)
        }
    }

    fun cancelNotification(notificationID: Int) {
        notificationManager?.cancel(notificationID)
    }

    fun sendNotification(
        notificationTitle: String,
        notificationText: String,
        notificationIcon: Int,
        threatID: String
    ) {
        val notificationID = getNotificationID(threatID)
        val notificationPendingIntent = createMainPendingIntent(notificationID)
        val zoomLocationPendingIntent = createActionPendingIntent(notificationID, threatID, Constants.ZOOM_LOCATION_ACTION)
        val alertAcceptedPendingIntent = createActionPendingIntent(notificationID, threatID, Constants.ALERT_ACCEPTED_ACTION)

        val notifyBuilder = NotificationCompat.Builder(context, Constants.PRIMARY_CHANNEL_ID)
            .setContentTitle(notificationTitle)
            .setContentText(notificationText)
            .setSmallIcon(notificationIcon)
            .setContentIntent(notificationPendingIntent)
            .setAutoCancel(true)
            .addAction(
                R.drawable.ic_check_black,
                context.getString(R.string.notification_accepted),
                alertAcceptedPendingIntent
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
        notificationManager?.notify(notificationID, notifyBuilder.build())
    }

    private fun getNotificationID(threatID: String): Int {
        var notificationID = notificationIds[threatID]

        if (notificationID == null) {
            notificationID = generateNotificationID(threatID)
        }

        return notificationID
    }

    private fun createMainPendingIntent(notificationID: Int): PendingIntent {
        val notificationIntent = Intent(context, MainActivity::class.java)

        return PendingIntent.getActivity(
            context,
            notificationID, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun createActionPendingIntent(notificationID: Int, threatID: String, action: String): PendingIntent {
        val actionIntent = Intent(action).apply {
            putExtra("threatID", threatID)
            putExtra("notificationID", notificationID)
        }

        return PendingIntent.getBroadcast(context, notificationID, actionIntent, 0)
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