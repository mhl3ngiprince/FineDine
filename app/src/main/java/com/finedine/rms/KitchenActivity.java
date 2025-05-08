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
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.finedine.rms.utils.EmailSender;
import com.finedine.rms.utils.NotificationUtils;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
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

    private List<Order> activeOrders;
    private KitchenOrderAdapter orderAdapter;

    private AppDatabase appDatabase;
    private ExecutorService databaseExecutor;
    private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
    private SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
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
                    redirectIntent = new Intent(this, LoginActivity.class);
                }
                redirectIntent.putExtra("user_role", userRole);
                startActivity(redirectIntent);
                finish();
                return;
            }

            // Use modern navigation panel
            setupModernNavigationPanel("Kitchen Orders", R.layout.activity_kitchen);

            // Initialize database
            appDatabase = ((FineDineApplication) getApplication()).getDatabase();
            databaseExecutor = ((FineDineApplication) getApplication()).getDatabaseExecutor();

            if (appDatabase == null || databaseExecutor == null) {
                Toast.makeText(this, "Error: Database not initialized", Toast.LENGTH_LONG).show();
                finish();
                return;
            }

            // Initialize shared preferences
            sharedPreferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            knownOrderIds = getKnownOrderIdsFromSharedPreferences();

            // Create notification channels
            createNotificationChannels();

            // Set up UI components
            recyclerView = findViewById(R.id.kitchenOrdersList);
            swipeRefreshLayout = findViewById(R.id.swipeRefresh);
            emptyView = findViewById(R.id.emptyView);
            kitchenNameView = findViewById(R.id.kitchenName);
            timeView = findViewById(R.id.timeView);
            dateView = findViewById(R.id.dateView);
            refreshButton = findViewById(R.id.refreshFab);
            orderCountView = findViewById(R.id.orderCountView);

            // Initialize activeOrders
            activeOrders = new ArrayList<>();

            // Setup RecyclerView
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            orderAdapter = new KitchenOrderAdapter(this, activeOrders, (order, action) -> {
                if (order != null) {
                    handleOrderAction(order, action);
                }
            });
            recyclerView.setAdapter(orderAdapter);

            // Setup SwipeRefreshLayout
            swipeRefreshLayout.setOnRefreshListener(this::refreshOrders);

            // Setup refresh button
            refreshButton.setOnClickListener(v -> refreshOrders());

            // Initial data load
            updateDateTime();
            refreshOrders();

            // Start periodic refresh timer
            refreshTimer = new Timer();
            refreshTimer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    runOnUiThread(KitchenActivity.this::refreshOrders);
                }
            }, 0, 15000); // 15 seconds
        } catch (Exception e) {
            Log.e("KitchenActivity", "Error initializing Kitchen: " + e.getMessage(), e);
            Toast.makeText(this, "Error initializing Kitchen screen", Toast.LENGTH_LONG).show();
            finish();
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

    private void refreshOrders() {
        swipeRefreshLayout.setRefreshing(true);

        try {
            if (databaseExecutor == null || appDatabase == null) {
                Log.e("KitchenActivity", "Database or executor null");
                swipeRefreshLayout.setRefreshing(false);
                Toast.makeText(this, "Database connection error", Toast.LENGTH_SHORT).show();
                return;
            }

            databaseExecutor.execute(() -> {
                try {
                    // Get pending, preparing, and completed orders
                    List<Order> newOrders = new ArrayList<>();

                    // Get pending orders
                    try {
                        List<Order> pendingOrders = appDatabase.orderDao().getOrdersByStatus("pending");
                        if (pendingOrders != null) {
                            Log.d("KitchenActivity", "Found " + pendingOrders.size() + " pending orders");
                            newOrders.addAll(pendingOrders);
                        }
                    } catch (Exception e) {
                        Log.e("KitchenActivity", "Error loading pending orders: " + e.getMessage(), e);
                    }

                    // Get preparing orders
                    try {
                        List<Order> preparingOrders = appDatabase.orderDao().getOrdersByStatus("preparing");
                        if (preparingOrders != null) {
                            Log.d("KitchenActivity", "Found " + preparingOrders.size() + " preparing orders");
                            newOrders.addAll(preparingOrders);
                        }
                    } catch (Exception e) {
                        Log.e("KitchenActivity", "Error loading preparing orders: " + e.getMessage(), e);
                    }

                    // Get completed orders
                    try {
                        List<Order> completedOrders = appDatabase.orderDao().getOrdersByStatus("completed");
                        if (completedOrders != null) {
                            Log.d("KitchenActivity", "Found " + completedOrders.size() + " completed orders");
                            newOrders.addAll(completedOrders);
                        }
                    } catch (Exception e) {
                        Log.e("KitchenActivity", "Error loading completed orders: " + e.getMessage(), e);
                    }

                    // Clean up completed orders
                    cleanCompletedOrders(newOrders);

                    // For each order, fetch the items separately
                    List<OrderWithItems> ordersWithItems = new ArrayList<>();
                    for (Order order : newOrders) {
                        try {
                            if (order != null && order.getOrderId() > 0) {
                                OrderWithItems orderWithItems = appDatabase.orderDao().getOrderWithItems(order.getOrderId());
                                if (orderWithItems != null && orderWithItems.orderItems != null) {
                                    if (!orderWithItems.orderItems.isEmpty()) {
                                        ordersWithItems.add(orderWithItems);
                                        Log.d("KitchenActivity", "Order #" + order.getOrderId() + " has " +
                                                orderWithItems.orderItems.size() + " items");
                                    } else {
                                        Log.w("KitchenActivity", "Order #" + order.getOrderId() + " has no items");
                                    }
                                } else {
                                    Log.w("KitchenActivity", "Order #" + order.getOrderId() +
                                            " has null orderWithItems or null orderItems");
                                }
                            }
                        } catch (Exception e) {
                            Log.e("KitchenActivity", "Error loading items for order " + order.getOrderId() + ": " + e.getMessage(), e);
                        }
                    }

                    // Convert to UI-ready list
                    final List<Order> finalOrders = new ArrayList<>();
                    for (OrderWithItems orderWithItems : ordersWithItems) {
                        if (orderWithItems != null && orderWithItems.order != null) {
                            finalOrders.add(orderWithItems.order);
                        }
                    }

                    // Update UI on the main thread
                    runOnUiThread(() -> {
                        try {
                            if (isFinishing() || isDestroyed()) return;

                            activeOrders.clear();
                            activeOrders.addAll(finalOrders);

                            if (orderAdapter != null) {
                                orderAdapter.notifyDataSetChanged();
                            }

                            if (orderCountView != null) {
                                orderCountView.setText(String.valueOf(activeOrders.size()));
                            }

                            if (activeOrders.isEmpty()) {
                                if (emptyView != null) emptyView.setVisibility(View.VISIBLE);
                                if (recyclerView != null) recyclerView.setVisibility(View.GONE);
                            } else {
                                if (emptyView != null) emptyView.setVisibility(View.GONE);
                                if (recyclerView != null) recyclerView.setVisibility(View.VISIBLE);
                            }

                            if (swipeRefreshLayout != null) {
                                swipeRefreshLayout.setRefreshing(false);
                            }
                            updateDateTime();

                            // Check for new orders and notify kitchen staff
                            checkForNewOrders(finalOrders);
                        } catch (Exception e) {
                            Log.e("KitchenActivity", "Error updating UI: " + e.getMessage(), e);
                            if (swipeRefreshLayout != null) {
                                swipeRefreshLayout.setRefreshing(false);
                            }
                        }
                    });
                } catch (Exception e) {
                    Log.e("KitchenActivity", "Error refreshing orders: " + e.getMessage(), e);
                    runOnUiThread(() -> {
                        if (swipeRefreshLayout != null) {
                            swipeRefreshLayout.setRefreshing(false);
                        }
                        Toast.makeText(KitchenActivity.this, "Error loading orders", Toast.LENGTH_SHORT).show();
                    });
                }
            });
        } catch (Exception e) {
            Log.e("KitchenActivity", "Error executing database query: " + e.getMessage(), e);
            swipeRefreshLayout.setRefreshing(false);
            Toast.makeText(this, "Error connecting to database", Toast.LENGTH_SHORT).show();
        }
    }

    private void cleanCompletedOrders(List<Order> orders) {
        // Remove orders that have been completed for more than 30 minutes
        long currentTime = System.currentTimeMillis();
        Iterator<Order> iterator = orders.iterator();
        while (iterator.hasNext()) {
            Order order = iterator.next();
            if (order.getStatus().equals("completed")) {
                long completionTime = order.getCompletionTime();
                if (completionTime != 0 && (currentTime - completionTime) > 30 * 60 * 1000) {
                    iterator.remove();
                    try {
                        appDatabase.orderDao().deleteOrder(order.getOrderId());
                    } catch (Exception e) {
                        Log.e("KitchenActivity", "Error deleting completed order: " + e.getMessage(), e);
                    }
                }
            }
        }
    }

    private void updateDateTime() {
        Date now = new Date();
        timeView.setText(timeFormat.format(now));
        dateView.setText(dateFormat.format(now));
    }

    private void handleOrderAction(Order order, String action) {
        if (order == null) return;

        try {
            if (databaseExecutor == null || appDatabase == null) {
                Toast.makeText(this, "Database connection error", Toast.LENGTH_SHORT).show();
                return;
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
}