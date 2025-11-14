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
import com.example.carekeeper.network.ApiClient;
import com.example.carekeeper.network.ApiService;
import com.example.carekeeper.dto.PanicAlertRequest;
import com.example.carekeeper.service.PanicStateService;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Fragment responsável pelo botão de pânico principal do app.
 * - Segurar por 3s para acionar alerta.
 * - Ripple animado em loop (1s) enquanto ativo.
 * - Clique simples após acionado reseta tudo.
 */
public class PanicButtonFragment extends Fragment {

    private static final long HOLD_DURATION_MS = 3000L;
    private static final long RIPPLE_INTERVAL_MS = 1000L; // intervalo entre ondas

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

    // ============================================================= //
    // =============== CICLO DE VIDA DO FRAGMENT =================== //
    // ============================================================= //

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

        // carrega estado persistido (se houver)
        triggered = PanicStateService.triggered;

        if (triggered) {
            // Se já estava acionado ao abrir fragment, restaura visuais e listeners de reset
            circularProgress.setProgress(PanicStateService.progress);
            startButtonFlashing();
            startAlertSound();
            startRippleLoop();
            // IMPORTANTE: garantir que o touch listener antigo não esteja consumindo cliques
            panicButton.setOnTouchListener(null);
            setupRestartListener();
        } else {
            circularProgress.reset();
            setupTouchListener();
        }
    }

    // ============================================================= //
    // ==================== LÓGICA DO BOTÃO ======================== //
    // ============================================================= //

    /**
     * Cria e associa o OnTouchListener que implementa "segurar para ativar".
     * Guardamos a instância em `touchListener` para poder removê-la facilmente quando necessário.
     */
    @SuppressLint("ClickableViewAccessibility")
    private void setupTouchListener() {
        // remove qualquer click listener anterior (segurança)
        panicButton.setOnClickListener(null);

        // se já estiver acionado, bloqueia o touch default
        // OnTouchListener reference (guardamos para poder remover/reativar)
        View.OnTouchListener touchListener = (v, event) -> {
            if (triggered) return true; // se já estiver acionado, bloqueia o touch default

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
        };
        panicButton.setOnTouchListener(touchListener);
    }

    private void startHold() {
        if (triggered) return;
        isHolding = true;

        float startProgress = PanicStateService.progress;
        float remainingProgress = 1f - startProgress;

        // leve ampliação do botão como feedback
        animateButtonScale(1f, 1.1f, 200);

        panicRunnable = () -> {
            triggered = true;
            PanicStateService.triggered = true;
            PanicStateService.progress = 1f;
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
            PanicStateService.progress = progress;
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

        // retorna à escala original
        animateButtonScale(1.1f, 1f, 150);

        if (!triggered) {
            PanicStateService.progress = 0f;
            circularProgress.reset();
        }
    }

    /**
     * Chamado quando o hold chegou ao final e o alerta é disparado.
     * Removemos o OnTouchListener e configuramos o OnClickListener para reset.
     */
    @SuppressLint("ClickableViewAccessibility")
    private void onPanicTriggered() {
        circularProgress.setProgress(1f);
        PanicStateService.progress = 1f;

        // mostra primeira onda imediatamente
        showRippleWaveEffect();

        // inicia loop de ondas, som e piscar
        startRippleLoop();
        startButtonFlashing();
        startAlertSound();

        // envia o alerta (API)
        triggerPanicButton();

        // REMOVER o touch listener para liberar eventos de clique
        panicButton.setOnTouchListener(null);

        // configurar listener de restart/reset (clique simples)
        setupRestartListener();
    }

    /**
     * Configura o clique simples que resetará tudo caso o alerta esteja ativo.
     * Garantimos que o OnTouchListener esteja removido antes de registrar esse OnClick.
     */
    @SuppressLint("ClickableViewAccessibility")
    private void setupRestartListener() {
        // garantia: se por algum motivo o touchListener ainda estiver, remove
        panicButton.setOnTouchListener(null);

        panicButton.setOnClickListener(v -> {
            if (!triggered) return; // segurança: só reseta se realmente estiver acionado

            // Para som, loop de ripple, animações e reset de estado
            stopAlertSound();
            stopRippleLoop();
            stopButtonFlashing();
            resetButtonState();

            // restaura comportamento original de "segurar para ativar"
            setupTouchListener();
        });
    }

    private void resetButtonState() {
        triggered = false;
        PanicStateService.reset();
        circularProgress.reset();

        // remove o click listener de reset (evita cliques fantasma)
        panicButton.setOnClickListener(null);

        if (flashAnimator != null) {
            flashAnimator.cancel();
            flashAnimator = null;
        }

        rippleWave.setVisibility(View.GONE);
    }

    // ============================================================= //
    // ======================= ANIMAÇÕES =========================== //
    // ============================================================= //

    private void showRippleWaveEffect() {
        // protege caso view não exista
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
                    // apenas esconde se não estiver no meio de novo ciclo
                    if (!triggered) rippleWave.setVisibility(View.GONE);
                })
                .start();

        vibrateShort();
    }

    private void startRippleLoop() {
        stopRippleLoop(); // garante um loop único
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

    // ============================================================= //
    // =================== SOM E VIBRAÇÃO ========================== //
    // ============================================================= //

    private void startAlertSound() {
        Context context = getSafeContext();
        if (context == null) return;

        if (alertSound == null) {
            alertSound = MediaPlayer.create(context, R.raw.alert_sound);
            if (alertSound != null) alertSound.setLooping(true);
        }

        if (alertSound != null && !alertSound.isPlaying()) {
            alertSound.start();
        }
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                v.vibrate(150);
            }
        }
    }

    // ============================================================= //
    // ===================== ENVIO DE ALERTA ======================= //
    // ============================================================= //

    private void triggerPanicButton() {
        if (ActivityCompat.checkSelfPermission(requireContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION) !=
                android.content.pm.PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 1001);
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                PanicStateService.currentLatitude = location.getLatitude();
                PanicStateService.currentLongitude = location.getLongitude();
            }

            PanicAlertRequest alertDTO = new PanicAlertRequest();
            alertDTO.setLeitura("Botão de pânico acionado");
            alertDTO.setLatitude(PanicStateService.currentLatitude);
            alertDTO.setLongitude(PanicStateService.currentLongitude);

            api.triggerPanicButton(alertDTO).enqueue(new Callback<>() {
                @Override
                public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) { }

                @Override
                public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                    t.printStackTrace();
                }
            });
        });
    }

    // ============================================================= //
    // =================== CICLO DE VIDA EXTRA ===================== //
    // ============================================================= //

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onResume() {
        super.onResume();

        if (triggered) {
            circularProgress.setProgress(PanicStateService.progress);
            startButtonFlashing();
            startAlertSound();
            startRippleLoop();
            // garantir listener de reset
            panicButton.setOnTouchListener(null);
            setupRestartListener();
        } else {
            circularProgress.reset();
        }
    }

    @Nullable
    private Context getSafeContext() {
        return isAdded() ? getContext() : null;
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == 1001 &&
                grantResults.length > 0 &&
                grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            triggerPanicButton();
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    public boolean isHolding() {
        return isHolding;
    }

    public void setHolding(boolean holding) {
        isHolding = holding;
    }
}
