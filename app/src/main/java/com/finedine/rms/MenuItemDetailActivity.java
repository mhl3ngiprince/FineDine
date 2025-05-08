package com.finedine.rms;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.textfield.TextInputEditText;

import java.util.List;

public class MenuItemDetailActivity extends BaseActivity {
    private static final String TAG = "MenuItemDetailActivity";
    private MenuItem menuItem;

    private ImageView ivMenuItemImage;
    private TextView tvMenuItemName;
    private TextView tvMenuItemDescription;
    private TextView tvMenuItemPrice;
    private TextView tvMenuItemCategory;
    private TextView tvPrepTime;
    private TextView tvCalories;
    private TextView tvSpiceLevel;
    private Button btnOrderNow;

    /**
     * Static helper method to launch this activity properly with required data
     *
     * @param context  The context to start the activity from
     * @param menuItem The menu item to display in detail
     */
    public static void launch(Context context, MenuItem menuItem) {
        if (context == null || menuItem == null) {
            Log.e("MenuItemDetailActivity", "Cannot launch with null context or menu item");
            return;
        }

        try {
            Intent intent = new Intent(context, MenuItemDetailActivity.class);
            intent.putExtra("item_id", menuItem.getItem_id());
            // Don't use FLAG_ACTIVITY_CLEAR_TOP to prevent finishing the parent activity
            context.startActivity(intent);
            Log.d("MenuItemDetailActivity", "Launching detail view for: " + menuItem.getName() + " (ID: " + menuItem.getItem_id() + ")");
        } catch (Exception e) {
            Log.e("MenuItemDetailActivity", "Error launching activity", e);
            Toast.makeText(context, "Error displaying menu item details", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            // Setup modern navigation panel
            setupModernNavigationPanel("Menu Item Detail", R.layout.activity_menu_item_detail);

            // Initialize views
            ivMenuItemImage = findViewById(R.id.ivMenuItemDetailImage);
            tvMenuItemName = findViewById(R.id.tvMenuItemDetailName);
            tvMenuItemDescription = findViewById(R.id.tvMenuItemDetailDescription);
            tvMenuItemPrice = findViewById(R.id.tvMenuItemDetailPrice);
            tvMenuItemCategory = findViewById(R.id.tvMenuItemDetailCategory);
            tvPrepTime = findViewById(R.id.tvMenuItemDetailPrepTime);
            tvCalories = findViewById(R.id.tvMenuItemDetailCalories);
            tvSpiceLevel = findViewById(R.id.tvMenuItemDetailSpiceLevel);
            btnOrderNow = findViewById(R.id.btnOrderNow);

            // Enable back navigation
            androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
            if (toolbar != null) {
                setSupportActionBar(toolbar);
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setDisplayShowHomeEnabled(true);
                toolbar.setNavigationOnClickListener(v -> onBackPressed());
            }

            // Get menu item from intent
            if (getIntent().hasExtra("item_id")) {
                int itemId = getIntent().getIntExtra("item_id", -1);
                loadMenuItemById(itemId);
            } else if (getIntent().hasExtra("MENU_ITEM_ID")) {
                int itemId = getIntent().getIntExtra("MENU_ITEM_ID", -1);
                loadMenuItemById(itemId);
            } else if (getIntent().hasExtra("item_name")) {
                String itemName = getIntent().getStringExtra("item_name");
                loadMenuItemByName(itemName);
            } else {
                Toast.makeText(this, "Error: No menu item specified", Toast.LENGTH_SHORT).show();
                finish();
            }

            // Set up order button click listener
            btnOrderNow.setOnClickListener(v -> placeOrder());
        } catch (Exception e) {
            Log.e(TAG, "Error initializing MenuItemDetailActivity", e);
            Toast.makeText(this, "Error loading menu item details", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadMenuItemById(int itemId) {
        try {
            AppDatabase db = AppDatabase.getDatabase(this);
            new Thread(() -> {
                try {
                    List<MenuItem> menuItems = db.menuItemDao().getAllAvailable();
                    MenuItem foundItem = null;

                    // Find the menu item with matching ID
                    for (MenuItem item : menuItems) {
                        if (item.item_id == itemId) {
                            foundItem = item;
                            break;
                        }
                    }

                    // Store found item for UI update
                    final MenuItem finalItem = foundItem;

                    runOnUiThread(() -> {
                        if (finalItem != null) {
                            menuItem = finalItem;
                            displayMenuItem();
                        } else {
                            Toast.makeText(MenuItemDetailActivity.this,
                                    "Menu item not found", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Error loading menu item by ID", e);
                    runOnUiThread(() -> {
                        Toast.makeText(MenuItemDetailActivity.this,
                                "Error loading menu item", Toast.LENGTH_SHORT).show();
                        finish();
                    });
                }
            }).start();
        } catch (Exception e) {
            Log.e(TAG, "Error in loadMenuItemById", e);
            Toast.makeText(this, "Error loading menu item", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void loadMenuItemByName(String itemName) {
        try {
            AppDatabase db = AppDatabase.getDatabase(this);
            new Thread(() -> {
                try {
                    List<MenuItem> menuItems = db.menuItemDao().getAllAvailable();
                    MenuItem foundItem = null;

                    // Find the menu item with matching name
                    for (MenuItem item : menuItems) {
                        if (item.name != null && item.name.equals(itemName)) {
                            foundItem = item;
                            break;
                        }
                    }

                    // Store found item for UI update
                    final MenuItem finalItem = foundItem;

                    runOnUiThread(() -> {
                        if (finalItem != null) {
                            menuItem = finalItem;
                            displayMenuItem();
                        } else {
                            Toast.makeText(MenuItemDetailActivity.this,
                                    "Menu item not found", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Error loading menu item by name", e);
                    runOnUiThread(() -> {
                        Toast.makeText(MenuItemDetailActivity.this,
                                "Error loading menu item", Toast.LENGTH_SHORT).show();
                        finish();
                    });
                }
            }).start();
        } catch (Exception e) {
            Log.e(TAG, "Error in loadMenuItemByName", e);
            Toast.makeText(this, "Error loading menu item", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void displayMenuItem() {
        try {
            if (menuItem == null) {
                Toast.makeText(this, "Error: Menu item not available", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            // Set text fields
            tvMenuItemName.setText(menuItem.name);
            tvMenuItemDescription.setText(menuItem.description);
            tvMenuItemPrice.setText(String.format("R%.2f", menuItem.price));
            tvMenuItemCategory.setText(menuItem.category);
            tvPrepTime.setText(String.format("Prep Time: %d minutes", menuItem.prepTimeMinutes));
            tvCalories.setText(String.format("Calories: %d", menuItem.calories));
            tvSpiceLevel.setText(String.format("Spice Level: %s", menuItem.spiceLevel));

            // Load image using Glide
            RequestOptions requestOptions = new RequestOptions();
            requestOptions.centerCrop();

            Glide.with(this)
                    .load(menuItem.imageResourceId)
                    .apply(requestOptions)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .placeholder(R.drawable.placeholder_food)
                    .error(R.drawable.placeholder_food)
                    .into(ivMenuItemImage);
        } catch (Exception e) {
            Log.e(TAG, "Error displaying menu item", e);
            Toast.makeText(this, "Error displaying menu item details", Toast.LENGTH_SHORT).show();
        }
    }

    private void placeOrder() {
        try {
            if (menuItem == null) {
                Toast.makeText(this, "Error: Menu item not available", Toast.LENGTH_SHORT).show();
                return;
            }

            // Show quantity dialog
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Order " + menuItem.name);

            View view = LayoutInflater.from(this).inflate(R.layout.dialog_quantity, null);
            TextInputEditText quantityInput = view.findViewById(R.id.quantityInput);
            TextInputEditText notesInput = view.findViewById(R.id.notesInput);

            // Set default quantity
            quantityInput.setText("1");

            builder.setView(view);
            builder.setPositiveButton("Place Order", (dialog, which) -> {
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

                    // Process the order
                    addToOrder(menuItem, quantity, notes);
                } catch (Exception e) {
                    Log.e(TAG, "Error placing order", e);
                    Toast.makeText(MenuItemDetailActivity.this,
                            "Error placing order", Toast.LENGTH_SHORT).show();
                }
            });
            builder.setNegativeButton("Cancel", null);
            builder.show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing quantity dialog", e);
            Toast.makeText(this, "Error placing order", Toast.LENGTH_SHORT).show();
        }
    }

    private void addToOrder(MenuItem item, int quantity, String notes) {
        try {
            // Show customer information dialog to collect details
            showCustomerInfoDialog(item, quantity, notes);
        } catch (Exception e) {
            Log.e(TAG, "Error processing order", e);
            Toast.makeText(this, "Error processing order: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void showCustomerInfoDialog(MenuItem menuItem, int quantity, String additionalInstructions) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Customer Information");

        View view = LayoutInflater.from(this).inflate(R.layout.dialog_customer_info, null);
        TextInputEditText nameInput = view.findViewById(R.id.etCustomerName);
        TextInputEditText phoneInput = view.findViewById(R.id.etCustomerPhone);
        TextInputEditText tableInput = view.findViewById(R.id.etTableNumber);
        TextInputEditText notesInput = view.findViewById(R.id.etCustomerNotes);

        // Set default table number
        tableInput.setText("1");

        builder.setView(view);
        builder.setPositiveButton("Place Order", null); // We'll override this below
        builder.setNegativeButton("Cancel", null);

        AlertDialog dialog = builder.create();
        dialog.show();

        // Override the positive button to prevent automatic dismissal on error
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            try {
                String customerName = nameInput.getText() != null ? nameInput.getText().toString() : "";
                String phoneNumber = phoneInput.getText() != null ? phoneInput.getText().toString() : "";
                String customerNotes = notesInput.getText() != null ? notesInput.getText().toString() : "";

                // Safely parse table number with error handling
                int tableNumber = 1;
                try {
                    String tableText = tableInput.getText().toString();
                    if (!tableText.isEmpty()) {
                        tableNumber = Integer.parseInt(tableText);
                    }
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Error parsing table number", e);
                    tableInput.setError("Please enter a valid table number");
                    return;
                }

                // Validate inputs
                if (customerName.trim().isEmpty()) {
                    nameInput.setError("Please enter customer name");
                    return;
                }

                // All validation passed, process the order
                processOrder(menuItem, quantity, additionalInstructions,
                        customerName, phoneNumber, tableNumber, customerNotes);
                dialog.dismiss();

            } catch (Exception e) {
                Log.e(TAG, "Error collecting customer information", e);
                Toast.makeText(MenuItemDetailActivity.this,
                        "Error processing order information", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void processOrder(MenuItem menuItem, int quantity, String additionalInstructions,
                              String customerName, String phoneNumber, int tableNumber, String customerNotes) {
        // Run database operations in background thread
        new Thread(() -> {
            try {
                AppDatabase database = AppDatabase.getDatabase(MenuItemDetailActivity.this);

                if (database != null) {
                    OrderItem orderItem = new OrderItem();
                    orderItem.setName(menuItem.name);
                    orderItem.setQuantity(quantity);
                    orderItem.setPrice(menuItem.price * quantity);
                    orderItem.setNotes(additionalInstructions);
                    // Create order with generated ID
                    Order order = new Order(tableNumber, "pending");
                    order.setCustomerName(customerName);
                    order.setCustomerPhone(phoneNumber);
                    order.setCustomerNotes(customerNotes);
                    order.setTimestamp(System.currentTimeMillis());
                    order.setOrderTime(System.currentTimeMillis());
                    order.setTotal(menuItem.price * quantity);

                    long orderId = database.orderDao().insert(order);

                    // Set the orderId on the orderItem
                    orderItem.setOrderId(orderId);

                    // Insert the orderItem
                    database.orderItemDao().insert(orderItem);

                    runOnUiThread(() -> {
                        Toast.makeText(MenuItemDetailActivity.this,
                                "Order placed successfully! Kitchen is being notified.",
                                Toast.LENGTH_LONG).show();
                        // Removed finish() to prevent activity from closing
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Error processing order", e);
                runOnUiThread(() -> {
                    Toast.makeText(MenuItemDetailActivity.this,
                            "Error placing order: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    // Simple order method that doesn't rely on the database
    private void simpleOrder(MenuItem item, int quantity, String notes) {
        // Simply show a success message and finish the activity
        runOnUiThread(() -> {
            Toast.makeText(this, quantity + "x " + item.name + " added to your order", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    /**
     * Override onBackPressed to ensure proper navigation when user presses back button
     */
    @Override
    public void onBackPressed() {
        Log.d(TAG, "Back button pressed in MenuItemDetailActivity");
        super.onBackPressed();
    }

    /**
     * Save instance state to preserve menu item data during lifecycle events
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (menuItem != null) {
            outState.putInt("item_id", menuItem.getItem_id());
            Log.d(TAG, "Saved menu item state: " + menuItem.getItem_id());
        }
    }

    /**
     * Restore instance state when activity is recreated
     */
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState != null && savedInstanceState.containsKey("item_id")) {
            int itemId = savedInstanceState.getInt("item_id", -1);
            if (itemId > 0) {
                Log.d(TAG, "Restoring menu item state: " + itemId);
                loadMenuItemById(itemId);
            }
        }
    }
}