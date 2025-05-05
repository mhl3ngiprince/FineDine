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

import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.navigation.NavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.Collections;
import java.util.List;

public class StaffManagementActivity extends BaseActivity implements StaffAdapter.OnStaffClickListener {

    private static final String TAG = "StaffManagementActivity";
    private static final int EDIT_STAFF_REQUEST = 0;
    private RecyclerView rvStaff;
    private StaffAdapter adapter;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_staff_management);

        try {
            // Setup navigation panel
            setupNavigationPanel("Staff Management");

            // Setup drawer menu
            setupDrawerMenu();

            // Initialize RecyclerView
            rvStaff = findViewById(R.id.rvStaff);
            rvStaff.setLayoutManager(new LinearLayoutManager(this));

            // Create adapter with empty staff list initially
            List<User> staff = Collections.emptyList();
            adapter = new StaffAdapter(staff, this);
            rvStaff.setAdapter(adapter);

            // Load staff data
            loadStaffData();

            // Set up floating action button
            findViewById(R.id.fabAddStaff).setOnClickListener(v -> addNewStaff());
        } catch (Exception e) {
            Log.e(TAG, "Error initializing StaffManagementActivity", e);
            Toast.makeText(this, "Error initializing staff management screen", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Always refresh the staff list when returning to this screen
        loadStaffData();
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

    /**
     * Set up the drawer navigation menu
     */
    private void setupDrawerMenu() {
        try {
            drawerLayout = findViewById(R.id.drawer_layout);
            navigationView = findViewById(R.id.nav_view);

            // Set up toolbar if it exists
            Toolbar toolbar = findViewById(R.id.toolbar);
            if (toolbar != null) {
                setSupportActionBar(toolbar);
            }

            // Set up drawer toggle if drawer layout exists
            if (drawerLayout != null && navigationView != null) {
                // Set the drawer menu resource
                navigationView.getMenu().clear();
                navigationView.inflateMenu(R.menu.staff_drawer_menu);

                // Set up toggle
                ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                        this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
                drawerLayout.addDrawerListener(toggle);
                toggle.syncState();

                // Set up navigation item selection
                navigationView.setNavigationItemSelectedListener(item -> {
                    int id = item.getItemId();

                    // Handle navigation item clicks
                    if (id == R.id.nav_dashboard) {
                        navigateToActivitySafely(ManagerDashboardActivity.class);
                    } else if (id == R.id.nav_staff) {
                        // We're already on this screen
                        drawerLayout.closeDrawers();
                    } else if (id == R.id.nav_inventory) {
                        navigateToActivitySafely(InventoryActivity.class);
                    } else if (id == R.id.nav_orders) {
                        navigateToActivitySafely(OrderActivity.class);
                    } else if (id == R.id.nav_reservation) {
                        navigateToActivitySafely(ReservationActivity.class);
                    } else if (id == R.id.nav_logout) {
                        logout();
                    }

                    drawerLayout.closeDrawers();
                    return true;
                });
            } else {
                Log.e(TAG, "Drawer layout or navigation view not found");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting up drawer menu", e);
        }
    }

    /**
     * Handle adding a new staff member
     */
    private void addNewStaff() {
        try {
            // Create a new empty user, add to database, then edit it
            AppDatabase db = AppDatabase.getDatabase(this);
            if (db == null) {
                Toast.makeText(this, "Database not initialized", Toast.LENGTH_SHORT).show();
                return;
            }

            // Create a simple user object with default values that won't cause crashes
            final User newUser = new User();
            newUser.name = "New Staff Member";
            newUser.email = "";
            newUser.password_hash = "";
            newUser.role = "waiter";
            newUser.phone = "";
            newUser.hireDate = "";
            newUser.notes = "";

            // Launch activity directly without saving yet - safer approach
            Intent intent = new Intent(this, EditStaffActivity.class);
            intent.putExtra("is_new_staff", true);
            startActivityForResult(intent, EDIT_STAFF_REQUEST);

        } catch (Exception e) {
            Log.e(TAG, "Error launching add staff flow", e);
            Toast.makeText(this, "Error adding new staff member: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}