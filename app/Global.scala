import akka.actor.Props
import play.api.libs.concurrent.Akka
import play.api.{GlobalSettings, Application}
import play.api.libs.concurrent.Execution.Implicits._
import scala.concurrent.duration._
import seven.DataLoaderActor

/**
 * Created with IntelliJ IDEA.
 * User: eyang
 * Date: 1/16/14
 * Time: 10:21 AM
 */
object Global extends GlobalSettings {

  override def onStart(app: Application): Unit = {
    // schedule task to load data timely
    val sys = Akka.system(app)
    val dataLoader = sys.actorOf(Props[DataLoaderActor])
    sys.scheduler.schedule(0.microsecond, 5.second, dataLoader, DataLoaderActor.Signal)
  }
}
