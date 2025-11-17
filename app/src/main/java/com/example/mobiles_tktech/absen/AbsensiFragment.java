package com.example.mobiles_tktech.absen;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.mobiles_tktech.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class AbsensiFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_absensi, container, false);

        ImageButton btnBack = view.findViewById(R.id.btn_back_header);
        btnBack.setOnClickListener(v -> {
            // Pastikan activity adalah NavigasiCard
            if (getActivity() instanceof com.example.mobiles_tktech.navigasi.NavigasiCard) {
                ((com.example.mobiles_tktech.navigasi.NavigasiCard) getActivity()).navigateToDashboard();

                // Update icon di bottom navigation agar kembali ke Beranda
                BottomNavigationView bottomNav = requireActivity().findViewById(R.id.bottom_nav_card);
                bottomNav.setSelectedItemId(R.id.nav_beranda);
            }
        });

        return view;
    }
}
