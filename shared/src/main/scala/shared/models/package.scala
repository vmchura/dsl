package shared
import eu.timepit.refined.api.Refined
import eu.timepit.refined.W
import eu.timepit.refined.string._

package object models {
  type DiscordDiscriminator = String Refined MatchesRegex[W.`"""[0-9]{4}"""`.T]
}
