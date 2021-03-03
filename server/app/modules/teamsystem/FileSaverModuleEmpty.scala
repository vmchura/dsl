package modules.teamsystem

import com.google.inject.AbstractModule
import net.codingwell.scalaguice.ScalaModule
import play.api.libs.concurrent.AkkaGuiceSupport

class FileSaverModuleEmpty
    extends AbstractModule
    with ScalaModule
    with AkkaGuiceSupport {
  override def configure(): Unit = {
    bind[FileSaver].to[FileSaverImplEmpty]
  }
}
