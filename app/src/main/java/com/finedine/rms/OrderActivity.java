package com.finedine.rms;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import android.annotation.SuppressLint;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.menu.MenuAdapter;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.finedine.rms.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class OrderActivity extends AppCompatActivity {
    private com.finedine.rms.MenuAdapter menuAdapter;
    private final List<MenuItem> menuItems = new ArrayList<>();
    private final List<OrderItem> currentOrderItems = new ArrayList<>();
    private final int currentTableNumber = 1; // Should come from intent
    private final int waiterId = 1; // Should come from session

    public interface OnItemClickListener {
        void onItemClick(MenuItem item);
    }

    @SuppressLint("RestrictedApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order);

        // Initialize RecyclerView
        RecyclerView menuRecyclerView = findViewById(R.id.menuRecyclerView);
        menuRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Fix: Create your custom adapter, not the framework MenuAdapter
        menuAdapter = new com.finedine.rms.MenuAdapter(menuItems, item -> onItemSelected(item));
        menuRecyclerView.setAdapter(menuAdapter);

        loadMenuItems();
    }

    private void loadMenuItems() {
        new Thread(() -> {
            List<MenuItem> items = AppDatabase.getDatabase(this).menuItemDao().getAllAvailable();
            runOnUiThread(() -> {
                menuItems.clear();
                menuItems.addAll(items);
                menuAdapter.notifyDataSetChanged();
            });
        }).start();
    }

    private void onItemSelected(MenuItem item) {
        // Show quantity dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add " + item.name);

        View view = LayoutInflater.from(this).inflate(R.layout.dialog_quantity, null);
        EditText quantityInput = view.findViewById(R.id.quantityInput);
        EditText notesInput = view.findViewById(R.id.notesInput);

        builder.setView(view);
        builder.setPositiveButton("Add", (dialog, which) -> {
            int quantity = Integer.parseInt(quantityInput.getText().toString());
            String notes = notesInput.getText().toString();
            addToOrder(item, quantity, notes);
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void addToOrder(MenuItem item, int quantity, String notes) {
        // In a real app, this would add to a current order or create a new one
        Toast.makeText(this, quantity + "x " + item.name + " added to order", Toast.LENGTH_SHORT).show();
    }


    public void submitOrder(View view) {
        if (currentOrderItems.isEmpty()) {
            Toast.makeText(this, "Add items to order first", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            AppDatabase db = AppDatabase.getDatabase(this);

            // Create order
            Order order = new Order();
            order.setTableNumber(currentTableNumber);
            order.waiterId = waiterId;
            order.setTimestamp(System.currentTimeMillis());
            order.setStatus("received");

            long orderId = db.orderDao().insert(order);

            // Add order items
            for (OrderItem item : currentOrderItems) {
                item.setOrderId(String.valueOf((int) orderId));
                db.orderItemDao().insert(item);
            }

            runOnUiThread(() -> {
                Toast.makeText(this, "Order #" + orderId + " submitted", Toast.LENGTH_SHORT).show();
                finish();
            });
        }).start();
    }
}