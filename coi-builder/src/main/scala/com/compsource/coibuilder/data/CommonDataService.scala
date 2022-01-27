package com.compsource.coibuilder.data

import java.time.Instant
import java.util.Date
import com.compsource.coibuilder.data.CassandraConnector._
import com.compsource.coibuilder.data.model.Model.{COIRecipients, COIRequested, CertificateOfInsurance}
import com.compsource.coibuilder.logger.StructuredLogger.log
import com.datastax.driver.core.{BoundStatement, ConsistencyLevel, ResultSet}

/**
 * Acts as DAO. It has prepared statements against the each tables that needs to be interacted.
 * It has methods to set values of constructed prepared statement to BoundStatement
 */
object CommonDataService {

  /**
   * Batch Insert - coi_requested, coi_recipients, certificates_of_insurance
   */
  private[this] val coiBatchInsertStmtWithNewRecipient = session.prepare(
    """
      |BEGIN BATCH
      | insert into coi_requested (coi_request_id, account_id, is_new_recipient,
      | recipient_id, recipient_name, recipient_email, is_valid, reason) values
      | (:coi_request_id, :account_id, :is_new_recipient,
      | :recipient_id, :recipient_name, :recipient_email, :is_valid, :reason);
      |
      | insert into coi_recipients (recipient_id, recipient_name, recipient_email,
      | create_correlation_id, created_at) values
      | (:recipient_id1, :recipient_name1, :recipient_email1, :create_correlation_id1,
      | :created_at1);
      |
      | insert into certificates_of_insurance (coi_id, recipient_name, recipient_email,
      | policy_holder_name, existing_policy_coverage_limit, created_at) values
      | (:coi_id2, :recipient_name2, :recipient_email2, :policy_holder_name2,
      | :existing_policy_coverage_limit2, :created_at2);
      |
      | APPLY BATCH
      | """.stripMargin).setConsistencyLevel(ConsistencyLevel.QUORUM)

  /**
   * Batch Insert - coi_requested, certificates_of_insurance
   */
  private[this] val coiBatchInsertStmtWithoutNewRecipient = session.prepare(
    """
      |BEGIN BATCH
      | insert into coi_requested (coi_request_id, account_id, is_new_recipient,
      | recipient_id, recipient_name, recipient_email, is_valid, reason) values
      | (:coi_request_id, :account_id, :is_new_recipient,
      | :recipient_id, :recipient_name, :recipient_email, :is_valid, :reason);
      |
      | insert into certificates_of_insurance (coi_id, recipient_name, recipient_email,
      | policy_holder_name, existing_policy_coverage_limit, created_at) values
      | (:coi_id2, :recipient_name2, :recipient_email2, :policy_holder_name2,
      | :existing_policy_coverage_limit2, :created_at2);
      |
      | APPLY BATCH
      | """.stripMargin).setConsistencyLevel(ConsistencyLevel.QUORUM)

  /**
   * Insert query of processed_events_by_correlation_id table
   */
  private[this] val processedEventInsertStmt = session.prepare(
    """INSERT INTO processed_events_by_correlation_id(
      | correlation_id, service_name, processing_completion_time)
      | VALUES (
      | :correlation_id, :service_name, :processing_completion_time) IF NOT EXISTS""".stripMargin
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
   * Insert query of coi_requested table
   */
  private[this] val coiRequestedInsertStmt = session.prepare(
    """INSERT INTO coi_requested(
      | coi_request_id, account_id,
      | is_new_recipient, recipient_id, recipient_name, recipient_email,
      | is_valid, reason)
      | VALUES (
      | :coi_request_id, :account_id,
      | :is_new_recipient, :recipient_id, :recipient_name,
      | :recipient_email, :is_valid, :reason)""".stripMargin
  ).setConsistencyLevel(ConsistencyLevel.QUORUM)

  /**
   * Insert query of coi_recipients table
   */
  private[this] val coiRecipientsInsertStmt = session.prepare(
    """INSERT INTO coi_recipients(
      | recipient_id, recipient_name,
      | recipient_email, create_correlation_id,
      | created_at)
      | VALUES (
      | :recipient_id, :recipient_name,
      |  :recipient_email, :create_correlation_id,
      | :created_at)""".stripMargin
  ).setConsistencyLevel(ConsistencyLevel.QUORUM)

  /**
   * Insert query of certificates_of_insurance table
   */
  private[this] val coiInsertStmt = session.prepare(
    """INSERT INTO certificates_of_insurance(
      | coi_id, recipient_name,
      | recipient_email, policy_holder_name,
      | existing_policy_coverage_limit,
      | created_at)
      | VALUES (
      | :coi_id, :recipient_name,
      |  :recipient_email, :policy_holder_name,
      |  :existing_policy_coverage_limit,
      | :created_at)""".stripMargin
  ).setConsistencyLevel(ConsistencyLevel.QUORUM)

  /**
   * Select query of coi_requested table
   */
  private[this] val selectStmtForCoiRequest = session.prepare(
    """SELECT * FROM coi_requested
      | WHERE coi_request_id =:coiRequestId""".stripMargin)
    .setConsistencyLevel(ConsistencyLevel.QUORUM)

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
   * Selects the coi_requested records by correlation Id
   *
   * @param coiRequestGuid - Coi Request Id / Correlation Id
   * @return ResultSet or null
   */
  def selectRecord(coiRequestGuid: String): ResultSet = {
    val boundedStmt = selectStmtForCoiRequest.bind().setString("coiRequestId", coiRequestGuid)
    executeQuery(boundedStmt, coiRequestGuid)
  }

  /**
   * Insert record to coi_requested table
   *
   * @param coiRequest - CoiRequested Object
   * @return ResultSet or null
   */
  def saveCoiRequest(coiRequest: COIRequested): ResultSet = {
    val boundedStmt: BoundStatement = coiRequestedInsertStmt.bind()
      .setString("coi_request_id", coiRequest.coiRequestId)
      .setInt("account_id", coiRequest.accountId)
      .setBool("is_new_recipient", coiRequest.isNewRecipient)
      .setString("recipient_id", coiRequest.recipientId)
      .setString("recipient_name", coiRequest.recipientName)
      .setString("recipient_email", coiRequest.recipientEmail)
      .setBool("is_valid", coiRequest.is_valid)
      .setString("reason", coiRequest.reason)

    executeQuery(boundedStmt, coiRequest.coiRequestId)
  }

  /**
   * Selects the policy_holders records by account id & correlation Id
   *
   * @param accountId     - Account Id
   * @param correlationId - Correlation Id
   * @return ResultSet or null
   */
  def selectFromPolicyHolders(accountId: Int, correlationId: String): ResultSet = {
    val boundedStmt = selectStmtForPolicyHolders.bind()
      .setInt("account_Id", accountId)
    executeQuery(boundedStmt, correlationId)
  }

  /**
   * Selects the processed_events_by_correlation_id records by correlation Id and Service Name
   *
   * @param correlationId - Correlation Id
   * @param serviceName   - Service Name
   * @return ResultSet or Null
   */
  def selectFromProcessedEventsByCorrId(correlationId: String, serviceName: String): ResultSet = {
    val boundStatement = selectStmtForProcessedEventsByCorrId
      .bind()
      .setString("correlation_id", correlationId)
      .setString("service_name", serviceName)

    executeQuery(boundStatement, correlationId)
  }

  /**
   * Selects the certificates_of_insurance records by correlation Id
   *
   * @param correlationId - Correlation Id
   * @return ResultSet or null
   */
  def selectFromCertificatesOfInsurance(correlationId: String): ResultSet = {
    val boundStatement = selectStmtForCertificateOfInsurance
      .bind()
      .setString("coi_id", correlationId)
    executeQuery(boundStatement, correlationId)

  }

  /**
   * Selects the published_events_by_correlation_id records by correlation Id and Downstream
   * Kafka topic
   *
   * @param correlationId - Correlation Id
   * @param kafkaTopic    - Downstream Kafka topic
   * @return ResultSet or Null
   */
  def selectFromPublishedEventsByCorrId(correlationId: String, kafkaTopic: String): ResultSet = {
    val boundStatement = selectStmtForPublishedEventsByCorrId
      .bind()
      .setString("correlation_id", correlationId)
      .setString("kafka_topic", kafkaTopic)
    executeQuery(boundStatement, correlationId)
  }

  /**
   * Selects the coi_recipients records by COI Recipient Id & correlation Id
   *
   * @param coiRecipientId - Coi Recipient Id
   * @param correlationId  - Correlation Id
   * @return ResultSet or Null
   */
  def selectFromCoiRecipients(coiRecipientId: String, correlationId: String): ResultSet = {
    val boundStatement = selectStmtForCoiRecipients.bind()
      .setString("recipient_id", coiRecipientId)

    executeQuery(boundStatement, correlationId)
  }

  /**
   * Insert record to coi_recipients table
   *
   * @param coiRecipients - COIRecipients object
   * @return ResultSet or Null
   */
  def insertRecordToCoiRecipients(coiRecipients: COIRecipients): ResultSet = {
    val now = new Date
    val boundedStmt: BoundStatement = coiRecipientsInsertStmt.bind()
      .setString("recipient_id", coiRecipients.recipient_id)
      .setString("recipient_name", coiRecipients.recipient_name)
      .setString("recipient_email", coiRecipients.recipient_email)
      .setString("create_correlation_id", coiRecipients.create_correlation_id)
      .setTimestamp("created_at", now)
    executeQuery(boundedStmt, coiRecipients.create_correlation_id)
  }

  /**
   * Insert record to certificates_of_insurance table
   *
   * @param certificateOfInsurance - CertificatesOfInsurance Object
   * @return ResultSet or Null
   */
  def insertRecordToCoi(certificateOfInsurance: CertificateOfInsurance): ResultSet = {
    val now = new Date
    val boundedStmt: BoundStatement = coiInsertStmt.bind()
      .setString("coi_id", certificateOfInsurance.coi_id)
      .setString("recipient_name", certificateOfInsurance.recipient_name)
      .setString("recipient_email", certificateOfInsurance.recipient_email)
      .setString("policy_holder_name", certificateOfInsurance.policy_holder_name)
      .setLong("existing_policy_coverage_limit", certificateOfInsurance.existing_policy_coverage_limit)
      .setTimestamp("created_at", now)
    executeQuery(boundedStmt, certificateOfInsurance.coi_id)
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
   * Insert record to published_events_by_correlation_id table
   *
   * @param correlationId        - Correlation Id
   * @param kafkaTopic           - Downstream kafka topic
   * @param publishedServiceName - Service Name
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

  /**
   * Constructs BoundStatement to insert records in coi_requested and certificates_of_insurance
   *
   * @param certificateOfInsurance - CertificatesOfInsurance Object
   * @param coiRequest             - CoiRequested Object
   * @return BoundStatement
   */
  def getBoundStatementWithoutRecipient(certificateOfInsurance: CertificateOfInsurance,
                                        coiRequest: COIRequested): BoundStatement = {
    coiBatchInsertStmtWithoutNewRecipient.bind()
      .setString("coi_request_id", coiRequest.coiRequestId)
      .setInt("account_id", coiRequest.accountId)
      .setBool("is_new_recipient", coiRequest.isNewRecipient)
      .setString("recipient_id", coiRequest.recipientId)
      .setString("recipient_name", coiRequest.recipientName)
      .setString("recipient_email", coiRequest.recipientEmail)
      .setBool("is_valid", coiRequest.is_valid)
      .setString("reason", coiRequest.reason)

      .setString("coi_id2", certificateOfInsurance.coi_id)
      .setString("recipient_name2", certificateOfInsurance.recipient_name)
      .setString("recipient_email2", certificateOfInsurance.recipient_email)
      .setString("policy_holder_name2", certificateOfInsurance.policy_holder_name)
      .setLong("existing_policy_coverage_limit2", certificateOfInsurance.existing_policy_coverage_limit)
      .setTimestamp("created_at2", Date.from(Instant.now()))
  }

  /**
   * Constructs BoundStatement to insert records in coi_requested, certificates_of_insurance
   * and coi_recipients
   *
   * @param certificateOfInsurance - CertificatesOfInsurance Object
   * @param coiRequest             - CoiRequested Object
   * @param newCoiRecipient        - CoiRecipient Object
   * @return BoundStatement
   */
  def getBoundStatementWithRecipient(certificateOfInsurance: CertificateOfInsurance,
                                     coiRequest: COIRequested, newCoiRecipient: COIRecipients): BoundStatement = {

    coiBatchInsertStmtWithNewRecipient.bind()
      .setString("coi_request_id", coiRequest.coiRequestId)
      .setInt("account_id", coiRequest.accountId)
      .setBool("is_new_recipient", coiRequest.isNewRecipient)
      .setString("recipient_id", coiRequest.recipientId)
      .setString("recipient_name", coiRequest.recipientName)
      .setString("recipient_email", coiRequest.recipientEmail)
      .setBool("is_valid", coiRequest.is_valid)
      .setString("reason", coiRequest.reason)

      .setString("recipient_id1", newCoiRecipient.recipient_id)
      .setString("recipient_name1", newCoiRecipient.recipient_name)
      .setString("recipient_email1", newCoiRecipient.recipient_email)
      .setString("create_correlation_id1", newCoiRecipient.create_correlation_id)
      .setTimestamp("created_at1", Date.from(Instant.now()))

      .setString("coi_id2", certificateOfInsurance.coi_id)
      .setString("recipient_name2", certificateOfInsurance.recipient_name)
      .setString("recipient_email2", certificateOfInsurance.recipient_email)
      .setString("policy_holder_name2", certificateOfInsurance.policy_holder_name)
      .setLong("existing_policy_coverage_limit2", certificateOfInsurance.existing_policy_coverage_limit)
      .setTimestamp("created_at2", Date.from(Instant.now()))
  }

  /**
   * Batch Insert to certificates_of_insurance, coi_requested and coi_recipients
   *
   * @param certificateOfInsurance - CertificateOfInsurance object
   * @param coiRequest             - COIRequested Object
   * @param newCoiRecipient        - CoiRecipients Object
   * @return true if inserted else false
   */
  def batchCOIInsert(certificateOfInsurance: CertificateOfInsurance,
                     coiRequest: COIRequested, newCoiRecipient: COIRecipients): Boolean = {
    try {
      var boundStatement: BoundStatement = null

      if (newCoiRecipient != null)
        boundStatement = getBoundStatementWithRecipient(certificateOfInsurance,
          coiRequest, newCoiRecipient)
      else
        boundStatement = getBoundStatementWithoutRecipient(certificateOfInsurance,
          coiRequest)

      executeQuery(boundStatement, coiRequest.coiRequestId)
      return true
    }
    catch {
      case exception: Exception =>
        log.error("database not accessible", status = "failure", technical_details =
          exception.getMessage, correlation_id = coiRequest.coiRequestId)
        System.exit(1)
    }
    false
  }

}
