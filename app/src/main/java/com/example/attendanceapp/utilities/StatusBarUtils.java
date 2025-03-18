package com.example.attendanceapp.utilities;

import android.app.Activity;
import android.os.Build;
import android.view.View;
import android.view.Window;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;

public class StatusBarUtils {

    public static void customizeStatusBar(Activity activity, int statusBarColor, boolean isLightStatusBar) {
        customizeStatusBarColor(activity, statusBarColor);
        customizeStatusBarIcon(activity, isLightStatusBar);
        disableNightMode();
    }

    private static void customizeStatusBarColor(Activity activity, int statusBarColor) {
        Window window = activity.getWindow();
        window.setStatusBarColor(ContextCompat.getColor(activity, statusBarColor));
    }

    private static void customizeStatusBarIcon(Activity activity, boolean isLightStatusBar) {
        View decor = activity.getWindow().getDecorView();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int visibility = isLightStatusBar ? View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR : 0;
            decor.setSystemUiVisibility(visibility);
        }
    }

    private static void disableNightMode() {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
    }
}
