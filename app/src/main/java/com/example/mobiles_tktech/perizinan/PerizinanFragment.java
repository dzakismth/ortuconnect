package com.example.mobiles_tktech.perizinan;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.mobiles_tktech.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.*;

public class PerizinanFragment extends Fragment {

    private EditText edtTanggalMulai, edtTanggalSelesai, edtKeterangan;
    private Spinner spinnerJenis;
    // Tambahkan Spinner untuk filter bulan09
    private Spinner spinnerBulanFilter;
    private Button btnKirim;
    private LinearLayout containerStatus;
    private RequestQueue requestQueue;
    private Calendar calendar = Calendar.getInstance();

    private static final String URL_IZIN = "https://ortuconnect.pbltifnganjuk.com/api/perizinan.php";
    private String selectedMonthFilter = "Semua Bulan";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_perizinan, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        requestQueue = Volley.newRequestQueue(requireContext());

        edtTanggalMulai = view.findViewById(R.id.edtTanggalMulai);
        edtTanggalSelesai = view.findViewById(R.id.edtTanggalSelesai);
        edtKeterangan = view.findViewById(R.id.edtKeterangan);
        spinnerJenis = view.findViewById(R.id.spinnerJenisIzin);
        btnKirim = view.findViewById(R.id.btnKirimIzin);
        containerStatus = view.findViewById(R.id.containerStatus);
        spinnerBulanFilter = view.findViewById(R.id.spinnerBulanFilter);

        // 🔙 Tombol kembali ke Dashboard lewat NavigasiCard (bukan membuat fragment baru)
        ImageButton btnBack = view.findViewById(R.id.btn_back_header);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                if (getActivity() instanceof com.example.mobiles_tktech.navigasi.NavigasiCard) {
                    ((com.example.mobiles_tktech.navigasi.NavigasiCard) getActivity()).navigateToDashboard();

                    // update icon bottom nav ke Beranda
                    BottomNavigationView bottomNav = requireActivity().findViewById(R.id.bottom_nav_card);
                    bottomNav.setSelectedItemId(R.id.nav_beranda);
                }
            });
        }

        edtTanggalMulai.setOnClickListener(v -> showDatePickerDialog(edtTanggalMulai));
        edtTanggalSelesai.setOnClickListener(v -> showDatePickerDialog(edtTanggalSelesai));
        btnKirim.setOnClickListener(v -> kirimIzin());

        setupBulanFilter();
        loadRiwayatIzin();
    }

    private void setupBulanFilter() {
        if (spinnerBulanFilter == null) return;
        spinnerBulanFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedMonthFilter = parent.getItemAtPosition(position).toString();
                loadRiwayatIzin();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });
    }

    private void showDatePickerDialog(final EditText editText) {
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog dialog = new DatePickerDialog(requireContext(),
                (view, y, m, d) -> {
                    Calendar newCal = Calendar.getInstance();
                    newCal.set(y, m, d);
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                    editText.setText(sdf.format(newCal.getTime()));
                }, year, month, day);
        dialog.show();
    }

    private void kirimIzin() {
        String tanggalMulai = edtTanggalMulai.getText().toString().trim();
        String tanggalSelesai = edtTanggalSelesai.getText().toString().trim();
        String jenisIzin = spinnerJenis.getSelectedItem().toString();
        String keterangan = edtKeterangan.getText().toString().trim();

        if (tanggalMulai.isEmpty() || jenisIzin.isEmpty()) {
            Toast.makeText(getContext(), "Tanggal mulai dan jenis izin wajib diisi", Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences prefs = requireActivity().getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE);
        String username = prefs.getString("username", "");

        if (username.isEmpty()) {
            Toast.makeText(getContext(), "Username tidak ditemukan", Toast.LENGTH_SHORT).show();
            return;
        }

        btnKirim.setEnabled(false);

        try {
            JSONObject body = new JSONObject();
            body.put("username", username);
            body.put("tanggal_mulai", tanggalMulai);
            body.put("tanggal_selesai", tanggalSelesai);
            body.put("jenis_izin", jenisIzin);
            body.put("keterangan", keterangan);

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST,
                    URL_IZIN,
                    body,
                    response -> {
                        btnKirim.setEnabled(true);
                        if (response.optBoolean("success")) {
                            Toast.makeText(getContext(), "Izin berhasil dikirim", Toast.LENGTH_SHORT).show();
                            edtTanggalMulai.setText("");
                            edtTanggalSelesai.setText("");
                            edtKeterangan.setText("");
                            loadRiwayatIzin();
                        } else {
                            Toast.makeText(getContext(), response.optString("message"), Toast.LENGTH_SHORT).show();
                        }
                    },
                    error -> {
                        btnKirim.setEnabled(true);
                        Toast.makeText(getContext(), "Gagal kirim izin ke server", Toast.LENGTH_SHORT).show();
                    }
            );

            requestQueue.add(request);

        } catch (JSONException e) {
            btnKirim.setEnabled(true);
            e.printStackTrace();
        }
    }

    private void loadRiwayatIzin() {
        SharedPreferences prefs = requireActivity().getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE);
        String username = prefs.getString("username", "");
        if (username.isEmpty()) return;

        String url = URL_IZIN + "?username=" + username;

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    if (!isAdded()) return;
                    try {
                        if (response.getBoolean("success")) {
                            JSONArray data = response.getJSONArray("data");
                            tampilkanStatus(data, selectedMonthFilter);
                        } else {
                            tampilkanKosong("Tidak ada riwayat perizinan.");
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                },
                error -> {
                    if (isAdded()) tampilkanKosong("Gagal memuat riwayat izin.");
                });
        requestQueue.add(request);
    }

    private void tampilkanStatus(JSONArray rawData, String filterBulan) throws JSONException {
        if (!isAdded() || getContext() == null) return;

        containerStatus.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        SimpleDateFormat sdfBulan = new SimpleDateFormat("MMMM", new Locale("id", "ID"));
        SimpleDateFormat sdfParse = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

        for (int i = 0; i < rawData.length(); i++) {
            JSONObject izin = rawData.getJSONObject(i);
            String tglMulai = izin.optString("tanggal_mulai", "");
            boolean passFilter = true;

            if (!filterBulan.equals("Semua Bulan") && !tglMulai.isEmpty()) {
                try {
                    Date dateMulai = sdfParse.parse(tglMulai);
                    String bulanData = sdfBulan.format(dateMulai);
                    passFilter = bulanData.equalsIgnoreCase(filterBulan);
                } catch (Exception e) {
                    Log.e("Perizinan", "Gagal parsing tanggal: " + tglMulai, e);
                    passFilter = false;
                }
            }

            if (passFilter) {
                View card = inflater.inflate(R.layout.item_status_izin, containerStatus, false);
                TextView tvTanggal = card.findViewById(R.id.tvTanggalIzin);
                TextView tvJenis = card.findViewById(R.id.tvJenisIzin);
                TextView tvStatus = card.findViewById(R.id.tvStatusIzin);

                String tglSelesai = izin.optString("tanggal_selesai", "");
                tvTanggal.setText(tglSelesai.isEmpty() ? tglMulai : tglMulai + " - " + tglSelesai);
                tvJenis.setText(izin.optString("jenis_izin", "-"));
                String status = izin.optString("status", "Menunggu");

                tvStatus.setText(status);
                if (status.equalsIgnoreCase("Menunggu")) {
                    tvStatus.setBackgroundResource(R.drawable.bg_status_menunggu);
                    tvStatus.setTextColor(getResources().getColor(android.R.color.black));
                } else if (status.equalsIgnoreCase("Disetujui")) {
                    tvStatus.setBackgroundResource(R.drawable.bg_status_disetujui);
                    tvStatus.setTextColor(getResources().getColor(android.R.color.white));
                } else {
                    tvStatus.setBackgroundResource(android.R.color.darker_gray);
                    tvStatus.setTextColor(getResources().getColor(android.R.color.white));
                }

                containerStatus.addView(card);
            }
        }

        if (containerStatus.getChildCount() == 0) tampilkanKosong("Tidak ada riwayat perizinan bulan " + filterBulan + ".");
    }

    private void tampilkanKosong(String pesan) {
        containerStatus.removeAllViews();
        TextView tv = new TextView(getContext());
        tv.setText(pesan);
        tv.setPadding(0, 32, 0, 0);
        containerStatus.addView(tv);
    }
}
