package models.services

import java.util.UUID
import javax.inject.Inject

import com.mohiva.play.silhouette.api.util.Clock
import models.AuthToken
import models.daos.AuthTokenDAO
import org.joda.time.DateTimeZone

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Handles actions to auth tokens.
  *
  * @param authTokenDAO The auth token DAO implementation.
  * @param clock The clock instance.
  */
class AuthTokenServiceImpl @Inject() (authTokenDAO: AuthTokenDAO, clock: Clock)
    extends AuthTokenService {

  /**
    * Creates a new auth token and saves it in the backing store.
    *
    * @param userID The user ID for which the token should be created.
    * @param expiry The duration a token expires.
    * @return The saved auth token.
    */
  def create(
      userID: UUID,
      expiry: FiniteDuration = 5 minutes
  ): Future[AuthToken] = {
    val token = AuthToken(
      UUID.randomUUID(),
      userID,
      clock.now
        .withZone(DateTimeZone.UTC)
        .plusSeconds(expiry.toSeconds.toInt)
        .getMillis / 1000
    )
    authTokenDAO.save(token)
  }

  /**
    * Validates a token ID.
    *
    * @param id The token ID to validate.
    * @return The token if it's valid, None otherwise.
    */
  def validate(id: UUID): Future[Option[AuthToken]] = authTokenDAO.find(id)

  /**
    * Cleans expired tokens.
    *
    * @return The list of deleted tokens.
    */
  def clean: Future[Seq[AuthToken]] =
    authTokenDAO
      .findExpired(clock.now.withZone(DateTimeZone.UTC).getMillis / 1000)
      .flatMap { tokens =>
        Future.sequence(tokens.map { token =>
          authTokenDAO.remove(token.id).map(_ => token)
        })
      }
}
