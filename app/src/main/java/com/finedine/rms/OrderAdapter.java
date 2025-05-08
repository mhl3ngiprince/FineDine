package com.finedine.rms;

import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.finedine.rms.R;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.OrderViewHolder> {
    private final List<Order> orders;
    private final OnOrderClickListener listener;

    public interface OnOrderClickListener {
        void onOrderClick(Order order);
    }

    public OrderAdapter(List<Order> orders, OnOrderClickListener listener) {
        this.orders = orders;
        this.listener = listener;
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_order, parent, false);
        return new OrderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        Order order = orders.get(position);
        holder.bind(order, listener);
    }

    @Override
    public int getItemCount() {
        return orders.size();
    }

    public void updateData(List<Order> newOrders) {
        orders.clear();
        orders.addAll(newOrders);
        notifyDataSetChanged();
    }

    static class OrderViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvTableNumber;
        private final TextView tvOrderTime;
        private final TextView tvStatus;
        private final ImageView ivStatusIcon;
        private MaterialButton btnSendToChef;

        public OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTableNumber = itemView.findViewById(R.id.tvTableNumber);
            tvOrderTime = itemView.findViewById(R.id.tvOrderTime);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            ivStatusIcon = itemView.findViewById(R.id.ivStatusIcon);

            // Create and add Send to Chef button programmatically
            btnSendToChef = new MaterialButton(itemView.getContext());
            btnSendToChef.setText("Send to Chef");
            btnSendToChef.setId(android.R.id.button1);
            btnSendToChef.setTextColor(itemView.getResources().getColor(R.color.luxe_gold));
            btnSendToChef.setBackgroundTintList(null);
            btnSendToChef.setStrokeWidth(1);
            btnSendToChef.setStrokeColorResource(R.color.luxe_gold);

            // Find the LinearLayout inside the CardView
            android.view.ViewGroup container = (android.view.ViewGroup)
                    ((android.view.ViewGroup) itemView).getChildAt(0);

            // Find the LinearLayout that contains order details
            android.view.ViewGroup orderDetails = (android.view.ViewGroup) container.getChildAt(1);

            // Add the button to the orderDetails LinearLayout
            orderDetails.addView(btnSendToChef);
        }

        public void bind(Order order, OnOrderClickListener listener) {
            tvTableNumber.setText("Table " + order.getTableNumber());
            tvOrderTime.setText(formatTime(String.valueOf(order.getTimestamp())));
            tvStatus.setText(capitalize(order.getStatus()));

            switch (order.getStatus()) {
                case "received":
                    ivStatusIcon.setImageResource(R.drawable.ic_received);
                    break;
                case "preparing":
                    ivStatusIcon.setImageResource(R.drawable.ic_preparing);
                    break;
                case "ready":
                    ivStatusIcon.setImageResource(R.drawable.ic_ready);
                    break;
                case "served":
                    ivStatusIcon.setImageResource(R.drawable.ic_served);
                    break;
            }

            itemView.setOnClickListener(v -> listener.onOrderClick(order));

            // Configure the Send to Chef button
            btnSendToChef.setOnClickListener(v -> {
                sendOrderToChef(order, v.getContext());
            });

            // Hide the button if order is already preparing or beyond
            if ("preparing".equals(order.getStatus()) ||
                    "ready".equals(order.getStatus()) ||
                    "served".equals(order.getStatus()) ||
                    "completed".equals(order.getStatus())) {
                btnSendToChef.setVisibility(android.view.View.GONE);
            } else {
                btnSendToChef.setVisibility(android.view.View.VISIBLE);
            }
        }

        private void sendOrderToChef(Order order, android.content.Context context) {
            try {
                // Get order items from the database
                AppDatabase db = AppDatabase.getDatabase(context);
                java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newSingleThreadExecutor();

                executor.execute(() -> {
                    try {
                        // Fetch order items
                        List<OrderItem> orderItems = db.orderItemDao().getItemsByOrderId(order.getOrderId());

                        // Prepare the data for chef
                        HashMap<String, Integer> selectedItems = new HashMap<>();
                        for (OrderItem item : orderItems) {
                            selectedItems.put(item.getName(), item.getQuantity());
                        }

                        // Run on main thread
                        android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
                        handler.post(() -> {
                            // Use the centralized sendOrderToChef method from OrderProcessor
                            com.finedine.rms.utils.OrderProcessor.sendOrderToChef(
                                    context,
                                    selectedItems,
                                    order.getSpecialInstructions(),
                                    order.getTableNumber(),
                                    new com.finedine.rms.utils.OrderProcessor.FirebaseCallback() {
                                        @Override
                                        public void onSuccess(String documentId) {
                                            Toast.makeText(context, "Order sent to kitchen successfully", Toast.LENGTH_SHORT).show();
                                        }

                                        @Override
                                        public void onFailure(String error) {
                                            Toast.makeText(context, "Failed to send order to kitchen: " + error, Toast.LENGTH_SHORT).show();
                                        }
                                    }
                            );
                        });
                    } catch (Exception e) {
                        android.util.Log.e("OrderAdapter", "Error sending to chef: " + e.getMessage(), e);
                        android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
                        handler.post(() -> {
                            Toast.makeText(context, "Error sending order to chef", Toast.LENGTH_SHORT).show();
                        });
                    }
                });
            } catch (Exception e) {
                android.util.Log.e("OrderAdapter", "Error sending to chef: " + e.getMessage(), e);
                Toast.makeText(context, "Error sending order to chef", Toast.LENGTH_SHORT).show();
            }
        }

        private String formatTime(String timestamp) {
            try {
                long timeMs;
                if (timestamp.matches("\\d+")) {
                    timeMs = Long.parseLong(timestamp);
                } else {
                    SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    Date date = inputFormat.parse(timestamp);
                    timeMs = date.getTime();
                }
                SimpleDateFormat outputFormat = new SimpleDateFormat("h:mm a");
                return outputFormat.format(new Date(timeMs));
            } catch (Exception e) {
                return "Unknown time";
            }
        }

        private String capitalize(String str) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
                if (str == null || str.isEmpty()) {
                    return str;
                }
            }
            return str.substring(0, 1).toUpperCase() + str.substring(1);
        }
    }
}