package com.example.mobiles_tktech.profile;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.mobiles_tktech.MainActivitycok;
import com.example.mobiles_tktech.R;

public class ProfileFragment extends Fragment {

    private TextView tvAnakNama;
    private TextView tvAnakAlamat;
    private TextView tvAnakTglLahir;
    private TextView tvOrtuNama;
    private TextView tvOrtuPhone;
    private ImageView imgEditIcon;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);


        tvAnakNama = view.findViewById(R.id.detail_name_anak).findViewById(R.id.tvDetailValue);
        tvAnakAlamat = view.findViewById(R.id.detail_alamat).findViewById(R.id.tvDetailValue);
        tvAnakTglLahir = view.findViewById(R.id.detail_tanggal_lahir).findViewById(R.id.tvDetailValue);

        tvOrtuNama = view.findViewById(R.id.detail_name_ortu).findViewById(R.id.tvDetailValue);
        tvOrtuPhone = view.findViewById(R.id.detail_phone_ortu).findViewById(R.id.tvDetailValue);

        imgEditIcon = view.findViewById(R.id.imgEditProfileIcon);


        loadProfileData();

        Button btnLogout = view.findViewById(R.id.btnKeluar);
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> logoutUser());
        }

        if (imgEditIcon != null) {
            imgEditIcon.setOnClickListener(v -> showEditDataDialog());
        }
    }

    private void loadProfileData() {

        tvAnakNama.setText("");
        tvAnakAlamat.setText("");
        tvAnakTglLahir.setText("");

        tvOrtuNama.setText("");
        tvOrtuPhone.setText("");
    }


    private void showEditDataDialog() {
        Context context = requireContext();
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.dialog_edit_data, null);
        builder.setView(dialogView);

        Button btnSimpan = dialogView.findViewById(R.id.btn_simpan_dialog);
        Button btnBatal = dialogView.findViewById(R.id.btn_batal_dialog);
        LinearLayout llEditFieldsContainer = dialogView.findViewById(R.id.ll_edit_fields_container);


        final EditText edtNamaAnak = createEditText(context, "Nama Anak", tvAnakNama.getText().toString());
        final EditText edtAlamat = createEditText(context, "Alamat", tvAnakAlamat.getText().toString());
        final EditText edtTglLahir = createEditText(context, "Tanggal Lahir", tvAnakTglLahir.getText().toString());
        final EditText edtNamaOrtu = createEditText(context, "Nama Orang Tua", tvOrtuNama.getText().toString());
        final EditText edtNomorTelepon = createEditText(context, "Nomor Telepon", tvOrtuPhone.getText().toString());

        llEditFieldsContainer.addView(edtNamaAnak);
        llEditFieldsContainer.addView(edtAlamat);
        llEditFieldsContainer.addView(edtTglLahir);
        llEditFieldsContainer.addView(edtNamaOrtu);
        llEditFieldsContainer.addView(edtNomorTelepon);

        final AlertDialog alertDialog = builder.create();
        alertDialog.show();

        btnSimpan.setOnClickListener(v -> {
            String newNamaAnak = edtNamaAnak.getText().toString();
            String newAlamat = edtAlamat.getText().toString();
            String newTglLahir = edtTglLahir.getText().toString();
            String newNamaOrtu = edtNamaOrtu.getText().toString();
            String newTelepon = edtNomorTelepon.getText().toString();


            tvAnakNama.setText(newNamaAnak);
            tvAnakAlamat.setText(newAlamat);
            tvAnakTglLahir.setText(newTglLahir);
            tvOrtuNama.setText(newNamaOrtu);
            tvOrtuPhone.setText(newTelepon);

            Toast.makeText(context, "Data berhasil diperbarui!", Toast.LENGTH_SHORT).show();
            alertDialog.dismiss();
        });

        btnBatal.setOnClickListener(v -> {
            Toast.makeText(context, "Edit dibatalkan.", Toast.LENGTH_SHORT).show();
            alertDialog.dismiss();
        });
    }

    private EditText createEditText(Context context, String hint, String initialValue) {
        EditText editText = new EditText(context);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.bottomMargin = 16;

        editText.setLayoutParams(params);
        editText.setHint(hint);
        editText.setText(initialValue);
        editText.setPadding(30, 30, 30, 30);
        editText.setTextSize(14);
        editText.setTextColor(getResources().getColor(android.R.color.black));


        return editText;
    }


    private void logoutUser() {
        SharedPreferences sharedPref = requireActivity().getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();

        editor.clear();
        editor.apply();

        Intent intent = new Intent(getActivity(), MainActivitycok.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }
}