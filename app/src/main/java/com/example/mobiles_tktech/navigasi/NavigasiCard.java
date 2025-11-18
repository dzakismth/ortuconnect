package com.example.mobiles_tktech.navigasi;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.mobiles_tktech.MainActivity;
import com.example.mobiles_tktech.R;
import com.example.mobiles_tktech.absen.AbsensiFragment;
import com.example.mobiles_tktech.dashboard.DashboardFragment;
import com.example.mobiles_tktech.kalender.KalenderFragment;
import com.example.mobiles_tktech.perizinan.PerizinanFragment;
import com.example.mobiles_tktech.profile.ProfileFragment;
import com.example.mobiles_tktech.login.SessionManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class NavigasiCard extends AppCompatActivity {

    private static final String TAG = "NavigasiCard";
    private FragmentManager fragmentManager;
    private Fragment dashboardFragment, absensiFragment, kalenderFragment, perizinanFragment, profileFragment;
    private Fragment activeFragment;
    private SessionManager sessionManager;
    private Handler sessionCheckHandler;
    private Runnable sessionCheckRunnable;
    private static final long SESSION_CHECK_INTERVAL = 50000; // Cek setiap 5 detik

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "=== NavigasiCard onCreate ===");

        // 🔹 VALIDASI SESSION DULU
        sessionManager = new SessionManager(getApplicationContext());

        // Cek session segera
        if (!isSessionValid()) {
            Log.e(TAG, "Session invalid on onCreate, redirecting to login");
            redirectToLogin("Sesi tidak valid. Silakan login kembali.");
            return;
        }

        setContentView(R.layout.activity_main_container);

        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav_card);
        ViewCompat.setOnApplyWindowInsetsListener(bottomNav, (v, insets) -> {
            int bottomInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom;
            v.setPadding(0, 0, 0, bottomInset);
            return insets;
        });

        fragmentManager = getSupportFragmentManager();

        // 🔹 Inisialisasi semua fragment
        dashboardFragment = new DashboardFragment();
        absensiFragment = new AbsensiFragment();
        kalenderFragment = new KalenderFragment();
        perizinanFragment = new PerizinanFragment();
        profileFragment = new ProfileFragment();

        // 🔹 Tambahkan ke fragment manager, hanya dashboard yang ditampilkan
        fragmentManager.beginTransaction()
                .add(R.id.fragment_container, profileFragment, "profile").hide(profileFragment)
                .add(R.id.fragment_container, perizinanFragment, "perizinan").hide(perizinanFragment)
                .add(R.id.fragment_container, kalenderFragment, "kalender").hide(kalenderFragment)
                .add(R.id.fragment_container, absensiFragment, "absensi").hide(absensiFragment)
                .add(R.id.fragment_container, dashboardFragment, "dashboard")
                .commit();

        activeFragment = dashboardFragment;

        // 🔹 Listener navigasi
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            Fragment newFragment = null;
            if (itemId == R.id.nav_beranda) {
                newFragment = dashboardFragment;
            } else if (itemId == R.id.nav_absensi) {
                newFragment = absensiFragment;
            } else if (itemId == R.id.nav_kalender) {
                newFragment = kalenderFragment;
            } else if (itemId == R.id.nav_perizinan) {
                newFragment = perizinanFragment;
            } else if (itemId == R.id.nav_profile) {
                newFragment = profileFragment;
            }

            if (newFragment != null && newFragment != activeFragment) {
                fragmentManager.beginTransaction()
                        .hide(activeFragment)
                        .show(newFragment)
                        .commit();
                activeFragment = newFragment;
            }

            return true;
        });

        bottomNav.setSelectedItemId(R.id.nav_beranda);

        // 🔹 START PERIODIC SESSION CHECK
        startSessionCheck();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "=== NavigasiCard onResume ===");

        // 🔹 CEK SESSION SETIAP KALI ACTIVITY MUNCUL LAGI
        if (!isSessionValid()) {
            Log.e(TAG, "Session invalid on onResume, redirecting to login");
            redirectToLogin("Sesi Anda telah berakhir. Silakan login kembali.");
            return;
        }

        // Restart periodic check jika belum jalan
        if (sessionCheckHandler == null) {
            startSessionCheck();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "=== NavigasiCard onPause ===");
        // Stop periodic check saat activity tidak terlihat
        stopSessionCheck();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "=== NavigasiCard onDestroy ===");
        stopSessionCheck();
    }

    private void startSessionCheck() {
        if (sessionCheckHandler != null) {
            return; // Sudah jalan
        }

        Log.d(TAG, "Starting periodic session check");
        sessionCheckHandler = new Handler(Looper.getMainLooper());
        sessionCheckRunnable = new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "Periodic session check...");
                if (!isSessionValid()) {
                    Log.e(TAG, "Session invalid during periodic check!");
                    redirectToLogin("Sesi Anda tidak valid. Silakan login kembali.");
                } else {
                    sessionCheckHandler.postDelayed(this, SESSION_CHECK_INTERVAL);
                }
            }
        };
        sessionCheckHandler.postDelayed(sessionCheckRunnable, SESSION_CHECK_INTERVAL);
    }

    private void stopSessionCheck() {
        if (sessionCheckHandler != null && sessionCheckRunnable != null) {
            Log.d(TAG, "Stopping periodic session check");
            sessionCheckHandler.removeCallbacks(sessionCheckRunnable);
            sessionCheckHandler = null;
            sessionCheckRunnable = null;
        }
    }

    private boolean isSessionValid() {
        boolean isLoggedIn = sessionManager.isLoggedIn();
        Log.d(TAG, "isLoggedIn: " + isLoggedIn);

        if (!isLoggedIn) {
            Log.w(TAG, "User not logged in");
            return false;
        }

        String username = sessionManager.getUsername();
        String userId = sessionManager.getUserId();
        String role = sessionManager.getUserRole();

        Log.d(TAG, "Session data - Username: '" + username + "', UserId: '" + userId + "', Role: '" + role + "'");

        boolean isUsernameValid = username != null && !username.trim().isEmpty();
        boolean isUserIdValid = userId != null && !userId.trim().isEmpty();

        if (!isUsernameValid || !isUserIdValid) {
            Log.w(TAG, "Session data incomplete - Username valid: " + isUsernameValid + ", UserId valid: " + isUserIdValid);
            return false;
        }

        Log.d(TAG, "Session valid for user: " + username + " (ID: " + userId + ")");
        return true;
    }

    private void redirectToLogin(String message) {
        Log.w(TAG, "Redirecting to login: " + message);

        stopSessionCheck();

        if (sessionManager != null) {
            sessionManager.logoutUser();
        }

        Toast.makeText(this, message, Toast.LENGTH_LONG).show();

        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    public void logout() {
        Log.d(TAG, "Logout initiated by user");
        redirectToLogin("Anda telah logout");
    }

    public void navigateToDashboard() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav_card);
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_beranda);
        }
    }

    public SessionManager getSessionManager() {
        return sessionManager;
    }

    /**
     * 🔹 METHOD TAMBAHAN UNTUK NAVIGASI MANUAL DARI FRAGMENT
     */
    public void navigateTo(String target) {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav_card);
        if (bottomNav == null) return;

        switch (target.toLowerCase()) {
            case "dashboard":
                bottomNav.setSelectedItemId(R.id.nav_beranda);
                break;
            case "absensi":
                bottomNav.setSelectedItemId(R.id.nav_absensi);
                break;
            case "kalender":
                bottomNav.setSelectedItemId(R.id.nav_kalender);
                break;
            case "perizinan":
                bottomNav.setSelectedItemId(R.id.nav_perizinan);
                break;
            case "profil":
            case "profile":
                bottomNav.setSelectedItemId(R.id.nav_profile);
                break;
        }
    }
}
