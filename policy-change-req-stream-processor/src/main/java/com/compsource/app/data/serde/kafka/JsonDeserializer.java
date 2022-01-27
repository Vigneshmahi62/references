package com.compsource.app.data.serde.kafka;

import com.compsource.app.data.serde.gson.localdate.LocalDateDeserializer;
import com.compsource.app.data.serde.gson.localdate.LocalDateSerializer;
import com.datastax.driver.core.LocalDate;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.kafka.common.serialization.Deserializer;

import java.util.Map;

/**
 * Kafka Deserializer class for de-serializing the Objects using Gson
 *
 * @param <T>
 */
public class JsonDeserializer<T> implements Deserializer<T> {

    private Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDate.class, new LocalDateSerializer())
            .registerTypeAdapter(LocalDate.class, new LocalDateDeserializer())
            .create();

    private Class<T> deserializedClass;

    public JsonDeserializer(Class<T> deserializedClass) {
        this.deserializedClass = deserializedClass;
    }

    public JsonDeserializer() {
    }

    @Override
    @SuppressWarnings("unchecked")
    public void configure(Map<String, ?> map, boolean b) {
        if (deserializedClass == null) {
            deserializedClass = (Class<T>) map.get("serializedClass");
        }
    }

    @Override
    public T deserialize(String s, byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        // Converting the byte array to Class
        return gson.fromJson(new String(bytes), deserializedClass);

    }

    @Override
    public void close() {

    }
}
