package com.finedine.rms;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import androidx.sqlite.db.SupportSQLiteDatabase;

import android.util.Log;

import java.util.List;

@Dao
public interface OrderItemDao {
    @Insert
    long insert(OrderItem orderItem);

    @Query("SELECT * FROM order_items WHERE orderId= :orderId")
    List<OrderItem> getByOrderId(int orderId);

    @Query("SELECT * FROM order_items WHERE orderId= :orderId")
    List<OrderItem> getItemsByOrderId(long orderId);

    @Query("SELECT * FROM order_items")
    List<OrderItem> getAllOrderItems();

    @Query("SELECT mi.* FROM menu_items mi " +
            "INNER JOIN order_items oi ON mi.item_id = oi.menu_item_id " +
            "WHERE oi.orderId = :orderId")
    List<MenuItem> getMenuItemsForOrder(int orderId);

    /**
     * Insert order item using raw SQL for when Room persistence is causing issues
     */
    default long insertRaw(OrderItem orderItem, SupportSQLiteDatabase db) {
        if (db == null) return -1;

        try {
            boolean inTransaction = false;
            try {
                // Check if we're already in a transaction
                inTransaction = db.inTransaction();
            } catch (Exception e) {
                Log.e("OrderItemDao", "Error checking transaction status: " + e.getMessage());
            }

            // Start transaction if not already in one
            if (!inTransaction) {
                try {
                    db.beginTransaction();
                } catch (Exception e) {
                    Log.e("OrderItemDao", "Error beginning transaction: " + e.getMessage());
                    // Continue without transaction
                }
            }

            // Check if menu item ID is valid - set to null if not exists
            Integer menuItemId = orderItem.getMenuItemId();
            if (menuItemId != null && menuItemId > 0) {
                String checkSql = "SELECT COUNT(*) FROM menu_items WHERE item_id = ?";
                android.database.Cursor cursor = db.query(checkSql, new Object[]{menuItemId});
                boolean exists = false;
                if (cursor != null) {
                    if (cursor.moveToFirst()) {
                        exists = cursor.getInt(0) > 0;
                    }
                    cursor.close();
                }

                if (!exists) {
                    // Menu item doesn't exist, set to null to avoid foreign key error
                    Log.w("OrderItemDao", "Menu item " + menuItemId + " doesn't exist, setting to null");
                    menuItemId = null;
                }
            }

            // Verify order exists in database to satisfy foreign key constraint
            long orderId = orderItem.getOrderId();
            if (orderId > 0) {
                String checkOrderSql = "SELECT COUNT(*) FROM orders WHERE orderId = ?";
                android.database.Cursor cursor = db.query(checkOrderSql, new Object[]{orderId});
                boolean orderExists = false;
                if (cursor != null) {
                    if (cursor.moveToFirst()) {
                        orderExists = cursor.getInt(0) > 0;
                    }
                    cursor.close();
                }

                if (!orderExists) {
                    // Order doesn't exist - this is a critical error as we can't insert without a valid order
                    Log.e("OrderItemDao", "Order " + orderId + " doesn't exist - cannot insert order item");
                    return -1;
                }
            } else {
                Log.e("OrderItemDao", "Invalid orderId: " + orderId);
                return -1;
            }

            // Direct SQL insertion
            String sql = "INSERT INTO order_items (name, quantity, orderId, notes, price, menu_item_id) " +
                    "VALUES (?, ?, ?, ?, ?, ?)";

            // Execute SQL with values
            db.execSQL(sql, new Object[]{
                    orderItem.getName(),
                    orderItem.getQuantity(),
                    orderItem.getOrderId(),
                    orderItem.getNotes(),
                    orderItem.getPrice(),
                    menuItemId
            });

            // Get the last inserted ID
            android.database.Cursor cursor = db.query("SELECT last_insert_rowid()");
            long newItemId = -1;
            if (cursor != null && cursor.moveToFirst()) {
                newItemId = cursor.getLong(0);
                cursor.close();
            }

            // Set transaction successful if we started one
            if (!inTransaction) {
                try {
                    db.setTransactionSuccessful();
                } catch (Exception e) {
                    Log.e("OrderItemDao", "Error marking transaction successful: " + e.getMessage());
                }
            }

            return newItemId;
        } catch (Exception e) {
            Log.e("OrderItemDao", "Error in insertRaw: " + e.getMessage(), e);
            return -1;
        } finally {
            // End transaction if we started one
            boolean inTransaction = false;
            try {
                inTransaction = db.inTransaction();
                if (inTransaction) {
                    db.endTransaction();
                }
            } catch (Exception e) {
                Log.e("OrderItemDao", "Error ending transaction: " + e.getMessage());
            }
        }
    }
}