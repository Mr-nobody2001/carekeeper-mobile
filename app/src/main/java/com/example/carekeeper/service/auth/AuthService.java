package com.example.carekeeper.service.auth;

import android.content.Context;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.navigation.NavController;

import com.example.carekeeper.dto.auth.LoginResponseDTO;
import com.example.carekeeper.network.ApiClient;
import com.example.carekeeper.network.ApiService;
import com.example.carekeeper.service.SensorService;
import com.example.carekeeper.service.SharedPreferencesService;
import com.example.carekeeper.utils.JwtUtils;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Centraliza a lógica de login, logout e gerenciamento de token.
 */
public class AuthService {

    private final Context context;
    private final SharedPreferencesService prefs;
    private final ApiService api;

    public AuthService(@NonNull Context context) {
        this.context = context;
        this.prefs = new SharedPreferencesService(context);
        this.api = ApiClient.getClient().create(ApiService.class);
    }

    /** Verifica se o usuário está logado (token existe e não expirou) */
    public boolean isLoggedIn() {
        String token = prefs.getJwtToken();
        return token != null && !token.isEmpty() && !JwtUtils.isTokenExpired(token);
    }

    /**
     * Realiza logout:
     * - limpa token
     * - para o serviço de sensores
     * - exibe mensagem
     * - navega para tela de login
     */
    public void logout(NavController navController) {
        prefs.clearJwtToken();
        SensorService.stopService(context);

        Toast.makeText(context, "Logout realizado com sucesso!", Toast.LENGTH_SHORT).show();

        if (navController != null) {
            navController.navigate(com.example.carekeeper.R.id.action_settings_to_login);
        }
    }

    /** Salva token JWT */
    public void saveToken(@NonNull String token) {
        prefs.saveJwtToken(token);
    }

    /** Interface de callback para login */
    public interface LoginCallback {
        void onSuccess(String token);
        void onFailure(String message);
    }

    /** Realiza login via API */
    public void login(@NonNull String email, @NonNull String password, @NonNull LoginCallback callback) {
        api.login(new com.example.carekeeper.dto.auth.LoginRequestDTO(email, password))
                .enqueue(new Callback<>() {
                    @Override
                    public void onResponse(@NonNull Call<LoginResponseDTO> call,
                                           @NonNull Response<LoginResponseDTO> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            String token = response.body().getToken();
                            if (token != null && !token.isEmpty()) {
                                saveToken(token);
                                callback.onSuccess(token);
                            } else {
                                callback.onFailure("Token inválido recebido");
                            }
                        } else {
                            callback.onFailure("Credenciais inválidas");
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<LoginResponseDTO> call,
                                          @NonNull Throwable t) {
                        callback.onFailure("Falha ao conectar ao servidor");
                    }
                });
    }
}
