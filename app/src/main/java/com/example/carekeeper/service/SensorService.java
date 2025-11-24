package com.example.carekeeper.service;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.example.carekeeper.dto.SensorDTO;
import com.example.carekeeper.network.ApiClient;
import com.example.carekeeper.network.ApiService;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SensorService extends Service implements SensorEventListener, LocationListener {

    private static final String TAG = "SensorService";
    private static final String CHANNEL_ID = "SensorServiceChannel";
    private static final long INTERVALO_ENVIO_MS = 1000L;

    private SensorManager sensorManager;
    private LocationManager locationManager;
    private ApiService apiService;
    private SharedPreferencesService sharedPreferencesService;

    private double ax, ay, az, gx, gy, gz;
    private double latitude, longitude;

    private final Handler handler = new Handler();
    private final Runnable envioPeriodico = new Runnable() {
        @Override
        public void run() {
            enviarLeitura();
            handler.postDelayed(this, INTERVALO_ENVIO_MS);
        }
    };

    // ===========================================================
    // =============== MÉTODOS ESTÁTICOS PARA ACTIVITY ==========
    // ===========================================================
    public static void iniciar(Context context) {
        // Verifica permissões de localização
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    (android.app.Activity) context,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    100
            );
            return;
        }

        Intent serviceIntent = new Intent(context, SensorService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }
    }

    public static void parar(Context context) {
        Intent serviceIntent = new Intent(context, SensorService.class);
        context.stopService(serviceIntent);
    }

    // ===========================================================
    // =============== MÉTODOS DO SERVICE =======================
    // ===========================================================
    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "Iniciando SensorService...");

        sharedPreferencesService = new SharedPreferencesService(this);
        apiService = ApiClient.getClientWithAuth(sharedPreferencesService).create(ApiService.class);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        registrarSensores();
        registrarLocalizacao();

        criarCanalDeNotificacao();
        iniciarComoForeground();

        handler.post(envioPeriodico);
    }

    private void registrarSensores() {
        if (sensorManager == null) return;

        Sensor acelerometro = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        Sensor giroscopio = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        if (acelerometro != null)
            sensorManager.registerListener(this, acelerometro, SensorManager.SENSOR_DELAY_NORMAL);

        if (giroscopio != null)
            sensorManager.registerListener(this, giroscopio, SensorManager.SENSOR_DELAY_NORMAL);
    }

    private void registrarLocalizacao() {
        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 3000, 1, this);
        } catch (SecurityException e) {
            Log.e(TAG, "Permissão de localização não concedida.");
        }
    }

    private void criarCanalDeNotificacao() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Canal de Monitoramento",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    private void iniciarComoForeground() {
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("CareKeeper ativo")
                .setContentText("Monitorando sensores e localização...")
                .setSmallIcon(android.R.drawable.ic_menu_compass)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
        startForeground(1, notification);
    }

    private void enviarLeitura() {
        boolean isAlertActive = sharedPreferencesService.isAlertActive();

        String token = sharedPreferencesService.getJwtToken();
        if (token == null || token.trim().isEmpty()) {
            Log.w(TAG, "⚠️ Nenhum token JWT encontrado. Ignorando envio da leitura.");
            return;
        }

        SensorDTO leitura = new SensorDTO(
                ax, ay, az,
                gx, gy, gz,
                latitude, longitude,
                System.currentTimeMillis()
        );

        Call<Void> call = apiService.sendReading(leitura, isAlertActive);
        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (response.isSuccessful()) {
                    Log.i(TAG, "✅ Leitura enviada com sucesso (ativo=" + isAlertActive + ")");
                } else if (response.code() == 401) {
                    Log.w(TAG, "🔒 Token expirado ou inválido. Ignorando leitura até novo login.");
                } else {
                    Log.w(TAG, "⚠️ Falha no envio: código HTTP " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                Log.e(TAG, "❌ Erro ao enviar leitura: " + t.getMessage());
            }
        });
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            ax = event.values[0];
            ay = event.values[1];
            az = event.values[2];
        } else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            gx = event.values[0];
            gy = event.values[1];
            gz = event.values[2];
        }
    }

    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {}
    @Override public void onLocationChanged(@NonNull Location location) {
        latitude = location.getLatitude();
        longitude = location.getLongitude();
    }
    @Override public void onStatusChanged(String provider, int status, Bundle extras) {}
    @Override public void onProviderEnabled(@NonNull String provider) {}
    @Override public void onProviderDisabled(@NonNull String provider) {}

    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(envioPeriodico);
        if (sensorManager != null) sensorManager.unregisterListener(this);
        if (locationManager != null) locationManager.removeUpdates(this);
        Log.i(TAG, "🛑 Serviço de sensores encerrado.");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
