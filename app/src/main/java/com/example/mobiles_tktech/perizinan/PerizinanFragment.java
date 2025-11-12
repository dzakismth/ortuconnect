package com.example.mobiles_tktech.perizinan;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.mobiles_tktech.R;
import com.example.mobiles_tktech.dashboard.DashboardFragment;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.*;

public class PerizinanFragment extends Fragment {

    private EditText edtTanggalMulai, edtTanggalSelesai, edtKeterangan;
    private Spinner spinnerJenis;
    private Spinner spinnerBulanFilter;
    private Button btnKirim;
    private LinearLayout containerStatus;
    private RequestQueue requestQueue;
    private Calendar calendar = Calendar.getInstance();

    private static final String URL_IZIN = "http://ortuconnect.atwebpages.com/api/perizinan.php";
    private String selectedMonthFilter = "Semua Bulan"; // Default filter

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Pastikan layout sesuai
        return inflater.inflate(R.layout.activity_ajukan_izin, container, false);
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

        // 🔙 Tombol kembali ke Dashboard
        ImageButton btnBack = view.findViewById(R.id.btn_back_header);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                FragmentTransaction transaction = requireActivity()
                        .getSupportFragmentManager()
                        .beginTransaction();
                transaction.replace(R.id.fragment_container, new DashboardFragment());
                transaction.addToBackStack(null);
                transaction.commit();
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
            public void onNothingSelected(AdapterView<?> parent) {
            }
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
                            containerStatus.removeAllViews();
                            TextView tvNoData = new TextView(getContext());
                            tvNoData.setText("Tidak ada riwayat perizinan.");
                            tvNoData.setPadding(0, 32, 0, 0);
                            containerStatus.addView(tvNoData);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                },
                error -> {
                    if (isAdded()) {
                        Toast.makeText(getContext(), "Gagal memuat riwayat izin.", Toast.LENGTH_SHORT).show();
                        containerStatus.removeAllViews();
                    }
                }
        );
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
                    if (!bulanData.equalsIgnoreCase(filterBulan)) passFilter = false;
                } catch (Exception e) {
                    Log.e("Perizinan", "Gagal memparsing tanggal: " + tglMulai, e);
                    passFilter = false;
                }
            }

            if (passFilter) {
                View card = inflater.inflate(R.layout.item_status_izin, containerStatus, false);
                TextView tvTanggal = card.findViewById(R.id.tvTanggalIzin);
                TextView tvJenis = card.findViewById(R.id.tvJenisIzin);
                TextView tvStatus = card.findViewById(R.id.tvStatusIzin);

                String tglSelesai = izin.optString("tanggal_selesai", "");
                String displayTglMulai = tglMulai.isEmpty() ? "-" : tglMulai;

                if (tglSelesai.isEmpty() || tglSelesai.equals(tglMulai)) {
                    tvTanggal.setText(displayTglMulai);
                } else {
                    tvTanggal.setText(displayTglMulai + " - " + tglSelesai);
                }

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

        if (containerStatus.getChildCount() == 0) {
            TextView tvNoData = new TextView(getContext());
            tvNoData.setText("Tidak ada riwayat perizinan pada bulan " + filterBulan + ".");
            tvNoData.setPadding(0, 32, 0, 0);
            containerStatus.addView(tvNoData);
        }
    }
}
