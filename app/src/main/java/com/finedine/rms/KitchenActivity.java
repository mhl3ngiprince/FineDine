package com.finedine.rms;

import android.content.Intent;
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
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;

public class KitchenActivity extends AppCompatActivity {
    private static final String TAG = "KitchenActivity";

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
        setContentView(R.layout.activity_kitchen);

        try {
            // Initialize database
            appDatabase = ((FineDineApplication) getApplication()).getDatabase();
            databaseExecutor = ((FineDineApplication) getApplication()).getDatabaseExecutor();

            if (appDatabase == null || databaseExecutor == null) {
                Toast.makeText(this, "Error: Database not initialized", Toast.LENGTH_LONG).show();
                finish();
                return;
            }

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
        } catch (Exception e) {
            Log.e("KitchenActivity", "Error initializing Kitchen: " + e.getMessage(), e);
            Toast.makeText(this, "Error initializing Kitchen screen", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshOrders();
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
                    // Get pending and preparing orders
                    List<Order> newOrders = new ArrayList<>();

                    try {
                        List<Order> pendingOrders = appDatabase.orderDao().getOrdersByStatus("pending");
                        if (pendingOrders != null) {
                            newOrders.addAll(pendingOrders);
                        }
                    } catch (Exception e) {
                        Log.e("KitchenActivity", "Error loading pending orders: " + e.getMessage(), e);
                    }

                    try {
                        List<Order> preparingOrders = appDatabase.orderDao().getOrdersByStatus("preparing");
                        if (preparingOrders != null) {
                            newOrders.addAll(preparingOrders);
                        }
                    } catch (Exception e) {
                        Log.e("KitchenActivity", "Error loading preparing orders: " + e.getMessage(), e);
                    }

                    // For each order, fetch the items separately
                    List<OrderWithItems> ordersWithItems = new ArrayList<>();
                    for (Order order : newOrders) {
                        try {
                            if (order != null && order.getOrderId() > 0) {
                                OrderWithItems orderWithItems = appDatabase.orderDao().getOrderWithItems(order.getOrderId());
                                if (orderWithItems != null && orderWithItems.orderItems != null && !orderWithItems.orderItems.isEmpty()) {
                                    ordersWithItems.add(orderWithItems);
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
                        appDatabase.orderDao().updateOrderStatus(order.getOrderId(), "preparing");

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
                        appDatabase.orderDao().updateOrderStatus(order.getOrderId(), "ready");

                        runOnUiThread(() -> {
                            try {
                                Toast.makeText(this, "Order #" + order.getOrderId() + " is ready for serving", Toast.LENGTH_SHORT).show();

                                // Send notification to customer if we have their contact information
                                if (order.getCustomerPhone() != null && !order.getCustomerPhone().isEmpty()) {
                                    notifyCustomerOrderReady(order);
                                }

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
}