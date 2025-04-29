package com.finedine.rms;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "order_items", indices = {@Index("orderId")},
        foreignKeys = {
                @ForeignKey(entity = Order.class,
                        parentColumns = "orderId",
                        childColumns = "orderId",
                        onDelete = ForeignKey.CASCADE,
                        deferred = true),
                @ForeignKey(entity = MenuItem.class,
                        parentColumns = "item_id",
                        childColumns = "item_id",
                        onDelete = ForeignKey.NO_ACTION,
                        deferred = true)
        })
public class OrderItem {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "item_id")
    public long item_id;
    private String name;
    private int quantity;
    private String orderId;
    private String notes;

    public OrderItem() {
        // Default constructor required for Room
    }

    public OrderItem(String name, int quantity, String orderId) {
        this.name = name;
        this.quantity = quantity;
        this.orderId = orderId;
        this.notes = "";
    }

    // Getters and setters
    public long  getItemId() { return item_id; }
    public void setItemId(long item_id) { this.item_id = item_id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes != null ? notes : "";
    }
}