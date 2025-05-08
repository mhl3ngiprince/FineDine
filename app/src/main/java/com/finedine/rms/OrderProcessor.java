package com.finedine.rms;

import android.content.Context;
import android.util.Log;

import com.finedine.rms.utils.FirebaseFallbackManager;
import com.finedine.rms.utils.FirebaseConnectionHelper;
import com.finedine.rms.utils.FirebaseSafetyWrapper;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;

public class OrderProcessor {
    private static final String TAG = "OrderProcessor";
    private static final Gson gson = new Gson();

    /**
     * Save order to Firestore and local backup
     *
     * @param context Application context
     * @param order   Order data
     * @param items   Order items list
     */
    public static void saveOrder(Context context, Order order, List<OrderItem> items) {
        // First save locally as backup
        saveOrderBackup(context, order, items);

        // Then attempt to save to Firebase
        saveOrderToFirestore(context, order, items);
    }

    private static void saveOrderToFirestore(Context context, Order order, List<OrderItem> items) {
        try {
            // Get Firestore instance safely
            FirebaseFirestore db = FirebaseSafetyWrapper.getFirestoreInstance(context);

            if (db == null) {
                Log.w(TAG, "Firestore unavailable, order saved only locally");
                return;
            }

            // Create order map
            Map<String, Object> orderMap = new HashMap<>();
            orderMap.put("orderId", order.getOrderId());
            orderMap.put("tableNumber", order.getTableNumber());
            orderMap.put("timestamp", order.getTimestamp());
            orderMap.put("status", order.getStatus());
            orderMap.put("totalAmount", order.getTotal());

            // Create a document reference with order ID
            DocumentReference orderRef = db.collection("orders").document(String.valueOf(order.getOrderId()));

            // Add the order
            orderRef.set(orderMap)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                Log.d(TAG, "Order saved to Firestore successfully: " + order.getOrderId());

                                // Now save order items in a subcollection
                                saveOrderItems(context, db, order.getOrderId(), items);

                                // Push order to Firebase Realtime Database to ensure it appears on the Kitchen screen
                                pushOrderToFirebase(context, order, items, order.getOrderId());
                            } else {
                                Log.e(TAG, "Error saving order to Firestore", task.getException());
                                // Fallback to local processing
                                try {
                                    FirebaseFallbackManager fallbackManager = new FirebaseFallbackManager(context);
                                    fallbackManager.processOrderFallback(order, items);

                                    // Also save the order locally as a JSON string
                                    OrderWithItems orderWithItems = new OrderWithItems();
                                    orderWithItems.order = order;
                                    orderWithItems.orderItems = items;
                                    String orderJson = gson.toJson(orderWithItems);
                                    FirebaseFallbackManager.saveOrderLocally(context, order.getOrderId(), orderJson);
                                } catch (Exception ex) {
                                    Log.e(TAG, "Fallback also failed: " + ex.getMessage());
                                }
                            }
                        }
                    });

        } catch (Exception e) {
            Log.e(TAG, "Error in saveOrderToFirestore: " + e.getMessage(), e);
            // Fallback to local processing
            try {
                // Fallback handling removed since we don't have order object here
                Log.e(TAG, "Failed to save order items, but order was already saved");
            } catch (Exception ex) {
                Log.e(TAG, "Fallback also failed: " + ex.getMessage());
            }
        }
    }

    private static void saveOrderItems(Context context, FirebaseFirestore db, long orderId, List<OrderItem> items) {
        try {
            if (db == null || items == null || items.isEmpty()) {
                Log.w(TAG, "Cannot save order items: Firestore unavailable or no items");
                return;
            }

            // Reference to the order document
            DocumentReference orderRef = db.collection("orders").document(String.valueOf(orderId));

            // Add each item to a subcollection
            for (OrderItem item : items) {
                Map<String, Object> itemMap = new HashMap<>();
                itemMap.put("itemId", item.getItemId());
                itemMap.put("name", item.getName());
                itemMap.put("quantity", item.getQuantity());
                itemMap.put("price", item.getPrice());
                itemMap.put("notes", item.getNotes());

                // Save to "items" subcollection
                orderRef.collection("items").add(itemMap)
                        .addOnCompleteListener(task -> {
                            if (!task.isSuccessful()) {
                                Log.e(TAG, "Error adding order item for order: " + orderId, task.getException());
                            }
                        });
            }

        } catch (Exception e) {
            Log.e(TAG, "Error in saveOrderItems: " + e.getMessage(), e);
            // Fallback handling removed since we don't have order object here
            Log.e(TAG, "Failed to save order items, but order was already saved");
        }
    }

    private static void saveOrderBackup(Context context, Order order, List<OrderItem> items) {
        try {
            // Create OrderWithItems object for serialization
            OrderWithItems orderWithItems = new OrderWithItems();
            orderWithItems.order = order;
            orderWithItems.orderItems = items;

            // Convert to JSON
            String orderJson = gson.toJson(orderWithItems);

            // Save locally
            FirebaseFallbackManager.saveOrderLocally(context, order.getOrderId(), orderJson);

            Log.d(TAG, "Order backup saved locally: " + order.getOrderId());
        } catch (Exception e) {
            Log.e(TAG, "Failed to save order backup: " + e.getMessage());
        }
    }

    /**
     * Push the order to Firebase Realtime Database to ensure it appears on the Kitchen screen
     */
    private static void pushOrderToFirebase(Context context, Order order, List<OrderItem> items, long orderId) {
        try {
            // Get Firebase instance
            FirebaseDatabase database = FirebaseDatabase.getInstance();

            // Create a reference to orders
            String orderKey = database.getReference("orders").push().getKey();

            if (orderKey != null) {
                // Update order with Firebase key
                order.setExternalId(orderKey);
                order.setOrderId(orderId);

                // Create order data map
                Map<String, Object> orderData = new HashMap<>();
                orderData.put("orderId", order.getOrderId());
                orderData.put("tableNumber", order.getTableNumber());
                orderData.put("status", order.getStatus());
                orderData.put("timestamp", order.getTimestamp());
                orderData.put("customerName", order.getCustomerName());
                orderData.put("customerPhone", order.getCustomerPhone());
                orderData.put("customerEmail", order.getCustomerEmail());
                orderData.put("customerNotes", order.getCustomerNotes());
                orderData.put("totalAmount", order.getTotal());
                orderData.put("waiterId", order.getWaiterId());

                // Create items data
                Map<String, Object> itemsData = new HashMap<>();
                for (int i = 0; i < items.size(); i++) {
                    OrderItem item = items.get(i);
                    Map<String, Object> itemData = new HashMap<>();
                    itemData.put("name", item.getName());
                    itemData.put("quantity", item.getQuantity());
                    itemData.put("price", item.getPrice());
                    itemData.put("notes", item.getNotes() != null ? item.getNotes() : "");
                    itemsData.put("item_" + i, itemData);
                }

                // Add items to order data
                orderData.put("items", itemsData);

                // Save to Firebase
                database.getReference("orders").child(orderKey).setValue(orderData);

                // Also save to kitchen_orders path which is specifically watched by the kitchen screen
                database.getReference("kitchen_orders").child(orderKey).setValue(orderData);

                Log.d(TAG, "Order successfully pushed to Firebase with key: " + orderKey);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error pushing order to Firebase: " + e.getMessage(), e);
        }
    }
}