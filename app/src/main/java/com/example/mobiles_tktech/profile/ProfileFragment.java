package com.example.mobiles_tktech.profile;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
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
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.mobiles_tktech.MainActivity;
import com.example.mobiles_tktech.R;
import com.example.mobiles_tktech.navigasi.NavigasiCard;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class ProfileFragment extends Fragment {

    private TextView tvAnakNama, tvAnakAlamat, tvAnakTglLahir, tvGender, tvOrtuNama,
            tvOrtuPhone, tvProfileNameTop, tvProfileClass;
    private ImageView imgEditIcon, imgProfilePicture;
    private RequestQueue requestQueue;

    private static final String BASE_URL = "https://ortuconnect.pbltifnganjuk.com/api/profile.php";
    private String usernameOrtu = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        requestQueue = Volley.newRequestQueue(requireContext());

        SharedPreferences prefs = requireActivity().getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE);
        usernameOrtu = prefs.getString("username", "");

        // Bind UI
        tvAnakNama = view.findViewById(R.id.detail_name_anak).findViewById(R.id.tvDetailValue);
        tvAnakAlamat = view.findViewById(R.id.detail_alamat).findViewById(R.id.tvDetailValue);
        tvAnakTglLahir = view.findViewById(R.id.detail_tanggal_lahir).findViewById(R.id.tvDetailValue);
        tvGender = view.findViewById(R.id.gender).findViewById(R.id.tvDetailValue);
        tvOrtuNama = view.findViewById(R.id.detail_name_ortu).findViewById(R.id.tvDetailValue);
        tvOrtuPhone = view.findViewById(R.id.detail_phone_ortu).findViewById(R.id.tvDetailValue);
        tvProfileNameTop = view.findViewById(R.id.tvProfileNameTop);
        tvProfileClass = view.findViewById(R.id.tvProfileClass);
        imgEditIcon = view.findViewById(R.id.imgEditProfileIcon);
        imgProfilePicture = view.findViewById(R.id.img_profile_large);
        ImageButton btnBackHeader = view.findViewById(R.id.btn_back_header);

        // Label
        setLabel(view, R.id.detail_name_anak, "Nama Anak:");
        setLabel(view, R.id.detail_alamat, "Alamat:");
        setLabel(view, R.id.detail_tanggal_lahir, "Tanggal Lahir:");
        setLabel(view, R.id.gender, "Gender:");
        setLabel(view, R.id.detail_name_ortu, "Nama Orang Tua:");
        setLabel(view, R.id.detail_phone_ortu, "Nomor Telepon:");

        view.findViewById(R.id.btnKeluar).setOnClickListener(v -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Konfirmasi")
                    .setMessage("Apakah Anda yakin ingin keluar dari perangkat?")
                    .setPositiveButton("Ya", (dialog, which) -> logoutUser())
                    .setNegativeButton("Tidak", (dialog, which) -> dialog.dismiss())
                    .show();
        });

        imgEditIcon.setOnClickListener(v -> showEditDataDialog());
        btnBackHeader.setOnClickListener(v -> {
            if (getActivity() instanceof NavigasiCard) {
                ((NavigasiCard) getActivity()).navigateToDashboard();
            }
        });

        loadProfileData();
    }

    private void loadProfileData() {
        String url = BASE_URL + "?username=" + usernameOrtu;

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET, url, null,
                response -> {
                    try {
                        if (response.getBoolean("success")) {
                            JSONObject data = response.getJSONObject("data");

                            String nama = data.getString("nama_siswa");
                            String alamat = data.getString("alamat");
                            String tglLahir = data.getString("tanggal_lahir");
                            String gender = data.getString("gender");
                            String ortu = data.getString("nama_ortu");
                            String telp = data.getString("no_telp_ortu");
                            String kelas = data.getString("kelas");

                            tvAnakNama.setText(nama);
                            tvAnakAlamat.setText(alamat);
                            tvAnakTglLahir.setText(tglLahir);
                            tvGender.setText(gender);
                            tvOrtuNama.setText(ortu);
                            tvOrtuPhone.setText(telp);
                            tvProfileNameTop.setText(nama);
                            tvProfileClass.setText(kelas.toUpperCase());

                            updateProfileIcon(gender);
                        }
                    } catch (JSONException e) {
                        Toast.makeText(getContext(), "Kesalahan parsing data", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> Toast.makeText(getContext(), "Gagal koneksi server", Toast.LENGTH_SHORT).show()
        );

        requestQueue.add(request);
    }

    private void updateProfileIcon(String gender) {
        if (imgProfilePicture == null) return;

        if (gender.equalsIgnoreCase("perempuan")) {
            imgProfilePicture.setImageResource(R.drawable.icon_cewe);
        } else {
            imgProfilePicture.setImageResource(R.drawable.icon_cowo);
        }
    }

    private void setLabel(View parent, int layoutId, String label) {
        ((TextView) parent.findViewById(layoutId).findViewById(R.id.tvDetailLabel)).setText(label);
    }

    private void showEditDataDialog() {
        Context context = requireContext();
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_edit_data, null);
        builder.setView(dialogView);

        LinearLayout llContainer = dialogView.findViewById(R.id.ll_edit_fields_container);
        Button btnSimpan = dialogView.findViewById(R.id.btn_simpan_dialog);
        Button btnBatal = dialogView.findViewById(R.id.btn_batal_dialog);

        final EditText edtNama = createEdit("Nama Anak", tvAnakNama.getText().toString());
        final EditText edtTgl = createEdit("Tanggal Lahir (YYYY-MM-DD)", tvAnakTglLahir.getText().toString());
        final EditText edtAlamat = createEdit("Alamat", tvAnakAlamat.getText().toString());

        //  Gender pakai Spinner
        final Spinner spGender = new Spinner(context);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Laki-Laki", "Perempuan"});
        spGender.setAdapter(adapter);

        spGender.setSelection(tvGender.getText().toString().equalsIgnoreCase("perempuan") ? 1 : 0);

        llContainer.addView(edtNama);
        llContainer.addView(edtTgl);
        llContainer.addView(edtAlamat);
        llContainer.addView(spGender);

        final EditText edtOrtu = createEdit("Nama Orang Tua", tvOrtuNama.getText().toString());
        final EditText edtTelp = createEdit("Nomor Telepon", tvOrtuPhone.getText().toString());
        final EditText edtKelas = createEdit("Kelas", tvProfileClass.getText().toString());
        edtKelas.setEnabled(false);

        llContainer.addView(edtOrtu);
        llContainer.addView(edtTelp);
        llContainer.addView(edtKelas);

        AlertDialog dialog = builder.create();
        dialog.show();

        btnSimpan.setOnClickListener(v -> {
            updateProfile(
                    edtNama.getText().toString(),
                    edtTgl.getText().toString(),
                    edtAlamat.getText().toString(),
                    spGender.getSelectedItem().toString(),
                    edtOrtu.getText().toString(),
                    edtTelp.getText().toString(),
                    dialog
            );
        });

        btnBatal.setOnClickListener(v -> dialog.dismiss());
    }

    private EditText createEdit(String hint, String value) {
        EditText e = new EditText(requireContext());
        e.setHint(hint);
        e.setText(value);

        e.setPadding(32, 24, 32, 24);
        e.setBackgroundResource(android.R.drawable.edit_text);

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = 20;
        e.setLayoutParams(params);

        return e;
    }

    private void updateProfile(String nama, String tgl, String alamat, String gender,
                               String ortu, String telp, AlertDialog dialog) {

        StringRequest request = new StringRequest(Request.Method.POST, BASE_URL,
                response -> {
                    Toast.makeText(getContext(), "Berhasil update profil", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    loadProfileData();
                },
                error -> Toast.makeText(getContext(), "Gagal update", Toast.LENGTH_SHORT).show()
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> p = new HashMap<>();
                p.put("username", usernameOrtu);
                p.put("nama_siswa", nama);
                p.put("tanggal_lahir", tgl);
                p.put("alamat", alamat);
                p.put("gender", gender);
                p.put("nama_ortu", ortu);
                p.put("no_telp_ortu", telp);
                return p;
            }
        };

        requestQueue.add(request);
    }

    private void logoutUser() {
        SharedPreferences prefs = requireActivity().getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE);
        prefs.edit().clear().apply();

        Intent i = new Intent(getActivity(), MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
    }
}
