package models

import anorm._
import play.api.db.DB
import play.api.Play.current
import anorm.SqlParser._
import play.Logger

/**
 * Created by eyang on 1/16/14.
 *
 * Open Channel app
 */
case class OC(id: Pk[Long], name: String, version: String, relayHost: Option[String],
              dormancy: Option[Int], versionCode: Option[String])

object OC {

  def apply(data: Map[String, String]): OC = {
    val id = Id(0L)
    val name = data.get("").getOrElse("unknown")
    val relayHost = data.get("relay_host")
    val dormancy = data.get("dormancy_timeout").map(s => s.toInt)
    val version = data.get("version_name").getOrElse("unknown")
    val versionCode = data.get("version_code")

    new OC(id, name, version, relayHost, dormancy, versionCode)
  }

  def addOC(name: String, version: String, relayHost: Option[String],
            dormancy: Option[Int], versionCode: Option[String]) = {
    DB.withConnection(implicit c => {
      val num = SQL("select count(1) from oc where name = {name} and version = {v}")
        .on('name -> name, 'v -> version).as(scalar[Long].single)

      if (num == 0) {
        Logger.info("Add new oc info: " + name)
        SQL( """
               | insert into oc(name, relay_host, dormancy, version, version_code)
               | values({name}, {relay_host}, {dormancy}, {version}, {version_code})
             """.stripMargin)
          .on('name -> name, 'relay_host -> relayHost,
            'dormancy -> dormancy, 'version -> version,
            'version_code -> versionCode)
          .executeInsert()
      } else {
        Logger.debug("OC info already exists, no need to add new one.")
      }
    })
  }
}
