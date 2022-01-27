package com.compsource.app.data.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

/**
 * Model class to parse the Policy Limit Change Request event from kafka topic
 */
public class PolicyLimitChangeRequest {

    @JsonProperty(value = "coiRequestId")
    private String coiRequestId;

    @JsonProperty(value = "accountId")
    private Integer accountId;

    @JsonProperty(value = "existingPolicyCoverageLimit")
    private Integer existingPolicyCoverageLimit;

    @JsonProperty(value = "isChangeRequested")
    private Boolean isChangeRequested;

    @JsonProperty(value = "newlyRequestedPolicyCoverageLimit")
    private Integer newlyRequestedPolicyCoverageLimit;

    @JsonProperty(value = "isNewRecipient")
    private Boolean isNewRecipient;

    @JsonProperty(value = "recipientId")
    private String recipientId;

    @JsonProperty(value = "recipientName")
    private String recipientName;

    @JsonProperty(value = "recipientEmail")
    private String recipientEmail;

    @JsonProperty(value = "dateRequested")
    private String dateRequested;

    public String getCoiRequestId() {
        return coiRequestId;
    }

    public void setCoiRequestId(String coiRequestId) {
        this.coiRequestId = coiRequestId;
    }

    public Integer getAccountId() {
        return accountId;
    }

    public void setAccountId(Integer accountId) {
        this.accountId = accountId;
    }


    public Integer getExistingPolicyCoverageLimit() {
        return existingPolicyCoverageLimit;
    }

    public void setExistingPolicyCoverageLimit(Integer existingPolicyCoverageLimit) {
        this.existingPolicyCoverageLimit = existingPolicyCoverageLimit;
    }

    public Boolean getIsChangeRequested() {
        return isChangeRequested;
    }

    public void setIsChangeRequested(Boolean changeRequested) {
        isChangeRequested = changeRequested;
    }

    public Integer getNewlyRequestedPolicyCoverageLimit() {
        return newlyRequestedPolicyCoverageLimit;
    }

    public void setNewlyRequestedPolicyCoverageLimit(Integer newlyRequestedPolicyCoverageLimit) {
        this.newlyRequestedPolicyCoverageLimit = newlyRequestedPolicyCoverageLimit;
    }

    public Boolean getIsNewRecipient() {
        return isNewRecipient;
    }

    public void setIsNewRecipient(Boolean newRecipient) {
        isNewRecipient = newRecipient;
    }

    public String getRecipientId() {
        return recipientId;
    }

    public void setRecipientId(String recipientId) {
        this.recipientId = recipientId;
    }


    public String getRecipientName() {
        return recipientName;
    }

    public void setRecipientName(String recipientName) {
        this.recipientName = recipientName;
    }

    public String getRecipientEmail() {
        return recipientEmail;
    }

    public void setRecipientEmail(String recipientEmail) {
        this.recipientEmail = recipientEmail;
    }

    public String getDateRequested() {
        return dateRequested;
    }

    public void setDateRequested(String dateRequested) {
        this.dateRequested = dateRequested;
    }

    @Override
    public String toString() {
        return "PolicyLimitChangeRequest{" +
                "coiRequestId='" + coiRequestId + '\'' +
                ", accountId=" + accountId +
                ", existingPolicyCoverageLimit=" + existingPolicyCoverageLimit +
                ", isChangeRequested=" + isChangeRequested +
                ", newlyRequestedPolicyCoverageLimit=" + newlyRequestedPolicyCoverageLimit +
                ", isNewRecipient=" + isNewRecipient +
                ", recipientId='" + recipientId + '\'' +
                ", recipientName='" + recipientName + '\'' +
                ", recipientEmail='" + recipientEmail + '\'' +
                ", dateRequested='" + dateRequested + '\'' +
                '}';
    }

    /**
     * Converts the policyLimitChangeRequest object to a JSON String
     *
     * @return - JSON String
     */
    public String toJSON() {
        ObjectWriter objectWriter = new ObjectMapper().writer();
        try {
            return objectWriter.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            return this.toString();
        }
    }
}
