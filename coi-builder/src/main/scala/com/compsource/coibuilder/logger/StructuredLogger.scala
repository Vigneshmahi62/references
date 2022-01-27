package com.compsource.coibuilder.logger

import com.compsource.coibuilder.CommonObject.{SERVICE_NAME, hostname}
import net.logstash.logback.argument.StructuredArguments.kv
import org.slf4j.{Logger, LoggerFactory}

/**
 * It uses slf4j logger to log the message. It also implicitly use the class that inherits Log trait
 * to include custom fields in structured logging.
 */
object StructuredLogger {
  /**
   * Slf4j Logger Object
   */
  private final val logger = LoggerFactory.getLogger(classOf[LoggerToLog])
  /**
   * Custom Logger of this service
   */
  final val log: LoggerToLog = new LoggerToLog(logger)

  /**
   * Class that is used to include custom fields in structured logging
   *
   * @param logger - Slf4j logger
   */
  implicit class LoggerToLog(logger: Logger) extends Log {
    override def debug(message: String): Unit = {
      logger.debug(message)
    }

    override def debug(message: String, correlation_id: String, status: String,
                       technical_details: String): Unit = {
      logger.debug(message, kv("correlation_id", correlation_id), kv("status", status),
        kv("hostname", hostname), kv("service_name", SERVICE_NAME),
        kv("technical_details", technical_details))
    }

    override def info(message: String): Unit = {
      logger.info(message)
    }

    override def info(message: String, correlation_id: String, status: String,
                      technical_details: String): Unit = {
      logger.info(message, kv("correlation_id", correlation_id), kv("status", status),
        kv("hostname", hostname), kv("service_name", SERVICE_NAME),
        kv("technical_details", technical_details))
    }

    override def warn(message: String): Unit = {
      logger.warn(message)
    }

    override def warn(message: String, correlation_id: String, status: String,
                      technical_details: String): Unit = {
      logger.warn(message, kv("correlation_id", correlation_id), kv("status", status),
        kv("hostname", hostname), kv("service_name", SERVICE_NAME),
        kv("technical_details", technical_details))
    }

    override def error(message: String): Unit = {
      logger.error(message)
    }

    override def error(message: String, correlation_id: String, status: String,
                       technical_details: String): Unit = {
      logger.error(message, kv("correlation_id", correlation_id), kv("status", status),
        kv("hostname", hostname), kv("service_name", SERVICE_NAME),
        kv("technical_details", technical_details))
    }
  }
}
