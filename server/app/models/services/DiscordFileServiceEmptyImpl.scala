package models.services

import java.io.File

import scala.concurrent.Future

class DiscordFileServiceEmptyImpl() extends DiscordFileService {
  protected def bot_token: String = ""
  def pushFileOnChannel(channelID: String, file: File, comment: String, fileName: String): Future[Boolean] = Future.successful(true)
}
