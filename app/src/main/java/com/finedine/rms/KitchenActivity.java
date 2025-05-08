package com.finedine.rms;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import com.finedine.rms.utils.FirebaseDatabaseSetup;
import com.finedine.rms.utils.FirebaseDbHelper;
import com.finedine.rms.firebase.FirebaseOrderDao;
import androidx.appcompat.app.AppCompatActivity;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.finedine.rms.utils.EmailSender;
import com.finedine.rms.utils.LayoutUtils;
import com.finedine.rms.utils.NotificationUtils;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;

public class KitchenActivity extends BaseActivity {
    private static final String TAG = "KitchenActivity";
    private Timer refreshTimer;
    private static final String PREF_NAME = "KitchenOrderPrefs";
    private static final String KNOWN_ORDERS_KEY = "knownOrderIds";
    private SharedPreferences sharedPreferences;
    private List<Long> knownOrderIds = new ArrayList<>();

    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView emptyView;
    private TextView kitchenNameView;
    private TextView timeView;
    private TextView dateView;
    private FloatingActionButton refreshButton;
    private TextView orderCountView;
    private ChipGroup filterChipGroup;
    private Chip chipPending;
    private Chip chipPreparing;
    private Chip chipReady;
    private Chip chipUpcoming;

    private List<Order> activeOrders;
    private KitchenOrderAdapter orderAdapter;

    private AppDatabase appDatabase;
    private ExecutorService databaseExecutor;
    private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
    private SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);

            // Set the content view directly first to ensure layout is available
            setContentView(R.layout.activity_kitchen);

            Log.d(TAG, "KitchenActivity.onCreate: Starting initialization");

            // Initialize Firebase Realtime Database
            try {
                FirebaseDatabaseSetup.enableFirebaseDatabase(this);
                Log.d(TAG, "Firebase database enabled");
            } catch (Exception e) {
                Log.e(TAG, "Error enabling Firebase", e);
            }

            // Get user role from intent or use a default
            String intentRole = getIntent().getStringExtra("user_role");
            if (intentRole != null) {
                userRole = intentRole;
                Log.d(TAG, "User role from intent: " + userRole);
            } else {
                // Use default from BaseActivity if available, or default to chef
                userRole = (userRole != null) ? userRole : "chef";
                Log.d(TAG, "Using default user role: " + userRole);
            }

            // Check user role for access control
            if (!("chef".equalsIgnoreCase(userRole) ||
                    "admin".equalsIgnoreCase(userRole) ||
                    "manager".equalsIgnoreCase(userRole))) {
                Log.w(TAG, "Unauthorized attempt to access Kitchen activity by role: " + userRole);
                Toast.makeText(this, "Access denied. This area is restricted.", Toast.LENGTH_SHORT).show();

                // Redirect to appropriate activity
                Intent redirectIntent;
                if ("waiter".equalsIgnoreCase(userRole) || "customer".equalsIgnoreCase(userRole)) {
                    redirectIntent = new Intent(this, OrderActivity.class);
                } else {
                    redirectIntent = new Intent(this, LoginActivity_fixed.class); // Changed to LoginActivity_fixed
                }
                redirectIntent.putExtra("user_role", userRole);
                startActivity(redirectIntent);
                finish();
                return;
            }

            // Initialize toolbar
            Toolbar toolbar = findViewById(R.id.toolbar);
            if (toolbar != null) {
                setSupportActionBar(toolbar);
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                    getSupportActionBar().setTitle("Kitchen Orders");
                }
            }

            // Initialize database
            try {
                appDatabase = AppDatabase.getDatabase(this);
                databaseExecutor = java.util.concurrent.Executors.newFixedThreadPool(4);

                if (appDatabase == null) {
                    Log.e(TAG, "AppDatabase is null");
                    Toast.makeText(this, "Error: Database not initialized", Toast.LENGTH_LONG).show();
                    createEmergencyUI();
                    return;
                }

                Log.d(TAG, "Database initialized successfully");
            } catch (Exception e) {
                Log.e(TAG, "Error initializing database", e);
                Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                createEmergencyUI();
                return;
            }

            // Initialize shared preferences
            sharedPreferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            knownOrderIds = getKnownOrderIdsFromSharedPreferences();

            // Create notification channels
            createNotificationChannels();

            // Set up UI components with safety checks
            try {
                recyclerView = findViewById(R.id.kitchenOrdersList);
                swipeRefreshLayout = findViewById(R.id.swipeRefresh);
                emptyView = findViewById(R.id.emptyView);
                kitchenNameView = findViewById(R.id.kitchenName);
                timeView = findViewById(R.id.timeView);
                dateView = findViewById(R.id.dateView);
                refreshButton = findViewById(R.id.refreshFab);
                orderCountView = findViewById(R.id.orderCountView);
                filterChipGroup = findViewById(R.id.filterChipGroup);
                chipPending = findViewById(R.id.chipPending);
                chipPreparing = findViewById(R.id.chipPreparing);
                chipReady = findViewById(R.id.chipReady);
                chipUpcoming = findViewById(R.id.chipUpcoming);

                // Verify critical views
                if (recyclerView == null || swipeRefreshLayout == null || emptyView == null) {
                    Log.e(TAG, "Critical UI components missing - creating emergency UI");
                    createEmergencyUI();
                    return;
                }

                Log.d(TAG, "UI components initialized successfully");
            } catch (Exception e) {
                Log.e(TAG, "Error initializing UI components", e);
                createEmergencyUI();
                return;
            }

            // Initialize activeOrders
            activeOrders = new ArrayList<>();

            // Setup RecyclerView
            try {
                recyclerView.setLayoutManager(new LinearLayoutManager(this));
                orderAdapter = new KitchenOrderAdapter(this, activeOrders, (order, action) -> {
                    if (order != null) {
                        handleOrderAction(order, action);
                    }
                });
                recyclerView.setAdapter(orderAdapter);
                Log.d(TAG, "RecyclerView setup complete");
            } catch (Exception e) {
                Log.e(TAG, "Error setting up RecyclerView", e);
            }

            // Setup SwipeRefreshLayout
            if (swipeRefreshLayout != null) {
                swipeRefreshLayout.setOnRefreshListener(this::refreshOrders);
                Log.d(TAG, "Swipe refresh listener setup complete");
            }

            // Setup refresh button
            if (refreshButton != null) {
                refreshButton.setOnClickListener(v -> refreshOrders());
                Log.d(TAG, "Refresh button setup complete");
            }

            // Setup filter chips
            if (filterChipGroup != null) {
                // Add filter change listener
                filterChipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
                    // Apply filter when chip selection changes
                    applyFilters();
                });
                Log.d(TAG, "Filter chip group setup complete");
            }

            // Initial data load
            updateDateTime();
            refreshOrders();
            Log.d(TAG, "Initial data load complete");

            // Start periodic refresh timer
            refreshTimer = new Timer();
            refreshTimer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    runOnUiThread(KitchenActivity.this::refreshOrders);
                }
            }, 15000, 15000); // 15 seconds, start after 15s to avoid double refresh

            Log.d(TAG, "KitchenActivity initialization complete");

        } catch (Exception e) {
            Log.e("KitchenActivity", "Error initializing Kitchen: " + e.getMessage(), e);
            Toast.makeText(this, "Error initializing Kitchen screen", Toast.LENGTH_LONG).show();

            // Create emergency UI as last resort
            createEmergencyUI();
        }
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            NotificationChannel newOrderChannel = new NotificationChannel(
                    NotificationUtils.NEW_ORDER_CHANNEL_ID,
                    "New Orders",
                    NotificationManager.IMPORTANCE_HIGH
            );
            newOrderChannel.setDescription("Notifications for new orders");

            NotificationChannel orderReadyChannel = new NotificationChannel(
                    NotificationUtils.ORDER_READY_CHANNEL_ID,
                    "Order Ready",
                    NotificationManager.IMPORTANCE_HIGH
            );
            orderReadyChannel.setDescription("Notifications for orders that are ready for pickup");

            NotificationChannel waiterPickupChannel = new NotificationChannel(
                    NotificationUtils.WAITER_PICKUP_CHANNEL_ID,
                    "Waiter Pickup",
                    NotificationManager.IMPORTANCE_HIGH
            );
            waiterPickupChannel.setDescription("Notifications for waiters to pick up orders");

            notificationManager.createNotificationChannel(newOrderChannel);
            notificationManager.createNotificationChannel(orderReadyChannel);
            notificationManager.createNotificationChannel(waiterPickupChannel);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshOrders();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (refreshTimer != null) {
            refreshTimer.cancel();
        }
        saveKnownOrderIdsToSharedPreferences();
    }

    public void refreshOrders() {
        // Safety check
        if (swipeRefreshLayout == null) {
            Log.e(TAG, "Cannot refresh orders - SwipeRefreshLayout is null");
            return;
        }

        swipeRefreshLayout.setRefreshing(true);

        try {
            Log.d(TAG, "Starting to refresh orders");
            FirebaseDbHelper dbHelper = FirebaseDbHelper.getInstance(this);

            if (dbHelper == null) {
                Log.e(TAG, "Firebase helper is null");
                swipeRefreshLayout.setRefreshing(false);
                Toast.makeText(this, "Error: Database connection unavailable", Toast.LENGTH_SHORT).show();
                return;
            }

            DatabaseReference ordersRef = dbHelper.getOrdersReference();
            if (ordersRef == null) {
                Log.e(TAG, "Orders reference is null");
                swipeRefreshLayout.setRefreshing(false);
                Toast.makeText(this, "Error: Orders database unavailable", Toast.LENGTH_SHORT).show();
                return;
            }

            Log.d(TAG, "Getting orders from Firebase");
            // Get orders from kitchen_orders path instead of regular orders path
            DatabaseReference kitchenOrdersRef = FirebaseDatabase.getInstance().getReference("kitchen_orders");
            kitchenOrdersRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    List<Order> newOrders = new ArrayList<>();

                    Log.d(TAG, "Order snapshot contains " + snapshot.getChildrenCount() + " orders");

                    for (DataSnapshot orderSnapshot : snapshot.getChildren()) {
                        try {
                            String status = orderSnapshot.child("status").getValue(String.class);

                            if (status == null) {
                                Log.d(TAG, "Order with null status found");
                                continue;
                            }

                            Log.d(TAG, "Found order with status: " + status);

                            // Include all orders: pending, preparing, ready, scheduled, and recently completed
                            if ("pending".equals(status) || "preparing".equals(status) || "ready".equals(status) ||
                                    "scheduled".equals(status) || ("completed".equals(status) && isRecentlyCompleted(orderSnapshot))) {

                                Log.d(TAG, "Processing order with status: " + status);

                                // Convert to Order object
                                Order order = new Order();

                                // Extract the order data
                                Long orderId = orderSnapshot.child("orderId").getValue(Long.class);
                                if (orderId != null) order.setOrderId(orderId);
                                else {
                                    // If orderId is null, use the snapshot key's hashcode
                                    String key = orderSnapshot.getKey();
                                    if (key != null) {
                                        orderId = (long) key.hashCode();
                                        order.setOrderId(orderId);
                                    }
                                }

                                Integer tableNumber = orderSnapshot.child("tableNumber").getValue(Integer.class);
                                if (tableNumber != null) order.setTableNumber(tableNumber);
                                else order.setTableNumber(0); // Fallback to table 0 if null

                                order.setStatus(status);

                                Long timestamp = orderSnapshot.child("timestamp").getValue(Long.class);
                                if (timestamp != null) order.setTimestamp(timestamp);
                                else order.setTimestamp(System.currentTimeMillis());

                                String customerName = orderSnapshot.child("customerName").getValue(String.class);
                                if (customerName != null) order.setCustomerName(customerName);
                                else order.setCustomerName("Customer"); // Set default name

                                String customerPhone = orderSnapshot.child("customerPhone").getValue(String.class);
                                if (customerPhone != null) order.setCustomerPhone(customerPhone);

                                String customerEmail = orderSnapshot.child("customerEmail").getValue(String.class);
                                if (customerEmail != null) order.setCustomerEmail(customerEmail);

                                Double total = orderSnapshot.child("total").getValue(Double.class);
                                if (total != null) order.setTotal(total);

                                // Set the Firebase key as externalId for future reference
                                order.setExternalId(orderSnapshot.getKey());

                                // Check if this is a recovered order
                                Boolean recovered = orderSnapshot.child("recoveredOrder").getValue(Boolean.class);
                                if (recovered != null && recovered) {
                                    order.setRecovered(true);
                                    // Log for debugging recovered orders
                                    Log.d(TAG, "Found recovered order #" + orderId + " with table " + tableNumber);
                                }

                                Log.d(TAG, "Successfully parsed order #" + orderId + " with table " +
                                        tableNumber + " and status " + status);

                                // Add to our list
                                newOrders.add(order);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing order: " + e.getMessage(), e);
                        }
                    }

                    Log.d(TAG, "Total orders to display in kitchen: " + newOrders.size());

                    // Sort orders by timestamp or scheduled time if available
                    Collections.sort(newOrders, (o1, o2) -> {
                        // If orders have different statuses, sort by priority
                        if (!o1.getStatus().equals(o2.getStatus())) {
                            // Priority: pending > preparing > ready > scheduled > completed
                            return getOrderStatusPriority(o1.getStatus()) - getOrderStatusPriority(o2.getStatus());
                        }
                        // If same status, sort by timestamp
                        return Long.compare(o1.getTimestamp(), o2.getTimestamp());
                    });

                    // Update the UI
                    runOnUiThread(() -> {
                        try {
                            if (isFinishing() || isDestroyed()) {
                                Log.d(TAG, "Activity is finishing or destroyed, skipping UI update");
                                return;
                            }

                            Log.d(TAG, "Updating UI with orders");

                            // Store full list of orders
                            final List<Order> allOrders = new ArrayList<>(newOrders);

                            // Apply filters
                            final List<Order> filteredOrders = filterOrders(newOrders);

                            if (activeOrders != null && orderAdapter != null) {
                                activeOrders.clear();
                                activeOrders.addAll(filteredOrders);
                                orderAdapter.notifyDataSetChanged();
                            } else {
                                Log.e(TAG, "activeOrders or orderAdapter is null");
                            }

                            if (orderCountView != null) {
                                orderCountView.setText(String.valueOf(filteredOrders.size()));
                            }

                            // Update empty state visibility
                            if (filteredOrders.isEmpty()) {
                                if (emptyView != null) emptyView.setVisibility(View.VISIBLE);
                                if (recyclerView != null) recyclerView.setVisibility(View.GONE);
                                Log.d(TAG, "No orders to display - showing empty view");
                            } else {
                                if (emptyView != null) emptyView.setVisibility(View.GONE);
                                if (recyclerView != null) recyclerView.setVisibility(View.VISIBLE);
                                Log.d(TAG, "Showing " + filteredOrders.size() + " orders in the kitchen view");
                            }

                            if (swipeRefreshLayout != null) {
                                swipeRefreshLayout.setRefreshing(false);
                            }

                            updateDateTime();

                            // Check for new orders and notify
                            checkForNewOrders(allOrders);

                        } catch (Exception e) {
                            Log.e(TAG, "Error updating UI: " + e.getMessage(), e);
                            if (swipeRefreshLayout != null) {
                                swipeRefreshLayout.setRefreshing(false);
                            }
                        }
                    });
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "Firebase error: " + error.getMessage());
                    runOnUiThread(() -> {
                        if (swipeRefreshLayout != null) {
                            swipeRefreshLayout.setRefreshing(false);
                        }
                        Toast.makeText(KitchenActivity.this, "Error loading orders: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error refreshing orders: " + e.getMessage(), e);
            if (swipeRefreshLayout != null) {
                swipeRefreshLayout.setRefreshing(false);
            }
            Toast.makeText(this, "Error loading orders: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void updateDateTime() {
        try {
            Date now = new Date();
            if (timeView != null) {
                timeView.setText(timeFormat.format(now));
            }
            if (dateView != null) {
                dateView.setText(dateFormat.format(now));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating date/time: " + e.getMessage());
        }
    }

    private void handleOrderAction(Order order, String action) {
        if (order == null) return;

        try {
            if (action.equals("start")) {
                FirebaseDatabaseSetup.updateOrderStatus(this, order.getOrderId(), "preparing");
                runOnUiThread(() -> {
                    Toast.makeText(this, "Started preparing order #" + order.getOrderId(), Toast.LENGTH_SHORT).show();
                });
            } else if (action.equals("ready")) {
                FirebaseDatabaseSetup.updateOrderStatus(this, order.getOrderId(), "ready");
                runOnUiThread(() -> {
                    Toast.makeText(this, "Order #" + order.getOrderId() + " is ready for serving", Toast.LENGTH_SHORT).show();
                    notifyCustomerOrderReady(order);
                    notifyWaitersOrderReady(order);
                });
            } else if (action.equals("done")) {
                FirebaseDatabaseSetup.updateOrderStatus(this, order.getOrderId(), "completed");
                runOnUiThread(() -> {
                    Toast.makeText(this, "Order #" + order.getOrderId() + " has been completed", Toast.LENGTH_SHORT).show();
                });
            }

            databaseExecutor.execute(() -> {
                try {
                    if ("start".equals(action)) {
                        // Update order status to preparing
                        Log.d(TAG, "Updating order #" + order.getOrderId() + " status to 'preparing'");
                        appDatabase.orderDao().updateOrderStatus(order.getOrderId(), "preparing");

                        // Double-check the update was successful
                        Order updatedOrder = appDatabase.orderDao().getOrderById(order.getOrderId());
                        if (updatedOrder != null && "preparing".equals(updatedOrder.getStatus())) {
                            Log.d(TAG, "Successfully updated order status to 'preparing'");
                        } else {
                            Log.e(TAG, "Failed to update order status to 'preparing'");
                        }

                        runOnUiThread(() -> {
                            try {
                                Toast.makeText(this, "Started preparing order #" + order.getOrderId(), Toast.LENGTH_SHORT).show();
                                refreshOrders();
                            } catch (Exception e) {
                                Log.e("KitchenActivity", "UI update error: " + e.getMessage(), e);
                            }
                        });

                    } else if ("ready".equals(action)) {
                        // Update order status to ready
                        Log.d(TAG, "Updating order #" + order.getOrderId() + " status to 'ready'");
                        appDatabase.orderDao().updateOrderStatus(order.getOrderId(), "ready");

                        // Double-check the update was successful
                        Order updatedOrder = appDatabase.orderDao().getOrderById(order.getOrderId());
                        if (updatedOrder != null && "ready".equals(updatedOrder.getStatus())) {
                            Log.d(TAG, "Successfully updated order status to 'ready'");
                        } else {
                            Log.e(TAG, "Failed to update order status to 'ready'");
                        }

                        runOnUiThread(() -> {
                            try {
                                Toast.makeText(this, "Order #" + order.getOrderId() + " is ready for serving", Toast.LENGTH_SHORT).show();

                                // Send notification to customer if we have their contact information
                                if (order.getCustomerPhone() != null && !order.getCustomerPhone().isEmpty()) {
                                    notifyCustomerOrderReady(order);
                                }

                                // Also notify waiters about the ready order
                                notifyWaitersOrderReady(order);

                                refreshOrders();
                            } catch (Exception e) {
                                Log.e("KitchenActivity", "UI update error: " + e.getMessage(), e);
                            }
                        });
                    } else if ("done".equals(action)) {
                        // Update order status to completed
                        Log.d(TAG, "Updating order #" + order.getOrderId() + " status to 'completed'");
                        appDatabase.orderDao().updateOrderStatus(order.getOrderId(), "completed");
                        order.setCompletionTime(System.currentTimeMillis());

                        // Update order in database with completion time
                        appDatabase.orderDao().updateOrderCompletionTime(order.getOrderId(), order.getCompletionTime());

                        // Double-check the update was successful
                        Order updatedOrder = appDatabase.orderDao().getOrderById(order.getOrderId());
                        if (updatedOrder != null && "completed".equals(updatedOrder.getStatus()) && updatedOrder.getCompletionTime() != 0) {
                            Log.d(TAG, "Successfully updated order status to 'completed' with completion time");
                        } else {
                            Log.e(TAG, "Failed to update order status to 'completed' with completion time");
                        }

                        runOnUiThread(() -> {
                            try {
                                Toast.makeText(this, "Order #" + order.getOrderId() + " has been completed", Toast.LENGTH_SHORT).show();
                                refreshOrders();
                            } catch (Exception e) {
                                Log.e("KitchenActivity", "UI update error: " + e.getMessage(), e);
                            }
                        });
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error updating order status: " + e.getMessage(), e);
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Error updating order: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error processing order action: " + e.getMessage(), e);
            Toast.makeText(this, "Error processing action: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Notify customer that their order is ready
     *
     * @param order The order that is ready
     */
    private void notifyCustomerOrderReady(Order order) {
        // In a real app, we would send an SMS to the customer's phone number
        // For now, we'll just use a notification and, if available, email

        // Create a notification for the customer
        NotificationUtils notificationUtils = new NotificationUtils();
        notificationUtils.sendOrderReadyNotification(this, (int) order.getOrderId(),
                order.getTableNumber(), order.getCustomerName());

        // If we have an email, also send an email notification
        if (order.getCustomerEmail() != null && !order.getCustomerEmail().isEmpty()) {
            sendOrderReadyEmail(order);
        }
    }

    /**
     * Notify waiters that an order is ready for pickup
     *
     * @param order The order that is ready
     */
    private void notifyWaitersOrderReady(Order order) {
        // Send notification to waiters using the new notification method
        NotificationUtils notificationUtils = new NotificationUtils();
        notificationUtils.sendWaiterPickupNotification(this, (int) order.getOrderId(),
                order.getTableNumber());

        Log.d(TAG, "Sent waiter pickup notification for Order #" + order.getOrderId());
    }

    /**
     * Send email notification to customer
     *
     * @param order The order that is ready
     */
    private void sendOrderReadyEmail(Order order) {
        // Using Gmail provider
        EmailSender.sendOrderReadyEmail(this, order.getCustomerEmail(),
                order.getCustomerName(), order.getOrderId(), EmailSender.EmailProvider.GMAIL,
                new EmailSender.EmailCallback() {
                    @Override
                    public void onSuccess() {
                        Log.d(TAG, "Email notification sent successfully to " + order.getCustomerEmail());
                    }

                    @Override
                    public void onFailure(String error) {
                        Log.e(TAG, "Failed to send email notification: " + error);
                    }
                });
    }

    private void checkForNewOrders(List<Order> orders) {
        for (Order order : orders) {
            if (order.getStatus().equals("pending") && !knownOrderIds.contains(order.getOrderId())) {
                knownOrderIds.add(order.getOrderId());
                NotificationUtils notificationUtils = new NotificationUtils();
                notificationUtils.sendNewOrderNotification(this, (int) order.getOrderId(), order.getTableNumber());
            }
        }
    }

    private List<Long> getKnownOrderIdsFromSharedPreferences() {
        List<Long> knownOrderIds = new ArrayList<>();
        String knownOrdersString = sharedPreferences.getString(KNOWN_ORDERS_KEY, "");
        if (!knownOrdersString.isEmpty()) {
            String[] knownOrdersArray = knownOrdersString.split(",");
            for (String orderIdString : knownOrdersArray) {
                try {
                    long orderId = Long.parseLong(orderIdString);
                    knownOrderIds.add(orderId);
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Error parsing order ID from shared preferences: " + e.getMessage(), e);
                }
            }
        }
        return knownOrderIds;
    }

    private void saveKnownOrderIdsToSharedPreferences() {
        StringBuilder knownOrdersStringBuilder = new StringBuilder();
        for (Long orderId : knownOrderIds) {
            knownOrdersStringBuilder.append(orderId).append(",");
        }
        if (knownOrdersStringBuilder.length() > 0) {
            knownOrdersStringBuilder.deleteCharAt(knownOrdersStringBuilder.length() - 1);
        }
        sharedPreferences.edit().putString(KNOWN_ORDERS_KEY, knownOrdersStringBuilder.toString()).apply();
    }

    /**
     * Create a simplified emergency UI if normal layout fails to load correctly
     * This ensures the kitchen can still see and process orders
     */
    private void createEmergencyUI() {
        try {
            // Clear any existing layout first
            setContentView(new LinearLayout(this)); // Temporary blank layout

            // Create a basic layout
            LinearLayout mainLayout = new LinearLayout(this);
            mainLayout.setOrientation(LinearLayout.VERTICAL);
            mainLayout.setPadding(20, 20, 20, 20);

            // Add title
            TextView titleView = new TextView(this);
            titleView.setText("Kitchen Orders (Emergency Mode)");
            titleView.setTextSize(20);
            titleView.setPadding(0, 0, 0, 20);
            mainLayout.addView(titleView);

            // Add manual refresh button
            Button refreshButton = new Button(this);
            refreshButton.setText("Refresh Orders");
            refreshButton.setOnClickListener(v -> refreshOrders());
            mainLayout.addView(refreshButton);

            // Create simple list container
            LinearLayout ordersContainer = new LinearLayout(this);
            ordersContainer.setOrientation(LinearLayout.VERTICAL);
            ordersContainer.setPadding(10, 10, 10, 10);

            // Add a scrollable container
            ScrollView scrollView = new ScrollView(this);
            scrollView.addView(ordersContainer);
            mainLayout.addView(scrollView);

            // Set this as main layout
            setContentView(mainLayout);

            // We'll reuse the container to show orders
            activeOrders = new ArrayList<>();

            // Manual loading of orders
            TextView loadingText = new TextView(this);
            loadingText.setText("Loading orders...");
            ordersContainer.addView(loadingText);

            // Set up a timer to refresh
            refreshTimer = new Timer();
            refreshTimer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    if (databaseExecutor != null && appDatabase != null) {
                        databaseExecutor.execute(() -> {
                            try {
                                // Get pending orders
                                List<Order> pendingOrders = appDatabase.orderDao().getOrdersByStatus("pending");
                                List<Order> preparingOrders = appDatabase.orderDao().getOrdersByStatus("preparing");

                                // Combine orders
                                final List<Order> allOrders = new ArrayList<>();
                                if (pendingOrders != null) allOrders.addAll(pendingOrders);
                                if (preparingOrders != null) allOrders.addAll(preparingOrders);

                                // Update UI
                                runOnUiThread(() -> {
                                    // Clear existing orders
                                    ordersContainer.removeAllViews();

                                    if (allOrders.isEmpty()) {
                                        TextView noOrdersText = new TextView(KitchenActivity.this);
                                        noOrdersText.setText("No orders waiting");
                                        ordersContainer.addView(noOrdersText);
                                    } else {
                                        // Create a basic view for each order
                                        for (Order order : allOrders) {
                                            addEmergencyOrderView(ordersContainer, order);
                                        }
                                    }
                                });
                            } catch (Exception e) {
                                Log.e(TAG, "Error refreshing emergency orders", e);
                            }
                        });
                    }
                }
            }, 0, 10000); // 10 seconds refresh

            Toast.makeText(this, "Using emergency kitchen view", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Log.e(TAG, "Failed to create emergency UI", e);
            // Use the layout utils as last resort
            LayoutUtils.createEmergencyLayout(this, "Kitchen Orders");
        }
    }

    /**
     * Add a simple order view to the container in emergency mode
     */
    private void addEmergencyOrderView(ViewGroup container, Order order) {
        try {
            if (order == null || container == null) return;

            // Create a card-like layout for the order
            LinearLayout orderCard = new LinearLayout(this);
            orderCard.setOrientation(LinearLayout.VERTICAL);
            orderCard.setPadding(15, 15, 15, 15);
            orderCard.setBackgroundColor(0xFFEEEEEE); // Light gray background

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 0, 0, 15); // Add margin to bottom
            orderCard.setLayoutParams(params);

            // Order info text
            TextView orderInfo = new TextView(this);
            String status = order.getStatus();
            orderInfo.setText("Order #" + order.getOrderId() +
                    " - Table " + order.getTableNumber() +
                    " - Status: " + status);
            orderInfo.setTextSize(16);
            orderCard.addView(orderInfo);

            // Add action buttons based on status
            if ("pending".equals(status)) {
                Button startButton = new Button(this);
                startButton.setText("Start Preparing");
                startButton.setOnClickListener(v -> handleOrderAction(order, "start"));
                orderCard.addView(startButton);
            } else if ("preparing".equals(status)) {
                Button readyButton = new Button(this);
                readyButton.setText("Mark Ready");
                readyButton.setOnClickListener(v -> handleOrderAction(order, "ready"));
                orderCard.addView(readyButton);
            }

            container.addView(orderCard);
        } catch (Exception e) {
            Log.e(TAG, "Error adding emergency order view", e);
        }
    }

    /**
     * Check if an order was completed recently (within last 30 minutes)
     */
    private boolean isRecentlyCompleted(DataSnapshot orderSnapshot) {
        try {
            Long completionTime = orderSnapshot.child("completionTime").getValue(Long.class);
            if (completionTime != null) {
                long currentTime = System.currentTimeMillis();
                return (currentTime - completionTime) <= 30 * 60 * 1000; // 30 minutes
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking completion time", e);
        }
        return false;
    }

    /**
     * Create test orders in Firebase for demonstration
     */
    private void createTestOrders() {
        try {
            // Use our SampleDataLoader to load and display orders
            com.finedine.rms.utils.SampleDataLoader.loadAndDisplaySampleData(this, "kitchen");
        } catch (Exception e) {
            Log.e(TAG, "Error creating test orders", e);
            Toast.makeText(this, "Failed to create test orders", Toast.LENGTH_SHORT).show();

            // Fall back to the original method if the sample data loader fails
            try {
                FirebaseDbHelper dbHelper = FirebaseDbHelper.getInstance(this);
                DatabaseReference ordersRef = dbHelper.getOrdersReference();

                // Show toast
                Toast.makeText(this, "Creating test orders (fallback method)...", Toast.LENGTH_SHORT).show();

                // Create 4 sample orders with different statuses including upcoming
                createSampleOrder(ordersRef, 1, "pending");
                createSampleOrder(ordersRef, 2, "preparing");
                createSampleOrder(ordersRef, 3, "ready");
                long tomorrowTimestamp = System.currentTimeMillis() + (24 * 60 * 60 * 1000);
                createSampleOrder(ordersRef, 4, "scheduled", tomorrowTimestamp, "Scheduled Customer");

                // Refresh the view
                new android.os.Handler().postDelayed(this::refreshOrders, 1000);
            } catch (Exception ex) {
                Log.e(TAG, "Fallback method also failed", ex);
                Toast.makeText(this, "Could not create test orders", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Create a single sample order
     */
    private void createSampleOrder(DatabaseReference ordersRef, int tableNumber, String status) {
        createSampleOrder(ordersRef, tableNumber, status, System.currentTimeMillis(), "Test Customer");
    }

    /**
     * Create a sample order with specified timestamp and customer name
     */
    private void createSampleOrder(DatabaseReference ordersRef, int tableNumber, String status, long timestamp, String customerName) {
        try {
            // Generate a unique key
            String key = ordersRef.push().getKey();
            if (key == null) return;

            // Get current time
            long now = System.currentTimeMillis();

            // Create order data
            Map<String, Object> orderData = new HashMap<>();
            orderData.put("orderId", Math.abs(key.hashCode()));
            orderData.put("tableNumber", tableNumber);
            orderData.put("status", status);
            orderData.put("timestamp", timestamp);
            orderData.put("customerName", customerName);
            orderData.put("customerPhone", "555-1234");
            orderData.put("customerEmail", "test@example.com");
            orderData.put("total", 45.99);
            orderData.put("externalId", key);

            // Save to Firebase
            ordersRef.child(key).setValue(orderData);
            Log.d(TAG, "Created sample order with status: " + status + " for table " + tableNumber);

            // Create some order items
            FirebaseDbHelper dbHelper = FirebaseDbHelper.getInstance(this);
            DatabaseReference itemsRef = dbHelper.getOrderItemsReference();

            // Add sample items
            createSampleOrderItem(itemsRef, Math.abs(key.hashCode()), "Burger", 15.99, 1);
            createSampleOrderItem(itemsRef, Math.abs(key.hashCode()), "Fries", 5.99, 2);
            createSampleOrderItem(itemsRef, Math.abs(key.hashCode()), "Soda", 2.99, 1);

        } catch (Exception e) {
            Log.e(TAG, "Error creating sample order", e);
        }
    }

    /**
     * Create a sample order item
     */
    private void createSampleOrderItem(DatabaseReference itemsRef, long orderId, String name, double price, int quantity) {
        try {
            // Generate a unique key
            String key = itemsRef.push().getKey();
            if (key == null) return;

            // Create item data
            Map<String, Object> itemData = new HashMap<>();
            itemData.put("id", Math.abs(key.hashCode()));
            itemData.put("orderId", orderId);
            itemData.put("name", name);
            itemData.put("price", price);
            itemData.put("quantity", quantity);
            itemData.put("notes", "Sample item");

            // Save to Firebase
            itemsRef.child(key).setValue(itemData);
            Log.d(TAG, "Created sample order item: " + name + " for order " + orderId);

        } catch (Exception e) {
            Log.e(TAG, "Error creating sample order item", e);
        }
    }

    /**
     * Get priority number for order status for sorting
     * Lower number = higher priority
     */
    private int getOrderStatusPriority(String status) {
        if (status == null) return 99;
        switch (status) {
            case "pending":
                return 1;
            case "preparing":
                return 2;
            case "ready":
                return 3;
            case "scheduled":
                return 4;
            case "completed":
                return 5;
            default:
                return 99;
        }
    }

    /**
     * Filter orders based on selected chips
     */
    private void applyFilters() {
        if (activeOrders == null) return;

        // Get copy of original orders from adapter before filtering
        List<Order> filteredOrders = filterOrders(new ArrayList<>(activeOrders));

        // Update adapter with filtered orders
        activeOrders.clear();
        activeOrders.addAll(filteredOrders);

        // Update UI
        if (orderAdapter != null) {
            orderAdapter.notifyDataSetChanged();
        }

        // Update empty state
        if (activeOrders.isEmpty()) {
            if (emptyView != null) emptyView.setVisibility(View.VISIBLE);
            if (recyclerView != null) recyclerView.setVisibility(View.GONE);
        } else {
            if (emptyView != null) emptyView.setVisibility(View.GONE);
            if (recyclerView != null) recyclerView.setVisibility(View.VISIBLE);
        }

        // Update count
        if (orderCountView != null) {
            orderCountView.setText(String.valueOf(activeOrders.size()));
        }
    }

    /**
     * Apply filters to the list of orders
     */
    private List<Order> filterOrders(List<Order> orders) {
        if (orders == null) return new ArrayList<>();

        // If filter chips aren't available, return all orders
        if (chipPending == null || chipPreparing == null || chipReady == null || chipUpcoming == null) {
            return orders;
        }

        // Get filter states
        boolean showPending = chipPending.isChecked();
        boolean showPreparing = chipPreparing.isChecked();
        boolean showReady = chipReady.isChecked();
        boolean showUpcoming = chipUpcoming.isChecked();

        // If all filters are disabled, show everything
        if (!showPending && !showPreparing && !showReady && !showUpcoming) {
            return orders;
        }

        // Filter orders by status
        List<Order> filteredOrders = new ArrayList<>();
        for (Order order : orders) {
            String status = order.getStatus();

            if ((showPending && "pending".equals(status)) ||
                    (showPreparing && "preparing".equals(status)) ||
                    (showReady && "ready".equals(status)) ||
                    (showUpcoming && "scheduled".equals(status))) {
                filteredOrders.add(order);
            }
        }

        return filteredOrders;
    }
}