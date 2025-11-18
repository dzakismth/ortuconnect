package com.example.mobiles_tktech.kalender;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CalendarView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.android.volley.Request;
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
import java.util.List;

public class KalenderFragment extends Fragment {

    private CalendarView calendarView;
    private LinearLayout containerAgenda;
    private JSONArray agendaData;
    private List<AgendaItem> agendaList;

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
        calendarView = view.findViewById(R.id.calendar_view);
        containerAgenda = view.findViewById(R.id.container_agenda_list);

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
            tampilkanAgendaBerdasarkanTanggal(tanggalPilih);
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
                            tampilkanPesanKosong("Tidak ada agenda");
                            return;
                        }

                        agendaData = json.getJSONArray("data");

                        if (agendaData.length() == 0) {
                            tampilkanPesanKosong("Tidak ada agenda bulan ini");
                            return;
                        }

                        // Parse dan urutkan agenda
                        parseAgendaData();

                        // Tampilkan semua agenda
                        tampilkanSemuaAgenda();

                    } catch (Exception e) {
                        Log.e("KalenderAgenda", "Parse Error: " + e.getMessage());
                        tampilkanPesanKosong("Error parsing data");
                    }

                }, error -> {
            Log.e("KalenderAgenda", "❌ ERROR: " + (error.getMessage() != null ? error.getMessage() : "Unknown error"));
            tampilkanPesanKosong("Gagal memuat data");
        });

        Volley.newRequestQueue(requireContext()).add(request);
    }

    // ==========================================================
    //              PARSE DAN URUTKAN DATA AGENDA
    // ==========================================================
    private void parseAgendaData() {
        agendaList = new ArrayList<>();

        try {
            for (int i = 0; i < agendaData.length(); i++) {
                JSONObject agenda = agendaData.getJSONObject(i);

                AgendaItem item = new AgendaItem(
                        agenda.getString("nama_kegiatan"),
                        agenda.getString("tanggal"),
                        agenda.getString("deskripsi")
                );

                agendaList.add(item);
            }

            // ✅ Urutkan berdasarkan tanggal TERBARU (descending)
            Collections.sort(agendaList, new Comparator<AgendaItem>() {
                @Override
                public int compare(AgendaItem a1, AgendaItem a2) {
                    // Descending order (terbaru di atas)
                    return a2.tanggal.compareTo(a1.tanggal);
                }
            });

        } catch (Exception e) {
            Log.e("KalenderAgenda", "Parse Item Error: " + e.getMessage());
        }
    }

    // ==========================================================
    //           TAMPILKAN SEMUA AGENDA DALAM LIST
    // ==========================================================
    private void tampilkanSemuaAgenda() {
        containerAgenda.removeAllViews();

        if (agendaList == null || agendaList.isEmpty()) {
            tampilkanPesanKosong("Tidak ada agenda");
            return;
        }

        for (AgendaItem item : agendaList) {
            View cardView = createAgendaCard(item);
            containerAgenda.addView(cardView);
        }
    }

    // ==========================================================
    //          TAMPILKAN AGENDA BERDASARKAN TANGGAL DIPILIH
    // ==========================================================
    private void tampilkanAgendaBerdasarkanTanggal(String tanggalDicari) {
        containerAgenda.removeAllViews();

        if (agendaList == null || agendaList.isEmpty()) {
            tampilkanPesanKosong("Tidak ada agenda");
            return;
        }

        boolean found = false;

        for (AgendaItem item : agendaList) {
            if (item.tanggal.equals(tanggalDicari)) {
                View cardView = createAgendaCard(item);
                containerAgenda.addView(cardView);
                found = true;
            }
        }

        if (!found) {
            tampilkanPesanKosong("Tidak ada agenda pada tanggal ini");
        }
    }

    // ==========================================================
    //                  BUAT CARD AGENDA
    // ==========================================================
    private View createAgendaCard(AgendaItem item) {
        View cardView = LayoutInflater.from(getContext()).inflate(
                R.layout.item_kegiatan,
                containerAgenda,
                false
        );

        TextView tvTitle = cardView.findViewById(R.id.tv_kegiatan_title);
        TextView tvDetail = cardView.findViewById(R.id.tv_kegiatan_detail);
        TextView tvDeskripsi = cardView.findViewById(R.id.tv_kegiatan_deskripsi);

        tvTitle.setText(item.namaKegiatan);
        tvDetail.setText(formatTanggal(item.tanggal));
        tvDeskripsi.setText(item.deskripsi);

        return cardView;
    }

    // ==========================================================
    //                FORMAT TANGGAL KE INDONESIA
    // ==========================================================
    private String formatTanggal(String tanggal) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd");
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd MMMM yyyy");
            return outputFormat.format(inputFormat.parse(tanggal));
        } catch (Exception e) {
            return tanggal;
        }
    }

    // ==========================================================
    //                  TAMPILKAN PESAN KOSONG
    // ==========================================================
    private void tampilkanPesanKosong(String pesan) {
        containerAgenda.removeAllViews();

        TextView tvKosong = new TextView(getContext());
        tvKosong.setText(pesan);
        tvKosong.setTextSize(16);
        tvKosong.setTextColor(getResources().getColor(android.R.color.darker_gray));
        tvKosong.setPadding(16, 32, 16, 16);
        tvKosong.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);

        containerAgenda.addView(tvKosong);
    }

    // ==========================================================
    //                    MODEL AGENDA ITEM
    // ==========================================================
    private static class AgendaItem {
        String namaKegiatan;
        String tanggal;
        String deskripsi;

        AgendaItem(String namaKegiatan, String tanggal, String deskripsi) {
            this.namaKegiatan = namaKegiatan;
            this.tanggal = tanggal;
            this.deskripsi = deskripsi;
        }
    }
}