package models

import anorm._
import anorm.SqlParser._
import play.api.Play.current
import play.api.db.DB
import play.Logger
import seven.SqlUtil

/**
 * Created by eyang on 1/16/14.
 *
 * Devices that used in test
 */
case class Device(id: Pk[Long], tag: String, serialId: String,
                  model: String, modelVersion: String, imei: String)

object Device {
  def addDevice(tag: String, serialId: Option[String] = None, model: Option[String] = None,
                modelVersion: Option[String] = None, imei: Option[String] = None) = {
    DB.withConnection(implicit c => {
      val id = SQL("select id from device where tag = {tag}")
        .on('tag -> tag)
        .as(scalar[Long].singleOpt)

      if (id.isEmpty) {
        SQL( """
               | insert into device(tag, serial_id, model, model_version, imei)
               | value({tag}, {sid}, {model}, {model_version}, {imei})
             """.stripMargin)
          .on('tag -> tag, 'sid -> serialId, 'model -> model,
            'model_version -> modelVersion, 'imei -> imei)
          .executeUpdate()

        SQL(SqlUtil.last_insert_id).as(scalar[Long].singleOpt)
      } else {
        Logger.debug("Device %s already exists, no need to add new one.".format(tag))
        id
      }
    })
  }
}