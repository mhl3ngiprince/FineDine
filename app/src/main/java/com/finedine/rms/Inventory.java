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

    @ColumnInfo(name = "unit")
    public String unit;

    @ColumnInfo(name = "category")
    public String category;

    @ColumnInfo(name = "cost")
    public double cost;

    @ColumnInfo(name = "supplier")
    public String supplier;

    public int getId() {
        return item_id;
    }

    public void setId(int id) {
        this.item_id = id;
    }

    public String getName() {
        return item_name;
    }

    public void setName(String name) {
        this.item_name = name;
    }

    public double getQuantity() {
        return quantity_in_stock;
    }

    public void setQuantity(double quantity) {
        this.quantity_in_stock = quantity;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public double getReorderLevel() {
        return reorder_threshold;
    }

    public void setReorderLevel(double reorderLevel) {
        this.reorder_threshold = reorderLevel;
    }

    public double getCost() {
        return cost;
    }

    public void setCost(double cost) {
        this.cost = cost;
    }

    public String getSupplier() {
        return supplier;
    }

    public void setSupplier(String supplier) {
        this.supplier = supplier;
    }

    public long getUpdatedTimestamp() {
        return last_updated;
    }

    public void setUpdatedTimestamp(long updatedTimestamp) {
        this.last_updated = updatedTimestamp;
    }
}