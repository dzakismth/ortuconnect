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
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.mobiles_tktech.MainActivity;
import com.example.mobiles_tktech.R;
import com.example.mobiles_tktech.navigasi.NavigasiCard;

import org.json.JSONException;
import org.json.JSONObject;

public class DashboardFragment extends Fragment {

    private TextView tvNamaSiswa, tvKelas, tvJadwal, tvKehadiran, tvIzin;
    private ImageView imgProfile;
    private RequestQueue requestQueue;
    private String usernameOrtu, kelasSiswa;

    private static final String BASE_PROFILE = "http://ortuconnect.atwebpages.com/api/profile.php";
    private static final String BASE_JADWAL = "http://ortuconnect.atwebpages.com/api/jadwal_hari_ini.php";
    private static final String BASE_KEHADIRAN = "http://ortuconnect.atwebpages.com/api/kehadiran.php";
    private static final String BASE_IZIN = "http://ortuconnect.atwebpages.com/api/izin_terbaru.php";
    private static final String BASE_AGENDA = "http://ortuconnect.atwebpages.com/api/admin/agenda.php";

    // ➕ URL baru untuk kehadiran minggu ini — hanya HADIR
    private static final String BASE_KEHADIRAN_MINGGU = "http://ortuconnect.atwebpages.com/api/kehadiran_minggu.php";

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
        usernameOrtu = prefs.getString("username", "");

        if (usernameOrtu.isEmpty()) {
            logoutUser();
            return;
        }

        tvNamaSiswa = view.findViewById(R.id.tv_nama_siswa);
        tvKelas = view.findViewById(R.id.tv_kelas);
        tvJadwal = view.findViewById(R.id.tv_jadwal_hari_ini);
        tvKehadiran = view.findViewById(R.id.tv_kehadiran_status);
        tvIzin = view.findViewById(R.id.tv_izin_status);
        imgProfile = view.findViewById(R.id.imgProfile);

        view.findViewById(R.id.card_kehadiran).setOnClickListener(v -> navigateTo("absensi"));
        view.findViewById(R.id.card_status_izin).setOnClickListener(v -> navigateTo("perizinan"));
        view.findViewById(R.id.card_jadwal).setOnClickListener(v -> navigateTo("kalender"));
        view.findViewById(R.id.card_profil).setOnClickListener(v -> navigateTo("profil"));

        loadProfile();
    }

    private void loadProfile() {
        String url = BASE_PROFILE + "?username=" + usernameOrtu;

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        if (response.getBoolean("success")) {
                            JSONObject d = response.getJSONObject("data");

                            String nama = d.getString("nama_siswa");
                            kelasSiswa = d.getString("kelas");
                            String gender = d.getString("gender");

                            tvNamaSiswa.setText(nama);
                            tvKelas.setText(kelasSiswa.toUpperCase());
                            updateProfileIcon(gender);

                            loadJadwalHariIni();
                            loadKehadiran();
                            loadIzinTerbaru();
                            loadAgendaTerbaru();

                            // ➕ TAMBAHAN — tampilkan hanya HADIR minggu ini bos
                            loadKehadiranMingguIni();

                        } else Toast.makeText(getContext(), "Profil tidak ditemukan", Toast.LENGTH_SHORT).show();

                    } catch (JSONException e) {
                        Log.e("DashboardFragment", "JSON Error: " + e.getMessage());
                    }
                },
                error -> Toast.makeText(getContext(), "Koneksi gagal", Toast.LENGTH_SHORT).show());

        requestQueue.add(request);
    }

    private void loadJadwalHariIni() {
        String url = BASE_JADWAL + "?kelas=" + kelasSiswa;

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                res -> {
                    try {
                        if (res.getBoolean("success")) {
                            tvJadwal.setText("Hari " +
                                    res.getString("hari") + ", " +
                                    res.getString("kegiatan"));
                        }
                    } catch (Exception ignored) {}
                },
                err -> tvJadwal.setText("Tidak ada jadwal"));

        requestQueue.add(request);
    }

    private void loadKehadiran() {
        String url = BASE_KEHADIRAN + "?username=" + usernameOrtu;

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                res -> {
                    try {
                        if (res.getBoolean("success")) {
                            int hadir = res.getInt("hadir");
                            int total = res.getInt("total");
                            tvKehadiran.setText(hadir + " dari " + total + " hari hadir");
                        }
                    } catch (Exception ignored) {}
                },
                err -> tvKehadiran.setText("Belum ada data"));

        requestQueue.add(request);
    }

    private void loadIzinTerbaru() {
        String url = BASE_IZIN + "?username=" + usernameOrtu;

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                res -> {
                    try {
                        if (res.getBoolean("success")) {
                            tvIzin.setText("Izin " +
                                    res.getString("status") +
                                    ": " + res.getString("tanggal"));
                        }
                    } catch (Exception ignored) {}
                },
                err -> tvIzin.setText("Belum ada izin"));

        requestQueue.add(request);
    }

    private void loadAgendaTerbaru() {
        String url = BASE_AGENDA + "?month=11&year=2025";

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                res -> {
                    try {
                        if (res.getString("status").equals("success")) {

                            if (res.getJSONArray("data").length() > 0) {

                                JSONObject d = res.getJSONArray("data").getJSONObject(0);

                                String kegiatan = d.getString("nama_kegiatan");
                                String tanggal = d.getString("tanggal");

                                tvJadwal.setText(kegiatan + "\nTanggal: " + tanggal);
                            } else {
                                tvJadwal.setText("Tidak ada agenda");
                            }

                        } else {
                            tvJadwal.setText("Agenda tidak ditemukan");
                        }
                    } catch (Exception e) {
                        tvJadwal.setText("Gagal parsing agenda");
                    }
                },
                err -> tvJadwal.setText("Gagal memuat agenda"));

        requestQueue.add(request);
    }

    // ============================================
    //    ➕ KEHADIRAN MINGGU INI — HANYA HADIR
    // ============================================
    private void loadKehadiranMingguIni() {

        String url = BASE_KEHADIRAN_MINGGU + "?username=" + usernameOrtu;

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                res -> {
                    try {

                        if (res.getBoolean("success")) {

                            int hadirMinggu = res.getInt("hadir");

                            // → tampilkan hanya hadir
                            tvKehadiran.setText("Hadir minggu ini: " + hadirMinggu + " hari");

                        } else {
                            tvKehadiran.setText("Belum ada data minggu ini");
                        }

                    } catch (Exception e) {
                        tvKehadiran.setText("Gagal membaca data");
                    }
                },
                err -> tvKehadiran.setText("Gagal memuat"));

        requestQueue.add(request);
    }

    private void updateProfileIcon(String gender) {
        if (gender.equalsIgnoreCase("perempuan")) imgProfile.setImageResource(R.drawable.icon_cewe);
        else imgProfile.setImageResource(R.drawable.icon_cowo);
    }

    private void navigateTo(String target) {
        if (getActivity() instanceof NavigasiCard) {
            ((NavigasiCard) getActivity()).navigateTo(target);
        }
    }

    private void logoutUser() {
        SharedPreferences prefs = requireActivity().getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE);
        prefs.edit().clear().apply();

        Intent i = new Intent(getActivity(), MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        requireActivity().finish();
    }
}
