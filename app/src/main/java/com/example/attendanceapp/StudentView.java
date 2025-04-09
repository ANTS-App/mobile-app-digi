package com.example.attendanceapp;

import static com.example.attendanceapp.utilities.ChildNameGenerator.getDynamicChildName;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Intent;
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
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.attendanceapp.Prevalent.Prevalent;
import com.example.attendanceapp.R;
import com.example.attendanceapp.utilities.StatusBarUtils;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import io.paperdb.Paper;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;

public class StudentView extends AppCompatActivity {
    private static final String TAG = "StudentView";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;
    private static final int REQUEST_ENABLE_BT = 0000;

    TextView o1, o2, o3, o4, o5, timeTV, nameTV, dateTV;
    private DatabaseReference databaseReference, databaseReference1, databaseReference0;
    private LinearProgressIndicator progressBar;
    private Button myButton;
    private CountDownTimer countDownTimer;
    private GeolocationHelper geolocationHelper;
    private String activeSessionNode;
    private String firebaseSSID; // Expected WiFi SSID from Firebase

    private static final int TOTAL_TIME = 60000; // 60 seconds in milliseconds
    private static final int INTERVAL = 1000;    // Update every second

    private String uid, rollNo;
    private long teacherNumber;
    private long selectedCode = -1; // Default unselected
    private String teacherId = "3"; // Teacher ID for location verification

    private static final int BLUETOOTH_PERMISSION_REQUEST_CODE = 100;
    private BluetoothHelper bluetoothHelper;

    private WiFiHelper wifiHelper; // New WiFi helper for SSID verification

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: Starting StudentView activity");
        setContentView(R.layout.activity_student_view);

        Paper.init(this);

        // Initialize GeolocationHelper (kept for location verification)
        geolocationHelper = new GeolocationHelper(this);
        Log.d(TAG, "onCreate: GeolocationHelper initialized");

        StatusBarUtils.customizeStatusBar(this, R.color.white, true);
        Log.d(TAG, "onCreate: Status bar customized");

        BluetoothManager bluetoothManager = getSystemService(BluetoothManager.class);
        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
        if (bluetoothAdapter == null) {
            // Device doesn't support Bluetooth
        }

        requestPermissions();

        bluetoothHelper = new BluetoothHelper(this);
        wifiHelper = new WiFiHelper(this); // Initialize WiFiHelper

        if (bluetoothHelper.hasBluetoothPermissions()) {

            if (!bluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
            initializeViews();
            displayCurrentDate();
            setupFirebaseListeners();
            startBluetoothScan();
            startProgress();
            setupClickListeners();

        } else {
            bluetoothHelper.requestBluetoothPermissions(this, BLUETOOTH_PERMISSION_REQUEST_CODE);
            initializeViews();
            displayCurrentDate();
            setupFirebaseListeners();
            noBluetoothClickListeners();
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
        Paper.init(this);

        String childName = getDynamicChildName(); // Get dynamic child name
        Log.d("dynamic", childName);

        uid = FirebaseAuth.getInstance().getUid();
        Log.d("uiduid", uid);
        databaseReference0 = FirebaseDatabase.getInstance().getReference("users").child("Students_uid").child(uid);

        databaseReference0.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()) {
                    rollNo = snapshot.child("roll_number").getValue(String.class);
                    Log.d("ROLLNO", rollNo);

                    databaseReference1 = FirebaseDatabase.getInstance().getReference("users").child("Students").child(rollNo);

                    // Retrieve student name from Firebase
                    Log.d(TAG, "setupFirebaseListeners: Retrieving student name from Firebase");
                    databaseReference1.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            Log.d(TAG, "onDataChange: Student name snapshot received");
                            if (snapshot.exists()) {
                                String name = snapshot.child("roll_number").getValue(String.class);
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
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });

        databaseReference = FirebaseDatabase.getInstance().getReference("attendance_sessions");

        Log.d(TAG, "setupFirebaseListeners: Firebase references initialized");

        // Retrieve session details from Firebase
        Log.d(TAG, "setupFirebaseListeners: Retrieving session details from Firebase");

        DatabaseReference sessionRef = FirebaseDatabase.getInstance().getReference("attendance_sessions");

        sessionRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // Loop through all sessions (child keys are timestamps)
                    for (DataSnapshot sessionSnapshot : snapshot.getChildren()) {
                        String status = sessionSnapshot.child("status").getValue(String.class);
                        if ("active".equals(status)) {
                            // Found the active session
                            Log.d(TAG, "Active session found: " + sessionSnapshot.getKey());

                            // Fetch teacher_number
                            Long teacherNum = sessionSnapshot.child("teacher_number").getValue(Long.class);
                            if (teacherNum != null) {
                                teacherNumber = teacherNum;
                                o5.setText(String.valueOf(teacherNumber));
                            } else {
                                o5.setText("N/A");
                            }

                            // Fetch other_numbers
                            List<Long> otherNumbers = (List<Long>) sessionSnapshot.child("other_numbers").getValue();
                            if (otherNumbers != null && !otherNumbers.isEmpty()) {
                                if (otherNumbers.size() > 0) o1.setText(String.valueOf(otherNumbers.get(0)));
                                if (otherNumbers.size() > 1) o2.setText(String.valueOf(otherNumbers.get(1)));
                                if (otherNumbers.size() > 2) o3.setText(String.valueOf(otherNumbers.get(2)));
                                if (otherNumbers.size() > 3) o4.setText(String.valueOf(otherNumbers.get(3)));
                            } else {
                                Log.w(TAG, "other_numbers is null or empty in the active session.");
                            }

                            String className = sessionSnapshot.child("class_name").getValue(String.class);
                            String day = sessionSnapshot.child("day").getValue(String.class);
                            String timeSlot = sessionSnapshot.child("time_slot").getValue(String.class);
                            activeSessionNode = sessionSnapshot.child("timestamp").getValue(String.class);

                            // Retrieve the expected WiFi SSID from Firebase
                            firebaseSSID = sessionSnapshot.child("ssid").getValue(String.class);
                            Log.d(TAG, "Active session WiFi SSID: " + firebaseSSID);

                            break;
                        }
                    }
                } else {
                    Log.w(TAG, "No sessions found under attendance_sessions");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Database error retrieving sessions: " + error.getMessage());
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

    private void noBluetoothClickListeners() {
        // Set click listener for attendance button
        myButton.setOnClickListener(view -> {
            Toast.makeText(this, "Please enable the bluetooth permission!", Toast.LENGTH_SHORT).show();
        });
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

    // Modified: First verify location; if location is verified, then verify WiFi.
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
                        Log.d(TAG, "Location verified, now verifying WiFi");
                        myButton.setText("Verifying WiFi...");
                        // Now check WiFi using WiFiHelper
                        wifiHelper.verifyAttendance(new Function1<Boolean, Unit>() {
                            @Override
                            public Unit invoke(Boolean isSSIDFound) {
                                Log.d(TAG, "WiFi verification result: " + isSSIDFound);
                                runOnUiThread(() -> {
                                    if (isSSIDFound) {
                                        Log.d(TAG, "WiFi verified, marking attendance");
                                        markAttendance();
                                    } else {
                                        Log.e(TAG, "Expected WiFi network not found");
                                        myButton.setText("Incorrect WiFi Network");
                                        myButton.setEnabled(true);
                                        myButton.setBackgroundColor(getResources().getColor(R.color.lightGreen, null));
                                        new AlertDialog.Builder(StudentView.this)
                                                .setTitle("Verification Failed")
                                                .setMessage("The expected WiFi network was not found among available networks. Please try again.")
                                                .setPositiveButton("OK", null)
                                                .show();
                                    }
                                });
                                return Unit.INSTANCE;
                            }
                        }, firebaseSSID);
                    } else {
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
            try {
                selectedCode = Long.parseLong(textView.getText().toString());
            } catch (NumberFormatException e) {
                Log.e(TAG, "Invalid number format for selected code: " + textView.getText(), e);
                Toast.makeText(this, "Invalid code selected. Please wait for it to load.", Toast.LENGTH_SHORT).show();
                selectedCode = -1; // Reset selection
                resetSelection();  // Remove highlight if invalid
            }
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

        Paper.init(this);

        String childName = getDynamicChildName();
        DatabaseReference attnRef = FirebaseDatabase.getInstance().getReference("attendance_sessions")
                .child(activeSessionNode).child("attn_marked").child(rollNo);

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
                registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                    Boolean fineLocationGranted = null;
                    Boolean coarseLocationGranted = null;

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        fineLocationGranted = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
                        coarseLocationGranted = result.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false);
                    }

                    if (fineLocationGranted != null && fineLocationGranted) {
                        // Precise location access granted.
                    } else if (coarseLocationGranted != null && coarseLocationGranted) {
                        // Only approximate location access granted.
                    } else {
                        // No location access granted.
                    }
                });

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
            } else {
            }
            return null;
        });
    }
}
