package models.services
import models.DiscordUser

import org.scalatest.AsyncFlatSpec

class DiscordUserServiceImplTest extends AsyncFlatSpec{

  behavior of "A Discord User Service"
  it should "get correct users" in {
      val s = new DiscordUserServiceImpl()
      s.findMembersOnGuild("NzI4MDU1NjY2NjEwMjc0MzI1.XwUKmQ.ktf1YVboGHzoLwVwoBQgP3NKJQs")("728442814832312372").map{
        users => assertResult(Seq(DiscordUser("698648718999814165","VmChQ")))(users)
      }

  }
}
