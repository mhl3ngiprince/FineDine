package com.finedine.rms;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.finedine.rms.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.finedine.rms.utils.RoleManager;
import com.finedine.rms.utils.SharedPrefsManager;

public class KitchenActivity extends BaseActivity {
    private RecyclerView rvOrders;
    private OrderAdapter orderAdapter;
    private final List<Order> orders = new ArrayList<>();
    private DatabaseReference ordersRef;
    private DatabaseReference orderItemsRef;
    private SharedPrefsManager prefsManager;
    private static final String TAG = "KitchenActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_kitchen);

        // Initialize SharedPrefsManager
        prefsManager = new SharedPrefsManager(this);

        // Verify user role
        String userRole = prefsManager.getUserRole();
        Log.d(TAG, "KitchenActivity - Current user role: " + userRole);

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
        if (!RoleManager.ROLE_CHEF.equals(userRole) && !RoleManager.ROLE_MANAGER.equals(userRole)) {
            Log.e(TAG, "Unauthorized role for KitchenActivity: " + userRole);
            Toast.makeText(this, "Unauthorized access. Chef or manager role required.", Toast.LENGTH_SHORT).show();

            // Launch the OrderActivity as a fallback
            try {
                startActivity(new Intent(this, OrderActivity.class));
            } catch (Exception e) {
                Log.e(TAG, "Failed to launch fallback activity", e);
            }

            finish();
            return;
        }

        // Initialize Firebase Database references
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        ordersRef = database.getReference("orders");
        orderItemsRef = database.getReference("orderItems");

        // Setup navigation panel
        setupNavigationPanel("Kitchen Orders");

        initializeViews();
        setupRecyclerView();
        loadActiveOrders();
    }

    private void initializeViews() {
        rvOrders = findViewById(R.id.rvOrders);
    }

    private void setupRecyclerView() {
        rvOrders.setLayoutManager(new LinearLayoutManager(this));
        orderAdapter = new OrderAdapter(orders, this::onOrderSelected);
        rvOrders.setAdapter(orderAdapter);
    }

    private void loadActiveOrders() {
        ordersRef.orderByChild("status").addValueEventListener(new ValueEventListener() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                orders.clear();
                for (DataSnapshot orderSnapshot : dataSnapshot.getChildren()) {
                    Order order = orderSnapshot.getValue(Order.class);
                    if (order != null && ("received".equals(order.getStatus()) ||
                            "preparing".equals(order.getStatus()))) {
                        order.setOrderId(Long.parseLong(Objects.requireNonNull(orderSnapshot.getKey())));
                        orders.add(order);
                    }
                }
                orderAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(KitchenActivity.this, "Failed to load orders: " +
                        databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void onOrderSelected(Order order) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Order #" + order.getOrderId() + " - Table " + order.getTableNumber());

        View view = LayoutInflater.from(this).inflate(R.layout.dialog_order_details, null);
        RecyclerView rvItems = view.findViewById(R.id.rvOrderItems);
        Button btnUpdateStatus = view.findViewById(R.id.btnUpdateStatus);

        rvItems.setLayoutManager(new LinearLayoutManager(this));
        List<OrderItem> items = new ArrayList<>();

        // Load order items from Firebase
        orderItemsRef.orderByChild("orderId").equalTo(order.getOrderId())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for (DataSnapshot itemSnapshot : dataSnapshot.getChildren()) {
                            OrderItem item = itemSnapshot.getValue(OrderItem.class);
                            if (item != null) {
                                items.add(item);
                            }
                        }
                        com.finedine.rms.OrderItemAdapter adapter = new com.finedine.rms.OrderItemAdapter(
                                items,
                                position -> { /* No deletion in kitchen view */ }
                        );
                        rvItems.setAdapter(adapter);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Toast.makeText(KitchenActivity.this, "Failed to load items: " +
                                databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });

        btnUpdateStatus.setOnClickListener(v -> updateOrderStatus(order));

        builder.setView(view);
        builder.setNegativeButton("Close", null);
        builder.show();
    }

    private void updateOrderStatus(Order order) {
        String[] statusOptions = {"Received", "Preparing", "Ready", "Served"};
        int currentStatusIndex = Arrays.asList(statusOptions).indexOf(capitalize(order.getStatus()));

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Update Order Status");
        builder.setSingleChoiceItems(statusOptions, currentStatusIndex,
                (dialog, which) -> {
                    String newStatus = statusOptions[which].toLowerCase();

                    // Update status in Firebase
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("status", newStatus);

                    ordersRef.child(String.valueOf(order.getOrderId())).updateChildren(updates)
                            .addOnSuccessListener(aVoid -> {
                                loadActiveOrders();
                                dialog.dismiss();
                                if ("ready".equals(newStatus)) {
                                    notifyWaiter(order);
                                }
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Update failed: " + e.getMessage(),
                                        Toast.LENGTH_SHORT).show();
                            });
                });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void notifyWaiter(Order order) {
        // Send notification to waiters through Firebase
        DatabaseReference notificationsRef = FirebaseDatabase.getInstance()
                .getReference("notifications");

        String notificationId = notificationsRef.push().getKey();
        Map<String, Object> notification = new HashMap<>();
        notification.put("orderId", order.getOrderId());
        notification.put("tableNumber", order.getTableNumber());
        notification.put("message", "Order #" + order.getOrderId() + " is ready");
        notification.put("timestamp", System.currentTimeMillis());
        notification.put("read", false);

        assert notificationId != null;
        notificationsRef.child(notificationId).setValue(notification)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this,
                            "Waiter notified that order #" + order.getOrderId() + " is ready",
                            Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this,
                            "Failed to notify waiter: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private String capitalize(String str) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            if (str == null || str.isEmpty()) {
                return str;
            }
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}