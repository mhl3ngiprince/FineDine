package com.finedine.rms;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;

@Entity(tableName = "inventory")
public class Inventory {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "item_id")
    public int item_id;

    @ColumnInfo(name = "item_name")
    public String item_name;
    @ColumnInfo(name = "quantity_in_stock")
    public double quantity_in_stock;
    @ColumnInfo(name = "reorder_threshold")
    public double reorder_threshold;
    @ColumnInfo(name = "last_updated")
    public long last_updated;
}