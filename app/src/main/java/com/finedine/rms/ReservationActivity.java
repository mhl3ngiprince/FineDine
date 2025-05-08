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

import com.finedine.rms.utils.NavigationHelper;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;

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
    private TextView tvSelectedDate;
    private TextView tvSelectedTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            // Initialize modern navigation with proper title and layout
            initializeModernNavigation("Reservations", R.layout.activity_reservation);

            // If user is staff and they're viewing all reservations, redirect to list view
            if (getIntent() != null && getIntent().getBooleanExtra("show_reservations", false)) {
                if (userRole != null && (userRole.equalsIgnoreCase("admin") ||
                        userRole.equalsIgnoreCase("manager") ||
                        userRole.equalsIgnoreCase("waiter"))) {
                    Intent listIntent = new Intent(this, ReservationListActivity.class);
                    listIntent.putExtra("user_role", userRole);
                    startActivity(listIntent);
                    finish();
                    return;
                }
            }

            etSpecialRequests = findViewById(R.id.etSpecialRequests);
            etCustomerName = findViewById(R.id.etCustomerName);
            etPhone = findViewById(R.id.etPhone);
            etEmail = findViewById(R.id.etEmail);
            tvSelectedDate = findViewById(R.id.tvSelectedDate);
            tvSelectedTime = findViewById(R.id.tvSelectedTime);

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

                        // Show toast confirmation
                        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault());

                        // Use Snackbar instead of Toast for better user feedback
                        Snackbar.make(findViewById(android.R.id.content),
                                        "Date selected: " + dateFormat.format(selectedDate.getTime()),
                                        Snackbar.LENGTH_SHORT)
                                .setActionTextColor(getResources().getColor(android.R.color.holo_green_light))
                                .show();

                        // Animate the date selection field
                        animateViewWithPulse(tvSelectedDate);
                    },
                    selectedDate.get(Calendar.YEAR),
                    selectedDate.get(Calendar.MONTH),
                    selectedDate.get(Calendar.DAY_OF_MONTH));

            // Set minimum date to today
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                datePicker.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
            }

            // Show date picker dialog
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

                        // Show better feedback with a Snackbar
                        String formattedTime = String.format(Locale.getDefault(), "%02d:%02d %s",
                                selectedHour % 12 == 0 ? 12 : selectedHour % 12,
                                selectedMinute,
                                selectedHour < 12 ? "AM" : "PM");

                        Snackbar.make(findViewById(android.R.id.content),
                                        "Time selected: " + formattedTime,
                                        Snackbar.LENGTH_SHORT)
                                .setActionTextColor(getResources().getColor(android.R.color.holo_green_light))
                                .show();

                        // Animate the time selection field
                        animateViewWithPulse(tvSelectedTime);
                    },
                    selectedHour, selectedMinute, false); // false for 12-hour format

            // Set up reasonable business hours for restaurant (e.g., 8AM to 10PM)
            timePicker.show();
        } catch (Exception e) {
            Log.e(TAG, "Error selecting time", e);
            Toast.makeText(this, "Could not open time picker", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Animate a view with a pulse effect to highlight a change
     */
    private void animateViewWithPulse(View view) {
        if (view == null) return;

        // Create scale animation for pulse effect
        android.view.animation.ScaleAnimation scaleAnimation = new android.view.animation.ScaleAnimation(
                1.0f, 1.1f, // X axis start and end scale
                1.0f, 1.1f, // Y axis start and end scale
                android.view.animation.Animation.RELATIVE_TO_SELF, 0.5f,
                android.view.animation.Animation.RELATIVE_TO_SELF, 0.5f);

        // Configure duration and behavior
        scaleAnimation.setDuration(150);
        scaleAnimation.setRepeatCount(1);
        scaleAnimation.setRepeatMode(android.view.animation.Animation.REVERSE);

        // Start animation
        view.startAnimation(scaleAnimation);
    }

    public void changePartySize(View view) {
        try {
            // Enhanced party size options
            String[] sizes = new String[]{
                    "1 person", "2 people", "3 people", "4 people",
                    "5 people", "6 people", "8 people", "10 people",
                    "12 people", "15 people", "20 people", "More than 20"
            };

            // Create a more visually appealing dialog
            AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomDialogTheme);
            builder.setTitle("Select Number of Guests");
            builder.setItems(sizes, (dialog, which) -> {
                try {
                    // Extract the number from the selection
                    String selectedSize = sizes[which];
                    int extractedSize;

                    if (selectedSize.contains("More than")) {
                        // For "More than 20", show an input dialog
                        showCustomPartySizeDialog();
                        return;
                    } else {
                        // Parse the number from the string (e.g. "4 people" -> 4)
                        extractedSize = Integer.parseInt(selectedSize.replaceAll("\\D+", ""));
                    }

                    partySize = extractedSize;
                    updateDateTimeDisplay();

                    // Animate the party size field
                    View partySizeField = findViewById(R.id.etPartySize);
                    if (partySizeField != null) {
                        animateViewWithPulse(partySizeField);
                    }

                    Snackbar.make(findViewById(android.R.id.content),
                            "Party size set to " + partySize + " people",
                            Snackbar.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Log.e(TAG, "Error setting party size", e);
                }
            });

            builder.setNegativeButton("Cancel", null);
            builder.show();
        } catch (Exception e) {
            Log.e(TAG, "Error changing party size", e);
            Toast.makeText(this, "Could not change party size", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Show a dialog for custom party size
     */
    private void showCustomPartySizeDialog() {
        // Create a dialog with number input for larger parties
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomDialogTheme);
        builder.setTitle("Enter Number of Guests");

        // Set up the input
        final EditText input = new EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        input.setHint("Enter number of guests");
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("OK", (dialog, which) -> {
            try {
                String inputText = input.getText().toString();
                if (!inputText.isEmpty()) {
                    int customSize = Integer.parseInt(inputText);
                    if (customSize > 0) {
                        partySize = customSize;
                        updateDateTimeDisplay();

                        // Animate the party size field
                        View partySizeField = findViewById(R.id.etPartySize);
                        if (partySizeField != null) {
                            animateViewWithPulse(partySizeField);
                        }

                        Snackbar.make(findViewById(android.R.id.content),
                                "Large party size set to " + partySize + " people",
                                Snackbar.LENGTH_SHORT).show();
                    }
                }
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Please enter a valid number", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    public void confirmReservation(View view) {
        try {
            // Get input values
            String name = etCustomerName.getText().toString().trim();
            String phone = etPhone.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String specialRequests = etSpecialRequests.getText().toString().trim();

            // Enhanced validation
            if (!validateReservationInputs(name, phone, email)) {
                return;
            }

            // Show progress dialog with animation
            AlertDialog progressDialog = getProgressDialogWithAnimation("Submitting your reservation...");
            progressDialog.show();

            // Save reservation in database
            new Thread(() -> {
                try {
                    // Get database instance
                    AppDatabase db = AppDatabase.getDatabase(ReservationActivity.this);

                    // Create new reservation
                    Reservation reservation = createReservationObject(name, phone, email, specialRequests);

                    // Insert reservation and get ID
                    long reservationId = db.reservationDao().insert(reservation);

                    // Find and assign a suitable table
                    Table assignedTable = findAndAssignTable(db, reservation, reservationId);

                    // Save to Firebase for manager visibility
                    saveReservationToFirebase(reservation, reservationId, assignedTable);

                    final long finalReservationId = reservationId;
                    final Table finalAssignedTable = assignedTable;

                    // Update UI on success
                    runOnUiThread(() -> {
                        try {
                            if (progressDialog.isShowing()) {
                                progressDialog.dismiss();
                            }

                            // Show success dialog with table information if available
                            if (finalAssignedTable != null) {
                                // Table was assigned
                                showSuccessDialogWithTable(finalReservationId, reservation, finalAssignedTable);
                            } else {
                                // No table was available/assigned
                                showSuccessDialog(finalReservationId, reservation);
                            }
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

                        // Show a more helpful error message
                        showErrorDialog("We couldn't complete your reservation", e.getMessage());
                    });
                }
            }).start();
        } catch (Exception e) {
            Log.e(TAG, "Error confirming reservation", e);
            Toast.makeText(this, "Error confirming reservation", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Find and assign an available table for the reservation
     *
     * @return The assigned table or null if no table was available
     */
    private Table findAndAssignTable(AppDatabase db, Reservation reservation, long reservationId) {
        try {
            // Find a table that can accommodate the party size
            List<Table> availableTables = db.tableDao().getAvailableTablesByCapacity(reservation.getNumber_of_guests());

            if (availableTables == null || availableTables.isEmpty()) {
                Log.d(TAG, "No available tables found for party size " + reservation.getNumber_of_guests());
                return null;
            }

            // Find best fit table (smallest available table that fits the party)
            Table bestFitTable = availableTables.get(0);
            for (Table table : availableTables) {
                if (table.getCapacity() < bestFitTable.getCapacity() &&
                        table.getCapacity() >= reservation.getNumber_of_guests()) {
                    bestFitTable = table;
                }
            }

            // Assign the table to the reservation
            bestFitTable.setStatus("reserved");
            bestFitTable.setReservationId(reservationId);
            db.tableDao().update(bestFitTable);

            Log.d(TAG, "Assigned table #" + bestFitTable.getTableNumber() +
                    " with capacity " + bestFitTable.getCapacity() +
                    " to reservation #" + reservationId);

            return bestFitTable;
        } catch (Exception e) {
            Log.e(TAG, "Error finding available table: " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * Show success dialog with assigned table information
     */
    private void showSuccessDialogWithTable(long reservationId, Reservation reservation, Table assignedTable) {
        try {
            // Get a formatted date and time for the message
            SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault());
            String formattedDate = dateFormat.format(selectedDate.getTime());

            String formattedTime = String.format(Locale.getDefault(), "%02d:%02d %s",
                    selectedHour % 12 == 0 ? 12 : selectedHour % 12,
                    selectedMinute,
                    selectedHour < 12 ? "AM" : "PM");

            // Build confirmation message with table information
            String confirmationMessage = String.format(
                    "Your reservation has been confirmed!\n\n" +
                            "Date: %s\n" +
                            "Time: %s\n" +
                            "Guests: %d\n" +
                            "Table: #%d (%s section)\n" +
                            "Reservation #%d\n\n" +
                            "Would you like to view our menu?",
                    formattedDate,
                    formattedTime,
                    partySize,
                    assignedTable.getTableNumber(),
                    assignedTable.getSection(),
                    reservationId
            );

            // Show success dialog
            new AlertDialog.Builder(this, R.style.CustomDialogTheme)
                    .setTitle("Reservation Confirmed with Table!")
                    .setMessage(confirmationMessage)
                    .setPositiveButton("View Menu", (dialog, which) -> {
                        // Navigate to menu activity
                        Intent intent = new Intent(this, CustomerMenuActivity.class);
                        intent.putExtra("source", "reservation");
                        intent.putExtra("reservation_id", reservationId);
                        intent.putExtra("table_number", assignedTable.getTableNumber());
                        startActivity(intent);
                    })
                    .setNegativeButton("Done", null)
                    .setIcon(android.R.drawable.ic_dialog_info)
                    .show();

            // Clear input fields
            etSpecialRequests.setText("");

            // Send notification
            sendConfirmationNotification();
        } catch (Exception e) {
            Log.e(TAG, "Error showing table success dialog", e);
            // Fall back to regular success dialog
            showSuccessDialog(reservationId, reservation);
        }
    }

    /**
     * Create a progress dialog with a custom spinner animation
     */
    private AlertDialog getProgressDialogWithAnimation(String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomDialogTheme);

        // Inflate a custom layout for the dialog
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_progress, null);
        builder.setView(dialogView);
        builder.setCancelable(false);

        return builder.create();
    }

    /**
     * Create a reservation object with the provided information
     */
    private Reservation createReservationObject(String name, String phone, String email, String specialRequests) {
        // Format date for database
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String formattedDate = dateFormat.format(selectedDate.getTime());

        // Format time for database
        String formattedTime = String.format(Locale.getDefault(), "%02d:%02d", selectedHour, selectedMinute);

        Reservation reservation = new Reservation();
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

        return reservation;
    }

    /**
     * Validate reservation inputs with improved error messages
     */
    private boolean validateReservationInputs(String name, String phone, String email) {
        boolean isValid = true;

        // Check if name is empty
        if (name.isEmpty()) {
            etCustomerName.setError("Please enter your name");
            etCustomerName.requestFocus();
            isValid = false;
        } else {
            etCustomerName.setError(null);
        }

        // Validate phone number
        if (phone.isEmpty()) {
            etPhone.setError("Please enter your phone number");
            if (isValid) etPhone.requestFocus();
            isValid = false;
        } else if (!phone.matches("\\d{10,15}")) {
            etPhone.setError("Please enter a valid phone number");
            if (isValid) etPhone.requestFocus();
            isValid = false;
        } else {
            etPhone.setError(null);
        }

        // Validate email if provided
        if (!email.isEmpty() && !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Please enter a valid email address");
            if (isValid) etEmail.requestFocus();
            isValid = false;
        } else {
            etEmail.setError(null);
        }

        // Validate date and time are selected
        if (tvSelectedDate.getText().toString().equals("Not selected")) {
            Toast.makeText(this, "Please select a date", Toast.LENGTH_SHORT).show();
            isValid = false;
        }

        if (tvSelectedTime.getText().toString().equals("Not selected")) {
            Toast.makeText(this, "Please select a time", Toast.LENGTH_SHORT).show();
            isValid = false;
        }

        return isValid;
    }

    /**
     * Show a success dialog with option to view menu
     */
    private void showSuccessDialog(long reservationId, Reservation reservation) {
        try {
            // Get a formatted date and time for the message
            SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault());
            String formattedDate = dateFormat.format(selectedDate.getTime());

            String formattedTime = String.format(Locale.getDefault(), "%02d:%02d %s",
                    selectedHour % 12 == 0 ? 12 : selectedHour % 12,
                    selectedMinute,
                    selectedHour < 12 ? "AM" : "PM");

            // Build confirmation message
            String confirmationMessage = String.format(
                    "Your reservation has been confirmed!\n\n" +
                            "Date: %s\n" +
                            "Time: %s\n" +
                            "Guests: %d\n" +
                            "Reservation #%d\n\n" +
                            "Would you like to view our menu?",
                    formattedDate,
                    formattedTime,
                    partySize,
                    reservationId
            );

            // Show success dialog
            new AlertDialog.Builder(this, R.style.CustomDialogTheme)
                    .setTitle("Reservation Confirmed!")
                    .setMessage(confirmationMessage)
                    .setPositiveButton("View Menu", (dialog, which) -> {
                        // Navigate to menu activity
                        Intent intent = new Intent(this, CustomerMenuActivity.class);
                        intent.putExtra("source", "reservation");
                        intent.putExtra("reservation_id", reservationId);
                        startActivity(intent);
                    })
                    .setNegativeButton("Done", null)
                    .setIcon(android.R.drawable.ic_dialog_info)
                    .show();

            // Clear input fields
            etSpecialRequests.setText("");

            // Send notification
            sendConfirmationNotification();
        } catch (Exception e) {
            Log.e(TAG, "Error showing success dialog", e);
            Toast.makeText(this, "Reservation confirmed!", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Show error dialog
     */
    private void showErrorDialog(String title, String message) {
        new AlertDialog.Builder(this, R.style.CustomDialogTheme)
                .setTitle(title)
                .setMessage("We encountered an error: " + message +
                        "\n\nPlease try again or contact the restaurant.")
                .setPositiveButton("Try Again", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void updateDateTimeDisplay() {
        try {
            // Update button texts to show current selections
            TextView dateButton = findViewById(R.id.btnDate);
            TextView timeButton = findViewById(R.id.btnTime);
            TextView partyText = findViewById(R.id.etPartySize);

            // Update the date selection displays
            SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault());
            SimpleDateFormat buttonFormat = new SimpleDateFormat("MMM d", Locale.getDefault());

            if (dateButton != null) {
                dateButton.setText("Date: " + buttonFormat.format(selectedDate.getTime()));
            }

            if (tvSelectedDate != null) {
                tvSelectedDate.setText(dateFormat.format(selectedDate.getTime()));
            }

            // Update the time selection displays
            String formattedTime = String.format(Locale.getDefault(), "%02d:%02d %s",
                    selectedHour % 12 == 0 ? 12 : selectedHour % 12,
                    selectedMinute,
                    selectedHour < 12 ? "AM" : "PM");

            if (timeButton != null) {
                timeButton.setText("Time: " + formattedTime);
            }

            if (tvSelectedTime != null) {
                tvSelectedTime.setText(formattedTime);
            }

            if (partyText != null) {
                partyText.setText(String.format(Locale.getDefault(), "%d people", partySize));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating date/time display", e);
        }
    }

    /**
     * Save reservation to Firebase Realtime Database for manager visibility
     */
    private void saveReservationToFirebase(Reservation reservation, long reservationId, Table assignedTable) {
        try {
            // Get Firebase Database reference
            FirebaseDatabase database = FirebaseDatabase.getInstance();
            DatabaseReference reservationsRef = database.getReference("reservations");

            // Create unique key for this reservation
            String reservationKey = reservationsRef.push().getKey();
            if (reservationKey == null) {
                Log.e(TAG, "Failed to create Firebase key for reservation");
                return;
            }

            // Create reservation data object
            Map<String, Object> reservationData = new HashMap<>();
            reservationData.put("id", reservationId);
            reservationData.put("customerName", reservation.getCustomerName());
            reservationData.put("phone", reservation.getPhone());
            reservationData.put("email", reservation.getEmail());
            reservationData.put("date", reservation.getReservation_date());
            reservationData.put("time", reservation.getReservation_time());
            reservationData.put("numberOfGuests", reservation.getNumber_of_guests());
            reservationData.put("specialRequests", reservation.getSpecialRequests());
            reservationData.put("status", reservation.getStatus());
            reservationData.put("timestamp", System.currentTimeMillis());

            // Add table info if available
            if (assignedTable != null) {
                Map<String, Object> tableData = new HashMap<>();
                tableData.put("tableNumber", assignedTable.getTableNumber());
                tableData.put("capacity", assignedTable.getCapacity());
                tableData.put("section", assignedTable.getSection());
                reservationData.put("assignedTable", tableData);
            }

            // Save to Firebase
            reservationsRef.child(reservationKey).setValue(reservationData)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Reservation saved to Firebase with key: " + reservationKey);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to save reservation to Firebase: " + e.getMessage());
                    });

            // Also update a summary for quick dashboard access
            DatabaseReference reservationSummaryRef = database.getReference("reservation_summary");
            Map<String, Object> summaryData = new HashMap<>();
            summaryData.put("total", getReservationCountForToday() + 1);  // Increment total
            summaryData.put("lastUpdated", System.currentTimeMillis());
            reservationSummaryRef.updateChildren(summaryData);

        } catch (Exception e) {
            Log.e(TAG, "Error saving reservation to Firebase", e);
        }
    }

    /**
     * Get count of reservations for today from local database
     */
    private int getReservationCountForToday() {
        try {
            AppDatabase db = AppDatabase.getDatabase(this);

            // Get today's date in the format used in the database
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            String today = dateFormat.format(new java.util.Date());

            // Query database for today's reservations count
            return db.reservationDao().getTodayReservationCount(today);
        } catch (Exception e) {
            Log.e(TAG, "Error getting reservation count", e);
            return 0;
        }
    }

    /**
     * Send confirmation notification
     */
    private void sendConfirmationNotification() {
        // Notify manager dashboard about new reservation
        try {
            FirebaseDatabase database = FirebaseDatabase.getInstance();
            DatabaseReference notificationsRef = database.getReference("manager_notifications");

            Map<String, Object> notification = new HashMap<>();
            notification.put("type", "new_reservation");
            notification.put("message", "New reservation by " + etCustomerName.getText().toString());
            notification.put("date", selectedDate.getTime().toString());
            notification.put("guests", partySize);
            notification.put("timestamp", System.currentTimeMillis());

            notificationsRef.push().setValue(notification);
        } catch (Exception e) {
            Log.e(TAG, "Error sending manager notification", e);
        }
    }
}