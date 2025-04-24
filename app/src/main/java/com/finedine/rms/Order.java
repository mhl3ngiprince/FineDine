package com.finedine.rms;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "orders",indices = {@Index(value = {"orderId"}, unique = true)})
public class Order {


@PrimaryKey(autoGenerate = true)
@ColumnInfo(name = "orderId")



    private long orderId;
    private int tableNumber;
    private String status;
    private long timestamp;

    @ColumnInfo(name = "customerName")
    private String customerName;

    @ColumnInfo(name = "customerPhone")
    private String customerPhone;

    @ColumnInfo(name = "customerNotes")
    private String customerNotes;

    long order_time;
    public int waiterId;

    public Order(int tableNumber, String status) {
        this.tableNumber = tableNumber;
        this.status = status;
        this.timestamp = System.currentTimeMillis();
    }

    public Order() {
    }

    // Getters and setters
    public long getOrderId() { return orderId; }
    public void setOrderId(long orderId) { this.orderId = orderId; }
    public int getTableNumber() { return tableNumber; }
    public void setTableNumber(int tableNumber) { this.tableNumber = tableNumber; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public void setCustomerPhone(String customerPhone) {
        this.customerPhone = customerPhone;
    }

    public String getCustomerNotes() {
        return customerNotes;
    }

    public void setCustomerNotes(String customerNotes) {
        this.customerNotes = customerNotes;
    }
}