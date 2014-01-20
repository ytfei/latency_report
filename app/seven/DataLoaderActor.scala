package seven

import akka.actor.Actor
import play.api.{Play, Logger}
import play.api.Play.current
import java.io.{FileFilter, File}
import play.api.libs.Files
import models.{LatencyData, Device, Behavior, Apk}

/**
 * Created with IntelliJ IDEA.
 * User: eyang
 * Date: 1/16/14
 * Time: 10:28 AM
 */

object DataLoaderActor {
  val Signal = "check"
}

class DataLoaderActor extends Actor {

  import DataLoaderActor._

  val conf = Play.application.configuration
  val dataDir = conf.getString("report.data.dir")

  val TIME_REGEX = """(\d{14})(.*)""".r // start with timestamp

  val subDirFilter = new FileFilter {
    def accept(f: File): Boolean = {
      f.isDirectory && TIME_REGEX.findFirstMatchIn(f.getName).isDefined
    }
  }

  def receive = {
    case Signal => // receive trigger and begin to load data
      dataDir.map(path => {
        val f = new File(path)
        Logger.info("Checking " + f.getAbsolutePath)

        if (f.exists()) {
          f.listFiles(subDirFilter).foreach(loadDataFrom)
        } else {
          Logger.error("File " + path + " doesn't exits!")
        }
      })
  }

  val PERFORMANCE = "performance.csv"

  private def loadDataFrom(subDir: File): Unit = {
    val TIME_REGEX(testTime, _) = subDir.getName
    subDir.listFiles.foreach(f => f.getName match {
      case PERFORMANCE => loadPerformanceData(f, testTime)
      case "device-only" =>
    })
  }

  private def loadPerformanceData(file: File, testTime: String): Unit = {
    Logger.info("processing file: " + file.getAbsolutePath)

    val isMalformedLine = (line: String) => {
      val items = line.stripMargin.split(",")

      items.size < 2 ||
        items.size % 2 != 0 ||
        line.contains("-1.0") // ignore lines that failed
    }

    var map = Map[Tag, Map[String, Data]]()
    val processLine = (line: String) => {
      val items = line.split(",")
      val key = Tag(items(0), items(1))

      for (i <- 2 to(items.length - 1, 2)) {
        var data = map.get(key).getOrElse(Map[String, Data]())
        val device = data.get(items(i)).getOrElse(Data(items(i), List[Double]()))

        data += items(i) -> device.copy(latency = items(i + 1).toDouble :: device.latency)
        map += (key -> data)
      }
    }

    Files.readFile(file).lines
      .filterNot(isMalformedLine) // drop empty lines
      .foreach(processLine)

    map.foreach((entry: (Tag, Map[String, Data])) => {
      val apkId = Apk.addApk(entry._1.appName)
      val behaviorId = apkId.flatMap(id => Behavior.addBehavior(id, entry._1.behaviorName))

      behaviorId.map((bId: Long) => {
        entry._2.foreach((e: (String, Data)) => {
          val deviceId = Device.addDevice(e._1)
          deviceId.map((dId: Long) => {
            // todo: update the default value
            LatencyData.addLatencyData(bId, dId, 1L, e._2.avg, testTime)
          })
        })
      })
    })
  }

  private[this] case class Tag(appName: String, behaviorName: String)

  private[this] case class Data(deviceTag: String, latency: List[Double]) {
    def avg: Double = {
      if (latency.size == 0)
        0.0
      else
        latency.foldLeft(0.0)((sum, elem) => sum + elem) / latency.size
    }
  }

}



