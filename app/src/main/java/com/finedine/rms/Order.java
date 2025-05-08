package com.finedine.rms;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.room.Ignore;
import android.util.Log;

import androidx.annotation.NonNull;

@Entity(tableName = "orders",indices = {@Index(value = {"orderId"}, unique = true)})
public class Order {

    private static final String TAG = "Order";

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

    @ColumnInfo(name = "totalAmount")
    private double totalAmount;

    @ColumnInfo(name = "specialInstructions")
    private String specialInstructions;

    private long orderTime;
    public int waiterId;
    private double total;

    @ColumnInfo(name = "completionTime")
    private long completionTime;

    @ColumnInfo(name = "externalId")
    private String externalId;

    @ColumnInfo(name = "recovered")
    private boolean recovered;

    @Ignore
    public Order(int tableNumber, String status) {
        this.tableNumber = tableNumber;
        this.status = status;
        this.timestamp = System.currentTimeMillis();
        this.orderTime = System.currentTimeMillis();
        this.waiterId = 1; // Default waiter ID
        this.totalAmount = 0.0;
        this.specialInstructions = "";
        this.recovered = false;
    }

    public Order() {
        // Initialize with defaults to avoid null values
        this.status = "pending";
        this.timestamp = System.currentTimeMillis();
        this.orderTime = System.currentTimeMillis();
        this.customerName = "Guest";
        this.waiterId = 1; // Default waiter ID
        this.totalAmount = 0.0;
        this.specialInstructions = "";
        this.recovered = false;
    }

    // Getters and setters
    public long getOrderId() { return orderId; }
    public void setOrderId(long orderId) { this.orderId = orderId; }

    public int getTableNumber() { return tableNumber; }
    public void setTableNumber(int tableNumber) { this.tableNumber = tableNumber; }

    public String getStatus() {
        return status != null ? status : "pending";
    }

    public void setStatus(String status) {
        this.status = status != null ? status : "pending";
    }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getCustomerName() {
        return customerName != null ? customerName : "Guest";
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName != null ? customerName : "Guest";
    }

    public String getCustomerPhone() {
        return customerPhone != null ? customerPhone : "";
    }

    public void setCustomerPhone(String customerPhone) {
        this.customerPhone = customerPhone;
    }

    public String getCustomerEmail() {
        return customerEmail != null ? customerEmail : "";
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }

    public String getCustomerNotes() {
        return customerNotes != null ? customerNotes : "";
    }

    public void setCustomerNotes(String customerNotes) {
        this.customerNotes = customerNotes;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getSpecialInstructions() {
        return specialInstructions != null ? specialInstructions : "";
    }

    public void setSpecialInstructions(String specialInstructions) {
        this.specialInstructions = specialInstructions;
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

    public long getCompletionTime() {
        return completionTime;
    }

    public void setCompletionTime(long completionTime) {
        this.completionTime = completionTime;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public boolean isRecovered() {
        return recovered;
    }

    public void setRecovered(boolean recovered) {
        this.recovered = recovered;
    }

    public void notifyCustomer() {
        // Add notification logic here
        try {
            Log.d(TAG, "Notification sent to customer: " + getCustomerName() + " for order: " + getOrderId());
        } catch (Exception e) {
            Log.e(TAG, "Error notifying customer", e);
        }
    }

    /**
     * Validate order data and ensure all required fields have values
     *
     * @return true if the order is valid
     */
    public boolean validate() {
        try {
            // Make sure we have required values
            if (tableNumber <= 0) {
                Log.e(TAG, "Invalid table number: " + tableNumber);
                return false;
            }

            // Make sure status is valid
            if (status == null || status.isEmpty()) {
                status = "pending";
            }

            // Make sure customer has a name
            if (customerName == null || customerName.isEmpty()) {
                customerName = "Guest";
            }

            // Set timestamps if not already set
            if (timestamp <= 0) {
                timestamp = System.currentTimeMillis();
            }

            if (orderTime <= 0) {
                orderTime = System.currentTimeMillis();
            }

            if (specialInstructions == null) specialInstructions = "";

            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error validating order", e);
            return false;
        }
    }

    @NonNull
    @Override
    public String toString() {
        return "Order{" +
                "orderId=" + orderId +
                ", tableNumber=" + tableNumber +
                ", status='" + status + '\'' +
                ", totalAmount=" + totalAmount +
                ", specialInstructions='" + specialInstructions + '\'' +
                ", customerName='" + customerName + '\'' +
                '}';
    }
}