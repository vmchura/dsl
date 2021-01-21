package database

import org.scalatest.{BeforeAndAfterEach, TestSuite}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

trait EmptyDBBeforeEach extends GuiceOneAppPerSuite with BeforeAndAfterEach {
  this: TestSuite =>
  System.setProperty("config.resource", "test-application.conf")
  override def beforeEach(): Unit = {
    import sys.process._
    "mongo dsl-test --eval \"db.dropDatabase();\"".!(ProcessLogger(_ => ()))

  }
}
