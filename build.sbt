name := "wikipedia"

version := "1.0"

scalaVersion := "2.10.5"

libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-core" % "1.5.1",
  "org.apache.spark" %% "spark-sql" % "1.5.1"
)

libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.4" % Test
libraryDependencies += "junit" % "junit" % "4.8.1" % "test"
libraryDependencies += "com.esri.geometry" % "esri-geometry-api" % "1.2.1"
libraryDependencies += "com.databricks" % "spark-csv_2.10" % "1.3.0"
libraryDependencies += "com.typesafe.play" %% "play-json" % "2.3.10"
libraryDependencies += "com.mandubian" %% "play-json-zipper" % "1.2"


// The Typesafe repository
resolvers += "Typesafe repository" at "https://repo.typesafe.com/typesafe/releases/"
resolvers += "mandubian maven bintray" at "http://dl.bintray.com/mandubian/maven"

