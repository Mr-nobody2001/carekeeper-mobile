package com.example.carekeeper.network;

import com.example.carekeeper.dto.PanicAlertRequest;
import com.example.carekeeper.dto.SensorDTO;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface ApiService {

    /**
     * Envia leitura dos sensores.
     * Inclui o parâmetro booleano "ativo" para indicar se há alerta.
     */
    @POST("/monitor/leitura")
    Call<Void> sendReading(@Body SensorDTO leitura, @Query("ativo") boolean isAlertActive);

    /**
     * Aciona o botão de pânico manualmente.
     */
    @POST("/emergencia/alerta")
    Call<Void> triggerPanicButton(@Body PanicAlertRequest alertDTO);
}
