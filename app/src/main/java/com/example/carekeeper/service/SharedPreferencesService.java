package com.example.carekeeper.service;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;

import com.example.carekeeper.ui.emergency.EmergencyContactsFragment.EmergencyContact;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Serviço centralizado de SharedPreferences para o app CareKeeper.
 *
 * Gerencia:
 *  - Estado do alerta
 *  - Progresso e estado do botão de pânico
 *  - Localização atual
 *  - Contatos personalizados
 *  - Configurações gerais do app
 *  - Token JWT de autenticação
 *  - Preferência de tema (claro/escuro)
 */
public class SharedPreferencesService {

    private static final String PREF_NAME = "carekeeper_prefs";
    private final SharedPreferences prefs;
    private final Gson gson = new Gson();

    // ===========================================================
    // =============== CHAVES DE CONFIGURAÇÃO ====================
    // ===========================================================

    private static final String KEY_IS_ALERT_ACTIVE = "isAlertActive";
    private static final String KEY_PANIC_PROGRESS = "panicProgress";
    private static final String KEY_PANIC_TRIGGERED = "panicTriggered";

    private static final String KEY_LATITUDE = "currentLatitude";
    private static final String KEY_LONGITUDE = "currentLongitude";

    private static final String KEY_CUSTOM_CONTACTS = "custom_contacts";
    private static final String KEY_MAX_CUSTOM_CONTACTS = "maxCustomContacts";

    private static final String KEY_DARK_THEME = "isDarkTheme";
    private static final String KEY_JWT_TOKEN = "jwt_token";
    private static final String KEY_USER_ID = "user_id";

    public SharedPreferencesService(Context context) {
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    // ===========================================================
    // =============== AUTENTICAÇÃO JWT ==========================
    // ===========================================================

    public void saveJwtToken(String token) {
        prefs.edit().putString(KEY_JWT_TOKEN, token).apply();
    }

    @Nullable
    public String getJwtToken() {
        return prefs.getString(KEY_JWT_TOKEN, null);
    }

    public void clearJwtToken() {
        prefs.edit().remove(KEY_JWT_TOKEN).apply();
    }

    public boolean isLoggedIn() {
        String token = getJwtToken();
        return token != null && !token.isEmpty();
    }

    // ===========================================================
    // =============== ESTADO DO ALERTA ==========================
    // ===========================================================

    public void setAlertActive(boolean active) {
        prefs.edit().putBoolean(KEY_IS_ALERT_ACTIVE, active).apply();
    }

    public boolean isAlertActive() {
        return prefs.getBoolean(KEY_IS_ALERT_ACTIVE, false);
    }

    // ===========================================================
    // =============== PROGRESSO DO BOTÃO DE PÂNICO ==============
    // ===========================================================

    public void setPanicProgress(float progress) {
        prefs.edit().putFloat(KEY_PANIC_PROGRESS, progress).apply();
    }

    public float getPanicProgress() {
        return prefs.getFloat(KEY_PANIC_PROGRESS, 0f);
    }

    public void setPanicTriggered(boolean triggered) {
        prefs.edit().putBoolean(KEY_PANIC_TRIGGERED, triggered).apply();
    }

    public boolean isPanicTriggered() {
        return prefs.getBoolean(KEY_PANIC_TRIGGERED, false);
    }

    // ===========================================================
    // =============== LOCALIZAÇÃO ATUAL =========================
    // ===========================================================

    public void setLastLocation(double latitude, double longitude) {
        prefs.edit()
                .putLong(KEY_LATITUDE, Double.doubleToRawLongBits(latitude))
                .putLong(KEY_LONGITUDE, Double.doubleToRawLongBits(longitude))
                .apply();
    }

    public double getLastLatitude() {
        return Double.longBitsToDouble(prefs.getLong(KEY_LATITUDE, 0));
    }

    public double getLastLongitude() {
        return Double.longBitsToDouble(prefs.getLong(KEY_LONGITUDE, 0));
    }

    // ===========================================================
    // =============== CONTATOS PERSONALIZADOS ===================
    // ===========================================================

    public void salvarContatosPersonalizados(List<EmergencyContact> contatos) {
        prefs.edit().putString(KEY_CUSTOM_CONTACTS, gson.toJson(contatos)).apply();
    }

    public List<EmergencyContact> carregarContatosPersonalizados() {
        String json = prefs.getString(KEY_CUSTOM_CONTACTS, null);
        if (json == null) return new ArrayList<>();
        Type listType = new TypeToken<ArrayList<EmergencyContact>>() {}.getType();
        List<EmergencyContact> contatos = gson.fromJson(json, listType);
        return contatos != null ? contatos : new ArrayList<>();
    }

    public void limparContatosPersonalizados() {
        prefs.edit().remove(KEY_CUSTOM_CONTACTS).apply();
    }

    // ===========================================================
    // =============== CONFIGURAÇÕES GERAIS ======================
    // ===========================================================

    public void setMaxCustomContacts(int max) {
        prefs.edit().putInt(KEY_MAX_CUSTOM_CONTACTS, max).apply();
    }

    public int getMaxCustomContacts() {
        return prefs.getInt(KEY_MAX_CUSTOM_CONTACTS, 3);
    }

    // ===========================================================
    // =============== TEMA CLARO / ESCURO =======================
    // ===========================================================

    public void setDarkThemeEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_DARK_THEME, enabled).apply();
    }

    public boolean isDarkThemeEnabled() {
        return prefs.getBoolean(KEY_DARK_THEME, false);
    }

    // ===========================================================
    // =============== LIMPEZA GERAL =============================
    // ===========================================================

    public void limparTudo() {
        prefs.edit().clear().apply();
    }
}
