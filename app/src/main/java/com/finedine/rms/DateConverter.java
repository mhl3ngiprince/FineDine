package com.finedine.rms;

import androidx.room.TypeConverter;
import java.util.Date;

public class DateConverter {
    private DateConverter() {
    }

    @TypeConverter
    public static Date toDate(Long timestamp) {
        return timestamp == null ? null : new Date(timestamp);
    }

    @TypeConverter
    public static Long toTimestamp(Date date) {
        return date == null ? null : date.getTime();
    }

    public static DateConverter createDateConverter() {
        return new DateConverter();
    }
}