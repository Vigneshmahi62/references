package com.compsource.app.data.serde.gson.localdate;

import com.datastax.driver.core.LocalDate;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;

/**
 * Gson Deserializer class for type com.datastax.driver.core.LocalDate
 */
public class LocalDateDeserializer implements JsonDeserializer<LocalDate> {
    @Override
    public LocalDate deserialize(JsonElement json, Type typeOfT,
                                 JsonDeserializationContext context) throws JsonParseException {
        String dateText = json.getAsString();
        if (dateText == null)
            return null;
        else {
            java.time.LocalDate date = java.sql.Date.valueOf(json.getAsString()).toLocalDate();
            return LocalDate.fromYearMonthDay(date.getYear(), date.getMonthValue(),
                    date.getDayOfMonth());
        }
    }
}
