package com.finedine.rms;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.finedine.rms.MenuAdapter;
import com.finedine.rms.OrderItemAdapter;
import com.finedine.rms.utils.SharedPrefsManager;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
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
    private EditText etSearchMenu;
    private int currentTableNumber = 0; // Will be set from user input
    private String userRole = "waiter"; // Default role
    private SharedPrefsManager prefsManager;
    private ChipGroup categoryChipGroup;
    private TextView tvCartItemCount;
    private MaterialCardView btnViewCart;
    private ExtendedFloatingActionButton fabCart;
    private String currentCategory = "All"; // Track current category

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            // Initialize prefsManager
            prefsManager = new SharedPrefsManager(this);

            // Get user role from intent or shared preferences
            if (getIntent().hasExtra("user_role")) {
                userRole = getIntent().getStringExtra("user_role");
            } else if (prefsManager != null) {
                userRole = prefsManager.getUserRole();
            }

            // Set appropriate layout based on user role
            if ("customer".equalsIgnoreCase(userRole)) {
                setContentView(R.layout.activity_customer_menu);
                initializeCustomerView();
            } else {
                setContentView(R.layout.activity_order);
                initializeStaffView();
            }

        } catch (Exception e) {
            Log.e(TAG, "Error initializing OrderActivity", e);
            Toast.makeText(this, "Error initializing order screen. Please try again.", Toast.LENGTH_LONG).show();

            // Fall back to regular view on error
            setContentView(R.layout.activity_order);
            initializeStaffView();
        }
    }

    /**
     * Initialize the full screen menu view for customers
     */
    private void initializeCustomerView() {
        try {
            // Setup navigation panel
            setupNavigationPanel("Menu");

            // Show welcome message for customers
            Toast.makeText(this, "Welcome to Fine Dine! Browse our menu and place your order.", Toast.LENGTH_LONG).show();

            // Initialize search
            etSearchMenu = findViewById(R.id.etSearchMenu);

            // Initialize category chips
            categoryChipGroup = findViewById(R.id.categoryChipGroup);
            if (categoryChipGroup != null) {
                categoryChipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
                    if (checkedIds.isEmpty()) {
                        // If nothing is selected, select "All" again
                        Chip allChip = findViewById(R.id.chipAll);
                        if (allChip != null) {
                            allChip.setChecked(true);
                        }
                        filterMenuByCategory("All");
                    } else {
                        Chip selectedChip = findViewById(checkedIds.get(0));
                        if (selectedChip != null) {
                            String category = selectedChip.getText().toString();
                            currentCategory = category;
                            filterMenuByCategory(category);
                        }
                    }
                });

                // Setup individual category chips
                setupCategoryChip(R.id.chipAll);
                setupCategoryChip(R.id.chipStarters);
                setupCategoryChip(R.id.chipMain);
                setupCategoryChip(R.id.chipDesserts);
                setupCategoryChip(R.id.chipBeverages);
            }

            // Initialize cart button and badge
            btnViewCart = findViewById(R.id.btnViewCart);
            tvCartItemCount = findViewById(R.id.tvCartItemCount);
            fabCart = findViewById(R.id.fabCart);

            if (btnViewCart != null) {
                btnViewCart.setOnClickListener(v -> showCart());
            }

            if (fabCart != null) {
                fabCart.setOnClickListener(v -> showCart());
            }

            updateCartBadge();

            // Initialize Menu RecyclerView with 2 columns
            RecyclerView menuRecyclerView = findViewById(R.id.menuRecyclerView);
            // Use more columns for grid display in landscape mode
            int columnCount = getResources().getConfiguration().orientation ==
                    android.content.res.Configuration.ORIENTATION_LANDSCAPE ? 3 : 2;
            menuRecyclerView.setLayoutManager(new GridLayoutManager(this, columnCount));

            // Create menu adapter
            menuAdapter = new MenuAdapter(menuItems, this::onItemSelected);
            menuRecyclerView.setAdapter(menuAdapter);

            // Load menu items safely
            loadMenuItems();

            // Initialize search functionality
            setupSearchFunctionality();
        } catch (Exception e) {
            Log.e(TAG, "Error initializing customer view", e);
            Toast.makeText(this, "Error initializing menu screen", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Initialize the regular staff order view
     */
    private void initializeStaffView() {
        try {
            // Setup navigation panel
            setupNavigationPanel("Order Management");

            // Initialize customer info fields
            etTableNumber = findViewById(R.id.etTableNumber);
            etCustomerName = findViewById(R.id.etCustomerName);
            etCustomerPhone = findViewById(R.id.etCustomerPhone);
            etSearchMenu = findViewById(R.id.etSearchMenu);

            // Adjust UI based on user role
            TextView activityTitle = findViewById(R.id.activityTitle);
            if (activityTitle != null) {
                activityTitle.setText("CREATE ORDER");
            }

            // Initialize Menu RecyclerView
            RecyclerView menuRecyclerView = findViewById(R.id.menuRecyclerView);
            // Use GridLayoutManager with 2 columns for grid display
            menuRecyclerView.setLayoutManager(new GridLayoutManager(this, 2));

            // Create menu adapter
            menuAdapter = new MenuAdapter(menuItems, this::onItemSelected);
            menuRecyclerView.setAdapter(menuAdapter);

            // Initialize Current Order RecyclerView
            RecyclerView currentOrderRecyclerView = findViewById(R.id.currentOrderRecyclerView);
            currentOrderRecyclerView.setLayoutManager(new LinearLayoutManager(this));

            // Create order item adapter with delete functionality
            orderItemAdapter = new OrderItemAdapter(currentOrderItems, this::removeOrderItem);
            currentOrderRecyclerView.setAdapter(orderItemAdapter);

            // Load menu items safely
            loadMenuItems();

            // Initialize search functionality
            setupSearchFunctionality();
        } catch (Exception e) {
            Log.e(TAG, "Error initializing staff view", e);
        }
    }

    /**
     * Filter menu items by category
     */
    private void filterMenuByCategory(String category) {
        try {
            currentCategory = category;
            // Clear search box if category is changed
            if (etSearchMenu != null && !etSearchMenu.getText().toString().isEmpty()) {
                filterMenuByCategoryAndSearch(category, etSearchMenu.getText().toString());
                return;
            }

            if (category == null || category.isEmpty() || "All".equalsIgnoreCase(category)) {
                // Show all items
                new Thread(() -> {
                    try {
                        AppDatabase db = AppDatabase.getDatabase(this);
                        final List<MenuItem> allItems = db.menuItemDao().getAllAvailable();

                        runOnUiThread(() -> {
                            menuItems.clear();
                            menuItems.addAll(allItems);
                            menuAdapter.notifyDataSetChanged();
                        });
                    } catch (Exception e) {
                        Log.e(TAG, "Error loading all menu items", e);
                    }
                }).start();
                return;
            }

            // Filter by category
            new Thread(() -> {
                try {
                    AppDatabase db = AppDatabase.getDatabase(this);
                    final List<MenuItem> allItems = db.menuItemDao().getAllAvailable();
                    final List<MenuItem> filteredItems = new ArrayList<>();

                    for (MenuItem item : allItems) {
                        if (item.category != null && item.category.equalsIgnoreCase(category)) {
                            filteredItems.add(item);
                        }
                    }

                    runOnUiThread(() -> {
                        menuItems.clear();
                        menuItems.addAll(filteredItems);
                        menuAdapter.notifyDataSetChanged();
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Error filtering menu items by category", e);
                }
            }).start();
        } catch (Exception e) {
            Log.e(TAG, "Error in filterMenuByCategory", e);
        }
    }

    /**
     * Setup individual category chip click listeners
     */
    private void setupCategoryChip(int chipId) {
        Chip chip = findViewById(chipId);
        if (chip != null) {
            chip.setOnClickListener(v -> {
                categoryChipGroup.check(chipId);
                currentCategory = chip.getText().toString();
                filterMenuByCategory(currentCategory);
            });
        }
    }

    /**
     * Filter menu items by both category and search text
     */
    private void filterMenuByCategoryAndSearch(String category, String searchText) {
        try {
            new Thread(() -> {
                try {
                    AppDatabase db = AppDatabase.getDatabase(this);
                    final List<MenuItem> allItems = db.menuItemDao().getAllAvailable();
                    final List<MenuItem> filteredItems = new ArrayList<>();

                    String lowerSearchText = searchText.toLowerCase();
                    boolean isAllCategory = category == null || category.isEmpty() || "All".equalsIgnoreCase(category);

                    for (MenuItem item : allItems) {
                        // Check category filter
                        boolean matchesCategory = isAllCategory ||
                                (item.category != null && item.category.equalsIgnoreCase(category));

                        // Check search text
                        boolean matchesSearch =
                                (item.name != null && item.name.toLowerCase().contains(lowerSearchText)) ||
                                        (item.description != null && item.description.toLowerCase().contains(lowerSearchText)) ||
                                        (item.category != null && item.category.toLowerCase().contains(lowerSearchText));

                        // Add if it matches both filters
                        if (matchesCategory && matchesSearch) {
                            filteredItems.add(item);
                        }
                    }

                    runOnUiThread(() -> {
                        menuItems.clear();
                        menuItems.addAll(filteredItems);
                        menuAdapter.notifyDataSetChanged();
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Error filtering by category and search", e);
                }
            }).start();
        } catch (Exception e) {
            Log.e(TAG, "Error in filterMenuByCategoryAndSearch", e);
        }
    }

    /**
     * Show the cart dialog to review and place order
     */
    private void showCart() {
        try {
            if (currentOrderItems.isEmpty()) {
                Toast.makeText(this, "Your cart is empty", Toast.LENGTH_SHORT).show();
                return;
            }

            // Create dialog view
            View cartView = LayoutInflater.from(this).inflate(R.layout.dialog_cart, null);
            RecyclerView cartRecyclerView = cartView.findViewById(R.id.cartRecyclerView);
            TextView totalText = cartView.findViewById(R.id.tvTotalPrice);

            // Set up recycler view
            cartRecyclerView.setLayoutManager(new LinearLayoutManager(this));
            cartRecyclerView.setAdapter(orderItemAdapter);

            // Calculate total
            double total = 0;
            for (OrderItem item : currentOrderItems) {
                total += item.getPrice();
            }
            totalText.setText(String.format("Total: R%.2f", total));

            // Show dialog
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Your Order");
            builder.setView(cartView);
            builder.setPositiveButton("Place Order", (dialog, which) -> {
                // Collect customer information first before submitting
                collectCustomerInformation();
            });
            builder.setNegativeButton("Continue Shopping", null);
            builder.show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing cart", e);
        }
    }

    /**
     * Collect customer information for checkout
     */
    private void collectCustomerInformation() {
        try {
            View customerInfoView = LayoutInflater.from(this).inflate(R.layout.dialog_customer_info, null);
            TextInputEditText etName = customerInfoView.findViewById(R.id.etCustomerName);
            TextInputEditText etPhone = customerInfoView.findViewById(R.id.etCustomerPhone);
            TextInputEditText etTableNum = customerInfoView.findViewById(R.id.etTableNumber);

            // Pre-fill name from shared preferences if available
            if (prefsManager != null) {
                String userName = prefsManager.getUserName();
                if (userName != null && !userName.isEmpty()) {
                    etName.setText(userName);
                }
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Complete Your Order");
            builder.setView(customerInfoView);
            builder.setPositiveButton("Confirm", (dialog, which) -> {
                // Validate inputs
                String name = etName.getText().toString().trim();
                String phone = etPhone.getText().toString().trim();
                String tableNumStr = etTableNum.getText().toString().trim();

                if (name.isEmpty() || phone.isEmpty() || tableNumStr.isEmpty()) {
                    Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                    return;
                }

                try {
                    int tableNum = Integer.parseInt(tableNumStr);
                    currentTableNumber = tableNum;
                    processOrderSubmission(name, phone, calculateOrderTotal());
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "Please enter a valid table number", Toast.LENGTH_SHORT).show();
                }
            });
            builder.setNegativeButton("Cancel", null);
            builder.show();
        } catch (Exception e) {
            Log.e(TAG, "Error collecting customer information", e);
        }
    }

    /**
     * Calculate the total order amount
     */
    private double calculateOrderTotal() {
        double total = 0;
        for (OrderItem item : currentOrderItems) {
            total += item.getPrice();
        }
        return total;
    }

    /**
     * Update the cart badge count
     */
    private void updateCartBadge() {
        try {
            // Update cart count
            int itemCount = 0;
            for (OrderItem item : currentOrderItems) {
                itemCount += item.getQuantity();
            }

            if (tvCartItemCount != null) {
                tvCartItemCount.setText(String.valueOf(itemCount));
            }

            if (fabCart != null) {
                if (itemCount > 0) {
                    fabCart.setText("View Cart (" + itemCount + ")");
                } else {
                    fabCart.setText("View Cart");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating cart badge", e);
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

                    // If no items found, add all premium menu items to database
                    if (items.isEmpty()) {
                        try {
                            // Load all premium menu items
                            MenuItem[] premiumItems = MenuItem.premiumMenu();
                            items = new ArrayList<>();
                            for (MenuItem premiumItem : premiumItems) {
                                // Insert each item to database
                                db.menuItemDao().insert(premiumItem);
                                // Add to our list
                                items.add(premiumItem);
                            }
                            Log.i(TAG, "Added " + premiumItems.length + " premium menu items to database");
                        } catch (Exception e) {
                            Log.e(TAG, "Error adding premium menu items", e);
                            // Fallback to sample menu item if premium menu fails
                            items = new ArrayList<>();
                            MenuItem item = new MenuItem();
                            item.name = "Sample Item";
                            item.price = 10.99;
                            item.description = "Sample description";
                            item.category = "Main Course";
                            items.add(item);
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
            orderItem.setPrice(item.price * quantity);
            orderItem.setNotes(notes);

            // Add to current order
            currentOrderItems.add(orderItem);

            // Update the adapter
            orderItemAdapter.notifyItemInserted(currentOrderItems.size() - 1);

            // Calculate and display total
            updateOrderTotal();

            // Update cart badge for customer view
            updateCartBadge();

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

                // Update total
                updateOrderTotal();

                // Update cart badge for customer view
                updateCartBadge();

                // Show confirmation toast
                Toast.makeText(this, item.getName() + " removed from order", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error removing order item", e);
            Toast.makeText(this, "Error removing item", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateOrderTotal() {
        try {
            double total = 0;
            for (OrderItem item : currentOrderItems) {
                total += item.getPrice();
            }

            TextView totalText = findViewById(R.id.tvOrderTotal);
            if (totalText != null) {
                totalText.setText(String.format("Total: R%.2f", total));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating order total", e);
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

            // Calculate total order amount
            double orderTotal = 0;
            for (OrderItem item : currentOrderItems) {
                orderTotal += item.getPrice();
            }
            final double finalOrderTotal = orderTotal;

            // Show confirmation dialog
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Confirm Order");
            builder.setMessage(String.format("Submit order for table #%d?\nTotal: R%.2f",
                    currentTableNumber, finalOrderTotal));
            builder.setPositiveButton("Submit", (dialog, which) -> {
                processOrderSubmission(customerName, customerPhone, finalOrderTotal);
            });
            builder.setNegativeButton("Cancel", null);
            builder.show();
        } catch (Exception e) {
            Log.e(TAG, "Error in submitOrder", e);
            Toast.makeText(this, "Error submitting order", Toast.LENGTH_SHORT).show();
        }
    }

    private void processOrderSubmission(String customerName, String customerPhone, double orderTotal) {
        // Show progress dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Processing order...");
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
                order.setTotal(orderTotal);

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

                        // Show payment options dialog
                        showPaymentOptionsDialog(orderId, orderTotal);
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
    }

    private void showPaymentOptionsDialog(long orderId, double total) {
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Order #" + orderId + " Submitted");

            // In a real app, we'd use a proper dialog layout
            builder.setMessage(String.format("Order #%d Total: R%.2f\n\nHow would you like to pay?",
                    orderId, total));

            // Add payment buttons
            builder.setPositiveButton("Pay Now", (dialog, which) -> {
                processPayment(orderId, total);
            });

            builder.setNegativeButton("Pay Later", (dialog, which) -> {
                // Just show confirmation toast
                Toast.makeText(this, "Order #" + orderId + " placed successfully!", Toast.LENGTH_LONG).show();

                // Clear order items and reset form
                clearOrder();
            });

            builder.setCancelable(false);
            builder.show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing payment options", e);
            Toast.makeText(this, "Order submitted successfully!", Toast.LENGTH_SHORT).show();
            clearOrder();
        }
    }

    private void processPayment(long orderId, double total) {
        try {
            // Show payment success dialog
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Payment Complete");
            builder.setMessage("Thank you for your payment of R" + String.format("%.2f", total));
            builder.setPositiveButton("OK", (dialog, which) -> {
                // Clear order after successful payment
                clearOrder();
                Toast.makeText(this, "Payment successful! Order #" + orderId, Toast.LENGTH_SHORT).show();
            });
            builder.setCancelable(false);
            builder.show();
        } catch (Exception e) {
            Log.e(TAG, "Error processing payment", e);
            Toast.makeText(this, "Error processing payment", Toast.LENGTH_SHORT).show();
        }
    }

    private void clearOrder() {
        // Clear current order items
        currentOrderItems.clear();
        orderItemAdapter.notifyDataSetChanged();

        // Update total - add a default message if no tvOrderTotal view
        try {
            TextView totalText = findViewById(R.id.tvOrderTotal);
            if (totalText != null) {
                totalText.setText("Total: R0.00");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating order total", e);
        }

        // Clear input fields
        etTableNumber.setText("");
        etCustomerName.setText("");
        etCustomerPhone.setText("");

        // If we're in customer mode, refill name from prefs
        if ("customer".equalsIgnoreCase(userRole) && prefsManager != null) {
            String userName = prefsManager.getUserName();
            if (userName != null && !userName.isEmpty()) {
                etCustomerName.setText(userName);
            }
        }
    }

    // Add search functionality
    private void setupSearchFunctionality() {
        etSearchMenu.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Not used
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Filter menu items based on search text and current category
                String searchText = s.toString();
                filterMenuByCategoryAndSearch(currentCategory, searchText);
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Not used
            }
        });

        // Add action listener for search keyboard button
        etSearchMenu.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                // Hide keyboard
                android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager)
                        getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }
                // Filter with current search text
                filterMenuByCategoryAndSearch(currentCategory, etSearchMenu.getText().toString());
                return true;
            }
            return false;
        });
    }

    private void filterMenuItems(String searchText) {
        // Use the combined filter method instead
        filterMenuByCategoryAndSearch(currentCategory, searchText);
    }
}