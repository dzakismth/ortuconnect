package com.example.mobiles_tktech.dashboard;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.mobiles_tktech.R;

import org.json.JSONException;
import org.json.JSONObject;

public class DashboardFragment extends Fragment {

    private TextView tvNamaSiswa, tvKelas;
    private static final String BASE_URL = "http://ortuconnect.atwebpages.com/api/profile.php";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Inisialisasi komponen
        tvNamaSiswa = view.findViewById(R.id.tv_nama_siswa);
        tvKelas = view.findViewById(R.id.tv_kelas);

        CardView cardJadwal = view.findViewById(R.id.card_jadwal);
        CardView cardKehadiran = view.findViewById(R.id.card_kehadiran);
        CardView cardStatusIzin = view.findViewById(R.id.card_status_izin);

        // Panggil data profil dari API
        ambilDataProfil();

        // Navigasi ke fragment lain
        cardJadwal.setOnClickListener(v -> navigateToFragment(new com.example.mobiles_tktech.kalender.KalenderFragment()));
        cardKehadiran.setOnClickListener(v -> navigateToFragment(new com.example.mobiles_tktech.absen.AbsensiFragment()));
        cardStatusIzin.setOnClickListener(v -> navigateToFragment(new com.example.mobiles_tktech.perizinan.PerizinanFragment()));
    }

    private void ambilDataProfil() {
        // Ambil ID user dari SharedPreferences (yang disimpan saat login)
        SharedPreferences sharedPreferences = requireContext().getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        String idUser = sharedPreferences.getString("id_user", null);

        if (idUser == null) {
            Log.e("DashboardFragment", "ID user tidak ditemukan di SharedPreferences");
            return;
        }

        String url = BASE_URL + "?id_user=" + idUser;

        StringRequest request = new StringRequest(Request.Method.GET, url,
                response -> {
                    try {
                        JSONObject jsonObject = new JSONObject(response);
                        if (jsonObject.getBoolean("success")) {
                            JSONObject data = jsonObject.getJSONObject("data");

                            String nama = data.optString("nama", "-");
                            String kelas = data.optString("kelas", "-");

                            tvNamaSiswa.setText("Halo, " + nama);
                            tvKelas.setText("Kelas: " + kelas);
                        } else {
                            Log.e("DashboardFragment", "Gagal ambil data: " + jsonObject.getString("message"));
                        }
                    } catch (JSONException e) {
                        Log.e("DashboardFragment", "Error parsing JSON", e);
                    }
                },
                error -> Log.e("DashboardFragment", "Volley error: " + error.getMessage()));

        RequestQueue queue = Volley.newRequestQueue(requireContext());
        queue.add(request);
    }

    private void navigateToFragment(Fragment fragment) {
        if (getFragmentManager() != null) {
            FragmentTransaction transaction = getFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_container, fragment);
            transaction.addToBackStack(null);
            transaction.commit();
        }
    }
}
