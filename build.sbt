organization := "me.lessis"

name := "airtraffic"

version := "0.1.0-SNAPSHOT"

libraryDependencies += "com.github.jnr" % "jnr-unixsocket" % "0.3"

crossScalaVersions := Seq("2.10.4", "2.11.0")

scalaVersion := crossScalaVersions.value.head
