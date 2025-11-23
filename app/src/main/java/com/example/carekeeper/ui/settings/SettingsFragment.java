package com.example.carekeeper.ui.settings;

import android.content.Context;
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
import com.example.carekeeper.service.SharedPreferencesService;
import com.example.carekeeper.ui.emergency.EmergencyContactsFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.List;

public class SettingsFragment extends Fragment {

    private SharedPreferencesService prefsService;
    private TextInputEditText editMaxContacts;
    private TextView textThemeLabel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_settings, container, false);

        prefsService = new SharedPreferencesService(requireContext());

        SwitchCompat switchDarkTheme = root.findViewById(R.id.switchDarkTheme);
        editMaxContacts = root.findViewById(R.id.editMaxContacts);
        MaterialButton btnSalvar = root.findViewById(R.id.btnSalvarConfig);
        textThemeLabel = root.findViewById(R.id.textThemeLabel);

        // Estado inicial do tema
        boolean isDark = prefsService.isDarkThemeEnabled();
        switchDarkTheme.setChecked(isDark);
        updateThemeLabel(isDark);
        editMaxContacts.setText(String.valueOf(prefsService.getMaxCustomContacts()));

        // Alternar tema claro/escuro
        switchDarkTheme.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefsService.setDarkThemeEnabled(isChecked);
            AppCompatDelegate.setDefaultNightMode(
                    isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
            );
            updateThemeLabel(isChecked);
            vibrateShort();
        });

        // Salvar configurações
        btnSalvar.setOnClickListener(v -> salvarConfiguracoes());

        return root;
    }

    private void salvarConfiguracoes() {
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

        // 🔸 Validação: não permitir menos do que já existe
        List<EmergencyContactsFragment.EmergencyContact> contatos = prefsService.carregarContatosPersonalizados();
        int cadastrados = contatos.size();

        if (max < cadastrados) {
            Toast.makeText(getContext(),
                    "Você já possui " + cadastrados + " contatos cadastrados.\n"
                            + "Aumente o limite ou remova alguns antes de reduzir.",
                    Toast.LENGTH_LONG).show();
            editMaxContacts.setText(String.valueOf(prefsService.getMaxCustomContacts()));
            return;
        }

        prefsService.setMaxCustomContacts(max);
        Toast.makeText(getContext(), "Configurações salvas com sucesso!", Toast.LENGTH_SHORT).show();
    }

    private void updateThemeLabel(boolean isDark) {
        textThemeLabel.setText(isDark ? "🌙 Tema Escuro" : "🌞 Tema Claro");
    }

    private void vibrateShort() {
        getContext();
        Vibrator vibrator = (Vibrator) requireContext().getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(50);
            }
        }
    }
}
