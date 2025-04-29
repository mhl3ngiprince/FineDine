package com.finedine.rms;

import android.content.Context;
import android.util.Log;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;

@Database(entities = {User.class, Reservation.class, MenuItem.class, Order.class, OrderItem.class, Inventory.class}, version = 2,
        exportSchema = true )
public abstract class AppDatabase extends RoomDatabase {

    private static final String TAG = "AppDatabase";

    public abstract UserDao userDao();
    public abstract ReservationDao reservationDao();
    // Add other DAOs
    public abstract OrderDao orderDao();
    public abstract InventoryDao inventoryDao();
    public abstract MenuItemDao menuItemDao();

    public abstract OrderItemDao orderItemDao();

    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "fine dine_db")
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    // Get Firebase database reference safely
    private FirebaseDatabase getFirebaseInstance() {
        try {
            return FirebaseDatabase.getInstance();
        } catch (Exception e) {
            Log.e(TAG, "Firebase is not initialized properly", e);
            return null;
        }
    }

    // Get active orders by status
    public Query getActiveOrdersQuery() {
        FirebaseDatabase database = getFirebaseInstance();
        if (database == null) {
            Log.e(TAG, "Cannot get active orders: Firebase not initialized");
            return null;
        }

        DatabaseReference ordersRef = database.getReference("orders");
        return ordersRef.orderByChild("status").equalTo("preparing");
    }

    // Get unread notifications
    public Query getUnreadNotificationsQuery() {
        FirebaseDatabase database = getFirebaseInstance();
        if (database == null) {
            Log.e(TAG, "Cannot get notifications: Firebase not initialized");
            return null;
        }

        DatabaseReference notificationsRef = database.getReference("notifications");
        return notificationsRef.orderByChild("read").equalTo(false);
    }

    // Get order with items
    public DatabaseReference getOrderRef(String orderId) {
        FirebaseDatabase database = getFirebaseInstance();
        if (database == null) {
            Log.e(TAG, "Cannot get order reference: Firebase not initialized");
            return null;
        }

        return database.getReference("orders").child(orderId);
    }
}
