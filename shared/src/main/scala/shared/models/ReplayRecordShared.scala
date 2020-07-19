package shared.models

import java.util.UUID
import upickle.default.{ macroRW, ReadWriter => RW }

case class ReplayRecordShared(replayID: UUID,
                              matchName: String, nombreOriginal: String,
                              enabled: Boolean)
object ReplayRecordShared {
  implicit val rw: RW[ReplayRecordShared] = macroRW
}
