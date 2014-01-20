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
case class OC(id: Pk[Long], name: String, relayHost: String,
              dormancy: Int, version: String, versionCode: String)

object OC {
  def addOC(name: String, relayHost: Option[String], dormancy: Option[Int],
            version: Option[String], versionCode: Option[String]) = {
    DB.withConnection(implicit c => {
      val num = SQL("select count(1) from oc where name = {name}")
        .on('name -> name).as(scalar[Long].single)

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
