package com.compsource.app.custom;

import com.compsource.app.data.model.PolicyLimitChangeRequest;
import com.compsource.app.logging.LogManager;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.streams.processor.TimestampExtractor;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Custom TimestampExtractor class for extracting the Timestamp from the kafka message instead
 * using the default Kafka time
 */
public class PolicyChangeReqTimestampExtractor implements TimestampExtractor {
    private static LogManager logger = new LogManager(PolicyChangeReqTimestampExtractor.class);

    /**
     * Extracts the event time from the event - dateRequested field
     *
     * @param record        - Consumer Record
     * @param partitionTime - Event created time of the previous record in the partition
     * @return - Extracted epoch milliseconds
     */
    @Override
    public long extract(ConsumerRecord<Object, Object> record, long partitionTime) {
        PolicyLimitChangeRequest policyLimitChangeRequest =
                (PolicyLimitChangeRequest) record.value();
        try {
            String dateRequestedStr = policyLimitChangeRequest.getDateRequested();
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            LocalDateTime dateRequested = LocalDateTime.parse(dateRequestedStr, dateTimeFormatter);

            return dateRequested.toInstant(ZoneOffset.UTC).toEpochMilli();
        } catch (Exception e) {
            logger.error("invalid content", policyLimitChangeRequest.getCoiRequestId(),
                    String.format("Event: %s, Reason: %s, Exception: %s",
                            policyLimitChangeRequest.toJSON(), "Invalid field - dateRequested",
                            e.toString()));
        }
        return -1;

    }
}