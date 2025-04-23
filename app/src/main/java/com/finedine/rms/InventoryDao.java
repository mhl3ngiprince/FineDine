package com.finedine.rms;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface InventoryDao {
    @Insert
    void insert(Inventory inventory);

    @Update
    void update(Inventory inventory);

    @Delete
    void delete(Inventory inventory);

    @Query("SELECT * FROM inventory ORDER BY item_name ASC")
    List<Inventory> getAll();

    @Query("SELECT * FROM inventory WHERE quantity_in_stock < reorder_threshold")
    List<Inventory> getLowStockItems();

    @Query("SELECT * FROM inventory WHERE item_id = :id")
    Inventory getById(int id);

    @Query("UPDATE inventory SET quantity_in_stock = quantity_in_stock - :amount WHERE item_id = :id")
    void decreaseQuantity(int id, double amount);

    @Query("SELECT * FROM inventory")
    List<Inventory> getItems();
}