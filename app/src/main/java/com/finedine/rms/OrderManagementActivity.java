package com.finedine.rms;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.finedine.rms.utils.FirebaseDatabaseSetup;

import java.util.ArrayList;
import java.util.List;

public class OrderManagementActivity extends AppCompatActivity {

    private static final String TAG = "OrderManagementActivity";
    private OrderAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_management);

        // 1. Enable Firebase Realtime Database
        FirebaseDatabaseSetup.enableFirebaseDatabase(this);

        // Setup UI components
        RecyclerView recyclerView = findViewById(R.id.ordersRecyclerView);
        // Initialize adapter with empty list and click listener
        adapter = new OrderAdapter(new ArrayList<>(), order -> {
            // Handle order click
            Toast.makeText(this, "Order " + order.getOrderId() + " selected", Toast.LENGTH_SHORT).show();
        });
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // 3. Observe real-time changes using LiveData
        FirebaseDatabaseSetup.setupOrderObservers(
                this,                          // Context
                this,                          // LifecycleOwner
                activeOrders -> {              // Active orders observer
                    adapter.updateData(activeOrders);
                },
                completedOrders -> {           // Completed orders observer
                    // Handle completed orders if needed
                },
                pendingOrders -> {             // Pending orders observer
                    // Handle pending orders if needed
                }
        );
    }

    // 2. Use static methods for common operations
    public void createNewOrder(View view) {
        Order order = new Order();
        order.setTableNumber(5);
        order.setStatus("pending");
        order.setCustomerName("Guest");

        // Create order in Firebase
        long orderId = FirebaseDatabaseSetup.createOrder(this, order);

        // Add an item to the order
        OrderItem item = new OrderItem();
        item.setName("Grilled Salmon");
        item.setQuantity(1);
        item.setPrice(15.99);

        FirebaseDatabaseSetup.addItemToOrder(this, orderId, item);

        Toast.makeText(this, "Order created with ID: " + orderId, Toast.LENGTH_SHORT).show();
    }

    public void markOrderAsActive(long orderId) {
        FirebaseDatabaseSetup.updateOrderStatus(this, orderId, "active");
    }

    public void completeOrder(long orderId) {
        FirebaseDatabaseSetup.updateOrderStatus(this, orderId, "completed");
    }

    public void deleteOrder(long orderId) {
        FirebaseDatabaseSetup.deleteOrder(this, orderId);
    }
}