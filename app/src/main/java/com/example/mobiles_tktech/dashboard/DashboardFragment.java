package com.example.mobiles_tktech.dashboard;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class DashboardFragment extends Fragment {

    private TextView tvNamaSiswa, tvKelas, tvJadwal, tvKehadiran, tvIzin;
    private ImageView imgProfile;

    private RequestQueue requestQueue;
    private String idSiswa;
    private String username;

    // Auto refresh variables - FIXED
    private static final long AUTO_REFRESH_INTERVAL = 5000; // 5 detik
    private boolean isFragmentVisible = false;
    private Handler autoRefreshHandler;
    private Runnable autoRefreshRunnable;

    private static final String TAG = "DashboardFragment";
    private static final String API_DASHBOARD = "https://ortuconnect.pbltifnganjuk.com/api/dashboard.php?id_siswa=";
    private static final String API_PROFILE = "https://ortuconnect.pbltifnganjuk.com/api/profile.php?username=";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        requestQueue = Volley.newRequestQueue(requireContext());

        // Initialize auto refresh handler
        autoRefreshHandler = new Handler();

        // Ambil username dan id_siswa dari SharedPreferences
        SharedPreferences prefs = requireActivity().getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE);
        username = prefs.getString("username", "");
        idSiswa = prefs.getString("id_siswa", "");

        Log.d(TAG, "Username from prefs: " + username);
        Log.d(TAG, "ID Siswa from prefs: " + idSiswa);

        // Jika username kosong, langsung logout
        if (username.isEmpty()) {
            logoutUser();
            return;
        }

        tvNamaSiswa = view.findViewById(R.id.tv_nama_siswa);
        tvKelas = view.findViewById(R.id.tv_kelas);
        tvJadwal = view.findViewById(R.id.tv_agenda);
        tvKehadiran = view.findViewById(R.id.tv_kehadiran_status);
        tvIzin = view.findViewById(R.id.tv_izin_status);
        imgProfile = view.findViewById(R.id.imgProfile);

        view.findViewById(R.id.card_kehadiran).setOnClickListener(v -> navigateTo("absensi"));
        view.findViewById(R.id.card_status_izin).setOnClickListener(v -> navigateTo("perizinan"));
        view.findViewById(R.id.card_jadwal).setOnClickListener(v -> navigateTo("kalender"));
        view.findViewById(R.id.card_profil).setOnClickListener(v -> navigateTo("profil"));

        // Jika id_siswa tidak ada, load profile dulu
        if (idSiswa.isEmpty()) {
            loadProfileFirst();
        } else {
            loadDashboard();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        isFragmentVisible = true;
        Log.d(TAG, "🎯 Fragment RESUMED - starting auto refresh");
        startAutoRefresh();
    }

    @Override
    public void onPause() {
        super.onPause();
        isFragmentVisible = false;
        Log.d(TAG, "🚫 Fragment PAUSED - stopping auto refresh");
        stopAutoRefresh();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "🗑️ Fragment DESTROYED - cleaning up");
        stopAutoRefresh();
        if (autoRefreshHandler != null) {
            autoRefreshHandler.removeCallbacksAndMessages(null);
        }
    }

    /**
     * Start auto refresh - FIXED VERSION
     */
    private void startAutoRefresh() {
        if (autoRefreshHandler == null) {
            autoRefreshHandler = new Handler();
        }

        // Hapus callback yang mungkin masih aktif
        stopAutoRefresh();

        // Buat runnable untuk auto refresh
        autoRefreshRunnable = new Runnable() {
            @Override
            public void run() {
                if (isFragmentVisible && idSiswa != null && !idSiswa.isEmpty()) {
                    Log.d(TAG, "🔄 Auto refresh triggered");
                    refreshDataSilent();

                    // Schedule next refresh hanya jika fragment masih visible
                    if (isFragmentVisible && autoRefreshHandler != null) {
                        autoRefreshHandler.postDelayed(this, AUTO_REFRESH_INTERVAL);
                        Log.d(TAG, "📅 Next refresh scheduled in " + (AUTO_REFRESH_INTERVAL/1000) + " seconds");
                    }
                } else {
                    Log.d(TAG, "⏸️ Auto refresh skipped - fragment not visible or no ID");
                }
            }
        };

        // Mulai auto refresh
        if (autoRefreshHandler != null) {
            autoRefreshHandler.postDelayed(autoRefreshRunnable, AUTO_REFRESH_INTERVAL);
            Log.d(TAG, "▶️ Auto refresh started successfully");
        }
    }

    /**
     * Stop auto refresh - FIXED VERSION
     */
    private void stopAutoRefresh() {
        Log.d(TAG, "⏹️ Stopping auto refresh");
        if (autoRefreshHandler != null && autoRefreshRunnable != null) {
            autoRefreshHandler.removeCallbacks(autoRefreshRunnable);
        }
    }

    /**
     * Refresh data tanpa animation (silent)
     */
    private void refreshDataSilent() {
        Log.d(TAG, "🔄 Silent refresh started");
        loadDashboard();
    }

    private void loadProfileFirst() {
        String url = API_PROFILE + username;
        Log.d(TAG, "Loading profile first: " + url);

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        Log.d(TAG, "Profile Response: " + response.toString());

                        if (response.getBoolean("success")) {
                            JSONObject data = response.getJSONObject("data");
                            idSiswa = data.getString("id_siswa");

                            // Simpan id_siswa ke SharedPreferences untuk penggunaan berikutnya
                            SharedPreferences prefs = requireActivity().getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE);
                            prefs.edit().putString("id_siswa", idSiswa).apply();

                            Log.d(TAG, "ID Siswa obtained: " + idSiswa);

                            // Setelah dapat id_siswa, load dashboard
                            loadDashboard();

                        } else {
                            Toast.makeText(getContext(), "Profil tidak ditemukan", Toast.LENGTH_SHORT).show();
                            logoutUser();
                        }

                    } catch (Exception e) {
                        Log.e(TAG, "Profile Parse Error: " + e.getMessage());
                        Toast.makeText(getContext(), "Error memuat profil", Toast.LENGTH_SHORT).show();
                        logoutUser();
                    }
                },
                error -> {
                    Log.e(TAG, "Profile Network Error: " + error.toString());
                    Toast.makeText(getContext(), "Gagal memuat profil", Toast.LENGTH_SHORT).show();
                    logoutUser();
                });

        requestQueue.add(request);
    }

    private void loadDashboard() {
        String url = API_DASHBOARD + idSiswa;
        Log.d(TAG, "Dashboard API: " + url);

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                res -> {
                    try {
                        Log.d(TAG, "=== RAW DASHBOARD RESPONSE ===");
                        Log.d(TAG, res.toString(2)); // Pretty print JSON

                        if (!res.getString("status").equals("success")) {
                            String message = res.optString("message", "Data tidak ditemukan");
                            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // === PROFIL ===
                        if (res.has("profil")) {
                            JSONObject p = res.getJSONObject("profil");
                            tvNamaSiswa.setText(p.optString("nama_siswa", "Nama tidak tersedia"));
                            tvKelas.setText(p.optString("kelas", "Kelas tidak tersedia"));

                            // Set icon berdasarkan gender jika ada
                            String gender = p.optString("jenis_kelamin", "").toLowerCase();
                            if (gender.equals("perempuan")) {
                                imgProfile.setImageResource(R.drawable.icon_cewe);
                            } else {
                                imgProfile.setImageResource(R.drawable.icon_cowo);
                            }
                        }

                        // === AGENDA BULAN INI ===
                        if (res.has("agenda")) {
                            JSONArray agendaArr = res.getJSONArray("agenda");
                            if (agendaArr.length() > 0) {
                                JSONObject a = agendaArr.getJSONObject(0);
                                String kegiatan = a.optString("nama_kegiatan", "Kegiatan");
                                String tanggal = a.optString("tanggal", "");

                                // Format tanggal jika perlu
                                if (!tanggal.isEmpty()) {
                                    String formattedDate = formatTanggal(tanggal);
                                    tvJadwal.setText(kegiatan + " - " + formattedDate);
                                } else {
                                    tvJadwal.setText(kegiatan);
                                }
                            } else {
                                tvJadwal.setText("Tidak ada agenda bulan ini");
                            }
                        } else {
                            tvJadwal.setText("Agenda tidak tersedia");
                        }

                        // === KEHADIRAN MINGGU INI ===
                        if (res.has("kehadiran_minggu_ini")) {
                            JSONObject k = res.getJSONObject("kehadiran_minggu_ini");
                            String hadir = k.optString("hadir", "0");
                            String total = k.optString("total_hari", "5");
                            tvKehadiran.setText("Hadir: " + hadir + "/" + total + " hari");
                        } else {
                            tvKehadiran.setText("Data kehadiran tidak tersedia");
                        }

                        // === IZIN TERBARU ===
                        if (res.has("izin_terbaru")) {
                            JSONObject izinObj = res.getJSONObject("izin_terbaru");

                            // Cek jika ada data izin
                            if (!izinObj.isNull("status") && !izinObj.getString("status").equals("null")) {
                                String status = izinObj.optString("status", "Pending");
                                String tanggal = izinObj.optString("tanggal_pengajuan", "");
                                String alasan = izinObj.optString("alasan", "");

                                if (!tanggal.isEmpty()) {
                                    String formattedDate = formatTanggal(tanggal);
                                    tvIzin.setText(status + " - " + formattedDate);
                                } else {
                                    tvIzin.setText(status);
                                }

                                // Tambahkan alasan jika ada
                                if (!alasan.isEmpty()) {
                                    tvIzin.append("\n" + alasan);
                                }
                            } else {
                                tvIzin.setText("Tidak ada izin terbaru");
                            }
                        } else {
                            tvIzin.setText("Data izin tidak tersedia");
                        }

                    } catch (JSONException e) {
                        Log.e(TAG, "Parse Error: " + e.getMessage());
                        e.printStackTrace();
                        Toast.makeText(getContext(), "Error parsing data dashboard", Toast.LENGTH_SHORT).show();
                    }
                },
                err -> {
                    Log.e(TAG, "Network Error: " + err.toString());
                    if (err.networkResponse != null) {
                        Log.e(TAG, "Status Code: " + err.networkResponse.statusCode);
                    }
                    Toast.makeText(getContext(), "Gagal memuat dashboard", Toast.LENGTH_SHORT).show();
                }
        );

        requestQueue.add(request);
    }

    /**
     * Format tanggal dari YYYY-MM-DD ke format Indonesia
     */
    private String formatTanggal(String tanggal) {
        try {
            java.text.SimpleDateFormat inputFormat = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
            java.text.SimpleDateFormat outputFormat = new java.text.SimpleDateFormat("dd MMMM yyyy", new java.util.Locale("id", "ID"));
            return outputFormat.format(inputFormat.parse(tanggal));
        } catch (Exception e) {
            return tanggal;
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
        if (getActivity() != null) {
            getActivity().finish();
        }
    }
}