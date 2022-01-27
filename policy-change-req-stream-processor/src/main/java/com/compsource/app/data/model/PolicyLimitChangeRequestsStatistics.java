package com.compsource.app.data.model;

import com.compsource.app.data.serde.json.instant.InstantDeserializer;
import com.compsource.app.data.serde.json.instant.InstantSerializer;
import com.compsource.app.data.serde.json.localdate.LocalDateDeserializer;
import com.compsource.app.data.serde.json.localdate.LocalDateSerializer;
import com.compsource.app.logging.LogManager;
import com.datastax.driver.core.LocalDate;
import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.Table;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.time.Instant;

/**
 * Model class for the cassandra table policy_limit_change_requests_statistics
 */
@Table(name = "policy_limit_change_requests_statistics")
public class PolicyLimitChangeRequestsStatistics {
    private static LogManager logger = new LogManager(PolicyLimitChangeRequestsStatistics.class);

    @JsonSerialize(using = LocalDateSerializer.class)
    @JsonDeserialize(using = LocalDateDeserializer.class)
    @Column(name = "date_requested")
    private LocalDate dateRequested;

    @Column(name = "hour_requested")
    private Byte hourRequested;

    @Column(name = "total_coverage_increase_amount")
    private Long totalCoverageIncreaseAmount;

    @Column(name = "total_coverage_increase_requests_count")
    private Integer totalCoverageIncreaseRequestsCount;

    @JsonSerialize(using = InstantSerializer.class)
    @JsonDeserialize(using = InstantDeserializer.class)
    @Column(name = "statistics_added_at")
    private Instant statisticsAddedAt;

    public PolicyLimitChangeRequestsStatistics() {
        this.totalCoverageIncreaseAmount = 0L;
        this.totalCoverageIncreaseRequestsCount = 0;
    }

    /**
     * Updates the Policy Coverage request count and Policy Coverage Increase Amount
     *
     * @param policyLimitChangeRequest - PolicyLimitChangeRequest object
     * @return - Updated PolicyLimitChangeRequestsStatistics object
     */
    public PolicyLimitChangeRequestsStatistics add(PolicyLimitChangeRequest policyLimitChangeRequest) {
        //Incrementing the Policy limit change request count
        this.totalCoverageIncreaseRequestsCount++;

        //Adding the Increased policy limit coverage to the existing increasedPolicyLimitCoverage
        this.totalCoverageIncreaseAmount +=
                (policyLimitChangeRequest.getNewlyRequestedPolicyCoverageLimit() -
                        policyLimitChangeRequest.getExistingPolicyCoverageLimit());

        logger.info("event processed", policyLimitChangeRequest.getCoiRequestId(), null);

        return this;
    }


    public Long getTotalCoverageIncreaseAmount() {
        return totalCoverageIncreaseAmount;
    }

    public void setTotalCoverageIncreaseAmount(Long totalCoverageIncreaseAmount) {
        this.totalCoverageIncreaseAmount = totalCoverageIncreaseAmount;
    }

    public Integer getTotalCoverageIncreaseRequestsCount() {
        return totalCoverageIncreaseRequestsCount;
    }

    public void setTotalCoverageIncreaseRequestsCount(Integer totalCoverageIncreaseRequestsCount) {
        this.totalCoverageIncreaseRequestsCount = totalCoverageIncreaseRequestsCount;
    }

    public LocalDate getDateRequested() {
        return dateRequested;
    }

    public void setDateRequested(LocalDate dateRequested) {
        this.dateRequested = dateRequested;
    }

    public Byte getHourRequested() {
        return hourRequested;
    }

    public void setHourRequested(Byte hourRequested) {
        this.hourRequested = hourRequested;
    }

    public Instant getStatisticsAddedAt() {
        return statisticsAddedAt;
    }

    public void setStatisticsAddedAt(Instant statisticsAddedAt) {
        this.statisticsAddedAt = statisticsAddedAt;
    }

    @Override
    public String toString() {
        return "PolicyLimitChangeRequestsStatistics{" +
                "dateRequested=" + dateRequested +
                ", hourRequested=" + hourRequested +
                ", totalCoverageIncreaseAmount=" + totalCoverageIncreaseAmount +
                ", totalCoverageIncreaseRequestsCount=" + totalCoverageIncreaseRequestsCount +
                ", statisticsAddedAt=" + statisticsAddedAt +
                '}';
    }

    /**
     * Converts the PolicyLimitChangeRequestsStatistics object to a JSON String
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
