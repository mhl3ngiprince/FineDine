package com.finedine.rms;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface MenuItemDao {
    @Insert
    long insert(MenuItem menuItem);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(MenuItem... menuItems);

    @Update
    void update(MenuItem menuItem);

    @Delete
    void delete(MenuItem menuItem);

    @Query("SELECT * FROM menu_items")
    List<MenuItem> getAll();

    @Query("SELECT * FROM menu_items WHERE availability = 1")
    List<MenuItem> getAllAvailable();

    @Query("SELECT * FROM menu_items WHERE item_id = :id")
    MenuItem getById(int id);

    @Query("SELECT * FROM menu_items WHERE item_id = :id")
    MenuItem getItemById(int id);

    @Query("SELECT * FROM menu_items WHERE name = :name LIMIT 1")
    MenuItem getByName(String name);

    @Query("SELECT * FROM menu_items WHERE category = :category")
    List<MenuItem> getByCategory(String category);

    @Query("SELECT * FROM menu_items WHERE price <= :maxPrice AND availability = 1")
    List<MenuItem> getAvailableByMaxPrice(double maxPrice);

    @Query("SELECT * FROM menu_items WHERE name LIKE '%' || :searchTerm || '%' OR description LIKE '%' || :searchTerm || '%'")
    List<MenuItem> searchMenuItems(String searchTerm);

    @Query("SELECT * FROM menu_items WHERE name LIKE :nameLike")
    List<MenuItem> searchByName(String nameLike);

    @Query("UPDATE menu_items SET availability = :availability WHERE item_id = :itemId")
    void updateAvailability(int itemId, boolean availability);

    @Query("SELECT COUNT(*) FROM menu_items WHERE category = :category")
    int getCountByCategory(String category);

    @Query("DELETE FROM menu_items")
    void deleteAll();
}