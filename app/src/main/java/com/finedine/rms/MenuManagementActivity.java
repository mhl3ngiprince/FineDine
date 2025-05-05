package com.finedine.rms;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MenuManagementActivity extends BaseActivity {
    private static final String TAG = "MenuManagementActivity";
    private RecyclerView rvMenuItems;
    private MenuAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu_management);

        try {
            // Setup navigation panel
            setupNavigationPanel("Menu Management");

            // Initialize RecyclerView
            rvMenuItems = findViewById(R.id.rvMenuItems);
            rvMenuItems.setLayoutManager(new GridLayoutManager(this, 2));

            // Add spacing between grid items
            int spacingInPixels = getResources().getDimensionPixelSize(R.dimen.grid_spacing);
            rvMenuItems.addItemDecoration(new GridSpacingItemDecoration(2, spacingInPixels, true));

            // Create adapter with empty list initially
            adapter = new MenuAdapter(new ArrayList<>(), item -> {
                // Handle menu item click
                Toast.makeText(MenuManagementActivity.this,
                        "Selected: " + item.getName(),
                        Toast.LENGTH_SHORT).show();
                // Show details of the menu item
                showMenuItemDetails(item);
            });

            rvMenuItems.setAdapter(adapter);

            // Setup Add Item button
            findViewById(R.id.fabAddItem).setOnClickListener(v -> {
                showAddItemDialog();
            });

            // Setup search functionality
            setupSearchFunctionality();

            // Load menu items
            loadMenuItems();
        } catch (Exception e) {
            Log.e(TAG, "Error initializing MenuManagementActivity", e);
            Toast.makeText(this, "Error initializing menu screen", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadMenuItems() {
        try {
            // Show loading indicator or message
            Toast.makeText(this, "Loading menu items...", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Starting menu item loading process");

            // Force direct loading without database first for immediate display
            final MenuItem[] premiumItems = MenuItem.premiumMenu();
            List<MenuItem> directItems = new ArrayList<>(Arrays.asList(premiumItems));

            // Update UI immediately with items
            adapter.updateItems(directItems);
            Log.d(TAG, "Direct loaded " + directItems.size() + " items for immediate display");

            // Then try to save to database in background
            AppDatabase db = AppDatabase.getDatabase(this);
            new Thread(() -> {
                try {
                    // First try directly adding premium items to avoid database issues
                    // Clear all existing items to avoid duplicates
                    try {
                        db.menuItemDao().deleteAll();
                        Log.d(TAG, "Cleared existing menu items");
                    } catch (Exception e) {
                        Log.e(TAG, "Error clearing menu items", e);
                    }

                    // Add the new items
                    List<MenuItem> items = new ArrayList<>();
                    for (MenuItem item : premiumItems) {
                        try {
                            long id = db.menuItemDao().insert(item);
                            item.setItem_id((int) id); // Set the generated ID
                            items.add(item);
                            Log.d(TAG, "Added menu item: " + item.getName() + " with ID: " + id);
                        } catch (Exception e) {
                            Log.e(TAG, "Error adding menu item: " + item.getName(), e);
                            // Still add to local list even if database insert fails
                            items.add(item);
                        }
                    }
                    Log.i(TAG, "Added " + items.size() + " premium menu items");

                    // If items list is still empty (unlikely but possible), use the premium items directly
                    final List<MenuItem> finalItems = items.isEmpty() ?
                            new ArrayList<>(Arrays.asList(premiumItems)) : new ArrayList<>(items);

                    // Update UI with database items (with IDs)
                    runOnUiThread(() -> {
                        try {
                            if (finalItems.isEmpty()) {
                                Log.w(TAG, "No menu items to display");
                                // Keep the direct loaded items visible
                            } else {
                                Log.d(TAG, "Updating adapter with " + finalItems.size() + " items from database");
                                // Update adapter with loaded items
                                adapter.updateItems(finalItems);
                                Toast.makeText(MenuManagementActivity.this,
                                        finalItems.size() + " menu items loaded",
                                        Toast.LENGTH_SHORT).show();
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error updating menu items UI", e);
                            Toast.makeText(MenuManagementActivity.this,
                                    "Error displaying menu items: " + e.getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Error loading menu items from database", e);
                    // We already have direct loaded items, so no need for another fallback
                }
            }).start();
        } catch (Exception e) {
            Log.e(TAG, "Error in loadMenuItems", e);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();

            // Emergency direct loading if thread fails
            try {
                final MenuItem[] items = MenuItem.premiumMenu();
                adapter.updateItems(new ArrayList<>(Arrays.asList(items)));
                Log.d(TAG, "Emergency direct loading " + items.length + " items");
            } catch (Exception ex) {
                Log.e(TAG, "Critical failure loading menu items", ex);
            }
        }
    }

    /**
     * Show dialog to add a new menu item
     */
    private void showAddItemDialog() {
        try {
            // Create an alert dialog for adding a new menu item
            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
            builder.setTitle("Add New Menu Item");

            // Inflate the layout for the dialog
            View view = getLayoutInflater().inflate(R.layout.dialog_add_menu_item, null);
            builder.setView(view);

            // Get references to the input fields
            final android.widget.EditText nameInput = view.findViewById(R.id.etItemName);
            final android.widget.EditText descriptionInput = view.findViewById(R.id.etItemDescription);
            final android.widget.EditText priceInput = view.findViewById(R.id.etItemPrice);
            final android.widget.EditText categoryInput = view.findViewById(R.id.etItemCategory);
            final android.widget.EditText prepTimeInput = view.findViewById(R.id.etPrepTime);
            final android.widget.EditText caloriesInput = view.findViewById(R.id.etCalories);
            final android.widget.EditText spiceLevelInput = view.findViewById(R.id.etSpiceLevel);

            // Setup buttons
            builder.setPositiveButton("Add", (dialog, which) -> {
                // Validate inputs
                String name = nameInput.getText().toString().trim();
                String description = descriptionInput.getText().toString().trim();
                String priceStr = priceInput.getText().toString().trim();
                String category = categoryInput.getText().toString().trim();
                String prepTimeStr = prepTimeInput.getText().toString().trim();
                String caloriesStr = caloriesInput.getText().toString().trim();
                String spiceLevel = spiceLevelInput.getText().toString().trim();

                if (name.isEmpty() || description.isEmpty() || priceStr.isEmpty() || category.isEmpty()) {
                    Toast.makeText(this, "Please fill in all required fields", Toast.LENGTH_SHORT).show();
                    return;
                }

                try {
                    double price = Double.parseDouble(priceStr);
                    int prepTime = prepTimeStr.isEmpty() ? 15 : Integer.parseInt(prepTimeStr);
                    int calories = caloriesStr.isEmpty() ? 0 : Integer.parseInt(caloriesStr);

                    // Create a new menu item
                    MenuItem newItem = new MenuItem();
                    newItem.setName(name);
                    newItem.setDescription(description);
                    newItem.setPrice(price);
                    newItem.setAvailability(true);
                    newItem.setCategory(category);
                    newItem.setPrepTimeMinutes(prepTime);
                    newItem.setCalories(calories);
                    newItem.setSpiceLevel(spiceLevel.isEmpty() ? "None" : spiceLevel);
                    newItem.setImageResourceId(MenuItem.IMG_PLACEHOLDER); // Use placeholder image

                    // Save to database
                    saveNewMenuItem(newItem);

                } catch (NumberFormatException e) {
                    Toast.makeText(this, "Invalid number format", Toast.LENGTH_SHORT).show();
                }
            });

            builder.setNegativeButton("Cancel", null);

            // Show the dialog
            builder.create().show();

        } catch (Exception e) {
            Log.e(TAG, "Error showing add item dialog", e);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Save a new menu item to the database and update the UI
     */
    private void saveNewMenuItem(MenuItem newItem) {
        new Thread(() -> {
            try {
                AppDatabase db = AppDatabase.getDatabase(this);
                long id = db.menuItemDao().insert(newItem);
                newItem.setItem_id((int) id);

                runOnUiThread(() -> {
                    try {
                        // Add to the current list and update adapter
                        List<MenuItem> currentItems = new ArrayList<>(adapter.getItems());
                        currentItems.add(newItem);
                        adapter.updateItems(currentItems);

                        Toast.makeText(this, "Menu item added: " + newItem.getName(), Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Log.e(TAG, "Error updating UI after adding item", e);
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error saving new menu item", e);
                runOnUiThread(() -> {
                    Toast.makeText(this, "Error saving menu item: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    /**
     * Show detailed information about a menu item
     */
    private void showMenuItemDetails(MenuItem item) {
        try {
            MenuItemDetailActivity.launch(this, item);
        } catch (Exception e) {
            Log.e(TAG, "Error showing menu item details", e);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Setup search functionality for the menu items
     */
    private void setupSearchFunctionality() {
        EditText searchEditText = findViewById(R.id.etSearchMenu);
        if (searchEditText != null) {
            searchEditText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    // Not used
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    String searchText = s.toString().toLowerCase();
                    filterMenuItems(searchText);
                }

                @Override
                public void afterTextChanged(Editable s) {
                    // Not used
                }
            });
        }
    }

    /**
     * Filter menu items based on search text
     *
     * @param searchText Text to search for
     */
    private void filterMenuItems(String searchText) {
        if (searchText == null || searchText.isEmpty()) {
            // If search is empty, reload all items
            loadMenuItems();
            return;
        }

        try {
            // Create a copy of the current items to filter
            List<MenuItem> currentItems = adapter.getItems();
            List<MenuItem> filteredItems = new ArrayList<>();

            // Filter items that match the search text
            for (MenuItem item : currentItems) {
                if (item.getName() != null && item.getName().toLowerCase().contains(searchText) ||
                        item.getDescription() != null && item.getDescription().toLowerCase().contains(searchText) ||
                        item.getCategory() != null && item.getCategory().toLowerCase().contains(searchText)) {
                    filteredItems.add(item);
                }
            }

            // Update the adapter with filtered items
            adapter.updateItems(filteredItems);

            // Show message if no items found
            if (filteredItems.isEmpty()) {
                Toast.makeText(this, "No menu items match '" + searchText + "'", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error filtering menu items", e);
        }
    }
}