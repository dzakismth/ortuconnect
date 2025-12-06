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
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class KalenderFragment extends Fragment {

    private CalendarView calendarView;
    private LinearLayout containerAgenda;
    private List<AgendaItem> agendaList = new ArrayList<>();

    private int currentMonth = -1;  // 1-12
    private int currentYear = -1;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_kalender, container, false);

        // Back button
        ImageButton btnBack = view.findViewById(R.id.btn_back_header);
        btnBack.setOnClickListener(v -> {
            if (getActivity() instanceof com.example.mobiles_tktech.navigasi.NavigasiCard) {
                ((com.example.mobiles_tktech.navigasi.NavigasiCard) getActivity()).navigateToDashboard();
                BottomNavigationView nav = requireActivity().findViewById(R.id.bottom_nav_card);
                if (nav != null) nav.setSelectedItemId(R.id.nav_beranda);
            }
        });

        calendarView = view.findViewById(R.id.calendar_view);
        containerAgenda = view.findViewById(R.id.container_agenda_list);

        // Inisialisasi bulan & tahun saat ini
        Calendar cal = Calendar.getInstance();
        currentMonth = cal.get(Calendar.MONTH) + 1;
        currentYear = cal.get(Calendar.YEAR);

        // Langsung load agenda bulan ini saat pertama buka
        loadAndShowAllAgenda(currentMonth, currentYear);

        // DETEKSI GANTI BULAN → LANGSUNG TAMPILKAN SEMUA AGENDA BULAN BARU
        calendarView.setOnDateChangeListener((view1, year, month, dayOfMonth) -> {
            int selectedMonth = month + 1;
            int selectedYear = year;

            // Hanya trigger kalau bulan atau tahun berubah
            if (selectedMonth != currentMonth || selectedYear != currentYear) {
                currentMonth = selectedMonth;
                currentYear = selectedYear;
                loadAndShowAllAgenda(currentMonth, currentYear);  // LANGSUNG TAMPILKAN SEMUA AGENDA BULAN INI
            }
            // Kalau klik tanggal di bulan yang sama → tetap tampilkan semua (karena sudah di-load)
        });

        return view;
    }

    // LOAD + LANGSUNG TAMPILKAN SEMUA AGENDA BULAN INI (INI YANG KAMU MAU!)ya
    private void loadAndShowAllAgenda(int month, int year) {
        String url = "https://ortuconnect.pbltifnganjuk.com/api/admin/agenda.php?month=" + month + "&year=" + year;
        Log.d("Kalender", "Loading: " + url);

        containerAgenda.removeAllViews();
        tampilkanLoading("Memuat agenda " + getNamaBulan(month) + " " + year + "...");

        StringRequest request = new StringRequest(Request.Method.GET, url,
                response -> {
                    try {
                        JSONObject json = new JSONObject(response);
                        if (!json.getString("status").equals("success")) {
                            tampilkanPesanKosong("Tidak ada agenda di bulan ini");
                            agendaList.clear();
                            return;
                        }

                        JSONArray data = json.getJSONArray("data");
                        parseAgendaData(data);
                        filterAndSortAgenda();  // FILTER (hilang kalau sudah lewat) + SORT (terdekat atas)
                        tampilkanSemuaAgenda();  // TAMPILKAN YANG SUDAH DIFILTER & SORT

                    } catch (Exception e) {
                        tampilkanPesanKosong("Error memuat data");
                    }
                },
                error -> tampilkanPesanKosong("Gagal memuat agenda"));

        Volley.newRequestQueue(requireContext()).add(request);
    }

    private void parseAgendaData(JSONArray data) {
        agendaList.clear();
        try {
            for (int i = 0; i < data.length(); i++) {
                JSONObject obj = data.getJSONObject(i);
                agendaList.add(new AgendaItem(
                        obj.optString("nama_kegiatan", "Tanpa Judul"),
                        obj.optString("tanggal", ""),
                        obj.optString("deskripsi", "Tidak ada keterangan")
                ));
            }
        } catch (Exception e) {
            Log.d("Kalender", "Parse error: " + e.getMessage());
        }
    }

    // FILTER (hilangkan yang sudah lewat) + SORT (terdekat di atas)
    private void filterAndSortAgenda() {
        // Dapatkan tanggal hari ini
        Calendar today = Calendar.getInstance();
        Date todayDate = today.getTime();

        // Filter: hilangkan yang sudah lewat
        List<AgendaItem> filteredList = new ArrayList<>();
        for (AgendaItem item : agendaList) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT);
                Date agendaDate = sdf.parse(item.tanggal);
                if (agendaDate != null && !agendaDate.before(todayDate)) {  // Hanya yang hari ini atau depan
                    filteredList.add(item);
                }
            } catch (Exception ignored) {}
        }
        agendaList = filteredList;

        // Sort: terdekat (ascending tanggal)
        Collections.sort(agendaList, new Comparator<AgendaItem>() {
            @Override
            public int compare(AgendaItem a, AgendaItem b) {
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT);
                    Date dateA = sdf.parse(a.tanggal);
                    Date dateB = sdf.parse(b.tanggal);
                    return dateA.compareTo(dateB);
                } catch (Exception e) {
                    return 0;
                }
            }
        });
    }

    private void tampilkanSemuaAgenda() {
        containerAgenda.removeAllViews();
        if (agendaList.isEmpty()) {
            tampilkanPesanKosong("Tidak ada agenda di bulan ini");
            return;
        }

        for (AgendaItem item : agendaList) {
            containerAgenda.addView(createAgendaCard(item));
        }
    }

    private View createAgendaCard(AgendaItem item) {
        View card = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_kegiatan, containerAgenda, false);

        TextView tvTitle = card.findViewById(R.id.tv_kegiatan_title);
        TextView tvDate = card.findViewById(R.id.tv_kegiatan_detail);
        TextView tvDesc = card.findViewById(R.id.tv_kegiatan_deskripsi);

        tvTitle.setText(item.namaKegiatan);
        tvDate.setText(formatTanggal(item.tanggal));
        tvDesc.setText(item.deskripsi);

        return card;
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

    private String getNamaBulan(int month) {
        String[] bulan = {"", "Januari", "Februari", "Maret", "April", "Mei", "Juni",
                "Juli", "Agustus", "September", "Oktober", "November", "Desember"};
        return bulan[month];
    }

    private void tampilkanLoading(String msg) {
        containerAgenda.removeAllViews();
        TextView tv = new TextView(requireContext());
        tv.setText(msg);
        tv.setTextColor(getResources().getColor(android.R.color.white));
        tv.setTextSize(15);
        tv.setPadding(32, 80, 32, 32);
        tv.setGravity(android.view.Gravity.CENTER);
        containerAgenda.addView(tv);
    }

    private void tampilkanPesanKosong(String msg) {
        containerAgenda.removeAllViews();
        TextView tv = new TextView(requireContext());
        tv.setText(msg);
        tv.setTextColor(getResources().getColor(android.R.color.darker_gray));
        tv.setTextSize(16);
        tv.setPadding(32, 80, 32, 32);
        tv.setGravity(android.view.Gravity.CENTER);
        containerAgenda.addView(tv);
    }

    private static class AgendaItem {
        String namaKegiatan, tanggal, deskripsi;
        AgendaItem(String nama, String tgl, String desc) {
            this.namaKegiatan = nama;
            this.tanggal = tgl;
            this.deskripsi = desc;
        }
    }
}