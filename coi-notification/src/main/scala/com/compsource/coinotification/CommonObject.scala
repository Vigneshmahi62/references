package com.compsource.coinotification

import akka.actor.ActorSystem

import java.net.InetAddress

/**
 * Holds the common objects that required across the service
 */
object CommonObject {
  /**
   * Actor System
   */
  final val system: ActorSystem = ActorSystem("coi-notification")
  /**
   * InetAddress to fetch hostname of the machine
   */
  final val inetAddress: InetAddress = InetAddress.getLocalHost
  /**
   * Hostname to write in structured logging
   */
  final val hostname: String = inetAddress.getHostName
  /**
   * Name of the service
   */
  final val SERVICE_NAME: String = "COI Notification"
}
