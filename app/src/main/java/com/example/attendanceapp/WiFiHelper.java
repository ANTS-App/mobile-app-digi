package com.example.attendanceapp;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.util.List;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;

public class WiFiHelper {
    private Context context;
    private static final String TAG = "WiFiHelper";

    public WiFiHelper(Context context) {
        this.context = context;
    }

    public void verifyAttendance(Function1<Boolean, Unit> callback, String expectedSSID) {
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager != null) {
            // Initiate a WiFi scan to update available networks
            wifiManager.startScan();
            // Retrieve the list of available networks
            List<ScanResult> scanResults = wifiManager.getScanResults();
            boolean found = false;
            if (scanResults != null) {
                for (ScanResult result : scanResults) {
                    String ssid = result.SSID;
                    // Remove quotes if present
                    if (ssid.startsWith("\"") && ssid.endsWith("\"")) {
                        ssid = ssid.substring(1, ssid.length() - 1);
                    }
                    Log.d(TAG, "Found SSID: " + ssid);
                    if (ssid.equals(expectedSSID)) {
                        found = true;
                        break;
                    }
                }
            } else {
                Log.e(TAG, "No scan results available");
            }
            callback.invoke(found);
        } else {
            Log.e(TAG, "WifiManager is null");
            callback.invoke(false);
        }
    }
}
