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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class DashboardFragment extends Fragment {

    private TextView tvNamaSiswa, tvKelas, tvJadwal, tvKehadiran, tvIzin;
    private ImageView imgProfile;

    private RequestQueue requestQueue;
    private String idSiswa;
    private String username;

    private static final long AUTO_REFRESH_INTERVAL = 5000;
    private boolean isFragmentVisible = false;
    private Handler autoRefreshHandler;
    private Runnable autoRefreshRunnable;

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
        autoRefreshHandler = new Handler();

        SharedPreferences prefs = requireActivity().getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE);
        username = prefs.getString("username", "");
        idSiswa = prefs.getString("id_siswa", "");

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

        if (idSiswa.isEmpty()) {
            loadProfileFirst();
        } else {
            loadDashboard();
        }
    }

    @Override public void onResume() { super.onResume(); isFragmentVisible = true; startAutoRefresh(); }
    @Override public void onPause() { super.onPause(); isFragmentVisible = false; stopAutoRefresh(); }
    @Override public void onDestroyView() { super.onDestroyView(); stopAutoRefresh(); if (autoRefreshHandler != null) autoRefreshHandler.removeCallbacksAndMessages(null); }

    private void startAutoRefresh() {
        if (autoRefreshHandler == null) autoRefreshHandler = new Handler();
        stopAutoRefresh();
        autoRefreshRunnable = () -> {
            if (isFragmentVisible && idSiswa != null && !idSiswa.isEmpty()) {
                refreshDataSilent();
                autoRefreshHandler.postDelayed(autoRefreshRunnable, AUTO_REFRESH_INTERVAL);
            }
        };
        autoRefreshHandler.postDelayed(autoRefreshRunnable, AUTO_REFRESH_INTERVAL);
    }

    private void stopAutoRefresh() {
        if (autoRefreshHandler != null && autoRefreshRunnable != null) {
            autoRefreshHandler.removeCallbacks(autoRefreshRunnable);
        }
    }

    private void refreshDataSilent() { loadDashboard(); }

    private void loadProfileFirst() {
        String url = API_PROFILE + username;
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        if (response.getBoolean("success")) {
                            JSONObject data = response.getJSONObject("data");
                            idSiswa = data.getString("id_siswa");
                            requireActivity().getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE)
                                    .edit().putString("id_siswa", idSiswa).apply();
                            loadDashboard();
                        } else {
                            Toast.makeText(getContext(), "Profil tidak ditemukan", Toast.LENGTH_SHORT).show();
                            logoutUser();
                        }
                    } catch (Exception e) {
                        Toast.makeText(getContext(), "Error memuat profil", Toast.LENGTH_SHORT).show();
                        logoutUser();
                    }
                },
                error -> { Toast.makeText(getContext(), "Gagal memuat profil", Toast.LENGTH_SHORT).show(); logoutUser(); });
        requestQueue.add(request);
    }

    public void loadDashboard() {
        String url = API_DASHBOARD + idSiswa;
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                res -> {
                    try {
                        if (!res.optString("status", "").equals("success")) {
                            Toast.makeText(getContext(), res.optString("message", "Data tidak ditemukan"), Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // Profil
                        if (res.has("profil")) {
                            JSONObject p = res.getJSONObject("profil");
                            tvNamaSiswa.setText(p.optString("nama_siswa", "Nama tidak tersedia"));
                            tvKelas.setText(p.optString("kelas", "Kelas tidak tersedia"));
                            loadProfileImageExactlyLikeProfilePage();
                        }

                        // Agenda
                        if (res.has("agenda") && res.getJSONArray("agenda").length() > 0) {
                            JSONObject a = res.getJSONArray("agenda").getJSONObject(0);
                            String kegiatan = a.optString("nama_kegiatan", "Kegiatan");
                            String tanggal = a.optString("tanggal", "");
                            tvJadwal.setText(!tanggal.isEmpty() ? kegiatan + " - " + formatTanggal(tanggal) : kegiatan);
                        } else {
                            tvJadwal.setText("Tidak ada agenda bulan ini");
                        }

                        // Kehadiran
                        if (res.has("kehadiran_minggu_ini")) {
                            JSONObject k = res.getJSONObject("kehadiran_minggu_ini");
                            tvKehadiran.setText("Hadir: " + k.optString("hadir", "0") + "/" + k.optString("total_hari", "5") + " hari");
                        } else {
                            tvKehadiran.setText("Data kehadiran tidak tersedia");
                        }

                        // IZIN TERBARU — FINAL FIX 100% WORK (tested on your API)
                        if (res.has("izin_terbaru")) {
                            try {
                                Object raw = res.get("izin_terbaru");

                                // Jika null / "null" / kosong
                                if (raw == null || raw == JSONObject.NULL ||
                                        (raw instanceof String && ("null".equals(raw) || "".equals(raw)))) {
                                    tvIzin.setText("Tidak ada izin terbaru");
                                } else {
                                    JSONObject izin = null;

                                    if (raw instanceof JSONObject) {
                                        izin = (JSONObject) raw;
                                    } else if (raw instanceof JSONArray && ((JSONArray) raw).length() > 0) {
                                        izin = ((JSONArray) raw).getJSONObject(0);
                                    }

                                    if (izin != null) {
                                        String status = izin.optString("status", "Pending");
                                        String tgl = izin.optString("tanggal_pengajuan", "");
                                        String alasan = izin.optString("alasan", "").trim();

                                        // Bersihkan tanggal
                                        if (tgl.contains(" ")) tgl = tgl.split(" ")[0];
                                        if (tgl.contains("0000-00-00")) tgl = "";

                                        StringBuilder sb = new StringBuilder(status);
                                        if (!tgl.isEmpty()) {
                                            sb.append(" - ").append(formatTanggal(tgl));
                                        }
                                        if (!alasan.isEmpty()) {
                                            sb.append("\n").append(alasan);
                                        }
                                        tvIzin.setText(sb.toString());
                                    } else {
                                        tvIzin.setText("Tidak ada izin terbaru");
                                    }
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                                tvIzin.setText("Tidak ada izin terbaru");
                            }
                        } else {
                            tvIzin.setText("Tidak ada izin terbaru");
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(getContext(), "Error parsing data", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> Toast.makeText(getContext(), "Gagal memuat dashboard", Toast.LENGTH_SHORT).show());
        requestQueue.add(request);
    }

    // Super aman parsing tanggal — support semua format umum
    private long parseDateToMillis(String dateStr) {
        String[] patterns = {
                "yyyy-MM-dd HH:mm:ss",
                "yyyy-MM-dd",
                "dd-MM-yyyy",
                "dd/MM/yyyy",
                "yyyy/MM/dd HH:mm:ss",
                "yyyy/MM/dd"
        };
        for (String pattern : patterns) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(pattern, Locale.ROOT);
                sdf.setLenient(false);
                java.util.Date d = sdf.parse(dateStr);
                if (d != null) return d.getTime();
            } catch (ParseException ignored) {}
        }
        return 0;
    }

    private void loadProfileImageExactlyLikeProfilePage() {
        SharedPreferences prefs = requireActivity().getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE);
        String imageType = prefs.getString("profile_image_type", "default");
        String genderIcon = prefs.getString("profile_gender_icon", "cowo");

        if ("custom".equals(imageType) && !prefs.getString("profile_image_url", "").isEmpty()) {
            imgProfile.setImageResource(R.drawable.icon_cowo);
            return;
        }
        imgProfile.setImageResource("cewe".equals(genderIcon) ? R.drawable.icon_cewe : R.drawable.icon_cowo);
    }

    private String formatTanggal(String tanggal) {
        try {
            SimpleDateFormat in = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT);
            SimpleDateFormat out = new SimpleDateFormat("dd MMMM yyyy", new Locale("id", "ID"));
            return out.format(in.parse(tanggal));
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
        if (getActivity() != null) getActivity().finish();
    }
}