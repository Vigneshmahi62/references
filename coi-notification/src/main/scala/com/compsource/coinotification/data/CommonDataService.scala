package com.compsource.coinotification.data

import com.compsource.coinotification.data.CassandraConnector._
import com.compsource.coinotification.logger.StructuredLogger.log
import com.datastax.driver.core.{ConsistencyLevel, PreparedStatement, ResultSet}

import java.time.Instant
import java.util.Date

/**
 * Acts as DAO. It has prepared statements against the each tables that needs to be interacted.
 * It has methods to set values of constructed prepared statement to BoundStatement
 */
object CommonDataService {

  /**
   * Insert query of coi_notifications table
   */
  private[this] val coiNotificationInsertStmt: PreparedStatement = session.prepare(
    """INSERT INTO coi_notifications(
      | coi_notification_id, email_body, email_from, email_subject, email_to,
      | notification_created_at)
      | VALUES (
      | :coi_notification_id, :email_body, :email_from, :email_subject, :email_to,
      | :notification_created_at)"""
      .stripMargin
  ).setConsistencyLevel(ConsistencyLevel.QUORUM)

  /**
   * Insert query of processed_events_by_correlation_id table
   */
  private[this] val processedEventInsertStmt = session.prepare(
    """INSERT INTO processed_events_by_correlation_id(
      | correlation_id, service_name, processing_completion_time)
      | VALUES (
      | :correlation_id, :service_name, :processing_completion_time)""".stripMargin
  ).setConsistencyLevel(ConsistencyLevel.QUORUM)

  /**
   * Insert query of published_events_by_correlation_id table
   */
  private[this] val publishedEventInsertStmt = session.prepare(
    """INSERT INTO published_events_by_correlation_id(
      | correlation_id, kafka_topic, time_published, publishing_service_name)
      | VALUES (
      | :correlation_id, :kafka_topic, :time_published, :publishing_service_name)""".stripMargin
  ).setConsistencyLevel(ConsistencyLevel.QUORUM)

  /**
   * Select query of policy_holders table
   */
  private[this] val selectStmtForPolicyHolders = session.prepare(
    """SELECT * FROM policy_holders
      | WHERE account_id =:account_id
      |""".stripMargin).setConsistencyLevel(ConsistencyLevel.QUORUM)

  /**
   * Select query of processed_events_by_correlation_id table
   */
  private[this] val selectStmtForProcessedEventsByCorrId = session.prepare(
    """SELECT * FROM processed_events_by_correlation_id
      | WHERE correlation_id =:correlation_id and service_name = :service_name
      |""".stripMargin).setConsistencyLevel(ConsistencyLevel.QUORUM)

  /**
   * Select query of published_events_by_correlation_id table
   */
  private[this] val selectStmtForPublishedEventsByCorrId = session.prepare(
    """SELECT * FROM published_events_by_correlation_id
      | WHERE correlation_id =:correlation_id and kafka_topic = :kafka_topic
      |""".stripMargin).setConsistencyLevel(ConsistencyLevel.QUORUM)

  /**
   * Select query of coi_recipients table
   */
  private[this] val selectStmtForCoiRecipients = session.prepare(
    """
      | SELECT * FROM coi_recipients
      | WHERE recipient_id = :recipient_id
      |""".stripMargin).setConsistencyLevel(ConsistencyLevel.QUORUM)

  /**
   * Select query of certificates_of_insurance table
   */
  private[this] val selectStmtForCertificateOfInsurance = session.prepare(
    """
      |SELECT * FROM certificates_of_insurance
      | WHERE coi_id =:coi_id
      |""".stripMargin).setConsistencyLevel(ConsistencyLevel.QUORUM)

  /**
   * Selects the policy_holders records by the account id
   *
   * @param accountId     - Account Id
   * @param correlationId - Correlation Id
   * @return - ResultSet or null
   */
  def selectFromPolicyHolders(accountId: Int, correlationId: String): ResultSet = {
    val boundedStmt = selectStmtForPolicyHolders.bind()
      .setInt("account_Id", accountId)
    executeQuery(boundedStmt, correlationId)
  }

  /**
   * Selects the processed_events_by_correlation_id records by the correlation id and service name
   *
   * @param correlationId - Correlation Id
   * @param serviceName   - Service Name
   * @return - ResultSet or null
   */
  def selectFromProcessedEventsByCorrId(correlationId: String, serviceName: String): ResultSet = {
    val boundStatement = selectStmtForProcessedEventsByCorrId
      .bind()
      .setString("correlation_id", correlationId)
      .setString("service_name", serviceName)

    executeQuery(boundStatement, correlationId)
  }

  /**
   * Selects the certificates_of_insurance records by the correlation id
   *
   * @param correlationId - Correlation Id
   * @return - ResultSet or null
   */
  def selectFromCertificatesOfInsurance(correlationId: String): ResultSet = {
    val boundStatement = selectStmtForCertificateOfInsurance
      .bind()
      .setString("coi_id", correlationId)
    executeQuery(boundStatement, correlationId)

  }

  /**
   * Selects the published_events_by_correlation_id records by the correlation id and downstream
   * kafka topic
   *
   * @param correlationId - Correlation Id
   * @param kafkaTopic    - Downstream Kafka topic
   * @return - ResultSet or null
   */
  def selectFromPublishedEventsByCorrId(correlationId: String, kafkaTopic: String): ResultSet = {
    val boundStatement = selectStmtForPublishedEventsByCorrId
      .bind()
      .setString("correlation_id", correlationId)
      .setString("kafka_topic", kafkaTopic)
    executeQuery(boundStatement, correlationId)
  }

  /**
   * Selects the coi_recipients records by the correlation id and coi_recipient id
   *
   * @param coiRecipientId - COI Recipient Id
   * @param correlationId  - Correlation Id
   * @return - ResultSet or null
   */
  def selectFromCoiRecipients(coiRecipientId: String, correlationId: String): ResultSet = {
    val boundStatement = selectStmtForCoiRecipients.bind()
      .setString("recipient_id", coiRecipientId)

    executeQuery(boundStatement, correlationId)
  }

  /**
   * Insert record to coi_notifications
   *
   * @param coi_notification_id - COI Notification Id / Correlation Id
   * @param email_body          - Body of mail
   * @param email_from          - From Address
   * @param email_subject       - Subject of the mail
   * @param email_to            - To Address
   * @return true if inserted else false
   */
  def insertRecordToCoiNotification(coi_notification_id: String, email_body: String,
                                    email_from: String, email_subject: String, email_to: String)
  : Boolean = {
    var isInserted = false
    try {
      val boundStatement = coiNotificationInsertStmt.bind()
        .setString("coi_notification_id", coi_notification_id)
        .setString("email_body", email_body)
        .setString("email_from", email_from)
        .setString("email_subject", email_subject)
        .setString("email_to", email_to)
        .setTimestamp("notification_created_at", Date.from(Instant.now))

      val result = executeQuery(boundStatement, coi_notification_id).one()
      if (result != null)
        isInserted = result.getBool("[applied]")
    }
    catch {
      case exception: Exception =>
        log.error("database not accessible", status = "failure", technical_details =
          exception.getMessage, correlation_id = coi_notification_id)
        System.exit(1)
    }
    isInserted
  }

  /**
   * Insert record to processed_events_by_correlation_id
   *
   * @param correlationId - Correlation Id
   * @param serviceName   - Service Name
   * @return true if inserted else false
   */
  def insertRecordToProcessedEvents(correlationId: String, serviceName: String): Boolean = {
    var isInserted = false
    try {
      val boundStatement = processedEventInsertStmt.bind()
        .setString("correlation_id", correlationId)
        .setString("service_name", serviceName)
        .setTimestamp("processing_completion_time", Date.from(Instant.now))

      val result = executeQuery(boundStatement, correlationId).one()
      if (result != null)
        isInserted = result.getBool("[applied]")
    }
    catch {
      case exception: Exception =>
        log.error("database not accessible", status = "failure", technical_details =
          exception.getMessage, correlation_id = correlationId)
        System.exit(1)
    }
    isInserted
  }

  /**
   * Insert record to published_events_by_correlation_id
   *
   * @param correlationId        - Correlation Id
   * @param kafkaTopic           - Downstream kafka topic
   * @param publishedServiceName - Published service name
   * @return true if inserted else false
   */
  def insertRecordToPublishedEvents(correlationId: String, kafkaTopic: String,
                                    publishedServiceName: String): Boolean = {
    try {
      val boundStatement = publishedEventInsertStmt.bind()
        .setString("correlation_id", correlationId)
        .setString("kafka_topic", kafkaTopic)
        .setTimestamp("time_published", Date.from(Instant.now))
        .setString("publishing_service_name", publishedServiceName)

      executeQuery(boundStatement, correlationId)
      return true
    }
    catch {
      case exception: Exception =>
        log.error("database not accessible", status = "failure", technical_details =
          exception.getMessage, correlation_id = correlationId)
        System.exit(1)
    }
    false
  }

}
