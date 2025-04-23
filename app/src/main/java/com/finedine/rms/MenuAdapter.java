package com.finedine.rms;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.finedine.rms.R;

import java.util.List;



public class MenuAdapter extends RecyclerView.Adapter<MenuAdapter.MenuViewHolder> {
    private final List<MenuItem> menuItems;
    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(MenuItem item);
    }

    public MenuAdapter(List<MenuItem> menuItems, OnItemClickListener listener) {
        this.menuItems = menuItems;
        this.listener = listener;
    }


    @NonNull
    @Override
    public MenuViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_menu, parent, false);
        return new MenuViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MenuViewHolder holder, int position) {
        MenuItem item = menuItems.get(position);
        holder.bind(item, listener);
    }

    @Override
    public int getItemCount() {
        return menuItems.size();
    }

    static class MenuViewHolder extends RecyclerView.ViewHolder {
        private final TextView itemName;
        private final TextView itemDescription;
        private final TextView itemPrice;

        public MenuViewHolder( View itemView) {
            super(itemView);
            itemName = itemView.findViewById(R.id.tvMenuItemName);
            itemDescription = itemView.findViewById(R.id.tvMenuItemDescription);
            itemPrice = itemView.findViewById(R.id.tvMenuItemPrice);
        }

        public void bind(MenuItem item, OnItemClickListener listener) {
            itemName.setText(item.name);
            itemDescription.setText(item.description);
            itemPrice.setText(String.format("$%.2f", item.price));

            itemView.setOnClickListener(v -> listener.onItemClick(item));
        }
    }
}