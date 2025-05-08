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
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

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

        try {
            // Use modern navigation panel
            setupModernNavigationPanel("Create Reservation", R.layout.activity_create_reservation);

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

            // Verify that required UI components are found
            if (dateEditText == null || timeEditText == null ||
                    customerNameEditText == null || guestsSpinner == null || saveButton == null) {
                Toast.makeText(this, "Error loading reservation form elements", Toast.LENGTH_SHORT).show();
                // Create a fallback minimal layout with essential fields rather than showing black screen
                createEmergencyReservationForm();
                return;
            }

            // Set up date picker dialog
            dateEditText.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showDatePicker(dateEditText);
                }
            });

            // Set up time picker dialog
            timeEditText.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showTimePicker(timeEditText);
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
            // Create a fallback layout if initialization fails
            createEmergencyReservationForm();
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

                            // Show feedback about selected time
                            String timeFeedback;
                            if (hourOfDay < 11) {
                                timeFeedback = "Morning reservation selected";
                            } else if (hourOfDay < 15) {
                                timeFeedback = "Lunch time reservation selected";
                            } else if (hourOfDay < 18) {
                                timeFeedback = "Afternoon reservation selected";
                            } else {
                                timeFeedback = "Evening reservation selected";
                            }
                            Toast.makeText(CreateReservationActivity.this,
                                    timeFeedback, Toast.LENGTH_SHORT).show();
                        }
                    },
                    // Set initial time to next available hour
                    calendar.get(Calendar.HOUR_OF_DAY) + 1,
                    0, // Start at top of hour
                    false
            );

            // Set business hours (11am to 10pm)
            timePickerDialog.updateTime(11, 0);
            timePickerDialog.setTitle("Select Reservation Time");
            timePickerDialog.show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing time picker", e);
            Toast.makeText(this, "Error with time selection", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void updateDateField() {
        String format = "EEE, MMM d, yyyy";
        SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.US);
        dateEditText.setText(sdf.format(calendar.getTime()));
        dateEditText.setTextColor(ContextCompat.getColor(this, android.R.color.black));
    }
    
    private void updateTimeField() {
        String format = "h:mm a";
        SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.US);
        timeEditText.setText(sdf.format(calendar.getTime()));
        timeEditText.setTextColor(ContextCompat.getColor(this, android.R.color.black));
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

    private void createEmergencyReservationForm() {
        try {
            // Create a simple linear layout
            LinearLayout layout = new LinearLayout(this);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setPadding(30, 50, 30, 30);

            // Add title
            TextView titleText = new TextView(this);
            titleText.setText("Create Reservation");
            titleText.setTextSize(24);
            titleText.setPadding(0, 0, 0, 30);
            layout.addView(titleText);

            // Add date field
            TextView dateLabel = new TextView(this);
            dateLabel.setText("Date:");
            layout.addView(dateLabel);

            dateEditText = new EditText(this);
            dateEditText.setHint("Select Date");
            dateEditText.setFocusable(false);
            dateEditText.setOnClickListener(v -> showDatePickerDialog());
            layout.addView(dateEditText);

            // Add time field
            TextView timeLabel = new TextView(this);
            timeLabel.setText("Time:");
            timeLabel.setPadding(0, 20, 0, 0);
            layout.addView(timeLabel);

            timeEditText = new EditText(this);
            timeEditText.setHint("Select Time");
            timeEditText.setFocusable(false);
            timeEditText.setOnClickListener(v -> showTimePickerDialog());
            layout.addView(timeEditText);

            // Add name field
            TextView nameLabel = new TextView(this);
            nameLabel.setText("Name:");
            nameLabel.setPadding(0, 20, 0, 0);
            layout.addView(nameLabel);

            customerNameEditText = new EditText(this);
            customerNameEditText.setHint("Your Name");
            layout.addView(customerNameEditText);

            // Add phone field
            TextView phoneLabel = new TextView(this);
            phoneLabel.setText("Phone:");
            phoneLabel.setPadding(0, 20, 0, 0);
            layout.addView(phoneLabel);

            phoneEditText = new EditText(this);
            phoneEditText.setHint("Phone Number");
            phoneEditText.setInputType(android.text.InputType.TYPE_CLASS_PHONE);
            layout.addView(phoneEditText);

            // Add button
            Button saveButtonEmergency = new Button(this);
            saveButtonEmergency.setText("Save Reservation");
            saveButtonEmergency.setBackgroundColor(getResources().getColor(R.color.luxe_burgundy));
            saveButtonEmergency.setPadding(20, 20, 20, 20);
            saveButtonEmergency.setOnClickListener(v -> saveReservation());
            LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            buttonParams.setMargins(0, 40, 0, 0);
            saveButtonEmergency.setLayoutParams(buttonParams);
            layout.addView(saveButtonEmergency);

            // Back button
            Button backButton = new Button(this);
            backButton.setText("Go Back");
            backButton.setOnClickListener(v -> onBackPressed());
            layout.addView(backButton);

            // Set this as the content view
            setContentView(layout);

            // Initialize other required variables
            calendar = Calendar.getInstance();

            // Initialize Firebase components
            db = FirebaseFirestore.getInstance();
            mAuth = FirebaseAuth.getInstance();

            Toast.makeText(this, "Using simplified reservation form", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Failed to create emergency reservation form", e);
            // If even this fails, show a simple error message
            TextView errorText = new TextView(this);
            errorText.setText("Error loading reservation form. Please try again later.");
            errorText.setPadding(50, 100, 50, 50);
            setContentView(errorText);

            // Add back button
            Button backButton = new Button(this);
            backButton.setText("Go Back");
            backButton.setOnClickListener(v -> onBackPressed());

            // Create a layout to hold both views
            LinearLayout errorLayout = new LinearLayout(this);
            errorLayout.setOrientation(LinearLayout.VERTICAL);
            errorLayout.addView(errorText);
            errorLayout.addView(backButton);

            setContentView(errorLayout);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }
}