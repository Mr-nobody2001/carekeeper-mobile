package com.example.carekeeper.ui.settings;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;

import com.example.carekeeper.R;
import com.example.carekeeper.network.ApiClient;
import com.example.carekeeper.network.ApiService;
import com.example.carekeeper.service.SensorService;
import com.example.carekeeper.service.SharedPreferencesService;
import com.example.carekeeper.ui.emergency.EmergencyContactsFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SettingsFragment extends Fragment {

    private SharedPreferencesService prefsService;
    private TextInputEditText editMaxContacts, editHoldDuration;
    private TextView textThemeLabel;
    private ApiService apiService;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_settings, container, false);

        prefsService = new SharedPreferencesService(requireContext());
        apiService = ApiClient.getClientWithAuth(prefsService).create(ApiService.class);

        SwitchCompat switchDarkTheme = root.findViewById(R.id.switchDarkTheme);
        editMaxContacts = root.findViewById(R.id.editMaxContacts);
        editHoldDuration = root.findViewById(R.id.editHoldDuration);
        MaterialButton btnSalvar = root.findViewById(R.id.btnSalvarConfig);
        MaterialButton btnLogout = root.findViewById(R.id.btnLogout);
        textThemeLabel = root.findViewById(R.id.textThemeLabel);

        // Estado inicial do tema
        boolean isDark = prefsService.isDarkThemeEnabled();
        switchDarkTheme.setChecked(isDark);
        updateThemeLabel(isDark);

        // Carrega valores do SharedPreferences
        editMaxContacts.setText(String.valueOf(prefsService.getMaxCustomContacts()));
        long holdDurationMs = prefsService.getHoldDuration();
        editHoldDuration.setText(String.valueOf(holdDurationMs / 1000)); // mostrar em segundos

        // Alternar tema claro/escuro
        switchDarkTheme.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefsService.setDarkThemeEnabled(isChecked);
            AppCompatDelegate.setDefaultNightMode(
                    isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
            );
            updateThemeLabel(isChecked);
            vibrateStrong();
        });

        // Salvar configurações
        btnSalvar.setOnClickListener(v -> salvarConfiguracoes());

        // Logout
        btnLogout.setOnClickListener(v -> performLogout());

        return root;
    }

    private void performLogout() {
        apiService.logout().enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (response.isSuccessful()) {
                    prefsService.clearJwtToken();
                    SensorService.parar(requireContext());

                    Toast.makeText(getContext(), "Logout realizado com sucesso!", Toast.LENGTH_SHORT).show();

                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            assert getView() != null;
                            androidx.navigation.NavController navController =
                                    androidx.navigation.Navigation.findNavController(getView());
                            navController.navigate(R.id.action_settings_to_login);
                        });
                    }
                } else {
                    Toast.makeText(getContext(), "Falha ao realizar logout: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                Toast.makeText(getContext(), "Erro ao conectar com servidor: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void salvarConfiguracoes() {
        // Salvar max de contatos
        String maxText = editMaxContacts.getText() != null
                ? editMaxContacts.getText().toString().trim()
                : "";

        if (maxText.isEmpty()) {
            Toast.makeText(getContext(), "Informe um número máximo de contatos.", Toast.LENGTH_SHORT).show();
            return;
        }

        int max = Integer.parseInt(maxText);
        if (max < 1) {
            Toast.makeText(getContext(), "O número mínimo é 1.", Toast.LENGTH_SHORT).show();
            return;
        }

        List<EmergencyContactsFragment.EmergencyContact> contatos = prefsService.carregarContatosPersonalizados();
        int cadastrados = contatos.size();

        if (max < cadastrados) {
            Toast.makeText(getContext(),
                    "Você já possui " + cadastrados + " contatos cadastrados.\n" +
                            "Aumente o limite ou remova alguns antes de reduzir.",
                    Toast.LENGTH_LONG).show();
            editMaxContacts.setText(String.valueOf(prefsService.getMaxCustomContacts()));
            return;
        }

        prefsService.setMaxCustomContacts(max);

        // Salvar tempo de hold em segundos
        String holdText = editHoldDuration.getText() != null
                ? editHoldDuration.getText().toString().trim()
                : "";

        if (holdText.isEmpty()) {
            Toast.makeText(getContext(), "Informe o tempo de hold em segundos.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            int holdSeconds = Integer.parseInt(holdText);
            if (holdSeconds < 1) {
                Toast.makeText(getContext(), "O tempo mínimo é 1 segundo.", Toast.LENGTH_SHORT).show();
                return;
            }
            prefsService.setHoldDuration(holdSeconds * 1000L); // salvar em ms
        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "Valor de tempo inválido.", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(getContext(), "Configurações salvas com sucesso!", Toast.LENGTH_SHORT).show();
    }

    private void updateThemeLabel(boolean isDark) {
        textThemeLabel.setText(isDark ? "🌙 Tema Escuro" : "🌞 Tema Claro");
    }

    private void vibrateStrong() {
        Context context = getContext();
        if (context == null) return;

        Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(150, 255));
            } else {
                vibrator.vibrate(150);
            }
        }
    }
}
