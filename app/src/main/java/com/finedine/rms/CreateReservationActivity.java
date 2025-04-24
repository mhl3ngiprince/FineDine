package com.finedine.rms;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
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

public class CreateReservationActivity extends AppCompatActivity {

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
        
        // Set up back button in toolbar
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Create Reservation");
    }
    
    private void showDatePickerDialog() {
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
    }
    
    private void showTimePickerDialog() {
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
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}