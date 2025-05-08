package com.finedine.rms;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.Ignore;
import androidx.room.ColumnInfo;
import androidx.room.Index;
import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.FirebaseFirestore;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

@Entity(tableName = "users", indices = {@Index(value = "email", unique = true)})
public class User {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "user_id")
    public int user_id;

    @ColumnInfo(name = "name", defaultValue = "")
    public String name;

    @ColumnInfo(name = "email", defaultValue = "")
    public String email;

    @ColumnInfo(name = "password_hash", defaultValue = "")
    public String password_hash;

    @ColumnInfo(name = "role", defaultValue = "waiter")
    public String role; // manager, chef, waiter

    @ColumnInfo(name = "phone", defaultValue = "")
    public String phone;

    @ColumnInfo(name = "hireDate", defaultValue = "")
    public String hireDate;

    @ColumnInfo(name = "notes", defaultValue = "")
    public String notes;

    public static class FirebaseFallbackManager {
        private static final String TAG = "FirebaseFallback";

        public static boolean isFirebaseAvailable(Context context) {
            if (FirebaseApp.getApps(context).isEmpty()) {
                return false;
            }
            ConnectivityManager cm = (ConnectivityManager) 
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        }

        public static void showToast(Context context, String message) {
            android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    @Ignore
    public User(String name, String email, String password_hash, String role) {
        this.name = name;
        this.email = email;
        this.password_hash = password_hash;
        this.role = role;
        this.phone = ""; // Initialize with empty string
        this.hireDate = ""; // Initialize with empty string
        this.notes = ""; // Initialize with empty string
    }

    public User() {
        // Empty constructor required by Room
        this.name = "";
        this.email = "";
        this.password_hash = "";
        this.role = "waiter";
        this.phone = "";
        this.hireDate = "";
        this.notes = "";
    }

    // Getters and setters
    public int getUser_id() {
        return user_id;
    }

    public void setUser_id(int user_id) {
        this.user_id = user_id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword_hash() {
        return password_hash;
    }

    public void setPassword_hash(String password_hash) {
        this.password_hash = password_hash;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getHireDate() {
        return hireDate;
    }

    public void setHireDate(String hireDate) {
        this.hireDate = hireDate;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}