package com.finedine.rms;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "inventory")
public class InventoryItem {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String name;
    public int quantity;
    public int lowStockThreshold;
}