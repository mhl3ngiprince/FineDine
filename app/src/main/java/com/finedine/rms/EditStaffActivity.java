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
import com.finedine.rms.firebase.FirebaseDao;
import com.google.android.material.textfield.TextInputEditText;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class EditStaffActivity extends BaseActivity {


    private TextInputEditText etStaffName, etStaffEmail, etStaffPhone, etHireDate, etAdditionalNotes;
    private AutoCompleteTextView actvRole;
    private Button btnSave, btnCancel;
    private User currentUser;
    private final List<String> roles = Arrays.asList("manager", "chef", "waiter");
    private Calendar hireDate = Calendar.getInstance();
    private TextView tvTitle;
    private TextInputEditText etPassword;
    private boolean isNewStaff = false;
    private FirebaseDao firebaseDao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            Log.d("EditStaffActivity", "Starting EditStaffActivity");
            setupModernNavigationPanel("Edit Staff", R.layout.activity_edit_staff);

            firebaseDao = new FirebaseDao();
            initializeViews();

            boolean isNewStaff = getIntent().getBooleanExtra("is_new_staff", false);
            int staffId = getIntent().getIntExtra("staff_id", -1);

            this.isNewStaff = isNewStaff;

            Log.d("EditStaffActivity", "Mode: " + (isNewStaff ? "New Staff" : "Edit Staff") +
                    ", ID: " + (isNewStaff ? "N/A" : staffId));

            if (isNewStaff) {
                currentUser = createNewUserInstance();
                currentUser.name = "New Staff Member";
                tvTitle.setText("Add New Staff Member");
                populateFields();
                if (etPassword != null) {
                    etPassword.setVisibility(View.VISIBLE);
                }
                Log.d("EditStaffActivity", "Prepared new staff member form");

            } else {
                loadStaffData();
                Log.d("EditStaffActivity", "Loading existing staff data");
            }

            setupRoleDropdown();

            btnCancel.setOnClickListener(v -> finish());
            Log.d("EditStaffActivity", "EditStaffActivity initialization complete");

        } catch (Exception e) {
            Log.e("EditStaffActivity", "Error initializing EditStaffActivity: " + e.getMessage(), e);
            Toast.makeText(this, "Error initializing staff edit screen: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            finish();
        }
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
        etPassword = findViewById(R.id.etPassword);

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
        Log.d("EditStaffActivity", "Loading staff data for user ID: " + userId);

        if (userId == -1) {
            currentUser = createNewUserInstance();
            tvTitle.setText("Add New Staff Member");
            populateFields();
            if (etPassword != null) {
                etPassword.setVisibility(View.VISIBLE);
            }
            return;
        }

        runOnUiThread(() -> Toast.makeText(this, "Loading staff data...", Toast.LENGTH_SHORT).show());

        new Thread(() -> {
            try {
                AppDatabase db = AppDatabase.getDatabase(this);
                if (db == null) {
                    Log.e("EditStaffActivity", "Database is null");
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Error: Database not available", Toast.LENGTH_SHORT).show();
                        finish();
                    });
                    return;
                }

                Log.d("EditStaffActivity", "Querying database for user ID: " + userId);
                currentUser = db.userDao().getUserById(userId);

                runOnUiThread(() -> {
                    if (currentUser == null) {
                        Log.e("EditStaffActivity", "User with ID " + userId + " not found in database");
                        Toast.makeText(this, "Staff member not found in database", Toast.LENGTH_SHORT).show();
                        currentUser = createNewUserInstance();
                        currentUser.name = "New Staff Member";
                        tvTitle.setText("Add New Staff Member");
                        populateFields();
                        if (etPassword != null) {
                            etPassword.setVisibility(View.VISIBLE);
                        }
                        return;
                    }

                    Log.d("EditStaffActivity", "Found user: " + currentUser.name + ", role: " + currentUser.role);
                    tvTitle.setText("Edit " + currentUser.name);
                    populateFields();
                });
            } catch (Exception e) {
                Log.e("EditStaffActivity", "Database error: " + e.getMessage(), e);
                runOnUiThread(() -> {
                    Toast.makeText(this, "Database error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
        }).start();
    }

    private User createNewUserInstance() {
        User user = new User();
        user.name = "";
        user.email = "";
        user.password_hash = "";
        user.role = "waiter";
        user.phone = "";
        user.hireDate = "";
        user.notes = "";
        return user;
    }

    private void populateFields() {
        etStaffName.setText(currentUser.name);
        etStaffEmail.setText(currentUser.email);

        if (currentUser.phone != null) {
            etStaffPhone.setText(currentUser.phone);
        }

        int roleIndex = roles.indexOf(currentUser.role);
        if (roleIndex >= 0) {
            actvRole.setText(roles.get(roleIndex));
        }

        if (currentUser.hireDate != null) {
            etHireDate.setText(currentUser.hireDate);
        }

        if (currentUser.notes != null) {
            etAdditionalNotes.setText(currentUser.notes);
        }

        if (etPassword != null) {
            etPassword.setVisibility(isNewStaff ? View.VISIBLE : View.GONE);
            etPassword.setText("");
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
        String name = etStaffName.getText() != null ? etStaffName.getText().toString().trim() : "";
        String email = etStaffEmail.getText() != null ? etStaffEmail.getText().toString().trim() : "";
        String phone = etStaffPhone.getText() != null ? etStaffPhone.getText().toString().trim() : "";
        String role = actvRole.getText().toString().trim();
        String additionalNotes = etAdditionalNotes.getText() != null ? etAdditionalNotes.getText().toString().trim() : "";
        String hireDateStr = etHireDate.getText() != null ? etHireDate.getText().toString().trim() : "";
        String password = etPassword != null && etPassword.getText() != null ? etPassword.getText().toString().trim() : "";

        Log.d("EditStaffActivity", "Validating staff data before save");

        if (name.isEmpty()) {
            etStaffName.setError("Name is required");
            Toast.makeText(this, "Name is required", Toast.LENGTH_SHORT).show();
            return;
        }

        if (email.isEmpty()) {
            etStaffEmail.setError("Email is required");
            Toast.makeText(this, "Email is required", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etStaffEmail.setError("Invalid email format");
            Toast.makeText(this, "Invalid email format", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isNewStaff && etPassword != null && etPassword.getVisibility() == View.VISIBLE) {
            if (password.length() < 6 && !password.isEmpty()) {
                etPassword.setError("Password must be at least 6 characters");
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        if (role.isEmpty() || !roles.contains(role.toLowerCase())) {
            role = "waiter";
            actvRole.setText(role);
            Toast.makeText(this, "Using default role: Waiter", Toast.LENGTH_SHORT).show();
        }

        Toast.makeText(this, "Saving staff information...", Toast.LENGTH_SHORT).show();
        btnSave.setEnabled(false);
        btnCancel.setEnabled(false);

        try {
            if (currentUser == null) {
                currentUser = createNewUserInstance();
                Log.d("EditStaffActivity", "Creating new staff member");
            } else {
                Log.d("EditStaffActivity", "Updating existing staff member ID: " + currentUser.user_id);
            }

            currentUser.name = name;
            currentUser.email = email;
            currentUser.role = role.toLowerCase();
            currentUser.phone = phone != null ? phone : "";
            currentUser.hireDate = hireDateStr != null ? hireDateStr : "";
            currentUser.notes = additionalNotes != null ? additionalNotes : "";

            if (currentUser.password_hash == null || currentUser.password_hash.isEmpty()) {
                if (isNewStaff && !password.isEmpty()) {
                    currentUser.password_hash = password;
                    Log.d("EditStaffActivity", "Using provided password for new staff");
                } else {
                    currentUser.password_hash = "default123";
                    Log.d("EditStaffActivity", "Setting default password for new staff");
                }
            }

            final User staffToSave = currentUser;

            Log.d("EditStaffActivity", "Saving staff data: name=" + staffToSave.name +
                    ", email=" + staffToSave.email +
                    ", role=" + staffToSave.role);

            saveUserToFirebase(staffToSave, new FirebaseDao.FirebaseOperationCallback() {
                @Override
                public void onSuccess(String message) {
                    Log.d("EditStaffActivity", "Successfully saved user to Firebase");
                    runOnUiThread(() -> {
                        Toast.makeText(EditStaffActivity.this, "Staff information saved successfully", Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        finish();
                    });
                }

                @Override
                public void onFailure(String errorMessage) {
                    Log.e("EditStaffActivity", "Failed to save user to Firebase: " + errorMessage);
                    runOnUiThread(() -> {
                        Toast.makeText(EditStaffActivity.this, "Error saving staff information: " + errorMessage, Toast.LENGTH_LONG).show();
                        btnSave.setEnabled(true);
                        btnCancel.setEnabled(true);
                    });
                }
            });
        } catch (Exception e) {
            Log.e("EditStaffActivity", "Error preparing staff data: " + e.getMessage(), e);
            Toast.makeText(this, "Error processing staff information: " + e.getMessage(), Toast.LENGTH_LONG).show();
            btnSave.setEnabled(true);
            btnCancel.setEnabled(true);
        }
    }

    private void saveUserToFirebase(User user, FirebaseDao.FirebaseOperationCallback callback) {
        if (firebaseDao == null) {
            firebaseDao = new FirebaseDao();
        }

        firebaseDao.saveUser(user, callback);
    }
}