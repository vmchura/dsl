package controllers

import com.mohiva.play.silhouette.api.actions.SecuredRequest
import javax.inject._
import jobs.ParticipantUpdater
import play.api.mvc._
import play.api.i18n.I18nSupport
import play.api.libs.json.Json
import shared.utils.BasicComparableByLabel
import upickle.default._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ParticipantController @Inject()(scc: SilhouetteControllerComponents,
                                      participantUpdater: ParticipantUpdater
                                    ) (
                                      implicit
                                      assets: AssetsFinder,
                                      ex: ExecutionContext
                                    )extends   AbstractAuthController(scc) with I18nSupport {

  def buildRelation(): Action[AnyContent] = silhouette.SecuredAction(WithAdmin()).async { implicit request: SecuredRequest[EnvType, AnyContent] =>
    def convertResponse(response: Boolean) = Ok(Json.obj("response" -> write(response)))
    println(request.body)
    request.body.asMultipartFormData.fold{
      logger.error("buildRelation has not a valid multipartFormData")
      Future.successful(convertResponse(false))
    }{ data =>
       println(data)
      val updateExecution = for{
        basicChallonge <-  try { Future.successful(read[BasicComparableByLabel](data.dataParts("challonge").head))} catch {case _: Throwable => Future.failed(new IllegalArgumentException("first cant parse to basic label"))}
        basicDiscord <-  try { Future.successful(read[BasicComparableByLabel](data.dataParts("discord").head))} catch {case _: Throwable => Future.failed(new IllegalArgumentException("second cant parse to basic label"))}
        update <- participantUpdater.updateParticipant(basicChallonge,basicDiscord)
      }yield {
            update
      }

      updateExecution.map{
          case Left(error) =>
            logger.error(error.toString)
            convertResponse(false)
          case Right(_) => convertResponse(true)
      }.recoverWith{
        case error: Throwable =>
          logger.error(error.toString)
          Future.successful(convertResponse(false))
      }



    }
  }

}