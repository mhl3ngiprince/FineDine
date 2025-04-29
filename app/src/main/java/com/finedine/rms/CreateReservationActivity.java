package com.finedine.rms;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class CreateReservationActivity extends BaseActivity {

    private static final String TAG = "CreateReservation";
    private EditText dateEditText, timeEditText, customerNameEditText, 
                     phoneEditText, emailEditText, specialRequestsEditText;
    private Spinner guestsSpinner;
    private Button saveButton;
    private Calendar calendar;
    
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_reservation);

        try {
            // Setup navigation panel
            setupNavigationPanel("Create Reservation");

            // Initialize Firebase
            db = FirebaseFirestore.getInstance();
            mAuth = FirebaseAuth.getInstance();

            // Initialize calendar for date and time pickers
            calendar = Calendar.getInstance();

            // Initialize UI components
            dateEditText = findViewById(R.id.dateEditText);
            timeEditText = findViewById(R.id.timeEditText);
            customerNameEditText = findViewById(R.id.customerNameEditText);
            phoneEditText = findViewById(R.id.phoneEditText);
            emailEditText = findViewById(R.id.emailEditText);
            specialRequestsEditText = findViewById(R.id.specialRequestsEditText);
            guestsSpinner = findViewById(R.id.guestsSpinner);
            saveButton = findViewById(R.id.saveButton);

            // Set up date picker dialog
            dateEditText.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showDatePickerDialog();
                }
            });

            // Set up time picker dialog
            timeEditText.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showTimePickerDialog();
                }
            });

            // Set up save button
            saveButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    saveReservation();
                }
            });

            // Pre-fill customer data if user is logged in
            prefillUserData();

        } catch (Exception e) {
            Log.e(TAG, "Error initializing CreateReservationActivity", e);
            Toast.makeText(this, "Error initializing reservation screen", Toast.LENGTH_SHORT).show();
        }
    }

    private void prefillUserData() {
        try {
            FirebaseUser user = mAuth.getCurrentUser();
            if (user != null) {
                // Set email from Firebase user
                emailEditText.setText(user.getEmail());

                // If we have user's name in Firebase
                if (user.getDisplayName() != null && !user.getDisplayName().isEmpty()) {
                    customerNameEditText.setText(user.getDisplayName());
                }

                // Get additional user data from Firestore
                db.collection("users").document(user.getUid()).get()
                        .addOnSuccessListener(documentSnapshot -> {
                            if (documentSnapshot.exists()) {
                                String name = documentSnapshot.getString("name");
                                String phone = documentSnapshot.getString("phone");

                                if (name != null && !name.isEmpty()) {
                                    customerNameEditText.setText(name);
                                }

                                if (phone != null && !phone.isEmpty()) {
                                    phoneEditText.setText(phone);
                                }
                            }
                        });
            }
        } catch (Exception e) {
            Log.e(TAG, "Error prefilling user data", e);
        }
    }
    
    private void showDatePickerDialog() {
        try {
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    this,
                    new DatePickerDialog.OnDateSetListener() {
                        @Override
                        public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                            calendar.set(Calendar.YEAR, year);
                            calendar.set(Calendar.MONTH, month);
                            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                            updateDateField();
                        }
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            );
            datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
            datePickerDialog.show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing date picker", e);
            Toast.makeText(this, "Error with date selection", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void showTimePickerDialog() {
        try {
            TimePickerDialog timePickerDialog = new TimePickerDialog(
                    this,
                    new TimePickerDialog.OnTimeSetListener() {
                        @Override
                        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                            calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                            calendar.set(Calendar.MINUTE, minute);
                            updateTimeField();
                        }
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    false
            );
            timePickerDialog.show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing time picker", e);
            Toast.makeText(this, "Error with time selection", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void updateDateField() {
        String format = "MM/dd/yyyy";
        SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.US);
        dateEditText.setText(sdf.format(calendar.getTime()));
    }
    
    private void updateTimeField() {
        String format = "hh:mm a";
        SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.US);
        timeEditText.setText(sdf.format(calendar.getTime()));
    }
    
    private void saveReservation() {
        try {
            // Validate input fields
            if (!validateFields()) {
                return;
            }

            // Get current user
            FirebaseUser currentUser = mAuth.getCurrentUser();
            String userId = currentUser != null ? currentUser.getUid() : "";

            // Create reservation data
            Map<String, Object> reservation = new HashMap<>();
            reservation.put("user_id", userId);
            reservation.put("reservation_date", dateEditText.getText().toString());
            reservation.put("reservation_time", timeEditText.getText().toString());
            reservation.put("number_of_guests", Integer.parseInt(guestsSpinner.getSelectedItem().toString()));
            reservation.put("status", "Pending");
            reservation.put("customerName", customerNameEditText.getText().toString());
            reservation.put("phone", phoneEditText.getText().toString());
            reservation.put("email", emailEditText.getText().toString());
            reservation.put("specialRequests", specialRequestsEditText.getText().toString());
            reservation.put("timestamp", System.currentTimeMillis());

            // Save to Firestore
            db.collection("reservations")
                    .add(reservation)
                    .addOnSuccessListener(documentReference -> {
                        // Also save to local database
                        saveToLocalDatabase(documentReference.getId(), reservation);

                        Toast.makeText(CreateReservationActivity.this,
                                "Reservation created successfully",
                                Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(CreateReservationActivity.this,
                                "Failed to create reservation: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });
        } catch (Exception e) {
            Log.e(TAG, "Error saving reservation", e);
            Toast.makeText(this, "Error creating reservation: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void saveToLocalDatabase(String reservationId, Map<String, Object> reservationData) {
        try {
            AppDatabase db = AppDatabase.getDatabase(this);

            new Thread(() -> {
                try {
                    Reservation localReservation = new Reservation();
                    // Convert string user_id to int (0 if parsing fails)
                    try {
                        localReservation.user_id = Integer.parseInt((String) reservationData.get("user_id"));
                    } catch (Exception e) {
                        localReservation.user_id = 0;
                    }

                    localReservation.reservation_date = (String) reservationData.get("reservation_date");
                    localReservation.reservation_time = (String) reservationData.get("reservation_time");

                    // Get number_of_guests as Integer from the map
                    try {
                        Object guestsObj = reservationData.get("number_of_guests");
                        if (guestsObj instanceof Integer) {
                            localReservation.number_of_guests = (Integer) guestsObj;
                        } else if (guestsObj instanceof String) {
                            localReservation.number_of_guests = Integer.parseInt((String) guestsObj);
                        }
                    } catch (Exception e) {
                        localReservation.number_of_guests = 1; // Default value
                    }

                    localReservation.status = (String) reservationData.get("status");
                    localReservation.customerName = (String) reservationData.get("customerName");
                    localReservation.phone = (String) reservationData.get("phone");
                    localReservation.email = (String) reservationData.get("email");
                    localReservation.specialRequests = (String) reservationData.get("specialRequests");

                    // Don't set reservation_id as it's auto-generated

                    db.reservationDao().insert(localReservation);
                } catch (Exception e) {
                    Log.e(TAG, "Error saving to local database", e);
                }
            }).start();
        } catch (Exception e) {
            Log.e(TAG, "Error accessing local database", e);
        }
    }

    private boolean validateFields() {
        boolean valid = true;
        
        if (TextUtils.isEmpty(dateEditText.getText())) {
            dateEditText.setError("Required");
            valid = false;
        }
        
        if (TextUtils.isEmpty(timeEditText.getText())) {
            timeEditText.setError("Required");
            valid = false;
        }
        
        if (TextUtils.isEmpty(customerNameEditText.getText())) {
            customerNameEditText.setError("Required");
            valid = false;
        }
        
        if (TextUtils.isEmpty(phoneEditText.getText())) {
            phoneEditText.setError("Required");
            valid = false;
        }
        
        return valid;
    }
    
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }
}