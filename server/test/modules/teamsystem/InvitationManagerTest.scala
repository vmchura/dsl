package modules.teamsystem

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import database.EmptyDBBeforeEach
import models.DiscordID
import models.daos.teamsystem.{InvitationDAO, TeamDAO}
import models.teamsystem.{
  Invitation,
  InvitationID,
  Member,
  MemberStatus,
  TeamID
}
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
class InvitationManagerTest
    extends ScalaTestWithActorTestKit
    with AnyWordSpecLike
    with GuiceOneAppPerSuite
    with EmptyDBBeforeEach {
  private val mainID = DiscordID("main")
  private val officialID = DiscordID("official")

  "Invitation Manager" must {
    "Realize invitation" in {
      val invitationDAOWorker =
        app.injector.instanceOf(classOf[InvitationDAOWorker])
      val invitationDAO = app.injector.instanceOf(classOf[InvitationDAO])
      implicit val teamDestroyer: TeamDestroyer =
        app.injector.instanceOf(classOf[TeamDestroyer])
      implicit val teamDAO: TeamDAO = app.injector.instanceOf(classOf[TeamDAO])
      implicit val teamMemberAddWorker: TeamMemberAddWorker =
        app.injector.instanceOf(classOf[TeamMemberAddWorker])
      implicit val memberSupervisor: MemberSupervisor =
        app.injector.instanceOf(classOf[MemberSupervisor])

      val teamManager = testKit.spawn(TeamManager())
      val invitationActor = testKit.spawn(
        InvitationManager(
          invitationDAOWorker,
          invitationDAO,
          teamManager,
          memberSupervisor
        )
      )
      val probe =
        testKit.createTestProbe[InvitationManager.InvitationManagerResponse](
          "probe-invitation-manager"
        )

      invitationActor ! InvitationManager.Invite(
        mainID,
        officialID,
        TeamID(UUID.randomUUID()),
        MemberStatus.Official,
        probe.ref
      )
      probe.expectMessage(InvitationManager.InvitationMade())
      whenReady(invitationDAO.invitationsFromUser(mainID)) { invitations =>
        assertResult(1)(invitations.size)
        val invitation = invitations.head
        assert(invitation.to == officialID)
      }
    }
    "Deny invitation" in {

      implicit val teamDAO = app.injector.instanceOf(classOf[TeamDAO])
      val _ = (for {
        created <- teamDAO.save(officialID, "skt1")
        added <- teamDAO.addMemberTo(
          Member(officialID, MemberStatus.Official),
          created
        )
      } yield {
        added
      }).futureValue

      val invitationDAOWorker =
        app.injector.instanceOf(classOf[InvitationDAOWorker])
      val invitationDAO = app.injector.instanceOf(classOf[InvitationDAO])

      implicit val teamDestroyer: TeamDestroyer =
        app.injector.instanceOf(classOf[TeamDestroyer])
      implicit val teamMemberAddWorker: TeamMemberAddWorker =
        app.injector.instanceOf(classOf[TeamMemberAddWorker])
      implicit val memberSupervisor: MemberSupervisor =
        app.injector.instanceOf(classOf[MemberSupervisor])

      val teamManager = testKit.spawn(TeamManager())
      val invitationActor = testKit.spawn(
        InvitationManager(
          invitationDAOWorker,
          invitationDAO,
          teamManager,
          memberSupervisor
        )
      )
      val probe =
        testKit.createTestProbe[InvitationManager.InvitationManagerResponse](
          "probe-invitation-manager"
        )
      invitationActor ! InvitationManager.Invite(
        mainID,
        officialID,
        TeamID(UUID.randomUUID()),
        MemberStatus.Official,
        probe.ref
      )
      probe.expectMessageType[InvitationManager.InvitationError]
    }
    "Accept invitation" in {
      implicit val teamDAO = app.injector.instanceOf(classOf[TeamDAO])
      val teamID = (for {
        created <- teamDAO.save(mainID, "skt1")
        _ <- teamDAO.addMemberTo(
          Member(mainID, MemberStatus.Official),
          created
        )
      } yield {
        created
      }).futureValue
      val invitationDAO = app.injector.instanceOf(classOf[InvitationDAO])
      val invitationID = InvitationID(UUID.randomUUID())
      invitationDAO
        .addInvitation(
          Invitation(
            invitationID,
            mainID,
            officialID,
            teamID,
            MemberStatus.Official
          )
        )
        .futureValue

      val invitationDAOWorker =
        app.injector.instanceOf(classOf[InvitationDAOWorker])

      implicit val teamDestroyer: TeamDestroyer =
        app.injector.instanceOf(classOf[TeamDestroyer])
      implicit val teamMemberAddWorker: TeamMemberAddWorker =
        app.injector.instanceOf(classOf[TeamMemberAddWorker])
      implicit val memberSupervisor: MemberSupervisor =
        app.injector.instanceOf(classOf[MemberSupervisor])

      val teamManager = testKit.spawn(TeamManager())
      val invitationActor = testKit.spawn(
        InvitationManager(
          invitationDAOWorker,
          invitationDAO,
          teamManager,
          memberSupervisor
        )
      )
      val probe =
        testKit.createTestProbe[InvitationManager.InvitationManagerResponse](
          "probe-invitation-manager"
        )

      whenReady(invitationDAO.invitationsFromUser(mainID)) { invitations =>
        assertResult(1)(invitations.size)
      }
      invitationActor ! InvitationManager.AcceptInvitation(
        invitationID,
        probe.ref
      )
      probe.expectMessage(InvitationManager.InvitationAccepted())

      whenReady(invitationDAO.invitationsFromUser(mainID)) { invitations =>
        assertResult(0)(invitations.size)
      }
      whenReady(teamDAO.loadTeams()) { teams =>
        assertResult(1)(teams.size)
        val team = teams.head
        assert(team.isOfficial(officialID))
        assert(team.isOfficial(mainID))
      }
    }
    "Remove invitation" in {

      val invitationDAO = app.injector.instanceOf(classOf[InvitationDAO])
      val invitationID = InvitationID(UUID.randomUUID())
      invitationDAO
        .addInvitation(
          Invitation(
            invitationID,
            mainID,
            officialID,
            TeamID(UUID.randomUUID()),
            MemberStatus.Official
          )
        )
        .futureValue
      val invitationDAOWorker =
        app.injector.instanceOf(classOf[InvitationDAOWorker])

      implicit val teamDestroyer: TeamDestroyer =
        app.injector.instanceOf(classOf[TeamDestroyer])
      implicit val teamMemberAddWorker: TeamMemberAddWorker =
        app.injector.instanceOf(classOf[TeamMemberAddWorker])
      implicit val memberSupervisor: MemberSupervisor =
        app.injector.instanceOf(classOf[MemberSupervisor])
      implicit val teamDAO = app.injector.instanceOf(classOf[TeamDAO])

      val teamManager = testKit.spawn(TeamManager())

      val invitationActor = testKit.spawn(
        InvitationManager(
          invitationDAOWorker,
          invitationDAO,
          teamManager,
          memberSupervisor
        )
      )
      val probe =
        testKit.createTestProbe[InvitationManager.InvitationManagerResponse](
          "probe-invitation-manager"
        )

      whenReady(invitationDAO.invitationsFromUser(mainID)) { invitations =>
        assertResult(1)(invitations.size)
      }
      invitationActor ! InvitationManager.RemoveInvitation(
        invitationID,
        probe.ref
      )
      probe.expectMessage(InvitationManager.InvitationRemoved())

      whenReady(invitationDAO.invitationsFromUser(mainID)) { invitations =>
        assertResult(0)(invitations.size)
      }
    }
  }

}
