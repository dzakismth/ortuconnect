package com.example.mobiles_tktech.dashboard;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import com.example.mobiles_tktech.R;

public class HomeFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Ganti fragment_home jika nama file Anda berbeda
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Inisialisasi CardView
        CardView cardJadwal = view.findViewById(R.id.card_jadwal);
        CardView cardKehadiran = view.findViewById(R.id.card_kehadiran);
        CardView cardStatusIzin = view.findViewById(R.id.card_status_izin);

    }

    // Fungsi utilitas untuk memuat Fragment baru
    private void navigateToFragment(Fragment fragment) {
        if (getFragmentManager() != null) {
            FragmentTransaction transaction = getFragmentManager().beginTransaction();
            // Asumsi ID container Fragment Anda adalah R.id.fragment_container
            transaction.replace(R.id.fragment_container, fragment);
            transaction.addToBackStack(null); // Memungkinkan tombol back untuk kembali ke HomeFragment
            transaction.commit();
        }
    }
}
