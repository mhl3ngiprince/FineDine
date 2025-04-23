package com.finedine.rms;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface ReservationDao {
    @Insert
    long insert(Reservation reservation);

    @Query("SELECT * FROM reservations WHERE user_id = :userId ORDER BY reservation_date DESC")
    List<Reservation> getUserReservations(int userId);

    @Query("SELECT * FROM reservations WHERE reservation_date = date('now')")
    List<Reservation> getTodayReservations();

    @Query("SELECT COUNT(*) FROM reservations WHERE reservation_date = date('now')")
    int getTodayReservationCount();
}