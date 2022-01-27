package com.compsource.app.logging;

import com.compsource.app.utils.ConfigUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Properties;
import java.util.UUID;

import static net.logstash.logback.argument.StructuredArguments.kv;

/**
 * Class that handles custom logging operations
 */
public class LogManager {
    private static Properties properties = ConfigUtil.loadProperty();
    private static String serviceName = properties.getProperty("service.name");
    private static String correlationIdKey = properties.getProperty("log.correlation-id.key");
    private static String statusKey = properties.getProperty("log.status.key");
    private static String technicalDetailsKey = properties.getProperty("log.technical-details.key");
    private static String hostNameKey = properties.getProperty("log.hostname.key");
    private static String serviceNameKey = properties.getProperty("log.service-name.key");
    private Logger logger;

    public LogManager(Class<?> loggerClass) {
        this.logger = LoggerFactory.getLogger(loggerClass);
    }

    /**
     * Publishes a log info message
     *
     * @param message          - log message
     * @param correlationId    - COI Request Id
     * @param technicalDetails - null
     */
    public void info(String message, String correlationId, String technicalDetails) {
        if (correlationId.equals("")) correlationId = UUID.randomUUID().toString();
        logger.info(message, kv(correlationIdKey, correlationId),
                kv(statusKey, "success"), kv(technicalDetailsKey, technicalDetails),
                kv(hostNameKey, getHostName()), kv(serviceNameKey, serviceName));
    }

    public void info(String message) {
        logger.info(message);
    }

    /**
     * Publishes a log error message
     *
     * @param message          - log message
     * @param correlationId    - COI Request Id
     * @param technicalDetails - Reason for the error along with the required information
     */
    public void error(String message, String correlationId, String technicalDetails) {
        if (correlationId.equals("")) correlationId = UUID.randomUUID().toString();

        logger.error(message, kv(correlationIdKey, correlationId),
                kv(statusKey, "failure"), kv(technicalDetailsKey, technicalDetails),
                kv(hostNameKey, getHostName()), kv(serviceNameKey, serviceName));

    }

    public void error(String message) {
        logger.error(message);
    }

    /**
     * @return - Hostname of the machine
     */
    private String getHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return null;
    }

}
