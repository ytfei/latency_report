package seven

import play.api.db.DB
import anorm._
import anorm.SqlParser._
import play.api.Play.current

/**
 * Created by eyang on 1/21/14.
 *
 * Generate the report data
 */
object Report {

  private val globalReportParser = {
    str("application.name") ~
      str("device.tag") ~
      get[Double]("latency") ~
      str("latency.test_time") map {
      case appName ~ deviceName ~ latency ~ testTime =>
        (appName, deviceName, latency, testTime)
    }
  }

  def reportByGlobal: Map[String, Map[String, List[String]]] = {
    val data = DB.withConnection(implicit c => {
      SQL( """
             | select app.name, d.tag, avg(l.latency) latency, l.test_time
             | from application app, behavior b, device d, latency l
             | where app.id = b.app_id and b.id = l.behavior_id and l.device_id = d.id
             | group by app.name, d.tag, l.test_time
             | order by test_time
           """.stripMargin).as(globalReportParser *)
    })

    var result = Map[String, Map[String, List[String]]]()
    for (e <- data) {
      val apkName = e._1.replace(".apk", "")
      var reportData = result.get(apkName).getOrElse(Map[String, List[String]]())

      val lineData = reportData.get(e._2).getOrElse(List[String]())
      reportData += e._2 -> (e._3.toString :: lineData)

      val xAxisLabel = "x_axis"
      val xAxis = reportData.get(xAxisLabel).getOrElse(List[String]())
      reportData += xAxisLabel -> (e._4 :: xAxis)

      result += apkName -> reportData
    }

    result
  }

  private val apkReportParser = {
    str("behavior.name") ~
      str("device.tag") ~
      get[Double]("latency") ~
      str("latency.test_time") map {
      case behaviorName ~ deviceName ~ latency ~ testTime =>
        (behaviorName, deviceName, latency, testTime)
    }
  }

  def reportByApk(apkName: String): Map[String, Map[String, List[String]]] = {
    val data = DB.withConnection(implicit c => {
      SQL( """
             | select b.name behavior_name, d.tag device_name, l.latency, l.test_time
             | from application app, behavior b, device d, latency l
             | where app.id = b.app_id and b.id = l.behavior_id and l.device_id = d.id and app.name = {app_name}
             | group by b.name, d.tag, l.test_time
             | order by l.test_time
           """.stripMargin).on('app_name -> apkName).as(apkReportParser *)
    })

    var result = Map[String, Map[String, List[String]]]()
    for (e <- data) {
      val behaviorName = e._1
      var reportData = result.get(behaviorName).getOrElse(Map[String, List[String]]())

      val lineData = reportData.get(e._2).getOrElse(List[String]())
      reportData += e._2 -> (e._3.toString :: lineData)

      val xAxisLabel = "x_axis"
      val xAxis = reportData.get(xAxisLabel).getOrElse(List[String]())
      reportData += xAxisLabel -> (e._4 :: xAxis)

      result += behaviorName -> reportData
    }

    result
  }
}
