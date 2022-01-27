package com.compsource.app.data.serde.json.localdate;

import com.datastax.driver.core.LocalDate;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;

/**
 * JSON Deserializer class for type com.datastax.driver.core.LocalDate
 */
public class LocalDateDeserializer extends JsonDeserializer<LocalDate> {

    protected LocalDateDeserializer() {
        super();
    }

    @Override
    public LocalDate deserialize(JsonParser parser, DeserializationContext context)
            throws IOException {
        String inputDate = parser.getText();
        if (inputDate == null)
            return null;
        try {
            java.time.LocalDate date = java.sql.Date.valueOf(inputDate).toLocalDate();
            return LocalDate.fromYearMonthDay(date.getYear(), date.getMonthValue(),
                    date.getDayOfMonth());
        } catch (Exception e) {
            return null;
        }
    }
}
