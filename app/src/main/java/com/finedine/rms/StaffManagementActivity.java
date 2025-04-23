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

import java.util.Collections;
import java.util.List;



public class StaffManagementActivity extends AppCompatActivity implements StaffAdapter.OnStaffClickListener {

    private static final int EDIT_STAFF_REQUEST = 0;
    private RecyclerView rvStaff;
    private StaffAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_staff_management);

        rvStaff = findViewById(R.id.rvStaff);
        rvStaff.setLayoutManager(new LinearLayoutManager(this));
        List<User> staff = Collections.emptyList();
        adapter = new StaffAdapter(staff, this);
        rvStaff.setAdapter(adapter);
        loadStaffData();
    }
    @Override
    public void onStaffClick(User staff) {
        // Show staff details
        showStaffDetails(staff);
    }

    private void showStaffDetails(User staff) {
    }

    private void loadStaffData() {
        AppDatabase db = AppDatabase.getDatabase(this);
        new Thread(() -> {
            List<User> staff = db.userDao().getAllStaff();
            runOnUiThread(() -> {
                // Update the adapter initialization
                adapter = new StaffAdapter(staff, new StaffAdapter.OnStaffClickListener() {
                    @Override
                    public void onStaffClick(User staff) {
                        // Handle staff member click
                    }

                    @Override
                    public void onEditClick(User staff) {
                        // Handle edit action
                    }

                    @Override
                    public void onDeleteClick(User staff) {
                        // Handle delete action
                    }
                });
                rvStaff.setAdapter(adapter);
            });
        }).start();
    }
    @Override
    public void onEditClick(User staff) {
        // Launch edit staff activity
        Intent intent = new Intent(this, EditStaffActivity.class);
        intent.putExtra("staff_id", staff.user_id);
        startActivityForResult(intent, EDIT_STAFF_REQUEST);
    }

    private void startActivityForResult(Intent intent, boolean editStaffRequest) {
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
        new Thread(() -> {
            AppDatabase.getDatabase(this).userDao().delete(staff);
            runOnUiThread(() -> {
                adapter.updateStaffList(
                        AppDatabase.getDatabase(this).userDao().getAllStaff()
                );
            });
        }).start();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable  Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == EDIT_STAFF_REQUEST && resultCode == RESULT_OK) {
            loadStaffData(); // Refresh the list
        }
    }
}
