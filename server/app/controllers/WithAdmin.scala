package controllers

import com.mohiva.play.silhouette.api.Authorization
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import models.User
import play.api.mvc.Request

import scala.concurrent.Future

case class WithAdmin() extends Authorization[User, CookieAuthenticator] {

  def isAuthorized[B](user: User, authenticator: CookieAuthenticator)(implicit
      request: Request[B]
  ): Future[Boolean] = {

    Future.successful(WithAdmin.isModeradorID(user.loginInfo.providerKey))
  }
}
object WithAdmin {
  private val moderadoresID = List(
    "562728682712465415", //kishigel
    "759633251509338123", //msluna
    "662869207633100840", //araknoides
    "445943955952107550", //Miky CC
    "698648718999814165", //vmchq
    "578353624699109435", //krakatoa
    "705804884045463583", //Richard
    "277854224795172868" // DeathFate
  )
  def isModeradorID(discordID: String): Boolean =
    moderadoresID.exists(_.equals(discordID))
}
