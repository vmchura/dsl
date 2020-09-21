package models

import java.io.{File, FileInputStream}
import java.util.UUID

import play.api.libs.json.{Json, OFormat}
import shared.models.ReplayRecordShared

case class ReplayRecord(replayID: UUID,
                        replayMD5Hash: String,
                        matchName: String, nombreOriginal: String,
                        tournamentID: Long, matchID: Long, enabled: Boolean,
                        uploaderDiscordID: String, dateGame: Option[String]){
  def sharedVersion(): ReplayRecordShared = ReplayRecordShared(replayID,matchName,nombreOriginal,enabled,dateGame)
}
object ReplayRecord{
  implicit val jsonFormat: OFormat[ReplayRecord] = Json.format[ReplayRecord]
  def md5HashString(file: File): String = {
    import java.security.MessageDigest
    import java.math.BigInteger
    val md = MessageDigest.getInstance("MD5")
    val in = new FileInputStream(file)
    val bytes = new Array[Byte](file.length.toInt)
    in.read(bytes)
    in.close()
    val digest = md.digest(bytes)
    val bigInt = new BigInteger(1,digest)
    val hashedString = bigInt.toString(16)
    hashedString
  }
}
