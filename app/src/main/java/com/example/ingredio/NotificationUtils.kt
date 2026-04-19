package com.example.ingredio

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.ingredio.data.model.Ingredient

object NotificationUtils {
    fun scheduleExpirationNotification(context: Context, ingredient: Ingredient) {
        val expiryDate = ingredient.expiryDate ?: return
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        val intent = Intent(context, ExpirationReceiver::class.java).apply {
            putExtra("ingredientName", ingredient.name)
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ingredient.id, // Use ingredient ID as request code to avoid overwriting
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // For demonstration/testing, we schedule it for the exact expiry time.
        // In a real app, you might want to schedule it 1 day before at a specific hour.
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            expiryDate,
            pendingIntent
        )
    }
}