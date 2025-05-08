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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            Log.d("EditStaffActivity", "Starting EditStaffActivity");
            // Use modern navigation panel
            setupModernNavigationPanel("Edit Staff", R.layout.activity_edit_staff);

            initializeViews();

            // Check if this is for creating a new staff member
            boolean isNewStaff = getIntent().getBooleanExtra("is_new_staff", false);
            int staffId = getIntent().getIntExtra("staff_id", -1);

            this.isNewStaff = isNewStaff;

            Log.d("EditStaffActivity", "Mode: " + (isNewStaff ? "New Staff" : "Edit Staff") +
                    ", ID: " + (isNewStaff ? "N/A" : staffId));

            if (isNewStaff) {
                // Create a new user instance for adding a staff member
                currentUser = createNewUserInstance();
                currentUser.name = "New Staff Member";

                // Update UI for new staff
                tvTitle.setText("Add New Staff Member");
                populateFields();
                // Make password field visible for new staff
                if (etPassword != null) {
                    etPassword.setVisibility(View.VISIBLE);
                }
                Log.d("EditStaffActivity", "Prepared new staff member form");

            } else {
                // Load existing staff data
                loadStaffData();
                Log.d("EditStaffActivity", "Loading existing staff data");
            }

            setupRoleDropdown();

            btnCancel.setOnClickListener(v -> finish());
            Log.d("EditStaffActivity", "EditStaffActivity initialization complete");

        } catch (Exception e) {
            Log.e("EditStaffActivity", "Error initializing EditStaffActivity: " + e.getMessage(), e);
            Toast.makeText(this, "Error initializing staff edit screen: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            // Finish activity if initialization fails
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

        if(userId == -1) {
            // If no staff ID was provided, create a new user
            Log.w("EditStaffActivity", "No staff ID provided, creating new user");
            currentUser = createNewUserInstance();
            tvTitle.setText("Add New Staff Member");
            populateFields();
            // Make password field visible for new staff
            if (etPassword != null) {
                etPassword.setVisibility(View.VISIBLE);
            }
            return;
        }

        // Show loading indicator
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
                    try {
                        if (currentUser == null) {
                            // User not found in database
                            Log.e("EditStaffActivity", "User with ID " + userId + " not found in database");
                            Toast.makeText(this, "Staff member not found in database", Toast.LENGTH_SHORT).show();

                            // Create an empty user to work with
                            currentUser = createNewUserInstance();
                            currentUser.name = "New Staff Member";
                            tvTitle.setText("Add New Staff Member");
                            populateFields();
                            // Make password field visible for new staff
                            if (etPassword != null) {
                                etPassword.setVisibility(View.VISIBLE);
                            }
                            return;
                        }

                        Log.d("EditStaffActivity", "Found user: " + currentUser.name + ", role: " + currentUser.role);
                        tvTitle.setText("Edit " + currentUser.name);
                        populateFields();
                    } catch (Exception e) {
                        Log.e("EditStaffActivity", "Error processing user data: " + e.getMessage(), e);
                        Toast.makeText(this, "Error loading staff data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        finish();
                    }
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

        // Show/hide password field based on whether this is a new staff
        if (etPassword != null) {
            etPassword.setVisibility(isNewStaff ? View.VISIBLE : View.GONE);
            etPassword.setText("");  // Always clear password field for security
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

        // Validate required fields
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

        // For new staff, validate password if provided
        if (isNewStaff && etPassword != null && etPassword.getVisibility() == View.VISIBLE) {
            if (password.length() < 6 && !password.isEmpty()) {
                etPassword.setError("Password must be at least 6 characters");
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // Ensure role is valid, default to waiter if not
        if (role.isEmpty() || !roles.contains(role.toLowerCase())) {
            role = "waiter";
            actvRole.setText(role);
            Toast.makeText(this, "Using default role: Waiter", Toast.LENGTH_SHORT).show();
        }

        // Show a progress indicator
        Toast.makeText(this, "Saving staff information...", Toast.LENGTH_SHORT).show();
        btnSave.setEnabled(false);
        btnCancel.setEnabled(false);

        try {
            // If this is a new user, initialize it
            if (currentUser == null) {
                currentUser = createNewUserInstance();
                Log.d("EditStaffActivity", "Creating new staff member");
            } else {
                Log.d("EditStaffActivity", "Updating existing staff member ID: " + currentUser.user_id);
            }

            // Ensure all fields have non-null values
            currentUser.name = name;
            currentUser.email = email;
            currentUser.role = role.toLowerCase();
            currentUser.phone = phone != null ? phone : "";
            currentUser.hireDate = hireDateStr != null ? hireDateStr : "";
            currentUser.notes = additionalNotes != null ? additionalNotes : "";

            // Set a default password for new users
            if (currentUser.password_hash == null || currentUser.password_hash.isEmpty()) {
                // Use provided password or default if empty
                if (isNewStaff && !password.isEmpty()) {
                    currentUser.password_hash = password;
                    Log.d("EditStaffActivity", "Using provided password for new staff");
                } else {
                    currentUser.password_hash = "default123";
                    Log.d("EditStaffActivity", "Setting default password for new staff");
                }
            }

            // Create a final reference for the thread
            final User staffToSave = currentUser;

            Log.d("EditStaffActivity", "Saving staff data: name=" + staffToSave.name +
                    ", email=" + staffToSave.email +
                    ", role=" + staffToSave.role);

            new Thread(() -> {
                try {
                    AppDatabase db = AppDatabase.getDatabase(this);
                    if (db == null) {
                        throw new Exception("Database instance is null");
                    }

                    UserDao userDao = db.userDao();
                    if (userDao == null) {
                        throw new Exception("UserDao is null");
                    }

                    // For new users, check if email already exists
                    if (staffToSave.user_id <= 0) {
                        try {
                            User existingUser = userDao.getUserByEmail(email);
                            if (existingUser != null) {
                                runOnUiThread(() -> {
                                    etStaffEmail.setError("Email already in use");
                                    Toast.makeText(this, "Email is already in use by another user", Toast.LENGTH_LONG).show();
                                    btnSave.setEnabled(true);
                                    btnCancel.setEnabled(true);
                                });
                                return;
                            }
                        } catch (Exception e) {
                            Log.e("EditStaffActivity", "Error checking existing email: " + e.getMessage(), e);
                            // Continue with save attempt even if check fails
                        }
                    }

                    if (staffToSave.user_id > 0) {
                        try {
                            // Update existing user
                            Log.d("EditStaffActivity", "Updating user in database: " + staffToSave.user_id);
                            userDao.update(staffToSave);
                            Log.d("EditStaffActivity", "Successfully updated user");
                        } catch (Exception e) {
                            Log.e("EditStaffActivity", "Error updating user: " + e.getMessage(), e);
                            throw new Exception("Failed to update user: " + e.getMessage());
                        }
                    } else {
                        try {
                            // Insert new user
                            Log.d("EditStaffActivity", "Inserting new user in database");
                            // Ensure we have a properly initialized user object
                            validateUserObject(staffToSave);

                            try {
                                // Try to insert the user with a transaction
                                long newId = userDao.insert(staffToSave);

                                if (newId <= 0) {
                                    throw new Exception("Database returned invalid ID for new user");
                                }

                                staffToSave.user_id = (int) newId;
                                Log.d("EditStaffActivity", "Successfully inserted user with new ID: " + newId);

                                // Log detailed information for debugging
                                Log.d("EditStaffActivity", "New user details - Name: " + staffToSave.name +
                                        ", Email: " + staffToSave.email +
                                        ", Role: " + staffToSave.role +
                                        ", Phone: " + staffToSave.phone +
                                        ", Notes: " + staffToSave.notes);
                            } catch (Exception e) {
                                Log.e("EditStaffActivity", "Error inserting user: " + e.getMessage(), e);

                                if (e.getMessage() != null && e.getMessage().contains("UNIQUE constraint")) {
                                    runOnUiThread(() -> {
                                        etStaffEmail.setError("Email already in use");
                                        Toast.makeText(EditStaffActivity.this, "Email already in use by another user", Toast.LENGTH_LONG).show();
                                        btnSave.setEnabled(true);
                                        btnCancel.setEnabled(true);
                                    });
                                    return;
                                }

                                // Try direct SQL insertion as fallback
                                try {
                                    Log.d("EditStaffActivity", "Attempting direct SQL insertion as fallback");
                                    // Get raw Room database
                                    androidx.sqlite.db.SupportSQLiteDatabase rawDb = db.getSQLiteDatabase();

                                    // Build insert statement with all fields explicitly specified
                                    String sql = "INSERT INTO users (name, email, password_hash, role, phone, hireDate, notes) " +
                                            "VALUES (?, ?, ?, ?, ?, ?, ?)";

                                    // Execute SQL with values
                                    rawDb.execSQL(sql, new Object[]{
                                            staffToSave.name,
                                            staffToSave.email,
                                            staffToSave.password_hash,
                                            staffToSave.role,
                                            staffToSave.phone,
                                            staffToSave.hireDate,
                                            staffToSave.notes
                                    });

                                    // Get the last inserted ID
                                    android.database.Cursor cursor = rawDb.query("SELECT last_insert_rowid()");
                                    int newUserId = 0;
                                    if (cursor != null && cursor.moveToFirst()) {
                                        newUserId = cursor.getInt(0);
                                        cursor.close();
                                    }

                                    if (newUserId > 0) {
                                        Log.d("EditStaffActivity", "Direct SQL insertion succeeded, new ID: " + newUserId);
                                        staffToSave.user_id = newUserId;
                                    } else {
                                        throw new Exception("Failed to get new user ID after direct insertion");
                                    }
                                } catch (Exception ex) {
                                    Log.e("EditStaffActivity", "Direct SQL insertion also failed: " + ex.getMessage(), ex);
                                    throw new Exception("Failed to insert user with both methods: " + e.getMessage() + "; " + ex.getMessage());
                                }
                            }
                        } catch (Exception e) {
                            Log.e("EditStaffActivity", "Error inserting user: " + e.getMessage(), e);
                            throw new Exception("Failed to insert new user: " + e.getMessage());
                        }
                    }

                    // Final check to ensure the user exists in the database
                    try {
                        User savedUser = userDao.getUserById(staffToSave.user_id);
                        if (savedUser == null) {
                            throw new Exception("Failed to retrieve saved user from database");
                        }
                        Log.d("EditStaffActivity", "Verified user saved correctly: " + savedUser.name);
                    } catch (Exception e) {
                        Log.e("EditStaffActivity", "Error verifying saved user: " + e.getMessage(), e);
                        // Continue if verification fails, user might still be saved
                    }

                    runOnUiThread(() -> {
                        Toast.makeText(this, "Staff information saved successfully", Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        finish();
                    });
                } catch (Exception e) {
                    Log.e("EditStaffActivity", "Database error: " + e.getMessage(), e);
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Error saving staff information: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        btnSave.setEnabled(true);
                        btnCancel.setEnabled(true);
                    });
                }
            }).start();
        } catch (Exception e) {
            Log.e("EditStaffActivity", "Error preparing staff data: " + e.getMessage(), e);
            Toast.makeText(this, "Error processing staff information: " + e.getMessage(), Toast.LENGTH_LONG).show();
            btnSave.setEnabled(true);
            btnCancel.setEnabled(true);
        }
    }

    /**
     * Ensures a user object has all fields properly initialized with non-null values
     */
    private void validateUserObject(User user) {
        if (user.name == null) user.name = "";
        if (user.email == null) user.email = "";
        if (user.role == null) user.role = "waiter";
        if (user.phone == null) user.phone = "";
        if (user.hireDate == null) user.hireDate = "";
        if (user.notes == null) user.notes = "";
        if (user.password_hash == null) user.password_hash = "default123";
    }
}