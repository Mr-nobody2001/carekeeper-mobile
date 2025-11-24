package com.example.carekeeper.network;

import com.example.carekeeper.dto.SensorDTO;
import com.example.carekeeper.dto.PanicAlertRequest;
import com.example.carekeeper.dto.auth.LoginRequest;
import com.example.carekeeper.dto.auth.LoginResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface ApiService {

    /**
     * Faz login e retorna um token JWT.
     */
    @POST("/autenticacao/login")
    Call<LoginResponse> login(@Body LoginRequest loginRequest);

    /**
     * Logout do usuário. O token JWT será adicionado automaticamente pelo interceptor.
     *
     * @return Void
     */
    @POST("/autenticacao/logout")
    Call<Void> logout();

    /**
     * Envia leitura dos sensores.
     * O userId será extraído automaticamente do JWT no backend.
     */
    @POST("/monitor/leitura")
    Call<Void> sendReading(@Body SensorDTO leitura, @Query("ativo") boolean isAlertActive);

    /**
     * Aciona o botão de pânico manualmente.
     * O userId também será extraído do JWT.
     */
    @POST("/emergencia/alerta")
    Call<Void> triggerPanicButton(@Body PanicAlertRequest alertDTO);
}
