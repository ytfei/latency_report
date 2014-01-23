package controllers

import play.api.mvc._
import views._
import models.Apk
import seven.Report
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.format.Formats
import java.util.Date

object Application extends Controller {

  def index = Action {
    Ok(html.index(Apk.getAll, queryForm))
  }

  def byGlobal = Action {
    Ok(html.reports.by_global(Report.reportByGlobal))
  }

  def byApk(apkName: String) = Action {
    Ok(html.reports.by_apk(Report.reportByApk(apkName)))
  }

  def queryReport = Action {
    implicit request =>
      queryForm.bindFromRequest.fold(
        error => BadRequest(html.index(Apk.getAll, error)),
        data => Ok(html.index(Apk.getAll, queryForm.fill(data)))
      )
  }

  val myDateFormat = of[Date](Formats.dateFormat("yyyy-MM-dd HH:mm:ss"))
  val queryForm = Form(mapping(
    "key" -> nonEmptyText,
    "startAt" -> myDateFormat.verifying("error date format, for demo", d => false),
    "endAt" -> myDateFormat)(ReportQuery.apply)(ReportQuery.unapply)
    .verifying("The start time should after end time!", data => {
      data.startAt.before(data.endAt)
  }))

}

case class ReportQuery(key: String, startAt: Date, endAt: Date)