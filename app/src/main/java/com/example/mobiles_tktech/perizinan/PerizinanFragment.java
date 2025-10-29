package com.example.mobiles_tktech.perizinan;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.mobiles_tktech.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.*;

public class PerizinanFragment extends Fragment {

    private EditText edtTanggalMulai, edtTanggalSelesai, edtKeterangan;
    private Spinner spinnerJenis;
    private Button btnKirim;
    private LinearLayout containerStatus;
    private RequestQueue requestQueue;
    private Calendar calendar = Calendar.getInstance();

    private static final String URL_IZIN = "http://ortuconnect.atwebpages.com/api/perizinan.php";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
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

        edtTanggalMulai.setOnClickListener(v -> showDatePickerDialog(edtTanggalMulai));
        edtTanggalSelesai.setOnClickListener(v -> showDatePickerDialog(edtTanggalSelesai));

        btnKirim.setOnClickListener(v -> kirimIzin());

        loadRiwayatIzin();
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

        if (username.isEmpty()) {
            return;
        }

        String url = URL_IZIN + "?username=" + username;

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    if (!isAdded()) return;
                    try {
                        if (response.getBoolean("success")) {
                            JSONArray data = response.getJSONArray("data");
                            tampilkanStatus(data);
                        } else {
                            containerStatus.removeAllViews();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                },
                error -> { }
        );
        requestQueue.add(request);
    }


    private void tampilkanStatus(JSONArray data) throws JSONException {
        if (!isAdded() || getContext() == null) {

            return;
        }

        containerStatus.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(requireContext());

        for (int i = 0; i < data.length(); i++) {
            JSONObject izin = data.getJSONObject(i);
            View card = inflater.inflate(R.layout.item_status_izin, containerStatus, false);

            TextView tvTanggal = card.findViewById(R.id.tvTanggalIzin);
            TextView tvJenis = card.findViewById(R.id.tvJenisIzin);
            TextView tvStatus = card.findViewById(R.id.tvStatusIzin);

            String tglMulai = izin.optString("tanggal_mulai", "");
            String tglSelesai = izin.optString("tanggal_selesai", "");

            if (tglMulai.isEmpty()) tglMulai = "-";

            if (tglSelesai.isEmpty() || tglSelesai.equals(tglMulai)) {
                tvTanggal.setText(tglMulai);
            } else {
                tvTanggal.setText(tglMulai + " - " + tglSelesai);
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


}
