package com.finedine.rms;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface UserDao {
    @Insert
    long insert(User user);

    @Update
    void update(User user);

    @Query("SELECT * FROM users WHERE email = :email")
    User getUserByEmail(String email);

    @Query("SELECT * FROM users WHERE email = :email AND password_hash LIKE '%' || :passwordHash || '%'")
    User login(String email, String passwordHash);
    
    @Query("SELECT * FROM users WHERE user_id = :userId")
    User getUserById(int userId);

    @Query("SELECT * FROM users WHERE role = :role")
    List<User> getUsersByRole(String role);

    @Query("SELECT * FROM users WHERE role != 'customer'")
    List<User> getAllStaff();

    @Delete
    void delete(User user);
}