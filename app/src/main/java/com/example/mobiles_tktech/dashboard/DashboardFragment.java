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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DashboardFragment extends Fragment {

    private static final String TAG = "DashboardFragment";
    private TextView tvNamaSiswa, tvKelas, tvAgendaTitle, tvAgendaDate, tvKehadiran, tvIzin;
    private ImageView imgProfile;

    private RequestQueue requestQueue;
    private String idSiswa;
    private String username;

    private boolean hasLoadedOnce = false;

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

        SharedPreferences prefs = requireActivity().getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE);
        username = prefs.getString("username", "");
        idSiswa = prefs.getString("id_siswa", "");

        Log.d(TAG, "Username: " + username + ", ID Siswa: " + idSiswa);

        if (username.isEmpty()) {
            logoutUser();
            return;
        }

        tvNamaSiswa = view.findViewById(R.id.tv_nama_siswa);
        tvKelas = view.findViewById(R.id.tv_kelas);
        tvAgendaTitle = view.findViewById(R.id.tv_agenda_title);
        tvAgendaDate = view.findViewById(R.id.tv_agenda_date);
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

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "Fragment RESUMED");
        loadProfileImageExactlyLikeProfilePage();

        if (!hasLoadedOnce && !idSiswa.isEmpty()) {
            loadDashboard();
            hasLoadedOnce = true;
        }
    }

    private void loadProfileFirst() {
        String url = API_PROFILE + username;
        Log.d(TAG, "Loading profile: " + url);

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        if (response.getBoolean("success")) {
                            JSONObject data = response.getJSONObject("data");
                            idSiswa = data.getString("id_siswa");

                            requireActivity().getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE)
                                    .edit().putString("id_siswa", idSiswa).apply();

                            Log.d(TAG, "ID Siswa obtained: " + idSiswa);
                            loadDashboard();
                            hasLoadedOnce = true;
                        } else {
                            Toast.makeText(getContext(), "Profil tidak ditemukan", Toast.LENGTH_SHORT).show();
                            logoutUser();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Profile parse error: " + e.getMessage());
                        Toast.makeText(getContext(), "Error memuat profil", Toast.LENGTH_SHORT).show();
                        logoutUser();
                    }
                },
                error -> {
                    Log.e(TAG, "Profile network error: " + error.toString());
                    Toast.makeText(getContext(), "Gagal memuat profil", Toast.LENGTH_SHORT).show();
                    logoutUser();
                });
        requestQueue.add(request);
    }

    public void loadDashboard() {
        String url = API_DASHBOARD + idSiswa;
        Log.d(TAG, "Loading dashboard: " + url);

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                res -> {
                    try {
                        if (!res.optString("status", "").equals("success")) {
                            Toast.makeText(getContext(), res.optString("message", "Data tidak ditemukan"), Toast.LENGTH_SHORT).show();
                            return;
                        }

                        if (res.has("profil")) {
                            JSONObject p = res.getJSONObject("profil");
                            tvNamaSiswa.setText(p.optString("nama_siswa", "Nama tidak tersedia"));
                            tvKelas.setText(p.optString("kelas", "Kelas tidak tersedia"));
                            loadProfileImageExactlyLikeProfilePage();
                        }

                        displayLatestAgenda(res);

                        if (res.has("kehadiran_minggu_ini")) {
                            JSONObject k = res.getJSONObject("kehadiran_minggu_ini");
                            String hadir = k.optString("hadir", "0");
                            String total = k.optString("total_hari", "5");
                            tvKehadiran.setText("Hadir: " + hadir + "/" + total + " hari");
                        } else {
                            tvKehadiran.setText("Data kehadiran tidak tersedia");
                        }

                        displayLatestIzin(res);

                    } catch (Exception e) {
                        Log.e(TAG, "Dashboard parse error: " + e.getMessage());
                        Toast.makeText(getContext(), "Error parsing data", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    Log.e(TAG, "Dashboard network error: " + error.toString());
                    Toast.makeText(getContext(), "Gagal memuat dashboard", Toast.LENGTH_SHORT).show();
                });
        requestQueue.add(request);
    }

    private void displayLatestAgenda(JSONObject res) {
        try {
            if (!res.has("agenda")) {
                tvAgendaTitle.setText("Tidak ada agenda/pengumuman");
                tvAgendaDate.setText("");
                return;
            }

            JSONArray agendaArray = res.getJSONArray("agenda");

            if (agendaArray.length() == 0) {
                tvAgendaTitle.setText("Tidak ada agenda/pengumuman");
                tvAgendaDate.setText("");
                return;
            }

            JSONObject latestAgenda = null;
            long latestTime = 0;

            for (int i = 0; i < agendaArray.length(); i++) {
                JSONObject agenda = agendaArray.getJSONObject(i);
                String tanggal = agenda.optString("tanggal", "");

                if (!tanggal.isEmpty()) {
                    long time = parseDateToMillis(tanggal);
                    if (time > latestTime) {
                        latestTime = time;
                        latestAgenda = agenda;
                    }
                }
            }

            if (latestAgenda != null) {
                String kegiatan = latestAgenda.optString("nama_kegiatan", "Kegiatan");
                String tanggal = latestAgenda.optString("tanggal", "");

                tvAgendaTitle.setText(kegiatan);

                if (!tanggal.isEmpty()) {
                    String formattedDate = formatTanggal(tanggal);
                    tvAgendaDate.setText(formattedDate);
                } else {
                    tvAgendaDate.setText("");
                }
            } else {
                tvAgendaTitle.setText("Tidak ada agenda/pengumuman");
                tvAgendaDate.setText("");
            }

        } catch (Exception e) {
            Log.e(TAG, "Error displaying agenda: " + e.getMessage());
            tvAgendaTitle.setText("Error memuat agenda");
            tvAgendaDate.setText("");
        }
    }

    // DIPERBAIKI: TIDAK PERNAH TAMPILKAN "null" LAGI
    private void displayLatestIzin(JSONObject res) {
        try {
            // Jika tidak ada key "izin_terbaru" sama sekali
            if (!res.has("izin_terbaru")) {
                tvIzin.setText("Belum ada izin terbaru");
                return;
            }

            Object rawIzin = res.get("izin_terbaru");

            // Cek apakah benar-benar null / kosong / string "null"
            boolean isEmpty = rawIzin == null
                    || rawIzin == JSONObject.NULL
                    || (rawIzin instanceof String && ("null".equalsIgnoreCase(rawIzin.toString()) || rawIzin.toString().trim().isEmpty()));

            if (isEmpty) {
                tvIzin.setText("Belum ada izin terbaru");
                return;
            }

            JSONObject latestIzin = null;

            if (rawIzin instanceof JSONObject) {
                latestIzin = (JSONObject) rawIzin;
            } else if (rawIzin instanceof JSONArray) {
                JSONArray izinArray = (JSONArray) rawIzin;
                if (izinArray.length() == 0) {
                    tvIzin.setText("Belum ada izin terbaru");
                    return;
                }

                long latestTime = 0;
                for (int i = 0; i < izinArray.length(); i++) {
                    JSONObject izin = izinArray.getJSONObject(i);
                    String tanggalPengajuan = izin.optString("tanggal_pengajuan", "");
                    if (!tanggalPengajuan.isEmpty()) {
                        long time = parseDateToMillis(tanggalPengajuan);
                        if (time > latestTime) {
                            latestTime = time;
                            latestIzin = izin;
                        }
                    }
                }
            }

            if (latestIzin == null) {
                tvIzin.setText("Belum ada izin terbaru");
                return;
            }

            // Ambil data dengan aman
            String status = latestIzin.optString("status", "Pending");
            String jenisIzin = latestIzin.optString("jenis_izin", "");
            String tanggalPengajuan = latestIzin.optString("tanggal_pengajuan", "");
            String alasan = latestIzin.optString("alasan", "");

            StringBuilder display = new StringBuilder();
            display.append(status.isEmpty() ? "Pending" : status);

            if (!jenisIzin.isEmpty()) {
                display.append(" (").append(jenisIzin).append(")");
            }

            if (!tanggalPengajuan.isEmpty()) {
                String tanggalOnly = tanggalPengajuan.split(" ")[0];
                if (!tanggalOnly.contains("0000") && !tanggalOnly.contains("null")) {
                    String formattedDate = formatTanggal(tanggalOnly);
                    display.append("\n").append(formattedDate);
                }
            }

            if (!alasan.isEmpty() && alasan.length() <= 50 && !alasan.equalsIgnoreCase("null")) {
                display.append("\n").append(alasan);
            }

            tvIzin.setText(display.toString());
            Log.d(TAG, "Latest Izin: " + display.toString());

        } catch (Exception e) {
            Log.e(TAG, "Error displaying izin: " + e.getMessage());
            tvIzin.setText("Belum ada izin terbaru");
        }
    }

    private void loadProfileImageExactlyLikeProfilePage() {
        if (getContext() == null || imgProfile == null) return;

        SharedPreferences prefs = requireActivity().getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE);
        String imageType = prefs.getString("profile_image_type", "default");
        String genderIcon = prefs.getString("profile_gender_icon", "cowo");

        if ("custom".equals(imageType)) {
            String imageUrl = prefs.getString("profile_image_url", "");
            if (!imageUrl.isEmpty()) {
                imgProfile.setImageResource(R.drawable.icon_cowo);
            } else {
                imgProfile.setImageResource("cewe".equals(genderIcon) ? R.drawable.icon_cewe : R.drawable.icon_cowo);
            }
        } else {
            imgProfile.setImageResource("cewe".equals(genderIcon) ? R.drawable.icon_cewe : R.drawable.icon_cowo);
        }

        Log.d(TAG, "Foto profil diperbarui → gender: " + genderIcon);
    }

    private long parseDateToMillis(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) return 0;

        String[] patterns = {
                "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd",
                "dd-MM-yyyy HH:mm:ss", "dd-MM-yyyy",
                "dd/MM/yyyy HH:mm:ss", "dd/MM/yyyy",
                "yyyy/MM/dd HH:mm:ss", "yyyy/MM/dd"
        };

        for (String pattern : patterns) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(pattern, Locale.ROOT);
                sdf.setLenient(false);
                Date date = sdf.parse(dateStr.trim());
                if (date != null) return date.getTime();
            } catch (ParseException ignored) {}
        }
        return 0;
    }

    private String formatTanggal(String tanggal) {
        try {
            SimpleDateFormat input = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT);
            SimpleDateFormat output = new SimpleDateFormat("dd MMMM yyyy", new Locale("id", "ID"));
            Date date = input.parse(tanggal);
            return date != null ? output.format(date) : tanggal;
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

        Intent intent = new Intent(getActivity(), MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        if (getActivity() != null) getActivity().finish();
    }
}