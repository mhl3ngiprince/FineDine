package com.finedine.rms;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "reservations")
public class Reservation {
    @PrimaryKey(autoGenerate = true)
    public int reservation_id;

    public int user_id;
    public String reservation_date;
    public String reservation_time;
    public int number_of_guests;
    public String status; // pending, confirmed, cancelled

    public String customerName;
}
