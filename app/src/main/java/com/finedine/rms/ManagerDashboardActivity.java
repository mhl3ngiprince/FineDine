package com.finedine.rms;

import android.app.DatePickerDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class ManagerDashboardActivity extends BaseActivity {
    private static final String TAG = "ManagerDashboard";

    // UI components
    private Toolbar toolbar;
    private TextView managerNameText;
    private TextView tvTotalSales, tvSalesChange, tvComparisonPeriod;
    private TextView tvDineInSales, tvTakeawaySales, tvDeliverySales;
    private TextView tvDineInPercentage, tvTakeawayPercentage, tvDeliveryPercentage;
    private TextView tvAvgOrderValue, tvAvgOrderChange;
    private TextView tvOrderCount, tvOrderCountChange;
    private TextView tvTableTurnover, tvTableTurnoverChange;
    private TextView tvActiveOrdersCount, tvTablesAvailable, tvStaffOnDuty, tvReservationsCount;
    private TextView periodToday, periodWeek, periodMonth, periodQuarter, periodYear;
    private View salesChartView;
    private View salesDistributionChart;
    private View btnNewOrder, btnNewReservation, btnInventory, btnMenu;
    private TextView currentTimeText;
    private Button btnDate;

    private DrawerLayout drawerLayout;

    // Selected time period
    private String selectedPeriod = "today";
    private String userRole;

    private Timer timer;

    // Firebase listeners
    private ValueEventListener inventoryListener;
    private ValueEventListener orderListener;
    private ValueEventListener reservationListener;
    private ValueEventListener notificationListener;
    private DatabaseReference inventoryRef;
    private DatabaseReference ordersRef;
    private DatabaseReference reservationsRef;
    private DatabaseReference notificationsRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            userRole = getIntent().getStringExtra("user_role");
            if (userRole == null || userRole.isEmpty()) {
                userRole = "manager"; // Default role for this activity
            }

            // Check if user is a manager - restrict access to others
            if (!"manager".equalsIgnoreCase(userRole)) {
                Log.w(TAG, "Unauthorized attempt to access Manager Dashboard by role: " + userRole);
                Toast.makeText(this, "Access denied. Only managers can access this area.", Toast.LENGTH_SHORT).show();

                // Redirect to appropriate activity based on role
                Intent redirectIntent;
                if ("admin".equalsIgnoreCase(userRole)) {
                    redirectIntent = new Intent(this, AdminActivity.class);
                } else if ("chef".equalsIgnoreCase(userRole)) {
                    redirectIntent = new Intent(this, KitchenActivity.class);
                } else {
                    // Waiter or Customer
                    redirectIntent = new Intent(this, OrderActivity.class);
                }
                redirectIntent.putExtra("user_role", userRole);
                startActivity(redirectIntent);
                finish();
                return;
            }

            // Use the original navigation panel
            setupModernNavigationPanel("Dashboard", R.layout.activity_manager_dashboard);

            // Initialize UI components
            initializeUI();

            // Setup click listeners
            setupClickListeners();

            // Load dashboard data
            loadDashboardData();

            // Set current selected period and make sure visuals match
            changePeriod("today");

            // Start timer to update time
            startTimer();

            // DIRECT EMERGENCY FIX: Try to create public test order first
            createPublicTestOrder();

        } catch (Exception e) {
            Log.e(TAG, "Error initializing ManagerDashboardActivity", e);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void initializeUI() {
        // Sales Overview components
        managerNameText = findViewById(R.id.managerNameText);
        tvTotalSales = findViewById(R.id.tvTotalSales);
        tvSalesChange = findViewById(R.id.tvSalesChange);
        tvComparisonPeriod = findViewById(R.id.tvComparisonPeriod);
        tvDineInSales = findViewById(R.id.tvDineInSales);
        tvTakeawaySales = findViewById(R.id.tvTakeawaySales);
        tvDeliverySales = findViewById(R.id.tvDeliverySales);
        tvDineInPercentage = findViewById(R.id.tvDineInPercentage);
        tvTakeawayPercentage = findViewById(R.id.tvTakeawayPercentage);
        tvDeliveryPercentage = findViewById(R.id.tvDeliveryPercentage);

        // Key Metrics
        tvAvgOrderValue = findViewById(R.id.tvAvgOrderValue);
        tvAvgOrderChange = findViewById(R.id.tvAvgOrderChange);
        tvOrderCount = findViewById(R.id.tvOrderCount);
        tvOrderCountChange = findViewById(R.id.tvOrderCountChange);
        tvTableTurnover = findViewById(R.id.tvTableTurnover);
        tvTableTurnoverChange = findViewById(R.id.tvTableTurnoverChange);

        // Period selection
        periodToday = findViewById(R.id.periodToday);
        periodWeek = findViewById(R.id.periodWeek);
        periodMonth = findViewById(R.id.periodMonth);
        periodQuarter = findViewById(R.id.periodQuarter);
        periodYear = findViewById(R.id.periodYear);

        // Charts (using placeholder views for now)
        salesChartView = findViewById(R.id.salesChartView);
        salesDistributionChart = findViewById(R.id.salesDistributionChart);

        // Restaurant Status components
        tvActiveOrdersCount = findViewById(R.id.tvActiveOrdersCount);
        tvTablesAvailable = findViewById(R.id.tvTablesAvailable);
        tvStaffOnDuty = findViewById(R.id.tvStaffOnDuty);
        tvReservationsCount = findViewById(R.id.tvReservationsCount);

        // Quick action buttons
        btnNewOrder = findViewById(R.id.btnNewOrder);
        btnNewReservation = findViewById(R.id.btnNewReservation);
        btnInventory = findViewById(R.id.btnInventory);
        btnMenu = findViewById(R.id.btnMenu);

        // Current time text
        currentTimeText = findViewById(R.id.currentTimeText);

        // Date button
        btnDate = findViewById(R.id.btnDate);

        // Set manager name and welcome message
        String managerName = getIntent().getStringExtra("user_name");
        if (managerName == null || managerName.isEmpty()) {
            // Try to get from SharedPrefsManager if available
            if (prefsManager != null) {
                managerName = prefsManager.getUserName();
            }
            // Use default if still null
            if (managerName == null || managerName.isEmpty()) {
                managerName = "Michael Johnson";
            }
        }
        managerNameText.setText(managerName);

        // Find and update the welcome text
        TextView welcomeText = findViewById(R.id.welcomeText);
        if (welcomeText != null) {
            welcomeText.setText("Welcome back, " + managerName + "!");
        }

        drawerLayout = findViewById(R.id.drawer_layout);
    }

    private void setupClickListeners() {
        // Quick action buttons
        btnNewOrder.setOnClickListener(v -> {
            Intent intent = new Intent(this, OrderActivity.class);
            intent.putExtra("new_order", true);
            navigateToActivitySafely(OrderActivity.class, "New Order");
        });
        btnNewReservation.setOnClickListener(v -> {
            Intent intent = new Intent(this, ReservationActivity.class);
            intent.putExtra("new_reservation", true);
            navigateToActivitySafely(ReservationActivity.class, "New Reservation");
        });
        btnInventory.setOnClickListener(v -> navigateToActivitySafely(InventoryActivity.class, "Inventory"));
        btnMenu.setOnClickListener(v -> navigateToActivitySafely(MenuManagementActivity.class, "Menu Management"));

        // Status containers
        findViewById(R.id.activeOrdersContainer).setOnClickListener(v -> {
            Intent intent = new Intent(this, OrderActivity.class);
            intent.putExtra("show_active", true);
            navigateToActivitySafely(OrderActivity.class, "Active Orders");
        });
        findViewById(R.id.tablesContainer).setOnClickListener(v -> {
            Intent intent = new Intent(this, ReservationActivity.class);
            intent.putExtra("show_tables", true);
            navigateToActivitySafely(ReservationActivity.class, "Table Management");
        });
        findViewById(R.id.staffContainer).setOnClickListener(v -> navigateToActivitySafely(StaffManagementActivity.class, "Staff Management"));
        findViewById(R.id.reservationsContainer).setOnClickListener(v -> {
            Intent intent = new Intent(this, ReservationListActivity.class);
            intent.putExtra("user_role", userRole);
            navigateToActivitySafely(ReservationListActivity.class, "Reservations");
        });

        // View more buttons
        findViewById(R.id.btnViewMoreSales).setOnClickListener(v -> {
            changePeriod("year");
            Toast.makeText(this, "Viewing sales data for the year", Toast.LENGTH_SHORT).show();
        });
        findViewById(R.id.btnViewAllItems).setOnClickListener(v -> navigateToActivitySafely(MenuManagementActivity.class, "Menu Items"));

        // Period selection
        periodToday.setOnClickListener(v -> changePeriod("today"));
        periodWeek.setOnClickListener(v -> changePeriod("week"));
        periodMonth.setOnClickListener(v -> changePeriod("month"));
        periodQuarter.setOnClickListener(v -> changePeriod("quarter"));
        periodYear.setOnClickListener(v -> changePeriod("year"));

        // Date button shows date selection
        btnDate.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            new DatePickerDialog(this, 
                (view, year, month, dayOfMonth) -> {
                    Calendar selectedDate = Calendar.getInstance();
                    selectedDate.set(year, month, dayOfMonth);

                    // Format date for button
                    SimpleDateFormat displayFormat = new SimpleDateFormat("EEE, MMM d", Locale.getDefault());
                    String dateStr = displayFormat.format(selectedDate.getTime());
                    btnDate.setText(dateStr + " ▼");

                    // Format for API/database query
                    SimpleDateFormat apiFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    String apiDateStr = apiFormat.format(selectedDate.getTime());

                    // Load data for selected date
                    loadDateSpecificData(apiDateStr);

                    // Update selected period to custom date
                    selectedPeriod = "custom";
                    updatePeriodUI();
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH))
                    .show();
        });
    }

    private void loadDashboardData() {
        // In a real app, this would load data from a database or API
        // For now, we'll just display the hardcoded sample data based on selected period

        // Load different data based on selected period
        if (selectedPeriod.equals("today")) {
            loadTodayData();
        } else if (selectedPeriod.equals("week")) {
            loadWeekData();
        } else if (selectedPeriod.equals("month")) {
            loadMonthData();
        } else if (selectedPeriod.equals("quarter")) {
            loadQuarterData();
        } else if (selectedPeriod.equals("year")) {
            loadYearData();
        }

        // Restaurant status - these don't change based on period
        tvActiveOrdersCount.setText("18");
        tvTablesAvailable.setText("12/20");
        tvStaffOnDuty.setText("8");
        tvReservationsCount.setText("5");

        // Update period text with current dates
        updatePeriodTexts();

        Log.d(TAG, "Dashboard data loaded for period: " + selectedPeriod);
    }

    private void updatePeriodTexts() {
        // Get the current date
        Calendar cal = Calendar.getInstance();
        Date today = cal.getTime();

        // Format for different period displays
        SimpleDateFormat monthFormat = new SimpleDateFormat("MMM", Locale.getDefault());
        SimpleDateFormat yearFormat = new SimpleDateFormat("yyyy", Locale.getDefault());

        // Update quarter text
        int quarter = (cal.get(Calendar.MONTH) / 3) + 1;
        periodQuarter.setText("Q" + quarter + " " + yearFormat.format(today));

        // Update year text
        periodYear.setText(yearFormat.format(today));

        // Update date shown in welcome section
        TextView btnDate = findViewById(R.id.btnDate);
        if (btnDate != null) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d", Locale.getDefault());
            btnDate.setText(dateFormat.format(today) + " ▼");
        }
    }

    private void loadTodayData() {
        // Sales data for today
        tvTotalSales.setText("R 12,586.50");
        tvSalesChange.setText("+8.5%");
        tvComparisonPeriod.setText("vs yesterday");
        tvDineInSales.setText("R 8,422.75");
        tvTakeawaySales.setText("R 3,098.50");
        tvDeliverySales.setText("R 1,065.25");
        tvDineInPercentage.setText("67%");
        tvTakeawayPercentage.setText("25%");
        tvDeliveryPercentage.setText("8%");

        // Key metrics
        tvAvgOrderValue.setText("R 436.50");
        tvAvgOrderChange.setText("+5.2%");
        tvOrderCount.setText("29");
        tvOrderCountChange.setText("+3.6%");
        tvTableTurnover.setText("1.5x");
        tvTableTurnoverChange.setText("-0.2%");
    }

    private void loadWeekData() {
        // Sales data for this week
        tvTotalSales.setText("R 68,450.25");
        tvSalesChange.setText("+12.3%");
        tvComparisonPeriod.setText("vs last week");
        tvDineInSales.setText("R 42,350.75");
        tvTakeawaySales.setText("R 18,766.25");
        tvDeliverySales.setText("R 7,333.25");
        tvDineInPercentage.setText("62%");
        tvTakeawayPercentage.setText("27%");
        tvDeliveryPercentage.setText("11%");

        // Key metrics
        tvAvgOrderValue.setText("R 445.80");
        tvAvgOrderChange.setText("+3.8%");
        tvOrderCount.setText("154");
        tvOrderCountChange.setText("+8.1%");
        tvTableTurnover.setText("1.6x");
        tvTableTurnoverChange.setText("+0.3%");
    }

    private void loadMonthData() {
        // Sales data for this month
        tvTotalSales.setText("R 287,655.75");
        tvSalesChange.setText("+6.7%");
        tvComparisonPeriod.setText("vs last month");
        tvDineInSales.setText("R 172,593.45");
        tvTakeawaySales.setText("R 83,420.15");
        tvDeliverySales.setText("R 31,642.15");
        tvDineInPercentage.setText("60%");
        tvTakeawayPercentage.setText("29%");
        tvDeliveryPercentage.setText("11%");

        // Key metrics
        tvAvgOrderValue.setText("R 450.25");
        tvAvgOrderChange.setText("+2.5%");
        tvOrderCount.setText("642");
        tvOrderCountChange.setText("+4.2%");
        tvTableTurnover.setText("1.7x");
        tvTableTurnoverChange.setText("+0.1%");
    }

    private void loadQuarterData() {
        // Sales data for this quarter
        tvTotalSales.setText("R 876,420.50");
        tvSalesChange.setText("+9.2%");
        tvComparisonPeriod.setText("vs last quarter");
        tvDineInSales.setText("R 525,852.30");
        tvTakeawaySales.setText("R 245,397.70");
        tvDeliverySales.setText("R 105,170.50");
        tvDineInPercentage.setText("60%");
        tvTakeawayPercentage.setText("28%");
        tvDeliveryPercentage.setText("12%");

        // Key metrics
        tvAvgOrderValue.setText("R 462.75");
        tvAvgOrderChange.setText("+4.8%");
        tvOrderCount.setText("1895");
        tvOrderCountChange.setText("+4.5%");
        tvTableTurnover.setText("1.7x");
        tvTableTurnoverChange.setText("+0.2%");
    }

    private void loadYearData() {
        // Sales data for this year
        tvTotalSales.setText("R 3,426,780.25");
        tvSalesChange.setText("+15.7%");
        tvComparisonPeriod.setText("vs last year");
        tvDineInSales.setText("R 2,021,800.30");
        tvTakeawaySales.setText("R 993,766.30");
        tvDeliverySales.setText("R 411,213.65");
        tvDineInPercentage.setText("59%");
        tvTakeawayPercentage.setText("29%");
        tvDeliveryPercentage.setText("12%");

        // Key metrics
        tvAvgOrderValue.setText("R 473.50");
        tvAvgOrderChange.setText("+8.2%");
        tvOrderCount.setText("7237");
        tvOrderCountChange.setText("+7.1%");
        tvTableTurnover.setText("1.8x");
        tvTableTurnoverChange.setText("+0.4%");
    }

    private void changePeriod(String period) {
        // Update selected period
        selectedPeriod = period;

        // Reset all period buttons to normal state
        periodToday.setBackgroundResource(R.drawable.bg_period_normal);
        periodWeek.setBackgroundResource(R.drawable.bg_period_normal);
        periodMonth.setBackgroundResource(R.drawable.bg_period_normal);
        periodQuarter.setBackgroundResource(R.drawable.bg_period_normal);
        periodYear.setBackgroundResource(R.drawable.bg_period_normal);

        periodToday.setTextColor(getResources().getColor(R.color.restaurant_text_secondary, null));
        periodWeek.setTextColor(getResources().getColor(R.color.restaurant_text_secondary, null));
        periodMonth.setTextColor(getResources().getColor(R.color.restaurant_text_secondary, null));
        periodQuarter.setTextColor(getResources().getColor(R.color.restaurant_text_secondary, null));
        periodYear.setTextColor(getResources().getColor(R.color.restaurant_text_secondary, null));

        // Highlight selected period
        switch (period) {
            case "today":
                periodToday.setBackgroundResource(R.drawable.bg_period_selected);
                periodToday.setTextColor(Color.WHITE);
                break;
            case "week":
                periodWeek.setBackgroundResource(R.drawable.bg_period_selected);
                periodWeek.setTextColor(Color.WHITE);
                break;
            case "month":
                periodMonth.setBackgroundResource(R.drawable.bg_period_selected);
                periodMonth.setTextColor(Color.WHITE);
                break;
            case "quarter":
                periodQuarter.setBackgroundResource(R.drawable.bg_period_selected);
                periodQuarter.setTextColor(Color.WHITE);
                break;
            case "year":
                periodYear.setBackgroundResource(R.drawable.bg_period_selected);
                periodYear.setTextColor(Color.WHITE);
                break;
        }

        // Reload dashboard data for the selected period
        loadDashboardData();
    }

    private void loadDateSpecificData(String dateStr) {
        // In a real app, this would fetch data from a database or API for the specific date
        // For now, we'll just show custom sales figures

        // Show a loading indicator
        Toast.makeText(this, "Loading data for " + dateStr, Toast.LENGTH_SHORT).show();

        // Update sales data with random values for demo purposes
        tvTotalSales.setText("R " + (9000 + new Random().nextInt(4000)) + ".50");
        tvSalesChange.setText("+" + (5 + new Random().nextInt(10)) + ".2%");
        tvComparisonPeriod.setText("vs previous day");

        // Update all other metrics with random data
        tvDineInSales.setText("R " + (6000 + new Random().nextInt(3000)) + ".75");
        tvTakeawaySales.setText("R " + (2000 + new Random().nextInt(2000)) + ".50");
        tvDeliverySales.setText("R " + (1000 + new Random().nextInt(1000)) + ".25");

        // Update percentages
        int dineInPercent = 55 + new Random().nextInt(20);
        int takeawayPercent = (100 - dineInPercent) / 2;
        int deliveryPercent = 100 - dineInPercent - takeawayPercent;

        tvDineInPercentage.setText(dineInPercent + "%");
        tvTakeawayPercentage.setText(takeawayPercent + "%");
        tvDeliveryPercentage.setText(deliveryPercent + "%");
    }

    private void updatePeriodUI() {
        // Reset all period buttons to normal state
        periodToday.setBackgroundResource(R.drawable.bg_period_normal);
        periodWeek.setBackgroundResource(R.drawable.bg_period_normal);
        periodMonth.setBackgroundResource(R.drawable.bg_period_normal);
        periodQuarter.setBackgroundResource(R.drawable.bg_period_normal);
        periodYear.setBackgroundResource(R.drawable.bg_period_normal);

        periodToday.setTextColor(getResources().getColor(R.color.restaurant_text_secondary, null));
        periodWeek.setTextColor(getResources().getColor(R.color.restaurant_text_secondary, null));
        periodMonth.setTextColor(getResources().getColor(R.color.restaurant_text_secondary, null));
        periodQuarter.setTextColor(getResources().getColor(R.color.restaurant_text_secondary, null));
        periodYear.setTextColor(getResources().getColor(R.color.restaurant_text_secondary, null));

        // If we have a standard period selected, highlight it
        if (selectedPeriod.equals("today")) {
            periodToday.setBackgroundResource(R.drawable.bg_period_selected);
            periodToday.setTextColor(Color.WHITE);
        } else if (selectedPeriod.equals("week")) {
            periodWeek.setBackgroundResource(R.drawable.bg_period_selected);
            periodWeek.setTextColor(Color.WHITE);
        } else if (selectedPeriod.equals("month")) {
            periodMonth.setBackgroundResource(R.drawable.bg_period_selected);
            periodMonth.setTextColor(Color.WHITE);
        } else if (selectedPeriod.equals("quarter")) {
            periodQuarter.setBackgroundResource(R.drawable.bg_period_selected);
            periodQuarter.setTextColor(Color.WHITE);
        } else if (selectedPeriod.equals("year")) {
            periodYear.setBackgroundResource(R.drawable.bg_period_selected);
            periodYear.setTextColor(Color.WHITE);
        }
        // For custom date, none of the period buttons should be highlighted
    }

    private void navigateToActivitySafely(Class<?> activityClass, String actionName) {
        try {
            Log.d(TAG, "Navigating to: " + activityClass.getSimpleName() + " - " + actionName);
            Intent intent = new Intent(this, activityClass);

            // Pass the user role
            intent.putExtra("user_role", userRole);

            // Pass username if available
            String userName = getIntent().getStringExtra("user_name");
            if (userName != null && !userName.isEmpty()) {
                intent.putExtra("user_name", userName);
            }

            // Optional: Add animation transitions
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);

            // Show brief confirmation toast
            Toast.makeText(this, "Opening " + actionName, Toast.LENGTH_SHORT).show();
        } catch (ActivityNotFoundException anfe) {
            Log.e(TAG, "Activity not found: " + activityClass.getSimpleName(), anfe);
            Toast.makeText(this, "Error: " + actionName + " screen is not available", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Navigation error to " + activityClass.getSimpleName() + ": " + e.getMessage(), e);
            Toast.makeText(this, "Error: Unable to open " + actionName, Toast.LENGTH_SHORT).show();
        }
    }

    private void startTimer() {
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(() -> {
                    Calendar calendar = Calendar.getInstance();
                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
                    String currentTime = simpleDateFormat.format(calendar.getTime());
                    currentTimeText.setText(currentTime);
                });
            }
        }, 0, 1000);
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            // Show a confirmation dialog before exiting
            new android.app.AlertDialog.Builder(this)
                    .setTitle("Exit Dashboard")
                    .setMessage("Are you sure you want to exit the dashboard?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        super.onBackPressed();
                    })
                    .setNegativeButton("No", null)
                    .show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            // Refresh dashboard data when returning to this activity
            loadDashboardData();

            // Update current time
            updateCurrentTime();
        } catch (Exception e) {
            Log.e(TAG, "Error in onResume: " + e.getMessage());
        }
    }

    private void updateCurrentTime() {
        try {
            if (currentTimeText != null) {
                Calendar calendar = Calendar.getInstance();
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
                String currentTime = simpleDateFormat.format(calendar.getTime());
                currentTimeText.setText(currentTime);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating time: " + e.getMessage());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (timer != null) {
            timer.cancel();
        }

        // Clean up Firebase listeners
        removeFirebaseListeners();
    }

    /**
     * Set up real-time listeners for database changes
     */
    private void setupFirebaseListeners() {
        try {
            FirebaseDatabase database = FirebaseDatabase.getInstance();

            // Ensure user is authenticated before setting up listeners
            com.finedine.rms.firebase.FirebaseAuthHelper authHelper = com.finedine.rms.firebase.FirebaseAuthHelper.getInstance();
            if (!authHelper.isAuthenticated()) {
                // Authenticate anonymously
                authHelper.authenticateAnonymously(new com.finedine.rms.firebase.FirebaseAuthHelper.AuthCallback() {
                    @Override
                    public void onAuthComplete(boolean success, String message) {
                        if (success) {
                            Log.d(TAG, "Successfully authenticated: " + message);
                            // Now that we're authenticated, set up the listeners
                            setupDatabaseListeners();
                        } else {
                            Log.e(TAG, "Authentication failed: " + message);
                            Toast.makeText(ManagerDashboardActivity.this,
                                    "Error: Unable to access database. Check your connection.",
                                    Toast.LENGTH_LONG).show();

                            // Fall back to offline data when authentication fails
                            loadOfflineData();
                        }
                    }
                });
            } else {
                // Already authenticated, set up listeners directly
                setupDatabaseListeners();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting up Firebase listeners: " + e.getMessage());
            // Fall back to offline data when there's an exception
            loadOfflineData();
        }
    }

    /**
     * Verify that we can connect to the Firebase database and get data
     */
    private void verifyDatabaseConnectivity() {
        FirebaseDatabase.getInstance().getReference(".info/connected")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        boolean connected = snapshot.getValue(Boolean.class) != null &&
                                snapshot.getValue(Boolean.class);

                        if (connected) {
                            Log.d(TAG, "Connected to Firebase database");
                            // Try direct force authentication method first - more likely to work
                            forceAuthenticationAndTest();
                        } else {
                            Log.w(TAG, "Not connected to Firebase database");
                            Toast.makeText(ManagerDashboardActivity.this,
                                    "Offline mode: Using local data", Toast.LENGTH_LONG).show();
                            loadOfflineData();
                        }
                    }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "Database connectivity check cancelled: " + error.getMessage());
                    Toast.makeText(ManagerDashboardActivity.this,
                            "Database connection error: " + error.getMessage(),
                            Toast.LENGTH_LONG).show();
                    loadOfflineData();
                }
            });
    }

    /**
     * Force authentication with default credentials and test database access
     * This is more likely to work than anonymous auth
     */
    private void forceAuthenticationAndTest() {
        try {
            // Get FirebaseAuth instance
            com.google.firebase.auth.FirebaseAuth auth = com.google.firebase.auth.FirebaseAuth.getInstance();

            // If already signed in, test database access immediately
            if (auth.getCurrentUser() != null) {
                Log.d(TAG, "Already authenticated as: " + auth.getCurrentUser().getEmail());
                testDatabaseAccess();
                return;
            }

            // Use default credentials for a quick test - ONLY FOR DEVELOPMENT
            String testEmail = "test@finedine.com";
            String testPassword = "test123";

            // Show a progress indicator
            Toast.makeText(this, "Connecting to database...", Toast.LENGTH_SHORT).show();

            // Try to sign in
            auth.signInWithEmailAndPassword(testEmail, testPassword)
                    .addOnSuccessListener(authResult -> {
                        Log.d(TAG, "Test auth successful");
                        testDatabaseAccess();
                    })
                    .addOnFailureListener(e -> {
                        Log.w(TAG, "Test auth failed, trying to create account", e);

                        // If sign in fails, try to create the account
                        auth.createUserWithEmailAndPassword(testEmail, testPassword)
                                .addOnSuccessListener(authResult -> {
                                    Log.d(TAG, "Test account created successfully");
                                    testDatabaseAccess();
                                })
                                .addOnFailureListener(createError -> {
                                    Log.e(TAG, "Failed to create test account", createError);

                                    // Fall back to anonymous auth as last resort
                                    setupFirebaseListeners();
                                });
                    });
        } catch (Exception e) {
            Log.e(TAG, "Error in forceAuthenticationAndTest", e);
            // Fall back to regular authentication
            setupFirebaseListeners();
        }
    }

    /**
     * Test database access after authentication
     */
    private void testDatabaseAccess() {
        // Print UID for rule debugging
        String uid = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid();
        Log.d(TAG, "Testing database access with UID: " + uid);

        // Try to read from orders path
        FirebaseDatabase.getInstance().getReference("orders").limitToFirst(1)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Log.d(TAG, "Orders access SUCCESS - path exists with " +
                                (snapshot.exists() ? snapshot.getChildrenCount() + " items" : "no items"));

                        // Continue with normal setup
                        setupFirebaseListeners();
                    }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "Orders access FAILED with error code: " + error.getCode() +
                            " - " + error.getMessage());

                    // Try test write to diagnose permission issues
                    testDatabaseWrite();
                }
            });
    }

    /**
     * Try a test write to diagnose permission issues
     */
    private void testDatabaseWrite() {
        // Try writing to a test path
        FirebaseDatabase.getInstance().getReference("test_access").child("timestamp")
                .setValue(System.currentTimeMillis())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Test write SUCCESS - security rules allow writes to test_access");

                    // Create a dialog to show instructions
                    showFirebaseRulesDialog();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Test write FAILED - security rules are too restrictive", e);

                    // Show dialog with security rules
                    showFirebaseRulesDialog();

                    // Fallback to offline mode
                    loadOfflineData();
                });
    }

    /**
     * Show dialog with Firebase rules fix instructions
     */
    private void showFirebaseRulesDialog() {
        runOnUiThread(() -> {
            com.finedine.rms.utils.FirebaseRulesHelper.showFirebaseRulesDialog(this);
        });
    }

    // ... [Additional new methods will be inserted here]

    /**
     * EMERGENCY FIX for orders permission denied issue - creates accessible test order 
     */
    private void createPublicTestOrder() {
        try {
            // Show message that we're attempting a fix
            Toast.makeText(this, "Fixing database access...", Toast.LENGTH_SHORT).show();

            // Use test path that should be publicly accessible for emergency access
            FirebaseDatabase.getInstance().getReference("public_test")
                    .child("connection_test").setValue("connected_" + System.currentTimeMillis())
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Public test write SUCCESS - continuing with normal verification");
                        // Success - now we know we have basic DB write access - continue with regular connectivity check
                        verifyDatabaseConnectivity();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "CRITICAL: Even public test write FAILED: " + e.getMessage());

                        // Fall back to simpler workaround - create direct public access order path
                        createEmergencyPublicOrder();
                    });
        } catch (Exception e) {
            Log.e(TAG, "Error in emergency fix", e);
            verifyDatabaseConnectivity(); // Fall back to normal process
        }
    }

    /**
     * Create emergency public order that works around permission issues
     */
    private void createEmergencyPublicOrder() {
        try {
            // Create a public test order in a special path that should work around permission issues
            String emergencyOrderPath = "emergency_orders/test_order_" + System.currentTimeMillis();
            Map<String, Object> testOrder = new HashMap<>();
            testOrder.put("id", "emergency_test_order");
            testOrder.put("table_number", "Table 1");
            testOrder.put("status", "pending");
            testOrder.put("total_amount", 100.0);
            testOrder.put("timestamp", System.currentTimeMillis());
            testOrder.put("emergency_created", true);

            // Write the test order
            FirebaseDatabase.getInstance().getReference(emergencyOrderPath)
                    .setValue(testOrder)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Emergency order created successfully at " + emergencyOrderPath);

                        // Since we can write to DB, try to diagnose the specific orders path permission issue
                        diagnoseOrdersPermission();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to create emergency order: " + e.getMessage());

                        // Extreme fallback - show rules dialog directly
                        showFirebaseRulesDialog();

                        // Continue to normal verification, which will fall back to offline mode
                        verifyDatabaseConnectivity();
                    });
        } catch (Exception e) {
            Log.e(TAG, "Error creating emergency order", e);
            verifyDatabaseConnectivity(); // Fall back to normal process
        }
    }

    /**
     * Diagnose specific permission issues with orders path
     */
    private void diagnoseOrdersPermission() {
        // 1. Verify current authentication status
        com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            Log.d(TAG, "User is authenticated: " + user.getUid());

            // 2. Try to read the orders path structure
            FirebaseDatabase.getInstance().getReference(".info/connected")
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            boolean connected = snapshot.getValue(Boolean.class) != null &&
                                    snapshot.getValue(Boolean.class);

                            if (connected) {
                                Log.d(TAG, "Database is connected - testing orders path specifically");

                                // Create emergency sample order in the correct path
                                createCriticalSampleOrder();
                            } else {
                                Log.e(TAG, "Database is not connected!");
                                verifyDatabaseConnectivity();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Log.e(TAG, "Info connected check cancelled: " + error.getMessage());
                            verifyDatabaseConnectivity();
                        }
                    });
        } else {
            Log.e(TAG, "User is NOT authenticated - attempting direct auth before accessing orders");
            forceAuthenticationAndTest();
        }
    }

    /**
     * Create a critical sample order using a workaround
     */
    private void createCriticalSampleOrder() {
        try {
            // Last resort - create in the main orders path with more direct approach
            String orderId = "emergency_order_" + System.currentTimeMillis();
            DatabaseReference orderRef = FirebaseDatabase.getInstance().getReference("orders").child(orderId);

            Map<String, Object> order = new HashMap<>();
            order.put("id", orderId);
            order.put("table_number", "Table 99");
            order.put("status", "emergency_test");
            order.put("total_amount", 0.0);
            order.put("items_count", 0);
            order.put("customer_name", "Test Customer");
            order.put("timestamp", System.currentTimeMillis());
            order.put("server_name", "Emergency Test");
            order.put("payment_status", "test");

            orderRef.setValue(order)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Critical sample order created successfully!");

                        // Since we can create orders now, continue with normal flow
                        verifyDatabaseConnectivity();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "CRITICAL: Failed to create order in main path: " + e.getMessage());

                        // Show Firebase rules dialog
                        showFirebaseRulesDialog();

                        // Continue normally
                        verifyDatabaseConnectivity();
                    });

        } catch (Exception e) {
            Log.e(TAG, "Error creating critical sample order", e);
            verifyDatabaseConnectivity();
        }
    }

    /**
     * Create sample data in the database if needed
     */
    private void createSampleDataIfNeeded() {
        FirebaseDatabase database = FirebaseDatabase.getInstance();

        // Check if orders exist
        database.getReference("orders").limitToFirst(1)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists() || !snapshot.hasChildren()) {
                            Log.d(TAG, "No orders found, creating sample data");
                            createSampleOrders();
                        } else {
                            Log.d(TAG, "Orders exist in database, no need to create samples");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Error checking for orders: " + error.getMessage());
                    }
                });

        // Check if inventory items exist
        database.getReference("inventory").limitToFirst(1)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists() || !snapshot.hasChildren()) {
                            Log.d(TAG, "No inventory found, creating sample data");
                            createSampleInventory();
                        } else {
                            Log.d(TAG, "Inventory exists in database, no need to create samples");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Error checking for inventory: " + error.getMessage());
                    }
                });
    }

    /**
     * Create sample orders in the database
     */
    private void createSampleOrders() {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference ordersRef = database.getReference("orders");

        // Create a few sample orders
        for (int i = 1; i <= 5; i++) {
            String orderId = "sample-order-" + System.currentTimeMillis() + "-" + i;
            Map<String, Object> order = new HashMap<>();
            order.put("id", orderId);
            order.put("table_number", "Table " + i);
            order.put("status", i % 3 == 0 ? "completed" : i % 2 == 0 ? "in_progress" : "pending");
            order.put("total_amount", 150.0 + (i * 42.5));
            order.put("items_count", 3 + i);
            order.put("customer_name", "Sample Customer " + i);
            order.put("timestamp", System.currentTimeMillis() - (i * 3600000)); // i hours ago
            order.put("server_name", "Sample Server");
            order.put("payment_status", i % 2 == 0 ? "paid" : "unpaid");

            ordersRef.child(orderId).setValue(order)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Sample order created successfully"))
                    .addOnFailureListener(e -> Log.e(TAG, "Error creating sample order", e));
        }

        // Also create some kitchen orders
        DatabaseReference kitchenOrdersRef = database.getReference("kitchen_orders");
        for (int i = 1; i <= 3; i++) {
            String orderId = "sample-kitchen-order-" + System.currentTimeMillis() + "-" + i;
            Map<String, Object> order = new HashMap<>();
            order.put("id", orderId);
            order.put("table_number", "Table " + (i + 2));
            order.put("status", i % 2 == 0 ? "preparing" : "ready");
            order.put("items_count", 2 + i);
            order.put("timestamp", System.currentTimeMillis() - (i * 1800000)); // i half-hours ago
            order.put("priority", i);

            kitchenOrdersRef.child(orderId).setValue(order)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Sample kitchen order created successfully"))
                    .addOnFailureListener(e -> Log.e(TAG, "Error creating sample kitchen order", e));
        }
    }

    /**
     * Create sample inventory items
     */
    private void createSampleInventory() {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference inventoryRef = database.getReference("inventory");

        String[] itemNames = {"Chicken", "Rice", "Tomatoes", "Potatoes", "Onions", "Garlic", "Olive Oil"};
        double[] quantities = {25.5, 40.0, 15.0, 30.0, 10.0, 5.0, 2.0};
        double[] thresholds = {20.0, 30.0, 10.0, 20.0, 8.0, 3.0, 1.0};
        String[] units = {"kg", "kg", "kg", "kg", "kg", "kg", "liters"};

        for (int i = 0; i < itemNames.length; i++) {
            String itemId = "sample-item-" + System.currentTimeMillis() + "-" + i;
            Map<String, Object> item = new HashMap<>();

            item.put("id", itemId);
            item.put("name", itemNames[i]);
            item.put("quantity", quantities[i]);
            item.put("threshold", thresholds[i]);
            item.put("unit", units[i]);
            item.put("last_updated", System.currentTimeMillis());

            inventoryRef.child(itemId).setValue(item)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Sample inventory item created successfully"))
                    .addOnFailureListener(e -> Log.e(TAG, "Error creating sample inventory item", e));
        }
    }

    /**
     * Load offline data when database is not available
     */
    private void loadOfflineData() {
        runOnUiThread(() -> {
            // Update UI with offline data
            tvActiveOrdersCount.setText("--");
            tvReservationsCount.setText("--");

            // Show a message to the user
            View rootView = findViewById(android.R.id.content);
            if (rootView != null) {
                com.google.android.material.snackbar.Snackbar.make(
                        rootView,
                        "Unable to load live data. Using offline mode.",
                        com.google.android.material.snackbar.Snackbar.LENGTH_LONG
                ).setAction("RETRY", v -> verifyDatabaseConnectivity()).show();
            }
        });
    }

    /**
     * Set up actual database listeners once authentication is confirmed
     */
    private void setupDatabaseListeners() {
        try {
            FirebaseDatabase database = FirebaseDatabase.getInstance();

            // Check if we need to create sample data first
            createSampleDataIfNeeded();

            // Listen for inventory changes
            inventoryRef = database.getReference("inventory");
            inventoryListener = inventoryRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    Log.d(TAG, "Inventory data received: " + snapshot.getChildrenCount() + " items");
                    // Check for low inventory items
                    checkForLowInventory(snapshot);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "Error listening for inventory updates: " + error.getMessage() +
                            ", code: " + error.getCode());
                }
            });

            // Listen for order changes
            ordersRef = database.getReference("orders");
            orderListener = ordersRef.orderByChild("timestamp")
                    .startAt(System.currentTimeMillis() - (24 * 60 * 60 * 1000)) // Last 24 hours
                    .addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            Log.d(TAG, "Orders data received: " + snapshot.getChildrenCount() + " orders");
                            // Update active orders count
                            updateActiveOrders(snapshot);
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Log.e(TAG, "Error listening for order updates: " + error.getMessage() +
                                    ", code: " + error.getCode());
                        }
                    });

            // Listen for reservation changes
            reservationsRef = database.getReference("reservations");
            reservationListener = reservationsRef.orderByChild("timestamp")
                    .startAt(System.currentTimeMillis() - (24 * 60 * 60 * 1000)) // Last 24 hours
                    .addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            Log.d(TAG, "Reservations data received: " + snapshot.getChildrenCount() + " reservations");
                            // Update reservations count
                            updateReservationsCount(snapshot);
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Log.e(TAG, "Error listening for reservation updates: " + error.getMessage() +
                                    ", code: " + error.getCode());
                        }
                    });

            // Listen for manager notifications
            notificationsRef = database.getReference("manager_notifications");
            notificationListener = notificationsRef.orderByChild("timestamp")
                    .startAt(System.currentTimeMillis() - (60 * 60 * 1000)) // Last hour
                    .addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            Log.d(TAG, "Notifications data received: " + snapshot.getChildrenCount() + " notifications");
                            // Show notifications for new events
                            processManagerNotifications(snapshot);
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Log.e(TAG, "Error listening for notification updates: " + error.getMessage() +
                                    ", code: " + error.getCode());
                        }
                    });

            Log.d(TAG, "Firebase listeners set up successfully");

        } catch (Exception e) {
            Log.e(TAG, "Error setting up database listeners: " + e.getMessage());
            loadOfflineData();
        }
    }

    // ... [Additional new methods will be inserted here]

    /**
     * Clean up Firebase listeners when activity is destroyed
     */
    private void removeFirebaseListeners() {
        try {
            if (inventoryRef != null && inventoryListener != null) {
                inventoryRef.removeEventListener(inventoryListener);
            }

            if (ordersRef != null && orderListener != null) {
                ordersRef.removeEventListener(orderListener);
            }

            if (reservationsRef != null && reservationListener != null) {
                reservationsRef.removeEventListener(reservationListener);
            }

            if (notificationsRef != null && notificationListener != null) {
                notificationsRef.removeEventListener(notificationListener);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error removing Firebase listeners: " + e.getMessage());
        }
    }

    /**
     * Check for low inventory items that need attention
     */
    private void checkForLowInventory(DataSnapshot snapshot) {
        try {
            int lowStockCount = 0;
            StringBuilder lowStockItems = new StringBuilder();

            for (DataSnapshot itemSnapshot : snapshot.getChildren()) {
                try {
                    // Get inventory data
                    double quantity = 0;
                    double threshold = 1000; // Default high value
                    String name = "Unknown item";

                    if (itemSnapshot.child("quantity").exists()) {
                        quantity = itemSnapshot.child("quantity").getValue(Double.class);
                    } else if (itemSnapshot.child("quantity_in_stock").exists()) {
                        quantity = itemSnapshot.child("quantity_in_stock").getValue(Double.class);
                    }

                    if (itemSnapshot.child("threshold").exists()) {
                        threshold = itemSnapshot.child("threshold").getValue(Double.class);
                    } else if (itemSnapshot.child("reorder_threshold").exists()) {
                        threshold = itemSnapshot.child("reorder_threshold").getValue(Double.class);
                    }

                    if (itemSnapshot.child("name").exists()) {
                        name = itemSnapshot.child("name").getValue(String.class);
                    } else if (itemSnapshot.child("item_name").exists()) {
                        name = itemSnapshot.child("item_name").getValue(String.class);
                    }

                    // Check if this item is low on stock
                    if (quantity < threshold) {
                        lowStockCount++;

                        if (lowStockItems.length() < 100) { // Keep message reasonably short
                            if (lowStockItems.length() > 0) {
                                lowStockItems.append(", ");
                            }
                            lowStockItems.append(name);
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing inventory item: " + e.getMessage());
                }
            }

            // Update UI if we found low stock items
            if (lowStockCount > 0) {
                // Create final copies of the variables used in lambda
                final int finalLowStockCount = lowStockCount;
                final String finalLowStockItems = lowStockItems.toString();

                runOnUiThread(() -> {
                    String message;
                    if (!finalLowStockItems.isEmpty()) {
                        message = "Low stock alert: " + finalLowStockItems;
                        if (finalLowStockCount > 3) {
                            message += " and " + (finalLowStockCount - 3) + " more";
                        }
                    } else {
                        message = finalLowStockCount + " items are below reorder threshold";
                    }

                    // Show notification
                    showManagerNotification("Inventory Alert", message, "inventory");
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking for low inventory: " + e.getMessage());
        }
    }

    /**
     * Update the active orders count
     */
    private void updateActiveOrders(final DataSnapshot snapshot) {
        try {
            // Count active orders
            int activeCount = 0;
            for (DataSnapshot orderSnapshot : snapshot.getChildren()) {
                try {
                    String status = orderSnapshot.child("status").getValue(String.class);
                    if (status != null && (
                            status.equals("pending") ||
                                    status.equals("in_progress") ||
                                    status.equals("preparing"))) {
                        activeCount++;
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing order: " + e.getMessage());
                }
            }

            // Get current count for comparison
            final String currentCountText = tvActiveOrdersCount.getText().toString();
            int currentCount = 0;
            try {
                currentCount = Integer.parseInt(currentCountText);
            } catch (NumberFormatException e) {
                Log.e(TAG, "Error parsing current count: " + e.getMessage());
            }

            // Final variables for runOnUiThread
            final int finalActiveCount = activeCount;
            final boolean hasNewOrders = activeCount > currentCount;

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // Update the count
                    if (tvActiveOrdersCount != null) {
                        tvActiveOrdersCount.setText(String.valueOf(finalActiveCount));
                    }

                    // Show notification if there are new orders
                    if (hasNewOrders) {
                        Toast.makeText(ManagerDashboardActivity.this,
                                "New orders have arrived!", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error updating active orders: " + e.getMessage());
        }
    }

    /**
     * Update the reservations count
     */
    private void updateReservationsCount(final DataSnapshot snapshot) {
        try {
            // Count today's reservations
            int count = 0;

            // Get today's date in yyyy-MM-dd format
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            String today = dateFormat.format(new Date());

            for (DataSnapshot reservationSnapshot : snapshot.getChildren()) {
                try {
                    String date = reservationSnapshot.child("date").getValue(String.class);
                    if (date != null && date.equals(today)) {
                        count++;
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing reservation: " + e.getMessage());
                }
            }

            // Get current count for comparison
            final String currentCountText = tvReservationsCount.getText().toString();
            int currentCount = 0;
            try {
                currentCount = Integer.parseInt(currentCountText);
            } catch (NumberFormatException e) {
                Log.e(TAG, "Error parsing current reservation count: " + e.getMessage());
            }

            // Final variables for runOnUiThread
            final int finalCount = count;
            final boolean hasNewReservations = count > currentCount;

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // Update the count
                    if (tvReservationsCount != null) {
                        tvReservationsCount.setText(String.valueOf(finalCount));
                    }

                    // Show notification if there are new reservations
                    if (hasNewReservations) {
                        Toast.makeText(ManagerDashboardActivity.this,
                                "New reservation received!", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error updating reservations count: " + e.getMessage());
        }
    }

    /**
     * Process manager notifications
     */
    private void processManagerNotifications(DataSnapshot snapshot) {
        try {
            // Create a list to store notifications we need to show
            final List<Map<String, String>> notifications = new ArrayList<>();

            for (DataSnapshot notificationSnapshot : snapshot.getChildren()) {
                try {
                    // Only process new notifications we haven't seen
                    if (notificationSnapshot.child("processed").exists() &&
                            Boolean.TRUE.equals(notificationSnapshot.child("processed").getValue(Boolean.class))) {
                        continue;
                    }

                    // Get notification data
                    String type = notificationSnapshot.child("type").getValue(String.class);
                    String message = notificationSnapshot.child("message").getValue(String.class);

                    if (type != null && message != null) {
                        // Determine notification title based on type
                        String title;
                        switch (type) {
                            case "new_reservation":
                                title = "New Reservation";
                                break;
                            case "inventory_alert":
                                title = "Inventory Alert";
                                break;
                            case "new_order":
                                title = "New Order";
                                break;
                            default:
                                title = "Manager Alert";
                                break;
                        }

                        // Store notification details for later display
                        Map<String, String> notificationData = new HashMap<>();
                        notificationData.put("title", title);
                        notificationData.put("message", message);
                        notificationData.put("type", type);
                        notifications.add(notificationData);

                        // Mark notification as processed
                        notificationSnapshot.getRef().child("processed").setValue(true);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error processing notification: " + e.getMessage());
                }
            }

            // Show all notifications on the UI thread
            if (!notifications.isEmpty()) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        for (Map<String, String> notification : notifications) {
                            showManagerNotification(
                                    notification.get("title"),
                                    notification.get("message"),
                                    notification.get("type")
                            );
                        }
                    }
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "Error processing manager notifications: " + e.getMessage());
        }
    }

    /**
     * Show a notification to the manager
     */
    private void showManagerNotification(String title, String message, String type) {
        try {
            // Show notification using Snackbar
            View rootView = findViewById(android.R.id.content);
            Snackbar snackbar = Snackbar.make(rootView, message, Snackbar.LENGTH_LONG);

            // Customize based on notification type
            int backgroundColor;
            int textColor = Color.WHITE;
            View.OnClickListener clickListener;
            String actionText;

            // Use if-else instead of switch to avoid lambda expression issues
            if ("inventory".equals(type)) {
                backgroundColor = getResources().getColor(android.R.color.holo_red_dark);
                actionText = "VIEW INVENTORY";
                clickListener = new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        navigateToActivitySafely(InventoryActivity.class, "Inventory");
                    }
                };
            } else if ("orders".equals(type)) {
                backgroundColor = getResources().getColor(android.R.color.holo_orange_dark);
                actionText = "VIEW ORDERS";
                clickListener = new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        navigateToActivitySafely(OrderActivity.class, "Orders");
                    }
                };
            } else if ("reservations".equals(type)) {
                backgroundColor = getResources().getColor(android.R.color.holo_blue_dark);
                actionText = "VIEW RESERVATIONS";
                clickListener = new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        navigateToActivitySafely(ReservationListActivity.class, "Reservations");
                    }
                };
            } else {
                backgroundColor = getResources().getColor(android.R.color.holo_green_dark);
                actionText = "DISMISS";
                clickListener = new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // Just dismiss the snackbar
                    }
                };
            }

            // Apply styling and show
            snackbar.setActionTextColor(Color.WHITE);
            snackbar.setAction(actionText, clickListener);
            View snackbarView = snackbar.getView();
            snackbarView.setBackgroundColor(backgroundColor);

            // Find the text view and set its color
            TextView textView = snackbarView.findViewById(com.google.android.material.R.id.snackbar_text);
            if (textView != null) {
                textView.setTextColor(textColor);
                textView.setMaxLines(3);
            }

            // Show the snackbar
            snackbar.show();

        } catch (Exception e) {
            Log.e(TAG, "Error showing manager notification: " + e.getMessage());
            // Fallback to simple toast
            Toast.makeText(this, title + ": " + message, Toast.LENGTH_LONG).show();
        }
    }
}