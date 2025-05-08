package com.finedine.rms;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.room.Ignore;

@Entity(tableName = "order_items", indices = {
        @Index("orderId"),
        @Index(value = "menu_item_id", unique = false)
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
                        onUpdate = ForeignKey.NO_ACTION,
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
    private Long menuItemId;

    @ColumnInfo(name = "externalId")
    private String externalId;

    public OrderItem() {
        // Default constructor required for Room
        this.notes = "";
        this.specialInstructions = "";
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

    public Long getMenuItemId() {
        return menuItemId;
    }

    public void setMenuItemId(Long menuItemId) {
        this.menuItemId = menuItemId;
    }

    public String getSpecialInstructions() {
        return specialInstructions != null ? specialInstructions : "";
    }

    public void setSpecialInstructions(String specialInstructions) {
        this.specialInstructions = specialInstructions != null ? specialInstructions : "";
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }
}