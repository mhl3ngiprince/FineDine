package com.finedine.rms;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class ReservationActivity extends BaseActivity {
    private static final String TAG = "ReservationActivity";
    private final Calendar selectedDate = Calendar.getInstance();
    private int selectedHour = 18; // Default 6 PM
    private int selectedMinute = 0;
    private int partySize = 2;
    private TextInputEditText etSpecialRequests;
    private TextInputEditText etCustomerName;
    private TextInputEditText etPhone;
    private TextInputEditText etEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            // Setup modern navigation panel
            setupModernNavigationPanel("Reservations", R.layout.activity_reservation);

            // Initialize input fields
            etSpecialRequests = findViewById(R.id.etSpecialRequests);
            etCustomerName = findViewById(R.id.etCustomerName);
            etPhone = findViewById(R.id.etPhone);
            etEmail = findViewById(R.id.etEmail);

            // Prefill user name and contact if available
            if (prefsManager != null) {
                String userName = prefsManager.getUserName();
                if (userName != null && !userName.isEmpty() && etCustomerName != null) {
                    etCustomerName.setText(userName);
                }
            }

            // Setup button click listeners
            findViewById(R.id.btnDate).setOnClickListener(v -> selectDate(v));
            findViewById(R.id.btnTime).setOnClickListener(v -> selectTime(v));
            findViewById(R.id.etPartySize).setOnClickListener(v -> changePartySize(v));
            findViewById(R.id.btnSubmitReservation).setOnClickListener(v -> confirmReservation(v));

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
                    selectedHour, selectedMinute, false); // Changed to false for 12-hour format
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
                String formattedTime = String.format(Locale.getDefault(), "%02d:%02d %s",
                        selectedHour % 12 == 0 ? 12 : selectedHour % 12,
                        selectedMinute,
                        selectedHour < 12 ? "AM" : "PM");
                timeText.setText(formattedTime);
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
            // Get input values
            String name = etCustomerName.getText().toString().trim();
            String phone = etPhone.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String specialRequests = etSpecialRequests.getText().toString().trim();

            // Validate input fields
            if (name.isEmpty()) {
                etCustomerName.setError("Name is required");
                etCustomerName.requestFocus();
                return;
            }

            if (phone.isEmpty()) {
                etPhone.setError("Phone number is required");
                etPhone.requestFocus();
                return;
            }

            // Show progress dialog
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("Submitting reservation...");
            builder.setCancelable(false);
            AlertDialog progressDialog = builder.create();
            progressDialog.show();

            // Save reservation in database
            new Thread(() -> {
                try {
                    // Get database instance
                    AppDatabase db = AppDatabase.getDatabase(ReservationActivity.this);

                    // Create new reservation
                    Reservation reservation = new Reservation();

                    // Format date for database
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    String formattedDate = dateFormat.format(selectedDate.getTime());

                    // Format time for database
                    String formattedTime = String.format(Locale.getDefault(), "%02d:%02d", selectedHour, selectedMinute);

                    reservation.setReservation_date(formattedDate);
                    reservation.setReservation_time(formattedTime);
                    reservation.setNumber_of_guests(partySize);
                    reservation.setCustomerName(name);
                    reservation.setPhone(phone);
                    reservation.setEmail(email);
                    reservation.setSpecialRequests(specialRequests);
                    reservation.setStatus("pending");

                    // Get user ID from SharedPreferences if available
                    int userId = 1; // Default ID
                    if (prefsManager != null) {
                        userId = prefsManager.getUserId();
                        if (userId <= 0) userId = 1;
                    }
                    reservation.setUser_id(userId);

                    // Insert reservation and get ID
                    long reservationId = db.reservationDao().insert(reservation);
                    final long finalReservationId = reservationId;

                    // Update UI on success
                    runOnUiThread(() -> {
                        try {
                            if (progressDialog.isShowing()) {
                                progressDialog.dismiss();
                            }

                            // Build confirmation message
                            String confirmationMessage = String.format(
                                    "Your reservation for %d people on %s at %02d:%02d %s has been confirmed! Reservation #%d",
                                    partySize,
                                    dateFormat.format(selectedDate.getTime()),
                                    selectedHour % 12 == 0 ? 12 : selectedHour % 12,
                                    selectedMinute,
                                    selectedHour < 12 ? "AM" : "PM",
                                    finalReservationId
                            );

                            // Show success dialog with option to view menu
                            AlertDialog.Builder successBuilder = new AlertDialog.Builder(this);
                            successBuilder.setTitle("Reservation Confirmed!");
                            successBuilder.setMessage(confirmationMessage);
                            successBuilder.setPositiveButton("View Menu", (dialog, which) -> {
                                // Navigate to menu (OrderActivity)
                                Intent intent = new Intent(this, OrderActivity.class);
                                intent.putExtra("user_role", "customer");
                                startActivity(intent);
                            });
                            successBuilder.setNegativeButton("OK", null);
                            successBuilder.show();

                            // Clear input fields
                            etSpecialRequests.setText("");

                            // Send notification
                            sendConfirmationNotification();
                        } catch (Exception e) {
                            Log.e(TAG, "Error updating UI after reservation submission", e);
                        }
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Error submitting reservation", e);

                    // Update UI on error
                    runOnUiThread(() -> {
                        if (progressDialog.isShowing()) {
                            progressDialog.dismiss();
                        }
                        Toast.makeText(ReservationActivity.this,
                                "Error submitting reservation: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                    });
                }
            }).start();
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