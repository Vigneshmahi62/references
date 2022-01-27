package com.compsource.app;

import com.compsource.app.custom.PolicyChangeReqTimestampExtractor;
import com.compsource.app.data.dao.PolicyLimitChangeRequestsStatisticsDao;
import com.compsource.app.data.model.PolicyLimitChangeRequest;
import com.compsource.app.data.model.PolicyLimitChangeRequestsStatistics;
import com.compsource.app.data.serde.kafka.JsonDeserializer;
import com.compsource.app.data.serde.kafka.JsonSerializer;
import com.compsource.app.data.serde.kafka.WrapperSerde;
import com.compsource.app.data.validation.PolicyLimitChangeReqValidator;
import com.compsource.app.logging.LogManager;
import com.compsource.app.utils.ConfigUtil;
import com.compsource.app.utils.KafkaUtil;
import com.compsource.app.utils.TypeConverter;
import com.datastax.driver.core.LocalDate;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.*;
import org.apache.kafka.streams.state.WindowStore;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Properties;

/**
 * This class consumes and process the events from COIWithPolicyCoverageLimitRequested topic using
 * Kafka Streams and saves the required statistics to a cassandra table
 */
public class PolicyChangeReqStreamProcessor {
    private static LogManager logger = new LogManager(PolicyChangeReqStreamProcessor.class);
    private static Properties properties = ConfigUtil.loadProperty();
    private static final String TOPIC_NAME = properties.getProperty("kafka.consumer.topic");
    private static PolicyLimitChangeRequestsStatisticsDao policyChangeStatDao =
            new PolicyLimitChangeRequestsStatisticsDao();

    public static void main(String[] args) {
        if (args.length != 1) {
            logger.error("Provide the Consumer-Group-Id as argument");
            System.exit(1);
        }
        final String consumerGroupId = args[0];
        final String clientId = "KSC-" + Instant.now().toString();

        Properties streamProperties = KafkaUtil.getStreamProperties(consumerGroupId, clientId,
                consumerGroupId, Serdes.String().getClass(), PolicyLimitChangeRequestSerde.class);

        StreamsBuilder streamsBuilder = new StreamsBuilder();
        KStream<String, PolicyLimitChangeRequest> streamSource = streamsBuilder
                .stream(TOPIC_NAME, Consumed.with(Serdes.String(),
                        new PolicyLimitChangeRequestSerde())
                        .withTimestampExtractor(new PolicyChangeReqTimestampExtractor()));


        streamSource
                //Filtering out the invalid requests that doesn't conforms to the business rules
                .filter((key, policyLimitChangeRequest) ->
                        PolicyLimitChangeReqValidator.validate(policyLimitChangeRequest))
                //Mapping all events to a single key in order to group all the events
                .map((key, policyLimitChangeRequest) -> new KeyValue<>("key", policyLimitChangeRequest))
                .groupByKey()
                //Creating an one hour Tumbling window (non-overlapping window)
                .windowedBy(TimeWindows.of(Duration.ofMinutes(60)).advanceBy(Duration.ofMinutes(60)).grace(Duration.ofDays(355)))
                // Aggregating the PolicyLimitChangeRequest events
                .aggregate(PolicyLimitChangeRequestsStatistics::new,
                        (key, policyLimitChangeRequest, policyStats) -> policyStats.add(policyLimitChangeRequest),
                        Materialized.<String, PolicyLimitChangeRequestsStatistics, WindowStore<Bytes, byte[]>>as
                                ("policy-change-aggregates")
                                .withValueSerde(new PolicyLimitChangeRequestStatsSerde())
                                .withRetention(Duration.ofDays(356)))
                .toStream()
                .foreach(PolicyChangeReqStreamProcessor::writeStatistics);

        KafkaStreams kafkaStreams = new KafkaStreams(streamsBuilder.build(), streamProperties);
        kafkaStreams.start();

        //Add State listener to kafka stream if it is newly created
        if (kafkaStreams.state() == KafkaStreams.State.CREATED) {
            //State listener to close the stream when it switches from RUNNING to PENDING_SHUTDOWN state
            kafkaStreams.setStateListener((newState, oldState) -> {
                if (oldState == KafkaStreams.State.RUNNING && newState == KafkaStreams.State.PENDING_SHUTDOWN) {
                    kafkaStreams.close();
                }
            });
        }

        Runtime.getRuntime().addShutdownHook(new Thread(kafkaStreams::close));
    }


    /**
     * Writes the policyLimitChangeRequestsStatistics to a cassandra table
     *
     * @param timeWindow                     - Time window (one hour)
     * @param policyLimitChangeRequestsStats - Policy Limit Change Request Statistics
     */
    private static void writeStatistics
    (Windowed<String> timeWindow, PolicyLimitChangeRequestsStatistics policyLimitChangeRequestsStats) {

        setRequestedDateAndHour(timeWindow, policyLimitChangeRequestsStats);

        String correlationId = policyLimitChangeRequestsStats.getDateRequested().toString()
                + "_" + policyLimitChangeRequestsStats.getHourRequested().toString();
        try {
            policyChangeStatDao.save(policyLimitChangeRequestsStats);
            logger.info("event processed", correlationId,
                    String.format("Statistics: %s, Message: Statistics for %s to %s was " +
                                    "successfully saved to Cassandra",
                            policyLimitChangeRequestsStats.toJSON(),
                            timeWindow.window().startTime(), timeWindow.window().endTime()));
        } catch (Exception e) {
            logger.error("database not accessible", correlationId,
                    String.format("Error in writing the Policy Limit Change Statistics to " +
                                    "Cassandra. Time Window: %s to %s. Record: %s",
                            timeWindow.window().startTime(), timeWindow.window().endTime(),
                            policyLimitChangeRequestsStats.toJSON()));
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Set the Requested Date and hour for the PolicyLimitChangeRequestsStatistics object from the
     * time Window
     *
     * @param timeWindow                     - Time window of the calculated Statistics
     * @param policyLimitChangeRequestsStats - PolicyLimitChangeRequestsStatistics object
     */
    private static void setRequestedDateAndHour
    (Windowed<String> timeWindow, PolicyLimitChangeRequestsStatistics policyLimitChangeRequestsStats) {
        Instant startTime = timeWindow.window().startTime();
        LocalDateTime localDateTime = LocalDateTime.ofInstant(startTime, ZoneOffset.UTC);
        LocalDate dateRequested = TypeConverter.stringToDatastaxDate(localDateTime.toLocalDate().toString());
        byte hourRequested = (byte) localDateTime.getHour();

        policyLimitChangeRequestsStats.setDateRequested(dateRequested);
        policyLimitChangeRequestsStats.setHourRequested(hourRequested);

    }

    /**
     * Serde class for  PolicyLimitChangeRequestsStatistics
     */
    public static final class PolicyLimitChangeRequestStatsSerde extends WrapperSerde<PolicyLimitChangeRequestsStatistics> {
        PolicyLimitChangeRequestStatsSerde() {
            super(new JsonSerializer<>(), new JsonDeserializer<>(PolicyLimitChangeRequestsStatistics.class));
        }
    }

    /**
     * Serde class for  PolicyLimitChangeRequest
     */
    public static final class PolicyLimitChangeRequestSerde extends WrapperSerde<PolicyLimitChangeRequest> {
        public PolicyLimitChangeRequestSerde() {
            super(new JsonSerializer<>(), new JsonDeserializer<>(PolicyLimitChangeRequest.class));
        }
    }

}
