package shared.models
import upickle.default.{macroRW, ReadWriter => RW}

case class DiscordID(id: String) extends AnyVal
object DiscordID {
  implicit val discordIDRW: RW[DiscordID] = macroRW
}
