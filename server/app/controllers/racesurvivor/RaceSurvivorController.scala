package controllers.racesurvivor
import controllers.{
  AbstractAuthController,
  AssetsFinder,
  SilhouetteControllerComponents
}

import javax.inject._
import play.api.i18n.I18nSupport

import scala.concurrent.{ExecutionContext, Future}
import akka.actor.typed._

import scala.concurrent.duration._
import akka.actor.typed.ActorRef
import play.api.mvc.{Action, AnyContent}
import akka.util.Timeout
import modules.gameparser.GameReplayManager.ManagerCommand
@Singleton
class RaceSurvivorController @Inject() (
    scc: SilhouetteControllerComponents
)(implicit
    assets: AssetsFinder,
    ex: ExecutionContext,
    scheduler: Scheduler
) extends AbstractAuthController(scc)
    with I18nSupport {
  implicit val timeout: Timeout = 5.seconds
  def sayHello(name: String): Action[AnyContent] = {
    Action.async { _ =>
      Future.successful(Ok(s"Hola $name"))

    }
    /* Action.async { _ =>
      cookieFabric.ask(ref => CookieFabric.GiveMeCookies(6, ref)).map {
        case Cookies(cookies) => Ok(s"Hello $name, you've got $cookies cookies")
        case InvalidRequest(_) =>
          Ok(s"Hello $name, you've got nothing, good day sr")
      }

    }*/
  }
}
