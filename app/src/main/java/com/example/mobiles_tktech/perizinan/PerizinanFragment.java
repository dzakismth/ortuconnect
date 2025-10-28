package com.example.mobiles_tktech.perizinan;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.mobiles_tktech.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class PerizinanFragment extends Fragment {

    private EditText edtTanggalMulai;
    private EditText edtTanggalSelesai;

    // Objek Calendar untuk menyimpan tanggal
    final Calendar calendar = Calendar.getInstance();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Menginflate layout yang benar: activity_perizinan.xml
        return inflater.inflate(R.layout.activity_ajukan_izin, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Dapatkan referensi EditText dari layout
        edtTanggalMulai = view.findViewById(R.id.edtTanggalMulai);
        edtTanggalSelesai = view.findViewById(R.id.edtTanggalSelesai);

        // 2. Pasang listener klik pada kedua EditText
        edtTanggalMulai.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePickerDialog(edtTanggalMulai);
            }
        });

        edtTanggalSelesai.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePickerDialog(edtTanggalSelesai);
            }
        });
    }

    /**
     * Menampilkan DatePickerDialog dan mengatur tanggal yang dipilih ke EditText yang diberikan.
     * @param editText EditText yang akan diperbarui (edtTanggalMulai atau edtTanggalSelesai).
     */
    private void showDatePickerDialog(final EditText editText) {
        // Mendapatkan tahun, bulan, dan hari saat ini
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        // Membuat DatePickerDialog
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(),
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(android.widget.DatePicker view, int selectedYear, int selectedMonth, int selectedDay) {
                        // Memperbarui objek Calendar dengan tanggal yang dipilih
                        calendar.set(Calendar.YEAR, selectedYear);
                        calendar.set(Calendar.MONTH, selectedMonth);
                        calendar.set(Calendar.DAY_OF_MONTH, selectedDay);

                        // Memformat tanggal ke format dd/MM/yyyy dan memperbarui EditText
                        String dateFormat = "dd/MM/yyyy";
                        SimpleDateFormat sdf = new SimpleDateFormat(dateFormat, Locale.US);

                        editText.setText(sdf.format(calendar.getTime()));
                    }
                },
                year, month, day);

        // Menampilkan dialog
        datePickerDialog.show();
    }
}
