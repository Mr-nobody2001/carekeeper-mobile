package com.example.carekeeper.service;

public class PanicStateService {
    public static boolean triggered = false;
    public static float progress = 0f;
    public static double currentLatitude = 0.0;
    public static double currentLongitude = 0.0;

    public static void reset() {
        triggered = false;
        progress = 0f;
        currentLatitude = 0.0;
        currentLongitude = 0.0;
    }
}
