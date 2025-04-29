package com.finedine.rms;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.finedine.rms.MenuAdapter;
import com.finedine.rms.OrderItemAdapter;
import com.finedine.rms.utils.SharedPrefsManager;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

public class OrderActivity extends BaseActivity {
    private static final String TAG = "OrderActivity";
    private MenuAdapter menuAdapter;
    private OrderItemAdapter orderItemAdapter;
    private final List<MenuItem> menuItems = new ArrayList<>();
    private final List<OrderItem> currentOrderItems = new ArrayList<>();
    private TextInputEditText etTableNumber;
    private TextInputEditText etCustomerName;
    private TextInputEditText etCustomerPhone;
    private int currentTableNumber = 0; // Will be set from user input
    private String userRole = "waiter"; // Default role
    private SharedPrefsManager prefsManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order);

        try {
            // Initialize prefsManager
            prefsManager = new SharedPrefsManager(this);

            // Get user role from intent or shared preferences
            if (getIntent().hasExtra("user_role")) {
                userRole = getIntent().getStringExtra("user_role");
            } else if (prefsManager != null) {
                userRole = prefsManager.getUserRole();
            }

            // Setup navigation panel
            setupNavigationPanel("Order Management");

            // Initialize customer info fields
            etTableNumber = findViewById(R.id.etTableNumber);
            etCustomerName = findViewById(R.id.etCustomerName);
            etCustomerPhone = findViewById(R.id.etCustomerPhone);

            // Initialize Menu RecyclerView
            RecyclerView menuRecyclerView = findViewById(R.id.menuRecyclerView);
            menuRecyclerView.setLayoutManager(new LinearLayoutManager(this));

            // Create menu adapter
            menuAdapter = new MenuAdapter(menuItems, item -> onItemSelected(item));
            menuRecyclerView.setAdapter(menuAdapter);

            // Initialize Current Order RecyclerView
            RecyclerView currentOrderRecyclerView = findViewById(R.id.currentOrderRecyclerView);
            currentOrderRecyclerView.setLayoutManager(new LinearLayoutManager(this));

            // Create order item adapter with delete functionality
            orderItemAdapter = new OrderItemAdapter(currentOrderItems, this::removeOrderItem);
            currentOrderRecyclerView.setAdapter(orderItemAdapter);

            // Load menu items safely
            loadMenuItems();
        } catch (Exception e) {
            Log.e(TAG, "Error initializing OrderActivity", e);
            Toast.makeText(this, "Error initializing order screen. Please try again.", Toast.LENGTH_LONG).show();
        }
    }

    private void loadMenuItems() {
        try {
            new Thread(() -> {
                try {
                    // Get AppDatabase instance safely
                    AppDatabase db = AppDatabase.getDatabase(OrderActivity.this);

                    // Use try-catch to handle potential exceptions
                    List<MenuItem> items;
                    try {
                        items = db.menuItemDao().getAllAvailable();
                    } catch (Exception e) {
                        Log.e(TAG, "Error loading menu items", e);
                        items = new ArrayList<>();
                    }

                    // If no items found, add sample menu items to database
                    if (items.isEmpty()) {
                        try {
                            // Skip this part since the dao doesn't support insertAll
                            items = new ArrayList<>();
                            MenuItem item = new MenuItem();
                            item.name = "Sample Item";
                            item.price = 10.99;
                            item.description = "Sample description";
                            item.category = "Main Course";
                            items.add(item);
                        } catch (Exception e) {
                            Log.e(TAG, "Error adding sample menu items", e);
                        }
                    }

                    // Make a final copy of items for the UI thread
                    final List<MenuItem> finalItems = new ArrayList<>(items);

                    // Update UI safely
                    runOnUiThread(() -> {
                        try {
                            menuItems.clear();
                            menuItems.addAll(finalItems);
                            menuAdapter.notifyDataSetChanged();
                        } catch (Exception e) {
                            Log.e(TAG, "Error updating menu UI", e);
                        }
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Error in loadMenuItems thread", e);
                }
            }).start();
        } catch (Exception e) {
            Log.e(TAG, "Error starting menu load thread", e);
            Toast.makeText(this, "Error loading menu items", Toast.LENGTH_SHORT).show();
        }
    }

    private void onItemSelected(MenuItem item) {
        try {
            // Show quantity dialog
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Add " + item.name);

            View view = LayoutInflater.from(this).inflate(R.layout.dialog_quantity, null);
            TextInputEditText quantityInput = view.findViewById(R.id.quantityInput);
            TextInputEditText notesInput = view.findViewById(R.id.notesInput);

            // Set default quantity
            quantityInput.setText("1");

            builder.setView(view);
            builder.setPositiveButton("Add", (dialog, which) -> {
                try {
                    // Safely parse quantity with error handling
                    int quantity = 1;
                    try {
                        String quantityText = quantityInput.getText().toString();
                        if (!quantityText.isEmpty()) {
                            quantity = Integer.parseInt(quantityText);
                        }
                    } catch (NumberFormatException e) {
                        Log.e(TAG, "Error parsing quantity", e);
                    }

                    String notes = notesInput.getText() != null ?
                            notesInput.getText().toString() : "";

                    addToOrder(item, quantity, notes);
                } catch (Exception e) {
                    Log.e(TAG, "Error adding item to order", e);
                    Toast.makeText(OrderActivity.this,
                            "Error adding item to order", Toast.LENGTH_SHORT).show();
                }
            });
            builder.setNegativeButton("Cancel", null);
            builder.show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing quantity dialog", e);
            Toast.makeText(this, "Error adding item", Toast.LENGTH_SHORT).show();
        }
    }

    private void addToOrder(MenuItem item, int quantity, String notes) {
        try {
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
        } catch (Exception e) {
            Log.e(TAG, "Error adding item to order", e);
            Toast.makeText(this, "Error adding item to order", Toast.LENGTH_SHORT).show();
        }
    }

    private void removeOrderItem(int position) {
        try {
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
        } catch (Exception e) {
            Log.e(TAG, "Error removing order item", e);
            Toast.makeText(this, "Error removing item", Toast.LENGTH_SHORT).show();
        }
    }

    public void submitOrder(View view) {
        try {
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

            // Show progress dialog
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("Submitting order...");
            builder.setCancelable(false);
            AlertDialog progressDialog = builder.create();
            progressDialog.show();

            new Thread(() -> {
                try {
                    AppDatabase db = AppDatabase.getDatabase(OrderActivity.this);

                    // Create order
                    Order order = new Order();
                    order.setTableNumber(currentTableNumber);

                    // Get user ID from SharedPreferences if available
                    int waiterId = 1; // Default ID
                    if (prefsManager != null) {
                        waiterId = prefsManager.getUserId();
                        if (waiterId <= 0) waiterId = 1;
                    }
                    order.waiterId = waiterId;

                    order.setTimestamp(System.currentTimeMillis());
                    order.setStatus("received");

                    // Add customer information
                    order.setCustomerName(customerName);
                    order.setCustomerPhone(customerPhone);

                    // Insert order and get ID
                    long orderId = db.orderDao().insert(order);

                    // Add order items, avoiding foreign key errors
                    for (OrderItem item : currentOrderItems) {
                        try {
                            item.setOrderId(String.valueOf((int) orderId));
                            db.orderItemDao().insert(item);
                        } catch (Exception e) {
                            Log.e(TAG, "Error inserting order item", e);
                        }
                    }

                    // Update UI on success
                    runOnUiThread(() -> {
                        try {
                            if (progressDialog.isShowing()) {
                                progressDialog.dismiss();
                            }
                            Toast.makeText(OrderActivity.this, "Order #" + orderId + " submitted", Toast.LENGTH_SHORT).show();

                            // Clear current order items
                            currentOrderItems.clear();
                            orderItemAdapter.notifyDataSetChanged();

                            // Clear input fields
                            etTableNumber.setText("");
                            etCustomerName.setText("");
                            etCustomerPhone.setText("");
                        } catch (Exception e) {
                            Log.e(TAG, "Error updating UI after order submission", e);
                        }
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Error submitting order", e);

                    // Update UI on error
                    runOnUiThread(() -> {
                        if (progressDialog.isShowing()) {
                            progressDialog.dismiss();
                        }
                        Toast.makeText(OrderActivity.this,
                                "Error submitting order: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                    });
                }
            }).start();
        } catch (Exception e) {
            Log.e(TAG, "Error in submitOrder", e);
            Toast.makeText(this, "Error submitting order", Toast.LENGTH_SHORT).show();
        }
    }
}