package com.finedine.rms;

import android.content.res.Resources;
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
import com.bumptech.glide.signature.ObjectKey;

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
        try {
            android.util.Log.d("MenuAdapter", "Updating menu items: " + (newItems != null ? newItems.size() : 0) + " items");
            this.menuItems.clear();
            if (newItems != null && !newItems.isEmpty()) {
                this.menuItems.addAll(newItems);
                android.util.Log.d("MenuAdapter", "Added items to adapter, new count: " + this.menuItems.size());
            } else {
                android.util.Log.w("MenuAdapter", "No items to add or null list provided");
            }
            notifyDataSetChanged();
            android.util.Log.d("MenuAdapter", "NotifyDataSetChanged called");
        } catch (Exception e) {
            android.util.Log.e("MenuAdapter", "Error updating items: " + e.getMessage());
        }
    }

    /**
     * Get the current list of menu items
     *
     * @return The list of menu items
     */
    public List<MenuItem> getItems() {
        return menuItems;
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
        private final View itemView;

        public MenuViewHolder(View itemView) {
            super(itemView);
            this.itemView = itemView;
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
            prepTime.setText(String.format("%dm", item.prepTimeMinutes));
            calories.setText(String.format("%dcal", item.calories));
            spiceLevel.setText(item.spiceLevel);

            int imageResource = item.imageResourceId;
            if (imageResource <= 0) {
                imageResource = R.drawable.placeholder_food;
            }

            // Debug logging to help diagnose image issues
            android.util.Log.d("MenuAdapter", "Loading image for " + item.name +
                    ", Resource ID: " + imageResource +
                    ", Image URL: " + (item.imageUrl != null ? item.imageUrl : "null"));

            // Try loading from resource first, fallback to URL if available
            try {
                // Use Glide for all image loading for consistency and better performance
                if (imageResource > 0 && imageResource != R.drawable.placeholder_food) {
                    try {
                        // First check if the resource exists
                        itemView.getContext().getResources().getResourceName(imageResource);
                        loadWithGlide(imageResource, item);
                    } catch (Resources.NotFoundException e) {
                        android.util.Log.e("MenuAdapter", "Resource not found: " + imageResource);
                        // Resource doesn't exist, try alternate methods
                        loadAlternativeImage(item);
                    }
                } else if (item.imageUrl != null && !item.imageUrl.isEmpty()) {
                    loadWithGlide(item.imageUrl, item);
                } else {
                    // Try to load specific image based on the item name
                    loadAlternativeImage(item);
                }
            } catch (Exception e) {
                android.util.Log.e("MenuAdapter", "Error loading image: " + e.getMessage());
                loadWithGlide(R.drawable.placeholder_food, item);
            }

            itemImage.setVisibility(View.VISIBLE);

            // Set double click behavior to show details
            itemView.setOnLongClickListener(v -> {
                // Launch detail view instead of just callback
                try {
                    MenuItemDetailActivity.launch(itemView.getContext(), item);
                    return true;
                } catch (Exception e) {
                    android.util.Log.e("MenuAdapter", "Error launching detail view", e);
                    return false;
                }
            });

            // Single click uses the regular callback (for order dialogs etc)
            itemView.setOnClickListener(v -> listener.onItemClick(item));

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

        /**
         * Attempts to load an alternative image based on the menu item name
         */
        private void loadAlternativeImage(MenuItem item) {
            if (item.name == null) {
                loadWithGlide(R.drawable.placeholder_food, item);
                return;
            }

            String lowerName = item.name.toLowerCase();

            // Try to find a matching image based on the item name
            if (lowerName.contains("oyster")) {
                loadWithGlide(R.drawable.oyster, item);
            } else if (lowerName.contains("scallop")) {
                loadWithGlide(R.drawable.scallops, item);
            } else if (lowerName.contains("foie") || lowerName.contains("torchon")) {
                loadWithGlide(R.drawable.torchon, item);
            } else if (lowerName.contains("lobster")) {
                loadWithGlide(R.drawable.lobster, item);
            } else if (lowerName.contains("truffle") || lowerName.contains("risotto")) {
                loadWithGlide(R.drawable.black_truffle_risotto_recipe, item);
            } else if (lowerName.contains("alaska")) {
                loadWithGlide(R.drawable.baked_alaska, item);
            } else if (lowerName.contains("souffle")) {
                loadWithGlide(R.drawable.chocolate_souffle, item);
            } else if (lowerName.contains("champagne")) {
                loadWithGlide(R.drawable.dom_perigon, item);
            } else if (lowerName.contains("beef") || lowerName.contains("wagyu") || lowerName.contains("steak")) {
                loadWithGlide(R.drawable.tenderloin, item);
            } else if (lowerName.contains("chocolate")) {
                loadWithGlide(R.drawable.chocolate_symphony, item);
            } else if (lowerName.contains("coffee")) {
                loadWithGlide(R.drawable.greek_coffee_demitasse_cup, item);
            } else if (lowerName.contains("crab")) {
                loadWithGlide(R.drawable.crab_leg, item);
            } else if (lowerName.contains("sea") && lowerName.contains("bass")) {
                loadWithGlide(R.drawable.sea_bass, item);
            } else if (lowerName.contains("whiskey")) {
                loadWithGlide(R.drawable.rare_whiskey_flight, item);
            } else if (lowerName.contains("cocktail")) {
                loadWithGlide(R.drawable.signature_cocktail_selection, item);
            } else {
                // Fallback to placeholder
                loadWithGlide(R.drawable.placeholder_food, item);
            }
        }

        /**
         * Helper method to load image with Glide
         */
        private void loadWithGlide(Object imageSource, MenuItem item) {
            try {
                // Add a unique request signature to prevent image caching problems
                RequestOptions options = new RequestOptions()
                        .centerCrop()
                        .placeholder(R.drawable.placeholder_food)
                        .error(R.drawable.placeholder_food)
                        .signature(new ObjectKey(item.name + "_" + item.imageResourceId + "_" + System.currentTimeMillis()));

                Glide.with(itemView.getContext())
                        .load(imageSource)
                        .apply(options)
                        .transition(DrawableTransitionOptions.withCrossFade(300))
                        .into(itemImage);
                android.util.Log.d("MenuAdapter", "Loaded image with Glide for: " + item.name);
            } catch (Exception e) {
                android.util.Log.e("MenuAdapter", "Glide error for " + item.name + ": " + e.getMessage());
                Glide.with(itemView.getContext())
                        .load(R.drawable.placeholder_food)
                        .into(itemImage);
            }
        }
    }
}