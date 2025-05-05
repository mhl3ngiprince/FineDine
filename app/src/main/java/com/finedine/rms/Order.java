package com.finedine.rms;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.room.Ignore;

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

    @ColumnInfo(name = "customerEmail")
    private String customerEmail;

    @ColumnInfo(name = "customerNotes")
    private String customerNotes;

    private long orderTime;
    public int waiterId;
    private double total;

    @Ignore
    public Order(int tableNumber, String status) {
        this.tableNumber = tableNumber;
        this.status = status;
        this.timestamp = System.currentTimeMillis();
        this.orderTime = System.currentTimeMillis();
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

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }

    public String getCustomerNotes() {
        return customerNotes;
    }

    public void setCustomerNotes(String customerNotes) {
        this.customerNotes = customerNotes;
    }

    public long getOrderTime() {
        return orderTime;
    }

    public void setOrderTime(long orderTime) {
        this.orderTime = orderTime;
    }

    public int getWaiterId() {
        return waiterId;
    }

    public void setWaiterId(int waiterId) {
        this.waiterId = waiterId;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    public void notifyCustomer() {
        // Add notification logic here
    }
}