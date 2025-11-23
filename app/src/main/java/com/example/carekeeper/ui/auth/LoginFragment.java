package com.example.carekeeper.ui.auth;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.carekeeper.MainActivity;
import com.example.carekeeper.R;
import com.example.carekeeper.dto.auth.LoginRequest;
import com.example.carekeeper.dto.auth.LoginResponse;
import com.example.carekeeper.network.ApiClient;
import com.example.carekeeper.network.ApiService;
import com.example.carekeeper.service.SharedPreferencesService;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginFragment extends Fragment {

    private TextInputEditText inputEmail, inputPassword;
    private MaterialButton btnLogin;

    private ApiService apiService;
    private SharedPreferencesService prefs;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_login, container, false);

        inputEmail = view.findViewById(R.id.inputEmail);
        inputPassword = view.findViewById(R.id.inputPassword);
        btnLogin = view.findViewById(R.id.btnLogin);

        apiService = ApiClient.getClient().create(ApiService.class);
        prefs = new SharedPreferencesService(requireContext());

        prefs.clearJwtToken();

        // ✅ Se já está logado e o token não expirou, pula o login
        if (prefs.isLoggedIn()) {
            NavHostFragment.findNavController(this)
                    .navigate(R.id.action_loginFragment_to_nav_emergency_contacts);
            ((MainActivity) requireActivity()).iniciarServicoSensoresSePermitido();
            return view;
        }

        btnLogin.setOnClickListener(v -> performLogin());

        return view;
    }

    private void performLogin() {
        String email = inputEmail.getText() != null ? inputEmail.getText().toString().trim() : "";
        String password = inputPassword.getText() != null ? inputPassword.getText().toString().trim() : "";

        // ✅ Valida campos
        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(getContext(), "Preencha todos os campos", Toast.LENGTH_SHORT).show();
            return;
        }

        // ✅ Valida formato de email
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(getContext(), "Email inválido", Toast.LENGTH_SHORT).show();
            return;
        }

        btnLogin.setEnabled(false);
        btnLogin.setText("Entrando...");

        LoginRequest request = new LoginRequest(email, password);

        apiService.login(request).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(@NonNull Call<LoginResponse> call, @NonNull Response<LoginResponse> response) {
                btnLogin.setEnabled(true);
                btnLogin.setText("Entrar");

                if (response.isSuccessful() && response.body() != null) {
                    String token = response.body().getToken();

                    if (token != null && !token.isEmpty()) {
                        // ✅ Salva token JWT
                        prefs.saveJwtToken(token);

                        Toast.makeText(getContext(), "Login realizado com sucesso!", Toast.LENGTH_SHORT).show();

                        // ✅ Inicia o serviço de sensores após login
                        ((MainActivity) requireActivity()).iniciarServicoSensoresSePermitido();

                        // ✅ Navega para a tela principal
                        NavHostFragment.findNavController(LoginFragment.this)
                                .navigate(R.id.action_loginFragment_to_nav_emergency_contacts);

                    } else {
                        Toast.makeText(getContext(), "Token inválido recebido", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(getContext(), "Credenciais inválidas", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<LoginResponse> call, @NonNull Throwable t) {
                btnLogin.setEnabled(true);
                btnLogin.setText("Entrar");
                Toast.makeText(getContext(), "Erro ao conectar ao servidor", Toast.LENGTH_SHORT).show();
                t.printStackTrace();
            }
        });
    }
}
