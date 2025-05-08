package com.finedine.rms;

import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;

import androidx.room.Entity;
import androidx.room.Ignore;
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
    public static final int IMG_VENISON = R.drawable.tenderloin; // Using same image as wagyu
    public static final int IMG_SOUFFLE = R.drawable.marniersuffle;
    public static final int IMG_CHOCOLATE = R.drawable.chocolate_symphony;
    public static final int IMG_BAKED_ALASKA = R.drawable.baked_alaska;
    public static final int IMG_CHOCOLATE_SOUFFLE = R.drawable.chocolate_souffle;
    public static final int IMG_CHAMPAGNE = R.drawable.dom_perigon;
    public static final int IMG_COFFEE = R.drawable.greek_coffee_demitasse_cup;
    public static final int IMG_KING_CRAB = R.drawable.crab_leg;
    public static final int IMG_OYSTER = R.drawable.oyster;
    public static final int IMG_SEA_BASS = R.drawable.sea_bass;
    public static final int IMG_WHISKEY = R.drawable.rare_whiskey_flight;
    public static final int IMG_COCKTAILS = R.drawable.signature_cocktail_selection;
    public static final int IMG_CRYING_TIGER = R.drawable.cryingtiger;
    public static final int IMG_PLACEHOLDER = R.drawable.placeholder_food;

    // Sample Menu Items
    public static MenuItem[] premiumMenu() {
        MenuItem[] menuItems = new MenuItem[]{
                // Starters
                new MenuItem(
                        "Seared Scallops",
                        "Day boat scallops with cauliflower purée, truffle foam, and micro herbs",
                        80.00,
                        true,
                        getValidResourceId(IMG_SCALLOPS),
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
                        getValidResourceId(IMG_FOIE_GRAS),
                        CATEGORY_STARTERS,
                        20,
                        380,
                        "Mild"
                ),
                new MenuItem(
                        "Oyster Selection",
                        "Fresh seasonal oysters with champagne mignonette",
                        110.00,
                        true,
                        getValidResourceId(IMG_OYSTER),
                        CATEGORY_STARTERS,
                        10,
                        190,
                        "None"
                ),
                new MenuItem(
                        "King Crab Legs",
                        "Alaskan king crab legs with citrus butter and sea salt",
                        180.00,
                        true,
                        getValidResourceId(IMG_KING_CRAB),
                        CATEGORY_STARTERS,
                        15,
                        220,
                        "None"
                ),

                // Main Courses
                new MenuItem(
                        "Wagyu Beef Tenderloin",
                        "A5 Japanese wagyu with smoked potato purée, heirloom carrots, and red wine jus",
                        200.00,
                        true,
                        getValidResourceId(IMG_WAGYU),
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
                        getValidResourceId(IMG_LOBSTER),
                        CATEGORY_MAIN,
                        40,
                        580,
                        "Mild"
                ),
                new MenuItem(
                        "Dry-Aged Prime Ribeye",
                        "45-day dry-aged prime ribeye with bone marrow crust and bordelaise sauce",
                        190.00,
                        true,
                        getValidResourceId(IMG_CRYING_TIGER),
                        CATEGORY_MAIN,
                        35,
                        680,
                        "Medium"
                ),
                new MenuItem(
                        "Chilean Sea Bass",
                        "Miso-glazed Chilean sea bass with yuzu beurre blanc",
                        210.00,
                        true,
                        getValidResourceId(IMG_SEA_BASS),
                        CATEGORY_MAIN,
                        25,
                        420,
                        "None"
                ),

                // Chef's Specials
                new MenuItem(
                        "Black Truffle Risotto",
                        "Carnaroli rice with white Alba truffle shavings and Parmigiano-Reggiano",
                        234.00,
                        true,
                        getValidResourceId(IMG_TRUFFLE_RISOTTO),
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
                        getValidResourceId(IMG_VENISON),
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
                        getValidResourceId(IMG_SOUFFLE),
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
                        getValidResourceId(IMG_CHOCOLATE),
                        CATEGORY_DESSERTS,
                        15,
                        410,
                        "None"
                ),
                new MenuItem(
                        "Baked Alaska",
                        "Vanilla bean, chocolate, and raspberry ice cream with meringue flambé",
                        85.00,
                        true,
                        getValidResourceId(IMG_BAKED_ALASKA),
                        CATEGORY_DESSERTS,
                        25,
                        450,
                        "None"
                ),
                new MenuItem(
                        "Chocolate Soufflé",
                        "Valrhona dark chocolate soufflé with vanilla crème anglaise",
                        75.00,
                        true,
                        getValidResourceId(IMG_CHOCOLATE_SOUFFLE),
                        CATEGORY_DESSERTS,
                        20,
                        380,
                        "None"
                ),

                // Beverages
                new MenuItem(
                        "Vintage Champagne Flight",
                        "Three 2oz pours of Krug Grande Cuvée, Dom Pérignon, and Bollinger RD",
                        150.00,
                        true,
                        getValidResourceId(IMG_CHAMPAGNE),
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
                        getValidResourceId(IMG_COFFEE),
                        CATEGORY_BEVERAGES,
                        10,
                        85,
                        "None"
                ),
                new MenuItem(
                        "Rare Whiskey Flight",
                        "Three 1oz pours of rare Japanese, Scotch, and American whiskeys",
                        160.00,
                        true,
                        getValidResourceId(IMG_WHISKEY),
                        CATEGORY_BEVERAGES,
                        5,
                        135,
                        "None"
                ),
                new MenuItem(
                        "Signature Cocktail Selection",
                        "Choose three cocktails from our award-winning mixology menu",
                        95.00,
                        true,
                        getValidResourceId(IMG_COCKTAILS),
                        CATEGORY_BEVERAGES,
                        10,
                        180,
                        "None"
                )
        };

        // Log all created menu items for debugging
        Log.d("MenuItem", "Created " + menuItems.length + " menu items");
        for (MenuItem item : menuItems) {
            Log.d("MenuItem", "Item: " + item.name + ", Image Resource: " + item.imageResourceId);
        }

        return menuItems;
    }

    /**
     * Ensures valid image resource ID or falls back to placeholder
     * Handles cases where:
     * - Resource ID is 0 or negative
     * - Resource name contains invalid characters
     * - Resource might not exist in current configuration
     */
    private static int getValidResourceId(int resourceId) {
        try {
            // Basic validation - if resource ID is 0 or negative, use placeholder
            if (resourceId <= 0) {
                Log.w("MenuItem", "Invalid resource ID: " + resourceId + ", using placeholder");
                return IMG_PLACEHOLDER;
            }

            // Additional validation for resource existence
            String resName = "";
            try {
                resName = FineDineApplication.getAppContext().getResources().getResourceName(resourceId);
                if (resName == null || resName.contains("invalid") || resName.contains("null")) {
                    Log.w("MenuItem", "Invalid resource name: " + resName + ", using placeholder");
                    return IMG_PLACEHOLDER;
                }

                // Check specifically for resource existence with getDrawable
                try {
                    android.graphics.drawable.Drawable drawable =
                            FineDineApplication.getAppContext().getResources().getDrawable(resourceId, null);
                    if (drawable == null) {
                        Log.w("MenuItem", "Resource drawable is null: " + resName + ", using placeholder");
                        return IMG_PLACEHOLDER;
                    }
                } catch (Exception e) {
                    Log.w("MenuItem", "Resource drawable failed to load: " + resName + ", using placeholder", e);
                    return IMG_PLACEHOLDER;
                }
            } catch (Resources.NotFoundException e) {
                Log.w("MenuItem", "Resource not found: " + resourceId + ", using placeholder");
                return IMG_PLACEHOLDER;
            }

            return resourceId;
        } catch (Exception e) {
            Log.e("MenuItem", "Error validating resource ID", e);
            return IMG_PLACEHOLDER;
        }
    }

    public MenuItem() {
    }

    @Ignore
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

    @Ignore
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

    // Additional getters needed by FirebaseDatabaseHelper
    public String getTitle() {
        return name; // Returns name as title
    }

    public String getImage() {
        return imageUrl; // Returns imageUrl
    }

    public boolean isIs_available() {
        return availability;
    }

    public boolean getAvailability() {
        return availability;
    }

    public int getItemId() {
        return item_id;
    }
}