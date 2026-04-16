package com.example.ingredio;

import android.app.Application;
import androidx.appcompat.app.AppCompatDelegate;

public class IngredioApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Force Light Mode across the entire application
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
    }
}