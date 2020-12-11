package modules.gameparser

import com.google.inject.AbstractModule
import play.api.libs.concurrent.AkkaGuiceSupport

class GameParserModule extends AbstractModule with AkkaGuiceSupport {
  override def configure(): Unit = {
    bindTypedActor(GameReplayManager, "game-replay-manager-actor")
  }
}
