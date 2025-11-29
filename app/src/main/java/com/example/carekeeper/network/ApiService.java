package com.example.carekeeper.network;

import com.example.carekeeper.dto.SensorDTO;
import com.example.carekeeper.dto.auth.LoginRequestDTO;
import com.example.carekeeper.dto.auth.LoginResponseDTO;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface ApiService {

    /**
     * Faz login e retorna um token JWT.
     */
    @POST("auth/login")
    Call<LoginResponseDTO> login(@Body LoginRequestDTO loginRequest);

    /**
     * Envia leitura dos sensores.
     * O userId será extraído automaticamente do JWT no backend.
     */
    @POST("monitor/leitura")
    Call<Void> sendReading(@Body SensorDTO leitura, @Query("ativo") boolean isAlertActive);

    /**
     * Aciona o botão de pânico manualmente.
     * O userId também será extraído do JWT.
     */
    @POST("emergencia/alerta")
    Call<Void> triggerPanicButton(@Body SensorDTO leitura);
}
