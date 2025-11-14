package com.example.carekeeper.dto;

public class PanicAlertRequest {
    private String leitura;
    private double latitude;
    private double longitude;

    // Getters e setters
    public String getLeitura() { return leitura; }
    public void setLeitura(String leitura) { this.leitura = leitura; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
}
