package com.compsource.coinotification

import com.compsource.coinotification.CommonObject.system
import com.compsource.coinotification.data.CassandraConnector
import com.compsource.coinotification.service.CoiNotificationService.startConsuming

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

object Main extends App {

 private[this] def shutdownHook(): Unit = {
    println(s"Waiting for Actor System to terminate ...")
    system.terminate()
    Await.result(system.whenTerminated, 30 seconds)

    println(s"Waiting for Cassandra cluster to shutdown ...")
    CassandraConnector.shutdownCluster()

    println(s"Steam processing completed. Good Bye.")
  }

  scala.sys.addShutdownHook(() -> shutdownHook())

  system.registerOnTermination({
    println("Terminating Actor System ...")
  })
  
  startConsuming(args(0), args(1))

}
