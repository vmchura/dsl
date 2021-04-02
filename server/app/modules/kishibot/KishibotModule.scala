package modules.kishibot

import com.google.inject.AbstractModule
import play.api.libs.concurrent.AkkaGuiceSupport

class KishibotModule extends AbstractModule with AkkaGuiceSupport {
  override def configure(): Unit = {
    bindTypedActor(
      KishibotActor,
      "Kishibot-actor"
    )
  }
}
