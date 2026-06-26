package com.example.ui

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("QUEST_TITLE") ?: "Quest Reminder"
        val desc = intent.getStringExtra("QUEST_DESC") ?: "It's time to complete your quest!"
        val questId = intent.getIntExtra("QUEST_ID", 0)
        
        showNotification(context, title, desc, questId)
    }

    private fun showNotification(context: Context, title: String, content: String, questId: Int) {
        val channelId = "quest_reminders"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Quest Reminders", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }
        
        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(context, questId, mainIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val builder = NotificationCompat.Builder(context, channelId)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        val resId = context.resources.getIdentifier("ic_launcher_foreground", "drawable", context.packageName)
        if (resId != 0) {
            builder.setSmallIcon(resId)
        } else {
            builder.setSmallIcon(android.R.drawable.ic_dialog_info)
        }

        notificationManager.notify(questId, builder.build())
    }
}
