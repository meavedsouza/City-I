package com.city_i.utils;

import java.util.Calendar;
import java.util.Date;

public final class DateUtils {
    private DateUtils() {}

    public static int getHourOfDay(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date != null ? date : new Date());
        return cal.get(Calendar.HOUR_OF_DAY);
    }

    /**
     * Returns day of week as 1..7, where 1=Sunday (matches {@link Calendar#DAY_OF_WEEK}).
     */
    public static int getDayOfWeek(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date != null ? date : new Date());
        return cal.get(Calendar.DAY_OF_WEEK);
    }
}

