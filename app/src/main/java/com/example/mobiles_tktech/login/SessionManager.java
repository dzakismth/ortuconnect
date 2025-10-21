package com.example.mobiles_tktech.login;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    private static final String PREF_NAME = "LoginPrefs";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_USER_TOKEN = "userToken"; // Untuk menyimpan token API

    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;
    Context context;
    int PRIVATE_MODE = 0; // Mode private agar hanya aplikasi ini yang bisa mengakses

    public SessionManager(Context context) {
        this.context = context;
        sharedPreferences = context.getSharedPreferences(PREF_NAME, PRIVATE_MODE);
        editor = sharedPreferences.edit();
    }

    /**
     * Menyimpan status login dan token
     * @param isLoggedIn status login
     * @param token token dari API
     */
    public void createLoginSession(boolean isLoggedIn, String token) {
        editor.putBoolean(KEY_IS_LOGGED_IN, isLoggedIn);
        editor.putString(KEY_USER_TOKEN, token);
        editor.apply();
    }

    /**
     * Mengecek apakah user sudah login
     * @return boolean true jika sudah login
     */
    public boolean isLoggedIn() {
        return sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    /**
     * Mengambil token user
     * @return String token
     */
    public String getUserToken() {
        return sharedPreferences.getString(KEY_USER_TOKEN, null);
    }

    /**
     * Menghapus semua data sesi (Logout)
     */
    public void logoutUser() {
        editor.clear();
        editor.apply();
        // Logika redirect ke Login akan dilakukan di ProfileFragment/Activity
    }
}