package com.example.attendanceapp.utilities;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class ChildNameGenerator {
    public static String getDynamicChildName() {
        // Format: YYYYMMDDHH (Year, Month, Day, Hour)
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHH", Locale.getDefault());
        return sdf.format(Calendar.getInstance().getTime());
    }

    public static void main(String[] args) {
        // Example Usage
        String childName = getDynamicChildName();
        System.out.println("Generated Child Name: " + childName);
    }
}

