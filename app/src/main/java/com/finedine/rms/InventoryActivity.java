package com.finedine.rms;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.finedine.rms.firebase.FirebaseDao;
import com.finedine.rms.firebase.FirebaseDataLoader;

import java.util.ArrayList;
import java.util.List;

public class InventoryActivity extends BaseActivity {
    private static final String TAG = "InventoryActivity";
    private RecyclerView inventoryRecyclerView;
    private InventoryAdapter inventoryAdapter;
    private final List<Inventory> inventoryItems = new ArrayList<>();
    private FirebaseFirestore db;
    private ListenerRegistration inventoryListener;
    private TextView tvTotalItems;
    private TextView tvLowStockItems;
    private TextView tvOutOfStockItems;
    private FirebaseDao firebaseDao;
    private FirebaseDataLoader firebaseDataLoader;

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

            // Initialize stat text views
            tvTotalItems = findViewById(R.id.tvTotalItems);
            tvLowStockItems = findViewById(R.id.tvLowStockItems);
            tvOutOfStockItems = findViewById(R.id.tvOutOfStockItems);

            // Initialize FloatingActionButton with null check
            FloatingActionButton fabAddItem = findViewById(R.id.fabAddItem);
            if (fabAddItem != null) {
                fabAddItem.setOnClickListener(v -> showAddItemDialog());
            } else {
                Log.e(TAG, "Add item button not found in layout");
                Toast.makeText(this, "Some features may be limited", Toast.LENGTH_SHORT).show();
            }

            // Set up View Inventory Directory button
            Button btnViewInventoryDirectory = findViewById(R.id.btnViewInventoryDirectory);
            if (btnViewInventoryDirectory != null) {
                btnViewInventoryDirectory.setOnClickListener(v -> {
                    Intent intent = new Intent(InventoryActivity.this, ViewInventoryActivity.class);
                    startActivity(intent);
                });
            } else {
                Log.e(TAG, "View Inventory Directory button not found in layout");
                Toast.makeText(this, "Some features may be limited", Toast.LENGTH_SHORT).show();
            }

            // Initialize Firebase
            db = FirebaseFirestore.getInstance();

            // Initialize FirebaseDao
            firebaseDao = new FirebaseDao();

            // Initialize filter chips
            setupFilterChips();

            // Load inventory data
            loadInventory();

            // Set up real-time listener for inventory changes
            setupInventoryListener();

            // Setup options menu for sample data
            setupOptionsMenu();

            // Add a sample data button
            addSampleDataButton();
        } catch (Exception e) {
            Log.e(TAG, "Error initializing InventoryActivity", e);
            Toast.makeText(this, "Error initializing inventory screen", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupFilterChips() {
        try {
            // Set up the filter chips
            Chip chipAllItems = findViewById(R.id.chipAllItems);
            Chip chipLowStock = findViewById(R.id.chipLowStock);
            Chip chipOutOfStock = findViewById(R.id.chipOutOfStock);

            if (chipAllItems != null) {
                chipAllItems.setOnClickListener(v -> {
                    filterInventoryItems("all");
                });
            }

            if (chipLowStock != null) {
                chipLowStock.setOnClickListener(v -> {
                    filterInventoryItems("low");
                });
            }

            if (chipOutOfStock != null) {
                chipOutOfStock.setOnClickListener(v -> {
                    filterInventoryItems("out");
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting up filter chips", e);
        }
    }

    private void filterInventoryItems(String filter) {
        List<Inventory> filteredItems = new ArrayList<>();

        switch (filter) {
            case "low":
                for (Inventory item : inventoryItems) {
                    if (item.quantity_in_stock < item.reorder_threshold && item.quantity_in_stock > 0) {
                        filteredItems.add(item);
                    }
                }
                break;

            case "out":
                for (Inventory item : inventoryItems) {
                    if (item.quantity_in_stock == 0) {
                        filteredItems.add(item);
                    }
                }
                break;

            case "all":
            default:
                filteredItems.addAll(inventoryItems);
                break;
        }

        // Update the adapter with the filtered list
        inventoryAdapter = new InventoryAdapter(filteredItems, this::onItemSelected);
        inventoryRecyclerView.setAdapter(inventoryAdapter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Remove Firestore listener when activity is destroyed
        if (inventoryListener != null) {
            inventoryListener.remove();
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    public void loadInventory() {
        try {
            Toast.makeText(this, "Loading inventory items...", Toast.LENGTH_SHORT).show();

            if (firebaseDataLoader == null) {
                firebaseDataLoader = new FirebaseDataLoader();
            }

            runOnUiThread(() -> {
                firebaseDataLoader.loadInventory(new FirebaseDataLoader.DataLoadCallback<Inventory>() {
                    @Override
                    public void onDataLoaded(List<Inventory> items) {
                        updateInventoryUI(items);
                    }

                    @Override
                    public void onError(String errorMessage) {
                        Log.e(TAG, "Firebase error: " + errorMessage);
                        Toast.makeText(InventoryActivity.this, "Error loading inventory: " + errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
            });
        } catch (Exception e) {
            Log.e(TAG, "Error starting inventory loading", e);
            Toast.makeText(this, "Error loading inventory data", Toast.LENGTH_SHORT).show();
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private void updateInventoryUI(List<Inventory> items) {
        runOnUiThread(() -> {
            try {
                inventoryItems.clear();
                inventoryItems.addAll(items);

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
                    updateInventoryStats();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error updating inventory UI", e);
            }
        });
    }

    /**
     * Sets up a real-time listener for inventory changes in Firestore
     */
    private void setupInventoryListener() {
        // Remove any existing listener
        if (inventoryListener != null) {
            inventoryListener.remove();
        }

        // Set up a listener for changes to the inventory collection
        inventoryListener = db.collection("inventory")
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Error listening for inventory updates", e);
                        return;
                    }

                    if (snapshots != null && !snapshots.isEmpty()) {
                        // Reload inventory data when changes occur
                        loadInventory();
                    }
                });
    }

    /**
     * Update the inventory statistics
     */
    private void updateInventoryStats() {
        int totalItems = inventoryItems.size();
        int lowStockItems = 0;
        int outOfStockItems = 0;

        for (Inventory item : inventoryItems) {
            if (item.quantity_in_stock == 0) {
                outOfStockItems++;
            } else if (item.quantity_in_stock < item.reorder_threshold) {
                lowStockItems++;
            }
        }

        if (tvTotalItems != null) {
            tvTotalItems.setText(String.valueOf(totalItems));
        }

        if (tvLowStockItems != null) {
            tvLowStockItems.setText(String.valueOf(lowStockItems));
        }

        if (tvOutOfStockItems != null) {
            tvOutOfStockItems.setText(String.valueOf(outOfStockItems));
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
                            if (firebaseDao == null) {
                                firebaseDao = new FirebaseDao();
                            }

                            firebaseDao.saveInventoryItem(item, new FirebaseDao.FirebaseOperationCallback() {
                                @Override
                                public void onSuccess(String message) {
                                    runOnUiThread(() -> {
                                        loadInventory();
                                        Toast.makeText(InventoryActivity.this, "Inventory updated", Toast.LENGTH_SHORT).show();
                                    });
                                }

                                @Override
                                public void onFailure(String errorMessage) {
                                    runOnUiThread(() -> {
                                        Toast.makeText(InventoryActivity.this, "Failed to update inventory: " + errorMessage, Toast.LENGTH_SHORT).show();
                                    });
                                }
                            });
                        } catch (Exception e) {
                            Log.e(TAG, "Error updating inventory item", e);
                            runOnUiThread(() -> {
                                Toast.makeText(InventoryActivity.this, "Error updating inventory: " + e.getMessage(),
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

                new Thread(() -> {
                    try {
                        if (firebaseDao == null) {
                            firebaseDao = new FirebaseDao();
                        }

                        firebaseDao.saveInventoryItem(newItem, new FirebaseDao.FirebaseOperationCallback() {
                            @Override
                            public void onSuccess(String message) {
                                runOnUiThread(() -> {
                                    progressDialog.dismiss();
                                    loadInventory();
                                    Toast.makeText(InventoryActivity.this, "Item added successfully", Toast.LENGTH_SHORT).show();
                                });
                            }

                            @Override
                            public void onFailure(String errorMessage) {
                                runOnUiThread(() -> {
                                    progressDialog.dismiss();
                                    showErrorDialog("Add Error", "Could not add item to inventory: " + errorMessage);
                                });
                            }
                        });
                    } catch (Exception e) {
                        Log.e(TAG, "Error saving inventory item to Firebase", e);
                        runOnUiThread(() -> {
                            progressDialog.dismiss();
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

    private void updateFirebaseItem(Inventory item) {
        try {
            if (item == null || item.item_name == null) return;

            if (firebaseDao == null) {
                firebaseDao = new FirebaseDao();
            }

            firebaseDao.saveInventoryItem(item, new FirebaseDao.FirebaseOperationCallback() {
                @Override
                public void onSuccess(String message) {
                    Log.d(TAG, "Item updated in Firebase: " + item.item_name);
                }

                @Override
                public void onFailure(String errorMessage) {
                    Log.e(TAG, "Error updating item in Firebase: " + errorMessage);
                    runOnUiThread(() -> Toast.makeText(InventoryActivity.this,
                            "Failed to update item in cloud database", Toast.LENGTH_SHORT).show());
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error in updateFirebaseItem", e);
        }
    }

    private void addItemToFirebase(Inventory item) {
        try {
            if (item == null || item.item_name == null) return;

            if (firebaseDao == null) {
                firebaseDao = new FirebaseDao();
            }

            firebaseDao.saveInventoryItem(item, new FirebaseDao.FirebaseOperationCallback() {
                @Override
                public void onSuccess(String message) {
                    Log.d(TAG, "Item added to Firebase: " + item.item_name);
                }

                @Override
                public void onFailure(String errorMessage) {
                    Log.e(TAG, "Error adding item to Firebase: " + errorMessage);
                    runOnUiThread(() -> {
                        Toast.makeText(InventoryActivity.this,
                                "Note: Item saved locally but not to cloud database", Toast.LENGTH_SHORT).show();
                    });
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error in addItemToFirebase", e);
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

    /**
     * Set up the options menu for this activity
     */
    private void setupOptionsMenu() {
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.inflateMenu(R.menu.inventory_menu);
            toolbar.setOnMenuItemClickListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.action_sample_data) {
                    // Load sample inventory data
                    com.finedine.rms.utils.SampleDataHelper.loadSampleInventory(this);
                    Toast.makeText(this, "Creating sample inventory items...", Toast.LENGTH_SHORT).show();

                    // Reload after a delay
                    new Handler().postDelayed(this::loadInventory, 2000);
                    return true;
                } else if (itemId == R.id.action_refresh) {
                    loadInventory();
                    return true;
                }
                return false;
            });
        }
    }

    /**
     * Add a sample data button programmatically if not in layout
     */
    private void addSampleDataButton() {
        try {
            // Find a container to add the button to
            ViewGroup rootView = findViewById(android.R.id.content);
            ViewGroup container = rootView;

            if (container != null) {
                Button btnSampleData = new Button(this);
                btnSampleData.setText("Load Sample Inventory");
                btnSampleData.setLayoutParams(new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));

                // Style button
                btnSampleData.setBackgroundColor(getResources().getColor(android.R.color.holo_blue_light));
                btnSampleData.setTextColor(getResources().getColor(android.R.color.white));
                btnSampleData.setPadding(20, 10, 20, 10);

                // Add click listener
                btnSampleData.setOnClickListener(v -> loadSampleData());

                // Add to layout
                container.addView(btnSampleData);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error adding sample data button", e);
        }
    }

    /**
     * Load sample inventory data using SampleDataHelper
     */
    private void loadSampleData() {
        try {
            // Show progress dialog
            AlertDialog progress = new AlertDialog.Builder(this)
                    .setMessage("Loading sample inventory data...")
                    .setCancelable(false)
                    .create();
            progress.show();

            // Load sample data
            com.finedine.rms.utils.SampleDataHelper.loadSampleInventory(this);

            // Dismiss progress and reload after a delay
            new Handler().postDelayed(() -> {
                if (progress.isShowing()) {
                    progress.dismiss();
                }
                loadInventory();
                Toast.makeText(this, "Sample inventory data loaded", Toast.LENGTH_SHORT).show();
            }, 2000);
        } catch (Exception e) {
            Log.e(TAG, "Error loading sample data", e);
            Toast.makeText(this, "Error loading sample data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}