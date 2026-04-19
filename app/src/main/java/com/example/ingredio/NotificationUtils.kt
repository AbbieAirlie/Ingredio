package com.example.ingredio

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.ingredio.data.model.Ingredient
import java.util.Calendar

object NotificationUtils {
    fun scheduleExpirationNotification(context: Context, ingredient: Ingredient) {
        val expiryDate = ingredient.expiryDate ?: return
        
        val sharedPref = context.getSharedPreferences("IngredioSettings", Context.MODE_PRIVATE)
        val daysBefore = sharedPref.getInt("notification_days_before", 1)
        
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        val intent = Intent(context, ExpirationReceiver::class.java).apply {
            putExtra("ingredientName", ingredient.name)
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ingredient.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Calculate trigger time: expiryDate - (daysBefore in milliseconds)
        val calendar = Calendar.getInstance().apply {
            timeInMillis = expiryDate
            add(Calendar.DAY_OF_YEAR, -daysBefore)
            // Set it to trigger at 9:00 AM on that day
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }

        var triggerTime = calendar.timeInMillis
        
        // If the calculated time is in the past, schedule it for 5 seconds from now for immediate feedback
        if (triggerTime < System.currentTimeMillis()) {
            triggerTime = System.currentTimeMillis() + 5000
        }

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerTime,
            pendingIntent
        )
    }
}