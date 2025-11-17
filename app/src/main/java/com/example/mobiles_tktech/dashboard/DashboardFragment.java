package com.example.mobiles_tktech.dashboard;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.mobiles_tktech.MainActivity;
import com.example.mobiles_tktech.R;
import com.example.mobiles_tktech.navigasi.NavigasiCard;

import org.json.JSONException;
import org.json.JSONObject;

public class DashboardFragment extends Fragment {

    private TextView tvNamaSiswa, tvKelas, tvJadwal, tvKehadiran, tvIzin;
    private ImageView imgProfile;
    private RequestQueue requestQueue;
    private String usernameOrtu;
    private static final String BASE_URL = "http://ortuconnect.atwebpages.com/api/profile.php";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        requestQueue = Volley.newRequestQueue(requireContext());
        SharedPreferences prefs = requireActivity().getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE);
        usernameOrtu = prefs.getString("username", "");

        if (usernameOrtu.isEmpty()) {
            Log.e("DashboardFragment", "⚠️ ID user tidak ditemukan di SharedPreferences");
            logoutUser();
            return;
        }

        // Bind view
        tvNamaSiswa = view.findViewById(R.id.tv_nama_siswa);
        tvKelas = view.findViewById(R.id.tv_kelas);
        tvJadwal = view.findViewById(R.id.tv_jadwal_hari_ini);
        tvKehadiran = view.findViewById(R.id.tv_kehadiran_status);
        tvIzin = view.findViewById(R.id.tv_izin_status);
        imgProfile = view.findViewById(R.id.imgProfile);

        // Navigasi card click
        CardView cardAbsensi = view.findViewById(R.id.card_kehadiran);
        CardView cardPerizinan = view.findViewById(R.id.card_status_izin);
        CardView cardKalender = view.findViewById(R.id.card_jadwal);
        CardView cardProfil = view.findViewById(R.id.card_profil);

        cardAbsensi.setOnClickListener(v -> navigateTo("absensi"));
        cardPerizinan.setOnClickListener(v -> navigateTo("perizinan"));
        cardKalender.setOnClickListener(v -> navigateTo("kalender"));
        cardProfil.setOnClickListener(v -> navigateTo("profil"));

        // Load data profil
        loadProfileData();
    }

    private void loadProfileData() {
        String url = BASE_URL + "?username=" + usernameOrtu;
        Log.d("DashboardFragment", "🌐 Request Profil: " + url);

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        Log.d("DashboardFragment", "✅ Response Profil: " + response);
                        if (response.getBoolean("success")) {
                            JSONObject data = response.getJSONObject("data");

                            String nama = data.getString("nama_siswa");
                            String kelas = data.getString("kelas");
                            String gender = data.getString("gender");

                            tvNamaSiswa.setText(nama);
                            tvKelas.setText(kelas.toUpperCase());
                            updateProfileIcon(gender);
                        } else {
                            Toast.makeText(getContext(), "Profil tidak ditemukan", Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        Toast.makeText(getContext(), "Kesalahan parsing data", Toast.LENGTH_SHORT).show();
                        Log.e("DashboardFragment", "❌ JSON Error: " + e.getMessage());
                    }
                },
                error -> {
                    Toast.makeText(getContext(), "Gagal koneksi ke server", Toast.LENGTH_SHORT).show();
                    Log.e("DashboardFragment", "❌ Volley Error: " + error.toString());
                });

        requestQueue.add(request);
    }

    private void updateProfileIcon(String gender) {
        if (imgProfile == null) return;
        if (gender.equalsIgnoreCase("perempuan")) {
            imgProfile.setImageResource(R.drawable.icon_cewe);
        } else {
            imgProfile.setImageResource(R.drawable.icon_cowo);
        }
    }

    private void navigateTo(String target) {
        if (getActivity() instanceof NavigasiCard) {
            ((NavigasiCard) getActivity()).navigateTo(target);
        }
    }

    private void logoutUser() {
        SharedPreferences sharedPref = requireActivity().getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE);
        sharedPref.edit().clear().apply();

        Intent intent = new Intent(getActivity(), MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }
}
