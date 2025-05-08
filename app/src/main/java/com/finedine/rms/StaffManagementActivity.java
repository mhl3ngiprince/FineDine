package com.finedine.rms;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.finedine.rms.StaffAdapter;
import com.finedine.rms.AppDatabase;
import com.finedine.rms.User;
import com.finedine.rms.R;
import android.util.Log;
import android.widget.Toast;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import android.view.Menu;
import android.view.MenuItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.ListenerRegistration;
import com.finedine.rms.firebase.FirebaseDataLoader;

public class StaffManagementActivity extends BaseActivity implements StaffAdapter.OnStaffClickListener {

    private static final String TAG = "StaffManagementActivity";
    private static final int EDIT_STAFF_REQUEST = 0;
    private RecyclerView rvStaff;
    private StaffAdapter adapter;
    private FirebaseFirestore db;
    private ListenerRegistration staffListener;
    private TextView tvStaffCount;
    private FirebaseDataLoader firebaseDataLoader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            // Initialize modern navigation with proper title and layout
            initializeModernNavigation("Staff Management", R.layout.activity_staff_management);

            // Initialize RecyclerView
            rvStaff = findViewById(R.id.rvStaff);
            rvStaff.setLayoutManager(new LinearLayoutManager(this));

            // Create adapter with empty staff list initially
            List<User> staff = new ArrayList<>();
            adapter = new StaffAdapter(staff, this);
            rvStaff.setAdapter(adapter);

            // Find the staff count TextView
            tvStaffCount = findViewById(R.id.tvStaffCount);

            // Initialize Firebase Firestore
            db = FirebaseFirestore.getInstance();

            // Initialize the Firebase Data Loader
            firebaseDataLoader = new FirebaseDataLoader();

            // Load staff data
            loadStaffData();

            // Set up floating action button
            findViewById(R.id.fabAddStaff).setOnClickListener(v -> addNewStaff());

            // Set up View Staff Directory button
            Button btnViewStaffDirectory = findViewById(R.id.btnViewStaffDirectory);
            if (btnViewStaffDirectory != null) {
                btnViewStaffDirectory.setOnClickListener(v -> {
                    Intent intent = new Intent(StaffManagementActivity.this, ViewStaffActivity.class);
                    startActivity(intent);
                });
            }

            // Set up toolbar with menu
            setupToolbar();

            // Add sample data button
            addSampleDataButton();
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
    protected void onDestroy() {
        super.onDestroy();
        // Remove Firestore listener when activity is destroyed
        if (staffListener != null) {
            staffListener.remove();
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
        AppDatabase localDb = AppDatabase.getDatabase(this);
        new Thread(() -> {
            try {
                List<User> localStaff = localDb.userDao().getAllStaff();

                // Load staff from Firebase Realtime Database using our FirebaseDataLoader
                runOnUiThread(() -> {
                    if (firebaseDataLoader == null) {
                        firebaseDataLoader = new FirebaseDataLoader();
                    }

                    firebaseDataLoader.loadUsers(new FirebaseDataLoader.DataLoadCallback<User>() {
                        @Override
                        public void onDataLoaded(List<User> firebaseStaff) {
                            List<User> combinedStaff = new ArrayList<>(localStaff);

                            // Remove duplicates if a staff member exists in both local and Firebase
                            for (User firebaseUser : firebaseStaff) {
                                boolean isDuplicate = false;
                                for (User localUser : localStaff) {
                                    if (localUser.getEmail() != null &&
                                            firebaseUser.getEmail() != null &&
                                            localUser.getEmail().equals(firebaseUser.getEmail())) {
                                        isDuplicate = true;
                                        break;
                                    }
                                }
                                if (!isDuplicate) {
                                    combinedStaff.add(firebaseUser);
                                }
                            }

                            try {
                                adapter.updateStaffList(combinedStaff);
                                updateStaffCount(combinedStaff.size());
                            } catch (Exception e) {
                                Log.e(TAG, "Error updating staff list UI", e);
                            }
                        }

                        @Override
                        public void onError(String errorMessage) {
                            Log.e(TAG, "Error loading staff from Firebase: " + errorMessage);

                            // If Firebase fails, still show local staff
                            try {
                                adapter.updateStaffList(localStaff);
                                updateStaffCount(localStaff.size());
                            } catch (Exception e) {
                                Log.e(TAG, "Error updating staff list UI", e);
                            }
                        }
                    });
                });
            } catch (Exception e) {
                Log.e(TAG, "Error loading staff data from database", e);
            }
        }).start();
    }

    /**
     * Sets up a real-time listener for staff changes in Firestore
     */
    private void setupFirestoreListener() {
        // Remove any existing listener
        if (staffListener != null) {
            staffListener.remove();
        }

        // Set up a listener for changes to the staff collection
        staffListener = db.collection("users")
                .whereEqualTo("role", "staff")
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Error listening for staff updates", e);
                        return;
                    }

                    if (snapshots != null && !snapshots.isEmpty()) {
                        // Refresh staff data when changes occur
                        loadStaffData();
                    }
                });
    }

    @Override
    public void onEditClick(User staff) {
        // Launch edit staff activity
        try {
            Log.d(TAG, "Launching EditStaffActivity for staff ID: " + staff.user_id);
            Intent intent = new Intent(StaffManagementActivity.this, EditStaffActivity.class);
            intent.putExtra("staff_id", staff.user_id);

            // For better debugging
            if (isActivityAvailable(intent)) {
                startActivityForResult(intent, EDIT_STAFF_REQUEST);
                Log.d(TAG, "Started EditStaffActivity successfully");
            } else {
                Log.e(TAG, "EditStaffActivity is not available or not registered in AndroidManifest.xml");
                Toast.makeText(this, "Error: Staff edit screen is not available", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error launching edit staff activity: " + e.getMessage(), e);
            Toast.makeText(this, "Error opening edit screen: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
                    String notes = staff.getNotes();
                    if (notes != null && notes.startsWith("firebase_id:")) {
                        String firebaseId = notes.substring("firebase_id:".length());
                        db.collection("users").document(firebaseId).delete();
                    }
                    runOnUiThread(() -> {
                        try {
                            List<User> updatedList = AppDatabase.getDatabase(this).userDao().getAllStaff();
                            adapter.updateStaffList(updatedList);
                            Toast.makeText(this, "Staff member deleted", Toast.LENGTH_SHORT).show();
                            updateStaffCount(updatedList.size());
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
            Log.d(TAG, "Adding new staff member");

            // Launch activity directly without saving yet - safer approach
            Intent intent = new Intent(StaffManagementActivity.this, EditStaffActivity.class);
            intent.putExtra("is_new_staff", true);

            // For better debugging
            if (isActivityAvailable(intent)) {
                startActivityForResult(intent, EDIT_STAFF_REQUEST);
                Log.d(TAG, "Started EditStaffActivity for new staff member");
            } else {
                Log.e(TAG, "EditStaffActivity is not available or not registered in AndroidManifest.xml");
                Toast.makeText(this, "Error: Staff edit screen is not available", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error launching add staff flow: " + e.getMessage(), e);
            Toast.makeText(this, "Error adding new staff member: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Check if an activity is available through Intent resolution
     *
     * @param intent The intent to check
     * @return true if the activity is available, false otherwise
     */
    private boolean isActivityAvailable(Intent intent) {
        return intent.resolveActivity(getPackageManager()) != null;
    }

    /**
     * Updates the staff count displayed in the UI
     */
    private void updateStaffCount(int count) {
        if (tvStaffCount != null) {
            tvStaffCount.setText("Staff Members: " + count);
        }
    }

    /**
     * Set up the toolbar with menu
     */
    private void setupToolbar() {
        try {
            Toolbar toolbar = findViewById(R.id.toolbar);
            if (toolbar != null) {
                setSupportActionBar(toolbar);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting up toolbar", e);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present
        getMenuInflater().inflate(R.menu.staff_management_menu, menu);

        // Add sample data option
        menu.add(Menu.NONE, 9999, Menu.NONE, "Load Sample Staff")
                .setIcon(android.R.drawable.ic_menu_add)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here
        int id = item.getItemId();
        if (id == R.id.action_view_staff_directory) {
            // Launch ViewStaffActivity
            Intent intent = new Intent(StaffManagementActivity.this, ViewStaffActivity.class);
            startActivity(intent);
            return true;
        } else if (id == 9999) {
            // Load sample staff data
            loadSampleStaffData();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Add a sample data button programmatically
     */
    private void addSampleDataButton() {
        try {
            // Find a container to add the button to
            ViewGroup rootView = (ViewGroup) findViewById(android.R.id.content);
            ViewGroup container = rootView;

            if (container != null) {
                Button btnSampleData = new Button(this);
                btnSampleData.setText("Load Sample Staff");
                btnSampleData.setLayoutParams(new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));

                // Style button
                btnSampleData.setBackgroundColor(getResources().getColor(android.R.color.holo_blue_light));
                btnSampleData.setTextColor(getResources().getColor(android.R.color.white));
                btnSampleData.setPadding(20, 10, 20, 10);

                // Add click listener
                btnSampleData.setOnClickListener(v -> loadSampleStaffData());

                // Add to layout
                container.addView(btnSampleData);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error adding sample data button", e);
        }
    }

    /**
     * Load sample staff data using SampleDataHelper
     */
    private void loadSampleStaffData() {
        try {
            // Show progress dialog
            AlertDialog progress = new AlertDialog.Builder(this)
                    .setMessage("Loading sample staff data...")
                    .setCancelable(false)
                    .create();
            progress.show();

            // Load sample data
            com.finedine.rms.utils.SampleDataHelper.loadSampleStaff(this);

            // Dismiss progress and reload after a delay
            new Handler().postDelayed(() -> {
                if (progress.isShowing()) {
                    progress.dismiss();
                }
                loadStaffData();
                Toast.makeText(this, "Sample staff data loaded", Toast.LENGTH_SHORT).show();
            }, 2000);
        } catch (Exception e) {
            Log.e(TAG, "Error loading sample data", e);
            Toast.makeText(this, "Error loading sample data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}