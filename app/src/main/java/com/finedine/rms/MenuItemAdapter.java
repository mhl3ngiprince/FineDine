package com.finedine.rms;

import android.annotation.SuppressLint;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.finedine.rms.R;
import com.finedine.rms.MenuItem;
import java.util.List;



public class MenuItemAdapter extends RecyclerView.Adapter<MenuItemAdapter.ViewHolder> {

    private List<MenuItem> menuItems;
    private final OnMenuItemClickListener listener;

    public interface OnMenuItemClickListener {
        void onMenuItemClick(MenuItem item);
        void onEditClick(MenuItem item);
        void onDeleteClick(MenuItem item);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView tvName, tvDescription, tvPrice, tvAvailability;

        public ViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvMenuItemName);
            tvDescription = itemView.findViewById(R.id.tvMenuItemDescription);
            tvPrice = itemView.findViewById(R.id.tvMenuItemPrice);
            tvAvailability = itemView.findViewById(R.id.tvMenuItemAvailability);
        }
    }

    public MenuItemAdapter(List<MenuItem> items, OnMenuItemClickListener listener) {
        this.menuItems = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_menu, parent, false);
        return new ViewHolder(view);
    }

    @SuppressLint("DefaultLocale")
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MenuItem item = menuItems.get(position);

        holder.tvName.setText(item.name);
        holder.tvDescription.setText(item.description);
        holder.tvPrice.setText(String.format("$%.2f", item.price));
        holder.tvAvailability.setText(item.availability ? "Available" : "Not Available");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            holder.tvAvailability.setTextColor(item.availability ?
                    holder.itemView.getContext().getColor(R.color.green) :
                    holder.itemView.getContext().getColor(R.color.red));
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onMenuItemClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return menuItems.size();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void updateMenuItems(List<MenuItem> newItems) {
        menuItems = newItems;
        notifyDataSetChanged();
    }
}