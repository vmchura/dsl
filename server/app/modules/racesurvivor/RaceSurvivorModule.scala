package modules.racesurvivor

import com.google.inject.AbstractModule
import play.api.libs.concurrent.AkkaGuiceSupport

class RaceSurvivorModule extends AbstractModule with AkkaGuiceSupport {
  override def configure(): Unit = {
    bindTypedActor(CookieFabric(), "cookiefabric-actor")
  }
}
