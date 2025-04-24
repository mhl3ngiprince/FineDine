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

    // Sample Menu Items
    public static MenuItem[] premiumMenu() {
        return new MenuItem[]{
                // Starters
                new MenuItem(

                        "Seared Scallops",
                        "Day boat scallops with cauliflower purée, truffle foam, and micro herbs",
                        28.50,
                        true
                ),
                new MenuItem(
                        "Foie Gras Torchon",
                        "Sous-vide duck liver terrine with spiced pear chutney and brioche toast",
                        42.00,
                        true
                ),

                // Main Courses
                new MenuItem(
                        "Wagyu Beef Tenderloin",
                        "A5 Japanese wagyu with smoked potato purée, heirloom carrots, and red wine jus",
                        89.00,
                        true
                ),
                new MenuItem(
                        "Lobster Thermidor",
                        "Atlantic lobster with cognac cream sauce, gruyère gratin, and asparagus tips",
                        75.00,
                        true
                ),

                // Chef's Specials
                new MenuItem(
                        "Black Truffle Risotto",
                        "Carnaroli rice with white Alba truffle shavings and Parmigiano-Reggiano",
                        65.00,
                        true
                ),
                new MenuItem(
                        "Venison Medallions",
                        "New Zealand red deer with juniper berry reduction and root vegetable pave",
                        78.00,
                        true
                ),

                // Desserts
                new MenuItem(
                        "Grand Marnier Soufflé",
                        "Freshly baked orange-liqueur soufflé with crème anglaise",
                        22.00,
                        true
                ),
                new MenuItem(
                        "Chocolate Symphony",
                        "70% Valrhona chocolate trio: mousse, ganache, and flourless cake",
                        24.50,
                        true
                ),

                // Beverages
                new MenuItem(
                        "Vintage Champagne Flight",
                        "Three 2oz pours of Krug Grande Cuvée, Dom Pérignon, and Bollinger RD",
                        150.00,
                        true
                ),
                new MenuItem(
                        "Artisan Coffee Service",
                        "French press of rare Ethiopian Yirgacheffe with handmade chocolates",
                        18.00,
                        true
                )
        };
    }

    // Default constructor needed for Room
    public MenuItem() {
    }

    // Constructor for sample data
    public MenuItem(String name, String description, double price, boolean availability) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.availability = availability;
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
}
