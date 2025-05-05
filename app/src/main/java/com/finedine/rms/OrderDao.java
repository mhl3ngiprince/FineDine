package com.finedine.rms;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import java.util.List;

@Dao
public interface OrderDao {
    @Insert
    long insert(Order order);

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