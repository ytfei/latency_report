package seven.util

import scala.io.Source
import java.io.File

/**
 * Created by eyang on 1/24/14.
 *
 */
object YamlLoader {
  def load(file: File): Map[String, String] = {
    var data = Map[String, String]()

    Source.fromFile(file, "UTF-8").getLines()
      .filterNot(v => v.startsWith("--"))
      .foreach(line => {
      val parts = line.split(":")
      if (parts.size == 2)
        data += strip(parts(0)) -> strip(parts(1))
    })

    data
  }

  private val STRIPABLE = """^['|"](.*)['|"]$""".r

  private def strip(s: String): String = {
    s.trim match {
      case STRIPABLE(v) => v
      case str => str
    }
  }

  def load(path: String): Map[String, String] = {
    load(new File(path))
  }
}
