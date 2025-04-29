package com.finedine.rms;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class ReservationActivity extends BaseActivity {
    private static final String TAG = "ReservationActivity";
    private final Calendar selectedDate = Calendar.getInstance();
    private int selectedHour = 18; // Default 6 PM
    private int selectedMinute = 0;
    private int partySize = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reservation);

        try {
            // Setup navigation panel
            setupNavigationPanel("Reservations");

            updateDateTimeDisplay();
        } catch (Exception e) {
            Log.e(TAG, "Error initializing ReservationActivity", e);
            Toast.makeText(this, "Error initializing reservation screen", Toast.LENGTH_SHORT).show();
        }
    }

    public void selectDate(View view) {
        try {
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
        } catch (Exception e) {
            Log.e(TAG, "Error selecting date", e);
            Toast.makeText(this, "Could not open date picker", Toast.LENGTH_SHORT).show();
        }
    }

    public void selectTime(View view) {
        try {
            TimePickerDialog timePicker = new TimePickerDialog(this,
                    (view12, hourOfDay, minute) -> {
                        selectedHour = hourOfDay;
                        selectedMinute = minute;
                        updateDateTimeDisplay();
                    },
                    selectedHour, selectedMinute, true);
            timePicker.show();
        } catch (Exception e) {
            Log.e(TAG, "Error selecting time", e);
            Toast.makeText(this, "Could not open time picker", Toast.LENGTH_SHORT).show();
        }
    }

    public void changePartySize(View view) {
        try {
            String[] sizes = new String[]{"1", "2", "3", "4", "5", "6", "7", "8", "10", "12", "15", "20"};

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Select Party Size");
            builder.setItems(sizes, (dialog, which) -> {
                try {
                    partySize = Integer.parseInt(sizes[which]);
                    updateDateTimeDisplay();
                } catch (Exception e) {
                    Log.e(TAG, "Error setting party size", e);
                }
            });
            builder.show();
        } catch (Exception e) {
            Log.e(TAG, "Error changing party size", e);
            Toast.makeText(this, "Could not change party size", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateDateTimeDisplay() {
        try {
            TextView dateText = findViewById(R.id.btnDate);
            TextView timeText = findViewById(R.id.btnTime);
            TextView partyText = findViewById(R.id.etPartySize);

            if (dateText != null) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, MMM d", Locale.getDefault());
                dateText.setText(dateFormat.format(selectedDate.getTime()));
            }

            if (timeText != null) {
                timeText.setText(String.format(Locale.getDefault(), "%02d:%02d", selectedHour, selectedMinute));
            }

            if (partyText != null) {
                partyText.setText(String.format(Locale.getDefault(), "%d people", partySize));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating date/time display", e);
        }
    }

    public void confirmReservation(View view) {
        try {
            Toast.makeText(this, "Thank you! Your reservation is confirmed.", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Log.e(TAG, "Error confirming reservation", e);
            Toast.makeText(this, "Error confirming reservation", Toast.LENGTH_SHORT).show();
        }
    }

    private void sendConfirmationNotification() {
        // Simplified notification without Firebase
        Toast.makeText(this, "Reservation confirmed!", Toast.LENGTH_SHORT).show();
    }
}