package com.finedine.rms;

import androidx.lifecycle.LiveData;
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

    @Delete
    void delete(User user);

    @Query("SELECT * FROM users")
    List<User> getAllUsers();

    @Query("SELECT * FROM users WHERE role != 'customer'")
    List<User> getAllStaff();

    @Query("SELECT * FROM users WHERE user_id = :id")
    User getUserById(int id);

    @Query("SELECT * FROM users WHERE email = :email")
    User getUserByEmail(String email);
    
    @Query("SELECT * FROM users WHERE role = :role")
    List<User> getUsersByRole(String role);

    @Query("SELECT * FROM users WHERE email = :email AND password_hash = :password")
    User login(String email, String password);

    @Query("SELECT COUNT(*) FROM users WHERE role = :role")
    int getUserCountByRole(String role);

    @Query("DELETE FROM users WHERE user_id = :id")
    void deleteById(int id);

    @Query("SELECT * FROM users WHERE name LIKE '%' || :searchTerm || '%' OR email LIKE '%' || :searchTerm || '%'")
    List<User> searchUsers(String searchTerm);

    @Query("UPDATE users SET role = :role WHERE user_id = :id")
    void updateUserRole(int id, String role);
}