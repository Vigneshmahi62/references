package com.compsource.app.data.serde.gson.localdate;

import com.datastax.driver.core.LocalDate;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;

/**
 * Gson Serializer class for type com.datastax.driver.core.LocalDate
 */
public class LocalDateSerializer implements JsonSerializer<LocalDate> {

    @Override
    public JsonElement serialize(LocalDate localDate, Type srcType,
                                 JsonSerializationContext context) {
        return new JsonPrimitive(localDate.toString());
    }
}
