package com.compsource.coibuilder.service

import akka.actor.ActorSystem
import akka.kafka.ConsumerMessage.CommittableOffset
import akka.kafka.scaladsl.{Committer, Consumer}
import akka.kafka.{CommitterSettings, ConsumerSettings, ProducerSettings, Subscriptions}
import akka.stream.Materializer
import akka.stream.scaladsl.Keep
import com.compsource.coibuilder.CommonObject.{SERVICE_NAME, system}
import com.compsource.coibuilder.data.CommonDataService._
import com.compsource.coibuilder.data.model.Model._
import com.compsource.coibuilder.logger.StructuredLogger.log
import com.compsource.coibuilder.utils.ValidationUtils
import com.compsource.coibuilder.utils.ValidationUtils.{isNullOrEmpty, loadCOIRequestedObjFromJson}
import com.datastax.driver.core.ResultSet
import com.datastax.driver.core.utils.UUIDs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.{Callback, Producer, ProducerRecord, RecordMetadata}
import org.apache.kafka.common.serialization.{StringDeserializer, StringSerializer}
import play.api.libs.json.{Json, OFormat}

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

/**
 * Object that holds logic to process COI requests read from upstream topic in step by step
 */
object CoiBuilderService {

  /**
   * Model class to store the required event properties
   *
   * @param topic     - Topic Name
   * @param partition - Partition Id
   * @param offset    - Offset of message
   * @param timestamp - Message timestamp
   * @param key       - Message key
   * @param value     - Message
   */
  private[this] case class KafkaMessage(
                                         topic: String,
                                         partition: Int,
                                         offset: Long,
                                         timestamp: Long,
                                         key: String,
                                         value: String)

  /**
   * Format of the message published to the COICreated topic
   *
   * @param coiId - Certificate of Insurance Id
   */
  case class CoiCreatedMessage(coiId: String)


  object CoiCreatedMessage {
    implicit val fmtJson: OFormat[CoiCreatedMessage] = Json.format[CoiCreatedMessage]
  }

  /**
   * Creates PolicyHolder object from the policy_holders table
   *
   * @param accountId     - Account Id received from COIRequested Json
   * @param correlationId - Correlation Id
   * @return PolicyHolder obj if there is a record else throws an exception
   */
  private[this] def loadExistingPolicyHolder(accountId: Int, correlationId: String): PolicyHolders = {
    val resultSet = selectFromPolicyHolders(accountId, correlationId)
    val row = resultSet.one()
    if (row != null)
      PolicyHolders(accountId, row.getString("policy_holder_name"),
        row.getLong("existing_policy_coverage_limit"))
    else
      null
  }

  /**
   * Constructs CertificateOfInsurance object from the COIRequested object
   *
   * @param coiRequest   - COIRequested object
   * @param policyHolder - PolicyHolder object
   * @return CertificateOfInsurance object
   */
  private[this] def getCertificateOfInsurance(coiRequest: COIRequested, policyHolder: PolicyHolders)
  : CertificateOfInsurance = {

    CertificateOfInsurance(coiRequest.coiRequestId, coiRequest.recipientName, coiRequest.recipientEmail,
      policyHolder.policy_holder_name, policyHolder.existing_policy_coverage_limit)
  }

  /**
   * Checks if the recipient exists or not
   *
   * @param coiRequested - COIRequested object
   * @return true if exists else false
   */
  private[this] def isExistingRecipient(coiRequested: COIRequested): Boolean = {
    val coiRecipientResult = selectFromCoiRecipients(coiRequested.recipientId, coiRequested
      .coiRequestId)
    val coiRecipient = coiRecipientResult.all()

    if (coiRecipient.isEmpty) {
      false
    } else {
      val row = coiRecipient.get(0)
      coiRequested.recipientName = row.getString("recipient_name")
      coiRequested.recipientEmail = row.getString("recipient_email")
      true
    }
  }

  /**
   * This method will take COIRequested object as input.
   * Call the method to load PolicyHolders object
   * If is_new_recipient flag is true, inserts the recipient record to coi_recipients table
   * Call the method to create & save certificate of insurance
   * Persist the COIRequested event to coi_requested table
   *
   * @param coiRequest - COIRequested Object
   * @return true if message successfully saved else false
   */
  private[this] def processMessage(coiRequest: COIRequested): Boolean = {
    var success = false
    // Create PolicyHolders
    val policyHolder = loadExistingPolicyHolder(coiRequest.accountId, coiRequest.coiRequestId)
    if (policyHolder != null) {
      // Create CertificateOfInsurance
      val certificateOfInsurance = getCertificateOfInsurance(coiRequest, policyHolder)
      // If isNewRecipient is true, create COIRecipients & include it to batch insert
      // Batch Insert includes coi_requested, coi_recipients, certificates_of_insurance
      if (coiRequest.isNewRecipient) {
        val newCoiRecipient = COIRecipients(coiRequest.recipientId, coiRequest.recipientName,
          coiRequest.recipientEmail,
          coiRequest.coiRequestId)
        success = batchCOIInsert(certificateOfInsurance, coiRequest, newCoiRecipient)
      }
      // Check if received recipientId has records in coi_recipients table
      // If so, do batch insert. Batch Insert includes coi_requested, coi_recipients
      else if (isExistingRecipient(coiRequest)) {
        success = batchCOIInsert(certificateOfInsurance, coiRequest, null)
      }
      // Persist record to coi_requested table by saying Recipient information not available
      else {
        log.error("recipient information not available", status = "failure", technical_details =
          coiRequest.recipientId, correlation_id = coiRequest.coiRequestId)
        coiRequest.is_valid = false
        coiRequest.reason = "Recipient information not available"
        saveCoiRequest(coiRequest)
      }
    }
    // Persist record to coi_requested table by saying Policy holder information information not
    // available
    else {
      log.error("policy holder information not available", status = "failure", technical_details =
        coiRequest.accountId.toString, correlation_id = coiRequest.coiRequestId)
      coiRequest.is_valid = false
      coiRequest.reason = "Policy holder information not available"
      saveCoiRequest(coiRequest)
    }
    success
  }

  /**
   * Checks if the request has been processed already in processed_events_by_correlation_id table
   *
   * @param coiRequest - Constructed COIRequested object
   * @return true if not processed else false
   */
  private[this] def isRequestProcessed(coiRequest: COIRequested): Boolean = {
    val processedEventResult: ResultSet = selectFromProcessedEventsByCorrId(coiRequest.coiRequestId,
      SERVICE_NAME)
    val processedEvents = processedEventResult.all()

    !processedEvents.isEmpty
  }

  /**
   * Checks if the message published already
   *
   * @param coiRequest - COIRequested object
   * @param topicName  - Downstream topic name
   * @return
   */
  private[this] def isMessagePublished(coiRequest: COIRequested, topicName: String): Boolean = {
    val publishedMsgResult = selectFromPublishedEventsByCorrId(coiRequest.coiRequestId, topicName)
    val publishedMsg = publishedMsgResult.all()

    !publishedMsg.isEmpty
  }

  /**
   * Method to publish message to COICreated topic
   *
   * @param producer          - Producer
   * @param producerTopicName - Producer Topic Name
   * @param key               - Message Key
   * @param value             - Message Value
   * @return
   */
  private[this] def publishMessageToCoiCreatedTopic(producer: Producer[String, String],
                                                    producerTopicName: String, key: String, value: String): Boolean = {
    producer.send(new ProducerRecord(producerTopicName, key, value), ResultClass())
    true
  }

  /**
   * Method to call publish message to COICreated topic & insert records to published events
   *
   * @param producer          - Producer
   * @param coiRequest        - COIRequested object
   * @param producerTopicName - Producer topic name
   * @return
   */
  private[this] def publishMessageAndLogInDatabase(producer: Producer[String, String],
                                                   coiRequest: COIRequested, producerTopicName: String): Boolean = {
    val correlationId = coiRequest.coiRequestId
    //Check if the message is already published or not
    if (!isMessagePublished(coiRequest, producerTopicName)) {
      val coiCreatedMessage = CoiCreatedMessage.fmtJson
        .writes(CoiCreatedMessage(correlationId)).toString()

      publishMessageToCoiCreatedTopic(producer, producerTopicName, correlationId, coiCreatedMessage)
      insertRecordToPublishedEvents(coiRequest.coiRequestId, producerTopicName, SERVICE_NAME)
    }

    true
  }

  /** Starts processing COIRequested message upon successful validation
   *
   * @param actorSystem       - Actor System
   * @param coiRequest        - COIRequested Object
   * @param producerTopicName - Producer topic name to send COICreated message
   */
  private[this] def processCoiRequestedObject(actorSystem: ActorSystem, coiRequest: COIRequested,
                                              producerTopicName: String, json: String,
                                              producer: Producer[String, String]): Unit = {
    val correlationId = coiRequest.internalMessageId

    //Check if the request is valid
    if (ValidationUtils.isValidRequest(coiRequest, json)) {
      //Check if the request is already processed or not
      if (!isRequestProcessed(coiRequest)) {
        // process the message
        val success = processMessage(coiRequest)
        if (success) {
          // Write to processed events
          if (!insertRecordToProcessedEvents(coiRequest.coiRequestId, SERVICE_NAME)) {
            log.error(correlationId + " Unable to insert record to the Processed events table")
            System.exit(1)
          }
          //Check and Write to Published events
          if (publishMessageAndLogInDatabase(producer, coiRequest, producerTopicName)) {
            log.info("event processed", coiRequest.coiRequestId,
              "success", null)
          }
        }
      } else {
        log.warn("duplicate event", coiRequest.coiRequestId,
          "success", null)
        //If already processed, Check and Write to Published events
        if (publishMessageAndLogInDatabase(producer, coiRequest, producerTopicName)) {
          log.info("event processed", coiRequest.coiRequestId,
            "success", null)
        }
      }
    }
    else {
      coiRequest.is_valid = false
      // coiRequest.reason = "Invalid request - doesn't conforms to Business rules"
      saveCoiRequest(coiRequest)
    }

  }

  /**
   * Base method of all steps to process coi requested event upon successful Json validation
   *
   * @param json              - Received Json message
   * @param producerTopicName - Producer Topic Name
   * @param producer          - Producer
   */
  private[this] def processSuccessEvent(json: String, producerTopicName: String,
                                        producer: Producer[String, String]): Unit = {
    val coiRequest: COIRequested = loadCOIRequestedObjFromJson(json)
    if (coiRequest != null && !isNullOrEmpty(coiRequest.coiRequestId)) {
      val correlationId = "<: " + coiRequest.coiRequestId + " :> "
      coiRequest.internalMessageId = correlationId
      log.debug(correlationId + " COI Object created successfully from the COI " +
        "request Json")
      processCoiRequestedObject(system, coiRequest, producerTopicName, json, producer)
    } else {
      val sysCoiId = UUIDs.timeBased().toString
      log.info("event received", sysCoiId, "success", null)
      log.error("malformed json", status = "failure", technical_details = json,
        correlation_id = sysCoiId)
      val coiRequest: COIRequested = COIRequested(sysCoiId, 0, isNewRecipient = false,
        null, null, null, is_valid = false, reason = "malformed json")
      saveCoiRequest(coiRequest)
    }
  }

  /**
   * Reads event from kafka topic & initiates the process
   *
   * @param consumerTopicName - Upstream Consumer Topic Name
   * @param producerTopicName - Downstream Producer Topic Name
   */
  def startConsuming(consumerTopicName: String,
                     producerTopicName: String): Unit = {
    implicit val ec: ExecutionContext = system.dispatcher
    implicit val m: Materializer = Materializer.createMaterializer(system)
    val consumerSettings = getConsumerSettings(system)

    val committerSettings = CommitterSettings(system.settings.config.
      getConfig("akka.kafka.committer"))

    val producerSettings = getProducerSettings(system)

    val producer = producerSettings.createKafkaProducer()

    Consumer.committableSource(consumerSettings,
      Subscriptions.topics(consumerTopicName))
      .mapAsync(system.settings.config.getConfig("akka.kafka.consumer")
        .getInt("parallelism"))(msg => {
        Future {

          val kafkaMessage = KafkaMessage(msg.record.topic, msg.record.partition, msg.record.offset,
            msg.record.timestamp, msg.record.key, msg.record.value)
          log.debug("Incoming Message :: " + kafkaMessage.value)

          val offSet: CommittableOffset = msg.committableOffset

          Try {
            Json.parse(kafkaMessage.value)
          } match {
            case Success(json) =>
              log.debug("JSON Parsing Success! " + json.toString())
              processSuccessEvent(kafkaMessage.value, producerTopicName, producer)
            case Failure(reason) =>
              log.error("malformed json", status = "failure", technical_details = kafkaMessage
                .value, correlation_id = null)
              val coiRequest: COIRequested = COIRequested(UUIDs.timeBased().toString, 0,
                isNewRecipient = false,
                null, null, null, is_valid = false, reason = "malformed json")
              saveCoiRequest(coiRequest)
          }
          offSet
        }
      })
      .toMat(Committer.sink(committerSettings))(Keep.both)
      .run()

  }

  /**
   * Class to check if message sent to Downstream topic
   */
  case class ResultClass() extends Callback {
    override def onCompletion(metadata: RecordMetadata, exception: Exception): Unit = {
      if (exception != null) {
        exception.printStackTrace()
        log.error("event not published", status = "failure", technical_details = exception.getMessage,
          correlation_id = null)
        sys.exit(4)
      } else {
        log.debug("Successfully inserted Record in partition: " + metadata.partition() +
          ", offset: " + metadata.offset())
      }
    }
  }

  /**
   * Constructs ConsumerSettings object
   *
   * @param actorSystem - Actor System
   * @return Consumer Settings
   */
  private[this] def getConsumerSettings(actorSystem: ActorSystem):
  ConsumerSettings[String, String] = {

    val bootstrapServers = actorSystem.settings.config.getConfig("kafka.cluster").
      getString("bootstrap-servers")

    val consumerConfig = actorSystem.settings.config.getConfig("akka.kafka.consumer")

    ConsumerSettings(consumerConfig, new StringDeserializer, new StringDeserializer)
      .withBootstrapServers(bootstrapServers)
      .withGroupId(consumerConfig.getString("groupId"))
      .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest") //"latest"
      .withClientId("COI-" + Instant.now().toString) //To help track this container instance
    // related messages in Kafka logs
  }

  /**
   * Constructs ProducerSettings object
   *
   * @param actorSystem - Actor System
   * @return Producer Settings
   */
  private[this] def getProducerSettings(actorSystem: ActorSystem): ProducerSettings[String, String]
  = {
    val config = actorSystem.settings.config.getConfig("akka.kafka.producer")

    val bootstrapServers = actorSystem.settings.config.getConfig("kafka.cluster").
      getString("bootstrap-servers")

    ProducerSettings(config, new StringSerializer, new StringSerializer)
      .withBootstrapServers(bootstrapServers)
  }

}
