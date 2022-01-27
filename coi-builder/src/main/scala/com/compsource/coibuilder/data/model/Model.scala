package com.compsource.coibuilder.data.model

/**
 * Holds the Model case classes similar to Database tables
 */
object Model {

  /**
   * Model case class for coi_requested table i.e similar to entity
   *
   * Pojo of the Json Message consumed from CoiRequested topic
   *
   * @param coiRequestId      - This field should be unique for each message (Not NULL field)
   * @param accountId         - Account Id assigned to policy holder
   * @param isNewRecipient    - Says, it is either new recipient or not
   * @param recipientId       - Recipient Id against the recipient details
   * @param recipientName     - Recipient Name
   * @param recipientEmail    - Recipient Mail ID
   * @param is_valid          - Says, received event is valid or not
   * @param reason            - Says the reason if the received event is invalid
   * @param internalMessageId - Same as coiRequestId. Used for debugging purpose
   */
  case class COIRequested(
                           var coiRequestId: String,
                           accountId: Int,
                           isNewRecipient: Boolean,
                           recipientId: String,
                           var recipientName: String,
                           var recipientEmail: String,
                           var is_valid: Boolean = true,
                           var reason: String = null,
                           var internalMessageId: String = null)

  /**
   * Model class for coi_recipients table
   *
   * @param recipient_id          - Recipient ID
   * @param recipient_name        - Recipient Name
   * @param recipient_email       - Recipient Email
   * @param create_correlation_id - Correlation Id of coi requested event
   */
  case class COIRecipients(
                            recipient_id: String,
                            recipient_name: String,
                            recipient_email: String,
                            create_correlation_id: String)

  /**
   * Model class for policy_holders table
   *
   * @param account_id                     - Account ID
   * @param policy_holder_name             - Policy Holder Name
   * @param existing_policy_coverage_limit - Existing Policy Coverage Limit
   */
  case class PolicyHolders(
                            account_id: Int,
                            policy_holder_name: String,
                            existing_policy_coverage_limit: Long)

  /**
   * Model class for certificates_of_insurance table
   *
   * @param coi_id                         - COI Requested ID
   * @param recipient_name                 - Recipient Name
   * @param recipient_email                - Recipient Email
   * @param policy_holder_name             - Policy Holder Name
   * @param existing_policy_coverage_limit - Existing Policy Coverage Limit
   */
  case class CertificateOfInsurance(
                                     coi_id: String,
                                     recipient_name: String,
                                     recipient_email: String,
                                     policy_holder_name: String,
                                     existing_policy_coverage_limit: Long)

}
