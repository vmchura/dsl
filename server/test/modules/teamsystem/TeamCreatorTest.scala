package modules.teamsystem

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import database.EmptyDBBeforeEach
import models.daos.teamsystem.TeamDAO
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.wordspec.AnyWordSpecLike
import shared.models.DiscordID

class TeamCreatorTest
    extends ScalaTestWithActorTestKit
    with AnyWordSpecLike
    with ScalaFutures
    with EmptyDBBeforeEach {
  "Team creator" must {
    "Create a team" in {

      /**
        *  teamCreatorWorker: TeamCreatorWorker,
      memberSupervisor: MemberSupervisor,
      teamMemberAddWorker: TeamMemberAddWorker
        */
      val userRequesting = DiscordID("userID")
      val teamCreatorWorker =
        app.injector.instanceOf(classOf[TeamCreatorWorker])
      val memberSupervisor = app.injector.instanceOf(classOf[MemberSupervisor])
      val teamMemberAddWorker =
        app.injector.instanceOf(classOf[TeamMemberAddWorker])
      val teamCreator =
        TeamCreator(teamCreatorWorker, memberSupervisor, teamMemberAddWorker)
      val teamDAO = app.injector.instanceOf(classOf[TeamDAO])
      val probe =
        testKit.createTestProbe[TeamCreator.CreationResponse]("probe-creation")
      val teamCreatorManager = testKit.spawn(teamCreator)

      teamCreatorManager ! TeamCreator.CreateTeam(
        probe.ref,
        userRequesting,
        "Skt1"
      )

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
