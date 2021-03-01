package models.services

import java.io.File

import sttp.client.{SttpBackend, basicRequest}
import sttp.client._
import sttp.client.asynchttpclient.WebSocketHandler
import sttp.client.asynchttpclient.future.AsyncHttpClientFutureBackend
import utils.Logger
import javax.inject.Inject
import play.api.Configuration
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class DiscordFileServiceImpl @Inject() (configuration: Configuration)
    extends DiscordFileService
    with Logger {
  implicit val sttpBackend: SttpBackend[Future, Nothing, WebSocketHandler] =
    AsyncHttpClientFutureBackend()

  override protected val bot_token: String =
    configuration.get[String]("discord.bottoken")

  import net.dv8tion.jda.api.JDA
  import net.dv8tion.jda.api.JDABuilder

  private val jda: JDA = JDABuilder.createDefault(bot_token).build
  def pushFileOnChannel(
      channelID: String,
      file: File,
      comment: String,
      fileName: String
  ): Future[Boolean] = {

    try {
      jda.awaitReady
      val channel = jda.getTextChannelById(channelID.toLong)
      val res = channel.sendFile(file, fileName)
      res.queue(_.getAuthor)
      Future.successful(true)
    } catch {
      case _: Throwable => Future.successful(false)
    }

  }
}
