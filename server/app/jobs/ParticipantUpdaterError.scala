package jobs

import models.ParticipantPK

sealed trait ParticipantUpdaterError extends Exception
case class ParticipantPKBadFormed(source: String) extends ParticipantUpdaterError
case class ParticipantNotFound(participantID: ParticipantPK) extends ParticipantUpdaterError
case class ParticipantCantUpdate(participantID: ParticipantPK) extends ParticipantUpdaterError
case class UnknowParticipantUpdateError(error: String) extends ParticipantUpdaterError
