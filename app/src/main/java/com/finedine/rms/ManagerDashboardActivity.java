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
import com.finedine.rms.utils.RoleManager;
import com.finedine.rms.utils.SharedPrefsManager;

import java.util.ArrayList;
import java.util.List;

public class ManagerDashboardActivity extends AppCompatActivity {
    private static final String TAG = "ManagerDashboard";
    private TextView tvTodaySales, tvActiveOrders, tvLowStockItems;
    private RecyclerView rvRecentReservations;
    private ReservationAdapter reservationAdapter;
    private SharedPrefsManager prefsManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manager_dashboard);

        // Initialize SharedPrefsManager
        prefsManager = new SharedPrefsManager(this);

        // Verify user role
        String userRole = prefsManager.getUserRole();
        Log.d(TAG, "ManagerDashboardActivity - Current user role: " + userRole);

        // Get intent extras if coming from login/registration
        Intent intent = getIntent();
        if (intent.hasExtra("user_role")) {
            String intentRole = intent.getStringExtra("user_role");
            Log.d(TAG, "Role from intent: " + intentRole);

            // If intent provides a role and it's different from what's in prefs, update prefs
            if (intentRole != null && !intentRole.isEmpty() && !intentRole.equals(userRole)) {
                Log.w(TAG, "Role mismatch between SharedPrefs and Intent. Updating to: " + intentRole);
                prefsManager.setUserRole(intentRole);
                userRole = intentRole;
            }
        }

        // Check if role is valid for this activity
        if (!RoleManager.ROLE_MANAGER.equals(userRole)) {
            Log.e(TAG, "Unauthorized role for ManagerDashboardActivity: " + userRole);
            Toast.makeText(this, "Unauthorized access. Manager role required.", Toast.LENGTH_SHORT).show();

            // Launch the OrderActivity as a fallback
            try {
                startActivity(new Intent(this, OrderActivity.class));
            } catch (Exception e) {
                Log.e(TAG, "Failed to launch fallback activity", e);
            }

            finish();
            return;
        }

        tvTodaySales = findViewById(R.id.tvTodaySales);
        tvActiveOrders = findViewById(R.id.tvActiveOrders);
        tvLowStockItems = findViewById(R.id.tvLowStockItems);
        rvRecentReservations = findViewById(R.id.rvRecentReservations);

        setupRecyclerView();
        loadDashboardData();
    }

    private void setupRecyclerView() {
        rvRecentReservations.setLayoutManager(new LinearLayoutManager(this));
        reservationAdapter = new ReservationAdapter(new ArrayList<>());
        rvRecentReservations.setAdapter(reservationAdapter);
    }




        @SuppressLint("DefaultLocale")
        private void loadDashboardData() {
            AppDatabase db = Room.databaseBuilder(getApplicationContext(),
                            AppDatabase.class, "restaurant-db")
                    .build();

            new Thread(() -> {
                double todaySales = db.orderDao().getTodayOrderCount(); // 24h
                List<Order> activeOrders = db.orderDao().getOrdersByStatus("active");
                List<Inventory> lowStockItems = db.inventoryDao().getLowStockItems(); // <5 quantity
                List<Reservation> reservations = db.reservationDao().getTodayReservations();

                runOnUiThread(() -> {
                    tvTodaySales.setText(String.format("$%.2f", todaySales));
                    tvActiveOrders.setText(String.valueOf(activeOrders));
                    tvLowStockItems.setText(String.valueOf(lowStockItems));
                    reservationAdapter.updateData(reservations);
                });
            }).start();
        }

    public void navigateToInventory(View view) {
        startActivity(new Intent(this, InventoryActivity.class));
    }

    public void navigateToMenuManagement(View view) {
        startActivity(new Intent(this, MenuManagementActivity.class));
    }

    public void navigateToStaffManagement(View view) {
        startActivity(new Intent(this, StaffManagementActivity.class));
    }


}