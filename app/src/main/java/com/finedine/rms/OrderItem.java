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
                        childColumns = "orderId"),
                @ForeignKey(entity = MenuItem.class,
                        parentColumns = "item_id", // Match parent column name
                        childColumns = "item_id")
        })

public class OrderItem {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "item_id")
    public long item_id;
    private String name;
    private int quantity;
    private String orderId;


    public OrderItem() {
        // Default constructor required for Room
    }

    public OrderItem(String name, int quantity, String orderId) {
        this.name = name;
        this.quantity = quantity;
        this.orderId = orderId;
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
}
