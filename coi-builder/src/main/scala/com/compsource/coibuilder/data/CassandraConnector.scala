package com.compsource.coibuilder.data

import akka.actor.ActorSystem
import com.compsource.coibuilder.CommonObject.system
import com.compsource.coibuilder.logger.StructuredLogger.log
import com.datastax.driver.core._
import com.datastax.driver.core.policies._

import scala.util.{Failure, Success, Try}

/**
 * Object to connect cassandra & holds the session across the service
 */
object CassandraConnector {

  /**
   * Model class for Cassandra config
   *
   * @param contact_points          - Sequence of contact points with comma separated
   * @param native_transport_port   - Port to connect Cassandra
   * @param max_local_requests      - Max requests per local
   * @param max_remote_requests     - Max requests per remote
   * @param core_local_connections  - Core connections per local
   * @param max_local_connections   - Max connections per local
   * @param core_remote_connections - Core connections per remote
   * @param max_remote_connections  - Max connections per remote
   * @param keyspace                - Keyspace to use
   */
  private[this] case class CassandraConfig(contact_points: String, native_transport_port: Int,
                                           max_local_requests: Int, max_remote_requests: Int,
                                           core_local_connections: Int, max_local_connections: Int,
                                           core_remote_connections: Int,
                                           max_remote_connections: Int, keyspace: String,
                                           userName: String, password: String)

  /**
   * Method to read config & create CassandraConfig
   *
   * @param actorSystem - Actor System
   * @return - CassandraConfig
   */
  private[this] def getCassandraConfig(actorSystem: ActorSystem): CassandraConfig = {

    val config = actorSystem.settings.config.getConfig("cassandra.cluster")

    val contact_points = config.getString("contact-points")
    val native_transport_port = config.getInt("native-transport-port")

    val max_local_requests = config.getInt("max-local-requests")
    val max_remote_requests = config.getInt("max-remote-requests")

    val core_local_connections = config.getInt("core-local-connections")
    val max_local_connections = config.getInt("max-local-connections")

    val core_remote_connections = config.getInt("core-remote-connections")
    val max_remote_connections = config.getInt("max-remote-connections")

    val keyspace = config.getString("keyspace")

    val userName = System.getenv("C_USERNAME")
    val password = System.getenv("C_PASSWORD")

    CassandraConfig(contact_points, native_transport_port, max_local_requests,
      max_remote_requests, core_local_connections, max_local_connections,
      core_remote_connections, max_remote_connections, keyspace, userName, password)
  }

  /**
   * Cassandra Config
   */
  private[this] val cassandraConfig = try getCassandraConfig(system) catch {
    case e: Exception => log.error("Cassandra configuration not available. Reason: " +
      e.getMessage)
      null
  }

  /**
   * Cluster Instance
   */
  private[this] val cluster = Cluster.builder
    .addContactPoints(cassandraConfig.contact_points.split(',').toSeq: _*) //NOTE: All contact points must be resolvable; if any of them cannot be resolved, this method will fail.
    .withLoadBalancingPolicy(new TokenAwarePolicy(DCAwareRoundRobinPolicy.builder.build))
    .withPoolingOptions(
      new PoolingOptions().setMaxRequestsPerConnection(HostDistance.LOCAL, cassandraConfig.max_local_requests) // default for protocol v3 and above: 1024 for LOCAL hosts
        .setMaxRequestsPerConnection(HostDistance.REMOTE, cassandraConfig.max_remote_requests) // default for protocol v3 and above: 256 for REMOTE hosts
        .setConnectionsPerHost(HostDistance.LOCAL, cassandraConfig.core_local_connections, cassandraConfig.max_local_connections) // default for protocol v3 and above: LOCAL hosts: core = max = 1
        .setConnectionsPerHost(HostDistance.REMOTE, cassandraConfig.core_remote_connections, cassandraConfig.max_remote_connections) // default for protocol v3 and above: REMOTE hosts: core = max = 1
    ).withCredentials(cassandraConfig.userName, cassandraConfig.password)
    .withCompression(ProtocolOptions.Compression.LZ4)
    .withProtocolVersion(ProtocolVersion.V4)
    .withPort(cassandraConfig.native_transport_port)
    .build

  /**
   * Cassandra Session
   */
  private[data] implicit val session: Session = cluster.connect(cassandraConfig.keyspace)

  /**
   * Method to execute query and returns the ResultSet
   *
   * @param boundStatement - BoundStatement
   * @param correlationId  - Correlation Id
   * @return - ResultSet or null
   */
  private[data] def executeQuery(boundStatement: BoundStatement, correlationId: String): ResultSet = Try {
    session.execute(boundStatement)
  }
  match {
    case Failure(reason) =>
      log.error("database not accessible", status = "failure", technical_details =
        reason.getMessage, correlation_id = correlationId)
      System.exit(1)
      null

    case Success(resultSet) => resultSet
  }

  /**
   * Method to shutdown the cluster
   */
  def shutdownCluster(): Unit = {
    log.info("Closing Cassandra cluster. This will closes all connections from all sessions and " +
      "reclaims all resources used by this Cluster instance.")
    cluster.close()
    log.info("Cluster successfully closed.")

  }
}
