package com.compsource.coibuilder

import com.compsource.coibuilder.data.CassandraConnector
import com.compsource.coibuilder.CommonObject.system
import com.compsource.coibuilder.service.CoiBuilderService.startConsuming

import scala.concurrent.duration.DurationInt
import scala.concurrent.Await
import scala.language.postfixOps

object Main extends App {

  // Register a shutdown hook to be run when the VM exits.
  def shutdownHook(): Unit = {
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
