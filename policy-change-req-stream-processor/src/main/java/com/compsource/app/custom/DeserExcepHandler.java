package com.compsource.app.custom;

import com.compsource.app.logging.LogManager;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.streams.errors.DeserializationExceptionHandler;
import org.apache.kafka.streams.processor.ProcessorContext;

import java.util.Map;
import java.util.UUID;

/**
 * Custom Deserialization Exception handler class to handle the exception occurs while
 * de-serializing the kafka message
 */
public class DeserExcepHandler implements DeserializationExceptionHandler {
    private static LogManager logger = new LogManager(DeserExcepHandler.class);

    /**
     * Logs the error message and continue to consume the upcoming messages
     *
     * @param context   - Processor context
     * @param record    - Malformed event
     * @param exception - Exception
     * @return - DeserializationHandlerResponse - CONTINUE / FAIL
     */
    @Override
    public DeserializationHandlerResponse handle(ProcessorContext context, ConsumerRecord<byte[], byte[]> record, Exception exception) {
        String event = new String(record.value());
        logger.error("malformed json", UUID.randomUUID().toString(),
                String.format("Event: %s, Reason: Unable to convert the Json to required Object, " +
                        "Exception: %s", event, exception.toString()));
        return DeserializationHandlerResponse.CONTINUE;
    }

    @Override
    public void configure(Map<String, ?> configs) {

    }
}
