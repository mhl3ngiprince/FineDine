package com.finedine.rms;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.TypeConverters;

import java.util.Date;
import java.util.List;

@Dao
@TypeConverters(DateConverter.class)
public interface OrderDao {
    @Insert
    long insert(Order order);

    @Query("SELECT * FROM orders WHERE status = :status ORDER BY timestamp DESC")
    List<Order> getOrdersByStatus(String status);

    @Update
    void update(Order order);

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