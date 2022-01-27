package com.compsource.coibuilder.utils

import akka.actor.ActorSystem
import com.compsource.coibuilder.data.model.Model.COIRequested
import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.io.File

/**
 * This class contains tests for Utils object.
 */
class ValidationUtilsTest extends AnyFlatSpec with Matchers {


  val myConfig: Config = ConfigFactory.parseFile(new File("src/test/resources/application_test.conf"))

  private val actorSystem: ActorSystem = ActorSystem("Consumer-Test", myConfig)
  private val staticData: Config = actorSystem.settings.config.getConfig("static_data")
  private val internal_correlation_id = staticData.getString("internal_correlation_id")

  "Utils isValid method" should "return true since COIRequested has " +
    "valid fields (is_new_recipient is true)" in {

    val coiRequested: COIRequested = loadCoiRequested("event_with_new_recipient")

    ValidationUtils.isValidRequest(coiRequested, null) should be(true)
  }

  it should "return true since COIRequested has valid fields (is_new_recipient is false)" in {

    val coiRequested: COIRequested = loadCoiRequested("event_without_new_recipient")

    ValidationUtils.isValidRequest(coiRequested, null) should be(true)
  }

  it should "return false since recipient_id has null value" in {

    val coiRequested: COIRequested = loadCoiRequested("event_without_recipient_id")

    ValidationUtils.isValidRequest(coiRequested, null) should be(false)
  }

  it should "return false since account_id has negative value" in {

    val coiRequested: COIRequested = loadCoiRequested("event_with_negative_account_id")

    ValidationUtils.isValidRequest(coiRequested, null) should be(false)
  }

  it should "return false since coi_request_id has null value" in {

    val coiRequested: COIRequested = loadCoiRequested("event_without_coi_request_id")

    ValidationUtils.isValidRequest(coiRequested, null) should be(false)
  }

  private def loadCoiRequested(config: String): COIRequested = {
    val coiRequestedData: Config = actorSystem.settings.config.getConfig(config)

    COIRequested(coiRequestedData.getString("coi_request_id"),
      coiRequestedData.getInt("account_id"),
      coiRequestedData.getBoolean("is_new_recipient"),
      coiRequestedData.getString("recipient_id"),
      coiRequestedData.getString("recipient_name"),
      coiRequestedData.getString("recipient_email"))
  }
}
