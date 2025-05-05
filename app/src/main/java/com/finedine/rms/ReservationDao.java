package com.finedine.rms;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface ReservationDao {
    @Insert
    long insert(Reservation reservation);

    @Update
    void update(Reservation reservation);

    @Delete
    void delete(Reservation reservation);

    @Query("SELECT * FROM reservations ORDER BY reservation_date DESC, reservation_time ASC")
    List<Reservation> getAllReservations();

    @Query("SELECT * FROM reservations WHERE reservation_date = :date ORDER BY reservation_time ASC")
    List<Reservation> getReservationsByDate(String date);

    @Query("SELECT * FROM reservations WHERE user_id = :userId")
    List<Reservation> getUserReservations(int userId);

    @Query("SELECT * FROM reservations WHERE reservation_id = :id")
    Reservation getReservationById(int id);

    @Query("SELECT * FROM reservations WHERE status = :status")
    List<Reservation> getReservationsByStatus(String status);

    @Query("UPDATE reservations SET status = :newStatus WHERE reservation_id = :id")
    void updateReservationStatus(int id, String newStatus);

    @Query("SELECT COUNT(*) FROM reservations WHERE reservation_date = :today")
    int getTodayReservationCount(String today);

    @Query("SELECT * FROM reservations WHERE reservation_date = :today ORDER BY reservation_time ASC")
    List<Reservation> getTodayReservations(String today);

    @Query("SELECT * FROM reservations WHERE customerName LIKE '%' || :searchTerm || '%' OR phone LIKE '%' || :searchTerm || '%'")
    List<Reservation> searchReservations(String searchTerm);

    @Query("SELECT COUNT(*) FROM reservations WHERE reservation_date = :date AND reservation_time = :time AND number_of_guests <= :capacity AND status != 'cancelled'")
    int getConflictingReservationsCount(String date, String time, int capacity);
}