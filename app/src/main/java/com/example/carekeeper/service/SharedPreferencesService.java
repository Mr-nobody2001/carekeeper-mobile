package com.example.carekeeper.service;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Serviço simples para gerenciar o estado de alerta no app.
 */
public class SharedPreferencesService {

    private static final String PREF_NAME = "carekeeper_prefs";
    private static final String KEY_IS_ALERT_ACTIVE = "isAlertActive";

    private final SharedPreferences prefs;

    public SharedPreferencesService(Context context) {
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void setAlertActive(boolean active) {
        prefs.edit().putBoolean(KEY_IS_ALERT_ACTIVE, active).apply();
    }

    public boolean isAlertActive() {
        return prefs.getBoolean(KEY_IS_ALERT_ACTIVE, false);
    }
}
