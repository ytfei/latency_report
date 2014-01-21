package controllers

import play.api._
import play.api.mvc._
import views._
import models.Apk
import seven.Report

object Application extends Controller {

  def index = Action {
    Ok(html.index(Apk.getAll))
  }

  def byGlobal = Action {
    Ok(html.reports.by_global(Report.reportByGlobal))
  }

  def byApk(apkName: String) = Action {
    Ok(html.reports.by_apk(Report.reportByApk(apkName)))
  }

}