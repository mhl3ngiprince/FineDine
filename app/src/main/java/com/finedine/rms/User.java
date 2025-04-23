package com.finedine.rms;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "users")
public class User {
    @PrimaryKey(autoGenerate = true)
    public int user_id;
    public String name;
    public String email;
    public String password_hash;
    public String role; // manager, chef, waiter

    public User(String name, String email, String password_hash, String role) {
        this.name = name;
        this.email = email;
        this.password_hash = password_hash;
        this.role = role;
    }

    public User() {
        this.name = name;
        this.email = email;
        this.password_hash = password_hash;
        this.role = role;
    }
}