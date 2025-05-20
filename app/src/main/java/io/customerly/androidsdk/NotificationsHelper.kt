package io.customerly.androidsdk

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat

class NotificationsHelper(context: Context) {
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val channelId = "customerly_messages"
    private val notificationPermissionRequestCode = 1001

    init {
        createNotificationChannel()
        checkNotificationPermission()
    }

    fun requestNotificationPermissionIfNeeded(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return
        }

        val permission = android.Manifest.permission.POST_NOTIFICATIONS
        if (context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED) {
            return
        }

        if (context is Activity) {
            context.requestPermissions(
                arrayOf(permission), notificationPermissionRequestCode
            )
        } else {
            Log.w(
                "CustomerlySDK",
                "Cannot request notification permission: context is not an Activity"
            )
        }
    }

    fun showNotification(
        context: Context, message: String, notificationId: Int, conversationId: Int
    ) {
        val abstractedMessage = abstractify(message)

        val intent = Intent(context, MessengerActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MessengerActivity.ExtraKey.CONVERSATION_ID.name, conversationId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_email).setContentText(abstractedMessage)
            .setPriority(NotificationCompat.PRIORITY_HIGH).setAutoCancel(true)
            .setContentIntent(pendingIntent).build()

        notificationManager.notify(notificationId, notification)
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!notificationManager.areNotificationsEnabled()) {
                Log.w("CustomerlySDK", "Notifications are not enabled for this app")
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Customer Support"
            val descriptionText = "Get notified when you receive responses from customer support"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
            }
            notificationManager.createNotificationChannel(channel)
        } else {
            Log.w(
                "CustomerlySDK",
                "Notification channel not created: API level ${Build.VERSION.SDK_INT} is below Oreo"
            )
        }
    }

    private fun abstractify(html: String?): String {
        if (html.isNullOrEmpty()) {
            return "📎 Attachment"
        }

        // Decode HTML entities
        val decodedHtml =
            android.text.Html.fromHtml(html, android.text.Html.FROM_HTML_MODE_LEGACY).toString()
                .replace(Regex("\\s+"), " ") // Replace multiple spaces with single space
                .trim()

        if (decodedHtml.isEmpty()) {
            return "🖼 Image"
        }

        return if (decodedHtml.length > 100) {
            "${decodedHtml.substring(0, 100)}..."
        } else {
            decodedHtml
        }
    }
}