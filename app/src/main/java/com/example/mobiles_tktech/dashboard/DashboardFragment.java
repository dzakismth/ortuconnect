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

    private TextView tvNamaSiswa, tvKelas, tvJadwal, tvKehadiran, tvIzin, tvPengumuman;
    private ImageView imgProfile;
    private RequestQueue requestQueue;
    private String usernameOrtu, kelasSiswa;

    private static final String TAG = "DashboardFragment";
    private static final String BASE_PROFILE = "http://ortuconnect.atwebpages.com/api/profile.php";
    private static final String BASE_JADWAL = "http://ortuconnect.atwebpages.com/api/jadwal_hari_ini.php";
    private static final String BASE_KEHADIRAN_MINGGU = "http://ortuconnect.atwebpages.com/api/kehadiran_minggu.php";
    private static final String BASE_IZIN = "http://ortuconnect.atwebpages.com/api/izin_terbaru.php";
    private static final String BASE_AGENDA = "http://ortuconnect.atwebpages.com/api/admin/agenda.php";

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
            logoutUser();
            return;
        }

        // Inisialisasi Views
        tvNamaSiswa = view.findViewById(R.id.tv_nama_siswa);
        tvKelas = view.findViewById(R.id.tv_kelas);
        tvJadwal = view.findViewById(R.id.tv_jadwal_hari_ini);
        tvKehadiran = view.findViewById(R.id.tv_kehadiran_status);
        tvIzin = view.findViewById(R.id.tv_izin_status);
        imgProfile = view.findViewById(R.id.imgProfile);

        // Card Listeners
        view.findViewById(R.id.card_kehadiran).setOnClickListener(v -> navigateTo("absensi"));
        view.findViewById(R.id.card_status_izin).setOnClickListener(v -> navigateTo("perizinan"));
        view.findViewById(R.id.card_jadwal).setOnClickListener(v -> navigateTo("kalender"));
        view.findViewById(R.id.card_profil).setOnClickListener(v -> navigateTo("profil"));

        loadProfile();
    }

    private void loadProfile() {
        String url = BASE_PROFILE + "?username=" + usernameOrtu;
        Log.d(TAG, "Loading profile: " + url);

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        Log.d(TAG, "Profile Response: " + response.toString());

                        if (response.getBoolean("success")) {
                            JSONObject d = response.getJSONObject("data");

                            String nama = d.getString("nama_siswa");
                            kelasSiswa = d.getString("kelas");
                            String gender = d.getString("gender");

                            tvNamaSiswa.setText(nama);
                            tvKelas.setText(kelasSiswa.toUpperCase());
                            updateProfileIcon(gender);

                            // Load data lainnya
                            loadJadwalHariIni();
                            loadKehadiranMingguIni();
                            loadIzinTerbaru();
                            loadAgendaTerbaru();

                        } else {
                            String msg = response.optString("message", "Profil tidak ditemukan");
                            Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "Profile failed: " + msg);
                        }

                    } catch (JSONException e) {
                        Log.e(TAG, "JSON Parse Error: " + e.getMessage());
                        Toast.makeText(getContext(), "Error parsing data", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    String errorMsg = error.getMessage() != null ? error.getMessage() : "Unknown error";
                    Log.e(TAG, "Profile Network Error: " + errorMsg);
                    Toast.makeText(getContext(), "Koneksi gagal: " + errorMsg, Toast.LENGTH_SHORT).show();
                });

        requestQueue.add(request);
    }

    private void loadJadwalHariIni() {
        String url = BASE_JADWAL + "?kelas=" + kelasSiswa;
        Log.d(TAG, "Loading jadwal: " + url);

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                res -> {
                    try {
                        Log.d(TAG, "Jadwal Response: " + res.toString());

                        if (res.getBoolean("success")) {
                            String jadwalText = "Hari " +
                                    res.getString("hari") + ", " +
                                    res.getString("kegiatan");
                            tvJadwal.setText(jadwalText);
                            Log.d(TAG, "Jadwal set: " + jadwalText);
                        } else {
                            tvJadwal.setText("Tidak ada jadwal hari ini");
                            Log.w(TAG, "Jadwal not found");
                        }
                    } catch (JSONException e) {
                        tvJadwal.setText("Error parsing jadwal");
                        Log.e(TAG, "Jadwal Parse Error: " + e.getMessage());
                    }
                },
                err -> {
                    tvJadwal.setText("Gagal memuat jadwal");
                    Log.e(TAG, "Jadwal Network Error: " + err.toString());
                });

        requestQueue.add(request);
    }

    private void loadKehadiranMingguIni() {
        String url = BASE_KEHADIRAN_MINGGU + "?username=" + usernameOrtu;
        Log.d(TAG, "Loading kehadiran minggu: " + url);

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                res -> {
                    try {
                        Log.d(TAG, "Kehadiran Response: " + res.toString());

                        if (res.getBoolean("success")) {
                            int hadirMinggu = res.getInt("hadir");
                            String kehadiranText = "Hadir minggu ini: " + hadirMinggu + " hari";
                            tvKehadiran.setText(kehadiranText);
                            Log.d(TAG, "Kehadiran set: " + kehadiranText);
                        } else {
                            tvKehadiran.setText("Belum ada data minggu ini");
                            Log.w(TAG, "Kehadiran data not found");
                        }

                    } catch (JSONException e) {
                        tvKehadiran.setText("Error parsing kehadiran");
                        Log.e(TAG, "Kehadiran Parse Error: " + e.getMessage());
                    }
                },
                err -> {
                    tvKehadiran.setText("Gagal memuat kehadiran");
                    Log.e(TAG, "Kehadiran Network Error: " + err.toString());
                });

        requestQueue.add(request);
    }

    private void loadIzinTerbaru() {
        String url = BASE_IZIN + "?username=" + usernameOrtu;
        Log.d(TAG, "Loading izin: " + url);

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                res -> {
                    try {
                        Log.d(TAG, "Izin Response: " + res.toString());

                        if (res.getBoolean("success")) {
                            String izinText = "Izin " +
                                    res.getString("status") +
                                    ": " + res.getString("tanggal");
                            tvIzin.setText(izinText);
                            Log.d(TAG, "Izin set: " + izinText);
                        } else {
                            tvIzin.setText("Belum ada izin terbaru");
                            Log.w(TAG, "Izin data not found");
                        }
                    } catch (JSONException e) {
                        tvIzin.setText("Error parsing izin");
                        Log.e(TAG, "Izin Parse Error: " + e.getMessage());
                    }
                },
                err -> {
                    tvIzin.setText("Gagal memuat izin");
                    Log.e(TAG, "Izin Network Error: " + err.toString());
                });

        requestQueue.add(request);
    }

    private void loadAgendaTerbaru() {
        String url = BASE_AGENDA + "?month=11&year=2025";
        Log.d(TAG, "Loading agenda: " + url);

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                res -> {
                    try {
                        Log.d(TAG, "Agenda Response: " + res.toString());

                        if (res.getString("status").equals("success")) {
                            if (res.getJSONArray("data").length() > 0) {
                                JSONObject d = res.getJSONArray("data").getJSONObject(0);
                                String kegiatan = d.getString("nama_kegiatan");
                                String tanggal = d.getString("tanggal");

                                String agendaText = kegiatan + "\nTanggal: " + tanggal;

                                // ⚠️ PENTING: Gunakan TextView terpisah untuk pengumuman
                                // Jika ingin tampil di tvJadwal, maka akan timpa jadwal hari ini
                                // Lebih baik buat TextView baru: tvPengumuman

                                // Sementara pakai Toast atau Log
                                Toast.makeText(getContext(), "Agenda: " + agendaText, Toast.LENGTH_LONG).show();
                                Log.d(TAG, "Agenda: " + agendaText);

                                // Jika sudah ada tvPengumuman:
                                // tvPengumuman.setText(agendaText);
                            } else {
                                Log.w(TAG, "No agenda data");
                            }
                        } else {
                            Log.w(TAG, "Agenda status failed");
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Agenda Parse Error: " + e.getMessage());
                    }
                },
                err -> {
                    Log.e(TAG, "Agenda Network Error: " + err.toString());
                });

        requestQueue.add(request);
    }

    private void updateProfileIcon(String gender) {
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
        SharedPreferences prefs = requireActivity().getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE);
        prefs.edit().clear().apply();

        Intent i = new Intent(getActivity(), MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        requireActivity().finish();
    }
}