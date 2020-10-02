package jobs

import akka.actor._
import com.mohiva.play.silhouette.api.util.Clock
import javax.inject.Inject
import jobs.DiscordNameUpdater.Update
import models.services.UserHistoryService
import utils.Logger

import scala.concurrent.ExecutionContext.Implicits.global

/**
 * A job which cleanup invalid auth tokens.
 *
 * @param service The auth token service implementation.
 * @param clock The clock implementation.
 */
class DiscordNameUpdater @Inject()(
  userHistoryService: UserHistoryService,
  clock: Clock
)
  extends Actor with Logger {

  /**
   * Process the received messages.
   */
  def receive: Receive = {
    case Update =>
      val start = clock.now.getMillis
      val msg = new StringBuffer("\n")
      println("update")
      msg.append("=================================\n")
      msg.append("Start to update discord names\n")
      msg.append("=================================\n")

      userHistoryService.update().map { changed =>
        val seconds = (clock.now.getMillis - start) / 1000
        msg.append("Total of %s accounts(s) were updated in %s seconds".format(changed, seconds)).append("\n")
        msg.append("=================================\n")

        msg.append("=================================\n")
        println(msg.toString)
        logger.info(msg.toString)
      }.recover {
        case e =>
          msg.append("Couldn't update discordUsers because of unexpected error\n")
          msg.append("=================================\n")
          println(msg.toString)
          logger.error(msg.toString, e)
      }
  }
}

object DiscordNameUpdater{
  case object Update
}
