package shared.models
import upickle.default.{macroRW, ReadWriter => RW}
import eu.timepit.refined._
import eu.timepit.refined.api.{RefType, Refined}
import eu.timepit.refined.auto._
import eu.timepit.refined.numeric._
case class DiscordPlayerLogged(
    discordID: DiscordID,
    username: String,
    discriminator: DiscordDiscriminator,
    avatar: Option[String]
)
object DiscordPlayerLogged {
  implicit val discriminatorRW: RW[DiscordDiscriminator] = upickle.default
    .readwriter[String]
    .bimap[DiscordDiscriminator](
      x => x.value,
      str => {
        RefType.applyRef[DiscordDiscriminator](str) match {
          case Right(value) => value
          case Left(_)      => "0000"
        }

      }
    )
  implicit val rw: RW[DiscordPlayerLogged] = macroRW

}
