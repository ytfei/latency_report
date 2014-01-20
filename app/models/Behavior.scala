package models

import anorm._
import anorm.SqlParser._
import play.api.Play.current
import play.api.db.DB
import play.Logger

/**
 * Created by eyang on 1/16/14.
 *
 * Behavior belongs to a Apk (scenarios)
 */
case class Behavior(id: Pk[Long], apk: Apk, name: String)

object Behavior {
  def addBehavior(apkId: Long, name: String) = {
    DB.withConnection(implicit c => {
      val id = SQL("select id from behavior where app_id = {app_id} and name = {name}")
        .on('app_id -> apkId, 'name -> name).as(scalar[Long].singleOpt)

      if (!id.isDefined) {
        SQL("insert into behavior(app_id, name) values({app_id},{name})")
          .on('app_id -> apkId, 'name -> name).executeUpdate()

        SQL(SqlUtil.last_insert_id).as(scalar[Long].singleOpt)
      } else {
        Logger.debug("Behavior info already exists, no need to add new one.")
        id
      }
    })
  }
}
