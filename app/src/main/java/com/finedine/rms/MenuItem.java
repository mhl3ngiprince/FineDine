package com.finedine.rms;

import android.os.Bundle;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.google.firebase.analytics.FirebaseAnalytics;

@Entity(tableName = "menu_items")

public class MenuItem {
    @PrimaryKey(autoGenerate = true)
    public int item_id;

    public String name;
    public String description;
    public double price;
    public boolean availability;
    public String imageUrl;
    public int imageResourceId;
    public String category;
    public int prepTimeMinutes;
    public int calories;
    public String spiceLevel;

    // Standard categories
    public static final String CATEGORY_STARTERS = "Starters";
    public static final String CATEGORY_MAIN = "Main Course";
    public static final String CATEGORY_DESSERTS = "Desserts";
    public static final String CATEGORY_BEVERAGES = "Beverages";

    // Drawable image resources for menu items
    public static final int IMG_SCALLOPS = R.drawable.scallops;
    public static final int IMG_FOIE_GRAS = R.drawable.torchon;
    public static final int IMG_WAGYU = R.drawable.tenderloin;
    public static final int IMG_LOBSTER = R.drawable.lobster;
    public static final int IMG_TRUFFLE_RISOTTO = R.drawable.black_truffle_risotto_recipe;
    public static final int IMG_VENISON = R.drawable.tenderloin; // Using tenderloin as fallback
    public static final int IMG_SOUFFLE = R.drawable.marniersuffle;
    public static final int IMG_CHOCOLATE = R.drawable.chocolate_symphony;
    public static final int IMG_CHAMPAGNE = R.drawable.dom_perigon;
    public static final int IMG_COFFEE = R.drawable.greek_coffee_demitasse_cup;
    public static final int IMG_PLACEHOLDER = R.drawable.placeholder_food;

    // Sample Menu Items
    public static MenuItem[] premiumMenu() {
        return new MenuItem[]{
                // Starters
                new MenuItem(
                        "Seared Scallops",
                        "Day boat scallops with cauliflower purée, truffle foam, and micro herbs",
                        80.00,
                        true,
                        IMG_SCALLOPS,
                        CATEGORY_STARTERS,
                        15,
                        210,
                        "Mild"
                ),
                new MenuItem(
                        "Foie Gras Torchon",
                        "Sous-vide duck liver terrine with spiced pear chutney and brioche toast",
                        140.00,
                        true,
                        IMG_FOIE_GRAS,
                        CATEGORY_STARTERS,
                        20,
                        380,
                        "Mild"
                ),

                // Main Courses
                new MenuItem(
                        "Wagyu Beef Tenderloin",
                        "A5 Japanese wagyu with smoked potato purée, heirloom carrots, and red wine jus",
                        200.00,
                        true,
                        IMG_WAGYU,
                        CATEGORY_MAIN,
                        35,
                        620,
                        "Medium"
                ),
                new MenuItem(
                        "Lobster Thermidor",
                        "Atlantic lobster with cognac cream sauce, gruyère gratin, and asparagus tips",
                        275.00,
                        true,
                        IMG_LOBSTER,
                        CATEGORY_MAIN,
                        40,
                        580,
                        "Mild"
                ),

                // Chef's Specials
                new MenuItem(
                        "Black Truffle Risotto",
                        "Carnaroli rice with white Alba truffle shavings and Parmigiano-Reggiano",
                        234.00,
                        true,
                        IMG_TRUFFLE_RISOTTO,
                        CATEGORY_MAIN,
                        30,
                        450,
                        "Mild"
                ),
                new MenuItem(
                        "Venison Medallions",
                        "New Zealand red deer with juniper berry reduction and root vegetable pave",
                        260.00,
                        true,
                        IMG_VENISON,
                        CATEGORY_MAIN,
                        25,
                        520,
                        "Medium"
                ),

                // Desserts
                new MenuItem(
                        "Grand Marnier Soufflé",
                        "Freshly baked orange-liqueur soufflé with crème anglaise",
                        90.00,
                        true,
                        IMG_SOUFFLE,
                        CATEGORY_DESSERTS,
                        20,
                        320,
                        "None"
                ),
                new MenuItem(
                        "Chocolate Symphony",
                        "70% Valrhona chocolate trio: mousse, ganache, and flourless cake",
                        100.00,
                        true,
                        IMG_CHOCOLATE,
                        CATEGORY_DESSERTS,
                        15,
                        410,
                        "None"
                ),

                // Beverages
                new MenuItem(
                        "Vintage Champagne Flight",
                        "Three 2oz pours of Krug Grande Cuvée, Dom Pérignon, and Bollinger RD",
                        150.00,
                        true,
                        IMG_CHAMPAGNE,
                        CATEGORY_BEVERAGES,
                        5,
                        120,
                        "None"
                ),
                new MenuItem(
                        "Artisan Coffee Service",
                        "French press of rare Ethiopian Yirgacheffe with handmade chocolates",
                        40.00,
                        true,
                        IMG_COFFEE,
                        CATEGORY_BEVERAGES,
                        10,
                        85,
                        "None"
                )
        };
    }

    public MenuItem() {
    }

    public MenuItem(String name, String description, double price, boolean availability, String imageUrl, String category, int prepTimeMinutes, int calories, String spiceLevel) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.availability = availability;
        this.imageUrl = imageUrl;
        this.imageResourceId = IMG_PLACEHOLDER; // Default to placeholder
        this.category = category;
        this.prepTimeMinutes = prepTimeMinutes;
        this.calories = calories;
        this.spiceLevel = spiceLevel;
    }

    public MenuItem(String name, String description, double price, boolean availability, int imageResourceId, String category, int prepTimeMinutes, int calories, String spiceLevel) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.availability = availability;
        this.imageUrl = ""; // Empty URL
        this.imageResourceId = imageResourceId;
        this.category = category;
        this.prepTimeMinutes = prepTimeMinutes;
        this.calories = calories;
        this.spiceLevel = spiceLevel;
    }

    public int getItem_id() {
        return item_id;
    }

    public void setItem_id(int item_id) {
        this.item_id = item_id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public boolean isAvailability() {
        return availability;
    }

    public void setAvailability(boolean availability) {
        this.availability = availability;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public int getImageResourceId() {
        return imageResourceId;
    }

    public void setImageResourceId(int imageResourceId) {
        this.imageResourceId = imageResourceId;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public int getPrepTimeMinutes() {
        return prepTimeMinutes;
    }

    public void setPrepTimeMinutes(int prepTimeMinutes) {
        this.prepTimeMinutes = prepTimeMinutes;
    }

    public int getCalories() {
        return calories;
    }

    public void setCalories(int calories) {
        this.calories = calories;
    }

    public String getSpiceLevel() {
        return spiceLevel;
    }

    public void setSpiceLevel(String spiceLevel) {
        this.spiceLevel = spiceLevel;
    }
}