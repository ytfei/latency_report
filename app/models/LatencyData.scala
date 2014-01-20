package models

import anorm._
import anorm.SqlParser._
import play.api.db.DB
import play.api.Play.current

/**
 * Created by eyang on 1/16/14.
 *
 * Load all latency data (from the data dir at one time), or rollback all data
 */

case class LatencyData(id: Pk[Long], behaviorId: Long, ocId: Long, latency: Double, testTime: String)

object LatencyData {
  def addLatencyData(behaviorId: Long, deviceId: Long, ocId: Long, latency: Double, testTime: String) {
    DB.withConnection(implicit c => {
      val num = SQL( """
                       | select 1 from latency where
                       | behavior_id = {behavior_id} and device_id = {device_id}
                       | and oc_id = {oc_id} and test_time = {test_time}
                     """.stripMargin)
        .on('behavior_id -> behaviorId, 'device_id -> deviceId, 'oc_id -> ocId, 'test_time -> testTime)
        .as(scalar[Long].singleOpt)

      if (num.isEmpty) {
        SQL( """
               | insert into latency(behavior_id, device_id, oc_id, latency, test_time)
               | values ({behavior_id}, {device_id}, {oc_id}, {latency}, {test_time})
             """.stripMargin).on('behavior_id -> behaviorId, 'device_id -> deviceId,
            'oc_id -> ocId, 'latency -> latency, 'test_time -> testTime).executeUpdate()
      }
    })
  }
}