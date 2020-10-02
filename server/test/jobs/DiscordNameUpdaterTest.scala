package jobs
import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import org.scalatest.{BeforeAndAfterAll, MustMatchers, WordSpec, WordSpecLike}
import akka.actor.Actor
import jobs.DiscordNameUpdater.Update
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import org.specs2.matcher.ShouldMatchers

class DiscordNameUpdaterTest extends PlaySpec with GuiceOneAppPerSuite {


  "An Echo actor" must {

    "send back messages unchanged" in {
      val a = app.actorSystem.actorSelection("discord-name-updated")
      println(a)
      a ! jobs.DiscordNameUpdater.Update

    }

  }
}
