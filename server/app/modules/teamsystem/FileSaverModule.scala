package modules.teamsystem

import com.google.inject.AbstractModule
import net.codingwell.scalaguice.ScalaModule
import play.api.libs.concurrent.AkkaGuiceSupport

class FileSaverModule
    extends AbstractModule
    with ScalaModule
    with AkkaGuiceSupport {
  override def configure(): Unit = {
    bind[FileSaver].to[FileSaverImpl]

  }
}
