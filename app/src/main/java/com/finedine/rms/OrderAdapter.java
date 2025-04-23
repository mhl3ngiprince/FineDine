package com.finedine.rms;

import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.finedine.rms.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;



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

        public OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTableNumber = itemView.findViewById(R.id.tvTableNumber);
            tvOrderTime = itemView.findViewById(R.id.tvOrderTime);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            ivStatusIcon = itemView.findViewById(R.id.ivStatusIcon);
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