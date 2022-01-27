package com.compsource.app.data.serde.json.instant;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.Instant;

/**
 * JSON Deserializer class for type Instant
 */
public class InstantDeserializer extends JsonDeserializer<Instant> {
    @Override
    public Instant deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException {
        return Instant.parse(jp.readValueAs(String.class));
    }
}
