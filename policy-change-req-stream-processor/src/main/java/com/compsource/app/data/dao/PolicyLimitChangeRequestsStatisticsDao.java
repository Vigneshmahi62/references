package com.compsource.app.data.dao;

import com.compsource.app.data.connector.CassandraConnector;
import com.compsource.app.data.model.PolicyLimitChangeRequestsStatistics;
import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.Session;
import com.datastax.driver.mapping.Mapper;
import com.datastax.driver.mapping.MappingManager;

import java.time.Instant;

/**
 * This class handles the read/write operations on the cassandra table
 * policy_limit_change_requests_statistics
 */
public class PolicyLimitChangeRequestsStatisticsDao {

    private static CassandraConnector cassandra = CassandraConnector.getInstance();
    private static Session session = cassandra.getSession();
    private static MappingManager mappingMgr = new MappingManager(session);
    private static Mapper<PolicyLimitChangeRequestsStatistics> mapper =
            mappingMgr.mapper(PolicyLimitChangeRequestsStatistics.class);

    /**
     * Writes the PolicyLimitChangeRequestsStatistics object to the cassandra table
     *
     * @param policyLimitChangeRequestsStats - PolicyLimitChangeRequestsStatistics object
     */
    public void save(PolicyLimitChangeRequestsStatistics policyLimitChangeRequestsStats) {
        policyLimitChangeRequestsStats.setStatisticsAddedAt(Instant.now());
        mapper.save(policyLimitChangeRequestsStats, Mapper.Option.saveNullFields(false),
                Mapper.Option.consistencyLevel(ConsistencyLevel.QUORUM));
    }


}
