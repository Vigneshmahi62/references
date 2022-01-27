package com.compsource.app.utils;

import com.compsource.app.custom.DeserExcepHandler;
import com.compsource.app.custom.PolicyChangeReqTimestampExtractor;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.streams.StreamsConfig;

import java.util.Properties;

/**
 * This class handles kafka related operations
 */
public class KafkaUtil {
    private static Properties properties = ConfigUtil.loadProperty();
    private static final String BOOTSTRAP_SERVERS = properties.getProperty("kafka.bootstrap.servers");

    /**
     * @param consumerGroupId   - Consumer group Id
     * @param clientId          - Client name
     * @param applicationId     - Application Name
     * @param defaultKeySerde   - Key Serde class
     * @param defaultValueSerde - Value Serde class
     * @return - Properties required for creating a KafkaStream
     */
    public static Properties getStreamProperties(String consumerGroupId, String clientId,
                                                 String applicationId,
                                                 Class<?> defaultKeySerde,
                                                 Class<?> defaultValueSerde) {
        Properties properties = new Properties();
        //An identifier for the stream processing application
        properties.put(StreamsConfig.APPLICATION_ID_CONFIG, applicationId);
        //A unique string that identifies the consumer group this consumer belongs to
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, consumerGroupId);
        //A list of host/port pairs to use for establishing the initial connection
        properties.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        //An ID prefix string used for the client IDs of internal consumer, producer
        // and restore-consumer
        properties.put(StreamsConfig.CLIENT_ID_CONFIG, clientId);
        //The replication factor for change log topics and repartition topics created by the
        // stream processing application
        properties.put(StreamsConfig.REPLICATION_FACTOR_CONFIG, -1);
        //Default serializer / deserializer class for the key
        properties.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, defaultKeySerde);
        //Default serializer / deserializer class for the value
        properties.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, defaultValueSerde);

        // Default Timestamp extractor class to override the kafka time for Windowing operations
        properties.put(StreamsConfig.DEFAULT_TIMESTAMP_EXTRACTOR_CLASS_CONFIG,
                PolicyChangeReqTimestampExtractor.class);

        //Exception handling class for handling the exceptions while deserializing the kafka
        // message
        properties.put(StreamsConfig.DEFAULT_DESERIALIZATION_EXCEPTION_HANDLER_CLASS_CONFIG,
                DeserExcepHandler.class);

        //Reads the earliest data from the topic when there is no initial offset in Kafka or if the
        // current offset does not exist any more on the server
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        return properties;
    }


}
