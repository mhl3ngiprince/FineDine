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
        setContentView(R.layout.activity_inventory);

        try {
            // Setup navigation panel
            setupNavigationPanel("Inventory Management");

            // Initialize RecyclerView
            inventoryRecyclerView = findViewById(R.id.inventoryRecyclerView);
            inventoryRecyclerView.setLayoutManager(new LinearLayoutManager(this));
            inventoryAdapter = new InventoryAdapter(inventoryItems, this::onItemSelected);
            inventoryRecyclerView.setAdapter(inventoryAdapter);

            // Initialize FloatingActionButton
            FloatingActionButton fabAddItem = findViewById(R.id.fabAddItem);
            fabAddItem.setOnClickListener(v -> showAddItemDialog());

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

            new Thread(() -> {
                try {
                    // Get inventory items safely
                    List<Inventory> items = new ArrayList<>();
                    try {
                        items = db.inventoryDao().getAll();
                    } catch (Exception e) {
                        Log.e(TAG, "Database error when loading inventory", e);
                    }

                    // Final copy for UI thread
                    final List<Inventory> finalItems = items;

                    runOnUiThread(() -> {
                        try {
                            inventoryItems.clear();
                            inventoryItems.addAll(finalItems);
                            inventoryAdapter.notifyDataSetChanged();
                            checkLowStockItems();
                        } catch (Exception e) {
                            Log.e(TAG, "Error updating inventory UI", e);
                        }
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Error in inventory loading thread", e);
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
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Reorder " + item.item_name);

        View view = LayoutInflater.from(this).inflate(R.layout.dialog_reorder, null);
        EditText quantityInput = view.findViewById(R.id.quantityInput);
        quantityInput.setText(String.valueOf(item.reorder_threshold * 2));

        builder.setView(view);
        builder.setPositiveButton("Submit", (dialog, which) -> {
            double quantity = Double.parseDouble(quantityInput.getText().toString());
            // In real app, this would send to supplier
            Toast.makeText(this, "Order placed for " + quantity + " " + item.item_name,
                    Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void onItemSelected(Inventory item) {
        // Show edit dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit " + item.item_name);

        View view = LayoutInflater.from(this).inflate(R.layout.dialog_edit_inventory, null);
        EditText quantityInput = view.findViewById(R.id.quantityInput);
        EditText thresholdInput = view.findViewById(R.id.thresholdInput);

        quantityInput.setText(String.valueOf(item.quantity_in_stock));
        thresholdInput.setText(String.valueOf(item.reorder_threshold));

        builder.setView(view);
        builder.setPositiveButton("Save", (dialog, which) -> {
            item.quantity_in_stock = Double.parseDouble(quantityInput.getText().toString());
            item.reorder_threshold = Double.parseDouble(thresholdInput.getText().toString());
            item.last_updated = System.currentTimeMillis();

            new Thread(() -> {
                AppDatabase.getDatabase(this).inventoryDao().update(item);
                runOnUiThread(() -> {
                    loadInventory();
                    Toast.makeText(this, "Inventory updated", Toast.LENGTH_SHORT).show();
                });
            }).start();
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showAddItemDialog() {
        // Create dialog for adding a new inventory item
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add New Inventory Item");

        // Inflate custom layout
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_inventory, null);
        EditText nameInput = view.findViewById(R.id.addNameInput);
        EditText quantityInput = view.findViewById(R.id.addQuantityInput);
        EditText thresholdInput = view.findViewById(R.id.addThresholdInput);

        // Set default values
        quantityInput.setText("0.0");
        thresholdInput.setText("10.0");

        builder.setView(view);
        builder.setPositiveButton("Add", (dialog, which) -> {
            // Validate inputs
            String itemName = nameInput.getText().toString().trim();
            if (itemName.isEmpty()) {
                Toast.makeText(this, "Item name is required", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                // Create new inventory item
                Inventory newItem = new Inventory();
                newItem.item_name = itemName;
                newItem.quantity_in_stock = Double.parseDouble(quantityInput.getText().toString());
                newItem.reorder_threshold = Double.parseDouble(thresholdInput.getText().toString());
                newItem.last_updated = System.currentTimeMillis();

                // Save to database
                new Thread(() -> {
                    AppDatabase.getDatabase(this).inventoryDao().insert(newItem);
                    runOnUiThread(() -> {
                        loadInventory();
                        Toast.makeText(this, "Item added successfully", Toast.LENGTH_SHORT).show();
                    });
                }).start();
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Please enter valid numbers for quantity and threshold", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
}