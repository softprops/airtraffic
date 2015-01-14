organization := "me.lessis"

name := "airtraffic"

version := "0.1.0-SNAPSHOT"

libraryDependencies += "com.github.jnr" % "jnr-unixsocket" % "0.3"

crossScalaVersions := Seq("2.10.4", "2.11.4")

scalaVersion := crossScalaVersions.value.head

initialCommands := """import airtraffic._, scala.concurrent.ExecutionContext.Implicits.global; val ctl = Control(new java.io.File("src/test/resources/conf/haproxy.stat"))"""
