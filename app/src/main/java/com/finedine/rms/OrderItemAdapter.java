package com.finedine.rms;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class OrderItemAdapter extends RecyclerView.Adapter<OrderItemAdapter.ViewHolder> {
    private final List<OrderItem> orderItems;
    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onDeleteItem(int position);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView tvName, tvQuantity;
        public TextView tvPrice, tvNotes;
        public ImageButton btnDelete;

        public ViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvOrderItemName);
            tvQuantity = itemView.findViewById(R.id.tvOrderItemQuantity);
            tvPrice = itemView.findViewById(R.id.tvOrderItemPrice);
            tvNotes = itemView.findViewById(R.id.tvOrderItemNotes);
            btnDelete = itemView.findViewById(R.id.ibDeleteOrderItem);
        }
    }

    public OrderItemAdapter(List<OrderItem> orderItems, OnItemClickListener listener) {
        this.orderItems = orderItems;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_order_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        OrderItem item = orderItems.get(position);

        holder.tvName.setText(item.getName());
        holder.tvQuantity.setText("x" + item.getQuantity());

        // Set price
        if (holder.tvPrice != null) {
            holder.tvPrice.setText(String.format("R%.2f", item.getPrice()));
        }

        // Set notes if available
        if (holder.tvNotes != null) {
            if (item.getNotes() != null && !item.getNotes().isEmpty()) {
                holder.tvNotes.setText(item.getNotes());
                holder.tvNotes.setVisibility(View.VISIBLE);
            } else {
                holder.tvNotes.setVisibility(View.GONE);
            }
        }

        // Delete button click listener
        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteItem(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return orderItems.size();
    }
}