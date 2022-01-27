package com.compsource.app.data.connector;

import com.compsource.app.logging.LogManager;
import com.compsource.app.utils.ConfigUtil;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.PoolingOptions;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.policies.ConstantReconnectionPolicy;
import com.datastax.driver.extras.codecs.jdk8.InstantCodec;

import java.util.Properties;

/**
 * This class handles Cassandra connection
 */
public class CassandraConnector {

    private static LogManager logger = new LogManager(CassandraConnector.class);
    private static CassandraConnector cassandra = null;
    private Session session = null;

    private CassandraConnector() {
        init();
    }

    /**
     * Used to get the single instance of this class
     */
    public static CassandraConnector getInstance() {
        if (cassandra == null)
            synchronized (CassandraConnector.class) {
                cassandra = new CassandraConnector();
            }
        return cassandra;
    }

    public Session getSession() {
        return session;
    }

    /**
     * This method is used read relevant properties from conf and invoke cluster
     * object initialization.
     */
    private void init() {
        Properties properties = ConfigUtil.loadProperty();
        try {
            String[] contactPoints = properties.getProperty("cassandra.contact-points").split(",");
            int port = Integer.parseInt(properties.getProperty("cassandra.port"));
            String username = System.getenv("C_USERNAME");
            String password = System.getenv("C_PASSWORD");
            String keyspace = properties.getProperty("cassandra.keyspace");

            initCluster(contactPoints, port, username, password, keyspace);
        } catch (Exception e) {
            logger.error("Error initializing cassandra cluster using properties");
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Connects to cluster with defined policies and initialize session
     */
    private void initCluster(String[] servers, int port, String username, String password,
                             String keyspace) {
        Cluster cluster = Cluster.builder().addContactPoints(servers)
                .withPort(port)
                .withPoolingOptions(new PoolingOptions())
                .withReconnectionPolicy(new ConstantReconnectionPolicy(100L))

                .withCredentials(username, password).withoutJMXReporting().build();
        cluster.getConfiguration().getCodecRegistry().register(InstantCodec.instance);
        session = cluster.connect(keyspace);
        logger.info("CONNECTED SUCCESSFULLY TO CASSANDRA CLUSTER: " + cluster.getClusterName());
    }
}