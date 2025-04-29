package com.finedine.rms;

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

    public void updateItems(List<MenuItem> newItems) {
        this.menuItems.clear();
        if (newItems != null) {
            this.menuItems.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    static class MenuViewHolder extends RecyclerView.ViewHolder {
        private final TextView itemName;
        private final TextView itemDescription;
        private final TextView itemPrice;
        private final ImageView itemImage;
        private final TextView itemCategory;
        private final TextView prepTime;
        private final TextView calories;
        private final TextView spiceLevel;

        public MenuViewHolder(View itemView) {
            super(itemView);
            itemName = itemView.findViewById(R.id.tvMenuItemName);
            itemDescription = itemView.findViewById(R.id.tvMenuItemDescription);
            itemPrice = itemView.findViewById(R.id.tvMenuItemPrice);
            itemImage = itemView.findViewById(R.id.ivMenuItemImage);
            itemCategory = itemView.findViewById(R.id.tvMenuItemCategory);
            prepTime = itemView.findViewById(R.id.tvPrepTime);
            calories = itemView.findViewById(R.id.tvCalories);
            spiceLevel = itemView.findViewById(R.id.tvSpiceLevel);
        }

        public void bind(MenuItem item, OnItemClickListener listener) {
            itemName.setText(item.name);
            itemDescription.setText(item.description);
            itemPrice.setText(String.format("$%.2f", item.price));
            itemCategory.setText(item.category);
            prepTime.setText(String.format("Prep Time: %s minutes", item.prepTimeMinutes));
            calories.setText(String.format("Calories: %s", item.calories));
            spiceLevel.setText(String.format("Spice Level: %s", item.spiceLevel));

            RequestOptions requestOptions = new RequestOptions();
            requestOptions.centerCrop();

            Glide.with(itemView.getContext())
                    .load(item.imageUrl)
                    .apply(requestOptions)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .placeholder(R.drawable.placeholder_food)
                    .error(R.drawable.placeholder_food)
                    .into(itemImage);

            itemView.setOnClickListener(v -> listener.onItemClick(item));
        }
    }
}