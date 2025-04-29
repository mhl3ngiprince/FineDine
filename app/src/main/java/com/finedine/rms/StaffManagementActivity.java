package com.finedine.rms;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.finedine.rms.StaffAdapter;
import com.finedine.rms.AppDatabase;
import com.finedine.rms.User;
import com.google.firebase.database.annotations.Nullable;
import com.finedine.rms.R;

import android.util.Log;
import android.widget.Toast;
import java.util.Collections;
import java.util.List;

public class StaffManagementActivity extends BaseActivity implements StaffAdapter.OnStaffClickListener {

    private static final String TAG = "StaffManagementActivity";
    private static final int EDIT_STAFF_REQUEST = 0;
    private RecyclerView rvStaff;
    private StaffAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_staff_management);

        try {
            // Setup navigation panel
            setupNavigationPanel("Staff Management");

            // Initialize RecyclerView
            rvStaff = findViewById(R.id.rvStaff);
            rvStaff.setLayoutManager(new LinearLayoutManager(this));

            // Create adapter with empty staff list initially
            List<User> staff = Collections.emptyList();
            adapter = new StaffAdapter(staff, this);
            rvStaff.setAdapter(adapter);

            // Load staff data
            loadStaffData();
        } catch (Exception e) {
            Log.e(TAG, "Error initializing StaffManagementActivity", e);
            Toast.makeText(this, "Error initializing staff management screen", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onStaffClick(User staff) {
        // Show staff details
        showStaffDetails(staff);
    }

    private void showStaffDetails(User staff) {
        // Simple implementation to show user details in a toast
        try {
            if (staff != null) {
                String details = "ID: " + staff.user_id + "\n" +
                        "Name: " + staff.name + "\n" +
                        "Email: " + staff.email + "\n" +
                        "Role: " + staff.role;

                Toast.makeText(this, details, Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error showing staff details", e);
        }
    }

    private void loadStaffData() {
        AppDatabase db = AppDatabase.getDatabase(this);
        new Thread(() -> {
            try {
                List<User> staff = db.userDao().getAllStaff();
                runOnUiThread(() -> {
                    try {
                        adapter.updateStaffList(staff);
                    } catch (Exception e) {
                        Log.e(TAG, "Error updating staff list UI", e);
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error loading staff data from database", e);
            }
        }).start();
    }

    @Override
    public void onEditClick(User staff) {
        // Launch edit staff activity
        try {
            Intent intent = new Intent(this, EditStaffActivity.class);
            intent.putExtra("staff_id", staff.user_id);
            startActivityForResult(intent, EDIT_STAFF_REQUEST);
        } catch (Exception e) {
            Log.e(TAG, "Error launching edit staff activity", e);
            Toast.makeText(this, "Error opening edit screen", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDeleteClick(User staff) {
        new AlertDialog.Builder(this)
                .setTitle("Confirm Delete")
                .setMessage("Delete " + staff.name + "?")
                .setPositiveButton("Delete", (d, w) -> deleteStaff(staff))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteStaff(User staff) {
        try {
            new Thread(() -> {
                try {
                    AppDatabase.getDatabase(this).userDao().delete(staff);
                    runOnUiThread(() -> {
                        try {
                            List<User> updatedList = AppDatabase.getDatabase(this).userDao().getAllStaff();
                            adapter.updateStaffList(updatedList);
                            Toast.makeText(this, "Staff member deleted", Toast.LENGTH_SHORT).show();
                        } catch (Exception e) {
                            Log.e(TAG, "Error updating UI after staff deletion", e);
                            Toast.makeText(this, "Error refreshing staff list", Toast.LENGTH_SHORT).show();
                        }
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Error deleting staff from database", e);
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Error deleting staff member", Toast.LENGTH_SHORT).show();
                    });
                }
            }).start();
        } catch (Exception e) {
            Log.e(TAG, "Error starting delete staff thread", e);
            Toast.makeText(this, "Error processing delete request", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == EDIT_STAFF_REQUEST && resultCode == RESULT_OK) {
            loadStaffData(); // Refresh the list
        }
    }
}
