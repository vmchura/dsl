package modules.teamsystem

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import database.EmptyDBBeforeEach
import models.daos.teamsystem.TeamDAO
import models.teamsystem.{Member, MemberStatus, TeamID}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.wordspec.AnyWordSpecLike
import shared.models.DiscordID

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
      implicit val teamDAO = app.injector.instanceOf(classOf[TeamDAO])
      val teamID = setup()

      whenReady(teamDAO.loadTeams()) { teams =>
        assertResult(1)(teams.size)
        val team = teams.head
        assert(team.isOfficial(officialID))
      }
      implicit val teamDestroyer: TeamDestroyer =
        app.injector.instanceOf(classOf[TeamDestroyer])
      implicit val teamMemberAddWorker: TeamMemberAddWorker =
        app.injector.instanceOf(classOf[TeamMemberAddWorker])
      implicit val memberSupervisor: MemberSupervisor =
        app.injector.instanceOf(classOf[MemberSupervisor])

      val actorRemove = testKit.spawn(TeamManager())

      val probe = testKit.createTestProbe[TeamManager.TeamManagerResponse](
        "teammanager-test"
      )
      actorRemove ! TeamManager.RemoveUserFrom(officialID, teamID, probe.ref)
      probe.expectMessage(TeamManager.Done())
      whenReady(teamDAO.loadTeams()) { teams =>
        assertResult(1)(teams.size)
        val team = teams.head
        assert(!team.isOfficial(officialID))
      }

    }
    "Destroy team if member quitting is principal" in {

      val teamID = setup()
      implicit val teamDestroyer: TeamDestroyer =
        app.injector.instanceOf(classOf[TeamDestroyer])
      implicit val teamMemberAddWorker: TeamMemberAddWorker =
        app.injector.instanceOf(classOf[TeamMemberAddWorker])
      implicit val memberSupervisor: MemberSupervisor =
        app.injector.instanceOf(classOf[MemberSupervisor])
      implicit val teamDAO = app.injector.instanceOf(classOf[TeamDAO])

      val actorRemove = testKit.spawn(TeamManager())
      val probe = testKit.createTestProbe[TeamManager.TeamManagerResponse](
        "teammanager-test"
      )

      whenReady(teamDAO.loadTeams()) { teams =>
        assert(teams.nonEmpty)

      }
      actorRemove ! TeamManager.RemoveUserFrom(mainID, teamID, probe.ref)
      probe.expectMessage(TeamManager.Done())
      whenReady(teamDAO.loadTeams()) { teams =>
        assert(teams.isEmpty)

      }

    }
  }
}
