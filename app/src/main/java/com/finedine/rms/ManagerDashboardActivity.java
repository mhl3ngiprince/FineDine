package com.finedine.rms;

import android.app.DatePickerDialog;
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
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
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
            Intent intent = new Intent(this, ReservationActivity.class);
            intent.putExtra("show_reservations", true);
            navigateToActivitySafely(ReservationActivity.class, "Reservations");
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
    }
}