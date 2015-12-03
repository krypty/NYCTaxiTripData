name := "wikipedia"

version := "1.0"

scalaVersion := "2.10.5"

libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-core" % "1.5.1",
  "org.apache.spark" %% "spark-sql" % "1.5.1"
)

libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.4" % Test
libraryDependencies += "junit" % "junit" % "4.8.1" % "test"
libraryDependencies += "com.databricks" % "spark-csv_2.10" % "1.3.0"
