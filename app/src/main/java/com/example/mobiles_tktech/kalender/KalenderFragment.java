package com.example.mobiles_tktech.kalender;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CalendarView;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.mobiles_tktech.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class KalenderFragment extends Fragment {

    private TextView tvJudul, tvTanggal, tvDeskripsi;
    private CalendarView calendarView;
    private JSONArray agendaData; // simpan data agenda

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_kalender, container, false);

        // --- Back Button ---
        ImageButton btnBack = view.findViewById(R.id.btn_back_header);
        btnBack.setOnClickListener(v -> {
            if (getActivity() instanceof com.example.mobiles_tktech.navigasi.NavigasiCard) {
                ((com.example.mobiles_tktech.navigasi.NavigasiCard) getActivity()).navigateToDashboard();

                BottomNavigationView bottomNav = requireActivity().findViewById(R.id.bottom_nav_card);
                bottomNav.setSelectedItemId(R.id.nav_beranda);
            }
        });

        // --- Referensi UI ---
        tvJudul = view.findViewById(R.id.tv_judul_kegiatan);
        tvTanggal = view.findViewById(R.id.tv_tanggal_kegiatan);
        tvDeskripsi = view.findViewById(R.id.tv_deskripsi_kegiatan);
        calendarView = view.findViewById(R.id.calendar_view);

        // Ambil bulan & tahun saat ini
        Calendar cal = Calendar.getInstance();
        int month = cal.get(Calendar.MONTH) + 1;
        int year = cal.get(Calendar.YEAR);

        // Load agenda langsung
        loadAgenda(month, year);

        // Jika user klik tanggal
        calendarView.setOnDateChangeListener((viewCal, y, m, day) -> {
            m = m + 1; // karena 0-11
            String tanggalPilih = y + "-" + String.format("%02d", m) + "-" + String.format("%02d", day);
            tampilkanAgendaHariIni(tanggalPilih);
        });

        return view;
    }


    // ==========================================================
    //                 LOAD AGENDA (SEMUA AGENDA BULAN)
    // ==========================================================
    private void loadAgenda(int month, int year) {

        String url = "http://ortuconnect.atwebpages.com/api/admin/agenda.php?month="
                + month + "&year=" + year;

        Log.d("KalenderAgenda", "🌐 Request: " + url);

        StringRequest request = new StringRequest(Request.Method.GET, url,
                response -> {
                    Log.d("KalenderAgenda", "✅ Response: " + response);

                    try {
                        JSONObject json = new JSONObject(response);

                        if (!json.getString("status").equals("success")) {
                            tvJudul.setText("Tidak ada agenda");
                            tvTanggal.setText("-");
                            tvDeskripsi.setText("-");
                            return;
                        }

                        agendaData = json.getJSONArray("data");

                        if (agendaData.length() == 0) {
                            tvJudul.setText("Tidak ada agenda bulan ini");
                            tvTanggal.setText("-");
                            tvDeskripsi.setText("-");
                            return;
                        }

                        // --- Tampilkan agenda hari ini ---
                        Calendar cal = Calendar.getInstance();
                        String today = new SimpleDateFormat("yyyy-MM-dd").format(cal.getTime());
                        tampilkanAgendaHariIni(today);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }, error -> {
            Log.e("KalenderAgenda", "❌ ERROR: " + error.getMessage());
        });

        Volley.newRequestQueue(requireContext()).add(request);
    }


    // ==========================================================
    //            TAMPILKAN AGENDA BERDASARKAN TANGGAL
    // ==========================================================
    private void tampilkanAgendaHariIni(String tanggalDicari) {
        try {
            if (agendaData == null || agendaData.length() == 0) return;

            for (int i = 0; i < agendaData.length(); i++) {
                JSONObject agenda = agendaData.getJSONObject(i);
                String tanggal = agenda.getString("tanggal");

                if (tanggal.equals(tanggalDicari)) {
                    // FIELD JSON YANG BENAR:
                    tvJudul.setText(agenda.getString("nama_kegiatan"));
                    tvTanggal.setText("Tanggal: " + tanggal);
                    tvDeskripsi.setText(agenda.getString("deskripsi"));
                    return;
                }
            }

            // Jika tidak ada agenda pada tanggal itu → tampilkan agenda pertama
            JSONObject agenda = agendaData.getJSONObject(0);
            tvJudul.setText(agenda.getString("nama_kegiatan"));
            tvTanggal.setText("Tanggal: " + agenda.getString("tanggal"));
            tvDeskripsi.setText(agenda.getString("deskripsi"));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
