package com.finedine.rms;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface MenuItemDao {
    @Insert
    void insertAll(MenuItem... items);
    @Insert
    long insert(MenuItem menuItem);

    @Update
    void update(MenuItem menuItem);

    @Delete
    void delete(MenuItem menuItem);

    @Query("SELECT * FROM menu_items WHERE availability = 1 ORDER BY name ASC")
    List<MenuItem> getAllAvailable();

    @Query("SELECT * FROM menu_items WHERE item_id = :id")
    MenuItem getById(int id);

    @Query("SELECT * FROM menu_items WHERE name LIKE :query AND availability = 1")
    List<MenuItem> searchAvailable(String query);

    @Query("SELECT * FROM menu_items WHERE name = :name LIMIT 1")
    MenuItem getByName(String name);
}