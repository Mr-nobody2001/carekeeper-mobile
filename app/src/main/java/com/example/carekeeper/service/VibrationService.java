package com.example.carekeeper.service;

import android.content.Context;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;

public class VibrationService {

    private final Vibrator vibrator;

    public VibrationService(Context context) {
        this.vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
    }

    /**
     * Inicia vibração contínua (loop) até chamar stopVibration()
     */
    public void startContinuousVibration() {
        if (vibrator == null) return;

        long[] pattern = {0, 500, 500}; // [delay, vibrar, pausa]
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0));
        } else {
            vibrator.vibrate(pattern, 0);
        }
    }

    /**
     * Para a vibração
     */
    public void stopVibration() {
        if (vibrator != null) {
            vibrator.cancel();
        }
    }
}
