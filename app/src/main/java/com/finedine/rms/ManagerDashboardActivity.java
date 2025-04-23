package com.finedine.rms;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;

import com.finedine.rms.R;

import java.util.ArrayList;
import java.util.List;


public class ManagerDashboardActivity extends AppCompatActivity {
    private TextView tvTodaySales, tvActiveOrders, tvLowStockItems;
    private RecyclerView rvRecentReservations;
    private ReservationAdapter reservationAdapter;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manager_dashboard);

        tvTodaySales = findViewById(R.id.tvTodaySales);
        tvActiveOrders = findViewById(R.id.tvActiveOrders);
        tvLowStockItems = findViewById(R.id.tvLowStockItems);
        rvRecentReservations = findViewById(R.id.rvRecentReservations);

        setupRecyclerView();
        loadDashboardData();
    }

    private void setupRecyclerView() {
        rvRecentReservations.setLayoutManager(new LinearLayoutManager(this));
        reservationAdapter = new ReservationAdapter(new ArrayList<>());
        rvRecentReservations.setAdapter(reservationAdapter);
    }




        @SuppressLint("DefaultLocale")
        private void loadDashboardData() {
            AppDatabase db = Room.databaseBuilder(getApplicationContext(),
                            AppDatabase.class, "restaurant-db")
                    .build();

            new Thread(() -> {
                double todaySales = db.orderDao().getTodayOrderCount(); // 24h
                List<Order> activeOrders = db.orderDao().getOrdersByStatus("active");
                List<Inventory> lowStockItems = db.inventoryDao().getLowStockItems(); // <5 quantity
                List<Reservation> reservations = db.reservationDao().getTodayReservations();

                runOnUiThread(() -> {
                    tvTodaySales.setText(String.format("$%.2f", todaySales));
                    tvActiveOrders.setText(String.valueOf(activeOrders));
                    tvLowStockItems.setText(String.valueOf(lowStockItems));
                    reservationAdapter.updateData(reservations);
                });
            }).start();
        }

    public void navigateToInventory(View view) {
        startActivity(new Intent(this, InventoryActivity.class));
    }

    public void navigateToMenuManagement(View view) {
        startActivity(new Intent(this, MenuManagementActivity.class));
    }

    public void navigateToStaffManagement(View view) {
        startActivity(new Intent(this, StaffManagementActivity.class));
    }


}