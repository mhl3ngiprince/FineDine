package com.finedine.rms;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class EditStaffActivity extends AppCompatActivity {


    private TextInputEditText etStaffName, etStaffEmail, etStaffPhone, etHireDate, etAdditionalNotes;
    private AutoCompleteTextView actvRole;
    private Button btnSave, btnCancel;
    private User currentUser;
    private final List<String> roles = Arrays.asList("manager", "chef", "waiter");
    private Calendar hireDate = Calendar.getInstance();
    private TextView tvTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_staff);

        initializeViews();

        // Check if this is for creating a new staff member
        boolean isNewStaff = getIntent().getBooleanExtra("is_new_staff", false);
        if (isNewStaff) {
            // Create a new user instance for adding a staff member
            currentUser = new User();
            currentUser.name = "New Staff Member";
            currentUser.role = "waiter";
            currentUser.email = "";
            currentUser.password_hash = "";
            currentUser.phone = "";
            currentUser.hireDate = "";
            currentUser.notes = "";

            // Update UI for new staff
            tvTitle.setText("Add New Staff Member");
            populateFields();
        } else {
            // Load existing staff data
            loadStaffData();
        }

        setupRoleDropdown();

        btnCancel.setOnClickListener(v -> finish());
    }

    private void initializeViews() {
        etStaffName = findViewById(R.id.etStaffName);
        etStaffEmail = findViewById(R.id.etStaffEmail);
        etStaffPhone = findViewById(R.id.etStaffPhone);
        actvRole = findViewById(R.id.actvRole);
        etHireDate = findViewById(R.id.etHireDate);
        etAdditionalNotes = findViewById(R.id.etAdditionalNotes);
        btnSave = findViewById(R.id.btnSave);
        btnCancel = findViewById(R.id.btnCancel);
        tvTitle = findViewById(R.id.tvTitle);

        btnSave.setOnClickListener(this::saveChanges);
        etHireDate.setOnClickListener(this::showDatePicker);
    }

    public void showDatePicker(View view) {
        int year = hireDate.get(Calendar.YEAR);
        int month = hireDate.get(Calendar.MONTH);
        int day = hireDate.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view1, selectedYear, selectedMonth, selectedDay) -> {
                    hireDate.set(Calendar.YEAR, selectedYear);
                    hireDate.set(Calendar.MONTH, selectedMonth);
                    hireDate.set(Calendar.DAY_OF_MONTH, selectedDay);

                    SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.US);
                    etHireDate.setText(dateFormat.format(hireDate.getTime()));
                },
                year, month, day);

        datePickerDialog.show();
    }

    private void loadStaffData() {
        int userId = getIntent().getIntExtra("staff_id", -1);
        if(userId == -1) {
            // If no staff ID was provided, create a new user
            currentUser = new User();
            currentUser.name = "";
            currentUser.role = "waiter";
            tvTitle.setText("Add New Staff Member");
            populateFields();
            return;
        }

        new Thread(() -> {
            currentUser = AppDatabase.getDatabase(this).userDao().getUserById(userId);
            runOnUiThread(() -> {
                if(currentUser == null) {
                    // This could be a new user being created
                    // Create an empty user to work with
                    currentUser = new User();
                    currentUser.name = "New Staff Member";
                    currentUser.role = "waiter";
                    currentUser.email = "";
                    currentUser.password_hash = "";
                    currentUser.phone = "";
                    currentUser.hireDate = "";
                    currentUser.notes = "";
                    tvTitle.setText("Add New Staff Member");
                    populateFields();
                    return;
                }
                populateFields();
            });
        }).start();
    }

    private void populateFields() {
        etStaffName.setText(currentUser.name);
        etStaffEmail.setText(currentUser.email);

        // Set phone if available
        if (currentUser.phone != null) {
            etStaffPhone.setText(currentUser.phone);
        }

        // Set role in dropdown
        int roleIndex = roles.indexOf(currentUser.role);
        if (roleIndex >= 0) {
            actvRole.setText(roles.get(roleIndex));
        }

        // Set hire date if available
        if (currentUser.hireDate != null) {
            etHireDate.setText(currentUser.hireDate);
        }

        // Set additional notes if available
        if (currentUser.notes != null) {
            etAdditionalNotes.setText(currentUser.notes);
        }
    }

    private void setupRoleDropdown() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                roles
        );
        actvRole.setAdapter(adapter);
    }

    private void saveChanges(View view) {
        String name = Objects.requireNonNull(etStaffName.getText()).toString().trim();
        String email = Objects.requireNonNull(etStaffEmail.getText()).toString().trim();
        String phone = Objects.requireNonNull(etStaffPhone.getText()).toString().trim();
        String role = actvRole.getText().toString().trim();
        String additionalNotes = Objects.requireNonNull(etAdditionalNotes.getText()).toString().trim();
        String hireDateStr = Objects.requireNonNull(etHireDate.getText()).toString().trim();

        // Validate required fields
        if (name.isEmpty() || email.isEmpty()) {
            Toast.makeText(this, "Name and email are required", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Invalid email format", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!roles.contains(role.toLowerCase())) {
            Toast.makeText(this, "Please select a valid role", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // If this is a new user, initialize it
            if (currentUser == null) {
                currentUser = new User();
            }

            // Update user data
            currentUser.name = name;
            currentUser.email = email;
            currentUser.role = role.toLowerCase();
            currentUser.phone = phone;
            currentUser.hireDate = hireDateStr;
            currentUser.notes = additionalNotes;

            // Set a default password for new users
            if (currentUser.password_hash == null || currentUser.password_hash.isEmpty()) {
                currentUser.password_hash = "default123";
            }

            new Thread(() -> {
                try {
                    AppDatabase db = AppDatabase.getDatabase(this);
                    if (currentUser.user_id > 0) {
                        // Update existing user
                        db.userDao().update(currentUser);
                    } else {
                        // Insert new user
                        long newId = db.userDao().insert(currentUser);
                        currentUser.user_id = (int) newId;
                    }

                    runOnUiThread(() -> {
                        Toast.makeText(this, "Changes saved", Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        finish();
                    });
                } catch (Exception e) {
                    Log.e("EditStaffActivity", "Database error: " + e.getMessage(), e);
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Error saving: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                }
            }).start();
        } catch (Exception e) {
            Log.e("EditStaffActivity", "Error saving staff data", e);
            Toast.makeText(this, "Error updating staff: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}