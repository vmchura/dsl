package controllers

import com.mohiva.play.silhouette.api.Authorization
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import models.User
import play.api.mvc.Request

import scala.concurrent.Future

case class WithAdmin() extends Authorization[User, CookieAuthenticator] {

  def isAuthorized[B](user: User, authenticator: CookieAuthenticator)(
    implicit request: Request[B]): Future[Boolean] = {
    Future.successful(user.loginInfo.providerKey.equals("698648718999814165"))
  }
}
