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

    // ⚠️ GUNAKAN ENDPOINT YANG BENAR - sesuaikan dengan API Anda
    // Pilih salah satu yang sesuai:
    private static final String BASE_ABSENSI = "http://ortuconnect.atwebpages.com/api/kehadiran_detail.php";
    // private static final String BASE_ABSENSI = "http://ortuconnect.atwebpages.com/api/admin/absensi.php";

    Spinner spinnerBulan;
    RecyclerView rvAbsensi;
    AbsensiAdapter adapter;
    ArrayList<AbsensiModel> listAbsensi = new ArrayList<>();
    TextView tvEmptyState;

    String usernameOrtu;
    String idSiswa;
    int selectedYear;
    int selectedMonth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_absensi, container, false);

        // Get username dari SharedPreferences
        SharedPreferences prefs = requireActivity().getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE);
        usernameOrtu = prefs.getString("username", "");

        if (usernameOrtu.isEmpty()) {
            Toast.makeText(getContext(), "Username tidak ditemukan", Toast.LENGTH_SHORT).show();
            return view;
        }

        // Back button
        ImageButton btnBack = view.findViewById(R.id.btn_back_header);
        btnBack.setOnClickListener(v -> {
            if (getActivity() instanceof com.example.mobiles_tktech.navigasi.NavigasiCard) {
                ((com.example.mobiles_tktech.navigasi.NavigasiCard) getActivity()).navigateToDashboard();

                BottomNavigationView bottomNav = requireActivity().findViewById(R.id.bottom_nav_card);
                bottomNav.setSelectedItemId(R.id.nav_beranda);
            }
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
        String url = BASE_PROFILE + "?username=" + usernameOrtu;
        Log.d(TAG, "Loading profile: " + url);

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        Log.d(TAG, "Profile Response: " + response.toString());

                        if (response.getBoolean("success")) {
                            JSONObject data = response.getJSONObject("data");
                            idSiswa = data.getString("id_siswa");

                            Log.d(TAG, "ID Siswa: " + idSiswa);

                            // Setelah dapat id_siswa, load absensi
                            loadAbsensi();

                        } else {
                            Toast.makeText(getContext(), "Profil tidak ditemukan", Toast.LENGTH_SHORT).show();
                        }

                    } catch (Exception e) {
                        Log.e(TAG, "Profile Parse Error: " + e.getMessage());
                        Toast.makeText(getContext(), "Error parsing profile", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    Log.e(TAG, "Profile Network Error: " + error.toString());
                    Toast.makeText(getContext(), "Gagal memuat profil", Toast.LENGTH_SHORT).show();
                });

        Volley.newRequestQueue(requireContext()).add(request);
    }

    // ==========================================================
    //                  LOAD ABSENSI
    // ==========================================================
    private void loadAbsensi() {
        if (idSiswa == null || idSiswa.isEmpty()) {
            Log.e(TAG, "ID Siswa belum tersedia");
            return;
        }

        // Format: bulan (1-12)
        int bulan = selectedMonth + 1;

        String url = BASE_ABSENSI + "?id_siswa=" + idSiswa
                + "&bulan=" + bulan
                + "&tahun=" + selectedYear;

        Log.d(TAG, "=== LOADING ABSENSI ===");
        Log.d(TAG, "URL: " + url);
        Log.d(TAG, "ID Siswa: " + idSiswa);
        Log.d(TAG, "Bulan: " + bulan);
        Log.d(TAG, "Tahun: " + selectedYear);

        RequestQueue queue = Volley.newRequestQueue(requireContext());

        // Coba gunakan StringRequest dulu untuk lihat response mentah
        StringRequest request = new StringRequest(Request.Method.GET, url,
                response -> {
                    try {
                        Log.d(TAG, "=== RAW RESPONSE ===");
                        Log.d(TAG, response);

                        listAbsensi.clear();

                        JSONObject json = new JSONObject(response);

                        // Cek berbagai format response yang mungkin
                        if (json.has("success") && json.getBoolean("success")) {
                            JSONArray arr = json.getJSONArray("data");
                            Log.d(TAG, "Jumlah data: " + arr.length());

                            for (int i = 0; i < arr.length(); i++) {
                                JSONObject o = arr.getJSONObject(i);

                                // Coba berbagai kemungkinan nama field
                                String tanggal = o.optString("tanggal",
                                        o.optString("date", ""));
                                String status = o.optString("status",
                                        o.optString("keterangan",
                                                o.optString("kehadiran", "")));

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

                        } else if (json.has("status") && json.getString("status").equals("success")) {
                            // Format alternatif dengan "status": "success"
                            JSONArray arr = json.getJSONArray("data");
                            Log.d(TAG, "Jumlah data (format 2): " + arr.length());

                            for (int i = 0; i < arr.length(); i++) {
                                JSONObject o = arr.getJSONObject(i);
                                String tanggal = o.optString("tanggal", o.optString("date", ""));
                                String status = o.optString("status", o.optString("keterangan", ""));

                                if (!tanggal.isEmpty() && !status.isEmpty()) {
                                    listAbsensi.add(new AbsensiModel(tanggal, status));
                                }
                            }

                            Collections.sort(listAbsensi, new Comparator<AbsensiModel>() {
                                @Override
                                public int compare(AbsensiModel a1, AbsensiModel a2) {
                                    return a2.tanggal.compareTo(a1.tanggal);
                                }
                            });

                            adapter.notifyDataSetChanged();

                            if (listAbsensi.isEmpty()) {
                                rvAbsensi.setVisibility(View.GONE);
                                tvEmptyState.setVisibility(View.VISIBLE);
                                tvEmptyState.setText("Tidak ada data absensi bulan ini");
                            } else {
                                rvAbsensi.setVisibility(View.VISIBLE);
                                tvEmptyState.setVisibility(View.GONE);
                            }

                        } else {
                            // Response tidak sesuai format
                            Log.e(TAG, "Format response tidak dikenali");
                            rvAbsensi.setVisibility(View.GONE);
                            tvEmptyState.setVisibility(View.VISIBLE);
                            tvEmptyState.setText("Tidak ada data absensi");
                        }

                    } catch (Exception e) {
                        Log.e(TAG, "=== PARSE ERROR ===");
                        Log.e(TAG, "Error: " + e.getMessage());
                        e.printStackTrace();

                        rvAbsensi.setVisibility(View.GONE);
                        tvEmptyState.setVisibility(View.VISIBLE);
                        tvEmptyState.setText("Error parsing data");
                        Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                },
                error -> {
                    Log.e(TAG, "=== NETWORK ERROR ===");
                    if (error.networkResponse != null) {
                        Log.e(TAG, "Status Code: " + error.networkResponse.statusCode);
                        Log.e(TAG, "Response: " + new String(error.networkResponse.data));
                    }
                    Log.e(TAG, "Error: " + error.toString());

                    rvAbsensi.setVisibility(View.GONE);
                    tvEmptyState.setVisibility(View.VISIBLE);
                    tvEmptyState.setText("Gagal memuat data");
                    Toast.makeText(getContext(), "Gagal memuat: " + error.toString(), Toast.LENGTH_LONG).show();
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