package com.finedine.rms;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class OrderItemAdapter extends RecyclerView.Adapter<OrderItemAdapter.OrderItemViewHolder> {
    private final List<OrderItem> orderItems;
    private final OnItemDeleteListener deleteListener;

    public interface OnItemDeleteListener {
        void onDeleteClick(int position);
    }

    public OrderItemAdapter(List<OrderItem> orderItems, OnItemDeleteListener deleteListener) {
        this.orderItems = orderItems;
        this.deleteListener = deleteListener;
    }

    @NonNull
    @Override
    public OrderItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_order_item, parent, false);
        return new OrderItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderItemViewHolder holder, int position) {
        OrderItem item = orderItems.get(position);
        holder.bind(item, position, deleteListener);
    }

    @Override
    public int getItemCount() {
        return orderItems.size();
    }

    static class OrderItemViewHolder extends RecyclerView.ViewHolder {
        private final TextView itemName;
        private final TextView itemQuantity;
        private final ImageButton deleteButton;

        public OrderItemViewHolder(@NonNull View itemView) {
            super(itemView);
            itemName = itemView.findViewById(R.id.tvOrderItemName);
            itemQuantity = itemView.findViewById(R.id.tvOrderItemQuantity);
            deleteButton = itemView.findViewById(R.id.ibDeleteOrderItem);
        }

        public void bind(OrderItem item, int position, OnItemDeleteListener listener) {
            itemName.setText(item.getName());
            itemQuantity.setText("x" + item.getQuantity());
            deleteButton.setOnClickListener(v -> listener.onDeleteClick(position));
        }
    }
}