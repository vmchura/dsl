package modules.winnersgeneration

import com.google.inject.AbstractModule
import play.api.libs.concurrent.AkkaGuiceSupport

class WinnersGenerationModule extends AbstractModule with AkkaGuiceSupport {
  override def configure(): Unit = {
    bindTypedActor(
      WinnersGathering,
      "Winners-generation-actor"
    )
  }
}
