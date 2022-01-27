package com.compsource.coibuilder.utils

import com.compsource.coibuilder.data.model.Model.COIRequested
import com.compsource.coibuilder.logger.StructuredLogger.log
import com.datastax.driver.core.utils.UUIDs
import com.fasterxml.jackson.databind.ObjectMapper

import scala.util.matching.Regex

/**
 * Holds the validation rules & parsing logic
 */
object ValidationUtils {
  /**
   * Checks the arg value is null or Empty
   *
   * @param value - Value to be checked
   * @return true if it is null
   */
  def isNullOrEmpty(value: Any): Boolean = {
    value == null || value.toString.equals("null") || value.toString.isBlank
  }

  /**
   * Checks the arg value is null
   *
   * @param value - Value to be checked
   * @return true if it is null
   */
  def isNull(value: Any): Boolean = {
    value == null || value.toString.equals("null")
  }

  /**
   * Method to validate coiRequestId using Regex
   *
   * @param coiRequestId - Correlation Id
   * @return
   */
  def isValidCoiId(coiRequestId: String): Boolean = {
    var isValid = false
    val regex = new Regex("^[{]?[0-9a-fA-F]{8}-([0-9a-fA-F]{4}-){3}[0-9a-fA-F]{12}[}]?$")
    if (regex.matches(coiRequestId) && !"00000000-0000-0000-0000-000000000000".equals(coiRequestId)) {
      isValid = true
    }
    isValid
  }

  /**
   * This method validates the incoming json fields as per defined business rules
   *
   * @param coiRequest - Constructed COIRequested object
   * @return - True if validation success
   */
  def isValidRequest(coiRequest: COIRequested, json: String): Boolean = {
    var isValid = false

    // Check if coiRequestId is valid
    if (isValidCoiId(coiRequest.coiRequestId)) {
      log.info("event received", coiRequest.coiRequestId,
        "success", null)

      // Check if accountId, recipientId and isNewRecipient is not null
      if (!isNullOrEmpty(coiRequest.accountId) && coiRequest.accountId > 0 && !isNullOrEmpty
      (coiRequest.recipientId) && !isNullOrEmpty(coiRequest.isNewRecipient)) {

        // If isNewRecipient is true, recipientName & recipientEmail should not be null
        if (coiRequest.isNewRecipient && !isNullOrEmpty(coiRequest.recipientName)
          && !isNullOrEmpty(coiRequest.recipientEmail)) {
          isValid = true
        }
        // If isNewRecipient is false, recipientName & recipientEmail should be null
        else if (!coiRequest.isNewRecipient && isNull(coiRequest.recipientName)
          && isNull(coiRequest.recipientEmail)) {
          isValid = true
        } else {
          isValid = false
          log.error(
            "invalid content",
            status = "failure",
            technical_details = "event = " + json + ", " +
              "reason = Failed at Recipient details Validation. If isNewRecipient is true, " +
              "recipientName & recipientEmail should not be null. " +
              "Else, recipientName & recipientEmail should be null",
            correlation_id = coiRequest.coiRequestId)
          coiRequest.reason = "Failed at Recipient details Validation. If isNewRecipient is true, " +
            "recipientName & recipientEmail should not be null. Else, recipientName & recipientEmail " +
            "should be null"
        }

      } else {
        log.error(
          "invalid content",
          status = "failure",
          technical_details = "event = " + json + ", " +
            "reason = Failed at Not NULL field check (coi_request_id, isNewRecipient, recipientId, accountId)",
          correlation_id = coiRequest.coiRequestId)
        coiRequest.reason = "Failed at Not NULL field check (coi_request_id, isNewRecipient, recipientId, accountId)"
      }
    } else {
      isValid = false
      // Since coiRequestId is not valid, System will generate time based UUID to avoid
      // duplicates if the same event received again.
      val systemCoiId = UUIDs.timeBased().toString

      log.info("event received", systemCoiId, "success", null)
      coiRequest.reason = "coiRequestId " + coiRequest.coiRequestId + " is invalid"
      log.error("invalid content", status = "failure", technical_details = coiRequest.reason,
        correlation_id = systemCoiId)
      coiRequest.coiRequestId = systemCoiId
    }
    log.debug(coiRequest.internalMessageId + "JSON Validation status :: " + isValid)
    isValid
  }

  /**
   * Constructs COIRequested object from received message
   *
   * @param json - coiRequested json message
   * @return - COIRequested object
   */
  def loadCOIRequestedObjFromJson(json: String): COIRequested = {
    var coiRequested: COIRequested = null
    try {
      val mapper = new ObjectMapper
      val root = mapper.readTree(json)
      val coiRequestGuid = root.at("/coiRequestId").asText
      val accountId = root.at("/accountId").asInt
      val isNewRecipient = root.at("/isNewRecipient").asBoolean
      val recipientId = root.at("/recipientId").asText
      val recipientName = root.at("/recipientName").asText
      val recipientEmail = root.at("/recipientEmail").asText
      coiRequested = COIRequested(coiRequestGuid, accountId, isNewRecipient, recipientId
        , recipientName, recipientEmail)
      log.debug(coiRequestGuid + " JSON Unmarshalling success")
    } catch {
      case exception: Exception => exception.printStackTrace()
    }
    coiRequested
  }
}
