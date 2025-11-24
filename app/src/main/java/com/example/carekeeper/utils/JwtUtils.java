package com.example.carekeeper.utils;

import android.util.Base64;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;

/**
 * Utilitário para trabalhar com JWT no lado do cliente.
 * Permite decodificar o payload e verificar expiração.
 */
public class JwtUtils {

    /**
     * Verifica se o JWT está expirado.
     *
     * @param token JWT recebido do servidor
     * @return true se expirado ou token inválido; false se ainda válido
     */
    public static boolean isTokenExpired(String token) {
        if (token == null || token.isEmpty()) return true;

        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) return true; // token malformado

            String payload = parts[1];
            byte[] decodedBytes = Base64.decode(payload, Base64.URL_SAFE | Base64.NO_PADDING | Base64.NO_WRAP);
            String decodedPayload = new String(decodedBytes, StandardCharsets.UTF_8);

            JSONObject jsonObject = new JSONObject(decodedPayload);
            if (!jsonObject.has("exp")) return true; // sem exp, considera expirado

            long exp = jsonObject.getLong("exp"); // exp em segundos desde epoch
            long now = System.currentTimeMillis() / 1000L;

            return now >= exp;

        } catch (JSONException | IllegalArgumentException e) {
            e.printStackTrace();
            return true; // qualquer erro, considera expirado
        }
    }

    /**
     * Retorna o payload decodificado do JWT como JSONObject.
     * @param token JWT
     * @return JSONObject do payload ou null se inválido
     */
    public static JSONObject getPayload(String token) {
        if (token == null || token.isEmpty()) return null;

        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) return null;

            String payload = parts[1];
            byte[] decodedBytes = Base64.decode(payload, Base64.URL_SAFE | Base64.NO_PADDING | Base64.NO_WRAP);
            String decodedPayload = new String(decodedBytes, StandardCharsets.UTF_8);

            return new JSONObject(decodedPayload);

        } catch (JSONException | IllegalArgumentException e) {
            e.printStackTrace();
            return null;
        }
    }
}
