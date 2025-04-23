package com.finedine.rms;


import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;


import com.google.android.material.textfield.TextInputEditText;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;


public class EditStaffActivity extends AppCompatActivity {

    private TextInputEditText etName, etEmail, etPassword;
    private Spinner spinnerRole;
    private User currentUser;
    private final List<String> roles = Arrays.asList("manager", "chef", "waiter");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_staff);

        initializeViews();
        loadStaffData();
        setupRoleSpinner();
    }

    private void initializeViews() {
        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        spinnerRole = findViewById(R.id.spinnerRole);
        findViewById(R.id.btnSave).setOnClickListener(this::saveChanges);
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
        etName.setText(currentUser.name);
        etEmail.setText(currentUser.email);
        spinnerRole.setSelection(roles.indexOf(currentUser.role));
    }

    private void setupRoleSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                roles
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRole.setAdapter(adapter);
    }

    private void saveChanges(View view) {
        String name = Objects.requireNonNull(etName.getText()).toString().trim();
        String email = Objects.requireNonNull(etEmail.getText()).toString().trim();
        String password = Objects.requireNonNull(etPassword.getText()).toString().trim();
        String role = roles.get(spinnerRole.getSelectedItemPosition());

        if (name.isEmpty() || email.isEmpty()) {
            Toast.makeText(this, "Name and email are required", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Invalid email format", Toast.LENGTH_SHORT).show();
            return;
        }

        currentUser.name = name;
        currentUser.email = email;
        currentUser.role = role;

        if (!password.isEmpty()) {
            // In production, use proper password hashing
            currentUser.password_hash = String.valueOf(password.hashCode());
        }

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