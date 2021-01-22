package modules.teamsystem

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import database.EmptyDBBeforeEach
import models.DiscordID
import models.daos.teamsystem.TeamDAO
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.wordspec.AnyWordSpecLike

class TeamCreatorTest
    extends ScalaTestWithActorTestKit
    with AnyWordSpecLike
    with ScalaFutures
    with EmptyDBBeforeEach {
  "Team creator" must {
    "Create a team" in {
      val userRequesting = DiscordID("userID")
      val teamCreator = app.injector.instanceOf(classOf[TeamCreator])
      val teamDAO = app.injector.instanceOf(classOf[TeamDAO])
      val probe =
        testKit.createTestProbe[TeamCreator.CreationResponse]("probe-creation")
      val teamCreatorActor = testKit.spawn(
        teamCreator.initialBehavior(userRequesting, "Skt1", probe.ref)
      )
      teamCreatorActor ! TeamCreator.Create()

      probe.expectMessage(TeamCreator.CreationDone())

      whenReady(teamDAO.loadTeams()) { teams =>
        assertResult(1)(teams.length)
        val team = teams.head
        assert(team.principal == userRequesting)
        assert(team.isOfficial(userRequesting))
      }

    }
  }
}
