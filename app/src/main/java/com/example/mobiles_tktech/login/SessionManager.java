package com.example.mobiles_tktech.login;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public class SessionManager {
    private static final String TAG = "SessionManager";
    private static final String PREF_NAME = "LoginPrefs";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_USER_ROLE = "userRole";

    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;
    Context context;
    int PRIVATE_MODE = 0;

    public SessionManager(Context context) {
        this.context = context;
        sharedPreferences = context.getSharedPreferences(PREF_NAME, PRIVATE_MODE);
        editor = sharedPreferences.edit();

        Log.d(TAG, "SessionManager created");
        logCurrentSession();
    }

    /**
     * Simpan session login (tanpa token)
     */
    public void createLoginSession(String username, String userId, String role) {
        Log.d(TAG, "createLoginSession - username: " + username + ", userId: " + userId + ", role: " + role);

        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putString(KEY_USERNAME, username);
        editor.putString(KEY_USER_ID, userId);
        editor.putString(KEY_USER_ROLE, role);

        boolean success = editor.commit();

        Log.d(TAG, "Session save result: " + success);
        logCurrentSession();
    }

    public String getUsername() {
        String username = sharedPreferences.getString(KEY_USERNAME, null);
        Log.d(TAG, "getUsername: " + username);
        return username;
    }

    public String getUserId() {
        String userId = sharedPreferences.getString(KEY_USER_ID, null);
        Log.d(TAG, "getUserId: " + userId);
        return userId;
    }

    public String getUserRole() {
        String role = sharedPreferences.getString(KEY_USER_ROLE, null);
        Log.d(TAG, "getUserRole: " + role);
        return role;
    }

    public boolean isLoggedIn() {
        boolean loggedIn = sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false);
        Log.d(TAG, "isLoggedIn: " + loggedIn);
        return loggedIn;
    }

    public void logoutUser() {
        Log.d(TAG, "logoutUser called");
        editor.clear();
        boolean success = editor.commit();
        Log.d(TAG, "Logout result: " + success);
        logCurrentSession();
    }

    /**
     * Validasi session - cek apakah data lengkap
     */
    public boolean isSessionValid() {
        boolean isLoggedIn = sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false);
        String username = sharedPreferences.getString(KEY_USERNAME, null);
        String userId = sharedPreferences.getString(KEY_USER_ID, null);

        boolean isUsernameValid = username != null && !username.trim().isEmpty();
        boolean isUserIdValid = userId != null && !userId.trim().isEmpty();

        boolean isValid = isLoggedIn && isUsernameValid && isUserIdValid;

        Log.d(TAG, "isSessionValid check:");
        Log.d(TAG, "  - isLoggedIn: " + isLoggedIn);
        Log.d(TAG, "  - username: '" + username + "' (valid: " + isUsernameValid + ")");
        Log.d(TAG, "  - userId: '" + userId + "' (valid: " + isUserIdValid + ")");
        Log.d(TAG, "  - RESULT: " + isValid);

        return isValid;
    }

    /**
     * Clear session jika data tidak valid atau rusak
     */
    public void clearInvalidSession() {
        if (!isSessionValid()) {
            Log.w(TAG, "Invalid session detected, clearing...");
            logoutUser();
        }
    }

    private void logCurrentSession() {
        Log.d(TAG, "=== Current Session State ===");
        Log.d(TAG, "isLoggedIn: " + sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false));
        Log.d(TAG, "username: " + sharedPreferences.getString(KEY_USERNAME, "null"));
        Log.d(TAG, "userId: " + sharedPreferences.getString(KEY_USER_ID, "null"));
        Log.d(TAG, "role: " + sharedPreferences.getString(KEY_USER_ROLE, "null"));
        Log.d(TAG, "============================");
    }
}