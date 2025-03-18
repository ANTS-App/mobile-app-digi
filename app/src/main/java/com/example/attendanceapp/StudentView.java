package com.example.attendanceapp;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.attendanceapp.utilities.StatusBarUtils;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;

public class StudentView extends AppCompatActivity {

    TextView o1, o2, o3, o4, o5;
    MaterialCardView m1, m2, m3, m4;
    private DatabaseReference databaseReference;
    String TAG = "ad";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_student_view);
//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
//            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
//            return insets;
//        });

// Initialize TextViews
        StatusBarUtils.customizeStatusBar(this, R.color.white, true);

        o1 = findViewById(R.id.t0);
        o1 = findViewById(R.id.t1);
        o2 = findViewById(R.id.t2);
        o3 = findViewById(R.id.t3);
        o4 = findViewById(R.id.t4);

        // Get database reference
        databaseReference = FirebaseDatabase.getInstance().getReference("attendance_sessions").child("20250317173817");

        // Retrieve Data from Firebase
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    List<Long> otherNumbers = (List<Long>) snapshot.child("other_numbers").getValue();
                    if (otherNumbers != null && otherNumbers.size() > 0) {
                        // Set values to TextViews (max 4 numbers)
                        if (otherNumbers.size() > 0) o1.setText(String.valueOf(otherNumbers.get(0)));
                        if (otherNumbers.size() > 1) o2.setText(String.valueOf(otherNumbers.get(1)));
                        if (otherNumbers.size() > 2) o3.setText(String.valueOf(otherNumbers.get(2)));
                        if (otherNumbers.size() > 3) o4.setText(String.valueOf(otherNumbers.get(3)));
                    }
                } else {
                    Log.d(TAG, "onDataChange: ");

                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Firebase", "Database error: " + error.getMessage());
            }
        });
        // Retrieve teacher_number from Firebase
        databaseReference.child("teacher_number").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    long teacherNumber = snapshot.getValue(Long.class);
                    o1.setText(String.valueOf(teacherNumber)); // Display teacher_number
                } else {
                    o1.setText("N/A"); // Handle missing data
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Firebase", "Database error: " + error.getMessage());
            }
        });
    }
}