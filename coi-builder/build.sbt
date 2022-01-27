val AkkaVersion = "2.6.14"
val JacksonVersion = "2.10.5"

val coreDependencies = Seq(
  "com.typesafe.akka" %% "akka-stream-kafka" % "2.0.7",
  "com.typesafe.akka" %% "akka-stream" % AkkaVersion,
  "com.fasterxml.jackson.core" % "jackson-databind" % JacksonVersion,
  "ch.qos.logback" % "logback-classic" % "1.2.3"
)

name := "coi-builder"

version := "0.1"

scalaVersion := "2.13.5"

libraryDependencies ++= coreDependencies

libraryDependencies += "org.apache.kafka" % "kafka-clients" % "2.4.1"

libraryDependencies += "org.scala-lang.modules" %% "scala-collection-compat" % "2.1.6"

libraryDependencies += "org.scala-lang" % "scala-library" % "2.13.5"

libraryDependencies += "com.datastax.cassandra" % "cassandra-driver-core" % "3.6.0"

libraryDependencies += "com.typesafe.play" %% "play-json" % "2.9.2"

libraryDependencies += "net.liftweb" %% "lift-json" % "3.4.3"

libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.8" % Test

libraryDependencies += "net.logstash.logback" % "logstash-logback-encoder" % "6.6"

assembly / assemblyMergeStrategy := {
  case PathList(ps@_*)
    if ps.last contains "version" => MergeStrategy.first
  case "module-info.class" => MergeStrategy.discard
  case x => val oldStrategy = (assembly / assemblyMergeStrategy).value
    oldStrategy(x)
}