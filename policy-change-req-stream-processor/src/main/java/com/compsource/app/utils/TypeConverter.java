package com.compsource.app.utils;

import com.datastax.driver.core.LocalDate;

/**
 * Util class for common Type conversions
 */
public class TypeConverter {

    /**
     * Converts given string(valid) to datastax based LocalDate
     *
     * @param dateText - Date String in yyyy-MM-dd format
     * @return - Cassandra LocalDate
     */
    public static LocalDate stringToDatastaxDate(String dateText) {
        LocalDate date = null;
        if (dateText != null && !dateText.equalsIgnoreCase("null")) {
            try {
                if (dateText.contains("T"))
                    dateText = dateText.split("T")[0];
                java.time.LocalDate javaDate = java.sql.Date.valueOf(dateText).toLocalDate();
                // datastax date object
                date = LocalDate.fromYearMonthDay(javaDate.getYear(), javaDate.getMonthValue(),
                        javaDate.getDayOfMonth());
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }

        }
        return date;
    }

}
