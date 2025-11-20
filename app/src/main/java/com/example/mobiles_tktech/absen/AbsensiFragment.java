package com.example.mobiles_tktech.absen;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.mobiles_tktech.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;

public class AbsensiFragment extends Fragment {

    private static final String TAG = "AbsensiFragment";
    private static final String BASE_PROFILE = "http://ortuconnect.atwebpages.com/api/profile.php";
    private static final String BASE_ABSENSI = "http://ortuconnect.atwebpages.com/api/admin/absensi.php";

    Spinner spinnerBulan;
    RecyclerView rvAbsensi;
    AbsensiAdapter adapter;
    ArrayList<AbsensiModel> listAbsensi = new ArrayList<>();
    TextView tvEmptyState;

    String usernameOrtu;
    String idSiswa;
    int selectedYear;
    int selectedMonth;

    // Tambahkan flag untuk melacak status fragment
    private boolean isFragmentActive = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_absensi, container, false);

        isFragmentActive = true;

        // Get username dari SharedPreferences
        SharedPreferences prefs = requireActivity().getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE);
        usernameOrtu = prefs.getString("username", "");

        if (usernameOrtu.isEmpty()) {
            showToast("Username tidak ditemukan");
            return view;
        }

        // Back button - PERBAIKAN DI SINI
        ImageButton btnBack = view.findViewById(R.id.btn_back_header);
        btnBack.setOnClickListener(v -> {
            handleBackButton();
        });

        spinnerBulan = view.findViewById(R.id.spinner_bulan);
        rvAbsensi = view.findViewById(R.id.rv_absensi_list);
        tvEmptyState = view.findViewById(R.id.tv_empty_state);

        // Setup RecyclerView
        rvAbsensi.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new AbsensiAdapter(listAbsensi);
        rvAbsensi.setAdapter(adapter);

        // Setup Spinner dengan bulan
        setupSpinnerBulan();

        // Set bulan dan tahun saat ini
        Calendar cal = Calendar.getInstance();
        selectedYear = cal.get(Calendar.YEAR);
        selectedMonth = cal.get(Calendar.MONTH); // 0-11

        // Set spinner ke bulan sekarang
        spinnerBulan.setSelection(selectedMonth);

        spinnerBulan.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                selectedMonth = position;
                loadProfile(); // Load profile dulu untuk dapat id_siswa
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });

        // Load data pertama kali
        loadProfile();

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        isFragmentActive = false;
    }

    // ==========================================================
    //                  HANDLE BACK BUTTON - METODE BARU
    // ==========================================================
    private void handleBackButton() {
        try {
            // Coba metode 1: Navigate menggunakan NavigasiCard
            if (getActivity() instanceof com.example.mobiles_tktech.navigasi.NavigasiCard) {
                ((com.example.mobiles_tktech.navigasi.NavigasiCard) getActivity()).navigateToDashboard();

                // Juga update bottom navigation
                BottomNavigationView bottomNav = requireActivity().findViewById(R.id.bottom_nav_card);
                if (bottomNav != null) {
                    bottomNav.setSelectedItemId(R.id.nav_beranda);
                }
            }
            // Metode 2: Gunakan requireActivity() untuk finish
            else if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error handling back button: " + e.getMessage());

            // Metode 3: Fallback sederhana
            try {
                if (getActivity() != null) {
                    getActivity().onBackPressed();
                }
            } catch (Exception ex) {
                Log.e(TAG, "Fallback also failed: " + ex.getMessage());
            }
        }
    }

    // ==========================================================
    //                  SAFE TOAST METHOD
    // ==========================================================
    private void showToast(String message) {
        if (isFragmentActive && getContext() != null) {
            try {
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Log.e(TAG, "Error showing toast: " + e.getMessage());
            }
        }
    }

    // ==========================================================
    //                  SETUP SPINNER BULAN
    // ==========================================================
    private void setupSpinnerBulan() {
        String[] bulanArray = {
                "Januari", "Februari", "Maret", "April", "Mei", "Juni",
                "Juli", "Agustus", "September", "Oktober", "November", "Desember"
        };

        ArrayAdapter<String> adapterBulan = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                bulanArray
        );
        adapterBulan.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerBulan.setAdapter(adapterBulan);
    }

    // ==========================================================
    //                  LOAD PROFILE (GET ID SISWA)
    // ==========================================================
    private void loadProfile() {
        if (!isFragmentActive || getContext() == null) {
            return;
        }

        String url = BASE_PROFILE + "?username=" + usernameOrtu;
        Log.d(TAG, "Loading profile: " + url);

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    if (!isFragmentActive) return;

                    try {
                        Log.d(TAG, "Profile Response: " + response.toString());

                        if (response.getBoolean("success")) {
                            JSONObject data = response.getJSONObject("data");
                            idSiswa = data.getString("id_siswa");

                            Log.d(TAG, "ID Siswa: " + idSiswa);

                            // Setelah dapat id_siswa, load absensi
                            loadAbsensi();

                        } else {
                            showToast("Profil tidak ditemukan");
                        }

                    } catch (Exception e) {
                        Log.e(TAG, "Profile Parse Error: " + e.getMessage());
                        showToast("Error parsing profile");
                    }
                },
                error -> {
                    if (!isFragmentActive) return;

                    Log.e(TAG, "Profile Network Error: " + error.toString());
                    showToast("Gagal memuat profil");
                });

        Volley.newRequestQueue(requireContext()).add(request);
    }

    // ==========================================================
    //                  LOAD ABSENSI (API BARU)
    // ==========================================================
    private void loadAbsensi() {
        if (!isFragmentActive || getContext() == null) {
            return;
        }

        if (idSiswa == null || idSiswa.isEmpty()) {
            Log.e(TAG, "ID Siswa belum tersedia");
            return;
        }

        // Format bulan untuk API baru: YYYY-MM (contoh: 2025-11)
        String bulanTahun = String.format(Locale.getDefault(), "%d-%02d", selectedYear, (selectedMonth + 1));

        String url = BASE_ABSENSI + "?id_siswa=" + idSiswa + "&bulan=" + bulanTahun;

        Log.d(TAG, "=== LOADING ABSENSI (API BARU) ===");
        Log.d(TAG, "URL: " + url);
        Log.d(TAG, "ID Siswa: " + idSiswa);
        Log.d(TAG, "Bulan-Tahun: " + bulanTahun);

        RequestQueue queue = Volley.newRequestQueue(requireContext());

        StringRequest request = new StringRequest(Request.Method.GET, url,
                response -> {
                    if (!isFragmentActive) return;

                    try {
                        Log.d(TAG, "=== RAW RESPONSE (API BARU) ===");
                        Log.d(TAG, response);

                        listAbsensi.clear();

                        JSONObject json = new JSONObject(response);

                        // Handle response berdasarkan format baru
                        if (json.has("status") && json.getString("status").equals("success")) {
                            JSONArray riwayatArray = json.getJSONArray("riwayat");
                            Log.d(TAG, "Jumlah data riwayat: " + riwayatArray.length());

                            for (int i = 0; i < riwayatArray.length(); i++) {
                                JSONObject o = riwayatArray.getJSONObject(i);

                                String tanggal = o.getString("tanggal");
                                String status = o.getString("status");

                                Log.d(TAG, "Item " + i + " - Tanggal: " + tanggal + ", Status: " + status);

                                if (!tanggal.isEmpty() && !status.isEmpty()) {
                                    listAbsensi.add(new AbsensiModel(tanggal, status));
                                }
                            }

                            // ✅ Urutkan berdasarkan tanggal TERBARU (descending)
                            Collections.sort(listAbsensi, new Comparator<AbsensiModel>() {
                                @Override
                                public int compare(AbsensiModel a1, AbsensiModel a2) {
                                    return a2.tanggal.compareTo(a1.tanggal);
                                }
                            });

                            adapter.notifyDataSetChanged();

                            // Show/hide empty state
                            if (listAbsensi.isEmpty()) {
                                rvAbsensi.setVisibility(View.GONE);
                                tvEmptyState.setVisibility(View.VISIBLE);
                                tvEmptyState.setText("Tidak ada data absensi bulan ini");
                                Log.d(TAG, "Data kosong setelah parsing");
                            } else {
                                rvAbsensi.setVisibility(View.VISIBLE);
                                tvEmptyState.setVisibility(View.GONE);
                                Log.d(TAG, "Menampilkan " + listAbsensi.size() + " data");
                            }

                        } else {
                            // Response tidak success
                            Log.e(TAG, "Response status tidak success");
                            rvAbsensi.setVisibility(View.GONE);
                            tvEmptyState.setVisibility(View.VISIBLE);
                            tvEmptyState.setText("Tidak ada data absensi");

                            // Tampilkan pesan error jika ada
                            if (json.has("message")) {
                                showToast(json.getString("message"));
                            }
                        }

                    } catch (Exception e) {
                        Log.e(TAG, "=== PARSE ERROR ===");
                        Log.e(TAG, "Error: " + e.getMessage());
                        e.printStackTrace();

                        rvAbsensi.setVisibility(View.GONE);
                        tvEmptyState.setVisibility(View.VISIBLE);
                        tvEmptyState.setText("Error parsing data");
                        showToast("Error: " + e.getMessage());
                    }
                },
                error -> {
                    if (!isFragmentActive) return;

                    Log.e(TAG, "=== NETWORK ERROR ===");
                    if (error.networkResponse != null) {
                        Log.e(TAG, "Status Code: " + error.networkResponse.statusCode);
                        Log.e(TAG, "Response: " + new String(error.networkResponse.data));
                    }
                    Log.e(TAG, "Error: " + error.toString());

                    rvAbsensi.setVisibility(View.GONE);
                    tvEmptyState.setVisibility(View.VISIBLE);
                    tvEmptyState.setText("Gagal memuat data");
                    showToast("Gagal memuat: " + error.toString());
                }
        );

        queue.add(request);
    }

    // ==========================================================
    //                  FORMAT TANGGAL
    // ==========================================================
    private String formatTanggal(String tanggal) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd MMMM yyyy", new Locale("id", "ID"));
            return outputFormat.format(inputFormat.parse(tanggal));
        } catch (Exception e) {
            return tanggal;
        }
    }

    // ==========================================================
    //                  GET STATUS COLOR
    // ==========================================================
    private int getStatusColor(String status) {
        switch (status.toUpperCase()) {
            case "HADIR":
                return 0xFF4CAF50; // Hijau
            case "IZIN":
                return 0xFFFFC107; // Kuning
            case "SAKIT":
                return 0xFF2196F3; // Biru
            case "ALPHA":
            case "ALPA":
                return 0xFFF44336; // Merah
            default:
                return 0xFF9E9E9E; // Abu-abu
        }
    }

    // ===========================
    // MODEL
    // ===========================
    public static class AbsensiModel {
        String tanggal, keterangan;

        public AbsensiModel(String tanggal, String keterangan) {
            this.tanggal = tanggal;
            this.keterangan = keterangan;
        }
    }

    // ===========================
    // ADAPTER
    // ===========================
    public class AbsensiAdapter extends RecyclerView.Adapter<AbsensiAdapter.AbsensiVH> {

        ArrayList<AbsensiModel> data;

        public AbsensiAdapter(ArrayList<AbsensiModel> data) {
            this.data = data;
        }

        @NonNull
        @Override
        public AbsensiVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_kegiatan, parent, false);
            return new AbsensiVH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull AbsensiVH holder, int position) {
            AbsensiModel m = data.get(position);

            // Format tanggal ke Indonesia
            String tanggalFormatted = formatTanggal(m.tanggal);

            holder.tvTime.setVisibility(View.GONE); // Sembunyikan jam
            holder.tvTitle.setText(m.keterangan);
            holder.tvDetail.setText(tanggalFormatted);

            // Set warna berdasarkan status
            int color = getStatusColor(m.keterangan);
            holder.tvTitle.setTextColor(color);
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        class AbsensiVH extends RecyclerView.ViewHolder {

            TextView tvTime, tvTitle, tvDetail;

            public AbsensiVH(@NonNull View itemView) {
                super(itemView);

                tvTime = itemView.findViewById(R.id.tv_kegiatan_time);
                tvTitle = itemView.findViewById(R.id.tv_kegiatan_title);
                tvDetail = itemView.findViewById(R.id.tv_kegiatan_detail);
            }
        }
    }
}