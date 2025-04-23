package com.finedine.rms;



import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;
import com.finedine.rms.R;

import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;



public class ReservationActivity extends AppCompatActivity {
    private final Calendar selectedDate = Calendar.getInstance();
    private int selectedHour = 18; // Default 6 PM
    private int selectedMinute = 0;
    private int partySize = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reservation);

        updateDateTimeDisplay();
    }

    public void selectDate(View view) {
        DatePickerDialog datePicker = new DatePickerDialog(this,
                (view1, year, month, dayOfMonth) -> {
                    selectedDate.set(year, month, dayOfMonth);
                    updateDateTimeDisplay();
                },
                selectedDate.get(Calendar.YEAR),
                selectedDate.get(Calendar.MONTH),
                selectedDate.get(Calendar.DAY_OF_MONTH));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            datePicker.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        }
        datePicker.show();
    }

    public void selectTime(View view) {
        TimePickerDialog timePicker = new TimePickerDialog(this,
                (view12, hourOfDay, minute) -> {
                    selectedHour = hourOfDay;
                    selectedMinute = minute;
                    updateDateTimeDisplay();
                },
                selectedHour, selectedMinute, true);
        timePicker.show();
    }

    public void changePartySize(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Party Size");

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_party_size, null);
         @SuppressLint({"MissingInflatedId", "LocalSuppress"}) NumberPicker numberPicker = dialogView.findViewById(R.id.etPartySize);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            numberPicker.setMinValue(1);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            numberPicker.setMaxValue(20);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            numberPicker.setValue(partySize);
        }

        builder.setView(dialogView);
        builder.setPositiveButton("OK", (dialog, which) -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                partySize = numberPicker.getValue();
            }
            updateDateTimeDisplay();
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void updateDateTimeDisplay() {
        TextView dateText = findViewById(R.id.btnDate);
        TextView timeText = findViewById(R.id.btnTime);

        TextView partyText = findViewById(R.id.etPartySize);

        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, MMM d", Locale.getDefault());
        dateText.setText(dateFormat.format(selectedDate.getTime()));

        timeText.setText(String.format(Locale.getDefault(), "%02d:%02d", selectedHour, selectedMinute));
        partyText.setText(getResources().getQuantityString(R.plurals.party_size, partySize, partySize));
    }

    public void confirmReservation(View view) {
        Reservation reservation = new Reservation();
        reservation.user_id = getTaskId(); // Get from shared prefs
        reservation.reservation_date = new SimpleDateFormat("yyyy-MM-dd").format(selectedDate.getTime());
        reservation.reservation_time = String.format(Locale.getDefault(), "%02d:%02d:00", selectedHour, selectedMinute);
        reservation.number_of_guests = partySize;
        reservation.status = "pending";

        new Thread(() -> {
            AppDatabase.getDatabase(this).reservationDao().insert(reservation);
            runOnUiThread(() -> {
                Toast.makeText(this, "Reservation requested!", Toast.LENGTH_SHORT).show();
                sendConfirmationNotification();
                finish();
            });
        }).start();
    }
    public void showPartySizeDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_party_size, null);

       // NumberPicker numberPicker = dialogView.findViewById(R.id.numberPicker);
      /*  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            numberPicker.setMinValue(1);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            numberPicker.setMaxValue(20);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            numberPicker.setValue(2); // Default value
        }*/

       /* builder.setView(dialogView)
                .setPositiveButton("OK", (dialog, which) -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                        int partySize = numberPicker.getValue();
                    }
                    // Handle the selected party size
                })
                .setNegativeButton("Cancel", null)*/;

        AlertDialog dialog = builder.create();
        dialog.show();
    }
    private void sendConfirmationNotification() {
        // Implementation would use Firebase Cloud Messaging
    }
}