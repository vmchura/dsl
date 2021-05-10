package controllers

import com.google.inject.Inject
import models.{TrovoUser, TrovoUserID, User}
import models.services.{AuthTokenService, UserService, UserTrovoService}
import play.api.i18n.I18nSupport
import play.api.libs.json.Json

import javax.inject.Singleton
import scala.concurrent.{ExecutionContext, Future}
import sttp.client._
import sttp.client.asynchttpclient.WebSocketHandler
import sttp.client.asynchttpclient.future.AsyncHttpClientFutureBackend

import java.util.UUID
import scala.util.Try
import play.api.Configuration
@Singleton
class TrovoController @Inject() (
    scc: SilhouetteControllerComponents,
    authTokenService: AuthTokenService,
    userTrovoService: UserTrovoService,
    userService: UserService,
    configuration: Configuration
)(implicit
    assets: AssetsFinder,
    ex: ExecutionContext
) extends AbstractAuthController(scc)
    with I18nSupport {
  implicit val sttpBackend: SttpBackend[Future, Nothing, WebSocketHandler] =
    AsyncHttpClientFutureBackend()

  def login() =
    silhouette.SecuredAction.async { implicit request =>
      authTokenService.create(request.identity.userID).map { token =>
        Redirect(
          "https://open.trovo.live/page/login.html",
          Map(
            "response_type" -> Seq("token"),
            "scope" -> Seq("user_details_self"),
            "client_id" -> Seq(configuration.get[String]("trovo.clientID")),
            "state" -> Seq(token.id.toString),
            "redirect_uri" -> Seq(
              configuration.get[String]("trovo.redirect")
            )
          ),
          SEE_OTHER
        )
      }

    }
  def trovoCallBack(access_token: String, expires_in: Int, state: String) =
    Action.async { implicit request =>
      val result = Redirect(routes.StaticsController.view())
      def retrieveInformation(user: User): Future[Either[String, TrovoUser]] = {
        val responseFut = basicRequest
          .headers(
            Map(
              "Accept" -> "application/json",
              "Client-ID" -> configuration.get[String]("trovo.clientID"),
              "Authorization" -> s"OAuth $access_token"
            )
          )
          .get(
            uri"https://open-api.trovo.live/openplatform/validate"
          )
          .send()

        responseFut.map {
          _.body match {
            case Left(error) =>
              Left(s"Error at trovo callback $error")
            case Right(body) =>
              Try {
                val json = Json.parse(body)
                TrovoUser(
                  user.loginInfo.providerKey,
                  TrovoUserID((json \ "uid").as[String].toLong),
                  (json \ "nick_name").as[String]
                )
              }.toEither match {
                case Left(throwable) => Left(throwable.getMessage)
                case Right(value)    => Right(value)
              }
          }

        }
      }
      for {
        validation <- authTokenService.validate(UUID.fromString(state))
        user <- validation.fold(Future.successful(Option.empty[User]))(token =>
          userService.retrieve(token.userID)
        )
        trovoUser <- user.fold(
          Future.successful(
            Left("Token response not assigned to a discord user"): Either[
              String,
              TrovoUser
            ]
          )
        )(retrieveInformation)

        insertion <- trovoUser match {
          case Left(error) => Future.successful(Left(error))
          case Right(tu) =>
            userTrovoService.save(tu).map {
              case Some(_) => Right("Registro exitoso")
              case None    => Left("Usuario NO se ha podido registrar")
            }
        }

      } yield {

        insertion match {
          case Left(error)    => result.flashing("error" -> error)
          case Right(message) => result.flashing("success" -> message)
        }
      }

    }

  def allTrovoUsers() =
    Action.async { implicit request =>
      userTrovoService.all().map { users =>
        Ok(Json.arr(users))
      }
    }
}
