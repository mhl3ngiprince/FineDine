package com.finedine.rms;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.DatePicker;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_staff);

        initializeViews();
        loadStaffData();
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
            Toast.makeText(this, "Invalid staff member", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        new Thread(() -> {
            currentUser = AppDatabase.getDatabase(this).userDao().getUserById(userId);
            runOnUiThread(() -> {
                if(currentUser == null) {
                    Toast.makeText(this, "Staff member not found", Toast.LENGTH_SHORT).show();
                    finish();
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

        // Update user data
        currentUser.name = name;
        currentUser.email = email;
        currentUser.role = role.toLowerCase();
        currentUser.phone = phone;
        currentUser.hireDate = hireDateStr;
        currentUser.notes = additionalNotes;

        new Thread(() -> {
            AppDatabase.getDatabase(this).userDao().update(currentUser);
            runOnUiThread(() -> {
                Toast.makeText(this, "Changes saved", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            });
        }).start();
    }
}