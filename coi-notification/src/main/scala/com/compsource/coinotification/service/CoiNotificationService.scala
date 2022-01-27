package com.compsource.coinotification.service

import akka.actor.ActorSystem
import akka.kafka.ConsumerMessage.CommittableOffset
import akka.kafka.scaladsl.{Committer, Consumer}
import akka.kafka.{CommitterSettings, ConsumerSettings, ProducerSettings, Subscriptions}
import akka.stream.Materializer
import akka.stream.scaladsl.Keep
import com.compsource.coinotification.CommonObject.{SERVICE_NAME, system}
import com.compsource.coinotification.data.CommonDataService.{insertRecordToCoiNotification, insertRecordToProcessedEvents, insertRecordToPublishedEvents, selectFromCertificatesOfInsurance, selectFromProcessedEventsByCorrId, selectFromPublishedEventsByCorrId}
import com.compsource.coinotification.logger.StructuredLogger.log
import com.datastax.driver.core.ResultSet
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.{Callback, Producer, ProducerRecord, RecordMetadata}
import org.apache.kafka.common.serialization.{StringDeserializer, StringSerializer}
import play.api.libs.json.{Json, OFormat}

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

/**
 * Object that holds logic to process notifications read from upstream topic in step by step
 */
object CoiNotificationService {

  /**
   * Format of the message published to the SendMail topic
   *
   * @param notificationId - Certificate of Insurance Notification Id
   */
  case class CoiNotificationMessage(notificationId: String)


  object CoiNotificationMessage {
    implicit val fmtJson: OFormat[CoiNotificationMessage] = Json.format[CoiNotificationMessage]
  }

  /**
   * Checks if the request has been processed already in processed_events_by_correlation_id table
   *
   * @param coiId - COI Id
   * @return true if not processed else false
   */
  private[this] def isRequestProcessed(coiId: String): Boolean = {
    val processedEventResult: ResultSet = selectFromProcessedEventsByCorrId(coiId,
      SERVICE_NAME)
    val processedEvents = processedEventResult.all()

    !processedEvents.isEmpty
  }

  /**
   * Checks if the message published already
   *
   * @param coiId     - COI Id
   * @param topicName - Downstream topic name
   * @return
   */
  private[this] def isMessagePublished(coiId: String, topicName: String): Boolean = {
    val publishedMsgResult = selectFromPublishedEventsByCorrId(coiId, topicName)
    val publishedMsg = publishedMsgResult.all()

    !publishedMsg.isEmpty
  }

  /**
   * Publish message to downstream topic
   *
   * @param producer          - Producer
   * @param producerTopicName - Producer Topic Name
   * @param key               - Message Key
   * @param value             - Message Value
   */
  private[this] def publishMessageToSendEmailTopic(producer: Producer[String, String],
                                                   producerTopicName: String, key: String,
                                                   value: String): Unit = {
    producer.send(new ProducerRecord(producerTopicName, key, value), ResultClass())
  }

  /**
   * Method to call publish message to Downstream topic & insert records to published events
   *
   * @param producer          - Producer
   * @param coiId             - Correlation Id
   * @param producerTopicName - Producer Topic Name
   * @return
   */
  private[this] def publishMessageAndLogInDatabase(producer: Producer[String, String],
                                                   coiId: String, producerTopicName: String): Boolean = {
    //Check if the message is already published or not
    if (!isMessagePublished(coiId, producerTopicName)) {
      val coiNotificationMessage = CoiNotificationMessage.fmtJson
        .writes(CoiNotificationMessage(coiId)).toString()

      publishMessageToSendEmailTopic(producer, producerTopicName, coiId, coiNotificationMessage)
      insertRecordToPublishedEvents(coiId, producerTopicName, SERVICE_NAME)
    }

    true
  }

  /**
   * Method to construct required values to persist COI Notification record
   *
   * @param coiId - Correlation Id
   * @return
   */
  private[this] def persistCoiNotification(coiId: String): Boolean = {
    val resultSet = selectFromCertificatesOfInsurance(coiId)
    val row = resultSet.one()
    if (row == null) {
      false
    } else {
      val to = row.getString("recipient_email")
      val policy_holder_name = row.getString("policy_holder_name")
      val recipient_name = row.getString("recipient_name")
      val from = "coi_mailer@compsourcemutual.com"
      val subject = "COI requested for " + policy_holder_name
      val body = recipient_name + " please view this document: \nhttps://documents" +
        ".compsourcemutual.com/coi/" + coiId
      if (insertRecordToCoiNotification(coiId, body, from, subject, to))
        true
      else false
    }
  }

  /**
   * Starts processing COINotification message upon successful validation
   *
   * @param coiId             - Correlation Id
   * @param producerTopicName - Producer Topic Name
   * @param producer          - Producer
   */
  private[this] def processNotification(coiId: String, producerTopicName: String,
                                        producer: Producer[String, String]): Unit = {
    if (!isRequestProcessed(coiId)) {
      val success = persistCoiNotification(coiId)
      if (success) {
        // Write to processed events
        if (!insertRecordToProcessedEvents(coiId, SERVICE_NAME)) {
          log.error(coiId + " Unable to insert record to the Processed events table")
          System.exit(1)
        }
        //Check and Write to Published events
        if (publishMessageAndLogInDatabase(producer, coiId, producerTopicName)) {
          log.info("event processed", coiId,
            "success", null)
        }
      }
    } else {
      log.warn("duplicate event", coiId, "success", null)
      //If already processed, Check and Write to Published events
      if (publishMessageAndLogInDatabase(producer, coiId, producerTopicName)) {
        log.info("event processed", coiId, "success", null)
      }
    }
  }

  /**
   * Consumes event from COICreated topic & initiates the process
   *
   * @param consumerTopicName - Upstream consumer topic name
   * @param producerTopicName - Downstream Producer topic name
   */
  def startConsuming(consumerTopicName: String, producerTopicName: String): Unit = {
    implicit val ec: ExecutionContext = system.dispatcher
    implicit val m: Materializer = Materializer.createMaterializer(system)
    val consumerSettings = getConsumerSettings(system)

    val committerSettings = CommitterSettings(system.settings.config.
      getConfig("akka.kafka.committer"))

    val producerSettings = getProducerSettings(system)
    val producer = producerSettings.createKafkaProducer()
    Consumer.committableSource(consumerSettings, Subscriptions.topics(consumerTopicName))
      .mapAsync(1)(msg => {
        Future {
          val offSet: CommittableOffset = msg.committableOffset

          Try {
            Json.parse(msg.record.value)
          } match {
            case Success(json) =>
              log.debug("JSON Parsing Success! " + json.toString())
              val mapper = new ObjectMapper
              val root = mapper.readTree(msg.record.value)
              val coiId = root.at("/coiId").asText
              log.info("event received", coiId, "success", null)
              processNotification(coiId, producerTopicName, producer)
            case Failure(exception) =>
              exception.printStackTrace()
              log.error("malformed json", null, "failure", msg.record.value)
          }
          offSet
        }
      }).toMat(Committer.sink(committerSettings))(Keep.both).run()
  }

  /**
   * Constructs ConsumerSettings object
   *
   * @param actorSystem - Actor System
   * @param groupId     - Group Id of consumer
   * @return Consumer Settings
   */
  private def getConsumerSettings(actorSystem: ActorSystem, groupId: String = "coi_notification"):
  ConsumerSettings[String, String] = {

    val bootstrapServers = actorSystem.settings.config.getConfig("kafka.cluster").
      getString("bootstrap-servers")

    val consumerConfig = actorSystem.settings.config.getConfig("akka.kafka.consumer")

    ConsumerSettings(consumerConfig, new StringDeserializer, new StringDeserializer)
      .withBootstrapServers(bootstrapServers)
      .withGroupId(groupId)
      .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest") //"latest"
      .withClientId("COIN-" + Instant.now().toString) //To help track this container instance
    // related messages in Kafka logs
  }

  /**
   * Constructs ProducerSettings object
   *
   * @param actorSystem - Actor System
   * @return Producer Settings
   */
  private def getProducerSettings(actorSystem: ActorSystem): ProducerSettings[String, String]
  = {
    val config = actorSystem.settings.config.getConfig("akka.kafka.producer")

    val bootstrapServers = actorSystem.settings.config.getConfig("kafka.cluster").
      getString("bootstrap-servers")

    ProducerSettings(config, new StringSerializer, new StringSerializer)
      .withBootstrapServers(bootstrapServers)
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
}
