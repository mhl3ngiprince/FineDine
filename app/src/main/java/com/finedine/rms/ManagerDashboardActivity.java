package com.finedine.rms;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;

import com.finedine.rms.R;
import com.finedine.rms.utils.SharedPrefsManager;

import java.util.ArrayList;
import java.util.List;

public class ManagerDashboardActivity extends BaseActivity {
    private static final String TAG = "ManagerDashboard";
    private TextView tvTodaySales, tvActiveOrders, tvLowStockItems;
    private RecyclerView rvRecentReservations;
    private ReservationAdapter reservationAdapter;
    private SharedPrefsManager prefsManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manager_dashboard);

        try {
            // Setup navigation panel
            setupNavigationPanel("Manager Dashboard");

            // Initialize SharedPrefsManager
            prefsManager = new SharedPrefsManager(this);

            // Get role for logging purposes only
            String userRole = prefsManager.getUserRole();
            Log.d(TAG, "ManagerDashboardActivity - Current user role: " + userRole);

            // Save role from intent if provided (but don't restrict access)
            Intent intent = getIntent();
            if (intent.hasExtra("user_role")) {
                String intentRole = intent.getStringExtra("user_role");
                Log.d(TAG, "Role from intent: " + intentRole);

                // If intent provides a role and it's different from what's in prefs, update prefs
                if (intentRole != null && !intentRole.isEmpty() && !intentRole.equals(userRole)) {
                    Log.w(TAG, "Role mismatch between SharedPrefs and Intent. Updating to: " + intentRole);
                    prefsManager.setUserRole(intentRole);
                }
            }

            tvTodaySales = findViewById(R.id.tvTodaySales);
            tvActiveOrders = findViewById(R.id.tvActiveOrders);
            tvLowStockItems = findViewById(R.id.tvLowStockItems);
            rvRecentReservations = findViewById(R.id.rvRecentReservations);

            setupRecyclerView();
            loadDashboardData();
            setupDashboardButtons();
        } catch (Exception e) {
            Log.e(TAG, "Error in ManagerDashboardActivity onCreate", e);
            Toast.makeText(this, "Error initializing dashboard. Please try again.", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupDashboardButtons() {
        try {
            // Find all dashboard buttons by ID and set click listeners
            View btnInventory = findViewById(R.id.btnInventory);
            View btnMenu = findViewById(R.id.btnMenu);
            View btnStaff = findViewById(R.id.btnStaff);

            if (btnInventory != null) {
                btnInventory.setOnClickListener(v -> navigateToActivitySafely(InventoryActivity.class));
            } else {
                Log.e(TAG, "Inventory button not found in layout");
            }

            if (btnMenu != null) {
                btnMenu.setOnClickListener(v -> navigateToActivitySafely(MenuManagementActivity.class));
            } else {
                Log.e(TAG, "Menu button not found in layout");
            }

            if (btnStaff != null) {
                btnStaff.setOnClickListener(v -> navigateToActivitySafely(StaffManagementActivity.class));
            } else {
                Log.e(TAG, "Staff button not found in layout");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting up dashboard buttons", e);
        }
    }

    private void setupRecyclerView() {
        try {
            rvRecentReservations.setLayoutManager(new LinearLayoutManager(this));
            reservationAdapter = new ReservationAdapter(new ArrayList<>());
            rvRecentReservations.setAdapter(reservationAdapter);
        } catch (Exception e) {
            Log.e(TAG, "Error setting up RecyclerView", e);
        }
    }

    @SuppressLint("DefaultLocale")
    private void loadDashboardData() {
        try {
            AppDatabase db = AppDatabase.getDatabase(this);

            new Thread(() -> {
                try {
                    // Use try-catch for each database operation
                    double todaySales = 0;
                    List<Order> activeOrders = new ArrayList<>();
                    List<Inventory> lowStockItems = new ArrayList<>();
                    List<Reservation> reservations = new ArrayList<>();

                    try {
                        todaySales = db.orderDao().getTodayOrderCount();
                    } catch (Exception e) {
                        Log.e(TAG, "Error getting today's sales", e);
                    }

                    try {
                        activeOrders = db.orderDao().getOrdersByStatus("active");
                    } catch (Exception e) {
                        Log.e(TAG, "Error getting active orders", e);
                    }

                    try {
                        lowStockItems = db.inventoryDao().getLowStockItems();
                    } catch (Exception e) {
                        Log.e(TAG, "Error getting low stock items", e);
                    }

                    try {
                        reservations = db.reservationDao().getTodayReservations();
                    } catch (Exception e) {
                        Log.e(TAG, "Error getting reservations", e);
                    }

                    // Use final variables for UI update
                    final double finalTodaySales = todaySales;
                    final List<Order> finalActiveOrders = activeOrders;
                    final List<Inventory> finalLowStockItems = lowStockItems;
                    final List<Reservation> finalReservations = reservations;

                    runOnUiThread(() -> {
                        try {
                            tvTodaySales.setText(String.format("$%.2f", finalTodaySales));
                            tvActiveOrders.setText(String.valueOf(finalActiveOrders.size()));
                            tvLowStockItems.setText(String.valueOf(finalLowStockItems.size()));
                            reservationAdapter.updateData(finalReservations);
                        } catch (Exception e) {
                            Log.e(TAG, "Error updating UI with dashboard data", e);
                        }
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Error in dashboard data thread", e);
                }
            }).start();
        } catch (Exception e) {
            Log.e(TAG, "Error setting up dashboard data loading", e);
            Toast.makeText(this, "Error loading dashboard data", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onBackPressed() {
        // Show a confirmation dialog instead of immediately going back
        new android.app.AlertDialog.Builder(this)
                .setTitle("Exit Application")
                .setMessage("Are you sure you want to exit?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    super.onBackPressed(); // Call super when user confirms
                    finishAffinity(); // Close all activities and exit app
                })
                .setNegativeButton("No", null)
                .show();
    }

    public void navigateToInventory(View view) {
        navigateToActivitySafely(InventoryActivity.class);
    }

    public void navigateToMenuManagement(View view) {
        navigateToActivitySafely(MenuManagementActivity.class);
    }

    public void navigateToStaffManagement(View view) {
        navigateToActivitySafely(StaffManagementActivity.class);
    }
}