package com.finedine.rms;

import android.content.Context;
import android.util.Log;

/**
 * Simple stub implementation of AppDatabase to avoid crashes
 */
public class AppDatabase {
    private static final String TAG = "AppDatabase";
    private static AppDatabase INSTANCE;

    public static AppDatabase getDatabase(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    try {
                        Log.d(TAG, "Creating new database instance");
                        INSTANCE = new AppDatabase();
                    } catch (Exception e) {
                        Log.e(TAG, "Error creating database", e);
                    }
                }
            }
        }
        return INSTANCE;
    }

    // DAO getters
    public OrderDao orderDao() {
        return new OrderDao();
    }

    public InventoryDao inventoryDao() {
        return new InventoryDao();
    }

    public ReservationDao reservationDao() {
        return new ReservationDao();
    }

    public UserDao userDao() {
        return new UserDao();
    }

    public MenuItemDao menuItemDao() {
        return new MenuItemDao();
    }

    public OrderItemDao orderItemDao() {
        return new OrderItemDao();
    }

    // Stub DAO classes to avoid crashes
    public static class OrderDao {
        public double getTodayOrderCount() {
            return 0.0;
        }

        public java.util.List<com.finedine.rms.Order> getOrdersByStatus(String status) {
            return new java.util.ArrayList<>();
        }

        public long insert(com.finedine.rms.Order order) {
            return 1L;
        }
    }

    public static class OrderItemDao {
        public void insert(com.finedine.rms.OrderItem item) {
            // Do nothing
        }
    }

    public static class MenuItemDao {
        public java.util.List<com.finedine.rms.MenuItem> getAllAvailable() {
            return new java.util.ArrayList<>();
        }

        public void insertAll(com.finedine.rms.MenuItem[] items) {
            // Do nothing
        }
    }

    public static class InventoryDao {
        public java.util.List<com.finedine.rms.Inventory> getLowStockItems() {
            return new java.util.ArrayList<>();
        }

        public java.util.List<com.finedine.rms.Inventory> getAll() {
            return new java.util.ArrayList<>();
        }

        public void update(com.finedine.rms.Inventory item) {
            // Do nothing
        }

        public void insert(com.finedine.rms.Inventory item) {
            // Do nothing
        }
    }

    public static class UserDao {
        public java.util.List<com.finedine.rms.User> getAllStaff() {
            return new java.util.ArrayList<>();
        }

        public void delete(com.finedine.rms.User user) {
            // Do nothing
        }

        public com.finedine.rms.User getUserById(int id) {
            return null;
        }

        public void update(com.finedine.rms.User user) {
            // Do nothing
        }

        public com.finedine.rms.User login(String email, String password) {
            return null;
        }
    }

    public static class ReservationDao {
        public java.util.List<com.finedine.rms.Reservation> getTodayReservations() {
            return new java.util.ArrayList<>();
        }

        public void insert(com.finedine.rms.Reservation reservation) {
            // Do nothing
        }
    }
}