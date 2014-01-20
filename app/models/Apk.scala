package models

import anorm._
import play.api.db.DB
import anorm.SqlParser._
import anorm.~
import play.api.Play.current
import play.Logger

/**
 * Created by eyang on 1/16/14.
 *
 * Apk model
 */
case class Apk(id: Pk[Long], name: String, version: String, versionCode: String)

object Apk {
  val simple = {
    get[Pk[Long]]("id") ~
      get[String]("name") ~
      get[String]("version") ~
      get[String]("version_code") map {
      case id ~ name ~ version ~ versionCode =>
        Apk(id, name, version, versionCode)
    }
  }

  def addApk(name: String, version: Option[String] = None, versionCode: Option[String] = None) = {
    DB.withConnection(implicit c => {
      val num = SQL("select id from application where name = {name}")
        .on('name -> name).as(scalar[Long].singleOpt)

      if (!num.isDefined) {
        Logger.info("Add new apk info: " + name)

        SQL( """
               | insert into application(name, version, version_code)
               | values({name}, {version}, {version_code})
             """.stripMargin)
          .on('name -> name, 'version -> version, 'version_code -> versionCode)
          .executeUpdate()

        SQL(SqlUtil.last_insert_id).as(scalar[Long].singleOpt)
      } else {
        Logger.debug("Apk info already exists, no need to add new one.")
        num
      }
    })
  }

  def getApkByName(name: String) = {
    DB.withConnection(implicit c => {
      SQL("select * from application where name = {name}")
        .on('name -> name)
        .as(simple.singleOpt)
    })
  }
}
