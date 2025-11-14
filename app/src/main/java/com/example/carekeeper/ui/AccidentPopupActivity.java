package com.example.carekeeper.ui;

import android.app.Activity;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import com.example.carekeeper.R;
import com.example.carekeeper.service.SharedPreferencesService;

public class AccidentPopupActivity extends Activity {

    private SharedPreferencesService sharedPreferencesService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Faz o popup aparecer sobre outros apps e até na tela bloqueada
        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        );

        setContentView(R.layout.activity_accident_popup);

        sharedPreferencesService = new SharedPreferencesService(this);

        TextView message = findViewById(R.id.textViewPopupMessage);
        Button buttonYes = findViewById(R.id.buttonConfirm);
        Button buttonNo = findViewById(R.id.buttonCancel);

        message.setText("Detectamos um possível acidente. Você está bem?");

        buttonYes.setOnClickListener(v -> {
            // Usuário confirmou que está bem → desativa alerta
            sharedPreferencesService.setAlertActive(false);
            finish();
        });

        buttonNo.setOnClickListener(v -> {
            // Usuário confirmou o acidente → mantém alerta ativo (ou chama outro fluxo)
            finish();
        });
    }
}
