package com.compsource.app.data.serde.json.localdate;

import com.datastax.driver.core.LocalDate;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

/**
 * JSON Serializer class for type com.datastax.driver.core.LocalDate
 */
public class LocalDateSerializer extends JsonSerializer<LocalDate> {
    protected LocalDateSerializer() {
        super();
    }

    @Override
    public void serialize(LocalDate value, JsonGenerator generator, SerializerProvider provider)
            throws IOException {
        generator.writeString(value.toString());
    }
}
