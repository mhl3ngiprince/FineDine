package com.finedine.rms;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.room.Ignore;

@Entity(tableName = "order_items", indices = {
        @Index("orderId"),
        @Index("menu_item_id")
},
        foreignKeys = {
                @ForeignKey(entity = Order.class,
                        parentColumns = "orderId",
                        childColumns = "orderId",
                        onDelete = ForeignKey.CASCADE,
                        deferred = true),
                @ForeignKey(entity = MenuItem.class,
                        parentColumns = "item_id",
                        childColumns = "menu_item_id",
                        onDelete = ForeignKey.NO_ACTION,
                        deferred = true)
        })
public class OrderItem {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "item_id")
    public long item_id;
    private String name;
    private int quantity;
    private long orderId;
    private String notes;
    private double price;

    @ColumnInfo(name = "specialInstructions")
    private String specialInstructions;

    @ColumnInfo(name = "menu_item_id")
    private Integer menuItemId;

    public OrderItem() {
        // Default constructor required for Room
    }

    @Ignore
    public OrderItem(String name, int quantity, long orderId) {
        this.name = name;
        this.quantity = quantity;
        this.orderId = orderId;
        this.notes = "";
        this.specialInstructions = "";
    }

    // Getters and setters
    public long  getItemId() { return item_id; }
    public void setItemId(long item_id) { this.item_id = item_id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public long getOrderId() {
        return orderId;
    }

    public void setOrderId(long orderId) {
        this.orderId = orderId;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes != null ? notes : "";
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public Integer getMenuItemId() {
        return menuItemId;
    }

    public void setMenuItemId(Integer menuItemId) {
        this.menuItemId = menuItemId;
    }

    public String getSpecialInstructions() {
        return specialInstructions != null ? specialInstructions : "";
    }

    public void setSpecialInstructions(String specialInstructions) {
        this.specialInstructions = specialInstructions != null ? specialInstructions : "";
    }
}