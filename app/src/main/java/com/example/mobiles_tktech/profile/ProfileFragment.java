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
import com.example.mobiles_tktech.MainActivityKt;
import com.example.mobiles_tktech.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class ProfileFragment extends Fragment {

    private TextView tvAnakNama, tvAnakAlamat, tvAnakTglLahir, tvOrtuNama, tvOrtuPhone, tvProfileNameTop, tvProfileClass;
    private ImageView imgEditIcon;
    private RequestQueue requestQueue;

    private static final String BASE_URL = "http://ortuconnect.atwebpages.com/api/profile.php";
    private String usernameOrtu = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        requestQueue = Volley.newRequestQueue(requireContext());

        SharedPreferences prefs = requireActivity().getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE);
        usernameOrtu = prefs.getString("username", "");

        tvAnakNama = view.findViewById(R.id.detail_name_anak).findViewById(R.id.tvDetailValue);
        tvAnakAlamat = view.findViewById(R.id.detail_alamat).findViewById(R.id.tvDetailValue);
        tvAnakTglLahir = view.findViewById(R.id.detail_tanggal_lahir).findViewById(R.id.tvDetailValue);
        tvOrtuNama = view.findViewById(R.id.detail_name_ortu).findViewById(R.id.tvDetailValue);
        tvOrtuPhone = view.findViewById(R.id.detail_phone_ortu).findViewById(R.id.tvDetailValue);
        tvProfileNameTop = view.findViewById(R.id.tvProfileNameTop);
        tvProfileClass = view.findViewById(R.id.tvProfileClass);
        imgEditIcon = view.findViewById(R.id.imgEditProfileIcon);

        ((TextView) view.findViewById(R.id.detail_name_anak).findViewById(R.id.tvDetailLabel)).setText("Nama Anak:");
        ((TextView) view.findViewById(R.id.detail_alamat).findViewById(R.id.tvDetailLabel)).setText("Alamat:");
        ((TextView) view.findViewById(R.id.detail_tanggal_lahir).findViewById(R.id.tvDetailLabel)).setText("Tanggal Lahir:");
        ((TextView) view.findViewById(R.id.detail_name_ortu).findViewById(R.id.tvDetailLabel)).setText("Nama Orang Tua:");
        ((TextView) view.findViewById(R.id.detail_phone_ortu).findViewById(R.id.tvDetailLabel)).setText("Nomor Telepon:");

        view.findViewById(R.id.btnKeluar).setOnClickListener(v -> logoutUser());
        imgEditIcon.setOnClickListener(v -> showEditDataDialog());

        loadProfileData();
    }

    private void loadProfileData() {
        String url = BASE_URL + "?username=" + usernameOrtu;

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        if (response.getBoolean("success")) {
                            JSONObject data = response.getJSONObject("data");

                            tvAnakNama.setText(data.getString("nama_siswa"));
                            tvAnakAlamat.setText(data.getString("alamat"));
                            tvAnakTglLahir.setText(data.getString("tanggal_lahir"));
                            tvOrtuNama.setText(data.getString("nama_ortu"));
                            tvOrtuPhone.setText(data.getString("no_telp_ortu"));
                            tvProfileNameTop.setText(data.getString("nama_siswa"));
                            tvProfileClass.setText(data.getString("kelas").toUpperCase());
                        } else {
                            Toast.makeText(getContext(), "Data tidak ditemukan", Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                },
                error -> Toast.makeText(getContext(), "Gagal koneksi ke server", Toast.LENGTH_SHORT).show()
        );

        requestQueue.add(request);
    }

    private void showEditDataDialog() {
        Context context = requireContext();
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.dialog_edit_data, null);
        builder.setView(dialogView);

        LinearLayout llContainer = dialogView.findViewById(R.id.ll_edit_fields_container);
        Button btnSimpan = dialogView.findViewById(R.id.btn_simpan_dialog);
        Button btnBatal = dialogView.findViewById(R.id.btn_batal_dialog);

        final EditText edtNamaAnak = createEditText(context, "Nama Anak", tvAnakNama.getText().toString());
        final EditText edtTanggalLahir = createEditText(context, "Tanggal Lahir", tvAnakTglLahir.getText().toString());
        final EditText edtAlamat = createEditText(context, "Alamat", tvAnakAlamat.getText().toString());
        final EditText edtNamaOrtu = createEditText(context, "Nama Orang Tua", tvOrtuNama.getText().toString());
        final EditText edtNoTelp = createEditText(context, "Nomor Telepon", tvOrtuPhone.getText().toString());
        final EditText edtKelas = createEditText(context, "Kelas", tvProfileClass.getText().toString());
        edtKelas.setEnabled(false);

        llContainer.addView(edtNamaAnak);
        llContainer.addView(edtTanggalLahir);
        llContainer.addView(edtAlamat);
        llContainer.addView(edtNamaOrtu);
        llContainer.addView(edtNoTelp);
        llContainer.addView(edtKelas);

        AlertDialog dialog = builder.create();
        dialog.show();

        btnSimpan.setOnClickListener(v -> updateProfile(
                edtNamaAnak.getText().toString(),
                edtTanggalLahir.getText().toString(),
                edtAlamat.getText().toString(),
                edtNamaOrtu.getText().toString(),
                edtNoTelp.getText().toString(),
                dialog
        ));

        btnBatal.setOnClickListener(v -> dialog.dismiss());
    }

    private EditText createEditText(Context context, String hint, String value) {
        EditText edt = new EditText(context);
        edt.setHint(hint);
        edt.setText(value);
        edt.setPadding(24, 16, 24, 16);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = 16;
        edt.setLayoutParams(params);
        return edt;
    }

    private void updateProfile(String nama, String tgl, String alamat, String ortu, String telp, AlertDialog dialog) {
        StringRequest request = new StringRequest(Request.Method.POST, BASE_URL,
                response -> {
                    Toast.makeText(getContext(), "Profil berhasil diperbarui", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    loadProfileData();
                },
                error -> Toast.makeText(getContext(), "Gagal memperbarui profil", Toast.LENGTH_SHORT).show()) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("username", usernameOrtu);
                params.put("nama_siswa", nama);
                params.put("tanggal_lahir", tgl);
                params.put("alamat", alamat);
                params.put("nama_ortu", ortu);
                params.put("no_telp_ortu", telp);
                return params;
            }
        };
        requestQueue.add(request);
    }

    private void logoutUser() {
        SharedPreferences sharedPref = requireActivity().getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE);
        sharedPref.edit().clear().apply();

        Intent intent = new Intent(getActivity(), MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }
}
