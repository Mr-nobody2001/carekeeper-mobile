package com.example.carekeeper.ui.panic;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.example.carekeeper.R;
import com.example.carekeeper.dto.PanicAlertRequest;
import com.example.carekeeper.network.ApiClient;
import com.example.carekeeper.network.ApiService;
import com.example.carekeeper.service.SharedPreferencesService;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PanicButtonFragment extends Fragment {

    private static final long HOLD_DURATION_MS = 3000L;
    private static final long RIPPLE_INTERVAL_MS = 1000L;

    private CircularProgressView circularProgress;
    private AppCompatButton panicButton;
    private View rippleWave;

    private boolean triggered = false;
    private boolean isHolding = false;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable panicRunnable;
    private Runnable rippleLoopRunnable;

    private ValueAnimator progressAnimator;
    private ValueAnimator flashAnimator;
    private MediaPlayer alertSound;
    private ApiService api;
    private FusedLocationProviderClient fusedLocationClient;
    private SharedPreferencesService prefs;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_panic_button, container, false);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        circularProgress = view.findViewById(R.id.circularProgress);
        panicButton = view.findViewById(R.id.panicButton);
        rippleWave = view.findViewById(R.id.rippleWave);

        api = ApiClient.getClient().create(ApiService.class);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext());
        prefs = new SharedPreferencesService(requireContext());

        // 🔹 Carrega estado persistido
        triggered = prefs.isAlertActive();
        float savedProgress = prefs.getPanicProgress();

        if (triggered) {
            circularProgress.setProgress(savedProgress);
            startButtonFlashing();
            startAlertSound();
            startRippleLoop();
            panicButton.setOnTouchListener(null);
            setupRestartListener();
        } else {
            circularProgress.reset();
            setupTouchListener();
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupTouchListener() {
        panicButton.setOnClickListener(null);
        panicButton.setOnTouchListener((v, event) -> {
            if (triggered) return true;
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    startHold();
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    cancelHold();
                    return true;
            }
            return false;
        });
    }

    private void startHold() {
        if (triggered) return;
        isHolding = true;

        float startProgress = prefs.getPanicProgress();
        float remainingProgress = 1f - startProgress;

        animateButtonScale(1f, 1.1f, 200);

        panicRunnable = () -> {
            triggered = true;
            prefs.setAlertActive(true);
            prefs.setPanicProgress(1f);
            isHolding = false;
            onPanicTriggered();
        };
        handler.postDelayed(panicRunnable, (long) (remainingProgress * HOLD_DURATION_MS));

        progressAnimator = ValueAnimator.ofFloat(startProgress, 1f);
        progressAnimator.setDuration((long) (remainingProgress * HOLD_DURATION_MS));
        progressAnimator.setInterpolator(new LinearInterpolator());
        progressAnimator.addUpdateListener(a -> {
            float progress = (float) a.getAnimatedValue();
            circularProgress.setProgress(progress);
            prefs.setPanicProgress(progress);
        });
        progressAnimator.start();
    }

    private void cancelHold() {
        isHolding = false;
        if (panicRunnable != null) handler.removeCallbacks(panicRunnable);
        if (progressAnimator != null) {
            progressAnimator.cancel();
            progressAnimator = null;
        }
        animateButtonScale(1.1f, 1f, 150);

        if (!triggered) {
            prefs.setPanicProgress(0f);
            circularProgress.reset();
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void onPanicTriggered() {
        circularProgress.setProgress(1f);
        prefs.setPanicProgress(1f);

        showRippleWaveEffect();
        startRippleLoop();
        startButtonFlashing();
        startAlertSound();
        triggerPanicButton();

        panicButton.setOnTouchListener(null);
        setupRestartListener();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupRestartListener() {
        panicButton.setOnTouchListener(null);
        panicButton.setOnClickListener(v -> {
            if (!triggered) return;
            stopAlertSound();
            stopRippleLoop();
            stopButtonFlashing();
            resetButtonState();
            setupTouchListener();
        });
    }

    private void resetButtonState() {
        triggered = false;
        prefs.setAlertActive(false);
        prefs.setPanicProgress(0f);
        circularProgress.reset();

        panicButton.setOnClickListener(null);

        if (flashAnimator != null) {
            flashAnimator.cancel();
            flashAnimator = null;
        }

        rippleWave.setVisibility(View.GONE);
    }

    private void showRippleWaveEffect() {
        if (rippleWave == null) return;
        rippleWave.setVisibility(View.VISIBLE);
        rippleWave.setScaleX(0f);
        rippleWave.setScaleY(0f);
        rippleWave.setAlpha(0.6f);

        rippleWave.animate()
                .scaleX(6f)
                .scaleY(6f)
                .alpha(0f)
                .setDuration(700)
                .withEndAction(() -> {
                    if (!triggered) rippleWave.setVisibility(View.GONE);
                })
                .start();

        vibrateShort();
    }

    private void startRippleLoop() {
        stopRippleLoop();
        rippleLoopRunnable = new Runnable() {
            @Override
            public void run() {
                if (!triggered) return;
                showRippleWaveEffect();
                handler.postDelayed(this, RIPPLE_INTERVAL_MS);
            }
        };
        handler.post(rippleLoopRunnable);
    }

    private void stopRippleLoop() {
        if (rippleLoopRunnable != null) {
            handler.removeCallbacks(rippleLoopRunnable);
            rippleLoopRunnable = null;
        }
        if (rippleWave != null) rippleWave.setVisibility(View.GONE);
    }

    private void startButtonFlashing() {
        stopButtonFlashing();
        flashAnimator = ValueAnimator.ofArgb(0xFFFF0000, 0xAAFF0000);
        flashAnimator.setDuration(500);
        flashAnimator.setRepeatCount(ValueAnimator.INFINITE);
        flashAnimator.setRepeatMode(ValueAnimator.REVERSE);
        flashAnimator.addUpdateListener(animation -> {
            int color = (int) animation.getAnimatedValue();
            // proteja cast em caso de drawable diferente
            if (panicButton.getBackground() instanceof GradientDrawable) {
                GradientDrawable drawable = (GradientDrawable) panicButton.getBackground();
                drawable.setColor(color);
            }
        });
        flashAnimator.start();
    }

    private void stopButtonFlashing() {
        if (flashAnimator != null) {
            flashAnimator.cancel();
            flashAnimator = null;
        }
    }

    private void animateButtonScale(float from, float to, long duration) {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(panicButton, "scaleX", from, to);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(panicButton, "scaleY", from, to);
        scaleX.setDuration(duration);
        scaleY.setDuration(duration);
        scaleX.start();
        scaleY.start();
    }

    private void startAlertSound() {
        Context context = getSafeContext();
        if (context == null) return;

        if (alertSound == null) {
            alertSound = MediaPlayer.create(context, R.raw.alert_sound);
            if (alertSound != null) alertSound.setLooping(true);
        }
        if (alertSound != null && !alertSound.isPlaying()) alertSound.start();
    }

    private void stopAlertSound() {
        if (alertSound != null) {
            try {
                if (alertSound.isPlaying()) alertSound.stop();
            } catch (IllegalStateException ignored) {}
            try {
                alertSound.release();
            } catch (Exception ignored) {}
            alertSound = null;
        }
    }

    private void vibrateShort() {
        Context context = getSafeContext();
        if (context == null) return;
        Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if (v != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                v.vibrate(VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE));
            else v.vibrate(150);
        }
    }

    private void triggerPanicButton() {
        if (ActivityCompat.checkSelfPermission(requireContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION) !=
                android.content.pm.PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 1001);
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            double lat = 0, lon = 0;
            if (location != null) {
                lat = location.getLatitude();
                lon = location.getLongitude();
                prefs.setLastLocation(lat, lon);
            }

            PanicAlertRequest alertDTO = new PanicAlertRequest();
            alertDTO.setLeitura("Botão de pânico acionado");
            alertDTO.setLatitude(lat);
            alertDTO.setLongitude(lon);

            api.triggerPanicButton(alertDTO).enqueue(new Callback<>() {
                @Override public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {}
                @Override public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                    t.printStackTrace();
                }
            });
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        cancelHold();
        stopRippleLoop();
        stopButtonFlashing();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopAlertSound();
        stopRippleLoop();
        stopButtonFlashing();
    }

    @Nullable
    private Context getSafeContext() {
        return isAdded() ? getContext() : null;
    }
}
