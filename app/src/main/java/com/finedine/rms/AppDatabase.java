package com.finedine.rms;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;


import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;

@Database(entities = {User.class, Reservation.class, MenuItem.class, Order.class, OrderItem.class, Inventory.class}, version = 2,

        exportSchema = true )

public abstract class AppDatabase extends RoomDatabase {

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
    // Get active orders by status
    DatabaseReference ordersRef = FirebaseDatabase.getInstance().getReference("orders");
    Query activeOrdersQuery = ordersRef.orderByChild("status").equalTo("preparing");

    // Get unread notifications
    DatabaseReference notificationsRef = FirebaseDatabase.getInstance().getReference("notifications");
    Query unreadNotificationsQuery = notificationsRef.orderByChild("read").equalTo(false);

    // Get order with items
    DatabaseReference orderRef = FirebaseDatabase.getInstance().getReference("orders/orderId2");

    public DatabaseReference getOrderRef() {
        return orderRef;
    }
}
