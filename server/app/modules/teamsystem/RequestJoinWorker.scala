package modules.teamsystem

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import com.google.inject.Inject
import models.daos.teamsystem.{RequestDAO, TeamDAO}
import models.teamsystem.{
  Member,
  MemberStatus,
  RequestJoin,
  RequestJoinID,
  TeamID
}
import shared.models.DiscordID

import java.util.UUID
import scala.util.{Failure, Success}

class RequestJoinWorker @Inject() ()(implicit
    requestDAO: RequestDAO,
    teamDAO: TeamDAO,
    memberSupervisor: MemberSupervisor
) {

  import RequestJoinManager._

  def initialBehavior(): Behavior[Command] = {
    def pendingReqSave(
        replyTo: ActorRef[RequestJoinResponse]
    ): Behavior[InternalCommand] =
      Behaviors.receiveMessage {
        case RequestSaved() =>
          replyTo ! RequestSuccessful()
          Behaviors.stopped
        case RequestError(reason) =>
          replyTo ! RequestProcessError(reason)
          Behaviors.stopped
        case _ =>
          replyTo ! RequestProcessError("Bad message process")
          Behaviors.stopped
      }

    def metaNotChecked(
        from: DiscordID,
        teamID: TeamID,
        replyTo: ActorRef[RequestJoinResponse]
    ): Behavior[InternalCommand] = {
      Behaviors.receive {
        case (ctx, MetaValid()) =>
          ctx.pipeToSelf(
            requestDAO.addRequest(
              models.teamsystem
                .RequestJoin(RequestJoinID(UUID.randomUUID()), from, teamID)
            )
          ) {
            case Success(_) => RequestSaved()

            case Failure(exception) =>
              RequestError(s"Meta not checked: ${exception.getMessage}")
          }

          pendingReqSave(replyTo)
        case (_, MetaInvalid()) =>
          replyTo ! RequestProcessError(
            "Sólo se puede ser miembro oficial de un equipo"
          )
          Behaviors.stopped

        case (_, RequestError(reason)) =>
          replyTo ! RequestProcessError(reason)
          Behaviors.stopped
        case _ =>
          replyTo ! RequestProcessError("Error de logica del sistema")
          Behaviors.stopped
      }
    }

    def pendingReqRemoval[T <: Response](
        responseOnSuccess: T,
        responseOnError: T,
        replyTo: Option[ActorRef[T]]
    ): Behavior[InternalCommand] = {
      Behaviors.receiveMessage {
        case ReqRemoved() =>
          replyTo.foreach(_ ! responseOnSuccess)
          Behaviors.stopped
        case _ =>
          replyTo.foreach(_ ! responseOnError)
          Behaviors.stopped
      }
    }

    def pendingMemberAdd(
        requestID: RequestJoinID,
        replyTo: ActorRef[AcceptRequestResponse]
    ): Behavior[InternalCommand] = {
      Behaviors.receive {
        case (ctx, MemberAdded()) =>
          ctx.pipeToSelf(requestDAO.removeRequest(requestID)) {
            case Success(true) => ReqRemoved()
            case _             => RequestError("Request removal failed")
          }
          pendingReqRemoval(
            RequestAcceptedSuccessful(),
            RequestProcessError(
              "El miembro se ha añadido, pero la Petición posiblemente siga base de datos"
            ),
            Some(replyTo)
          )

        case _ =>
          replyTo ! RequestProcessError(
            "El miembro se ha añadido, pero la Petición posiblemente siga base de datos"
          )
          Behaviors.stopped
      }
    }

    def requestNotChecked(
        request: RequestJoin,
        replyTo: ActorRef[AcceptRequestResponse]
    ): Behavior[InternalCommand] =
      Behaviors.receive {
        case (ctx, RequestValid()) =>
          ctx.pipeToSelf(
            teamDAO.addMemberTo(
              Member(request.from, MemberStatus.Official),
              request.teamID
            )
          ) {
            case Success(true) => MemberAdded()
            case _             => RequestError("Petición válida pero no se pudo procesar")
          }
          pendingMemberAdd(request.requestID, replyTo)
        case _ =>
          replyTo ! RequestProcessError(
            "La petición de ingreso ya no es válida"
          )
          Behaviors.stopped
      }

    def requestLoading(
        requestID: RequestJoinID,
        replyTo: ActorRef[AcceptRequestResponse]
    ): Behavior[InternalCommand] =
      Behaviors.receive {
        case (ctx, RequestLoaded(request)) =>
          val workerChecker =
            ctx.spawnAnonymous(memberSupervisor.initialBehavior())
          workerChecker ! MemberSupervisor.IsOfficial(
            request.from,
            ctx.messageAdapter {
              case MemberSupervisor.Yes() => RequestInvalid()
              case MemberSupervisor.No()  => RequestValid()
              case MemberSupervisor.ErrorOnQuery(reason) =>
                RequestError(s"Can't valid request: $reason")
            }
          )

          requestNotChecked(request, replyTo)

        case (ctx, RequestInvalid()) =>
          ctx.pipeToSelf(requestDAO.removeRequest(requestID)) {
            case Success(true) => ReqRemoved()
            case _             => RequestError("Request removal failed")
          }
          pendingReqRemoval(
            RequestProcessError(
              "La petición ya no es válida, se ha eliminado correctamente"
            ),
            RequestProcessError(
              "La petición ya no es válida, no se ha eliminado esta petición"
            ),
            Some(replyTo)
          )
        case _ =>
          replyTo ! RequestProcessError("Error checking Petición")
          Behaviors.stopped
      }

    Behaviors
      .receive[InternalCommand] {
        case (ctx, RequestJoinCommand(from, to, replyTo)) =>
          val workerChecker =
            ctx.spawnAnonymous(memberSupervisor.initialBehavior())
          workerChecker ! MemberSupervisor.IsOfficial(
            from,
            ctx.messageAdapter {
              case MemberSupervisor.Yes() => MetaInvalid()
              case MemberSupervisor.No()  => MetaValid()
              case MemberSupervisor.ErrorOnQuery(reason) =>
                RequestError(s"Can't valid meta information: $reason")
            }
          )
          metaNotChecked(from, to, replyTo)
        case (ctx, RemoveRequest(requestID, replyTo)) =>
          ctx.pipeToSelf(requestDAO.removeRequest(requestID)) {
            case Success(true) => ReqRemoved()
            case _             => RequestError("Error on removing request")
          }

          pendingReqRemoval(
            RequestRemovedSuccessful(),
            RequestProcessError("Error removing request"),
            replyTo
          )
        case (ctx, AcceptRequest(requestID, replyTo)) =>
          ctx.pipeToSelf(requestDAO.loadRequest(requestID)) {
            case Success(Some(value)) => RequestLoaded(value)
            case _                    => RequestError("Error on removing request")
          }
          requestLoading(requestID, replyTo)

      }
      .narrow
  }

}
