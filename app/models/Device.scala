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
case class Device(id: Pk[Long], tag: String, serialId: Option[String],
                  model: Option[String], modelVersion: Option[String], imei: Option[String])

object Device {

  def apply(data: Map[String, String]): Device = {
    val id = Id(0L)
    val tag = data.get("tag").getOrElse("D1") // todo: how to define the tag <-> imei mapping?
    val sid = data.get("serial_id") // todo: no serial id for now ...
    val model = data.get("model")
    val modelVersion = data.get("modelVersion")
    val imei = data.get("imei")

    new Device(id, tag, sid, model, modelVersion, imei)
  }

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