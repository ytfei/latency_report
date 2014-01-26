name := "latency_report"

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache
)

libraryDependencies += "org.yaml" % "snakeyaml" % "1.13"

libraryDependencies += "mysql" % "mysql-connector-java" % "5.1.27"

play.Project.playScalaSettings
