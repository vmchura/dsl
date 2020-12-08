package controllers.racesurvivor
import controllers.{
  AbstractAuthController,
  AssetsFinder,
  SilhouetteControllerComponents
}
import akka.actor._

import javax.inject._
import modules.racesurvivor.HelloActor
import play.api.i18n.I18nSupport

import scala.concurrent.ExecutionContext
import HelloActor._

import scala.concurrent.duration._
import akka.pattern.ask
import akka.util.Timeout
import play.api.mvc.{Action, AnyContent}
@Singleton
class RaceSurvivorController @Inject() (
    system: ActorSystem,
    scc: SilhouetteControllerComponents
)(implicit
    assets: AssetsFinder,
    ex: ExecutionContext
) extends AbstractAuthController(scc)
    with I18nSupport {
  implicit val timeout: Timeout = 5.seconds

  private val helloActor = system.actorOf(HelloActor.props, "hello-actor")
  def sayHello(name: String): Action[AnyContent] =
    Action.async {
      (helloActor ? SayHello(name)).mapTo[String].map { message =>
        Ok(message)
      }
    }
}
