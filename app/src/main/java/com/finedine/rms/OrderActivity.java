package com.finedine.rms;

import android.annotation.SuppressLint;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.finedine.rms.MenuAdapter;
import com.finedine.rms.OrderItemAdapter;
import com.finedine.rms.utils.NotificationUtils;
import com.finedine.rms.utils.OrderSafetyWrapper;
import com.finedine.rms.utils.DatabaseEmergencyRepair;
import com.finedine.rms.utils.SharedPrefsManager;
import com.finedine.rms.utils.OrderFirebaseHelper;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

public class OrderActivity extends BaseActivity {
    private static final String TAG = "OrderActivity";
    private MenuAdapter menuAdapter;
    private OrderItemAdapter orderItemAdapter;
    private final List<MenuItem> menuItems = new ArrayList<>();
    private final List<OrderItem> currentOrderItems = new ArrayList<>();
    private TextInputEditText etTableNumber;
    private TextInputEditText etCustomerName;
    private TextInputEditText etCustomerPhone;
    private TextInputEditText etCustomerEmail;
    private EditText etSearchMenu;
    private int currentTableNumber = 0; // Will be set from user input
    private String userRole = "waiter"; // Default role
    private SharedPrefsManager prefsManager;
    private ChipGroup categoryChipGroup;
    private TextView tvOrderCount; // For displaying the count of items in order
    private Button btnViewCart; // Changed from MaterialCardView to Button
    private Button btnSubmitOrder; // Added correct button reference
    private String currentCategory = "All"; // Track current category
    private AppDatabase appDatabase;
    private ExecutorService databaseExecutor;
    private DatabaseReference ordersRef; // Firebase database reference
    private OrderSafetyWrapper orderSafetyWrapper; // Added safety wrapper
    private OrderFirebaseHelper orderFirebaseHelper; // Added Firebase helper

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            // Setup modern navigation with proper title
            setupModernNavigationPanel("Orders", R.layout.activity_order);

            // Initialize manual navigation controls to ensure everything works
            setupNavigationManually();

            // Initialize prefsManager
            prefsManager = new SharedPrefsManager(this);

            // Get user role from intent or shared preferences
            if (getIntent().hasExtra("user_role")) {
                userRole = getIntent().getStringExtra("user_role");
            } else if (prefsManager != null) {
                userRole = prefsManager.getUserRole();
            }

            // Initialize database and executor
            appDatabase = AppDatabase.getDatabase(this);
            databaseExecutor = java.util.concurrent.Executors.newSingleThreadExecutor();
            Log.d(TAG, "Modern navigation setup completed");

            // Initialize our new OrderSafetyWrapper
            orderSafetyWrapper = new OrderSafetyWrapper(this, appDatabase, databaseExecutor);
            Log.d(TAG, "OrderSafetyWrapper initialized");

            // Run emergency database repair on startup
            new Thread(() -> {
                try {
                    Log.d(TAG, "Running database emergency repair on startup");
                    boolean repairResult = DatabaseEmergencyRepair.runFullDatabaseRepair(this);
                    Log.d(TAG, "Database emergency repair completed with result: " + repairResult);
                } catch (Exception e) {
                    Log.e(TAG, "Error during startup database repair: " + e.getMessage(), e);
                }
            }).start();

            // Initialize Firebase if available
            try {
                FirebaseDatabase database = FirebaseDatabase.getInstance();
                ordersRef = database.getReference("orders");
                orderFirebaseHelper = new OrderFirebaseHelper(this);
                Log.d(TAG, "Firebase initialized successfully");
            } catch (Exception e) {
                Log.w(TAG, "Firebase initialization failed, will use local database only: " + e.getMessage());
                ordersRef = null;
                orderFirebaseHelper = null;
            }

            // Set appropriate layout based on user role
            if ("customer".equalsIgnoreCase(userRole)) {
                initializeCustomerView();
            } else {
                initializeStaffView();
            }

        } catch (Exception e) {
            Log.e(TAG, "Error setting up modern navigation", e);
            Toast.makeText(this, "Error initializing navigation", Toast.LENGTH_SHORT).show();

            // Initialize appropriate view based on role
            if ("customer".equalsIgnoreCase(userRole)) {
                initializeCustomerView();
            } else {
                initializeStaffView();
            }
        }
    }

    /**
     * Initialize the full screen menu view for customers
     */
    private void initializeCustomerView() {
        try {
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

                // Make sure All is initially selected
                Chip allChip = findViewById(R.id.chipAll);
                if (allChip != null) {
                    allChip.setChecked(true);
                }
            }

            // Initialize cart button and badge
            btnViewCart = findViewById(R.id.btnViewCart);
            tvOrderCount = findViewById(R.id.tvOrderCount);

            // Initialize orderItemAdapter to handle cart items
            orderItemAdapter = new OrderItemAdapter(currentOrderItems, this::removeOrderItem);

            // Submit button initialization
            btnSubmitOrder = findViewById(R.id.btnSubmitOrder);
            if (btnSubmitOrder != null) {
                Log.d(TAG, "btnSubmitOrder found, setting click listener");
                btnSubmitOrder.setOnClickListener(v -> {
                    Log.d(TAG, "btnSubmitOrder clicked, calling submitOrder()");
                    submitOrder(v);
                });
            }

            // No FAB in this layout - removed fabOrder related code

            if (btnViewCart != null) {
                Log.d(TAG, "btnViewCart found, setting click listener");
                btnViewCart.setOnClickListener(v -> {
                    Log.d(TAG, "btnViewCart clicked, calling showCart()");
                    showCart();
                });
            } else {
                Log.e(TAG, "btnViewCart not found in layout");
            }

            updateCartBadge();

            // Initialize Menu RecyclerView with 2 columns
            RecyclerView menuRecyclerView = findViewById(R.id.menuRecyclerView);
            if (menuRecyclerView == null) {
                Log.e(TAG, "menuRecyclerView is null, cannot display menu items");
                return;
            }

            // Use more columns for grid display in landscape mode
            int columnCount = getResources().getConfiguration().orientation ==
                    android.content.res.Configuration.ORIENTATION_LANDSCAPE ? 3 : 2;
            GridLayoutManager gridLayoutManager = new GridLayoutManager(this, columnCount);
            menuRecyclerView.setLayoutManager(gridLayoutManager);
            // Add item decoration for spacing
            int spacingInPixels = getResources().getDimensionPixelSize(R.dimen.grid_spacing);
            menuRecyclerView.addItemDecoration(new GridSpacingItemDecoration(columnCount, spacingInPixels, true));

            // Create menu items array directly for faster loading
            List<MenuItem> initialItems = new ArrayList<>(Arrays.asList(MenuItem.premiumMenu()));

            // Create menu adapter with direct loading of images for customer view
            menuItems.clear();
            menuItems.addAll(initialItems);
            menuAdapter = new MenuAdapter(menuItems, this::onItemSelected);
            menuRecyclerView.setAdapter(menuAdapter);
            Log.d(TAG, "Initial menu items loaded for customer: " + initialItems.size());

            // Long press on search box to enable menu reset option
            if (etSearchMenu != null) {
                etSearchMenu.setOnLongClickListener(v -> {
                    showMenuResetOption();
                    return true;
                });
            }

            // Load menu items safely
            loadMenuItems();

            // Initialize search functionality
            setupSearchFunctionality();

            // Initialize reset menu button for customer view
            Button btnResetMenu = findViewById(R.id.btnResetMenu);
            if (btnResetMenu != null) {
                btnResetMenu.setOnClickListener(v -> resetAllMenuItems());
            }

            // Force display menu items directly - don't wait for database
            // This ensures items always appear for customers
            loadMenuItemsDirectFromClass();
        } catch (Exception e) {
            Log.e(TAG, "Error initializing customer view", e);
            Toast.makeText(this, "Error initializing menu screen", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Initialize the regular staff order view
     */
    private void setupNavigationManually() {
        // Use helper methods from BaseActivity to ensure navigation works
        ensureMenuButtonWorks();
        ensureBottomNavigationWorks(R.id.navigation_orders);

        // Add menu option for creating sample data
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null && userRole != null &&
                (userRole.equalsIgnoreCase("admin") || userRole.equalsIgnoreCase("manager"))) {
            toolbar.setOnMenuItemClickListener(item -> {
                int id = item.getItemId();
                if (id == R.id.action_sample_data) {
                    // Load sample order data
                    com.finedine.rms.utils.SampleDataHelper.loadSampleOrders(this);
                    Toast.makeText(this, "Creating sample orders...", Toast.LENGTH_SHORT).show();

                    // Refresh UI after a delay
                    new android.os.Handler().postDelayed(() -> {
                        // In a real app, this would reload the orders list
                        Toast.makeText(this, "Sample orders created! Please check orders list.",
                                Toast.LENGTH_LONG).show();
                    }, 3000);
                    return true;
                }
                return false;
            });

            // Inflate menu
            toolbar.inflateMenu(R.menu.order_menu);
        }
    }

    private void initializeStaffView() {
        try {
            // Initialize customer info fields
            etTableNumber = findViewById(R.id.etTableNumber);
            etCustomerName = findViewById(R.id.etCustomerName);
            etCustomerPhone = findViewById(R.id.etCustomerPhone);
            etCustomerEmail = findViewById(R.id.etCustomerEmail);
            etSearchMenu = findViewById(R.id.etSearchMenu);

            // Initialize reset menu button
            Button btnResetMenu = findViewById(R.id.btnResetMenu);
            if (btnResetMenu != null) {
                btnResetMenu.setOnClickListener(v -> {
                    resetAllMenuItems();
                });
            }

            if (btnViewCart != null) {
                btnViewCart.setOnClickListener(v -> showCart());
            }

            // Set button click listener for Submit Order
            btnSubmitOrder = findViewById(R.id.btnSubmitOrder);
            if (btnSubmitOrder != null) {
                btnSubmitOrder.setOnClickListener(v -> submitOrder(v));
            }

            // Adjust UI based on user role
            TextView activityTitle = findViewById(R.id.activityTitle);
            if (activityTitle != null) {
                activityTitle.setText("CREATE ORDER");
            }

            // Initialize Menu RecyclerView
            RecyclerView menuRecyclerView = findViewById(R.id.menuRecyclerView);
            // Use GridLayoutManager with 2 columns for grid display
            int columnCount = getResources().getConfiguration().orientation ==
                    android.content.res.Configuration.ORIENTATION_LANDSCAPE ? 3 : 2;
            GridLayoutManager gridLayoutManager = new GridLayoutManager(this, columnCount);
            menuRecyclerView.setLayoutManager(gridLayoutManager);
            // Add item decoration for spacing
            int spacingInPixels = getResources().getDimensionPixelSize(R.dimen.grid_spacing);
            menuRecyclerView.addItemDecoration(new GridSpacingItemDecoration(columnCount, spacingInPixels, true));

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
            Log.d(TAG, "showCart() called, items in cart: " + currentOrderItems.size());

            if (currentOrderItems.isEmpty()) {
                Toast.makeText(this, "Your cart is empty", Toast.LENGTH_SHORT).show();
                return;
            }

            // Create dialog view
            View cartView = LayoutInflater.from(this).inflate(R.layout.dialog_cart, null);
            RecyclerView cartRecyclerView = cartView.findViewById(R.id.cartRecyclerView);
            TextView totalText = cartView.findViewById(R.id.tvTotalPrice);
            TextView subtotalText = cartView.findViewById(R.id.tvSubtotal);
            TextView serviceFeeText = cartView.findViewById(R.id.tvServiceFee);

            // Ensure we have an adapter
            if (orderItemAdapter == null) {
                orderItemAdapter = new OrderItemAdapter(currentOrderItems, this::removeOrderItem);
            }

            // Set up recycler view
            cartRecyclerView.setLayoutManager(new LinearLayoutManager(this));
            cartRecyclerView.setAdapter(orderItemAdapter);

            // Calculate subtotal
            double subtotal = 0;
            for (OrderItem item : currentOrderItems) {
                subtotal += item.getPrice();
            }

            // Calculate service fee (10%)
            double serviceFee = subtotal * 0.10;

            // Calculate total
            double total = subtotal + serviceFee;

            // Update text views
            if (subtotalText != null) {
                subtotalText.setText(String.format("R%.2f", subtotal));
            }

            if (serviceFeeText != null) {
                serviceFeeText.setText(String.format("R%.2f", serviceFee));
            }

            if (totalText != null) {
                totalText.setText(String.format("R%.2f", total));
            }

            // Show dialog
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Your Order");
            builder.setView(cartView);
            builder.setPositiveButton("Place Order", (dialog, which) -> {
                // Collect customer information first before submitting
                collectCustomerInformation();
            });
            builder.setNegativeButton("Continue Shopping", null);

            AlertDialog dialog = builder.create();
            if (dialog.getWindow() != null) {
                dialog.getWindow().getDecorView().setTag("cart_dialog");
            }
            dialog.show();

            Log.d(TAG, "Cart dialog displayed with " + currentOrderItems.size() + " items");
        } catch (Exception e) {
            Log.e(TAG, "Error showing cart", e);
            Toast.makeText(this, "Error showing cart: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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

            // Pre-fill existing information if we have it
            if (etCustomerName != null && etCustomerName.getText() != null && !etCustomerName.getText().toString().isEmpty()) {
                etName.setText(etCustomerName.getText().toString());
            }

            if (etCustomerPhone != null && etCustomerPhone.getText() != null && !etCustomerPhone.getText().toString().isEmpty()) {
                etPhone.setText(etCustomerPhone.getText().toString());
            }

            if (etTableNumber != null && etTableNumber.getText() != null && !etTableNumber.getText().toString().isEmpty()) {
                etTableNum.setText(etTableNumber.getText().toString());
            } else {
                // Default to table 1 if not specified
                etTableNum.setText("1");
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Complete Your Order");
            builder.setView(customerInfoView);

            // Use null for button to prevent automatic dismiss on error
            builder.setPositiveButton("Confirm", null);
            builder.setNegativeButton("Cancel", null);

            AlertDialog dialog = builder.create();
            dialog.show();

            // Override the positive button click listener
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                // Validate inputs
                String name = etName.getText().toString().trim();
                String phone = etPhone.getText().toString().trim();
                String tableNumStr = etTableNum.getText().toString().trim();

                if (name.isEmpty()) {
                    etName.setError("Please enter your name");
                    return;
                }

                if (tableNumStr.isEmpty()) {
                    etTableNum.setError("Please enter a table number");
                    return;
                }

                try {
                    int tableNum = Integer.parseInt(tableNumStr);
                    currentTableNumber = tableNum;

                    // Email is optional, create a default one if needed
                    String email = phone + "@customer.com";

                    dialog.dismiss(); // Dismiss dialog after valid input

                    // Create order progress dialog
                    AlertDialog progressDialog = new AlertDialog.Builder(OrderActivity.this)
                            .setTitle("Processing Order")
                            .setMessage("Please wait...")
                            .setCancelable(false)
                            .create();
                    progressDialog.show();

                    // Create order object
                    Order order = new Order();
                    order.setTableNumber(currentTableNumber);
                    order.setWaiterId(prefsManager != null ? prefsManager.getUserId() : 1);
                    order.setTimestamp(System.currentTimeMillis());
                    order.setStatus("pending");
                    order.setCustomerName(name);
                    order.setCustomerPhone(phone);
                    order.setCustomerEmail(email);
                    order.setTotal(calculateOrderTotal());
                    order.setExternalId("ORD_" + System.currentTimeMillis());

                    // Copy order items to avoid modification issues
                    final List<OrderItem> orderItemsCopy = new ArrayList<>(currentOrderItems);

                    // Try Firebase submission first if available
                    if (orderFirebaseHelper != null) {
                        orderFirebaseHelper.submitOrder(order, orderItemsCopy, new OrderFirebaseHelper.OrderSubmissionCallback() {
                            @Override
                            public void onSuccess(String firebaseKey, long orderId) {
                                // Close progress dialog
                                progressDialog.dismiss();

                                // Update order with Firebase-generated ID
                                order.setOrderId(orderId);

                                // Show success UI
                                notifyKitchenAboutNewOrder(orderId, currentTableNumber);
                                showPaymentOptionsDialog(orderId, calculateOrderTotal());
                            }

                            @Override
                            public void onFailure(Exception e) {
                                // Firebase failed, fall back to local database
                                Log.e(TAG, "Firebase order submission failed: " + e.getMessage());

                                // Fall back to local database (keep progress dialog showing)
                                orderSafetyWrapper.submitOrderSafely(order, orderItemsCopy, new OrderSafetyWrapper.OrderSubmitCallback() {
                                    @Override
                                    public void onSuccess(long orderId) {
                                        // Close progress dialog
                                        progressDialog.dismiss();

                                        // Show success UI
                                        notifyKitchenAboutNewOrder(orderId, currentTableNumber);
                                        showPaymentOptionsDialog(orderId, calculateOrderTotal());
                                    }

                                    @Override
                                    public void onPartialSuccess(long orderId, String message) {
                                        // Close progress dialog
                                        progressDialog.dismiss();

                                        // Show partial success UI
                                        Toast.makeText(OrderActivity.this, "Order saved with limitations: " + message, Toast.LENGTH_LONG).show();

                                        if (orderId > 0) {
                                            notifyKitchenAboutNewOrder(orderId, currentTableNumber);
                                            showPaymentOptionsDialog(orderId, calculateOrderTotal());
                                        }
                                    }

                                    @Override
                                    public void onFailure(String error) {
                                        // Close progress dialog
                                        progressDialog.dismiss();

                                        // Show error dialog with retry option
                                        showErrorDialog("Order Submission Failed",
                                                "Unable to process your order: " + error + "\n\nPlease try again.");
                                    }
                                });
                            }
                        });
                    } else {
                        // No Firebase, use local database directly
                        orderSafetyWrapper.submitOrderSafely(order, orderItemsCopy, new OrderSafetyWrapper.OrderSubmitCallback() {
                            @Override
                            public void onSuccess(long orderId) {
                                // Close progress dialog
                                progressDialog.dismiss();

                                // Show success UI
                                notifyKitchenAboutNewOrder(orderId, currentTableNumber);
                                showPaymentOptionsDialog(orderId, calculateOrderTotal());
                            }

                            @Override
                            public void onPartialSuccess(long orderId, String message) {
                                // Close progress dialog
                                progressDialog.dismiss();

                                // Show partial success UI
                                Toast.makeText(OrderActivity.this, "Order saved with limitations: " + message, Toast.LENGTH_LONG).show();

                                if (orderId > 0) {
                                    notifyKitchenAboutNewOrder(orderId, currentTableNumber);
                                    showPaymentOptionsDialog(orderId, calculateOrderTotal());
                                }
                            }

                            @Override
                            public void onFailure(String error) {
                                // Close progress dialog
                                progressDialog.dismiss();

                                // Show error dialog with retry option
                                showErrorDialog("Order Submission Failed",
                                        "Unable to process your order: " + error + "\n\nPlease try again.");
                            }
                        });
                    }
                } catch (NumberFormatException e) {
                    etTableNum.setError("Please enter a valid table number");
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error collecting customer information", e);
            Toast.makeText(this, "Error processing checkout: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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

            // Update cart count display
            if (tvOrderCount != null) {
                tvOrderCount.setText(String.valueOf(itemCount));
                Log.d(TAG, "Updated cart badge count: " + itemCount);
            }

            if (btnViewCart != null) {
                if (itemCount > 0) {
                    btnViewCart.setText("View Cart (" + itemCount + ")");
                } else {
                    btnViewCart.setText("View Cart");
                }
            }

            // Update text displays of item count
            TextView orderCountTextView = findViewById(R.id.tvOrderCount);
            if (orderCountTextView != null) {
                orderCountTextView.setText(itemCount + " items");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating cart badge", e);
        }
    }

    private void loadMenuItems() {
        try {
            // If user is customer, show a welcome message with loading indicator
            if ("customer".equalsIgnoreCase(userRole)) {
                Toast.makeText(this, "Welcome to Fine Dine! Loading our delicious menu...", Toast.LENGTH_SHORT).show();
            }

            // Get the premium menu items directly first for immediate display
            final MenuItem[] directItems = MenuItem.premiumMenu();
            if (directItems != null && directItems.length > 0) {
                runOnUiThread(() -> {
                    try {
                        menuItems.clear();
                        menuItems.addAll(Arrays.asList(directItems));
                        if (menuAdapter != null) {
                            menuAdapter.notifyDataSetChanged();
                            Log.d(TAG, "Successfully loaded " + directItems.length + " menu items directly");
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error in direct menu load: " + e.getMessage());
                    }
                });
            }

            new Thread(() -> {
                try {
                    // Get AppDatabase instance safely
                    AppDatabase db = AppDatabase.getDatabase(OrderActivity.this);

                    // FORCE RESET: Clear and reload all menu items every time
                    try {
                        // Clear existing menu items
                        db.menuItemDao().deleteAll();
                        Log.d(TAG, "Force cleared existing menu items");

                        // Add all premium menu items
                        MenuItem[] premiumItems = MenuItem.premiumMenu();
                        for (MenuItem premiumItem : premiumItems) {
                            // Insert each item to database
                            db.menuItemDao().insert(premiumItem);
                        }
                        Log.d(TAG, "Force added " + premiumItems.length + " menu items with images");

                        // Get the newly added items
                        List<MenuItem> items = db.menuItemDao().getAllAvailable();
                        if (items == null || items.isEmpty()) {
                            // If database failed to return items, use the direct array
                            items = new ArrayList<>(Arrays.asList(premiumItems));
                            Log.w(TAG, "Database returned empty list, using direct premium menu items");
                        }

                        // Make a final copy of items for the UI thread
                        final List<MenuItem> finalItems = new ArrayList<>(items);

                        // Update UI safely on main thread
                        runOnUiThread(() -> {
                            try {
                                menuItems.clear();
                                menuItems.addAll(finalItems);
                                if (menuAdapter != null) {
                                    menuAdapter.notifyDataSetChanged();
                                    Log.d(TAG, "Successfully displayed " + finalItems.size() + " menu items from database");

                                    // For customers, select "All" category to show all items
                                    if ("customer".equalsIgnoreCase(userRole)) {
                                        Chip allChip = findViewById(R.id.chipAll);
                                        if (allChip != null) {
                                            allChip.setChecked(true);
                                            currentCategory = "All";
                                        }
                                    }

                                } else {
                                    Log.e(TAG, "menuAdapter is null");
                                    // Create adapter if it's null
                                    menuAdapter = new MenuAdapter(menuItems, OrderActivity.this::onItemSelected);

                                    // Try to find the RecyclerView
                                    RecyclerView menuRecyclerView = findViewById(R.id.menuRecyclerView);
                                    if (menuRecyclerView != null) {
                                        menuRecyclerView.setAdapter(menuAdapter);
                                    }
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error updating menu UI", e);
                                // If UI update fails, try direct population
                                loadMenuItemsDirectFromClass();
                            }
                        });
                    } catch (Exception e) {
                        Log.e(TAG, "Error in force reset of menu items", e);
                        // Fallback to standard loading
                        loadMenuItemsDirectFromClass();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error in loadMenuItems thread", e);
                    loadMenuItemsDirectFromClass();
                }
            }).start();
        } catch (Exception e) {
            Log.e(TAG, "Error starting menu load thread", e);
            Toast.makeText(this, "Error loading menu items", Toast.LENGTH_SHORT).show();
            // Last-ditch attempt to load menus directly
            loadMenuItemsDirectFromClass();
        }
    }

    /**
     * Direct loading method that bypasses database to ensure menu items always appear
     */
    private void loadMenuItemsDirectFromClass() {
        try {
            // Load directly from class method
            final MenuItem[] premiumItems = MenuItem.premiumMenu();

            runOnUiThread(() -> {
                try {
                    menuItems.clear();
                    menuItems.addAll(Arrays.asList(premiumItems));
                    menuAdapter.notifyDataSetChanged();
                    Log.d(TAG, "Emergency direct load successful: " + premiumItems.length + " items");
                    Toast.makeText(this, "Menu loaded successfully", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Log.e(TAG, "Failed to update UI in direct load", e);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Fatal error in direct menu loading", e);
        }
    }

    private void onItemSelected(MenuItem item) {
        try {
            // Show quantity dialog with view details option
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(item.name);

            View view = LayoutInflater.from(this).inflate(R.layout.dialog_quantity, null);
            TextInputEditText quantityInput = view.findViewById(R.id.quantityInput);
            TextInputEditText notesInput = view.findViewById(R.id.notesInput);

            // Set default quantity
            quantityInput.setText("1");

            // Add View Details button
            builder.setView(view);
            builder.setPositiveButton("Add to Order", (dialog, which) -> {
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
            builder.setNeutralButton("View Details", (dialog, which) -> {
                // Launch the detail view
                try {
                    Log.d(TAG, "Viewing details for item: " + item.getName());
                    MenuItemDetailActivity.launch(this, item);
                } catch (Exception e) {
                    Log.e(TAG, "Error launching detail view", e);
                    Toast.makeText(this, "Error viewing details", Toast.LENGTH_SHORT).show();
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
            orderItem.setMenuItemId(Long.valueOf(item.getItem_id()));  // Set the menu item ID reference

            // Validate the item
            if (item.name == null || item.name.isEmpty()) {
                Log.w(TAG, "MenuItem has no name, using default");
                orderItem.setName("Unknown Item");
            }

            if (quantity <= 0) {
                Log.w(TAG, "MenuItem has invalid quantity, using default");
                orderItem.setQuantity(1);
                orderItem.setPrice(item.price);
            }

            if (item.getItem_id() <= 0) {
                Log.w(TAG, "MenuItem has invalid ID: " + item.getItem_id());
                // We'll still allow this item
            }

            // Add to current order
            currentOrderItems.add(orderItem);

            // Initialize orderItemAdapter if it's null
            if (orderItemAdapter == null) {
                orderItemAdapter = new OrderItemAdapter(currentOrderItems, this::removeOrderItem);
                RecyclerView currentOrderRecyclerView = findViewById(R.id.currentOrderRecyclerView);
                if (currentOrderRecyclerView != null) {
                    currentOrderRecyclerView.setAdapter(orderItemAdapter);
                    currentOrderRecyclerView.setLayoutManager(new LinearLayoutManager(this));
                }
            }

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
                orderItemAdapter.notifyDataSetChanged(); // Refresh the entire list to ensure correctness

                // Update total
                updateOrderTotal();

                // Update cart badge for customer view
                updateCartBadge();

                // Show confirmation toast
                Toast.makeText(this, item.getName() + " removed from order", Toast.LENGTH_SHORT).show();

                // If cart is empty, inform the user
                if (currentOrderItems.isEmpty()) {
                    Toast.makeText(this, "Your cart is now empty", Toast.LENGTH_SHORT).show();
                }
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

            // For customer views, show the cart dialog first to collect customer information
            if ("customer".equalsIgnoreCase(userRole)) {
                showCart();
                return;
            }

            // Initialize database and executor if needed
            if (appDatabase == null) {
                appDatabase = AppDatabase.getDatabase(this);
            }

            if (databaseExecutor == null) {
                databaseExecutor = java.util.concurrent.Executors.newSingleThreadExecutor();
            }

            // Initialize OrderSafetyWrapper if needed
            if (orderSafetyWrapper == null) {
                orderSafetyWrapper = new OrderSafetyWrapper(this, appDatabase, databaseExecutor);
            }

            // Validate table number and customer info
            String tableNumberStr = "";
            String customerName = "";
            String customerPhone = "";
            String customerEmail = "";

            // Safely get text from EditText fields with null checks
            if (etTableNumber != null && etTableNumber.getText() != null) {
                tableNumberStr = etTableNumber.getText().toString().trim();
            }

            if (etCustomerName != null && etCustomerName.getText() != null) {
                customerName = etCustomerName.getText().toString().trim();
            }

            if (etCustomerPhone != null && etCustomerPhone.getText() != null) {
                customerPhone = etCustomerPhone.getText().toString().trim();
            }

            if (etCustomerEmail != null && etCustomerEmail.getText() != null) {
                customerEmail = etCustomerEmail.getText().toString().trim();
            }

            if (tableNumberStr.isEmpty()) {
                if (etTableNumber != null) {
                    etTableNumber.setError("Table number is required");
                    etTableNumber.requestFocus();
                }
                Toast.makeText(this, "Please enter a table number", Toast.LENGTH_SHORT).show();
                return;
            }

            if (customerName.isEmpty()) {
                if (etCustomerName != null) {
                    etCustomerName.setError("Customer name is required");
                    etCustomerName.requestFocus();
                }
                Toast.makeText(this, "Please enter customer name", Toast.LENGTH_SHORT).show();
                return;
            }

            // Email is less critical - if empty, generate a placeholder
            if (customerEmail.isEmpty()) {
                // Generate a placeholder email based on customer name and timestamp
                customerEmail = customerName.toLowerCase().replace(" ", ".")
                        + "." + System.currentTimeMillis() + "@customer.com";
                Log.d(TAG, "Generated placeholder email: " + customerEmail);

                // Update the field if it exists
                if (etCustomerEmail != null) {
                    etCustomerEmail.setText(customerEmail);
                }
            }

            // Convert table number to int safely
            try {
                currentTableNumber = Integer.parseInt(tableNumberStr);
                if (currentTableNumber <= 0) {
                    throw new NumberFormatException("Table number must be positive");
                }
            } catch (NumberFormatException e) {
                if (etTableNumber != null) {
                    etTableNumber.setError("Invalid table number - must be a positive number");
                    etTableNumber.requestFocus();
                }
                Toast.makeText(this, "Please enter a valid table number", Toast.LENGTH_SHORT).show();
                return;
            }

            // Calculate total order amount
            double orderTotal = calculateOrderTotal();
            final double finalOrderTotal = orderTotal;
            final String finalCustomerName = customerName;
            final String finalCustomerPhone = customerPhone;
            final String finalCustomerEmail = customerEmail;

            // Show confirmation dialog
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Confirm Order");
            builder.setMessage(String.format("Submit order for table #%d?\nTotal: R%.2f",
                    currentTableNumber, finalOrderTotal));
            builder.setPositiveButton("Submit", (dialog, which) -> {
                // Create simple progress dialog
                AlertDialog progressDialog = new AlertDialog.Builder(this)
                        .setTitle("Processing Order")
                        .setMessage("Please wait...")
                        .setCancelable(false)
                        .create();
                progressDialog.show();

                // Create order object
                Order order = new Order();
                order.setTableNumber(currentTableNumber);
                order.setWaiterId(prefsManager != null ? prefsManager.getUserId() : 1);
                order.setTimestamp(System.currentTimeMillis());
                order.setStatus("pending");
                order.setCustomerName(finalCustomerName);
                order.setCustomerPhone(finalCustomerPhone);
                order.setCustomerEmail(finalCustomerEmail);
                order.setTotal(finalOrderTotal);
                order.setExternalId("ORD_" + System.currentTimeMillis());

                // Copy order items to avoid modification issues
                final List<OrderItem> orderItemsCopy = new ArrayList<>(currentOrderItems);

                // Try Firebase submission first if available
                if (orderFirebaseHelper != null) {
                    orderFirebaseHelper.submitOrder(order, orderItemsCopy, new OrderFirebaseHelper.OrderSubmissionCallback() {
                        @Override
                        public void onSuccess(String firebaseKey, long orderId) {
                            // Close progress dialog
                            progressDialog.dismiss();

                            // Update order with Firebase-generated ID
                            order.setOrderId(orderId);

                            // Show success UI
                            notifyKitchenAboutNewOrder(orderId, currentTableNumber);
                            showPaymentOptionsDialog(orderId, finalOrderTotal);
                        }

                        @Override
                        public void onFailure(Exception e) {
                            // Firebase failed, fall back to local database
                            Log.e(TAG, "Firebase order submission failed: " + e.getMessage());

                            // Fall back to local database (keep progress dialog showing)
                            orderSafetyWrapper.submitOrderSafely(order, orderItemsCopy, new OrderSafetyWrapper.OrderSubmitCallback() {
                                @Override
                                public void onSuccess(long orderId) {
                                    // Close progress dialog
                                    progressDialog.dismiss();

                                    // Show success UI
                                    notifyKitchenAboutNewOrder(orderId, currentTableNumber);
                                    showPaymentOptionsDialog(orderId, finalOrderTotal);
                                }

                                @Override
                                public void onPartialSuccess(long orderId, String message) {
                                    // Close progress dialog
                                    progressDialog.dismiss();

                                    // Show partial success UI
                                    Toast.makeText(OrderActivity.this, "Order saved with limitations: " + message, Toast.LENGTH_LONG).show();

                                    if (orderId > 0) {
                                        notifyKitchenAboutNewOrder(orderId, currentTableNumber);
                                        showPaymentOptionsDialog(orderId, finalOrderTotal);
                                    }
                                }

                                @Override
                                public void onFailure(String error) {
                                    // Close progress dialog
                                    progressDialog.dismiss();

                                    // Show error dialog with retry option
                                    showErrorDialog("Order Submission Failed",
                                            "Unable to process your order: " + error + "\n\nPlease try again.");
                                }
                            });
                        }
                    });
                } else {
                    // No Firebase, use local database directly
                    orderSafetyWrapper.submitOrderSafely(order, orderItemsCopy, new OrderSafetyWrapper.OrderSubmitCallback() {
                        @Override
                        public void onSuccess(long orderId) {
                            // Close progress dialog
                            progressDialog.dismiss();

                            // Show success UI
                            notifyKitchenAboutNewOrder(orderId, currentTableNumber);
                            showPaymentOptionsDialog(orderId, finalOrderTotal);
                        }

                        @Override
                        public void onPartialSuccess(long orderId, String message) {
                            // Close progress dialog
                            progressDialog.dismiss();

                            // Show partial success UI
                            Toast.makeText(OrderActivity.this, "Order saved with limitations: " + message, Toast.LENGTH_LONG).show();

                            if (orderId > 0) {
                                notifyKitchenAboutNewOrder(orderId, currentTableNumber);
                                showPaymentOptionsDialog(orderId, finalOrderTotal);
                            }
                        }

                        @Override
                        public void onFailure(String error) {
                            // Close progress dialog
                            progressDialog.dismiss();

                            // Show error dialog with retry option
                            showErrorDialog("Order Submission Failed",
                                    "Unable to process your order: " + error + "\n\nPlease try again.");
                        }
                    });
                }
            });
            builder.setNegativeButton("Cancel", null);
            builder.show();
        } catch (Exception e) {
            Log.e(TAG, "Error in submitOrder", e);

            // Show a comprehensive error message
            AlertDialog.Builder errorBuilder = new AlertDialog.Builder(this);
            errorBuilder.setTitle("Order Submission Error");
            errorBuilder.setMessage("There was a problem submitting your order: " + e.getMessage() +
                    "\n\nPlease try again or contact support.");
            errorBuilder.setPositiveButton("OK", null);
            errorBuilder.show();
        }
    }

    /**
     * Show error dialog with retry option
     */
    private void showErrorDialog(String title, String message) {
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(title)
                    .setMessage(message)
                    .setPositiveButton("Try Again", (dialog, which) -> {
                        // Do nothing, dialog will dismiss
                    })
                    .setNegativeButton("Contact Support", (dialog, which) -> {
                        // Show support contact dialog with email option
                        try {
                            // Create contact options dialog
                            AlertDialog.Builder supportBuilder = new AlertDialog.Builder(this);
                            supportBuilder.setTitle("Contact Support");
                            supportBuilder.setItems(new String[]{"Email", "Call", "Live Chat"}, (d, which2) -> {
                                switch (which2) {
                                    case 0: // Email
                                        try {
                                            Intent emailIntent = new Intent(Intent.ACTION_SEND);
                                            emailIntent.setType("message/rfc822");
                                            emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{"support@finedine.com"});
                                            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Support Request - Order App");
                                            startActivity(Intent.createChooser(emailIntent, "Send Email to Support"));
                                        } catch (Exception e) {
                                            Log.e(TAG, "Error sending email", e);
                                        }
                                        break;
                                    case 1: // Call
                                        try {
                                            Intent callIntent = new Intent(Intent.ACTION_DIAL);
                                            callIntent.setData(android.net.Uri.parse("tel:+1234567890"));
                                            startActivity(callIntent);
                                        } catch (Exception e) {
                                            Log.e(TAG, "Error initiating call", e);
                                        }
                                        break;
                                    case 2: // Live Chat
                                        // In a real app, this would open a chat interface
                                        break;
                                }
                            });
                            supportBuilder.show();
                        } catch (Exception ex) {
                            Log.e(TAG, "Error showing support dialog", ex);
                        }
                    })
                    .show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing error dialog", e);
            // Last resort fallback
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        }
    }

    private void processOrderSubmission(String customerName, String customerPhone, String customerEmail, double orderTotal) {
        // Show basic progress dialog that works with BaseActivity
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Processing Order");
        builder.setMessage("Please wait while we process your order...");
        builder.setCancelable(false);
        AlertDialog progressDialog = builder.create();
        progressDialog.show();

        // Set a timeout for the progress dialog
        final Handler handler = new Handler();
        handler.postDelayed(() -> {
            if (progressDialog != null && progressDialog.isShowing()) {
                try {
                    progressDialog.setMessage("Order submission taking longer than expected...\nStill working...");
                } catch (Exception e) {
                    Log.e(TAG, "Error updating progress dialog", e);
                }
            }
        }, 5000); // 5 second timeout

        try {
            // Run database emergency repair before proceeding
            try {
                Log.d(TAG, "Running database emergency repair before submission");
                DatabaseEmergencyRepair.runFullDatabaseRepair(this);
            } catch (Exception e) {
                Log.e(TAG, "Error during pre-submission database repair: " + e.getMessage(), e);
                // Continue anyway, our wrapper will handle it
            }

            // Initialize database and executor if needed
            if (appDatabase == null) {
                appDatabase = AppDatabase.getDatabase(this);
            }

            if (databaseExecutor == null) {
                databaseExecutor = java.util.concurrent.Executors.newSingleThreadExecutor();
            }

            // Create order object
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
            order.setStatus("pending");

            // Add customer information
            order.setCustomerName(customerName);
            order.setCustomerPhone(customerPhone);
            order.setCustomerEmail(customerEmail);
            order.setTotal(orderTotal);

            // Validate order data before submission
            if (!order.validate()) {
                if (progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
                Toast.makeText(this, "Invalid order data. Please check all fields.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Create a copy of the current order items to avoid modification issues
            final List<OrderItem> orderItemsCopy = new ArrayList<>(currentOrderItems);

            // Set a timeout handler in case the order submission takes too long
            final boolean[] submissionCompleted = {false};

            // Create a handler to dismiss the dialog if submission takes too long
            new android.os.Handler().postDelayed(() -> {
                if (!submissionCompleted[0] && progressDialog != null && progressDialog.isShowing()) {
                    try {
                        progressDialog.dismiss();
                        Toast.makeText(OrderActivity.this, "Order submission is taking longer than expected. Please wait...", Toast.LENGTH_LONG).show();
                    } catch (Exception e) {
                        Log.e(TAG, "Error dismissing timed out dialog", e);
                    }
                }
            }, 10000); // 10-second timeout

            // Create order ID for unified tracking across all destinations
            final String unifiedOrderId = "ORD_" + System.currentTimeMillis() + "_" + Math.abs(java.util.UUID.randomUUID().hashCode() % 1000);
            order.setExternalId(unifiedOrderId);

            // Use our new OrderSafetyWrapper to reliably process the order
            orderSafetyWrapper.submitOrderSafely(order, orderItemsCopy, new OrderSafetyWrapper.OrderSubmitCallback() {
                @Override
                public void onSuccess(long orderId) {
                    // Dismiss the progress dialog
                    if (progressDialog != null && progressDialog.isShowing()) {
                        try {
                            progressDialog.dismiss();
                        } catch (Exception e) {
                            Log.e(TAG, "Error dismissing progress dialog", e);
                        }
                    }

                    Log.d(TAG, "Order successfully submitted with ID: " + orderId);

                    // Notify kitchen
                    notifyKitchenAboutNewOrder(orderId, order.getTableNumber());

                    // Show payment options
                    showPaymentOptionsDialog(orderId, orderTotal);

                    // Submit to Firebase for backup if it's available
                    if (ordersRef != null) {
                        submitOrderToFirebase(order, orderItemsCopy, orderId);
                    }
                }

                @Override
                public void onPartialSuccess(long orderId, String message) {
                    // Dismiss the progress dialog
                    if (progressDialog != null && progressDialog.isShowing()) {
                        try {
                            progressDialog.dismiss();
                        } catch (Exception e) {
                            Log.e(TAG, "Error dismissing progress dialog", e);
                        }
                    }

                    Log.w(TAG, "Order partially successful: " + message);
                    Toast.makeText(OrderActivity.this,
                            "Order saved with limitations: " + message,
                            Toast.LENGTH_LONG).show();

                    // We still need to notify kitchen if we have an order ID
                    if (orderId > 0) {
                        notifyKitchenAboutNewOrder(orderId, order.getTableNumber());
                        showPaymentOptionsDialog(orderId, orderTotal);
                    } else {
                        // Show a dialog with limited functionality
                        AlertDialog.Builder builder = new AlertDialog.Builder(OrderActivity.this);
                        builder.setTitle("Order Partially Processed");
                        builder.setMessage("Your order has been saved locally but there were some issues.\n\n" +
                                message + "\n\nA manager will help process your order.");
                        builder.setPositiveButton("OK", (dialog, which) -> {
                            // Clear the order form
                            clearOrder();
                        });
                        builder.setCancelable(false);
                        builder.show();
                    }
                }

                @Override
                public void onFailure(String error) {
                    // Dismiss the progress dialog
                    if (progressDialog != null && progressDialog.isShowing()) {
                        try {
                            progressDialog.dismiss();
                        } catch (Exception e) {
                            Log.e(TAG, "Error dismissing progress dialog", e);
                        }
                    }

                    Log.e(TAG, "Order submission failed: " + error);

                    // Try one last emergency attempt
                    try {
                        // Create emergency local backup
                        boolean emergencySaved = storeOrderForLaterSync(order, orderItemsCopy, unifiedOrderId);

                        if (emergencySaved) {
                            // Show a dialog with partial success
                            AlertDialog.Builder builder = new AlertDialog.Builder(OrderActivity.this);
                            builder.setTitle("Order Processing Issue");
                            builder.setMessage("There was a problem processing your order, but we've saved it locally.\n\n" +
                                    "Please notify a manager to help complete your order.");
                            builder.setPositiveButton("OK", (dialog, which) -> {
                                // Clear the order form
                                clearOrder();
                            });
                            builder.setCancelable(false);
                            builder.show();
                        } else {
                            // Complete failure, show error dialog with retry option
                            showErrorDialog("Order Submission Failed",
                                    "Unable to process your order: " + error + "\n\nPlease try again or contact a manager.");
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Emergency save also failed: " + e.getMessage(), e);
                        showErrorDialog("Critical Order Error",
                                "Unable to process or save your order. Please contact a manager immediately.");
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error submitting order", e);

            // Update UI on error
            try {
                if (progressDialog != null && progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
            } catch (Exception dialogEx) {
                Log.e(TAG, "Error dismissing dialog", dialogEx);
            }

            // Show a detailed error dialog
            AlertDialog.Builder errorBuilder = new AlertDialog.Builder(this);
            errorBuilder.setTitle("Order Error");
            errorBuilder.setMessage("There was a problem with your order: " + e.getMessage() +
                    "\n\nPlease try again or contact support.");
            errorBuilder.setPositiveButton("OK", null);
            errorBuilder.show();
        }
    }

    private void notifyKitchenAboutNewOrder(long orderId, int tableNumber) {
        try {
            // Send local notification
            NotificationUtils notificationUtils = new NotificationUtils();
            notificationUtils.sendOrderNotification(this, (int) orderId, tableNumber);

            // Send notification to kitchen tablets via Firebase
            sendKitchenTabletNotification(orderId, tableNumber);

            // Send notification to kitchen printers if configured
            sendKitchenPrinterNotification(orderId, tableNumber);

            // Send SMS notification if configured
            sendKitchenSmsNotification(orderId, tableNumber);

            Log.d(TAG, "Kitchen notifications sent for order #" + orderId);
        } catch (Exception e) {
            Log.e(TAG, "Failed to send kitchen notification: " + e.getMessage());
        }
    }

    /**
     * Send a notification to kitchen tablets or systems
     */
    private void sendKitchenTabletNotification(long orderId, int tableNumber) {
        // Try to send notification via Firebase Cloud Messaging if available
        try {
            if (ordersRef != null) {
                Map<String, Object> notification = new HashMap<>();
                notification.put("type", "new_order");
                notification.put("orderId", orderId);
                notification.put("tableNumber", tableNumber);
                notification.put("timestamp", System.currentTimeMillis());

                // Add to kitchen_notifications collection
                DatabaseReference notificationsRef = FirebaseDatabase.getInstance()
                        .getReference("kitchen_notifications");
                notificationsRef.push().setValue(notification)
                        .addOnSuccessListener(aVoid -> {
                            Log.d(TAG, "Kitchen tablet notification sent successfully");
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Failed to send kitchen tablet notification", e);
                        });
            }
        } catch (Exception e) {
            Log.e(TAG, "Error sending kitchen tablet notification", e);
        }
    }

    /**
     * Submit order to Firebase database with enhanced reliability
     */
    private void submitOrderToFirebase(Order order, List<OrderItem> orderItems, long localOrderId) {
        if (ordersRef == null || orderFirebaseHelper == null) {
            Log.w(TAG, "Firebase not initialized, skipping Firebase order submission");
            return;
        }

        try {
            // Create a new tracker for this operation
            final OrderSubmissionTracker tracker = new OrderSubmissionTracker();

            // Set the local order ID if it's valid
            if (localOrderId > 0) {
                order.setOrderId(localOrderId);
            }

            // Use our helper to submit the order
            orderFirebaseHelper.submitOrder(order, orderItems, new OrderFirebaseHelper.OrderSubmissionCallback() {
                @Override
                public void onSuccess(String firebaseKey, long orderId) {
                    Log.d(TAG, "Order successfully submitted to Firebase with key: " + firebaseKey);

                    // Update tracker
                    tracker.markFirebaseSuccess();
                    tracker.checkAndProceedWithOrder(OrderActivity.this, null, orderId, order);
                }

                @Override
                public void onFailure(Exception e) {
                    Log.e(TAG, "Failed to submit order to Firebase: " + e.getMessage(), e);

                    // Store locally for later sync
                    storeOrderForLaterSync(order, orderItems, order.getExternalId());

                    // Update tracker
                    tracker.markFirebaseFailure();
                    tracker.checkAndProceedWithOrder(OrderActivity.this, null, -1, order);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error submitting order to Firebase", e);

            // Store for later sync
            storeOrderForLaterSync(order, orderItems, order.getExternalId());
        }
    }

    /**
     * Find all active OrderSubmissionTracker instances
     */
    private List<OrderSubmissionTracker> findTrackers() {
        List<OrderSubmissionTracker> trackers = new ArrayList<>();
        // This is a simplified approach - in a real app we'd use a more robust approach
        try {
            // For now, we'll just create a new tracker if needed
            trackers.add(new OrderSubmissionTracker());
        } catch (Exception e) {
            Log.e(TAG, "Error finding trackers", e);
        }
        return trackers;
    }

    /**
     * Get application version name
     */
    private String getAppVersion() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (Exception e) {
            Log.e(TAG, "Error getting app version", e);
            return "unknown";
        }
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
            // Launch the payment activity
            Intent paymentIntent = new Intent(this, PaymentActivity.class);
            paymentIntent.putExtra("order_id", orderId);
            paymentIntent.putExtra("amount", total);
            paymentIntent.putExtra("order_status", "pending");
            paymentIntent.putExtra("order_reference", "ORD-" + orderId);
            startActivity(paymentIntent);

            // Clear order after sending to payment
            clearOrder();
        } catch (Exception e) {
            Log.e(TAG, "Error processing payment", e);
            Toast.makeText(this, "Error processing payment", Toast.LENGTH_SHORT).show();

            // Show manual payment completion dialog as fallback
            showPaymentCompletionDialog(orderId, total);
        }
    }

    private void showPaymentCompletionDialog(long orderId, double total) {
        try {
            // Show payment success dialog
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Payment Complete");
            builder.setMessage("Thank you for your payment of R" + String.format("%.2f", total));
            builder.setPositiveButton("View Receipt", (dialog, which) -> {
                // Launch receipt activity
                Intent receiptIntent = new Intent(this, ReceiptActivity.class);
                receiptIntent.putExtra("order_id", orderId);
                startActivity(receiptIntent);

                // Clear order
                clearOrder();
            });
            builder.setNegativeButton("Done", (dialog, which) -> {
                // Clear order after successful payment
                clearOrder();
            });
            builder.setCancelable(false);
            builder.show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing payment completion dialog", e);
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
        etCustomerEmail.setText("");

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

    /**
     * Show dialog to reset all menu items
     */
    private void showMenuResetOption() {
        new AlertDialog.Builder(this)
                .setTitle("Restore Menu Items")
                .setMessage("Would you like to restore all menu items with their images?")
                .setPositiveButton("Restore All Items", (dialog, which) -> {
                    resetAllMenuItems();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Reset all menu items in the database
     */
    private void resetAllMenuItems() {
        // Show progress dialog
        AlertDialog progressDialog = new AlertDialog.Builder(this)
                .setMessage("Restoring menu items...")
                .setCancelable(false)
                .create();
        progressDialog.show();

        if ("customer".equalsIgnoreCase(userRole)) {
            try {
                // Directly load menu items from MenuItem class
                MenuItem[] premiumItems = MenuItem.premiumMenu();

                // Update UI on main thread
                runOnUiThread(() -> {
                    try {
                        // Dismiss progress dialog
                        progressDialog.dismiss();

                        // Update adapter with new items
                        menuItems.clear();
                        menuItems.addAll(Arrays.asList(premiumItems));
                        menuAdapter.notifyDataSetChanged();

                        // Show success message
                        Toast.makeText(this, "Successfully restored menu items!", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Log.e(TAG, "Error updating UI after menu reset", e);
                    }
                });
                return;
            } catch (Exception e) {
                Log.e(TAG, "Error in direct menu reset", e);
                // Fall through to database approach
            }
        }

        AppDatabase db = AppDatabase.getDatabase(this);
        new Thread(() -> {
            try {
                // Clear existing menu items
                db.menuItemDao().deleteAll();

                // Add all premium menu items
                MenuItem[] premiumItems = MenuItem.premiumMenu();
                for (MenuItem item : premiumItems) {
                    db.menuItemDao().insert(item);
                }

                // Reload menu items
                List<MenuItem> items = db.menuItemDao().getAllAvailable();

                // Update UI on main thread
                runOnUiThread(() -> {
                    try {
                        // Dismiss progress dialog
                        progressDialog.dismiss();

                        // Update adapter with new items
                        menuItems.clear();
                        menuItems.addAll(items);
                        menuAdapter.notifyDataSetChanged();

                        // Show success message
                        Toast.makeText(this, "Successfully restored " + items.size() + " menu items!",
                                Toast.LENGTH_LONG).show();
                    } catch (Exception e) {
                        Log.e(TAG, "Error updating UI after menu reset", e);
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "Error resetting menu items", e);
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Error restoring menu items: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    /**
     * Submit order to local database with retry mechanism
     */
    private void submitToLocalDatabase(Order order, List<OrderItem> orderItems,
                                       OrderSubmissionTracker tracker, AlertDialog progressDialog,
                                       String unifiedOrderId) {
        try {
            if (appDatabase == null) {
                Log.e(TAG, "Database is null, initializing");
                try {
                    appDatabase = AppDatabase.getDatabase(this);
                    if (appDatabase == null) {
                        throw new Exception("Could not initialize database");
                    }
                } catch (Exception dbEx) {
                    Log.e(TAG, "Critical error initializing database", dbEx);
                    tracker.markLocalDatabaseFailure();
                    Toast.makeText(this, "Error connecting to database. Please try again.", Toast.LENGTH_SHORT).show();

                    // Try Firebase as fallback immediately
                    if (ordersRef != null && !tracker.isFirebaseSubmitted()) {
                        submitOrderToFirebase(order, orderItems, -1);
                    }

                    // Continue with order processing using Firebase or cloud data if available
                    tracker.checkAndProceedWithOrder(OrderActivity.this, progressDialog, -1, order);
                    return;
                }
            }

            if (databaseExecutor == null) {
                Log.e(TAG, "Database executor is null, initializing");
                try {
                    databaseExecutor = java.util.concurrent.Executors.newSingleThreadExecutor();
                } catch (Exception execEx) {
                    Log.e(TAG, "Critical error initializing database executor", execEx);
                    tracker.markLocalDatabaseFailure();

                    // Try Firebase as fallback
                    if (ordersRef != null && !tracker.isFirebaseSubmitted()) {
                        submitOrderToFirebase(order, orderItems, -1);
                    }

                    // Continue with order processing using Firebase or cloud data if available
                    tracker.checkAndProceedWithOrder(OrderActivity.this, progressDialog, -1, order);
                    return;
                }
            }

            // Use OrderWrapper utility for database submission
            com.finedine.rms.utils.OrderWrapper orderWrapper =
                    new com.finedine.rms.utils.OrderWrapper(this, appDatabase, databaseExecutor);

            // Attach the unified order ID
            order.setExternalId(unifiedOrderId);

            // Try up to 3 times to submit the order
            final int[] retryCount = {0};
            final int MAX_RETRIES = 3;

            // Make sure all order data is valid before submission
            if (!validateOrderBeforeSubmission(order, orderItems)) {
                Log.e(TAG, "Order validation failed, cannot submit to database");
                tracker.markLocalDatabaseFailure();

                // Show error and continue with other submission methods
                runOnUiThread(() -> {
                    Toast.makeText(OrderActivity.this, "Order validation failed. Trying alternate methods...", Toast.LENGTH_SHORT).show();
                });

                // Try Firebase as fallback
                if (ordersRef != null && !tracker.isFirebaseSubmitted()) {
                    submitOrderToFirebase(order, orderItems, -1);
                }

                // Continue with order processing using Firebase or cloud data if available
                tracker.checkAndProceedWithOrder(this, progressDialog, -1, order);
                return;
            }

            orderWrapper.submitOrder(order, orderItems, new com.finedine.rms.utils.OrderWrapper.OrderSubmitCallback() {
                @Override
                public void onSuccess(long orderId) {
                    // Mark local database submission as successful
                    tracker.markLocalDatabaseSuccess(orderId);
                    Log.d(TAG, "Local database order submission successful with ID: " + orderId);

                    // Submit to Firebase if not already done
                    if (!tracker.isFirebaseSubmitted()) {
                        submitOrderToFirebase(order, orderItems, orderId);
                    }

                    // Continue with order processing if we haven't done so already
                    tracker.checkAndProceedWithOrder(OrderActivity.this, progressDialog, orderId, order);
                }

                @Override
                public void onFailure(String error) {
                    Log.e(TAG, "Local database order submission failed: " + error + ", retry: " + retryCount[0]);

                    if (retryCount[0] < MAX_RETRIES) {
                        retryCount[0]++;
                        // Exponential backoff for retries
                        int delayMillis = 1000 * retryCount[0];

                        new android.os.Handler().postDelayed(() -> {
                            // Retry submission
                            Log.d(TAG, "Retrying local database submission, attempt " + retryCount[0]);
                            orderWrapper.submitOrder(order, orderItems, this);
                        }, delayMillis);
                    } else {
                        // Mark local database submission as failed after max retries
                        tracker.markLocalDatabaseFailure();

                        // Try Firebase as fallback if local database failed
                        if (ordersRef != null && !tracker.isFirebaseSubmitted()) {
                            submitOrderToFirebase(order, orderItems, -1);
                        }

                        // Continue with order processing using Firebase or cloud data if available
                        tracker.checkAndProceedWithOrder(OrderActivity.this, progressDialog, -1, order);
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Critical error in submitToLocalDatabase", e);
            tracker.markLocalDatabaseFailure();

            // Try Firebase as fallback
            if (ordersRef != null && !tracker.isFirebaseSubmitted()) {
                submitOrderToFirebase(order, orderItems, -1);
            }

            // Continue with order processing using Firebase or cloud data if available
            tracker.checkAndProceedWithOrder(this, progressDialog, -1, order);
        }
    }

    /**
     * Validates order data before submission
     */
    private boolean validateOrderBeforeSubmission(Order order, List<OrderItem> orderItems) {
        try {
            // Check essential order properties
            if (order == null) {
                Log.e(TAG, "Order object is null");
                return false;
            }

            // Make a deep copy of the order to avoid modifying the original
            // This prevents potential concurrent modification issues
            Order validatedOrder = new Order();
            // Copy order properties without using getId/setId which may not exist
            validatedOrder.setTableNumber(order.getTableNumber());
            validatedOrder.setCustomerName(order.getCustomerName());
            validatedOrder.setCustomerEmail(order.getCustomerEmail());
            validatedOrder.setCustomerPhone(order.getCustomerPhone());
            validatedOrder.setTimestamp(order.getTimestamp());
            validatedOrder.setStatus(order.getStatus());
            validatedOrder.setTotal(order.getTotal());
            validatedOrder.waiterId = order.waiterId;
            validatedOrder.setExternalId(order.getExternalId());

            // Validate table number
            if (validatedOrder.getTableNumber() <= 0) {
                Log.e(TAG, "Invalid table number: " + validatedOrder.getTableNumber());
                validatedOrder.setTableNumber(1); // Default to table 1
            }

            // Verify items list
            if (orderItems == null) {
                Log.e(TAG, "Order items list is null");
                return false;
            }

            if (orderItems.isEmpty()) {
                Log.e(TAG, "Order items list is empty");
                return false;
            }

            // Set default values for nullable fields
            if (validatedOrder.getStatus() == null) validatedOrder.setStatus("pending");
            if (validatedOrder.getTimestamp() == 0)
                validatedOrder.setTimestamp(System.currentTimeMillis());
            if (validatedOrder.getCustomerName() == null) validatedOrder.setCustomerName("Guest");
            if (validatedOrder.getCustomerEmail() == null)
                validatedOrder.setCustomerEmail("guest@example.com");
            if (validatedOrder.getCustomerPhone() == null) validatedOrder.setCustomerPhone("");
            if (validatedOrder.getExternalId() == null) {
                validatedOrder.setExternalId("ORD_" + System.currentTimeMillis());
            }

            // Create a validated copy of order items
            List<OrderItem> validatedItems = new ArrayList<>();

            // Validate all order items before submission
            for (OrderItem item : orderItems) {
                // Create a new validated item
                OrderItem validatedItem = new OrderItem();

                // Copy and validate essential fields
                // Copy and validate essential fields without using getId/setId
                validatedItem.setOrderId(item.getOrderId());

                // Validate name
                if (item.getName() == null || item.getName().isEmpty()) {
                    validatedItem.setName("Unknown Item");
                } else {
                    validatedItem.setName(item.getName());
                }

                // Validate notes and special instructions
                validatedItem.setNotes(item.getNotes() != null ? item.getNotes() : "");
                validatedItem.setSpecialInstructions(item.getSpecialInstructions() != null ?
                        item.getSpecialInstructions() : "");

                // Ensure menuItemId is properly set
                if (item.getMenuItemId() == null) {
                    validatedItem.setMenuItemId(-1L);  // Use default indicator for manually added items
                } else {
                    validatedItem.setMenuItemId(item.getMenuItemId());
                }

                // Ensure quantity and price are valid
                if (item.getQuantity() <= 0) {
                    validatedItem.setQuantity(1);
                } else {
                    validatedItem.setQuantity(item.getQuantity());
                }

                if (item.getPrice() < 0) {
                    validatedItem.setPrice(0.0);
                } else {
                    validatedItem.setPrice(item.getPrice());
                }

                // Add validated item to the list
                validatedItems.add(validatedItem);
            }

            // Copy the validated data back to the original objects for submission
            order.setTableNumber(validatedOrder.getTableNumber());
            order.setCustomerName(validatedOrder.getCustomerName());
            order.setCustomerEmail(validatedOrder.getCustomerEmail());
            order.setCustomerPhone(validatedOrder.getCustomerPhone());
            order.setTimestamp(validatedOrder.getTimestamp());
            order.setStatus(validatedOrder.getStatus());
            order.setExternalId(validatedOrder.getExternalId());

            // Clear and replace the items list with validated items
            orderItems.clear();
            orderItems.addAll(validatedItems);

            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error validating order: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Submit to cloud services (API endpoints)
     */
    private void submitToCloudServices(Order order, List<OrderItem> orderItems,
                                       OrderSubmissionTracker tracker, String unifiedOrderId) {
        try {
            // Check if we have connectivity
            android.net.ConnectivityManager cm =
                    (android.net.ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

            if (cm == null || cm.getActiveNetworkInfo() == null || !cm.getActiveNetworkInfo().isConnected()) {
                Log.w(TAG, "No network connectivity, skipping cloud submission");
                tracker.markCloudSubmissionFailure();
                return;
            }

            // Create request in background thread
            new Thread(() -> {
                try {
                    // Create JSON payload
                    com.google.gson.Gson gson = new com.google.gson.Gson();
                    Map<String, Object> payload = new HashMap<>();

                    payload.put("externalId", unifiedOrderId);
                    payload.put("tableNumber", order.getTableNumber());
                    payload.put("customerName", order.getCustomerName());
                    payload.put("customerPhone", order.getCustomerPhone());
                    payload.put("customerEmail", order.getCustomerEmail());
                    payload.put("timestamp", order.getTimestamp());
                    payload.put("status", order.getStatus());
                    payload.put("waiterId", order.waiterId);
                    payload.put("total", order.getTotal());
                    payload.put("restaurantId", prefsManager != null ? prefsManager.getRestaurantId() : "1");
                    payload.put("items", orderItems);

                    String jsonPayload = gson.toJson(payload);

                    // Establish connection
                    java.net.URL url = new java.net.URL("https://api.finedine.com/v1/orders");
                    java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setRequestProperty("Accept", "application/json");
                    conn.setDoOutput(true);

                    // Set auth token if available
                    if (prefsManager != null && prefsManager.getApiToken() != null) {
                        conn.setRequestProperty("Authorization", "Bearer " + prefsManager.getApiToken());
                    }

                    // Send payload
                    try (java.io.OutputStream os = conn.getOutputStream()) {
                        byte[] input = jsonPayload.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                        os.write(input, 0, input.length);
                    }

                    // Get response
                    int responseCode = conn.getResponseCode();

                    if (responseCode >= 200 && responseCode < 300) {
                        // Success
                        tracker.markCloudSubmissionSuccess();
                        Log.d(TAG, "Order successfully submitted to cloud API");

                        // Parse response to get any server-assigned IDs
                        try (java.io.BufferedReader br = new java.io.BufferedReader(
                                new java.io.InputStreamReader(conn.getInputStream(), java.nio.charset.StandardCharsets.UTF_8))) {
                            StringBuilder response = new StringBuilder();
                            String responseLine;
                            while ((responseLine = br.readLine()) != null) {
                                response.append(responseLine.trim());
                            }
                            Log.d(TAG, "Cloud API response: " + response.toString());
                        }
                    } else {
                        // HTTP error
                        tracker.markCloudSubmissionFailure();
                        Log.e(TAG, "Cloud API submission failed with HTTP code: " + responseCode);

                        // Store for later retry
                        storeOrderForLaterSync(order, orderItems, unifiedOrderId);
                    }

                    // Check if we can proceed
                    runOnUiThread(() -> {
                        tracker.checkAndProceedWithOrder(OrderActivity.this, null, -1, order);
                    });

                } catch (Exception e) {
                    Log.e(TAG, "Error submitting to cloud API", e);
                    tracker.markCloudSubmissionFailure();

                    // Store for later retry
                    storeOrderForLaterSync(order, orderItems, unifiedOrderId);

                    // Check if we can proceed
                    runOnUiThread(() -> {
                        tracker.checkAndProceedWithOrder(OrderActivity.this, null, -1, order);
                    });
                }
            }).start();

        } catch (Exception e) {
            Log.e(TAG, "Error initiating cloud submission", e);
            tracker.markCloudSubmissionFailure();
        }
    }

    /**
     * Send notification to Firebase Cloud Messaging topics
     */
    private void sendFcmNotification(long orderId, int tableNumber) {
        try {
            // This would normally use Firebase Cloud Messaging HTTP v1 API
            // For this demo, we're just logging that we would do this
            Log.d(TAG, "Would send FCM notification to 'kitchen' topic for order #" + orderId);

            // In a real implementation, we would make an HTTP request to the FCM API
        } catch (Exception e) {
            Log.e(TAG, "Error sending FCM notification", e);
        }
    }

    /**
     * Send notification to kitchen printers
     */
    private void sendKitchenPrinterNotification(long orderId, int tableNumber) {
        try {
            // Check if printer integration is enabled
            boolean printerEnabled = prefsManager != null && prefsManager.isPrinterEnabled();

            if (!printerEnabled) {
                Log.d(TAG, "Kitchen printer integration not enabled, skipping");
                return;
            }

            // In a real implementation, this would integrate with a printer service
            Log.d(TAG, "Would send order #" + orderId + " to kitchen printer");

        } catch (Exception e) {
            Log.e(TAG, "Error sending kitchen printer notification", e);
        }
    }

    /**
     * Send SMS notification to kitchen staff
     */
    private void sendKitchenSmsNotification(long orderId, int tableNumber) {
        try {
            // Check if SMS notification is enabled
            boolean smsEnabled = prefsManager != null && prefsManager.isSmsNotificationsEnabled();

            if (!smsEnabled) {
                Log.d(TAG, "SMS notifications not enabled, skipping");
                return;
            }

            String phoneNumber = prefsManager != null ? prefsManager.getKitchenPhoneNumber() : null;

            if (phoneNumber == null || phoneNumber.isEmpty()) {
                Log.d(TAG, "No kitchen phone number configured, skipping SMS");
                return;
            }

            // In a real implementation, this would integrate with an SMS service
            Log.d(TAG, "Would send SMS notification to " + phoneNumber + " for order #" + orderId);

        } catch (Exception e) {
            Log.e(TAG, "Error sending SMS notification", e);
        }
    }

    /**
     * Send order details to chef via Firebase Firestore
     * @param selectedItems Map of selected items and their quantities
     * @param notes Special instructions for the chef
     */
    private void sendOrderToChef(HashMap<String, Integer> selectedItems, String notes) {
        com.google.firebase.firestore.FirebaseFirestore db = FirebaseFirestore.getInstance();
        // Prepare order data
        ArrayList<String> orderList = new ArrayList<>();
        ArrayList<String> quantityList = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : selectedItems.entrySet()) {
            String itemWithQuantity = entry.getKey() + " x" + entry.getValue();
            orderList.add(itemWithQuantity);
            quantityList.add(String.valueOf(entry.getValue()));
        }
        // Create chef order data
        Map<String, Object> orderData = new HashMap<>();
        orderData.put("orders", orderList);
        orderData.put("quantities", quantityList);
        orderData.put("notes", notes != null ? new ArrayList<>(Arrays.asList(notes)) : new ArrayList<>());
        orderData.put("timestamp", System.currentTimeMillis());
        orderData.put("tableNumber", currentTableNumber);
        orderData.put("status", "new");
        // Add metadata
        if (prefsManager != null) {
            orderData.put("restaurantId", prefsManager.getRestaurantId());
            orderData.put("sentBy", prefsManager.getUserName());
        }
        // Add order to Firestore
        db.collection("chef_orders")
                .add(orderData)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Order sent to chef successfully with ID: " + documentReference.getId());
                    Toast.makeText(OrderActivity.this,
                            "Order sent to kitchen successfully",
                            Toast.LENGTH_SHORT).show();
                    sendKitchenTabletNotification(-1, currentTableNumber);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error sending order to chef", e);
                    Toast.makeText(OrderActivity.this,
                            "Failed to send order to kitchen. Please try again.",
                            Toast.LENGTH_SHORT).show();
                    storeChefOrderForSync(selectedItems, notes);
                });
    }

    /**
     * Store chef order locally for later synchronization
     */
    private void storeChefOrderForSync(HashMap<String, Integer> selectedItems, String notes) {
        try {
            if (prefsManager != null) {
                // Create a unique ID for this pending order
                String pendingId = "chef_order_" + System.currentTimeMillis();

                // Store data as JSON
                com.google.gson.Gson gson = new com.google.gson.Gson();
                String itemsJson = gson.toJson(selectedItems);

                // Store in SharedPreferences
                android.content.SharedPreferences prefs = getSharedPreferences("pending_chef_orders", MODE_PRIVATE);
                android.content.SharedPreferences.Editor editor = prefs.edit();
                editor.putString(pendingId + "_items", itemsJson);
                editor.putString(pendingId + "_notes", notes);
                editor.putInt(pendingId + "_table", currentTableNumber);
                editor.putLong(pendingId + "_timestamp", System.currentTimeMillis());
                editor.apply();

                Log.d(TAG, "Chef order stored locally with ID: " + pendingId);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error storing chef order locally", e);
        }
    }

    /**
     * Store order locally for later synchronization when connectivity is restored
     * @return true if the order was successfully stored locally
     */
    private boolean storeOrderForLaterSync(Order order, List<OrderItem> orderItems, String externalId) {
        try {
            // Store pending sync information in SharedPreferences
            if (prefsManager != null) {
                // Add to pending orders list
                prefsManager.addPendingSyncOrder(externalId);

                // Store order data in JSON format for later sync
                com.google.gson.Gson gson = new com.google.gson.Gson();
                String orderJson = gson.toJson(order);
                String itemsJson = gson.toJson(orderItems);

                prefsManager.storePendingOrderData(externalId, orderJson, itemsJson);

                Log.d(TAG, "Order stored locally for later sync: " + externalId);
                return true;
            }
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Failed to store order for later sync", e);
            return false;
        }
    }

    /**
     * Class to track order submission status across multiple destinations
     */
    private class OrderSubmissionTracker {
        private boolean localDatabaseSuccess = false;
        private boolean firebaseSubmitted = false;
        private boolean firebaseSuccess = false;
        private boolean cloudSubmitted = false;
        private long localOrderId = -1;
        private boolean hasProcessedOrder = false;

        // Order is considered submitted if ANY destination succeeds
        private boolean isOrderSubmitted() {
            return localDatabaseSuccess || firebaseSuccess || cloudSubmitted;
        }

        public void markLocalDatabaseSuccess(long orderId) {
            localDatabaseSuccess = true;
            localOrderId = orderId;
        }

        public void markLocalDatabaseFailure() {
            localDatabaseSuccess = false;
        }

        public void markFirebaseSuccess() {
            firebaseSubmitted = true;
            firebaseSuccess = true;
        }

        public void markFirebaseFailure() {
            firebaseSubmitted = true;
            firebaseSuccess = false;
        }

        public boolean isFirebaseSubmitted() {
            return firebaseSubmitted;
        }

        public void markCloudSubmissionSuccess() {
            cloudSubmitted = true;
        }

        public void markCloudSubmissionFailure() {
            cloudSubmitted = false;
        }

        /**
         * Check if order can be considered submitted and proceed with UI flow
         */
        public synchronized void checkAndProceedWithOrder(OrderActivity activity, AlertDialog progressDialog,
                                             long orderId, Order order) {
            // Prevent processing multiple times
            if (hasProcessedOrder) {
                return;
            }

            // Only proceed once
            if (!isOrderSubmitted()) {
                // Don't proceed yet, waiting for at least one successful submission
                return;
            }

            // Mark as processed to prevent duplicate processing
            hasProcessedOrder = true;

            activity.runOnUiThread(() -> {
                try {
                    // Dismiss progress dialog if still showing
                    if (progressDialog != null && progressDialog.isShowing()) {
                        try {
                            progressDialog.dismiss();
                        } catch (Exception e) {
                            Log.e(TAG, "Error dismissing progress dialog", e);
                        }
                    }

                    // Use local order ID if available, otherwise use -1
                    long effectiveOrderId = localOrderId > 0 ? localOrderId : orderId;

                    // Send notification to kitchen
                    activity.notifyKitchenAboutNewOrder(effectiveOrderId, order.getTableNumber());

                    // Show payment options dialog
                    activity.showPaymentOptionsDialog(effectiveOrderId, order.getTotal());

                    // Log submission state
                    Log.d(TAG, "Order submission complete. Local DB: " + localDatabaseSuccess +
                            ", Firebase: " + firebaseSubmitted + ", Cloud: " + cloudSubmitted);
                } catch (Exception e) {
                    Log.e(TAG, "Error processing order completion", e);
                    Toast.makeText(activity, "Order processed, but there was an issue displaying confirmation", Toast.LENGTH_LONG).show();

                    // Force clear the order even if there was an error showing dialogs
                    activity.clearOrder();
                }
            });
        }
    }
}