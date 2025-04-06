package com.example.attendanceapp.Views;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.attendanceapp.LoginActivity;
import com.example.attendanceapp.R;
import com.example.attendanceapp.StudentView;
import com.example.attendanceapp.utilities.StatusBarUtils;

public class SplashScreen extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        StatusBarUtils.customizeStatusBar(this,R.color.white,true);

        // Check if this is the first creation of the activity
        if (savedInstanceState == null) {
            int SPLASH_DISPLAY_LENGTH = 1000;
            new Handler().postDelayed(() -> {
                startActivity(new Intent(SplashScreen.this, LoginActivity.class));
                finish();
            }, SPLASH_DISPLAY_LENGTH);
        }

    }
}