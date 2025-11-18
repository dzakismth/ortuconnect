package com.example.mobiles_tktech.absen;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.mobiles_tktech.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

public class AbsensiFragment extends Fragment {

    Spinner spinnerBulan;
    RecyclerView rvAbsensi;
    AbsensiAdapter adapter;
    ArrayList<AbsensiModel> listAbsensi = new ArrayList<>();

    String idSiswa = "21"; // contoh
    String selectedBulan = "2025-11";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_absensi, container, false);

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

        rvAbsensi.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new AbsensiAdapter(listAbsensi);
        rvAbsensi.setAdapter(adapter);

        spinnerBulan.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                selectedBulan = spinnerBulan.getSelectedItem().toString();
                loadAbsensi();
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        loadAbsensi();

        return view;
    }

    private void loadAbsensi() {
        String url = "http://ortuconnect.atwebpages.com/api/admin/absensi.php?id_siswa=" + idSiswa + "&bulan=" + selectedBulan;

        RequestQueue queue = Volley.newRequestQueue(requireContext());

        StringRequest request = new StringRequest(Request.Method.GET, url,
                response -> {
                    try {
                        listAbsensi.clear();

                        JSONArray arr = new JSONArray(response);

                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject o = arr.getJSONObject(i);

                            String tanggal = o.getString("tanggal");
                            String keterangan = o.getString("keterangan");

                            listAbsensi.add(new AbsensiModel(tanggal, keterangan));
                        }

                        adapter.notifyDataSetChanged();

                    } catch (Exception e) {
                        Log.e("ABSENSI", "Error parsing: " + e.getMessage());
                    }
                },
                error -> Log.e("ABSENSI", "Volley Error: " + error.toString())
        );

        queue.add(request);
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
    // ADAPTER (PAKAI item_kegiatan.xml)
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

            holder.tvTime.setText("—"); // khusus absensi tidak ada jam
            holder.tvTitle.setText(m.keterangan);
            holder.tvDetail.setText("Tanggal: " + m.tanggal);
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
