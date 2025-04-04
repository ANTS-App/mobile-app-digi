//package com.example.attendanceapp;
//
//import static com.example.attendanceapp.utilities.ChildNameGenerator.getDynamicChildName;
//
//import android.graphics.Color;
//import android.os.Bundle;
//import android.os.CountDownTimer;
//import android.util.Log;
//import android.view.View;
//import android.widget.Button;
//import android.widget.TextView;
//
//import androidx.annotation.NonNull;
//import androidx.appcompat.app.AppCompatActivity;
//
//import com.example.attendanceapp.R;
//import com.example.attendanceapp.utilities.StatusBarUtils;
//import com.google.android.material.card.MaterialCardView;
//import com.google.android.material.progressindicator.LinearProgressIndicator;
//import com.google.firebase.database.DataSnapshot;
//import com.google.firebase.database.DatabaseError;
//import com.google.firebase.database.DatabaseReference;
//import com.google.firebase.database.FirebaseDatabase;
//import com.google.firebase.database.ValueEventListener;
//
//import java.text.SimpleDateFormat;
//import java.util.Calendar;
//import java.util.List;
//import java.util.Locale;
//
//public class StudentView extends AppCompatActivity {
//
//    TextView o1, o2, o3, o4, o5, timeTV, nameTV, dateTV;
//    private DatabaseReference databaseReference, databaseReference1;
//    private LinearProgressIndicator progressBar;
//    private Button myButton;
//    private CountDownTimer countDownTimer;
//
//    private static final int TOTAL_TIME = 60000; // 60 seconds in milliseconds
//    private static final int INTERVAL = 1000;    // Update every second
//
//    private String uid = "S12345";
//    private long teacherNumber;
//    private long selectedCode = -1; // Default unselected
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_student_view);
//
//        StatusBarUtils.customizeStatusBar(this, R.color.white, true);
//
//        progressBar = findViewById(R.id.progressBar);
//        timeTV = findViewById(R.id.timeTextView);
//        progressBar.setMax(60); // 60 seconds total progress
//
//        o1 = findViewById(R.id.t1);
//        o2 = findViewById(R.id.t2);
//        o3 = findViewById(R.id.t3);
//        o4 = findViewById(R.id.t4);
//        o5 = findViewById(R.id.t0);
//
//        nameTV = findViewById(R.id.nametv);
//        dateTV = findViewById(R.id.datetv);
//        myButton = findViewById(R.id.markAttendanceButton);
//
//        displayCurrentDate();
//        startProgress();
//
//        String childName = getDynamicChildName(); // Get dynamic child name
////        databaseReference = FirebaseDatabase.getInstance().getReference("attendance_sessions").child(childName); \\ For REAL TIME, AS SOON AS THE WEBAPP CREATES THE CHILD THEN IN THAT SAME MIN ANDROID APP ALSO CREATES THE CHILD
//        databaseReference = FirebaseDatabase.getInstance().getReference("attendance_sessions").child("202503240717");
//        databaseReference1 = FirebaseDatabase.getInstance().getReference("students").child(uid);
//
//        // Retrieve numbers from Firebase
//        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot snapshot) {
//                if (snapshot.exists()) {
//                    List<Long> otherNumbers = (List<Long>) snapshot.child("other_numbers").getValue();
//                    if (otherNumbers != null) {
//                        if (otherNumbers.size() > 0) o1.setText(String.valueOf(otherNumbers.get(0)));
//                        if (otherNumbers.size() > 1) o2.setText(String.valueOf(otherNumbers.get(1)));
//                        if (otherNumbers.size() > 2) o3.setText(String.valueOf(otherNumbers.get(2)));
//                        if (otherNumbers.size() > 3) o4.setText(String.valueOf(otherNumbers.get(3)));
//                    }
//                }
//            }
//            @Override
//            public void onCancelled(@NonNull DatabaseError error) {
//                Log.e("Firebase", "Database error: " + error.getMessage());
//            }
//        });
//
//        // Retrieve teacher_number from Firebase
//        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot snapshot) {
//                if (snapshot.exists()) {
//                    teacherNumber = snapshot.child("teacher_number").getValue(Long.class);
//                    o5.setText(String.valueOf(teacherNumber));
//                } else {
//                    o5.setText("N/A");
//                }
//            }
//            @Override
//            public void onCancelled(@NonNull DatabaseError error) {
//                Log.e("Firebase", "Database error: " + error.getMessage());
//            }
//        });
//
//        // Retrieve student name from Firebase
//        databaseReference1.addListenerForSingleValueEvent(new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot snapshot) {
//                if (snapshot.exists()) {
//                    String name = snapshot.child("name").getValue(String.class);
//                    nameTV.setText(name);
//                }
//            }
//            @Override
//            public void onCancelled(@NonNull DatabaseError error) {}
//        });
//
//        // Set click listeners for text views
//        setNumberSelection(o5);
//        setNumberSelection(o1);
//        setNumberSelection(o2);
//        setNumberSelection(o3);
//        setNumberSelection(o4);
//
//        myButton.setOnClickListener(view -> markAttendance());
//    }
//
//    private void setNumberSelection(TextView textView) {
//        textView.setOnClickListener(view -> {
//            resetSelection(); // Unselect previous selection
//            textView.setBackgroundColor(getColor(R.color.lightGreen)); // Highlight selected
//            selectedCode = Long.parseLong(textView.getText().toString()); // Store selected code
//        });
//    }
//
//    private void resetSelection() {
//        o1.setBackgroundColor(Color.TRANSPARENT);
//        o2.setBackgroundColor(Color.TRANSPARENT);
//        o3.setBackgroundColor(Color.TRANSPARENT);
//        o4.setBackgroundColor(Color.TRANSPARENT);
//        o5.setBackgroundColor(Color.TRANSPARENT);
//    }
//
//    private void markAttendance() {
//        if (selectedCode == teacherNumber) {
//            String childName = getDynamicChildName();
//            DatabaseReference attnRef = FirebaseDatabase.getInstance().getReference("attendance_sessions")
//                    .child("202503240710").child("attn_marked").child(uid);
//
//            attnRef.setValue("marked").addOnSuccessListener(aVoid -> {
//                myButton.setText("Attendance Marked");
//                myButton.setEnabled(false);
//                myButton.setBackgroundColor(Color.GRAY);
//                stopProgress(); // Stop countdown and progress bar
//            }).addOnFailureListener(e -> Log.e("Firebase", "Error marking attendance", e));
//        } else {
//            myButton.setText("Wrong Code Selected");
//            myButton.setEnabled(false);
//            myButton.setBackgroundColor(Color.GRAY);
//            stopProgress();
//        }
//    }
//
//    private void startProgress() {
//        countDownTimer = new CountDownTimer(TOTAL_TIME, INTERVAL) {
//            public void onTick(long millisUntilFinished) {
//                int secondsRemaining = (int) (millisUntilFinished / 1000);
//                timeTV.setText(secondsRemaining + "s");
//                progressBar.setProgress(secondsRemaining);
//            }
//
//            public void onFinish() {
//                timeTV.setText("0s");
//                progressBar.setProgress(0);
//                myButton.setEnabled(false);
//                myButton.setBackgroundColor(Color.GRAY);
//            }
//        }.start();
//    }
//
//    private void stopProgress() {
//        if (countDownTimer != null) {
//            countDownTimer.cancel();
//        }
//        timeTV.setText("Attendance Marked");
//        progressBar.setProgress(0);
//    }
//
//
////    val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
////    pairedDevices?.forEach { device ->
////            val deviceName = device.name
////        val deviceHardwareAddress = device.address // MAC address
////    }
//
//    private void displayCurrentDate() {
//        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault());
//        String currentDate = sdf.format(Calendar.getInstance().getTime());
//        dateTV.setText(currentDate);
//    }
//}

package com.example.attendanceapp;

import static com.example.attendanceapp.utilities.ChildNameGenerator.getDynamicChildName;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

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

import kotlin.Unit;
import kotlin.jvm.functions.Function1;

public class StudentView extends AppCompatActivity {
    private static final String TAG = "StudentView";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;

    TextView o1, o2, o3, o4, o5, timeTV, nameTV, dateTV;
    private DatabaseReference databaseReference, databaseReference1;
    private LinearProgressIndicator progressBar;
    private Button myButton;
    private CountDownTimer countDownTimer;
    private GeolocationHelper geolocationHelper;

    private static final int TOTAL_TIME = 60000; // 60 seconds in milliseconds
    private static final int INTERVAL = 1000;    // Update every second

    private String uid = "S12345";
    private long teacherNumber;
    private long selectedCode = -1; // Default unselected
    private String teacherId = "3"; // Teacher ID for geolocation verification

    private static final int BLUETOOTH_PERMISSION_REQUEST_CODE = 100;
    private BluetoothHelper bluetoothHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: Starting StudentView activity");
        setContentView(R.layout.activity_student_view);

        // Initialize GeolocationHelper
        geolocationHelper = new GeolocationHelper(this);
        Log.d(TAG, "onCreate: GeolocationHelper initialized");

        StatusBarUtils.customizeStatusBar(this, R.color.white, true);
        Log.d(TAG, "onCreate: Status bar customized");

        initializeViews();
        displayCurrentDate();
        startProgress();
        setupFirebaseListeners();
        setupClickListeners();

        requestPermissions();


        bluetoothHelper = new BluetoothHelper(this);

        if (bluetoothHelper.hasBluetoothPermissions()) {
            startBluetoothScan();
        } else {
            bluetoothHelper.requestBluetoothPermissions(this, BLUETOOTH_PERMISSION_REQUEST_CODE);
        }

        // Location permission will be requested only when needed
        Log.d(TAG, "onCreate: Activity setup complete");
    }

    private void initializeViews() {
        Log.d(TAG, "initializeViews: Initializing UI elements");

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

        Log.d(TAG, "initializeViews: All UI elements initialized");
    }

    private void setupFirebaseListeners() {
        Log.d(TAG, "setupFirebaseListeners: Setting up Firebase database references");

        String childName = getDynamicChildName(); // Get dynamic child name
        databaseReference = FirebaseDatabase.getInstance().getReference("attendance_sessions").child("202503240717");
        databaseReference1 = FirebaseDatabase.getInstance().getReference("students").child(uid);

        Log.d(TAG, "setupFirebaseListeners: Firebase references initialized");

        // Retrieve other numbers from Firebase
        Log.d(TAG, "setupFirebaseListeners: Retrieving other numbers from Firebase");
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Log.d(TAG, "onDataChange: Other numbers snapshot received");
                if (snapshot.exists()) {
                    List<Long> otherNumbers = (List<Long>) snapshot.child("other_numbers").getValue();
                    if (otherNumbers != null) {
                        Log.d(TAG, "onDataChange: Found " + otherNumbers.size() + " other numbers");
                        if (otherNumbers.size() > 0)
                            o1.setText(String.valueOf(otherNumbers.get(0)));
                        if (otherNumbers.size() > 1)
                            o2.setText(String.valueOf(otherNumbers.get(1)));
                        if (otherNumbers.size() > 2)
                            o3.setText(String.valueOf(otherNumbers.get(2)));
                        if (otherNumbers.size() > 3)
                            o4.setText(String.valueOf(otherNumbers.get(3)));
                    } else {
                        Log.w(TAG, "onDataChange: Other numbers list is null");
                    }
                } else {
                    Log.w(TAG, "onDataChange: No data found for other numbers");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "onCancelled: Database error retrieving other numbers: " + error.getMessage());
            }
        });

        // Retrieve teacher_number from Firebase
        Log.d(TAG, "setupFirebaseListeners: Retrieving teacher number from Firebase");
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Log.d(TAG, "onDataChange: Teacher number snapshot received");
                if (snapshot.exists()) {
                    teacherNumber = snapshot.child("teacher_number").getValue(Long.class);
                    Log.d(TAG, "onDataChange: Teacher number retrieved: " + teacherNumber);
                    o5.setText(String.valueOf(teacherNumber));
                } else {
                    Log.w(TAG, "onDataChange: No data found for teacher number");
                    o5.setText("N/A");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "onCancelled: Database error retrieving teacher number: " + error.getMessage());
            }
        });

        // Retrieve student name from Firebase
        Log.d(TAG, "setupFirebaseListeners: Retrieving student name from Firebase");
        databaseReference1.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Log.d(TAG, "onDataChange: Student name snapshot received");
                if (snapshot.exists()) {
                    String name = snapshot.child("name").getValue(String.class);
                    Log.d(TAG, "onDataChange: Student name retrieved: " + name);
                    nameTV.setText(name);
                } else {
                    Log.w(TAG, "onDataChange: No data found for student name");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "onCancelled: Database error retrieving student name: " + error.getMessage());
            }
        });
    }

    private void setupClickListeners() {
        Log.d(TAG, "setupClickListeners: Setting up click listeners for number selection");

        // Set click listeners for text views
        setNumberSelection(o5);
        setNumberSelection(o1);
        setNumberSelection(o2);
        setNumberSelection(o3);
        setNumberSelection(o4);

        // Set click listener for attendance button
        myButton.setOnClickListener(view -> {
            Log.d(TAG, "onButtonClick: Mark attendance button clicked");
            handleAttendanceButtonClick();
        });

        Log.d(TAG, "setupClickListeners: All click listeners set up");
    }

    private void handleAttendanceButtonClick() {
        Log.d(TAG, "handleAttendanceButtonClick: Starting verification process");

        if (selectedCode == -1) {
            Log.w(TAG, "handleAttendanceButtonClick: No code selected");
            Toast.makeText(this, "Please select a code first", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedCode != teacherNumber) {
            Log.e(TAG, "handleAttendanceButtonClick: Wrong code selected: " + selectedCode + " vs teacher: " + teacherNumber);
            myButton.setText("Wrong Code Selected");
            myButton.setEnabled(false);
            myButton.setBackgroundColor(Color.GRAY);
            stopProgress();
            return;
        }

        // Check if we have location permission before proceeding
        if (!geolocationHelper.hasLocationPermission()) {
            Log.i(TAG, "handleAttendanceButtonClick: No location permission, requesting it");
            requestLocationPermission();
        } else {
            Log.d(TAG, "handleAttendanceButtonClick: Already have location permission, proceeding to verification");
            verifyLocationAndMarkAttendance();
        }
    }

    private void requestLocationPermission() {
        Log.d(TAG, "requestLocationPermission: Requesting location permission");

        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
            Log.d(TAG, "requestLocationPermission: Should show rationale before requesting");
            Toast.makeText(this,
                    "Location permission is required to verify you are near the teacher",
                    Toast.LENGTH_LONG).show();
        }

        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                LOCATION_PERMISSION_REQUEST_CODE
        );

        Log.d(TAG, "requestLocationPermission: Permission request initiated");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        Log.d(TAG, "onRequestPermissionsResult: Received results for request code: " + requestCode);

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "onRequestPermissionsResult: Location permission granted");
                Toast.makeText(this, "Location permission granted", Toast.LENGTH_SHORT).show();
                verifyLocationAndMarkAttendance();
            } else {
                Log.e(TAG, "onRequestPermissionsResult: Location permission denied");
                Toast.makeText(this,
                        "Location permission denied. Cannot verify attendance location.",
                        Toast.LENGTH_LONG).show();

                myButton.setText("Location Permission Required");
                myButton.setEnabled(true); // Keep enabled so they can try again
            }
        }

        if (requestCode == BLUETOOTH_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && allPermissionsGranted(grantResults)) {
                startBluetoothScan();
            } else {
                Toast.makeText(this, "Bluetooth permissions are required.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void verifyLocationAndMarkAttendance() {
        Log.d(TAG, "verifyLocationAndMarkAttendance: Starting location verification process");

        myButton.setText("Verifying Location...");
        myButton.setEnabled(false);

        // Call GeolocationHelper to verify proximity
        geolocationHelper.verifyAttendance(teacherId, new Function1<Boolean, Unit>() {
            @Override
            public Unit invoke(Boolean isWithinRange) {
                Log.d(TAG, "Location verification result: " + isWithinRange);

                runOnUiThread(() -> {
                    if (isWithinRange) {
                        // Location verified, now mark attendance
                        Log.d(TAG, "Location verified, marking attendance");
                        markAttendance();
                    } else {
                        // Not within range
                        Log.e(TAG, "Not in proximity of teacher");
                        myButton.setText("Not in proximity of teacher");
                        myButton.setEnabled(true); // Re-enable to let them try again
                        myButton.setBackgroundColor(getResources().getColor(R.color.lightGreen, null));
                        Toast.makeText(StudentView.this,
                                "You are not within 10 meters of the teacher. Cannot mark attendance.",
                                Toast.LENGTH_LONG).show();
                    }
                });

                return Unit.INSTANCE;
            }
        });
    }

    private void setNumberSelection(TextView textView) {
        textView.setOnClickListener(view -> {
            Log.d(TAG, "Number selected: " + textView.getText());
            resetSelection(); // Unselect previous selection
            textView.setBackgroundColor(getColor(R.color.lightGreen)); // Highlight selected
            selectedCode = Long.parseLong(textView.getText().toString()); // Store selected code
        });
    }

    private void resetSelection() {
        Log.d(TAG, "resetSelection: Clearing previous number selection");
        o1.setBackgroundColor(Color.TRANSPARENT);
        o2.setBackgroundColor(Color.TRANSPARENT);
        o3.setBackgroundColor(Color.TRANSPARENT);
        o4.setBackgroundColor(Color.TRANSPARENT);
        o5.setBackgroundColor(Color.TRANSPARENT);
    }

    private void markAttendance() {
        Log.d(TAG, "markAttendance: Recording attendance in Firebase");

        String childName = getDynamicChildName();
        DatabaseReference attnRef = FirebaseDatabase.getInstance().getReference("attendance_sessions")
                .child("202503240710").child("attn_marked").child(uid);

        attnRef.setValue("marked").addOnSuccessListener(aVoid -> {
            Log.d(TAG, "markAttendance: Successfully marked attendance in Firebase");
            myButton.setText("Attendance Marked");
            myButton.setEnabled(false);
            myButton.setBackgroundColor(Color.GRAY);
            stopProgress(); // Stop countdown and progress bar
            Toast.makeText(this, "Attendance marked successfully!", Toast.LENGTH_SHORT).show();
        }).addOnFailureListener(e -> {
            Log.e(TAG, "markAttendance: Error marking attendance", e);
            Toast.makeText(this, "Failed to mark attendance. Please try again.", Toast.LENGTH_SHORT).show();
            myButton.setText("Mark Attendance");
            myButton.setEnabled(true);
        });
    }

    private void startProgress() {
        Log.d(TAG, "startProgress: Starting countdown timer for 60 seconds");

        countDownTimer = new CountDownTimer(TOTAL_TIME, INTERVAL) {
            public void onTick(long millisUntilFinished) {
                int secondsRemaining = (int) (millisUntilFinished / 1000);
                timeTV.setText(secondsRemaining + "s");
                progressBar.setProgress(secondsRemaining);
            }

            public void onFinish() {
                Log.d(TAG, "Countdown timer finished");
                timeTV.setText("0s");
                progressBar.setProgress(0);
                myButton.setEnabled(false);
                myButton.setBackgroundColor(Color.GRAY);
                myButton.setText("Time Expired");
            }
        }.start();

        Log.d(TAG, "startProgress: Countdown timer started");
    }

    private void stopProgress() {
        Log.d(TAG, "stopProgress: Stopping countdown timer");

        if (countDownTimer != null) {
            countDownTimer.cancel();
            Log.d(TAG, "stopProgress: Countdown timer cancelled");
        }

        timeTV.setText("Attendance Marked");
        progressBar.setProgress(0);
    }

    private void displayCurrentDate() {
        Log.d(TAG, "displayCurrentDate: Setting current date in UI");

        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault());
        String currentDate = sdf.format(Calendar.getInstance().getTime());
        dateTV.setText(currentDate);

        Log.d(TAG, "displayCurrentDate: Current date set: " + currentDate);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause: Activity paused");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: Activity resumed");
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy: Activity being destroyed, cleaning up resources");

        // Clean up location updates
        if (geolocationHelper != null) {
            geolocationHelper.stopLocationUpdates();
            Log.d(TAG, "onDestroy: Location updates stopped");
        }

        // Cancel the timer if it's running
        if (countDownTimer != null) {
            countDownTimer.cancel();
            Log.d(TAG, "onDestroy: Countdown timer cancelled");
        }

        super.onDestroy();
    }

    private void requestPermissions() {

        ActivityResultLauncher<String[]> locationPermissionRequest =
                registerForActivityResult(new ActivityResultContracts
                                .RequestMultiplePermissions(), result -> {

                            Boolean fineLocationGranted = null;
                            Boolean coarseLocationGranted = null;

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                fineLocationGranted = result.getOrDefault(
                                        Manifest.permission.ACCESS_FINE_LOCATION, false);
                                coarseLocationGranted = result.getOrDefault(
                                        Manifest.permission.ACCESS_COARSE_LOCATION, false);
                            }

                            if (fineLocationGranted != null && fineLocationGranted) {
                                // Precise location access granted.
                            } else if (coarseLocationGranted != null && coarseLocationGranted) {
                                // Only approximate location access granted.
                            } else {
                                // No location access granted.
                            }
                        }
                );

        // ...

        // Before you perform the actual permission request, check whether your app
        // already has the permissions, and whether your app needs to show a permission
        // rationale dialog. For more details, see Request permissions.
        locationPermissionRequest.launch(new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        });
    }

    private boolean allPermissionsGranted(int[] grantResults) {
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private void startBluetoothScan() {
        bluetoothHelper.scanForTeacherBluetooth(isTeacherInRange -> {
            if (isTeacherInRange) {
                Toast.makeText(StudentView.this, "Teacher's device found!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(StudentView.this, "Teacher's device not found.", Toast.LENGTH_SHORT).show();
            }
            return null;
        });
    }
}
