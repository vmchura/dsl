package models.services

import org.scalatest.AsyncFlatSpec

class ChallongeUserServiceImplTest extends AsyncFlatSpec {

  behavior of "A Discord User Service"
  it should "get correct users" in {
    val s = new ChallongeUserServiceImpl()
    s.findChallongeTournament("vmchura","l1GGHLM82hbVsAlm5mS2q5wfDvx5nhk2ByP2fmFf")("-")("DeathfateStarLeague").map{
      tournament =>
        assertResult(Some(8415514))(tournament.map(_.tournament.challongID))
    }

  }
}
