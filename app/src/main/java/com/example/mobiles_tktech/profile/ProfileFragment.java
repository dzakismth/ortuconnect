package com.example.mobiles_tktech.profile;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.mobiles_tktech.MainActivitycok; // Asumsi ini adalah Login Activity Anda
import com.example.mobiles_tktech.R;

public class ProfileFragment extends Fragment {

    // Asumsi layout XML Profile Anda dimuat di sini.
    // Pastikan R.layout.activity_profile adalah nama file XML Profile Anda.
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Dapatkan referensi tombol. Pastikan ID ini ada di XML Profil Anda.
        Button btnLogout = view.findViewById(R.id.btnKeluar);

        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                logoutUser();
            });
        }
    }

    private void logoutUser() {
        // 1. Hapus data sesi dari SharedPreferences
        // Penting: Ganti "LoginPrefs" jika Anda menggunakan nama SharedPreferences yang berbeda di SessionManager.
        SharedPreferences sharedPref = requireActivity().getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();

        editor.clear(); // Menghapus semua data sesi (status login, token, dll.)
        editor.apply();

        // 2. Arahkan pengguna kembali ke Login Activity
        Intent intent = new Intent(getActivity(), MainActivitycok.class);

        // Flag penting: Membersihkan semua Activity sebelumnya
        // Ini memastikan tombol back tidak akan membawa user kembali ke Dashboard
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);

        // Selesaikan Activity induk (DashboardActivity)
        requireActivity().finish();
    }
}