package com.finedine.rms;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.Transaction;

import java.util.List;

@Dao
public interface InventoryDao {
    @Insert
    long insert(Inventory item);

    @Update
    @Transaction
    void update(Inventory item);

    @Delete
    void delete(Inventory item);

    @Query("SELECT * FROM inventory ORDER BY item_name ASC")
    List<Inventory> getAll();

    @Query("SELECT * FROM inventory WHERE quantity_in_stock <= reorder_threshold ORDER BY (quantity_in_stock/reorder_threshold) ASC")
    List<Inventory> getLowStockItems();

    @Query("SELECT * FROM inventory WHERE item_id = :id")
    Inventory getItemById(int id);

    @Query("SELECT * FROM inventory WHERE item_name = :name LIMIT 1")
    Inventory getItemByName(String name);

    @Query("UPDATE inventory SET quantity_in_stock = quantity_in_stock + :amount, last_updated = :timestamp WHERE item_id = :id")
    void updateStock(int id, double amount, long timestamp);

    @Query("SELECT * FROM inventory WHERE item_name LIKE '%' || :searchTerm || '%'")
    List<Inventory> searchInventory(String searchTerm);

    @Query("SELECT COUNT(*) FROM inventory WHERE quantity_in_stock <= reorder_threshold")
    int getLowStockCount();

    @Query("DELETE FROM inventory WHERE item_id = :id")
    void deleteById(int id);
}