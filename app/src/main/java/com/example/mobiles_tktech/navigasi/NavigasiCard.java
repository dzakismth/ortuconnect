package com.example.mobiles_tktech.navigasi;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.mobiles_tktech.R;
import com.example.mobiles_tktech.absen.AbsensiFragment;
import com.example.mobiles_tktech.dashboard.DashboardFragment;
import com.example.mobiles_tktech.kalender.KalenderFragment;
import com.example.mobiles_tktech.perizinan.PerizinanFragment;
import com.example.mobiles_tktech.profile.ProfileFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class NavigasiCard extends AppCompatActivity {

    private FragmentManager fragmentManager;
    private Fragment dashboardFragment, absensiFragment, kalenderFragment, perizinanFragment, profileFragment;
    private Fragment activeFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
    }

    // 🔹 Tambahkan method agar fragment lain bisa balik ke Dashboard
    public void navigateToDashboard() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav_card);
        bottomNav.setSelectedItemId(R.id.nav_beranda);
    }
}
