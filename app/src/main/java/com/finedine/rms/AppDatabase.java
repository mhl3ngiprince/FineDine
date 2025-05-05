package com.finedine.rms;

import android.content.Context;
import android.util.Log;

/**
 * Database facade that delegates to RoomAppDatabase for actual operations
 */
public class AppDatabase {
    private static final String TAG = "AppDatabase";
    private static AppDatabase INSTANCE;
    private final RoomAppDatabase roomDatabase;

    private AppDatabase(Context context) {
        // Get Room database instance
        roomDatabase = RoomAppDatabase.getDatabase(context);
        Log.d(TAG, "Created new AppDatabase instance with RoomAppDatabase");
    }

    public static AppDatabase getDatabase(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    try {
                        Log.d(TAG, "Creating new database instance");
                        INSTANCE = new AppDatabase(context);
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
        return roomDatabase.orderDao();
    }

    public InventoryDao inventoryDao() {
        return roomDatabase.inventoryDao();
    }

    public ReservationDao reservationDao() {
        return roomDatabase.reservationDao();
    }

    public UserDao userDao() {
        return roomDatabase.userDao();
    }

    public MenuItemDao menuItemDao() {
        return roomDatabase.menuItemDao();
    }

    public OrderItemDao orderItemDao() {
        return roomDatabase.orderItemDao();
    }

    public TableDao tableDao() {
        return roomDatabase.tableDao();
    }

    public ReviewDao reviewDao() {
        return roomDatabase.reviewDao();
    }

    /**
     * Close the database connection
     */
    public void close() {
        if (roomDatabase != null) {
            roomDatabase.close();
        }
    }
}