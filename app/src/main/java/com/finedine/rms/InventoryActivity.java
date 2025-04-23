package com.finedine.rms;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;



import java.util.ArrayList;
import java.util.List;


public class InventoryActivity extends AppCompatActivity {
    private RecyclerView inventoryRecyclerView;
    private InventoryAdapter inventoryAdapter;
    private final List<Inventory> inventoryItems = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inventory);

        inventoryRecyclerView = findViewById(R.id.inventoryRecyclerView);
        inventoryRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        inventoryAdapter = new InventoryAdapter(inventoryItems, this::onItemSelected);
        inventoryRecyclerView.setAdapter(inventoryAdapter);

        loadInventory();
    }

    @SuppressLint("NotifyDataSetChanged")
    private void loadInventory() {
        new Thread(() -> {
            List<Inventory> items = AppDatabase.getDatabase(this).inventoryDao().getAll();
            runOnUiThread(() -> {
                inventoryItems.clear();
                inventoryItems.addAll(items);
                inventoryAdapter.notifyDataSetChanged();
                checkLowStockItems();
            });
        }).start();
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


}