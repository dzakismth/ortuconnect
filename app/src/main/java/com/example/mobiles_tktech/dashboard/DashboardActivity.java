package com.example.mobiles_tktech.dashboard;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.mobiles_tktech.R;
import com.example.mobiles_tktech.absen.AbsensiFragment;
import com.example.mobiles_tktech.kalender.KalenderFragment;
import com.example.mobiles_tktech.perizinan.PerizinanFragment;
import com.example.mobiles_tktech.profile.ProfileFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class DashboardActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_container);

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnItemSelectedListener(navListener);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new HomeFragment())
                    .commit();
        }
    }

    private final BottomNavigationView.OnItemSelectedListener navListener =
            item -> {
                Fragment selectedFragment = null;
                int itemId = item.getItemId();
                if (itemId == R.id.nav_beranda) {
                    selectedFragment = new HomeFragment();
                } else if (itemId == R.id.nav_absensi) {
                    selectedFragment = new AbsensiFragment();
                } else if (itemId == R.id.nav_kalender) {
                    selectedFragment = new KalenderFragment();
                } else if (itemId == R.id.nav_perizinan) {
                    selectedFragment = new PerizinanFragment();
                } else if (itemId == R.id.nav_profile) {
                    selectedFragment = new ProfileFragment();
                }

                if (selectedFragment != null) {
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, selectedFragment)
                            .commit();
                }
                return true;
            };
}