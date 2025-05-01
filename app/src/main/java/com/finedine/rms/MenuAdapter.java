package com.finedine.rms;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

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
        private final Button btnAddToOrder;
        private final ImageButton btnFavorite;

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
            btnAddToOrder = itemView.findViewById(R.id.btnAddToOrder);
            btnFavorite = itemView.findViewById(R.id.btnFavorite);
        }

        public void bind(MenuItem item, OnItemClickListener listener) {
            itemName.setText(item.name);
            itemDescription.setText(item.description);
            itemPrice.setText(String.format("%.2f", item.price));
            itemCategory.setText(item.category.toUpperCase());
            prepTime.setText(String.format("%d min", item.prepTimeMinutes));
            calories.setText(String.format("%d cal", item.calories));
            spiceLevel.setText(item.spiceLevel);

            RequestOptions requestOptions = new RequestOptions();
            requestOptions.centerCrop();

            int imageResource = item.imageResourceId;
            if (imageResource <= 0) {
                imageResource = R.drawable.placeholder_food;
            }

            Glide.with(itemView.getContext())
                    .load(imageResource)
                    .apply(requestOptions)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .placeholder(R.drawable.placeholder_food)
                    .error(R.drawable.placeholder_food)
                    .into(itemImage);

            // Set click listeners for all interactive elements
            itemView.setOnClickListener(v -> listener.onItemClick(item));

            // Add to order button
            btnAddToOrder.setOnClickListener(v -> {
                listener.onItemClick(item);
                Toast.makeText(itemView.getContext(),
                        item.name + " added to order", Toast.LENGTH_SHORT).show();
            });

            // Favorite button
            btnFavorite.setOnClickListener(v -> {
                // Toggle favorite status
                btnFavorite.setImageResource(
                        btnFavorite.getTag() != null && (Boolean) btnFavorite.getTag()
                                ? R.drawable.ic_favorite_border
                                : R.drawable.ic_favorite);

                btnFavorite.setTag(btnFavorite.getTag() != null && (Boolean) btnFavorite.getTag() ? false : true);

                Toast.makeText(itemView.getContext(),
                        (btnFavorite.getTag() != null && (Boolean) btnFavorite.getTag())
                                ? item.name + " added to favorites"
                                : item.name + " removed from favorites",
                        Toast.LENGTH_SHORT).show();
            });
        }
    }
}