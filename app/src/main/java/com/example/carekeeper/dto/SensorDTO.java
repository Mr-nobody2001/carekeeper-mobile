package com.example.carekeeper.dto;

import androidx.annotation.NonNull;

/**
 * DTO para dados de sensores do celular.
 * Contém acelerômetro, giroscópio, GPS e timestamp.
 */
public class SensorDTO {

    // Movimento (acelerômetro)
    private double accelerometerX;
    private double accelerometerY;
    private double accelerometerZ;

    // Movimento (giroscópio)
    private double gyroscopeX;
    private double gyroscopeY;
    private double gyroscopeZ;

    // Localização
    private double latitude;
    private double longitude;

    // Timestamp da leitura
    private long timestamp;

    // Construtor principal
    public SensorDTO(double ax, double ay, double az,
                     double gx, double gy, double gz,
                     double latitude, double longitude, long timestamp) {
        this.accelerometerX = ax;
        this.accelerometerY = ay;
        this.accelerometerZ = az;
        this.gyroscopeX = gx;
        this.gyroscopeY = gy;
        this.gyroscopeZ = gz;
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = timestamp;
    }

    // Getters e setters
    public double getAccelerometerX() { return accelerometerX; }
    public void setAccelerometerX(double accelerometerX) { this.accelerometerX = accelerometerX; }

    public double getAccelerometerY() { return accelerometerY; }
    public void setAccelerometerY(double accelerometerY) { this.accelerometerY = accelerometerY; }

    public double getAccelerometerZ() { return accelerometerZ; }
    public void setAccelerometerZ(double accelerometerZ) { this.accelerometerZ = accelerometerZ; }

    public double getGyroscopeX() { return gyroscopeX; }
    public void setGyroscopeX(double gyroscopeX) { this.gyroscopeX = gyroscopeX; }

    public double getGyroscopeY() { return gyroscopeY; }
    public void setGyroscopeY(double gyroscopeY) { this.gyroscopeY = gyroscopeY; }

    public double getGyroscopeZ() { return gyroscopeZ; }
    public void setGyroscopeZ(double gyroscopeZ) { this.gyroscopeZ = gyroscopeZ; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    @NonNull
    @Override
    public String toString() {
        return "SensorDTO{" +
                "accelerometerX=" + accelerometerX +
                ", accelerometerY=" + accelerometerY +
                ", accelerometerZ=" + accelerometerZ +
                ", gyroscopeX=" + gyroscopeX +
                ", gyroscopeY=" + gyroscopeY +
                ", gyroscopeZ=" + gyroscopeZ +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", timestamp=" + timestamp +
                '}';
    }
}
