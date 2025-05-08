package com.finedine.rms;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.finedine.rms.utils.SharedPrefsManager;
import com.finedine.rms.utils.MenuFirebaseIntegration;

public class MenuManagementActivity extends BaseActivity {
    private static final String TAG = "MenuManagementActivity";
    private RecyclerView rvMenuItems;
    private MenuAdapter adapter;
    private List<MenuItem> menuItemsList = new ArrayList<>();
    private MenuFirebaseIntegration menuFirebaseIntegration;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "Starting MenuManagementActivity");
        super.onCreate(savedInstanceState);
        initializeModernNavigation("Menu Management", R.layout.activity_menu_management);
        Log.d(TAG, "Content view set using initializeModernNavigation");

        // Fix the menu button - find and properly set its click handler
        setupMenuButtonClickHandler();

        try {
            // Set title in action bar if available
            setTitle("Menu Management");

            // Initialize Firebase integration
            try {
                db = FirebaseFirestore.getInstance();
                menuFirebaseIntegration = new MenuFirebaseIntegration(this);
                Log.d(TAG, "Firebase integration initialized successfully");
            } catch (Exception e) {
                Log.e(TAG, "Error initializing Firebase integration: " + e.getMessage(), e);
                menuFirebaseIntegration = null;
            }

            // Ensure the background color is set properly
            View rootView = findViewById(android.R.id.content);
            if (rootView != null) {
                rootView.setBackgroundColor(getResources().getColor(R.color.white));
            }

            // Set window background explicitly to avoid black screen
            getWindow().setBackgroundDrawableResource(R.color.white);
            Log.d(TAG, "Background color set");

            // Initialize RecyclerView
            rvMenuItems = findViewById(R.id.rvMenuItems);
            if (rvMenuItems == null) {
                Log.e(TAG, "Failed to find RecyclerView in layout");
                showErrorDialog("Layout Error", "Could not initialize menu view");
                // Create an emergency fallback RecyclerView to avoid null pointer exceptions
                rvMenuItems = new RecyclerView(this);
                Toast.makeText(this, "Using emergency fallback menu view", Toast.LENGTH_SHORT).show();
            } else {
                Log.d(TAG, "RecyclerView initialized successfully");
            }

            // Set layout manager safely
            try {
                rvMenuItems.setLayoutManager(new GridLayoutManager(this, 2));
            } catch (Exception e) {
                Log.e(TAG, "Error setting layout manager", e);
            }

            // Add spacing between grid items
            try {
                int spacingInPixels = getResources().getDimensionPixelSize(R.dimen.grid_spacing);
                rvMenuItems.addItemDecoration(new GridSpacingItemDecoration(2, spacingInPixels, true));
            } catch (Exception e) {
                Log.e(TAG, "Error adding item decoration", e);
            }

            // Create adapter with empty list initially
            adapter = new MenuAdapter(menuItemsList, item -> {
                // Handle menu item click
                Toast.makeText(MenuManagementActivity.this,
                        "Selected: " + item.getName(),
                        Toast.LENGTH_SHORT).show();
                // Show details of the menu item
                showMenuItemDetails(item);
            });

            // Set adapter safely
            try {
                rvMenuItems.setAdapter(adapter);
            } catch (Exception e) {
                Log.e(TAG, "Error setting adapter", e);
            }

            // Setup Add Item button - only show for staff, not customers
            View fabAddItem = findViewById(R.id.fabAddItem);
            if (fabAddItem != null) {
                if ("customer".equalsIgnoreCase(userRole) || "waiter".equalsIgnoreCase(userRole)) {
                    fabAddItem.setVisibility(View.GONE); // Hide for customers and waiters
                } else {
                    fabAddItem.setOnClickListener(v -> {
                        showAddItemDialog();
                    });
                }
            } else {
                Log.e(TAG, "Add item button not found in layout");
            }

            // Setup search functionality
            setupSearchFunctionality();

            // Load menu items
            loadMenuItems();
            Log.d(TAG, "MenuManagementActivity initialized successfully");

            // Set up sync button if available
            View syncButton = findViewById(R.id.syncButton);
            if (syncButton != null) {
                syncButton.setOnClickListener(v -> {
                    showSyncOptions();
                });
                Log.d(TAG, "Sync button initialized successfully");
            } else {
                Log.d(TAG, "Sync button not found in layout");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error initializing MenuManagementActivity", e);
            Toast.makeText(this, "Error initializing menu screen", Toast.LENGTH_SHORT).show();

            // Create an emergency layout to avoid black screen
            LinearLayout emergencyLayout = new LinearLayout(this);
            emergencyLayout.setBackgroundColor(getResources().getColor(R.color.white));
            emergencyLayout.setOrientation(LinearLayout.VERTICAL);

            TextView errorText = new TextView(this);
            errorText.setText("Error loading menu management. Please try again.");
            errorText.setPadding(50, 100, 50, 20);

            Button retryButton = new Button(this);
            retryButton.setText("Retry");
            retryButton.setOnClickListener(v -> recreate());

            emergencyLayout.addView(errorText);
            emergencyLayout.addView(retryButton);

            setContentView(emergencyLayout);
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
            updateAdapterItems(directItems);
            Log.d(TAG, "Direct loaded " + directItems.size() + " items for immediate display");

            // Force refresh of adapter after a delay to ensure items are displayed
            new android.os.Handler().postDelayed(() -> {
                if (adapter != null) {
                    adapter.notifyDataSetChanged();
                }
            }, 500);

            // Load from Firebase for latest data
            loadMenuItemsFromFirebase();

            try {
                new Thread(() -> {
                    try {
                        // Check if menuItemDao is available
                        AppDatabase db = AppDatabase.getDatabase(MenuManagementActivity.this);
                        MenuItemDao menuItemDao = db.menuItemDao();
                        if (menuItemDao == null) {
                            Log.e(TAG, "MenuItemDao is null");
                            return;
                        }

                        // First try directly adding premium items to avoid database issues
                        // Clear all existing items to avoid duplicates
                        try {
                            menuItemDao.deleteAll();
                            Log.d(TAG, "Cleared existing menu items");
                        } catch (Exception e) {
                            Log.e(TAG, "Error clearing menu items", e);
                        }

                        // Add the new items
                        List<MenuItem> items = new ArrayList<>();
                        for (MenuItem item : premiumItems) {
                            try {
                                long id = menuItemDao.insert(item);
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

                        // Update UI with database items (with IDs)
                        runOnUiThread(() -> {
                            try {
                                if (items.isEmpty()) {
                                    Log.w(TAG, "No menu items to display from database");
                                } else {
                                    Log.d(TAG, "Updating adapter with " + items.size() + " items from database");
                                    updateAdapterItems(items);
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error updating menu items", e);
                            }
                        });
                    } catch (Exception e) {
                        Log.e(TAG, "Error loading menu items from database", e);
                    }
                }).start();
            } catch (Exception e) {
                Log.e(TAG, "Error initializing database thread", e);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in loadMenuItems", e);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Safely update the adapter with new items
     */
    private void updateAdapterItems(List<MenuItem> items) {
        if (items == null) {
            Log.e(TAG, "Attempted to update adapter with null items");
            return;
        }

        if (adapter != null) {
            menuItemsList.clear();
            menuItemsList.addAll(items);
            adapter.notifyDataSetChanged();
        } else {
            Log.e(TAG, "Adapter is null, recreating");
            menuItemsList.clear();
            menuItemsList.addAll(items);
            adapter = new MenuAdapter(menuItemsList, item -> {
                Toast.makeText(MenuManagementActivity.this,
                        "Selected: " + item.getName(),
                        Toast.LENGTH_SHORT).show();
                showMenuItemDetails(item);
            });

            if (rvMenuItems != null) {
                rvMenuItems.setAdapter(adapter);
            } else {
                Log.e(TAG, "RecyclerView is null, can't set adapter");
                Toast.makeText(this, "Error displaying menu items", Toast.LENGTH_SHORT).show();
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
            if (view == null) {
                Log.e(TAG, "Failed to inflate menu item dialog layout");
                Toast.makeText(this, "Error creating dialog", Toast.LENGTH_SHORT).show();
                return;
            }
            builder.setView(view);

            // Get references to the input fields with null checks
            final android.widget.EditText nameInput = view.findViewById(R.id.etItemName);
            final android.widget.EditText descriptionInput = view.findViewById(R.id.etItemDescription);
            final android.widget.EditText priceInput = view.findViewById(R.id.etItemPrice);
            final android.widget.EditText categoryInput = view.findViewById(R.id.etItemCategory);
            final android.widget.EditText prepTimeInput = view.findViewById(R.id.etPrepTime);
            final android.widget.EditText caloriesInput = view.findViewById(R.id.etCalories);
            final android.widget.EditText spiceLevelInput = view.findViewById(R.id.etSpiceLevel);

            // Check if required fields were found
            if (nameInput == null || descriptionInput == null || priceInput == null || categoryInput == null) {
                Log.e(TAG, "Required input fields not found in dialog layout");
                Toast.makeText(this, "Error: Dialog layout problem", Toast.LENGTH_SHORT).show();
                return;
            }

            // Prefill category dropdown with standard options
            if (categoryInput != null) {
                categoryInput.setOnClickListener(v -> {
                    showCategorySelectionDialog(categoryInput);
                });
            }

            // Create the dialog
            android.app.AlertDialog dialog = builder.create();

            // Show the dialog first
            dialog.show();

            // Override the buttons after showing the dialog
            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                // Validate inputs
                String name = nameInput.getText().toString().trim();
                String description = descriptionInput.getText().toString().trim();
                String priceStr = priceInput.getText().toString().trim();
                String category = categoryInput.getText().toString().trim();
                String prepTimeStr = prepTimeInput != null ? prepTimeInput.getText().toString().trim() : "";
                String caloriesStr = caloriesInput != null ? caloriesInput.getText().toString().trim() : "";
                String spiceLevel = spiceLevelInput != null ? spiceLevelInput.getText().toString().trim() : "";

                // Validate required fields
                boolean hasErrors = false;

                if (name.isEmpty()) {
                    nameInput.setError("Name is required");
                    hasErrors = true;
                }

                if (description.isEmpty()) {
                    descriptionInput.setError("Description is required");
                    hasErrors = true;
                }

                if (priceStr.isEmpty()) {
                    priceInput.setError("Price is required");
                    hasErrors = true;
                }

                if (category.isEmpty()) {
                    categoryInput.setError("Category is required");
                    hasErrors = true;
                }

                if (hasErrors) {
                    Toast.makeText(MenuManagementActivity.this, "Please fill in all required fields", Toast.LENGTH_SHORT).show();
                    return; // Don't dismiss dialog
                }

                // Validate numeric fields
                double price;
                int prepTime = 15; // Default
                int calories = 0; // Default

                try {
                    price = Double.parseDouble(priceStr);
                    if (price <= 0) {
                        priceInput.setError("Price must be greater than zero");
                        return;
                    }
                } catch (NumberFormatException e) {
                    priceInput.setError("Invalid price format");
                    return;
                }

                try {
                    if (!prepTimeStr.isEmpty()) {
                        prepTime = Integer.parseInt(prepTimeStr);
                        if (prepTime <= 0) {
                            prepTimeInput.setError("Prep time must be positive");
                            return;
                        }
                    }
                } catch (NumberFormatException e) {
                    prepTimeInput.setError("Invalid number");
                    return;
                }

                try {
                    if (!caloriesStr.isEmpty()) {
                        calories = Integer.parseInt(caloriesStr);
                        if (calories < 0) {
                            caloriesInput.setError("Calories cannot be negative");
                            return;
                        }
                    }
                } catch (NumberFormatException e) {
                    caloriesInput.setError("Invalid number");
                    return;
                }

                // All validations passed, dismiss dialog
                dialog.dismiss();

                // Show progress dialog
                android.app.ProgressDialog progressDialog = new android.app.ProgressDialog(MenuManagementActivity.this);
                progressDialog.setMessage("Adding new menu item...");
                progressDialog.setCancelable(false);
                progressDialog.show();

                // Create a new menu item with validated data
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

                // Save to database with progress dialog
                saveNewMenuItem(newItem, progressDialog);
            });

            // Setup cancel button
            dialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE).setOnClickListener(v -> dialog.dismiss());

        } catch (Exception e) {
            Log.e(TAG, "Error showing add item dialog", e);
            Toast.makeText(this, "Error creating dialog: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Show a dialog to select from standard menu categories
     */
    private void showCategorySelectionDialog(android.widget.EditText categoryInput) {
        try {
            final String[] categories = {
                    MenuItem.CATEGORY_STARTERS,
                    MenuItem.CATEGORY_MAIN,
                    MenuItem.CATEGORY_DESSERTS,
                    MenuItem.CATEGORY_BEVERAGES,
                    "Other"
            };

            new android.app.AlertDialog.Builder(this)
                    .setTitle("Select Category")
                    .setItems(categories, (dialog, which) -> {
                        if (which < categories.length - 1) {
                            // Use standard category
                            categoryInput.setText(categories[which]);
                        } else {
                            // "Other" selected, allow custom input
                            categoryInput.setText("");
                            categoryInput.requestFocus();
                        }
                    })
                    .show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing category selection", e);
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
     * Save a new menu item to the database and update the UI
     */
    private void saveNewMenuItem(MenuItem newItem, android.app.ProgressDialog progressDialog) {
        // Set a timeout flag for long-running operations
        final boolean[] saveCompleted = {false};

        // Set a timeout handler to dismiss dialog if operation takes too long
        new android.os.Handler().postDelayed(() -> {
            if (!saveCompleted[0] && progressDialog.isShowing()) {
                try {
                    progressDialog.dismiss();
                    Toast.makeText(MenuManagementActivity.this,
                            "Operation is taking longer than expected. Please check menu list.",
                            Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    Log.e(TAG, "Error dismissing timeout dialog", e);
                }
            }
        }, 6000); // 6-second timeout

        new Thread(() -> {
            try {
                // Get database instance
                AppDatabase db = AppDatabase.getDatabase(this);
                if (db == null) {
                    throw new Exception("Could not access menu database");
                }

                // Check if a menu item with the same name already exists
                List<MenuItem> existingItems = db.menuItemDao().searchByName(newItem.getName());
                if (existingItems != null && !existingItems.isEmpty()) {
                    saveCompleted[0] = true;

                    // Menu item with same name exists
                    runOnUiThread(() -> {
                        try {
                            if (progressDialog.isShowing()) {
                                progressDialog.dismiss();
                            }

                            // Show confirmation dialog to update or add as new
                            new android.app.AlertDialog.Builder(this)
                                    .setTitle("Similar Item Exists")
                                    .setMessage("A menu item with the name '" + newItem.getName() + "' already exists. What would you like to do?")
                                    .setPositiveButton("Add as New", (dialog, which) -> {
                                        // Add as a new item anyway
                                        addNewMenuItemToDatabase(newItem);
                                    })
                                    .setNegativeButton("Cancel", null)
                                    .show();
                        } catch (Exception e) {
                            Log.e(TAG, "Error handling existing item", e);
                        }
                    });
                    return;
                }

                // No duplicate found, proceed with insertion
                long id = db.menuItemDao().insert(newItem);
                if (id <= 0) {
                    throw new Exception("Failed to insert menu item");
                }

                newItem.setItem_id((int) id);
                saveCompleted[0] = true;

                runOnUiThread(() -> {
                    try {
                        if (progressDialog.isShowing()) {
                            progressDialog.dismiss();
                        }

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
                saveCompleted[0] = true;
                Log.e(TAG, "Error saving new menu item", e);

                // Try direct SQL as fallback
                try {
                    AppDatabase db = AppDatabase.getDatabase(this);
                    if (db != null && db.getSQLiteDatabase() != null) {
                        // Insert using direct SQL
                        db.getSQLiteDatabase().execSQL(
                                "INSERT INTO menu_items (name, description, price, availability, category, prepTimeMinutes, calories, spiceLevel, imageResourceId) " +
                                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                                new Object[]{
                                        newItem.getName(),
                                        newItem.getDescription(),
                                        newItem.getPrice(),
                                        1, // True for availability
                                        newItem.getCategory(),
                                        newItem.getPrepTimeMinutes(),
                                        newItem.getCalories(),
                                        newItem.getSpiceLevel(),
                                        newItem.getImageResourceId()
                                }
                        );

                        // Successfully added item
                        runOnUiThread(() -> {
                            if (progressDialog.isShowing()) {
                                progressDialog.dismiss();
                            }

                            // Reload menu items to include the new one
                            loadMenuItems();
                            Toast.makeText(this, "Menu item added successfully", Toast.LENGTH_SHORT).show();
                        });
                        return;
                    }
                } catch (Exception sqlEx) {
                    Log.e(TAG, "Direct SQL insertion also failed", sqlEx);
                }

                runOnUiThread(() -> {
                    if (progressDialog.isShowing()) {
                        progressDialog.dismiss();
                    }
                    showErrorDialog("Error Adding Item", "Could not save menu item: " + e.getMessage());
                });
            }
        }).start();
    }

    /**
     * Add a new menu item to the database as a separate function
     * for reuse in different scenarios
     */
    private void addNewMenuItemToDatabase(MenuItem newItem) {
        // Show progress dialog
        android.app.ProgressDialog progressDialog = new android.app.ProgressDialog(this);
        progressDialog.setMessage("Adding menu item...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        new Thread(() -> {
            try {
                AppDatabase db = AppDatabase.getDatabase(this);
                long id = db.menuItemDao().insert(newItem);
                newItem.setItem_id((int) id);

                runOnUiThread(() -> {
                    if (progressDialog.isShowing()) {
                        progressDialog.dismiss();
                    }

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
                    if (progressDialog.isShowing()) {
                        progressDialog.dismiss();
                    }
                    showErrorDialog("Error Adding Item", "Could not save menu item: " + e.getMessage());
                });
            }
        }).start();
    }

    /**
     * Show an error dialog with details
     */
    private void showErrorDialog(String title, String message) {
        try {
            new android.app.AlertDialog.Builder(this)
                    .setTitle(title)
                    .setMessage(message)
                    .setPositiveButton("OK", null)
                    .setNegativeButton("Contact Support", (dialog, which) -> {
                        Toast.makeText(this, "Please contact support at support@finedine.com",
                                Toast.LENGTH_LONG).show();
                    })
                    .show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing error dialog", e);
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Load menu items directly from Firebase Firestore
     */
    private void loadMenuItemsFromFirebase() {
        try {
            if (db == null) {
                db = FirebaseFirestore.getInstance();
            }

            // Show progress
            Toast.makeText(this, "Syncing with Firebase...", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Loading menu items from Firebase");

            // Query menu_items collection in Firestore
            db.collection("menu_items")
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            List<MenuItem> firebaseItems = new ArrayList<>();

                            for (QueryDocumentSnapshot document : task.getResult()) {
                                try {
                                    MenuItem item = new MenuItem();

                                    // Set fields from document
                                    item.name = document.getString("name");
                                    item.description = document.getString("description");

                                    // Handle price - it could be a Double or Long
                                    if (document.contains("price")) {
                                        if (document.get("price") instanceof Double) {
                                            item.price = document.getDouble("price");
                                        } else if (document.get("price") instanceof Long) {
                                            item.price = document.getLong("price");
                                        }
                                    }

                                    // Handle availability
                                    if (document.contains("availability")) {
                                        item.availability = document.getBoolean("availability");
                                    } else {
                                        item.availability = true; // Default to true
                                    }

                                    // Set category
                                    item.category = document.getString("category");
                                    if (item.category == null) {
                                        item.category = "Main Course"; // Default category
                                    }

                                    // Handle numeric fields
                                    if (document.contains("prepTimeMinutes")) {
                                        if (document.get("prepTimeMinutes") instanceof Long) {
                                            item.prepTimeMinutes = Math.toIntExact(document.getLong("prepTimeMinutes"));
                                        }
                                    } else {
                                        item.prepTimeMinutes = 15; // Default prep time
                                    }

                                    if (document.contains("calories")) {
                                        if (document.get("calories") instanceof Long) {
                                            item.calories = Math.toIntExact(document.getLong("calories"));
                                        }
                                    } else {
                                        item.calories = 0; // Default calories
                                    }

                                    // Set spice level
                                    item.spiceLevel = document.getString("spiceLevel");
                                    if (item.spiceLevel == null) {
                                        item.spiceLevel = "None"; // Default spice level
                                    }

                                    // Set image resource
                                    item.imageResourceId = R.drawable.placeholder_food;

                                    // Validate item before adding
                                    if (item.name != null && !item.name.isEmpty()) {
                                        firebaseItems.add(item);
                                        Log.d(TAG, "Added Firebase menu item: " + item.name);
                                    }
                                } catch (Exception e) {
                                    Log.e(TAG, "Error parsing Firebase menu item: " + e.getMessage());
                                }
                            }

                            // Update adapter with Firebase items if any were found
                            if (!firebaseItems.isEmpty()) {
                                Log.d(TAG, "Loaded " + firebaseItems.size() + " menu items from Firebase");
                                runOnUiThread(() -> updateAdapterItems(firebaseItems));
                            } else {
                                Log.d(TAG, "No menu items found in Firebase");
                            }
                        } else {
                            Log.e(TAG, "Error loading menu items from Firebase", task.getException());
                        }
                    });
        } catch (Exception e) {
            Log.e(TAG, "Error in loadMenuItemsFromFirebase", e);
        }
    }

    /**
     * Show an error message with toast
     */
    private void showErrorMessage(String message) {
        try {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing toast: " + e.getMessage(), e);
        }
    }

    /**
     * Show detailed information about a menu item
     */
    private void showMenuItemDetails(MenuItem item) {
        try {
            if (item == null) {
                Log.e(TAG, "Attempted to show details for null menu item");
                return;
            }

            Log.d(TAG, "Opening details for menu item: " + item.getName() + " (ID: " + item.getItem_id() + ")");
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
        } else {
            Log.e(TAG, "Search edit text not found in layout");
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

    /**
     * Set up the menu button click handler properly to prevent crashes
     */
    private void setupMenuButtonClickHandler() {
        try {
            // First, find the top menuButton that would be in the toolbar
            ImageView menuButton = findViewById(R.id.menuButton);
            if (menuButton != null) {
                Log.d(TAG, "Found menu button in toolbar, setting up click listener");
                menuButton.setOnClickListener(v -> {
                    Log.d(TAG, "Menu button clicked");
                    Toast.makeText(this, "Menu options", Toast.LENGTH_SHORT).show();
                    // Show a simple menu options popup or dialog instead of trying to open drawer
                    showSimpleMenuOptions(v);
                });
            }

            // Also find any bottom navigation view and handle its menu item
            BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigation);
            if (bottomNavigationView != null) {
                Log.d(TAG, "Found bottom navigation, setting up on item selected listener");
                bottomNavigationView.setOnItemSelectedListener(item -> {
                    int itemId = item.getItemId();
                    Log.d(TAG, "Bottom navigation item selected: " + item.getTitle());

                    if (itemId == R.id.navigation_menu) {
                        // Already on menu screen, do nothing
                        return true;
                    }

                    // Handle other navigation items through the BaseActivity navigation
                    return false;
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting up menu button handler", e);
        }
    }

    /**
     * Show a simple menu options popup instead of trying to use the drawer
     */
    private void showSimpleMenuOptions(View anchorView) {
        try {
            android.widget.PopupMenu popup = new android.widget.PopupMenu(this, anchorView);

            // Add menu items programmatically
            popup.getMenu().add("Search");
            popup.getMenu().add("Sort");
            popup.getMenu().add("Filter");
            popup.getMenu().add("Sync to Firebase");
            popup.getMenu().add("Refresh");
            popup.getMenu().add("Settings");

            popup.setOnMenuItemClickListener(item -> {
                String title = item.getTitle().toString();
                if ("Sync to Firebase".equals(title)) {
                    showSyncOptions();
                    return true;
                }
                Toast.makeText(this, "Selected: " + item.getTitle(), Toast.LENGTH_SHORT).show();
                return true;
            });

            popup.show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing menu options", e);
            Toast.makeText(this, "Menu options unavailable", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Show options for synchronizing menu items with Firebase Realtime Database
     */
    private void showSyncOptions() {
        try {
            if (menuFirebaseIntegration == null) {
                Toast.makeText(this, "Firebase integration not available", Toast.LENGTH_SHORT).show();
                return;
            }

            String[] options = {
                    "Upload current menu items",
                    "Upload single selected item",
                    "Sync and replace all items in Firebase"
            };

            new android.app.AlertDialog.Builder(this)
                    .setTitle("Firebase Sync Options")
                    .setItems(options, (dialog, which) -> {
                        switch (which) {
                            case 0:
                                uploadAllMenuItemsToFirebase();
                                break;
                            case 1:
                                showItemSelectionForUpload();
                                break;
                            case 2:
                                confirmSyncAndReplaceAllItems();
                                break;
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing sync options", e);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Upload all current menu items to Firebase
     */
    private void uploadAllMenuItemsToFirebase() {
        try {
            // Get current menu items
            List<MenuItem> items = adapter.getItems();
            if (items.isEmpty()) {
                Toast.makeText(this, "No menu items to upload", Toast.LENGTH_SHORT).show();
                return;
            }

            // Show progress dialog
            android.app.ProgressDialog progressDialog = new android.app.ProgressDialog(this);
            progressDialog.setTitle("Uploading Menu Items");
            progressDialog.setMessage("Uploading " + items.size() + " items to Firebase...");
            progressDialog.setCancelable(false);
            progressDialog.show();

            // Convert list to array
            MenuItem[] itemsArray = items.toArray(new MenuItem[0]);

            // Upload items
            menuFirebaseIntegration.uploadAllMenuItems(itemsArray, new MenuFirebaseIntegration.MenuOperationListener() {
                @Override
                public void onSuccess(MenuItem menuItem) {
                    runOnUiThread(() -> {
                        if (progressDialog.isShowing()) {
                            progressDialog.dismiss();
                        }
                        Toast.makeText(MenuManagementActivity.this,
                                "Successfully uploaded all menu items to Firebase",
                                Toast.LENGTH_LONG).show();
                    });
                }

                @Override
                public void onFailure(String errorMessage) {
                    runOnUiThread(() -> {
                        if (progressDialog.isShowing()) {
                            progressDialog.dismiss();
                        }
                        showErrorDialog("Upload Error", "Error uploading menu items: " + errorMessage);
                    });
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error uploading menu items", e);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Show dialog to select a menu item for upload
     */
    private void showItemSelectionForUpload() {
        try {
            List<MenuItem> items = adapter.getItems();
            if (items.isEmpty()) {
                Toast.makeText(this, "No menu items to upload", Toast.LENGTH_SHORT).show();
                return;
            }

            // Create list of item names for selection
            String[] itemNames = new String[items.size()];
            for (int i = 0; i < items.size(); i++) {
                itemNames[i] = items.get(i).getName();
            }

            new android.app.AlertDialog.Builder(this)
                    .setTitle("Select Menu Item to Upload")
                    .setItems(itemNames, (dialog, which) -> {
                        uploadSingleMenuItemToFirebase(items.get(which));
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing item selection dialog", e);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Upload a single menu item to Firebase
     */
    private void uploadSingleMenuItemToFirebase(MenuItem menuItem) {
        try {
            // Show progress dialog
            android.app.ProgressDialog progressDialog = new android.app.ProgressDialog(this);
            progressDialog.setTitle("Uploading Menu Item");
            progressDialog.setMessage("Uploading " + menuItem.getName() + " to Firebase...");
            progressDialog.setCancelable(false);
            progressDialog.show();

            // Upload item
            menuFirebaseIntegration.uploadMenuItem(menuItem, new MenuFirebaseIntegration.MenuOperationListener() {
                @Override
                public void onSuccess(MenuItem uploadedItem) {
                    runOnUiThread(() -> {
                        if (progressDialog.isShowing()) {
                            progressDialog.dismiss();
                        }
                        Toast.makeText(MenuManagementActivity.this,
                                "Successfully uploaded " + uploadedItem.getName() + " to Firebase",
                                Toast.LENGTH_LONG).show();
                    });
                }

                @Override
                public void onFailure(String errorMessage) {
                    runOnUiThread(() -> {
                        if (progressDialog.isShowing()) {
                            progressDialog.dismiss();
                        }
                        showErrorDialog("Upload Error", "Error uploading " + menuItem.getName() + ": " + errorMessage);
                    });
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error uploading menu item", e);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Confirm and sync all menu items, replacing everything in Firebase
     */
    private void confirmSyncAndReplaceAllItems() {
        try {
            new android.app.AlertDialog.Builder(this)
                    .setTitle("Warning: Full Sync")
                    .setMessage("This will DELETE all existing menu items in Firebase and replace them with the current items. This cannot be undone. Continue?")
                    .setPositiveButton("Sync & Replace", (dialog, which) -> {
                        syncAndReplaceAllItems();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing confirmation dialog", e);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Sync and replace all menu items in Firebase
     */
    private void syncAndReplaceAllItems() {
        try {
            // Get current menu items
            List<MenuItem> items = adapter.getItems();
            if (items.isEmpty()) {
                Toast.makeText(this, "No menu items to sync", Toast.LENGTH_SHORT).show();
                return;
            }

            // Show progress dialog
            android.app.ProgressDialog progressDialog = new android.app.ProgressDialog(this);
            progressDialog.setTitle("Syncing Menu Items");
            progressDialog.setMessage("Syncing " + items.size() + " items to Firebase...");
            progressDialog.setCancelable(false);
            progressDialog.show();

            // Convert list to array
            MenuItem[] itemsArray = items.toArray(new MenuItem[0]);

            // Sync items
            menuFirebaseIntegration.synchronizeMenuItems(itemsArray, new MenuFirebaseIntegration.MenuOperationListener() {
                @Override
                public void onSuccess(MenuItem menuItem) {
                    runOnUiThread(() -> {
                        if (progressDialog.isShowing()) {
                            progressDialog.dismiss();
                        }
                        Toast.makeText(MenuManagementActivity.this,
                                "Successfully synced all menu items to Firebase",
                                Toast.LENGTH_LONG).show();
                    });
                }

                @Override
                public void onFailure(String errorMessage) {
                    runOnUiThread(() -> {
                        if (progressDialog.isShowing()) {
                            progressDialog.dismiss();
                        }
                        showErrorDialog("Sync Error", "Error syncing menu items: " + errorMessage);
                    });
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error syncing menu items", e);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}