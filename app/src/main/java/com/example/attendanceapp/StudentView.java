package com.example.attendanceapp;

import static com.example.attendanceapp.utilities.ChildNameGenerator.getDynamicChildName;

import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.attendanceapp.R;
import com.example.attendanceapp.utilities.StatusBarUtils;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class StudentView extends AppCompatActivity {

    TextView o1, o2, o3, o4, o5, timeTV, nameTV, dateTV;
    private DatabaseReference databaseReference, databaseReference1;
    private LinearProgressIndicator progressBar;
    private Button myButton;
    private CountDownTimer countDownTimer;

    private static final int TOTAL_TIME = 60000; // 60 seconds in milliseconds
    private static final int INTERVAL = 1000;    // Update every second

    private String uid = "S12345";
    private long teacherNumber;
    private long selectedCode = -1; // Default unselected

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_view);

        StatusBarUtils.customizeStatusBar(this, R.color.white, true);

        progressBar = findViewById(R.id.progressBar);
        timeTV = findViewById(R.id.timeTextView);
        progressBar.setMax(60); // 60 seconds total progress

        o1 = findViewById(R.id.t1);
        o2 = findViewById(R.id.t2);
        o3 = findViewById(R.id.t3);
        o4 = findViewById(R.id.t4);
        o5 = findViewById(R.id.t0);

        nameTV = findViewById(R.id.nametv);
        dateTV = findViewById(R.id.datetv);
        myButton = findViewById(R.id.markAttendanceButton);

        displayCurrentDate();
        startProgress();

        String childName = getDynamicChildName(); // Get dynamic child name
//        databaseReference = FirebaseDatabase.getInstance().getReference("attendance_sessions").child(childName); \\ For REAL TIME, AS SOON AS THE WEBAPP CREATES THE CHILD THEN IN THAT SAME MIN ANDROID APP ALSO CREATES THE CHILD
        databaseReference = FirebaseDatabase.getInstance().getReference("attendance_sessions").child("202503240717");
        databaseReference1 = FirebaseDatabase.getInstance().getReference("students").child(uid);

        // Retrieve numbers from Firebase
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    List<Long> otherNumbers = (List<Long>) snapshot.child("other_numbers").getValue();
                    if (otherNumbers != null) {
                        if (otherNumbers.size() > 0) o1.setText(String.valueOf(otherNumbers.get(0)));
                        if (otherNumbers.size() > 1) o2.setText(String.valueOf(otherNumbers.get(1)));
                        if (otherNumbers.size() > 2) o3.setText(String.valueOf(otherNumbers.get(2)));
                        if (otherNumbers.size() > 3) o4.setText(String.valueOf(otherNumbers.get(3)));
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Firebase", "Database error: " + error.getMessage());
            }
        });

        // Retrieve teacher_number from Firebase
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    teacherNumber = snapshot.child("teacher_number").getValue(Long.class);
                    o5.setText(String.valueOf(teacherNumber));
                } else {
                    o5.setText("N/A");
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Firebase", "Database error: " + error.getMessage());
            }
        });

        // Retrieve student name from Firebase
        databaseReference1.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String name = snapshot.child("name").getValue(String.class);
                    nameTV.setText(name);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        // Set click listeners for text views
        setNumberSelection(o5);
        setNumberSelection(o1);
        setNumberSelection(o2);
        setNumberSelection(o3);
        setNumberSelection(o4);

        myButton.setOnClickListener(view -> markAttendance());
    }

    private void setNumberSelection(TextView textView) {
        textView.setOnClickListener(view -> {
            resetSelection(); // Unselect previous selection
            textView.setBackgroundColor(getColor(R.color.lightGreen)); // Highlight selected
            selectedCode = Long.parseLong(textView.getText().toString()); // Store selected code
        });
    }

    private void resetSelection() {
        o1.setBackgroundColor(Color.TRANSPARENT);
        o2.setBackgroundColor(Color.TRANSPARENT);
        o3.setBackgroundColor(Color.TRANSPARENT);
        o4.setBackgroundColor(Color.TRANSPARENT);
        o5.setBackgroundColor(Color.TRANSPARENT);
    }

    private void markAttendance() {
        if (selectedCode == teacherNumber) {
            String childName = getDynamicChildName();
            DatabaseReference attnRef = FirebaseDatabase.getInstance().getReference("attendance_sessions")
                    .child("202503240710").child("attn_marked").child(uid);

            attnRef.setValue("marked").addOnSuccessListener(aVoid -> {
                myButton.setText("Attendance Marked");
                myButton.setEnabled(false);
                myButton.setBackgroundColor(Color.GRAY);
                stopProgress(); // Stop countdown and progress bar
            }).addOnFailureListener(e -> Log.e("Firebase", "Error marking attendance", e));
        } else {
            myButton.setText("Wrong Code Selected");
            myButton.setEnabled(false);
            myButton.setBackgroundColor(Color.GRAY);
            stopProgress();
        }
    }

    private void startProgress() {
        countDownTimer = new CountDownTimer(TOTAL_TIME, INTERVAL) {
            public void onTick(long millisUntilFinished) {
                int secondsRemaining = (int) (millisUntilFinished / 1000);
                timeTV.setText(secondsRemaining + "s");
                progressBar.setProgress(secondsRemaining);
            }

            public void onFinish() {
                timeTV.setText("0s");
                progressBar.setProgress(0);
                myButton.setEnabled(false);
                myButton.setBackgroundColor(Color.GRAY);
            }
        }.start();
    }

    private void stopProgress() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        timeTV.setText("Attendance Marked");
        progressBar.setProgress(0);
    }


//    val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
//    pairedDevices?.forEach { device ->
//            val deviceName = device.name
//        val deviceHardwareAddress = device.address // MAC address
//    }

    private void displayCurrentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault());
        String currentDate = sdf.format(Calendar.getInstance().getTime());
        dateTV.setText(currentDate);
    }
}
