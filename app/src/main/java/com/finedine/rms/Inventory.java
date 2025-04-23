package com.finedine.rms;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "inventory")
public class Inventory {
    @PrimaryKey(autoGenerate = true)
    public int item_id;

    public String item_name;
    public double quantity_in_stock;
    public double reorder_threshold;
    public long last_updated;

}
