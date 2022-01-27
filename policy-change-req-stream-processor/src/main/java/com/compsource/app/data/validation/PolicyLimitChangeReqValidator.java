package com.compsource.app.data.validation;

import com.compsource.app.data.model.PolicyLimitChangeRequest;
import com.compsource.app.logging.LogManager;
import com.compsource.app.utils.ConfigUtil;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * This class handles the validation process for the Policy Limit Change Request based on the
 * required Business rules
 */
public class PolicyLimitChangeReqValidator {
    private static LogManager logger = new LogManager(PolicyLimitChangeReqValidator.class);
    private static Properties properties = ConfigUtil.loadProperty();
    private static List<String> nonNullableFields =
            Arrays.asList(properties.getProperty("fields.non-nullable.policy-limit-change-request")
                    .split(","));

    /**
     * Validates the incoming PolicyLimitChangeRequest object. Checks whether the request conforms
     * to all the Business rules
     *
     * @param policyLimitChangeRequest - PolicyLimitChangeRequest object
     * @return - true - valid, false - invalid
     */
    public static boolean validate(PolicyLimitChangeRequest policyLimitChangeRequest) {
        if (validateCoiRequestId(policyLimitChangeRequest)) {
            logger.info("event received", policyLimitChangeRequest.getCoiRequestId(), null);

            return validateNonNullableFields(policyLimitChangeRequest)
                    && validateExistingPolicyLimit(policyLimitChangeRequest)
                    && validateAccountId(policyLimitChangeRequest)
                    && validateNewPolicyLimit(policyLimitChangeRequest)
                    && validateRecipientDetails(policyLimitChangeRequest);
        } else {
            String correlationId = UUID.randomUUID().toString();
            logger.info("event received", correlationId, null);
            writeErrorLog(policyLimitChangeRequest, "The incoming coiRequestedId is either " +
                            "Invalid (or) Empty. Hence using auto-generated Correlation Id.",
                    correlationId);
            return false;
        }

    }

    /**
     * The coiRequestId should be a valid and non-empty Guid
     *
     * @param policyLimitChangeRequest - PolicyLimitChangeRequest object
     * @return - true - valid, false - invalid
     */
    private static boolean validateCoiRequestId(PolicyLimitChangeRequest policyLimitChangeRequest) {
        String coiRequestId = policyLimitChangeRequest.getCoiRequestId();
        if (coiRequestId != null) {
            String guidRegex = "^[{]?[0-9a-fA-F]{8}-([0-9a-fA-F]{4}-){3}[0-9a-fA-F]{12}[}]?$";
            String emptyGuidRegex = "^0{8}-(0{4}-){3}0{12}$";
            return Pattern.matches(guidRegex, coiRequestId) && !Pattern.matches(emptyGuidRegex,
                    coiRequestId);
        }
        return false;
    }

    /**
     * Rule: 1) If isNewRecipient is true, then recipientName and recipientEmail must be a non-null
     * string value. 2) If isNewRecipient is false, then recipientName and recipientEmail must be
     * null
     *
     * @param policyLimitChangeRequest - PolicyLimitChangeRequest object
     * @return - true - valid, false - invalid
     */
    private static boolean validateRecipientDetails(PolicyLimitChangeRequest policyLimitChangeRequest) {
        boolean isValidNewRecipient = policyLimitChangeRequest.getIsNewRecipient()
                && !checkIfNull(policyLimitChangeRequest, "recipientName")
                && !checkIfNull(policyLimitChangeRequest, "recipientEmail");

        boolean isValidOldRecipient = !policyLimitChangeRequest.getIsNewRecipient()
                && checkIfNull(policyLimitChangeRequest, "recipientName")
                && checkIfNull(policyLimitChangeRequest, "recipientEmail");

        if (isValidNewRecipient || isValidOldRecipient)
            return true;
        else {
            writeErrorLog(policyLimitChangeRequest,
                    "The Recipient details doesn't conforms to the business rules",
                    policyLimitChangeRequest.getCoiRequestId());
            return false;
        }

    }

    /**
     * Rule: The change requested flag must be true and the Newly Requested Policy limit should
     * be greater than the Existing Policy limit
     *
     * @param policyLimitChangeRequest - PolicyLimitChangeRequest object
     * @return - true - valid, false - invalid
     */
    private static boolean validateNewPolicyLimit(PolicyLimitChangeRequest policyLimitChangeRequest) {

        if (policyLimitChangeRequest.getIsChangeRequested() &&
                (policyLimitChangeRequest.getNewlyRequestedPolicyCoverageLimit() >
                        policyLimitChangeRequest.getExistingPolicyCoverageLimit()))
            return true;
        else {
            writeErrorLog(policyLimitChangeRequest,
                    "New Policy Limit doesn't conforms to the business rules",
                    policyLimitChangeRequest.getCoiRequestId());
            return false;
        }
    }

    /**
     * Rule: Account Id should be a positive Integer
     *
     * @param policyLimitChangeRequest - PolicyLimitChangeRequest object
     * @return - true - valid, false - invalid
     */
    private static boolean validateAccountId(PolicyLimitChangeRequest policyLimitChangeRequest) {
        if (policyLimitChangeRequest.getAccountId() > 0) {
            return true;
        } else {
            writeErrorLog(policyLimitChangeRequest,
                    String.format("Account Id => %s is not a positive Integer",
                            policyLimitChangeRequest.getAccountId()),
                    policyLimitChangeRequest.getCoiRequestId());
            return false;
        }

    }


    /**
     * Rule: The Existing Policy Coverage Limit value should be a Non-zero positive Integer
     *
     * @param policyLimitChangeRequest - PolicyLimitChangeRequest object
     * @return -  true - valid, false - invalid
     */
    private static boolean validateExistingPolicyLimit(PolicyLimitChangeRequest policyLimitChangeRequest) {
        if (policyLimitChangeRequest.getExistingPolicyCoverageLimit() > 0) {
            return true;
        } else {
            writeErrorLog(policyLimitChangeRequest,
                    String.format("Existing Policy Limit => %s is not a Non-zero positive Integer",
                            policyLimitChangeRequest.getExistingPolicyCoverageLimit()),
                    policyLimitChangeRequest.getCoiRequestId());
            return false;
        }
    }


    /**
     * Checks whether all the required (non-nullable) fields contain a non-null value.
     *
     * @param policyLimitChangeRequest - PolicyLimitChangeRequest object
     * @return - true - valid, false - invalid
     */
    private static boolean validateNonNullableFields(PolicyLimitChangeRequest policyLimitChangeRequest) {

        for (String field : nonNullableFields) {
            if (checkIfNull(policyLimitChangeRequest, field)) {
                writeErrorLog(policyLimitChangeRequest, String.format("Field %s is NULL", field),
                        policyLimitChangeRequest.getCoiRequestId());
                return false;
            }
        }

        return true;
    }


    /**
     * Checks whether the incoming field is NULl in the PolicyLimitChangeRequest object
     *
     * @param policyLimitChangeRequest - PolicyLimitChangeRequest object
     * @param field                    - Field name to be checked
     * @return - true - Field value is NULL, false - field value is not NULL
     */
    private static boolean checkIfNull(PolicyLimitChangeRequest policyLimitChangeRequest, String field) {
        try {
            Field nonNullableField = PolicyLimitChangeRequest.class.getDeclaredField(field);
            // Need to change access, if the variable is declared as private in the class
            nonNullableField.setAccessible(true);
            if (nonNullableField.get(policyLimitChangeRequest) == null)
                return true;
        } catch (NoSuchFieldException | IllegalAccessException e) {
            logger.error("Field: " + field + " not found in the class " +
                    "PolicyLimitChangeRequest");
            e.printStackTrace();
        }
        return false;

    }

    /**
     * Writes error log in the required format
     *
     * @param policyLimitChangeRequest - PolicyLimitChangeRequest object
     * @param reason                   - Reason for marking the event as invalid
     * @param correlationId            - a valid Guid if the incoming coiRequestId is invalid
     */
    private static void writeErrorLog(PolicyLimitChangeRequest policyLimitChangeRequest,
                                      String reason, String correlationId) {
        logger.error("invalid content", correlationId,
                String.format("Event: %s, Reason: %s", policyLimitChangeRequest.toJSON(), reason));
    }

}
