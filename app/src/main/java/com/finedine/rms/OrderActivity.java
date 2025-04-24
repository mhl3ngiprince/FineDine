package com.finedine.rms;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class OrderActivity extends AppCompatActivity {
    private com.finedine.rms.MenuAdapter menuAdapter;
    private OrderItemAdapter orderItemAdapter;
    private final List<MenuItem> menuItems = new ArrayList<>();
    private final List<OrderItem> currentOrderItems = new ArrayList<>();
    private TextInputEditText etTableNumber;
    private TextInputEditText etCustomerName;
    private TextInputEditText etCustomerPhone;
    private int currentTableNumber = 0; // Will be set from user input
    private final int waiterId = 1; // Should come from session

    public interface OnItemClickListener {
        void onItemClick(MenuItem item);
    }

    @SuppressLint("RestrictedApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order);

        // Initialize customer info fields
        etTableNumber = findViewById(R.id.etTableNumber);
        etCustomerName = findViewById(R.id.etCustomerName);
        etCustomerPhone = findViewById(R.id.etCustomerPhone);

        // Initialize Menu RecyclerView
        RecyclerView menuRecyclerView = findViewById(R.id.menuRecyclerView);
        menuRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Fix: Create your custom adapter, not the framework MenuAdapter
        menuAdapter = new com.finedine.rms.MenuAdapter(menuItems, item -> onItemSelected(item));
        menuRecyclerView.setAdapter(menuAdapter);

        // Initialize Current Order RecyclerView
        RecyclerView currentOrderRecyclerView = findViewById(R.id.currentOrderRecyclerView);
        currentOrderRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Create order item adapter with delete functionality
        orderItemAdapter = new OrderItemAdapter(currentOrderItems, this::removeOrderItem);
        currentOrderRecyclerView.setAdapter(orderItemAdapter);

        loadMenuItems();
    }

    private void loadMenuItems() {
        new Thread(() -> {
            List<MenuItem> items = AppDatabase.getDatabase(this).menuItemDao().getAllAvailable();

            // If no items found, add sample menu items to database
            if (items.isEmpty()) {
                MenuItemDao dao = AppDatabase.getDatabase(this).menuItemDao();
                MenuItem[] sampleItems = MenuItem.premiumMenu();
                dao.insertAll(sampleItems);
                items = dao.getAllAvailable();
            }

            final List<MenuItem> finalItems = items;
            runOnUiThread(() -> {
                menuItems.clear();
                menuItems.addAll(finalItems);
                menuAdapter.notifyDataSetChanged();
            });
        }).start();
    }

    private void onItemSelected(MenuItem item) {
        // Show quantity dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add " + item.name);

        View view = LayoutInflater.from(this).inflate(R.layout.dialog_quantity, null);
        TextInputEditText quantityInput = view.findViewById(R.id.quantityInput);
        TextInputEditText notesInput = view.findViewById(R.id.notesInput);

        builder.setView(view);
        builder.setPositiveButton("Add", (dialog, which) -> {
            int quantity = Integer.parseInt(quantityInput.getText().toString());
            String notes = notesInput.getText().toString();
            addToOrder(item, quantity, notes);
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void addToOrder(MenuItem item, int quantity, String notes) {
        // Create a new OrderItem and add it to the current order items list
        OrderItem orderItem = new OrderItem();
        orderItem.setName(item.name);
        orderItem.setQuantity(quantity);

        // Add to current order
        currentOrderItems.add(orderItem);

        // Update the adapter
        orderItemAdapter.notifyItemInserted(currentOrderItems.size() - 1);

        // Show confirmation toast
        Toast.makeText(this, quantity + "x " + item.name + " added to order", Toast.LENGTH_SHORT).show();
    }

    private void removeOrderItem(int position) {
        if (position >= 0 && position < currentOrderItems.size()) {
            // Get the item for a more informative message
            OrderItem item = currentOrderItems.get(position);

            // Remove the item
            currentOrderItems.remove(position);

            // Update the adapter
            orderItemAdapter.notifyItemRemoved(position);

            // Show confirmation toast
            Toast.makeText(this, item.getName() + " removed from order", Toast.LENGTH_SHORT).show();
        }
    }

    public void submitOrder(View view) {
        if (currentOrderItems.isEmpty()) {
            Toast.makeText(this, "Add items to order first", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate table number and customer info
        String tableNumberStr = etTableNumber.getText().toString().trim();
        String customerName = etCustomerName.getText().toString().trim();
        String customerPhone = etCustomerPhone.getText().toString().trim();

        if (tableNumberStr.isEmpty()) {
            etTableNumber.setError("Table number is required");
            etTableNumber.requestFocus();
            return;
        }

        if (customerName.isEmpty()) {
            etCustomerName.setError("Customer name is required");
            etCustomerName.requestFocus();
            return;
        }

        // Convert table number to int
        try {
            currentTableNumber = Integer.parseInt(tableNumberStr);
        } catch (NumberFormatException e) {
            etTableNumber.setError("Invalid table number");
            etTableNumber.requestFocus();
            return;
        }

        new Thread(() -> {
            AppDatabase db = AppDatabase.getDatabase(this);

            // Create order
            Order order = new Order();
            order.setTableNumber(currentTableNumber);
            order.waiterId = waiterId;
            order.setTimestamp(System.currentTimeMillis());
            order.setStatus("received");
            // Add customer information
            order.setCustomerName(customerName);
            order.setCustomerPhone(customerPhone);

            long orderId = db.orderDao().insert(order);

            // Add order items
            for (OrderItem item : currentOrderItems) {
                item.setOrderId(String.valueOf((int) orderId));
                db.orderItemDao().insert(item);
            }

            runOnUiThread(() -> {
                Toast.makeText(this, "Order #" + orderId + " submitted", Toast.LENGTH_SHORT).show();
                finish();
            });
        }).start();
    }
}