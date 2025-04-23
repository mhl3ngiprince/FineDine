package com.finedine.rms;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;


// Add index

@Entity(tableName = "order_items",indices = {@Index("orderId")},
        foreignKeys = {

        @ForeignKey(entity=Order.class,
                parentColumns="orderId",
                childColumns = "orderId"),
        @ForeignKey(entity = MenuItem.class,

                parentColumns = "item_id", // Match parent column name
                childColumns = "item_id")
                })


 public class OrderItem {

     @PrimaryKey(autoGenerate = true) // ADD PRIMARY KEY
     @ColumnInfo(name = "item_id")
     public long   item_id;
    private String name;
    private int quantity;
    private String orderId;


    public OrderItem() {
        this.name = name;
        this.quantity = quantity;
        this.orderId = orderId;
        this.item_id=item_id;
    }

    public OrderItem(String tmpName, int tmpQuantity, String tmpOrderId) {
    }

    public OrderItem(int tmpQuantity) {
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
