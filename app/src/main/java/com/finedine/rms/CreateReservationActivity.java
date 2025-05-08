package com.finedine.rms;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DatabaseReference;
import com.finedine.rms.utils.FirebaseConnectivityManager;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
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
    private FirebaseConnectivityManager firebaseConnectivityManager;
    private AlertDialog loadingDialog = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            // Set content view directly first to ensure our layout is loaded properly
            setContentView(R.layout.activity_create_reservation);

            // Then setup modern navigation without setting content view again
            setupModernNavigationPanel("Create Reservation", 0);

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

            // Ensure the button is visible and clickable
            saveButton.setVisibility(View.VISIBLE);
            saveButton.setEnabled(true);

            // Fix for the button visibility issue - make it appear above navigation
            if (saveButton.getParent() instanceof ViewGroup) {
                ViewGroup parent = (ViewGroup) saveButton.getParent();

                // Set margin bottom to lift the button above navigation
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                params.setMargins(0, 0, 0, 160); // Add larger bottom margin to ensure button stays above navigation
                saveButton.setLayoutParams(params);

                // Make the button stand out more
                saveButton.setTextSize(16);
                saveButton.setPadding(0, 20, 0, 20);
                saveButton.setElevation(10f);
                saveButton.setZ(10f);
            }

            // Pre-fill customer data if user is logged in
            prefillUserData();

            // Add direct onClick listener to saveButton as an extra precaution
            if (saveButton != null) {
                // This is redundant but ensures the button works even if the XML onClick is broken
                saveButton.setOnClickListener(v -> saveReservation());

                // Make save button more visible by adding a highlighting border
                android.graphics.drawable.GradientDrawable border = new android.graphics.drawable.GradientDrawable();
                border.setColor(getResources().getColor(R.color.luxe_burgundy, getTheme()));
                border.setStroke(6, getResources().getColor(R.color.luxe_gold, getTheme()));
                border.setCornerRadius(12f);
                saveButton.setBackground(border);

                // Set text color explicitly
                saveButton.setTextColor(getResources().getColor(R.color.luxe_gold, getTheme()));
            }

            // Confirm initialization is complete with a toast message
            Toast.makeText(this, "Ready to create your reservation", Toast.LENGTH_SHORT).show();

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

    private void showDatePicker(EditText dateEditText) {
        try {
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    this,
                    new DatePickerDialog.OnDateSetListener() {
                        @Override
                        public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                            calendar.set(Calendar.YEAR, year);
                            calendar.set(Calendar.MONTH, month);
                            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                            // Update the date field with selected date
                            String format = "EEE, MMM d, yyyy";
                            SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.US);
                            dateEditText.setText(sdf.format(calendar.getTime()));
                            dateEditText.setTextColor(ContextCompat.getColor(CreateReservationActivity.this, android.R.color.black));
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

    private void showTimePicker(EditText timeEditText) {
        try {
            TimePickerDialog timePickerDialog = new TimePickerDialog(
                    this,
                    new TimePickerDialog.OnTimeSetListener() {
                        @Override
                        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                            calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                            calendar.set(Calendar.MINUTE, minute);

                            // Update the time field with selected time
                            String format = "h:mm a";
                            SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.US);
                            timeEditText.setText(sdf.format(calendar.getTime()));
                            timeEditText.setTextColor(ContextCompat.getColor(CreateReservationActivity.this, android.R.color.black));

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
    
    private void saveReservation() {
        try {
            // Make sure the UI components are not null before proceeding
            if (dateEditText == null || timeEditText == null || guestsSpinner == null ||
                    customerNameEditText == null || phoneEditText == null ||
                    emailEditText == null || specialRequestsEditText == null) {

                Toast.makeText(this, "Error: Some form elements are missing. Please try again.", Toast.LENGTH_LONG).show();
                Log.e(TAG, "Some UI components are null in saveReservation");
                return;
            }

            // Validate input fields
            if (!validateFields()) {
                return;
            }

            // Create direct feedback to user that submission is starting
            Toast.makeText(this, "Processing your reservation...", Toast.LENGTH_SHORT).show();

            // Get current user
            FirebaseUser currentUser = mAuth.getCurrentUser();
            String userId = currentUser != null ? currentUser.getUid() : "";

            // Check Firebase connectivity
            boolean isNetworkAvailable = firebaseConnectivityManager != null &&
                    firebaseConnectivityManager.isNetworkAvailable();

            if (!isNetworkAvailable) {
                saveToLocalDatabaseDirectly(createReservationData(userId), null);
                return;
            }

            // Create reservation data
            Map<String, Object> reservation = createReservationData(userId);

            // Show loading dialog with better styling and handling
            loadingDialog = createProgressDialog("Submitting Reservation",
                    "Please wait while we process your reservation...");

            if (loadingDialog != null) {
                loadingDialog.show();
            }

            // Save to Firebase Realtime Database first
            try {
                // Get Firebase reference using the connectivity manager for better reliability
                FirebaseDatabase database = firebaseConnectivityManager.getRealtimeDatabase();
                DatabaseReference reservationsRef = database.getReference("reservations");
                String key = reservationsRef.push().getKey();

                if (key != null) {
                    // Add the key to the reservation data
                    reservation.put("id", key);

                    // Make sure the app doesn't freeze if Firebase takes too long
                    Runnable timeoutRunnable = () -> {
                        if (loadingDialog != null && loadingDialog.isShowing()) {
                            try {
                                Toast.makeText(CreateReservationActivity.this,
                                        "Taking longer than expected, please wait...",
                                        Toast.LENGTH_SHORT).show();
                            } catch (Exception e) {
                                Log.e(TAG, "Error showing timeout toast", e);
                            }
                        }
                    };

                    // Set a timeout handler with weak reference to avoid memory leaks
                    final WeakReference<CreateReservationActivity> weakActivity = new WeakReference<>(this);
                    new android.os.Handler().postDelayed(() -> {
                        CreateReservationActivity activity = weakActivity.get();
                        if (activity != null && !activity.isFinishing()) {
                            timeoutRunnable.run();
                        }
                    }, 8000);

                    // Save to Firebase Realtime Database
                    reservationsRef.child(key).setValue(reservation)
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "Reservation saved to Firebase Realtime Database");

                                // Now also save to Firestore for backup
                                saveToFirestore(key, reservation, loadingDialog);
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to save to Firebase Realtime Database: " + e.getMessage());
                                // Try Firestore as backup
                                saveToFirestore(key, reservation, loadingDialog);
                            });
                } else {
                    // Fall back to just Firestore
                    saveToFirestore(null, reservation, loadingDialog);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error accessing Firebase Realtime Database: " + e.getMessage());
                // Fall back to just Firestore and local database
                saveToFirestore(null, reservation, loadingDialog);

                // Also try to save directly to local database as last resort
                saveToLocalDatabaseDirectly(reservation, loadingDialog);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error saving reservation", e);
            Toast.makeText(this, "Error creating reservation: " + e.getMessage(), Toast.LENGTH_LONG).show();

            // Attempt emergency save to local storage
            try {
                emergencySaveToPreferences();
            } catch (Exception ex) {
                Log.e(TAG, "Even emergency save failed", ex);
            }
        }
    }

    private Map<String, Object> createReservationData(String userId) {
        // Create reservation data map
        Map<String, Object> reservation = new HashMap<>();
        reservation.put("user_id", userId);
        reservation.put("reservation_date", dateEditText.getText().toString());
        reservation.put("reservation_time", timeEditText.getText().toString());

        // Safely parse guest count with error handling
        try {
            String guestCountStr = guestsSpinner.getSelectedItem().toString();
            int guestCount = Integer.parseInt(guestCountStr);
            reservation.put("number_of_guests", guestCount);
        } catch (Exception e) {
            // Default to 2 guests if parsing fails
            Log.e(TAG, "Error parsing guest count, using default", e);
            reservation.put("number_of_guests", 2);
        }

        reservation.put("status", "Pending");
        reservation.put("customerName", customerNameEditText.getText().toString());
        reservation.put("phone", phoneEditText.getText().toString());
        reservation.put("email", emailEditText.getText().toString());
        reservation.put("specialRequests", specialRequestsEditText.getText().toString());
        reservation.put("timestamp", System.currentTimeMillis());
        reservation.put("visibility", "all");

        return reservation;
    }

    private void saveToFirestore(String realtimeDbKey, Map<String, Object> reservation, AlertDialog loadingDialog) {
        // If we have a key from Realtime Database, use it as the document ID
        if (realtimeDbKey != null) {
            reservation.put("realtimeDbKey", realtimeDbKey);
        }

        // Save to Firestore using the connectivity manager for better reliability
        firebaseConnectivityManager.getFirestore().collection("reservations")
                .add(reservation)
                .addOnSuccessListener(documentReference -> {
                    // Also save to local database and find available table
                    saveToLocalDatabaseAndFindTable(documentReference.getId(), reservation, loadingDialog);
                })
                .addOnFailureListener(e -> {
                    if (loadingDialog.isShowing()) {
                        loadingDialog.dismiss();
                    }
                    Toast.makeText(CreateReservationActivity.this,
                            "Failed to create reservation: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void saveToLocalDatabaseAndFindTable(String reservationId, Map<String, Object> reservationData, AlertDialog loadingDialog) {
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

                    // Insert reservation and get ID
                    long reservationDbId = db.reservationDao().insert(localReservation);

                    // Find available table based on party size
                    Table assignedTable = findAvailableTable(localReservation.number_of_guests);

                    if (assignedTable != null) {
                        // Update table status to reserved
                        assignedTable.setStatus("reserved");
                        assignedTable.setReservationId(reservationDbId);
                        db.tableDao().update(assignedTable);

                        // Update Firestore with table info
                        updateFirestoreWithTableInfo(reservationId, assignedTable.getTableNumber());

                        final int tableNumber = assignedTable.getTableNumber();

                        // Show success on UI thread
                        runOnUiThread(() -> {
                            if (loadingDialog != null && loadingDialog.isShowing()) {
                                loadingDialog.dismiss();
                            }

                            Toast.makeText(CreateReservationActivity.this,
                                    "Reservation saved successfully! Table #" + tableNumber + " has been reserved for you.",
                                    Toast.LENGTH_LONG).show();

                            // Save table number to shared preferences for reference
                            if (getSharedPreferences("reservation_prefs", MODE_PRIVATE) != null) {
                                getSharedPreferences("reservation_prefs", MODE_PRIVATE)
                                        .edit()
                                        .putInt("last_table_number", tableNumber)
                                        .apply();
                            }
                        });
                    } else {
                        // No table available, but still save reservation
                        runOnUiThread(() -> {
                            if (loadingDialog != null && loadingDialog.isShowing()) {
                                loadingDialog.dismiss();
                            }

                            // Show dialog about no available tables
                            AlertDialog.Builder builder = new AlertDialog.Builder(CreateReservationActivity.this);
                            builder.setTitle("Reservation on Hold")
                                    .setMessage("Your reservation request has been submitted, but we need to confirm table availability. Our staff will contact you shortly to confirm.")
                                    .setPositiveButton("OK", (dialog, which) -> finish())
                                    .setCancelable(false)
                                    .show();
                        });
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error saving to local database", e);
                    runOnUiThread(() -> {
                        if (loadingDialog != null && loadingDialog.isShowing()) {
                            loadingDialog.dismiss();
                        }
                        Toast.makeText(CreateReservationActivity.this,
                                "Error creating reservation: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });
                }
            }).start();
        } catch (Exception e) {
            if (loadingDialog != null && loadingDialog.isShowing()) {
                loadingDialog.dismiss();
            }
            Log.e(TAG, "Error accessing local database", e);
            Toast.makeText(this, "Error creating reservation: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private Table findAvailableTable(int partySize) {
        try {
            AppDatabase db = AppDatabase.getDatabase(this);
            // Find tables with capacity >= party size and status 'available'
            List<Table> availableTables = db.tableDao().getAvailableTablesByCapacity(partySize);

            if (availableTables != null && !availableTables.isEmpty()) {
                // Find the best fit table (smallest capacity that fits the party)
                Table bestFitTable = availableTables.get(0);

                for (Table table : availableTables) {
                    // Find table with capacity closest to party size
                    if (table.getCapacity() < bestFitTable.getCapacity() &&
                            table.getCapacity() >= partySize) {
                        bestFitTable = table;
                    }
                }

                return bestFitTable;
            }

            return null; // No available table found
        } catch (Exception e) {
            Log.e(TAG, "Error finding available table", e);
            return null;
        }
    }

    private void updateFirestoreWithTableInfo(String reservationId, int tableNumber) {
        try {
            // Update the reservation document with table info
            Map<String, Object> updates = new HashMap<>();
            updates.put("tableNumber", tableNumber);
            updates.put("tableStatus", "reserved");

            db.collection("reservations").document(reservationId)
                    .update(updates)
                    .addOnSuccessListener(aVoid ->
                            Log.d(TAG, "Firestore updated with table information"))
                    .addOnFailureListener(e ->
                            Log.e(TAG, "Error updating Firestore with table info", e));

            // Also send notification to staff about new reservation
            sendStaffNotification(reservationId, tableNumber);
        } catch (Exception e) {
            Log.e(TAG, "Error updating Firestore", e);
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
            dateEditText.setOnClickListener(v -> showDatePicker(dateEditText));
            layout.addView(dateEditText);

            // Add time field
            TextView timeLabel = new TextView(this);
            timeLabel.setText("Time:");
            timeLabel.setPadding(0, 20, 0, 0);
            layout.addView(timeLabel);

            timeEditText = new EditText(this);
            timeEditText.setHint("Select Time");
            timeEditText.setFocusable(false);
            timeEditText.setOnClickListener(v -> showTimePicker(timeEditText));
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

    @Override
    protected void onResume() {
        super.onResume();

        // Fix for the Submit Reservation button visibility
        if (saveButton != null) {
            // Make button more prominent to prevent visibility issues
            saveButton.setVisibility(View.VISIBLE);
            saveButton.setEnabled(true);
            saveButton.bringToFront();

            // Add visual enhancements
            saveButton.setTextSize(20); // Larger text
            saveButton.setAllCaps(true); // ALL CAPS TEXT
            saveButton.setPadding(0, 30, 0, 30); // Taller button

            // Increase the elevation for more prominence
            saveButton.setElevation(24f);

            // Make it stand out visually
            saveButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    getResources().getColor(R.color.luxe_burgundy, getTheme())));

            // Set bottom margin to lift the button above navigation
            if (saveButton.getParent() instanceof ViewGroup) {
                ViewGroup parent = (ViewGroup) saveButton.getParent();

                // Apply larger bottom margin
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                params.setMargins(16, 24, 16, 160); // Add side margins too
                saveButton.setLayoutParams(params);
            }

            // Make sure the scroll view scrolls down to show the button
            View scrollParent = findScrollParent(saveButton);
            if (scrollParent instanceof ScrollView) {
                ((ScrollView) scrollParent).post(() -> {
                    ((ScrollView) scrollParent).fullScroll(ScrollView.FOCUS_DOWN);

                    // Add a small delay and scroll again to ensure button visibility
                    new android.os.Handler().postDelayed(() -> {
                        ((ScrollView) scrollParent).fullScroll(ScrollView.FOCUS_DOWN);

                        // Flash animation to draw attention to the button
                        android.animation.ObjectAnimator.ofFloat(saveButton, "alpha", 0.4f, 1.0f)
                                .setDuration(500)
                                .start();
                    }, 300);
                });
            }

            // Add click effect for better feedback
            saveButton.setOnTouchListener((v, event) -> {
                switch (event.getAction()) {
                    case android.view.MotionEvent.ACTION_DOWN:
                        v.setScaleX(0.95f);
                        v.setScaleY(0.95f);
                        break;
                    case android.view.MotionEvent.ACTION_UP:
                    case android.view.MotionEvent.ACTION_CANCEL:
                        v.setScaleX(1.0f);
                        v.setScaleY(1.0f);
                        break;
                }
                // Don't consume the event
                return false;
            });
        }
    }

    /**
     * Helper method to find the parent ScrollView of a view
     */
    private View findScrollParent(View view) {
        if (view == null) return null;

        ViewParent parent = view.getParent();
        while (parent != null) {
            if (parent instanceof ScrollView) {
                return (View) parent;
            }
            parent = parent.getParent();
        }
        return null;
    }

    /**
     * Send notification to staff about the new reservation
     */
    private void sendStaffNotification(String reservationId, int tableNumber) {
        try {
            // Create notification for managers, waiters and admins
            Map<String, Object> notification = new HashMap<>();
            notification.put("type", "new_reservation");
            notification.put("title", "New Reservation");
            notification.put("message", "New reservation for Table #" + tableNumber);
            notification.put("reservationId", reservationId);
            notification.put("timestamp", System.currentTimeMillis());

            // Create notifications for different staff roles using the connectivity manager
            DatabaseReference notificationsRef = firebaseConnectivityManager.getRealtimeDatabase().getReference("notifications");

            // For managers
            Map<String, Object> managerNotification = new HashMap<>(notification);
            managerNotification.put("role", "manager");
            notificationsRef.child("manager").push().setValue(managerNotification);

            // For waiters
            Map<String, Object> waiterNotification = new HashMap<>(notification);
            waiterNotification.put("role", "waiter");
            notificationsRef.child("waiter").push().setValue(waiterNotification);

            // For admins
            Map<String, Object> adminNotification = new HashMap<>(notification);
            adminNotification.put("role", "admin");
            notificationsRef.child("admin").push().setValue(adminNotification);

            Log.d(TAG, "Notification sent to staff about new reservation for Table #" + tableNumber);
        } catch (Exception e) {
            Log.e(TAG, "Error sending staff notification: " + e.getMessage(), e);
        }
    }

    /**
     * Create a better progress dialog with animation
     */
    private AlertDialog createProgressDialog(String title, String message) {
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            View progressView = getLayoutInflater().inflate(R.layout.dialog_progress, null);

            // If we can't inflate the progress layout, use a simpler dialog
            if (progressView == null) {
                return new AlertDialog.Builder(this)
                        .setTitle(title)
                        .setMessage(message)
                        .setCancelable(false)
                        .create();
            }

            // Set up text views in the progress layout if found
            TextView titleTextView = progressView.findViewById(R.id.dialogTitle);
            TextView messageTextView = progressView.findViewById(R.id.dialogMessage);

            if (titleTextView != null) {
                titleTextView.setText(title);
            }

            if (messageTextView != null) {
                messageTextView.setText(message);
            }

            builder.setView(progressView);
            builder.setCancelable(false);

            return builder.create();
        } catch (Exception e) {
            Log.e(TAG, "Error creating progress dialog", e);
            // Fall back to standard dialog
            return new AlertDialog.Builder(this)
                    .setTitle(title)
                    .setMessage(message)
                    .setCancelable(false)
                    .create();
        }
    }

    /**
     * Last-resort method to directly save to local database
     */
    private void saveToLocalDatabaseDirectly(Map<String, Object> reservationData, AlertDialog loadingDialog) {
        try {
            AppDatabase db = AppDatabase.getDatabase(this);

            new Thread(() -> {
                try {
                    Reservation localReservation = new Reservation();

                    // Convert data from map to reservation object with safe conversions
                    if (reservationData.get("reservation_date") instanceof String) {
                        localReservation.reservation_date = (String) reservationData.get("reservation_date");
                    }

                    if (reservationData.get("reservation_time") instanceof String) {
                        localReservation.reservation_time = (String) reservationData.get("reservation_time");
                    }

                    // Get number_of_guests safely
                    try {
                        Object guestsObj = reservationData.get("number_of_guests");
                        if (guestsObj instanceof Integer) {
                            localReservation.number_of_guests = (Integer) guestsObj;
                        } else if (guestsObj instanceof String) {
                            localReservation.number_of_guests = Integer.parseInt((String) guestsObj);
                        } else {
                            localReservation.number_of_guests = 2; // Default
                        }
                    } catch (Exception e) {
                        localReservation.number_of_guests = 2; // Default
                    }

                    if (reservationData.get("status") instanceof String) {
                        localReservation.status = (String) reservationData.get("status");
                    } else {
                        localReservation.status = "Pending"; // Default
                    }

                    if (reservationData.get("customerName") instanceof String) {
                        localReservation.customerName = (String) reservationData.get("customerName");
                    }

                    if (reservationData.get("phone") instanceof String) {
                        localReservation.phone = (String) reservationData.get("phone");
                    }

                    if (reservationData.get("email") instanceof String) {
                        localReservation.email = (String) reservationData.get("email");
                    }

                    if (reservationData.get("specialRequests") instanceof String) {
                        localReservation.specialRequests = (String) reservationData.get("specialRequests");
                    }

                    // Insert reservation
                    long reservationId = db.reservationDao().insert(localReservation);
                    Log.d(TAG, "Reservation saved locally with ID: " + reservationId);

                    // Show success
                    runOnUiThread(() -> {
                        if (loadingDialog != null && loadingDialog.isShowing()) {
                            loadingDialog.dismiss();
                        }

                        // Show success message
                        AlertDialog.Builder builder = new AlertDialog.Builder(CreateReservationActivity.this);
                        builder.setTitle("Reservation Created!");
                        builder.setMessage("Your reservation has been saved successfully!");
                        builder.setPositiveButton("OK", (dialog, which) -> {
                            // Return to previous screen
                            finish();
                        });
                        builder.setCancelable(false);
                        builder.show();
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Error saving to local database", e);
                    runOnUiThread(() -> {
                        if (loadingDialog != null && loadingDialog.isShowing()) {
                            loadingDialog.dismiss();
                        }
                        Toast.makeText(CreateReservationActivity.this,
                                "Error saving reservation locally", Toast.LENGTH_SHORT).show();
                    });
                }
            }).start();
        } catch (Exception e) {
            Log.e(TAG, "Error initializing local database", e);
            if (loadingDialog != null && loadingDialog.isShowing()) {
                loadingDialog.dismiss();
            }
        }
    }

    /**
     * Emergency save of reservation data to SharedPreferences
     */
    private void emergencySaveToPreferences() {
        try {
            // Get input values
            String date = dateEditText.getText().toString();
            String time = timeEditText.getText().toString();
            String name = customerNameEditText.getText().toString();
            String phone = phoneEditText.getText().toString();

            // Save to SharedPreferences
            getSharedPreferences("emergency_reservation", MODE_PRIVATE)
                    .edit()
                    .putString("date", date)
                    .putString("time", time)
                    .putString("name", name)
                    .putString("phone", phone)
                    .putLong("timestamp", System.currentTimeMillis())
                    .apply();

            Toast.makeText(this, "Reservation data saved temporarily", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Error in emergency save", e);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Dismiss any dialogs to prevent window leaks
        if (loadingDialog != null && loadingDialog.isShowing()) {
            try {
                loadingDialog.dismiss();
            } catch (Exception e) {
                Log.e(TAG, "Error dismissing dialog", e);
            }
            loadingDialog = null;
        }
    }
}