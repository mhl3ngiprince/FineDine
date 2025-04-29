package com.finedine.rms;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.cardview.widget.CardView;

public class ManagerDashboardActivity extends BaseActivity {
    private static final String TAG = "ManagerDashboard";

    // Stats fields
    private TextView tvRevenue, tvOrderCount, tvReservationCount, tvStaffCount;
    private TextView tvLowStockItems, tvCustomerRating;

    // Management cards
    private CardView cardStaffManagement, cardMenuManagement, cardInventoryManagement;
    private CardView cardReservations, cardOrders, cardKitchenStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manager_dashboard);

        // Setup navigation panel
        setupNavigationPanel("Manager Dashboard");

        try {
            // Initialize UI components
            initializeUI();

            // Set welcome message
            String role = getIntent().getStringExtra("user_role");
            if (role == null) role = "manager";

            TextView welcomeTextView = findViewById(R.id.welcomeTextView);
            if (welcomeTextView != null) {
                welcomeTextView.setText("Welcome, " + role + "!");
            }

            // Setup click listeners
            setupClickListeners();

            // Load dashboard data
            loadDashboardData();

            Toast.makeText(this, "Manager Dashboard Loaded", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Error initializing ManagerDashboardActivity", e);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void initializeUI() {
        // Initialize TextView elements
        tvRevenue = findViewById(R.id.tvRevenue);
        tvOrderCount = findViewById(R.id.tvOrderCount);
        tvReservationCount = findViewById(R.id.tvReservationCount);
        tvStaffCount = findViewById(R.id.tvStaffCount);
        tvLowStockItems = findViewById(R.id.tvLowStockItems);
        tvCustomerRating = findViewById(R.id.tvCustomerRating);

        // Initialize CardView elements
        cardStaffManagement = findViewById(R.id.cardStaffManagement);
        cardMenuManagement = findViewById(R.id.cardMenuManagement);
        cardInventoryManagement = findViewById(R.id.cardInventoryManagement);
        cardReservations = findViewById(R.id.cardReservations);
        cardOrders = findViewById(R.id.cardOrders);
        cardKitchenStatus = findViewById(R.id.cardKitchenStatus);
    }

    private void setupClickListeners() {
        // Staff Management
        if (cardStaffManagement != null) {
            cardStaffManagement.setOnClickListener(v -> {
                Log.d(TAG, "Staff Management card clicked");
                navigateToActivity(StaffManagementActivity.class, "Opening Staff Management");
            });
        }

        // Menu Management
        if (cardMenuManagement != null) {
            cardMenuManagement.setOnClickListener(v -> {
                Log.d(TAG, "Menu Management card clicked");
                navigateToActivity(MenuManagementActivity.class, "Opening Menu Management");
            });
        }

        // Inventory Management
        if (cardInventoryManagement != null) {
            cardInventoryManagement.setOnClickListener(v -> {
                Log.d(TAG, "Inventory Management card clicked");
                navigateToActivity(InventoryActivity.class, "Opening Inventory Management");
            });
        }

        // Reservations
        if (cardReservations != null) {
            cardReservations.setOnClickListener(v -> {
                Log.d(TAG, "Reservations card clicked");
                navigateToActivity(ReservationActivity.class, "Opening Reservations");
            });
        }

        // Orders
        if (cardOrders != null) {
            cardOrders.setOnClickListener(v -> {
                Log.d(TAG, "Orders card clicked");
                navigateToActivity(OrderActivity.class, "Opening Orders");
            });
        }

        // Kitchen Status
        if (cardKitchenStatus != null) {
            cardKitchenStatus.setOnClickListener(v -> {
                Log.d(TAG, "Kitchen Status card clicked");
                navigateToActivity(KitchenActivity.class, "Opening Kitchen Status");
            });
        }
    }

    private void navigateToActivity(Class<?> activityClass, String toastMessage) {
        try {
            Intent intent = new Intent(this, activityClass);

            // Pass the user role
            String role = getIntent().getStringExtra("user_role");
            if (role != null) {
                intent.putExtra("user_role", role);
            }

            startActivity(intent);
            Toast.makeText(this, toastMessage, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Navigation error: " + e.getMessage());
            Toast.makeText(this, "Error navigating: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void loadDashboardData() {
        // In a real app, this would load data from a database or API
        // For now, we'll just use the hardcoded sample data

        // You could implement actual data fetching here, for example:
        // 1. Get order count from OrderDao
        // 2. Calculate revenue from OrderItems
        // 3. Get reservation count from ReservationDao
        // 4. Get staff count from UserDao where role != customer
        // 5. Get low stock items count from InventoryDao where quantity < threshold

        Log.d(TAG, "Dashboard data loaded");
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh dashboard data when returning to this activity
        loadDashboardData();
    }
}