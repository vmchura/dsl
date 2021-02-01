package modules.teamsystem

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import database.EmptyDBBeforeEach
import models.daos.teamsystem.{RequestDAO, TeamDAO}
import models.teamsystem._
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import shared.models.DiscordID

import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global

class RequestManagerTest
    extends ScalaTestWithActorTestKit
    with AnyWordSpecLike
    with GuiceOneAppPerSuite
    with EmptyDBBeforeEach {
  private val mainID = DiscordID("main")
  private val officialID = DiscordID("official")

  "Request Manager" must {
    "Realize request" in {
      val requestJoinWorker =
        app.injector.instanceOf(classOf[RequestJoinWorker])
      val requestDAO = app.injector.instanceOf(classOf[RequestDAO])
      implicit val teamDestroyer: TeamDestroyer =
        app.injector.instanceOf(classOf[TeamDestroyer])
      implicit val teamDAO: TeamDAO = app.injector.instanceOf(classOf[TeamDAO])
      implicit val teamMemberAddWorker: TeamMemberAddWorker =
        app.injector.instanceOf(classOf[TeamMemberAddWorker])
      implicit val memberSupervisor: MemberSupervisor =
        app.injector.instanceOf(classOf[MemberSupervisor])

      val requestWorker = testKit.spawn(
        RequestJoinManager(requestJoinWorker)
      )
      val probe =
        testKit.createTestProbe[RequestJoinManager.RequestJoinResponse](
          "probe-invitation-manager"
        )

      val teamID = TeamID(UUID.randomUUID())
      requestWorker ! RequestJoinManager.RequestJoinCommand(
        officialID,
        teamID,
        probe.ref
      )
      probe.expectMessage(RequestJoinManager.RequestSuccessful())
      whenReady(requestDAO.requestsToTeam(teamID)) { requests =>
        assertResult(1)(requests.size)
        val request = requests.head
        assert(request.from == officialID)
      }
    }
    "Deny request" in {
      implicit val teamDAO: TeamDAO = app.injector.instanceOf(classOf[TeamDAO])
      val _ = (for {
        created <- teamDAO.save(officialID, "skt1")
        added <- teamDAO.addMemberTo(
          Member(officialID, MemberStatus.Official),
          created
        )
      } yield {
        added
      }).futureValue
      val requestJoinWorker =
        app.injector.instanceOf(classOf[RequestJoinWorker])
      val requestDAO = app.injector.instanceOf(classOf[RequestDAO])
      implicit val teamDestroyer: TeamDestroyer =
        app.injector.instanceOf(classOf[TeamDestroyer])
      implicit val teamMemberAddWorker: TeamMemberAddWorker =
        app.injector.instanceOf(classOf[TeamMemberAddWorker])
      implicit val memberSupervisor: MemberSupervisor =
        app.injector.instanceOf(classOf[MemberSupervisor])

      val requestWorker = testKit.spawn(
        RequestJoinManager(requestJoinWorker)
      )
      val probe =
        testKit.createTestProbe[RequestJoinManager.RequestJoinResponse](
          "probe-invitation-manager"
        )

      val teamID = TeamID(UUID.randomUUID())
      requestWorker ! RequestJoinManager.RequestJoinCommand(
        officialID,
        teamID,
        probe.ref
      )
      probe.expectMessageType[RequestJoinManager.RequestProcessError]
      whenReady(requestDAO.requestsToTeam(teamID)) { requests =>
        assertResult(0)(requests.size)
      }
    }
    "Accept request" in {
      implicit val teamDAO: TeamDAO = app.injector.instanceOf(classOf[TeamDAO])

      val requestJoinWorker =
        app.injector.instanceOf(classOf[RequestJoinWorker])
      val requestDAO = app.injector.instanceOf(classOf[RequestDAO])
      val (teamID, requestID) = (for {
        created <- teamDAO.save(mainID, "skt1")
        _ <- teamDAO.addMemberTo(
          Member(mainID, MemberStatus.Official),
          created
        )
        requestID <- requestDAO.addRequest(
          RequestJoin(RequestJoinID(UUID.randomUUID()), officialID, created)
        )
      } yield {
        (created, requestID)
      }).futureValue

      implicit val teamDestroyer: TeamDestroyer =
        app.injector.instanceOf(classOf[TeamDestroyer])
      implicit val teamMemberAddWorker: TeamMemberAddWorker =
        app.injector.instanceOf(classOf[TeamMemberAddWorker])
      implicit val memberSupervisor: MemberSupervisor =
        app.injector.instanceOf(classOf[MemberSupervisor])

      val requestWorker = testKit.spawn(
        RequestJoinManager(requestJoinWorker)
      )
      val probe =
        testKit.createTestProbe[RequestJoinManager.AcceptRequestResponse](
          "probe-invitation-manager"
        )

      whenReady(requestDAO.requestsToTeam(teamID)) { requests =>
        assertResult(1)(requests.size)
        val request = requests.head
        assert(request.from == officialID)
      }
      whenReady(teamDAO.loadTeam(teamID)) { teamOpt =>
        assert(teamOpt.isDefined)
        val team = teamOpt.get
        assert(!team.isOfficial(officialID))
      }

      requestWorker ! RequestJoinManager.AcceptRequest(
        requestID,
        probe.ref
      )
      probe.expectMessage(RequestJoinManager.RequestAcceptedSuccessful())

      whenReady(requestDAO.requestsToTeam(teamID)) { requests =>
        assertResult(0)(requests.size)
      }
      whenReady(teamDAO.loadTeam(teamID)) { teamOpt =>
        assert(teamOpt.isDefined)
        val team = teamOpt.get
        assert(team.isOfficial(officialID))
      }
    }

    "Remove request" in {
      implicit val teamDAO: TeamDAO = app.injector.instanceOf(classOf[TeamDAO])

      val requestJoinWorker =
        app.injector.instanceOf(classOf[RequestJoinWorker])
      val requestDAO = app.injector.instanceOf(classOf[RequestDAO])
      val (teamID, requestID) = (for {
        created <- teamDAO.save(mainID, "skt1")
        _ <- teamDAO.addMemberTo(
          Member(mainID, MemberStatus.Official),
          created
        )
        requestID <- requestDAO.addRequest(
          RequestJoin(RequestJoinID(UUID.randomUUID()), officialID, created)
        )
      } yield {
        (created, requestID)
      }).futureValue

      implicit val teamDestroyer: TeamDestroyer =
        app.injector.instanceOf(classOf[TeamDestroyer])
      implicit val teamMemberAddWorker: TeamMemberAddWorker =
        app.injector.instanceOf(classOf[TeamMemberAddWorker])
      implicit val memberSupervisor: MemberSupervisor =
        app.injector.instanceOf(classOf[MemberSupervisor])

      val requestWorker = testKit.spawn(
        RequestJoinManager(requestJoinWorker)
      )
      val probe =
        testKit.createTestProbe[RequestJoinManager.RemoveRequestResponse](
          "probe-invitation-manager"
        )

      whenReady(requestDAO.requestsToTeam(teamID)) { requests =>
        assertResult(1)(requests.size)
        val request = requests.head
        assert(request.from == officialID)
      }
      whenReady(teamDAO.loadTeam(teamID)) { teamOpt =>
        assert(teamOpt.isDefined)
        val team = teamOpt.get
        assert(!team.isOfficial(officialID))
      }

      requestWorker ! RequestJoinManager.RemoveRequest(
        requestID,
        Some(probe.ref)
      )
      probe.expectMessage(RequestJoinManager.RequestRemovedSuccessful())

      whenReady(requestDAO.requestsToTeam(teamID)) { requests =>
        assertResult(0)(requests.size)
      }
      whenReady(teamDAO.loadTeam(teamID)) { teamOpt =>
        assert(teamOpt.isDefined)
        val team = teamOpt.get
        assert(!team.isOfficial(officialID))
      }
    }

    "Deny then remove request" in {
      implicit val teamDAO: TeamDAO = app.injector.instanceOf(classOf[TeamDAO])

      val requestJoinWorker =
        app.injector.instanceOf(classOf[RequestJoinWorker])
      val requestDAO = app.injector.instanceOf(classOf[RequestDAO])
      val (teamID, requestID) = (for {
        created <- teamDAO.save(mainID, "skt1")
        _ <- teamDAO.addMemberTo(
          Member(mainID, MemberStatus.Official),
          created
        )
        requestID <- requestDAO.addRequest(
          RequestJoin(RequestJoinID(UUID.randomUUID()), officialID, created)
        )
        _ <- teamDAO.addMemberTo(
          Member(officialID, MemberStatus.Official),
          created
        )
      } yield {
        (created, requestID)
      }).futureValue

      implicit val teamDestroyer: TeamDestroyer =
        app.injector.instanceOf(classOf[TeamDestroyer])
      implicit val teamMemberAddWorker: TeamMemberAddWorker =
        app.injector.instanceOf(classOf[TeamMemberAddWorker])
      implicit val memberSupervisor: MemberSupervisor =
        app.injector.instanceOf(classOf[MemberSupervisor])

      val requestWorker = testKit.spawn(
        RequestJoinManager(requestJoinWorker)
      )
      val probe =
        testKit.createTestProbe[RequestJoinManager.AcceptRequestResponse](
          "probe-invitation-manager"
        )

      whenReady(requestDAO.requestsToTeam(teamID)) { requests =>
        assertResult(1)(requests.size)
        val request = requests.head
        assert(request.from == officialID)
      }

      requestWorker ! RequestJoinManager.AcceptRequest(
        requestID,
        probe.ref
      )
      probe.expectMessageType[RequestJoinManager.RequestProcessError]

      whenReady(requestDAO.requestsToTeam(teamID)) { requests =>
        assertResult(0)(requests.size)
      }
      whenReady(teamDAO.loadTeam(teamID)) { teamOpt =>
        assert(teamOpt.isDefined)
        val team = teamOpt.get
        assert(team.isOfficial(officialID))
      }
    }
  }

}
