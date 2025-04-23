package com.finedine.rms;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface OrderItemDao {
    @Insert
    long insert(OrderItem orderItem);  // [[3]] Return row ID for traceability

    @Query("SELECT * FROM order_items WHERE orderId= :orderId")
    List<OrderItem> getByOrderId(int orderId);  // [[1]] Removed invalid default keyword

    @Query("SELECT mi.* FROM menu_items mi " +
            "INNER JOIN order_items oi ON mi.item_id = oi.item_id " +
            "WHERE oi.orderId = :orderId")
    List<MenuItem> getMenuItemsForOrder(int orderId);  // [[4]] Valid SQL syntax
}