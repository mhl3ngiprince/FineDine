package com.finedine.rms;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;
import androidx.sqlite.db.SupportSQLiteDatabase;

import android.util.Log;

import java.util.List;

@Dao
public interface OrderDao {
    @Insert
    long insert(Order order);

    /**
     * Insert order using raw SQL for when Room persistence is causing issues
     */
    default long insertRaw(Order order, SupportSQLiteDatabase db) {
        if (db == null) return -1;

        try {
            boolean inTransaction = false;
            try {
                // Check if we're already in a transaction
                inTransaction = db.inTransaction();
            } catch (Exception e) {
                Log.e("OrderDao", "Error checking transaction status: " + e.getMessage());
            }

            // Start transaction if not already in one
            if (!inTransaction) {
                try {
                    db.beginTransaction();
                } catch (Exception e) {
                    Log.e("OrderDao", "Error beginning transaction: " + e.getMessage());
                    // Continue without transaction
                }
            }

            // Direct SQL insertion
            String sql = "INSERT INTO orders (tableNumber, status, timestamp, customerName, customerPhone, " +
                    "customerEmail, customerNotes, orderTime, waiterId, total) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

            // Execute SQL with values
            db.execSQL(sql, new Object[]{
                    order.getTableNumber(),
                    order.getStatus(),
                    order.getTimestamp(),
                    order.getCustomerName(),
                    order.getCustomerPhone(),
                    order.getCustomerEmail(),
                    order.getCustomerNotes(),
                    order.getOrderTime(),
                    order.getWaiterId(),
                    order.getTotal()
            });

            // Get the last inserted ID
            android.database.Cursor cursor = db.query("SELECT last_insert_rowid()");
            long newOrderId = -1;
            if (cursor != null && cursor.moveToFirst()) {
                newOrderId = cursor.getLong(0);
                cursor.close();
            }

            // Set transaction successful if we started one
            if (!inTransaction) {
                try {
                    db.setTransactionSuccessful();
                } catch (Exception e) {
                    Log.e("OrderDao", "Error marking transaction successful: " + e.getMessage());
                }
            }

            return newOrderId;
        } catch (Exception e) {
            Log.e("OrderDao", "Error in insertRaw: " + e.getMessage(), e);
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
                Log.e("OrderDao", "Error ending transaction: " + e.getMessage());
            }
        }
    }

    @Update
    void update(Order order);

    @Delete
    void delete(Order order);

    @Query("SELECT * FROM orders ORDER BY timestamp DESC")
    List<Order> getAllOrders();

    @Query("SELECT * FROM orders WHERE status = :status ORDER BY timestamp DESC")
    List<Order> getOrdersByStatus(String status);

    @Query("SELECT * FROM orders WHERE orderId = :orderId")
    Order getOrderById(long orderId);

    @Query("SELECT * FROM orders WHERE tableNumber = :tableNumber AND status != 'completed' AND status != 'cancelled'")
    List<Order> getActiveOrdersForTable(int tableNumber);

    @Query("SELECT COUNT(*) FROM orders WHERE timestamp >= :startTime AND timestamp <= :endTime")
    int getOrderCountForPeriod(long startTime, long endTime);

    @Query("SELECT SUM(total) FROM orders WHERE timestamp >= :startTime AND timestamp <= :endTime AND status = 'completed'")
    double getTotalRevenueForPeriod(long startTime, long endTime);

    @Query("UPDATE orders SET status = :newStatus WHERE orderId = :orderId")
    void updateOrderStatus(long orderId, String newStatus);

    @Query("SELECT COUNT(*) FROM orders WHERE timestamp >= :todayStart")
    int getTodayOrderCount(long todayStart);

    @Transaction
    @Query("SELECT * FROM orders WHERE orderId = :orderId")
    OrderWithItems getOrderWithItems(long orderId);

    @Query("DELETE FROM orders WHERE orderId = :orderId")
    void deleteOrder(long orderId);

    @Query("UPDATE orders SET completionTime = :completionTime WHERE orderId = :orderId")
    void updateOrderCompletionTime(long orderId, long completionTime);

    @Query("SELECT * FROM orders WHERE waiterId = :waiterId")
    List<Order> getOrdersByWaiter(int waiterId);

    @Query("SELECT COUNT(*) FROM orders WHERE status = 'preparing'")
    int getPreparingOrderCount();

    @Query("SELECT COALESCE(SUM(mi.price * oi.quantity), 0) FROM orders o " +
           "JOIN order_items oi ON o.orderId = CAST(oi.orderId AS INTEGER) " +
           "JOIN menu_items mi ON oi.item_id = mi.item_id " +
           "WHERE o.timestamp >= :startOfDay AND o.timestamp <= :endOfDay")
    double calculateTodaySales(long startOfDay, long endOfDay);
    
    default double getTodayOrderCount() {
        long now = System.currentTimeMillis();
        long startOfDay = now - (now % (24 * 60 * 60 * 1000));
        return calculateTodaySales(startOfDay, now);
    }
}