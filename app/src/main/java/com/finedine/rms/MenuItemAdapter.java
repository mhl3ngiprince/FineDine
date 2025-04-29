package com.finedine.rms;

import android.annotation.SuppressLint;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;

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
        public ImageView ivMenuItemImage;
        public TextView tvCategory, tvPrepTime, tvCalories, tvSpiceLevel;

        public ViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvMenuItemName);
            tvDescription = itemView.findViewById(R.id.tvMenuItemDescription);
            tvPrice = itemView.findViewById(R.id.tvMenuItemPrice);
            tvAvailability = itemView.findViewById(R.id.tvAvailabilityBadge);
            ivMenuItemImage = itemView.findViewById(R.id.ivMenuItemImage);
            tvCategory = itemView.findViewById(R.id.tvMenuItemCategory);
            tvPrepTime = itemView.findViewById(R.id.tvPrepTime);
            tvCalories = itemView.findViewById(R.id.tvCalories);
            tvSpiceLevel = itemView.findViewById(R.id.tvSpiceLevel);
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

        holder.tvCategory.setText(item.category);
        holder.tvPrepTime.setText(String.format("Prep Time: %s minutes", item.prepTimeMinutes));
        holder.tvCalories.setText(String.format("Calories: %d", item.calories));
        holder.tvSpiceLevel.setText(String.format("Spice Level: %s", item.spiceLevel));

        Glide.with(holder.itemView.getContext())
                .load(item.imageUrl)
                .apply(new RequestOptions().centerCrop())
                .transition(DrawableTransitionOptions.withCrossFade())
                .placeholder(R.drawable.placeholder_food)
                .error(R.drawable.placeholder_food)
                .into(holder.ivMenuItemImage);

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