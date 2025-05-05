package com.finedine.rms;

import android.annotation.SuppressLint;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;

import java.util.List;

public class MenuItemAdapter extends RecyclerView.Adapter<MenuItemAdapter.ViewHolder> {

    private List<MenuItem> menuItems;
    private final OnMenuItemClickListener listener;
    private int lastPosition = -1;

    public interface OnMenuItemClickListener {
        void onMenuItemClick(MenuItem item);
        void onEditClick(MenuItem item);
        void onDeleteClick(MenuItem item);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView tvName, tvDescription, tvPrice, tvAvailability;
        public ImageView ivMenuItemImage;
        public TextView tvCategory, tvPrepTime, tvCalories, tvSpiceLevel;
        public CardView cardView;
        public View divider;

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
            cardView = itemView.findViewById(R.id.cardMenuItem);
            divider = itemView.findViewById(R.id.menuItemDivider);
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

        // Apply styling for item name
        holder.tvName.setText(item.name);
        holder.tvName.setTypeface(holder.tvName.getTypeface(), Typeface.BOLD);

        // Apply styling for description
        holder.tvDescription.setText(item.description);

        // Format and style price
        holder.tvPrice.setText(String.format("R%.2f", item.price));
        holder.tvPrice.setTypeface(holder.tvPrice.getTypeface(), Typeface.BOLD);

        // Set divider color
        if (holder.divider != null) {
            holder.divider.setBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(),
                    R.color.restaurant_divider));
        }

        // Apply elevation to card
        if (holder.cardView != null) {
            holder.cardView.setCardElevation(8f);
            holder.cardView.setRadius(16f);

            // Add ripple effect for better touch feedback
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                holder.cardView.setForeground(ContextCompat.getDrawable(
                        holder.itemView.getContext(),
                        R.drawable.ripple_effect));
            }
        }

        // Check if tvAvailability view exists before trying to use it
        if (holder.tvAvailability != null) {
            // Set availability status with enhanced styling
            holder.tvAvailability.setText(item.availability ? "Available" : "Not Available");

            int textColor;
            int backgroundColor;

            if (item.availability) {
                textColor = ContextCompat.getColor(holder.itemView.getContext(), R.color.restaurant_text_light);
                backgroundColor = ContextCompat.getColor(holder.itemView.getContext(), R.color.restaurant_success);
            } else {
                textColor = ContextCompat.getColor(holder.itemView.getContext(), R.color.restaurant_text_light);
                backgroundColor = ContextCompat.getColor(holder.itemView.getContext(), R.color.restaurant_error);
            }

            holder.tvAvailability.setTextColor(textColor);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                holder.tvAvailability.setBackgroundTintList(ColorStateList.valueOf(backgroundColor));
            } else {
                holder.tvAvailability.setBackgroundColor(backgroundColor);
            }

            // Add rounded corners to availability badge
            holder.tvAvailability.setPadding(20, 8, 20, 8);
        }

        // Style category, prep time, calories, and spice level
        holder.tvCategory.setText(item.category);
        holder.tvCategory.setTypeface(holder.tvCategory.getTypeface(), Typeface.ITALIC);

        holder.tvPrepTime.setText(String.format("Prep Time: %s min", item.prepTimeMinutes));
        holder.tvCalories.setText(String.format("Calories: %d", item.calories));
        holder.tvSpiceLevel.setText(String.format("Spice Level: %s", item.spiceLevel));

        // Enhanced image loading with smoother transitions and better placeholder handling
        Glide.with(holder.itemView.getContext())
                .load(item.imageResourceId)
                .apply(new RequestOptions()
                        .centerCrop()
                        .placeholder(R.drawable.placeholder_food)
                        .error(R.drawable.placeholder_food))
                .transition(DrawableTransitionOptions.withCrossFade(300))
                .into(holder.ivMenuItemImage);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onMenuItemClick(item);
            }
        });

        // Apply animations for smoother scrolling
        setAnimation(holder.itemView, position);
    }

    private void setAnimation(View viewToAnimate, int position) {
        // If the bound view wasn't previously displayed on screen, animate it
        if (position > lastPosition) {
            Animation animation = AnimationUtils.loadAnimation(viewToAnimate.getContext(),
                    android.R.anim.slide_in_left);
            animation.setDuration(350);
            animation.setInterpolator(new android.view.animation.DecelerateInterpolator());
            viewToAnimate.startAnimation(animation);
            lastPosition = position;
        }
    }

    // Reset the animation when adapter is attached to prevent issues on scroll direction change
    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    // Reset position counter when scrolling stops to ensure animations play when scrolling direction changes
                    lastPosition = -1;
                }
            }
        });
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull ViewHolder holder) {
        super.onViewDetachedFromWindow(holder);
        holder.itemView.clearAnimation();
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