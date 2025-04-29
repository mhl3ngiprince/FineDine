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
    public String imageUrl; // URL or resource path for the menu item image
    public String category; // Category of the menu item (e.g., "Starters", "Main Courses", etc.)
    public int prepTimeMinutes; // Preparation time in minutes
    public int calories; // Calories in the dish
    public String spiceLevel; // Spice level (e.g., "Mild", "Medium", "Hot")

    // Sample Menu Items
    public static MenuItem[] premiumMenu() {
        return new MenuItem[]{
                // Starters
                new MenuItem(
                        "Seared Scallops",
                        "Day boat scallops with cauliflower purée, truffle foam, and micro herbs",
                        28.50,
                        true,
                        "https://raw.githubusercontent.com/finedine/menu-images/main/seared_scallops.jpg",
                        "Starters",
                        15,
                        210,
                        "Mild"
                ),
                new MenuItem(
                        "Foie Gras Torchon",
                        "Sous-vide duck liver terrine with spiced pear chutney and brioche toast",
                        42.00,
                        true,
                        "https://raw.githubusercontent.com/finedine/menu-images/main/foie_gras.jpg",
                        "Starters",
                        20,
                        380,
                        "Mild"
                ),

                // Main Courses
                new MenuItem(
                        "Wagyu Beef Tenderloin",
                        "A5 Japanese wagyu with smoked potato purée, heirloom carrots, and red wine jus",
                        89.00,
                        true,
                        "https://raw.githubusercontent.com/finedine/menu-images/main/wagyu_beef.jpg",
                        "Main Courses",
                        35,
                        620,
                        "Medium"
                ),
                new MenuItem(
                        "Lobster Thermidor",
                        "Atlantic lobster with cognac cream sauce, gruyère gratin, and asparagus tips",
                        275.00,
                        true,
                        "https://raw.githubusercontent.com/finedine/menu-images/main/lobster_thermidor.jpg",
                        "Main Courses",
                        40,
                        580,
                        "Mild"
                ),

                // Chef's Specials
                new MenuItem(
                        "Black Truffle Risotto",
                        "Carnaroli rice with white Alba truffle shavings and Parmigiano-Reggiano",
                        65.00,
                        true,
                        "https://raw.githubusercontent.com/finedine/menu-images/main/truffle_risotto.jpg",
                        "Chef's Specials",
                        30,
                        450,
                        "Mild"
                ),
                new MenuItem(
                        "Venison Medallions",
                        "New Zealand red deer with juniper berry reduction and root vegetable pave",
                        78.00,
                        true,
                        "https://raw.githubusercontent.com/finedine/menu-images/main/venison.jpg",
                        "Chef's Specials",
                        25,
                        520,
                        "Medium"
                ),

                // Desserts
                new MenuItem(
                        "Grand Marnier Soufflé",
                        "Freshly baked orange-liqueur soufflé with crème anglaise",
                        22.00,
                        true,
                        "https://raw.githubusercontent.com/finedine/menu-images/main/souffle.jpg",
                        "Desserts",
                        20,
                        320,
                        "None"
                ),
                new MenuItem(
                        "Chocolate Symphony",
                        "70% Valrhona chocolate trio: mousse, ganache, and flourless cake",
                        24.50,
                        true,
                        "https://raw.githubusercontent.com/finedine/menu-images/main/chocolate_symphony.jpg",
                        "Desserts",
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
                        "https://raw.githubusercontent.com/finedine/menu-images/main/champagne_flight.jpg",
                        "Beverages",
                        5,
                        120,
                        "None"
                ),
                new MenuItem(
                        "Artisan Coffee Service",
                        "French press of rare Ethiopian Yirgacheffe with handmade chocolates",
                        18.00,
                        true,
                        "https://raw.githubusercontent.com/finedine/menu-images/main/artisan_coffee.jpg",
                        "Beverages",
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