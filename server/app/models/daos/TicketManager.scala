package models.daos

import java.util.UUID

import org.joda.time.DateTime

class TicketManager(secondsDelayed: Int,limitTicketsActive: Int){
  val tickets: scala.collection.concurrent.Map[UUID,List[DateTime]] = scala.collection.concurrent.TrieMap()
  def isAble(id: UUID, now: DateTime): Boolean = {
    tickets.get(id).fold(true){_.count(_.isAfter(now)) < limitTicketsActive }
  }
  def marksAsUsing(id: UUID, now: DateTime): Unit = tickets.update(id, tickets.get(id) match {
    case None => List(now.plusSeconds(secondsDelayed))
    case Some(expirations) => {
      now.plusSeconds(secondsDelayed) :: expirations.filter(_.isAfter(now))
    }
  })

}
