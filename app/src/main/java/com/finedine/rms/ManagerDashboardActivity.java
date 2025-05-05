package com.finedine.rms;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class ManagerDashboardActivity extends BaseActivity implements NavigationView.OnNavigationItemSelectedListener {
    private static final String TAG = "ManagerDashboard";

    // UI components
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private BottomNavigationView bottomNavigationView;
    private TextView managerNameText;
    private TextView tvTotalSales, tvSalesChange, tvComparisonPeriod;
    private TextView tvDineInSales, tvTakeawaySales, tvDeliverySales;
    private TextView tvDineInPercentage, tvTakeawayPercentage, tvDeliveryPercentage;
    private TextView tvActiveOrdersCount, tvTablesAvailable, tvStaffOnDuty, tvReservationsCount;
    private TextView tvAvgOrderValue, tvAvgOrderChange;
    private TextView tvOrderCount, tvOrderCountChange;
    private TextView tvTableTurnover, tvTableTurnoverChange;
    private TextView periodToday, periodWeek, periodMonth, periodQuarter, periodYear;
    private View salesChartView;
    private View salesDistributionChart;
    private View btnNewOrder, btnNewReservation, btnInventory, btnMenu;
    private TextView currentTimeText;
    private Button btnDate;

    // Selected time period
    private String selectedPeriod = "today";

    private Timer timer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manager_dashboard);

        try {
            // Initialize UI components
            initializeUI();

            // Setup toolbar and navigation
            setupNavigation();

            // Setup click listeners
            setupClickListeners();

            // Load dashboard data
            loadDashboardData();

            // Set current selected period and make sure visuals match
            changePeriod("today");

            // Start timer to update time
            startTimer();

        } catch (Exception e) {
            Log.e(TAG, "Error initializing ManagerDashboardActivity", e);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void initializeUI() {
        // Setup Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Initialize DrawerLayout
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);

        // Bottom Navigation
        bottomNavigationView = findViewById(R.id.bottomNavigation);

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

        // Set manager name
        String managerName = getIntent().getStringExtra("user_name");
        if (managerName == null) managerName = "Michael Johnson";
        managerNameText.setText(managerName);
    }

    private void setupNavigation() {
        // Setup drawer toggle
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, null, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // Setup navigation view listener
        navigationView.setNavigationItemSelectedListener(this);

        // Setup bottom navigation
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.navigation_dashboard) {
                // Already on dashboard
                return true;
            } else if (itemId == R.id.navigation_orders) {
                navigateToActivitySafely(OrderActivity.class, "Orders");
                return true;
            } else if (itemId == R.id.navigation_reservation) {
                navigateToActivitySafely(ReservationActivity.class, "Reservations");
                return true;
            } else if (itemId == R.id.navigation_menu) {
                navigateToActivitySafely(MenuManagementActivity.class, "Menu Management");
                return true;
            } else if (itemId == R.id.navigation_more) {
                drawerLayout.openDrawer(GravityCompat.START);
                return true;
            }

            return false;
        });

        // Set current selected item
        bottomNavigationView.setSelectedItemId(R.id.navigation_dashboard);

        // Menu button opens drawer
        ImageView menuButton = findViewById(R.id.menuButton);
        menuButton.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
    }

    private void setupClickListeners() {
        // Quick action buttons
        btnNewOrder.setOnClickListener(v -> navigateToActivitySafely(OrderActivity.class, "New Order"));
        btnNewReservation.setOnClickListener(v -> navigateToActivitySafely(ReservationActivity.class, "New Reservation"));
        btnInventory.setOnClickListener(v -> navigateToActivitySafely(InventoryActivity.class, "Inventory"));
        btnMenu.setOnClickListener(v -> navigateToActivitySafely(MenuManagementActivity.class, "Menu Management"));

        // Status containers
        findViewById(R.id.activeOrdersContainer).setOnClickListener(v -> navigateToActivitySafely(OrderActivity.class, "Active Orders"));
        findViewById(R.id.tablesContainer).setOnClickListener(v -> navigateToActivitySafely(ReservationActivity.class, "Table Management"));
        findViewById(R.id.staffContainer).setOnClickListener(v -> navigateToActivitySafely(StaffManagementActivity.class, "Staff Management"));
        findViewById(R.id.reservationsContainer).setOnClickListener(v -> navigateToActivitySafely(ReservationActivity.class, "Reservations"));

        // View more buttons
        findViewById(R.id.btnViewMoreSales).setOnClickListener(v -> {
            // Since there's no dedicated sales activity yet, show an informative toast
            Toast.makeText(this, "Detailed sales reports coming soon", Toast.LENGTH_SHORT).show();

            // For now, just refresh the dashboard with year period to show more data
            changePeriod("year");
        });
        findViewById(R.id.btnViewAllItems).setOnClickListener(v -> navigateToActivitySafely(MenuManagementActivity.class, "Menu Items"));

        // Period selection
        periodToday.setOnClickListener(v -> changePeriod("today"));
        periodWeek.setOnClickListener(v -> changePeriod("week"));
        periodMonth.setOnClickListener(v -> changePeriod("month"));
        periodQuarter.setOnClickListener(v -> changePeriod("quarter"));
        periodYear.setOnClickListener(v -> changePeriod("year"));

        // Menu button opens drawer
        ImageView menuButton = findViewById(R.id.menuButton);
        menuButton.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        // Profile button navigates to settings
        ImageView profileButton = findViewById(R.id.profileButton);
        profileButton.setOnClickListener(v -> navigateToActivitySafely(SettingsActivity.class, "Settings"));

        // Notification button
        ImageView notificationButton = findViewById(R.id.notificationButton);
        notificationButton.setOnClickListener(v -> Toast.makeText(this, "Notifications coming soon", Toast.LENGTH_SHORT).show());

        // Date button shows date selection
        btnDate.setOnClickListener(v -> {
            // Show a simple toast for now
            Toast.makeText(this, "Date selection coming soon", Toast.LENGTH_SHORT).show();

            // For a complete implementation, you could show a DatePickerDialog:
            /*
            Calendar cal = Calendar.getInstance();
            new DatePickerDialog(this, 
                (view, year, month, dayOfMonth) -> {
                    Calendar selectedDate = Calendar.getInstance();
                    selectedDate.set(year, month, dayOfMonth);
                    // Process the selected date
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)).show();
            */
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

    private void navigateToActivitySafely(Class<?> activityClass, String actionName) {
        try {
            Log.d(TAG, "Navigating to: " + activityClass.getSimpleName() + " - " + actionName);
            Intent intent = new Intent(this, activityClass);

            // Pass the user role
            String role = getIntent().getStringExtra("user_role");
            if (role == null || role.isEmpty()) {
                role = "manager"; // Default role for this activity
            }
            intent.putExtra("user_role", role);

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
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation drawer item clicks
        int id = item.getItemId();

        try {
            if (id == R.id.nav_dashboard) {
                // Already on dashboard
            } else if (id == R.id.nav_orders) {
                navigateToActivitySafely(OrderActivity.class, "Orders");
            } else if (id == R.id.nav_reservation) {
                navigateToActivitySafely(ReservationActivity.class, "Reservations");
            } else if (id == R.id.nav_staff) {
                navigateToActivitySafely(StaffManagementActivity.class, "Staff Management");
            } else if (id == R.id.nav_menu) {
                navigateToActivitySafely(MenuManagementActivity.class, "Menu Management");
            } else if (id == R.id.nav_inventory) {
                navigateToActivitySafely(InventoryActivity.class, "Inventory");
            } else if (id == R.id.nav_reviews) {
                // Navigate to ReviewsActivity
                navigateToActivitySafely(ReviewsActivity.class, "Reviews");
            } else if (id == R.id.nav_settings) {
                // Navigate to SettingsActivity 
                navigateToActivitySafely(SettingsActivity.class, "Settings");
            } else if (id == R.id.nav_logout) {
                logout();
                return true; // Return early as logout handles its own drawer closing
            }

            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error in navigation: " + e.getMessage());
            Toast.makeText(this, "Navigation error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            drawerLayout.closeDrawer(GravityCompat.START);
            return false;
        }
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
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

            // Ensure bottom navigation shows the correct selection
            bottomNavigationView.setSelectedItemId(R.id.navigation_dashboard);

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
    }
}