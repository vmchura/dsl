package database

import org.scalatest.TestData
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import org.scalatest._
import org.scalatestplus.play._
import play.api.http.MimeTypes
import play.api.test._
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder

class TemporalDB extends PlaySpec with GuiceOneAppPerTest {

  override def newAppForTest(td: TestData): Application = {
    System.setProperty("config.resource", "test-application.conf")
    GuiceApplicationBuilder()
      .configure(Map("ehcacheplugin" -> "disabled"))
      .build()
  }
  "Creation test" should {
    "load config from config test ad-hoc" in {
      println(
        app.configuration
          .getOptional[String]("mongodb.uri")
      )
      assert(
        app.configuration
          .getOptional[String]("mongodb.uri")
          .exists(_.endsWith("dsl-test"))
      )
    }
  }
}
