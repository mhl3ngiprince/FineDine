package com.finedine.rms;

import android.app.AlertDialog;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu_item_detail);

        try {
            // Setup navigation panel
            setupNavigationPanel("Menu Item Detail");

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

            // Get menu item from intent
            if (getIntent().hasExtra("item_id")) {
                int itemId = getIntent().getIntExtra("item_id", -1);
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
                    .load(menuItem.imageUrl)
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
            // Create a new OrderItem
            OrderItem orderItem = new OrderItem();
            orderItem.setName(item.name);
            orderItem.setQuantity(quantity);
            orderItem.setNotes(notes);
            orderItem.setPrice(item.price);

            // Save to database or pass back to calling activity
            Toast.makeText(this, quantity + "x " + item.name + " added to your order", Toast.LENGTH_SHORT).show();

            // You could either:
            // 1. Start OrderActivity with this item
            // 2. Add to cart and return to previous activity
            // 3. Show order confirmation screen

            // For now, we'll just finish the activity
            finish();
        } catch (Exception e) {
            Log.e(TAG, "Error adding item to order", e);
            Toast.makeText(this, "Error adding item to order", Toast.LENGTH_SHORT).show();
        }
    }
}