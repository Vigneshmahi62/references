package com.compsource.app.data.serde.kafka;

import com.compsource.app.data.serde.gson.localdate.LocalDateDeserializer;
import com.compsource.app.data.serde.gson.localdate.LocalDateSerializer;
import com.datastax.driver.core.LocalDate;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.kafka.common.serialization.Serializer;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Kafka Serializer class for serializing the Objects using Gson
 *
 * @param <T>
 */
public class JsonSerializer<T> implements Serializer<T> {

    private Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDate.class, new LocalDateSerializer())
            .registerTypeAdapter(LocalDate.class, new LocalDateDeserializer())
            .create();

    @Override
    public void configure(Map<String, ?> map, boolean b) {
    }

    @Override
    public byte[] serialize(String topic, T obj) {
        //Converts the incoming Object to json string and returns the byte array of the string by
        // encoding using UTF-8
        return gson.toJson(obj).getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public void close() {
    }

}
