package com.finedine.rms;

import android.content.Context;
import android.util.Log;

import androidx.sqlite.db.SupportSQLiteDatabase;

/**
 * Database facade that delegates to RoomAppDatabase for actual operations
 */
public class AppDatabase {
    private static final String TAG = "AppDatabase";
    private static AppDatabase INSTANCE;
    private RoomAppDatabase roomDatabase;

    private AppDatabase(Context context) {
        roomDatabase = null;
        if (context != null) {
            try {
                // Get Room database instance
                roomDatabase = RoomAppDatabase.getDatabase(context);
                Log.d(TAG, "Created new AppDatabase instance with RoomAppDatabase");
            } catch (Exception e) {
                Log.e(TAG, "Error creating RoomAppDatabase", e);
            }
        } else {
            Log.e(TAG, "Creating AppDatabase with null context as emergency fallback");
        }
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
                        // Create empty instance as fallback to prevent null pointer errors
                        INSTANCE = createEmptyInstance(context);
                    }
                }
            }
        }
        return INSTANCE;
    }

    // Create a minimal functioning database instance that won't crash the app
    private static AppDatabase createEmptyInstance(Context context) {
        Log.d(TAG, "Creating fallback empty database instance");
        try {
            return new AppDatabase(context.getApplicationContext());
        } catch (Exception e) {
            Log.e(TAG, "Critical failure creating empty database instance", e);
            // Last resort emergency fallback that won't crash but won't work either
            return new AppDatabase(null);
        }
    }

    // DAO getters with null checks
    public OrderDao orderDao() {
        return roomDatabase != null ? roomDatabase.orderDao() : null;
    }

    public InventoryDao inventoryDao() {
        return roomDatabase != null ? roomDatabase.inventoryDao() : null;
    }

    public ReservationDao reservationDao() {
        return roomDatabase != null ? roomDatabase.reservationDao() : null;
    }

    public UserDao userDao() {
        return roomDatabase != null ? roomDatabase.userDao() : null;
    }

    public MenuItemDao menuItemDao() {
        return roomDatabase != null ? roomDatabase.menuItemDao() : null;
    }

    public OrderItemDao orderItemDao() {
        return roomDatabase != null ? roomDatabase.orderItemDao() : null;
    }

    public TableDao tableDao() {
        return roomDatabase != null ? roomDatabase.tableDao() : null;
    }

    public ReviewDao reviewDao() {
        return roomDatabase != null ? roomDatabase.reviewDao() : null;
    }

    public ReceiptDao receiptDao() {
        return roomDatabase != null ? roomDatabase.receiptDao() : null;
    }

    /**
     * Close the database connection
     */
    public void close() {
        if (roomDatabase != null) {
            roomDatabase.close();
        }
    }

    /**
     * Get the raw SupportSQLiteDatabase for direct SQL operations
     *
     * @return SupportSQLiteDatabase instance
     */
    public SupportSQLiteDatabase getSQLiteDatabase() {
        if (roomDatabase != null) {
            try {
                return roomDatabase.getOpenHelper().getWritableDatabase();
            } catch (Exception e) {
                Log.e(TAG, "Error getting raw SQLiteDatabase: " + e.getMessage(), e);
                // Try read-only as fallback
                try {
                    return roomDatabase.getOpenHelper().getReadableDatabase();
                } catch (Exception ex) {
                    Log.e(TAG, "Error getting read-only SQLiteDatabase: " + ex.getMessage(), ex);
                }
            }
        }
        Log.e(TAG, "Cannot get SQLiteDatabase - roomDatabase is null");
        return null;
    }

    /**
     * Get the raw SupportSQLiteDatabase for direct SQL operations with context fallback
     *
     * @param context Application context to create database if needed
     * @return SupportSQLiteDatabase instance
     */
    public SupportSQLiteDatabase getSQLiteDatabase(Context context) {
        // Try regular method first
        SupportSQLiteDatabase db = getSQLiteDatabase();
        if (db != null) {
            return db;
        }

        // If that fails and we have a context, try to recreate the database connection
        if (context != null && roomDatabase == null) {
            Log.w(TAG, "Attempting to reconnect to database using provided context");
            try {
                roomDatabase = RoomAppDatabase.getDatabase(context);
                if (roomDatabase != null) {
                    return getSQLiteDatabase(); // Try again with the new connection
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to reconnect to database: " + e.getMessage(), e);
            }
        }

        // If even reconstruction fails, try get it directly from static method
        if (context != null) {
            try {
                return RoomAppDatabase.getSQLiteDatabase(context);
            } catch (Exception e) {
                Log.e(TAG, "All database connection attempts failed", e);
            }
        }

        return null;
    }

    /**
     * Get the underlying RoomAppDatabase instance
     *
     * @return RoomAppDatabase instance
     */
    public RoomAppDatabase getRoomDatabase() {
        return roomDatabase;
    }

    /**
     * Execute a database operation in a transaction, safely handling nested transactions
     *
     * @param operation The operation to execute
     * @return true if operation was successful, false otherwise
     */
    public boolean runInTransaction(Runnable operation) {
        if (roomDatabase == null) {
            Log.e(TAG, "Cannot execute in transaction: roomDatabase is null");
            return false;
        }

        SupportSQLiteDatabase db = getSQLiteDatabase();
        if (db == null) {
            Log.e(TAG, "Cannot execute in transaction: database is null");
            return false;
        }

        boolean inTransaction = false;
        boolean transactionStarted = false;

        try {
            // Check if we're already in a transaction
            try {
                inTransaction = db.inTransaction();
            } catch (Exception e) {
                Log.e(TAG, "Error checking transaction status: " + e.getMessage(), e);
                // Continue and try to start a new transaction
            }

            // Start transaction if not already in one
            if (!inTransaction) {
                try {
                    db.beginTransaction();
                    transactionStarted = true;
                    Log.d(TAG, "Transaction started successfully");
                } catch (Exception e) {
                    Log.e(TAG, "Error beginning transaction: " + e.getMessage(), e);
                    // Continue without transaction
                }
            }

            // Execute the operation
            operation.run();

            // Mark transaction successful if we started one
            if (transactionStarted) {
                try {
                    db.setTransactionSuccessful();
                    Log.d(TAG, "Transaction marked as successful");
                } catch (Exception e) {
                    Log.e(TAG, "Error marking transaction as successful: " + e.getMessage(), e);
                    return false;
                }
            }

            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error executing in transaction: " + e.getMessage(), e);
            return false;
        } finally {
            // End transaction if we started one
            if (transactionStarted) {
                try {
                    db.endTransaction();
                    Log.d(TAG, "Transaction ended");
                } catch (Exception e) {
                    Log.e(TAG, "Error ending transaction: " + e.getMessage(), e);
                }
            }
        }
    }
}