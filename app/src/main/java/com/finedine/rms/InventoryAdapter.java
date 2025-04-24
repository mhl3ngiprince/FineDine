package com.finedine.rms;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;



public class InventoryAdapter extends RecyclerView.Adapter<InventoryAdapter.InventoryViewHolder> {
    private final List<Inventory> inventoryItems;
    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Inventory item);
    }

    public InventoryAdapter(List<Inventory> inventoryItems, OnItemClickListener listener) {
        this.inventoryItems = inventoryItems;
        this.listener = listener;
    }

    @NonNull
    @Override
    public InventoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_inventory, parent, false);
        return new InventoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull InventoryViewHolder holder, int position) {
        Inventory item = inventoryItems.get(position);
        holder.bind(item, listener);

    }

    @Override
    public int getItemCount() {
        return inventoryItems.size();
    }

    static class InventoryViewHolder extends RecyclerView.ViewHolder {
        private final TextView itemName;
        private final TextView quantityText;
        private final TextView thresholdText;
        private final ImageView warningIcon;

        public InventoryViewHolder(@NonNull View itemView) {
            super(itemView);
            itemName = itemView.findViewById(R.id.inventoryItemName);
            quantityText = itemView.findViewById(R.id.quantityText);
            thresholdText = itemView.findViewById(R.id.thresholdText);
            warningIcon = itemView.findViewById(R.id.warningIcon);
        }

        @SuppressLint("DefaultLocale")
        public void bind(Inventory item, OnItemClickListener listener) {
            itemName.setText(item.item_name);
            quantityText.setText(String.format("Stock: %.1f", item.quantity_in_stock));
            thresholdText.setText(String.format("Reorder at: %.1f", item.reorder_threshold));

            if (item.quantity_in_stock < item.reorder_threshold) {
                warningIcon.setVisibility(View.VISIBLE);
            } else {
                warningIcon.setVisibility(View.GONE);
            }

            itemView.setOnClickListener(v -> listener.onItemClick(item));
        }
    }
}