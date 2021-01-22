package modules.teamsystem

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import database.EmptyDBBeforeEach
import models.DiscordID
import models.daos.teamsystem.TeamDAO
import models.teamsystem.{Member, MemberStatus, TeamID}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.ExecutionContext.Implicits.global

class TeamManagerTest
    extends ScalaTestWithActorTestKit
    with AnyWordSpecLike
    with ScalaFutures
    with EmptyDBBeforeEach {
  private val mainID = DiscordID("main")
  private val officialID = DiscordID("official")
  private val suplenteID = DiscordID("suplente")
  val teamDAO = app.injector.instanceOf(classOf[TeamDAO])
  def setup(): TeamID = {

    val teamID = (for {
      created <- teamDAO.save(mainID, "skt1")
      _ <- teamDAO.addMemberTo(
        Member(mainID, MemberStatus.Official),
        created
      )
      _ <- teamDAO.addMemberTo(
        Member(officialID, MemberStatus.Official),
        created
      )
    } yield {
      created
    }).futureValue
    teamID
  }
  "Team Manager" must {
    "Remove member" in {

      val teamID = setup()

      whenReady(teamDAO.loadTeams()) { teams =>
        assertResult(1)(teams.size)
        val team = teams.head
        assert(team.isOfficial(officialID))
      }
      val teamManager = app.injector.instanceOf(classOf[TeamManager])
      val probe = testKit.createTestProbe[TeamManager.TeamManagerResponse](
        "teammanager-test"
      )
      val actorRemove = testKit.spawn(teamManager.initialBehavior(probe.ref))
      actorRemove ! TeamManager.RemoveUserFrom(officialID, teamID)
      probe.expectMessage(TeamManager.Done())
      whenReady(teamDAO.loadTeams()) { teams =>
        assertResult(1)(teams.size)
        val team = teams.head
        assert(!team.isOfficial(officialID))
      }

    }
    "Destroy team if member quitting is principal" in {

      val teamID = setup()

      val teamManager = app.injector.instanceOf(classOf[TeamManager])
      val probe = testKit.createTestProbe[TeamManager.TeamManagerResponse](
        "teammanager-test"
      )
      val actorRemove = testKit.spawn(teamManager.initialBehavior(probe.ref))

      whenReady(teamDAO.loadTeams()) { teams =>
        assert(teams.nonEmpty)

      }
      actorRemove ! TeamManager.RemoveUserFrom(mainID, teamID)
      probe.expectMessage(TeamManager.Done())
      whenReady(teamDAO.loadTeams()) { teams =>
        assert(teams.isEmpty)

      }

    }
  }
}
