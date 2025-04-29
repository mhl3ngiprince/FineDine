package com.finedine.rms;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
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
            rvMenuItems.setLayoutManager(new LinearLayoutManager(this));

            // Create adapter with empty list initially
            adapter = new MenuAdapter(new ArrayList<>(), item -> {
                // Handle menu item click
                Toast.makeText(MenuManagementActivity.this,
                        "Selected: " + item.name,
                        Toast.LENGTH_SHORT).show();
            });

            rvMenuItems.setAdapter(adapter);

            // Load menu items from database
            loadMenuItems();
        } catch (Exception e) {
            Log.e(TAG, "Error initializing MenuManagementActivity", e);
            Toast.makeText(this, "Error initializing menu screen", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadMenuItems() {
        try {
            AppDatabase db = AppDatabase.getDatabase(this);
            new Thread(() -> {
                try {
                    List<MenuItem> items = db.menuItemDao().getAllAvailable();
                    runOnUiThread(() -> {
                        try {
                            // Update adapter with loaded items
                            adapter.updateItems(items);
                        } catch (Exception e) {
                            Log.e(TAG, "Error updating menu items UI", e);
                        }
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Error loading menu items from database", e);
                }
            }).start();
        } catch (Exception e) {
            Log.e(TAG, "Error in loadMenuItems", e);
        }
    }
}