package models.daos

import java.util.UUID

import org.joda.time.DateTime
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

class TicketReplayDAOTest extends PlaySpec with GuiceOneAppPerSuite {
  val service: TicketReplayDAO = app.injector.instanceOf(classOf[TicketReplayDAO])
  val id = UUID.randomUUID()
  "Ticket Replay" should {
    "upload download correctly once"in
      {
          assert(service.ableToDownload(id,DateTime.now()))
          assert(service.ableToUpload(id,DateTime.now()))
      }
    "cant upload twice "in
      {
        val now = DateTime.now()
        service.uploading(id,now)
        assert(!service.ableToUpload(id,now.plusSeconds(9)))
        assert(service.ableToUpload(id,now.plusSeconds(11)))
        val after = now.plusSeconds(654)
        assert(service.ableToUpload(id,after))
        service.uploading(id,after)
        assert(!service.ableToUpload(id,after.plusSeconds(1)))
      }
    "cant download as robot "in
      {
        val now = DateTime.now()

        val downloadingAttemp = (1 to 50).map(_ =>
          {
            service.downloading(id,now.plusSeconds(1))
            service.ableToDownload(id,now.plusSeconds(1))
          })
        val (head,tail) = downloadingAttemp.span(u => u)
        assert(head.length <= 30)
        assert(head.length >= 29)
        assert(tail.forall(u => !u))
      }
  }


}
