package seven

import akka.actor.Actor
import play.api.{Play, Logger}
import play.api.Play.current
import java.io.{FileFilter, File}
import play.api.libs.Files
import models.{LatencyData, Device, Behavior, Apk}
import seven.util.YamlLoader

/**
 * Created with IntelliJ IDEA.
 * User: eyang
 * Date: 1/16/14
 * Time: 10:28 AM
 */

object DataLoaderActor {
  val Signal = "check"

  // todo: we should load this info from configuration files.
  private val sid2imei = Map("4df1d4cc71639f5f" -> "354666058239559",
    "4df715a1116dcf5f" -> "355251053959128",
    "4df71bad4728cf6f" -> "355251055270136",
    "4df798b5208dcf9f" -> "355251055270607")

  private val imei2sid = sid2imei.map(x => x._2 -> x._1)

  private val sid2tag = Map("4df1d4cc71639f5f" -> "D4", "4df715a1116dcf5f" -> "D1",
    "4df71bad4728cf6f" -> "D3", "4df71bad4728cf6f" -> "D2")

  private def isSerialId(id: String) = sid2imei.contains(id)

  private def isIMEI(id: String) = imei2sid.contains(id)

  private def toSerialId(imei: String) = imei2sid.get(imei)

  private def toIMEI(sid: String) = sid2imei.get(sid)

  private def toTag(id: String): String = {
    if (isSerialId(id))
      sid2tag.get(id).get
    else if (isIMEI(id)) {
      toSerialId(id).fold("")((imei: String) => toSerialId(imei).fold("")(sid => sid2tag.get(sid).get))
    } else "unknown"
  }

  def fromId(id: String): Option[(String, String)] = {
    if (isSerialId(id) && toIMEI(id).isDefined)
      Some((id, toIMEI(id).get))
    else if (isIMEI(id) && toSerialId(id).isDefined) {
      Some((toSerialId(id).get, id))
    } else {
      None
    }
  }
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

  private val PERFORMANCE = "performance.csv"
  private val OC = """device-(\w{1,})-oc-info.yml""".r
  private val DEVICE = """device-(\w{1,})-device-info.yml""".r

  private def loadDataFrom(subDir: File): Unit = {
    val TIME_REGEX(testTime, _) = subDir.getName
    subDir.listFiles.foreach(f => f.getName match {
      case PERFORMANCE => loadPerformanceData(f, testTime)
      case OC(id) => loadOCData(id, f)
      case DEVICE(id) => loadDeviceData(id, f)
      case str => Logger.warn("Unsupported file: " + f.getAbsolutePath)
    })
  }

  def loadOCData(id: String, file: File) = {
    fromId(id).fold(Logger.error("Cannot find any id info for device: %s, data file %s".format(id, file.getAbsolutePath)))(v => {
      val data = YamlLoader.load(file)
      val oc = models.OC(data)

      models.OC.addOC(oc.name, oc.version, oc.relayHost, oc.dormancy, oc.versionCode)
    })
  }

  def loadDeviceData(id: String, file: File): Unit = {
    fromId(id).fold(Logger.error("Cannot find any id info for device: %s, data file %s".format(id, file.getAbsolutePath)))(v => {
      val (sid, _) = v
      val data = YamlLoader.load(file)
      val device = Device(data).copy(serialId = Some(sid), tag = toTag(sid))

      Device.addDevice(device.tag, device.serialId, device.model,
        device.modelVersion, device.imei)
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
    def avg: Double =
      if (latency.size == 0) 0.0
      else latency.foldLeft(0.0)((sum, elem) => sum + elem) / latency.size
  }

}



