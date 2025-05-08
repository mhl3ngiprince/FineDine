package com.finedine.rms;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class InventoryActivity extends BaseActivity {
    private static final String TAG = "InventoryActivity";
    private RecyclerView inventoryRecyclerView;
    private InventoryAdapter inventoryAdapter;
    private final List<Inventory> inventoryItems = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            // Setup modern navigation panel
            setupModernNavigationPanel("Inventory Management", R.layout.activity_inventory);

            // Initialize RecyclerView with null checks
            inventoryRecyclerView = findViewById(R.id.inventoryRecyclerView);
            if (inventoryRecyclerView == null) {
                Log.e(TAG, "Failed to find RecyclerView in layout");
                showErrorDialog("Layout Error", "Could not initialize inventory view. Please contact support.");
                return;
            }

            try {
                inventoryRecyclerView.setLayoutManager(new LinearLayoutManager(this));
                inventoryAdapter = new InventoryAdapter(inventoryItems, this::onItemSelected);
                inventoryRecyclerView.setAdapter(inventoryAdapter);
            } catch (Exception e) {
                Log.e(TAG, "Error setting up RecyclerView", e);
                showErrorDialog("Setup Error", "Could not initialize inventory list. Please try again.");
            }

            // Initialize FloatingActionButton with null check
            FloatingActionButton fabAddItem = findViewById(R.id.fabAddItem);
            if (fabAddItem != null) {
                fabAddItem.setOnClickListener(v -> showAddItemDialog());
            } else {
                Log.e(TAG, "Add item button not found in layout");
                Toast.makeText(this, "Some features may be limited", Toast.LENGTH_SHORT).show();
            }

            // Load inventory data
            loadInventory();
        } catch (Exception e) {
            Log.e(TAG, "Error initializing InventoryActivity", e);
            Toast.makeText(this, "Error initializing inventory screen", Toast.LENGTH_SHORT).show();
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private void loadInventory() {
        try {
            AppDatabase db = AppDatabase.getDatabase(this);
            if (db == null) {
                Log.e(TAG, "Failed to get database instance");
                Toast.makeText(this, "Error accessing inventory database", Toast.LENGTH_SHORT).show();
                return;
            }

            // Show loading indicator
            Toast.makeText(this, "Loading inventory items...", Toast.LENGTH_SHORT).show();

            new Thread(() -> {
                try {
                    // Get inventory items safely
                    List<Inventory> items = new ArrayList<>();
                    try {
                        items = db.inventoryDao().getAll();
                        Log.d(TAG, "Successfully loaded " + items.size() + " inventory items from database");

                        // Log each item for debugging
                        for (Inventory item : items) {
                            Log.d(TAG, "Loaded item: " + item.item_id + " - " + item.item_name +
                                    " (Qty: " + item.quantity_in_stock + ", Threshold: " +
                                    item.reorder_threshold + ")");
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Database error when loading inventory", e);
                    }

                    // Final copy for UI thread
                    final List<Inventory> finalItems = items;

                    runOnUiThread(() -> {
                        try {
                            inventoryItems.clear();
                            inventoryItems.addAll(finalItems);

                            if (inventoryAdapter != null) {
                                inventoryAdapter.notifyDataSetChanged();
                            } else {
                                Log.e(TAG, "Inventory adapter is null, recreating");
                                inventoryAdapter = new InventoryAdapter(inventoryItems, this::onItemSelected);

                                if (inventoryRecyclerView != null) {
                                    inventoryRecyclerView.setAdapter(inventoryAdapter);
                                } else {
                                    Log.e(TAG, "RecyclerView is null, can't set adapter");
                                }
                            }

                            // Update UI based on inventory status
                            if (inventoryItems.isEmpty()) {
                                Toast.makeText(this, "No inventory items found. Add some items!", Toast.LENGTH_SHORT).show();
                            } else {
                                checkLowStockItems();
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error updating inventory UI", e);
                        }
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Error in inventory loading thread", e);
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Error loading inventory data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                }
            }).start();
        } catch (Exception e) {
            Log.e(TAG, "Error starting inventory loading", e);
            Toast.makeText(this, "Error loading inventory data", Toast.LENGTH_SHORT).show();
        }
    }

    private void checkLowStockItems() {
        for (Inventory item : inventoryItems) {
            if (item.quantity_in_stock < item.reorder_threshold) {
                showLowStockAlert(item);
                break; // Just show first low stock item for demo
            }
        }
    }

    private void showLowStockAlert(Inventory item) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Low Stock Alert");
        builder.setMessage(item.item_name + " is below reorder threshold (" +
                item.quantity_in_stock + "/" + item.reorder_threshold + ")");

        builder.setPositiveButton("Reorder", (dialog, which) -> showReorderDialog(item)).setNegativeButton("Later", null);
        builder.show();
    }

    private void showReorderDialog(Inventory item) {
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Reorder " + item.item_name);

            View view = LayoutInflater.from(this).inflate(R.layout.dialog_reorder, null);
            if (view == null) {
                Log.e(TAG, "Failed to inflate reorder dialog layout");
                Toast.makeText(this, "Could not create reorder dialog", Toast.LENGTH_SHORT).show();
                return;
            }

            EditText quantityInput = view.findViewById(R.id.quantityInput);
            if (quantityInput == null) {
                Log.e(TAG, "Could not find quantity input in reorder dialog");
                Toast.makeText(this, "Error in reorder dialog", Toast.LENGTH_SHORT).show();
                return;
            }

            quantityInput.setText(String.valueOf(item.reorder_threshold * 2));

            builder.setView(view);
            builder.setPositiveButton("Submit", (dialog, which) -> {
                try {
                    String quantityStr = quantityInput.getText().toString();
                    if (quantityStr.isEmpty()) {
                        Toast.makeText(this, "Please enter a quantity", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    double quantity = Double.parseDouble(quantityStr);
                    // In real app, this would send to supplier
                    Toast.makeText(this, "Order placed for " + quantity + " " + item.item_name,
                            Toast.LENGTH_SHORT).show();
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "Please enter a valid quantity", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Toast.makeText(this, "Error processing order", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error in reorder dialog", e);
                }
            });
            builder.setNegativeButton("Cancel", null);
            builder.show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing reorder dialog", e);
            Toast.makeText(this, "Could not open reorder dialog", Toast.LENGTH_SHORT).show();
        }
    }

    private void onItemSelected(Inventory item) {
        try {
            // Show edit dialog
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Edit " + item.item_name);

            View view = LayoutInflater.from(this).inflate(R.layout.dialog_edit_inventory, null);
            if (view == null) {
                Log.e(TAG, "Failed to inflate dialog_edit_inventory layout");
                Toast.makeText(this, "Dialog layout error. Please contact support.", Toast.LENGTH_SHORT).show();
                return;
            }

            EditText quantityInput = view.findViewById(R.id.quantityInput);
            EditText thresholdInput = view.findViewById(R.id.thresholdInput);

            if (quantityInput == null || thresholdInput == null) {
                Log.e(TAG, "One or more required fields missing in the layout");
                Toast.makeText(this, "Dialog layout error. Please contact support.", Toast.LENGTH_SHORT).show();
                return;
            }

            quantityInput.setText(String.valueOf(item.quantity_in_stock));
            thresholdInput.setText(String.valueOf(item.reorder_threshold));

            builder.setView(view);
            builder.setPositiveButton("Save", (dialog, which) -> {
                try {
                    // Handle potential errors in input parsing
                    String quantityStr = quantityInput.getText().toString();
                    String thresholdStr = thresholdInput.getText().toString();

                    if (quantityStr.isEmpty() || thresholdStr.isEmpty()) {
                        Toast.makeText(this, "Please enter valid values", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    item.quantity_in_stock = Double.parseDouble(quantityStr);
                    item.reorder_threshold = Double.parseDouble(thresholdStr);
                    item.last_updated = System.currentTimeMillis();

                    new Thread(() -> {
                        try {
                            AppDatabase db = AppDatabase.getDatabase(this);
                            if (db != null) {
                                db.inventoryDao().update(item);
                                runOnUiThread(() -> {
                                    loadInventory();
                                    Toast.makeText(this, "Inventory updated", Toast.LENGTH_SHORT).show();
                                });
                            } else {
                                runOnUiThread(() -> {
                                    Toast.makeText(this, "Database error", Toast.LENGTH_SHORT).show();
                                });
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error updating inventory item", e);
                            runOnUiThread(() -> {
                                Toast.makeText(this, "Error updating inventory: " + e.getMessage(),
                                        Toast.LENGTH_SHORT).show();
                            });
                        }
                    }).start();
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "Please enter valid numbers", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Log.e(TAG, "Error saving inventory changes", e);
                    Toast.makeText(this, "Error saving changes: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
            });
            builder.setNegativeButton("Cancel", null);
            builder.show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing edit dialog", e);
            Toast.makeText(this, "Could not open edit dialog", Toast.LENGTH_SHORT).show();
        }
    }

    private void showAddItemDialog() {
        try {
            // Create dialog for adding a new inventory item
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Add New Inventory Item");

            // Inflate custom layout - check for null to prevent crashes
            View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_inventory, null);
            if (view == null) {
                Log.e(TAG, "Failed to inflate dialog_add_inventory layout");
                Toast.makeText(this, "Error loading dialog. Please try again.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Get references to UI elements with null checks
            EditText nameInput = view.findViewById(R.id.addNameInput);
            EditText quantityInput = view.findViewById(R.id.addQuantityInput);
            EditText thresholdInput = view.findViewById(R.id.addThresholdInput);

            if (nameInput == null || quantityInput == null || thresholdInput == null) {
                Log.e(TAG, "One or more required fields missing in the layout");
                Toast.makeText(this, "Dialog layout error. Please contact support.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Set default values
            quantityInput.setText("0.0");
            thresholdInput.setText("10.0");

            builder.setView(view);

            // Create dialog but don't set auto-dismiss on positive button
            AlertDialog dialog = builder.setPositiveButton("Add", null)
                    .setNegativeButton("Cancel", null)
                    .create();

            // Show the dialog first
            dialog.show();

            // Then override the positive button to avoid auto-dismiss on validation errors
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                // Validate inputs
                String itemName = nameInput.getText().toString().trim();
                if (itemName.isEmpty()) {
                    nameInput.setError("Item name is required");
                    return; // Don't dismiss dialog
                }

                // Validate numeric inputs
                double quantity;
                double threshold;
                try {
                    quantity = Double.parseDouble(quantityInput.getText().toString());
                    if (quantity < 0) {
                        quantityInput.setError("Quantity cannot be negative");
                        return;
                    }
                } catch (NumberFormatException e) {
                    quantityInput.setError("Please enter a valid number");
                    return;
                }

                try {
                    threshold = Double.parseDouble(thresholdInput.getText().toString());
                    if (threshold < 0) {
                        thresholdInput.setError("Threshold cannot be negative");
                        return;
                    }
                } catch (NumberFormatException e) {
                    thresholdInput.setError("Please enter a valid number");
                    return;
                }

                // All validation passed, dismiss dialog
                dialog.dismiss();

                // Show progress dialog
                AlertDialog progressDialog = new AlertDialog.Builder(this)
                        .setMessage("Adding " + itemName + " to inventory...")
                        .setCancelable(false)
                        .create();
                progressDialog.show();

                // Create new inventory item
                final Inventory newItem = new Inventory();
                newItem.item_name = itemName;
                newItem.quantity_in_stock = quantity;
                newItem.reorder_threshold = threshold;
                newItem.last_updated = System.currentTimeMillis();

                // Initialize database and save in background
                AppDatabase db = AppDatabase.getDatabase(this);
                if (db == null) {
                    progressDialog.dismiss();
                    showErrorDialog("Database Error", "Unable to connect to inventory database");
                    return;
                }

                // Save to database
                new Thread(() -> {
                    try {
                        // Check if item already exists
                        Inventory existingItem = null;
                        try {
                            existingItem = db.inventoryDao().getItemByName(itemName);
                        } catch (Exception e) {
                            Log.e(TAG, "Error checking for existing item", e);
                            // Continue with null existingItem
                        }

                        if (existingItem != null) {
                            // Update existing item instead of creating new one
                            final Inventory finalExistingItem = existingItem;
                            runOnUiThread(() -> {
                                // Dismiss progress dialog first
                                progressDialog.dismiss();

                                AlertDialog.Builder updateDialog = new AlertDialog.Builder(this);
                                updateDialog.setTitle("Item Already Exists");
                                updateDialog.setMessage("\"" + itemName + "\" already exists with " +
                                        finalExistingItem.quantity_in_stock + " in stock. Do you want to update the quantity?");
                                updateDialog.setPositiveButton("Update", (d, w) -> {
                                    // Show progress dialog for update
                                    AlertDialog updateProgressDialog = new AlertDialog.Builder(this)
                                            .setMessage("Updating inventory...")
                                            .setCancelable(false)
                                            .create();
                                    updateProgressDialog.show();

                                    // Update the existing item's quantity
                                    new Thread(() -> {
                                        try {
                                            finalExistingItem.quantity_in_stock += quantity;
                                            finalExistingItem.last_updated = System.currentTimeMillis();
                                            db.inventoryDao().update(finalExistingItem);

                                            runOnUiThread(() -> {
                                                updateProgressDialog.dismiss();
                                                loadInventory();
                                                Toast.makeText(this, "Inventory updated successfully", Toast.LENGTH_SHORT).show();
                                            });
                                        } catch (Exception e) {
                                            Log.e(TAG, "Error updating inventory item", e);
                                            runOnUiThread(() -> {
                                                updateProgressDialog.dismiss();
                                                showErrorDialog("Update Error", "Could not update inventory: " + e.getMessage());
                                            });
                                        }
                                    }).start();
                                });
                                updateDialog.setNegativeButton("Cancel", null);
                                updateDialog.show();
                            });
                        } else {
                            // Insert new item with timeout protection
                            boolean[] insertCompleted = {false};

                            // Create a timeout handler
                            new android.os.Handler(getMainLooper()).postDelayed(() -> {
                                if (!insertCompleted[0]) {
                                    Log.w(TAG, "Insert operation taking too long, may have failed");
                                    runOnUiThread(() -> {
                                        if (progressDialog.isShowing()) {
                                            progressDialog.dismiss();
                                            Toast.makeText(InventoryActivity.this,
                                                    "Operation is taking longer than expected. Please check inventory list.",
                                                    Toast.LENGTH_LONG).show();
                                        }
                                    });
                                }
                            }, 5000); // 5-second timeout

                            try {
                                long itemId = db.inventoryDao().insert(newItem);
                                Log.d(TAG, "Added new inventory item with ID: " + itemId);
                                insertCompleted[0] = true;

                                // Verify the item was inserted by trying to retrieve it
                                Inventory checkItem = null;
                                try {
                                    checkItem = db.inventoryDao().getItemById((int) itemId);
                                } catch (Exception e) {
                                    Log.e(TAG, "Failed to verify item after insert", e);
                                }

                                if (checkItem == null) {
                                    throw new Exception("Failed to verify newly added inventory item");
                                }

                                Log.d(TAG, "Verified new inventory item: " + checkItem.item_name);
                                Log.d(TAG, "New item quantity: " + checkItem.quantity_in_stock);
                                Log.d(TAG, "New item threshold: " + checkItem.reorder_threshold);

                                runOnUiThread(() -> {
                                    progressDialog.dismiss();
                                    loadInventory();
                                    Toast.makeText(this, "Item added successfully", Toast.LENGTH_SHORT).show();
                                });
                            } catch (Exception e) {
                                insertCompleted[0] = true; // Mark as completed even though it failed
                                Log.e(TAG, "Error inserting inventory item", e);

                                // Try a more direct insert approach as fallback
                                try {
                                    db.getSQLiteDatabase().execSQL(
                                            "INSERT INTO inventory (item_name, quantity_in_stock, reorder_threshold, last_updated) " +
                                                    "VALUES (?, ?, ?, ?)",
                                            new Object[]{itemName, quantity, threshold, System.currentTimeMillis()}
                                    );
                                    Log.d(TAG, "Successfully added item using direct SQL");

                                    runOnUiThread(() -> {
                                        progressDialog.dismiss();
                                        loadInventory();
                                        Toast.makeText(this, "Item added successfully", Toast.LENGTH_SHORT).show();
                                    });
                                } catch (Exception e2) {
                                    Log.e(TAG, "Both insert methods failed", e2);
                                    runOnUiThread(() -> {
                                        progressDialog.dismiss();
                                        showErrorDialog("Add Error", "Could not add item to inventory database");
                                    });
                                }
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error saving inventory item to database: " + e.getMessage(), e);
                        runOnUiThread(() -> {
                            if (progressDialog.isShowing()) {
                                progressDialog.dismiss();
                            }
                            showErrorDialog("Database Error", "Error adding inventory item: " + e.getMessage());
                        });
                    }
                }).start();
            });
        } catch (Exception e) {
            Log.e(TAG, "Error showing add item dialog: " + e.getMessage(), e);
            showErrorDialog("Dialog Error", "Could not show the add item dialog");
        }
    }

    /**
     * Show an error dialog with details and option to retry
     */
    private void showErrorDialog(String title, String message) {
        try {
            new AlertDialog.Builder(this)
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
}