package jobs
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

class DiscordNameUpdaterTest extends PlaySpec with GuiceOneAppPerSuite {

  "An Echo actor" must {

    "send back messages unchanged" in {
      val a = app.actorSystem.actorSelection("discord-name-updated")
      println(a)
      a ! jobs.DiscordNameUpdater.Update

    }

  }
}
