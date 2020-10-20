package models.daos
import java.io.{File, FileInputStream}

import com.fasterxml.jackson.databind.JsonNode
import models.{DiscordID, GuildID, Smurf, ValidUserSmurf}
import models.services.SmurfService
import models.services.SmurfService.SmurfAdditionResult
import org.scalatest.Assertion
import org.scalatest.concurrent.PatienceConfiguration.{Interval, Timeout}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Seconds, Span}
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json.{JsArray, JsValue, Json}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Random
import language.{existentials, postfixOps}
import scala.concurrent.Future
class ValidUserSmurfDAOImplTest extends PlaySpec with GuiceOneAppPerSuite with ScalaFutures{
  implicit override val patienceConfig: PatienceConfig =
    PatienceConfig(timeout =  Span(10, Seconds), interval = Span(1, Seconds))

  val service: SmurfService = app.injector.instanceOf(classOf[SmurfService])
  val userGuildDAO: UserGuildDAO = app.injector.instanceOf(classOf[UserGuildDAO])

  "Smurf service" should {
    "no result of unknown user" in {
      val u = service.loadSmurfs(DiscordID(Random.nextString(12)))
      whenReady(u){ fv =>
        assert(fv.isEmpty)
      }
    }
    "result on insertion" in {
      val id = DiscordID(Random.nextString(12))
      val smurf = Smurf(Random.nextString(12))
      val insertion = service.addSmurf(id, smurf)
      val futResponse = for{
        _ <- service.addSmurf(id, smurf)
        loaded <- service.loadSmurfs(id)
      }yield{

        loaded
      }
      whenReady(insertion){ insertion =>
        assertResult(SmurfAdditionResult.Added)(insertion) }

      whenReady(futResponse){ fv =>
        assertResult(Some(Seq(smurf)))(fv.map(_.smurfs))
      }
    }
    "result valid response on insertions" in {
      val id = DiscordID(Random.nextString(12))
      val smurf = Smurf(Random.nextString(12))
      val validInsertion = service.addSmurf(id, smurf)
      val alreadyInserted = validInsertion.flatMap(_ => service.addSmurf(id, smurf))
      val alreadyHasOwner = validInsertion.flatMap(_ => service.addSmurf(DiscordID(Random.nextString(12)), smurf))
      def checkResult(result: Future[SmurfAdditionResult.AdditionResult],
                      expected: SmurfAdditionResult.AdditionResult): Assertion = {
        whenReady(result){ fv =>  assertResult(expected)(fv)}
      }

      checkResult(validInsertion, SmurfAdditionResult.Added)
      checkResult(alreadyInserted, SmurfAdditionResult.AlreadyRegistered)
      checkResult(alreadyHasOwner, SmurfAdditionResult.CantBeAdded)
    }
  }

  "ByPass smurfs legacy" should{
    "transfer all smurfs" in {
      val legacySmurfs = service.showAcceptedSmurfs().flatMap{
        userSmurfs => Future.sequence(userSmurfs.flatMap(u =>
                        u.matchSmurf.map(sm =>
                          service.addSmurf(DiscordID(u.discordUser.discordID),
                                          Smurf(sm.smurf))
                        ))
                      )
      }
      whenReady(legacySmurfs,Timeout(Span(120,Seconds)),Interval(Span(10,Seconds))){ responses =>
        println(responses.length)
        assert(responses.forall{
          case SmurfAdditionResult.Added => true
          case SmurfAdditionResult.AlreadyRegistered => true
          case _ => false
        })
      }
    }
  }

  "python Load and store  smurfs" should{
    "transfer all smurfs" in {
      val f = new File("/home/vmchura/Downloads/accountSmurfs.json")
      val i = new FileInputStream(f)
      trait RecordPython
      case class CompleteRecord(discordID: DiscordID, smurfs: Seq[Smurf]) extends RecordPython
      case class IncompleteRecord(userName: Option[String], id: Option[String], smurfs: Seq[String]) extends RecordPython
      val json= Json.parse(i).asInstanceOf[JsArray]
      val records: List[Either[IncompleteRecord,CompleteRecord]] = json.value.map{ v =>
        val smurfs = (v \ "smurfs").asOpt[Seq[String]].getOrElse(Seq.empty[String]).map(_.trim)
        (v \ "id").asOpt[String] match {
          case Some(id) if id.length == 18 && id.forall(_.isDigit) =>
            Right(CompleteRecord(DiscordID(id),smurfs.map(Smurf.apply)))
          case _ => Left(IncompleteRecord((v \ "username").asOpt[String], (v \ "id").asOpt[String],smurfs))
        }
      }.toList
      val incompletes = records.flatMap {
          case Left(x) => Some(x)
          case _ => None
      }
      val completes = records.flatMap{
        case Right(x) => Some(x)
        case _ => None
      }
      val smurfID: Seq[(Smurf,DiscordID)] = completes.flatMap(c => c.smurfs.map(s => (s,c.discordID)))
      val sameSmurfMultipleID: Map[Smurf, Seq[DiscordID]] = smurfID.groupBy(_._1).transform((_,v) => v.map(_._2).distinct).filter(_._2.length>1)
      sameSmurfMultipleID.foreach{
        case (k,v) => println(s"${k.name}: {${v.map(_.id).mkString(",")}}")
      }
      println("gg")
      val smurfsToPush: List[(Smurf, DiscordID)] = smurfID.groupBy(_._1).transform((_,v) => v.map(_._2).distinct).filter(_._2.length==1).transform((_,v) => v.head).toList
      val sh = service.loadValidSmurfs()
      trait ResultBySmurf
      case class AddSmurf(discordID: DiscordID, newSmurf: Smurf) extends ResultBySmurf
      case class SmurfAlreadyRegistered(discordID: DiscordID, newSmurf: Smurf) extends ResultBySmurf
      case class SmurfRegisteredOtherUser(smurf: Smurf, registeredOn: DiscordID, registerTo: DiscordID) extends ResultBySmurf
      case class CreateUser(discordID: DiscordID, smurf: Smurf) extends ResultBySmurf

      trait ResultIncompleteBySmurf
      case class SmurfRegisteredOnUser(discordID: DiscordID, smurf: Smurf) extends ResultIncompleteBySmurf
      case class SmurfNotRegistered(smurf: Smurf) extends ResultIncompleteBySmurf



      def findUserBySmurf(smurf: Smurf)(implicit lista: Seq[ValidUserSmurf]): Option[DiscordID] = {
        lista.find(_.smurfs.contains(smurf)).map(_.discordID)
      }
      def discordIDRegistered(discordID: DiscordID)(implicit lista: Seq[ValidUserSmurf]): Boolean = {
        lista.exists(_.discordID == discordID)
      }
      val resultsByIncompleteSmurfs: Future[Seq[ResultIncompleteBySmurf]] = sh.map{ implicit listValidSmurfs =>
        incompletes.flatMap(ir => ir.smurfs).map(Smurf.apply).map{ smurf =>
          findUserBySmurf(smurf) match {

            case Some(registeredDiscordID) => SmurfRegisteredOnUser(registeredDiscordID,smurf)
            case None => SmurfNotRegistered(smurf)
          }
        }
      }
      val resultsBySmurfs: Future[Seq[ResultBySmurf]] = sh.map{ implicit listValidSmurfs =>
        smurfsToPush.map{ case (smurf, discordID) =>
          findUserBySmurf(smurf) match {
            case Some(registeredDiscordID) if registeredDiscordID == discordID =>
              SmurfAlreadyRegistered(discordID,smurf)
            case Some(registeredDiscordID) =>
              SmurfRegisteredOtherUser(smurf, registeredDiscordID, discordID)
            case None =>
              if(discordIDRegistered(discordID))
                AddSmurf(discordID, smurf)
              else
                CreateUser(discordID,smurf)
          }
        }
      }



      def filterByClass[T](f: ResultBySmurf => Option[T])(data: Seq[ResultBySmurf]): Seq[T] = {
        data.flatMap(f)
      }
      def createUser: ResultBySmurf => Option[CreateUser] = {case a: CreateUser => Some(a)  case _ => None}
      def addSmurfToRegisteredUser: ResultBySmurf => Option[AddSmurf] = {case a: AddSmurf => Some(a)  case _ => None}
      def smurfRegistered: ResultBySmurf => Option[SmurfAlreadyRegistered] = {case a: SmurfAlreadyRegistered => Some(a)  case _ => None}
      def smurfCantBeRegistered: ResultBySmurf => Option[SmurfRegisteredOtherUser] = {case a: SmurfRegisteredOtherUser => Some(a)  case _ => None}

      val addNewSmurfToUserRegistered = resultsBySmurfs.map(filterByClass(addSmurfToRegisteredUser))
      val alreadyRegistered = resultsBySmurfs.map(filterByClass(smurfRegistered))
      val smurfsRegisteredOnOtherUser = resultsBySmurfs.map(filterByClass(smurfCantBeRegistered))
      val newUsersToCreate = resultsBySmurfs.map(filterByClass(createUser))


      whenReady(resultsByIncompleteSmurfs){ i =>
        println(s"Results incmplete")
        println(s"${i.mkString("\n")}")
      }
      //newUserToCreate -> Register on guilds

      //insertions on guilds

/*
      for{
        results <- resultsBySmurfs
        toInsertOnGuilds <- Future.successful(filterByClass{
          case a: CreateUser => Some(a)
          case _ => None
        }(results))
        //insert to guilds
        insertionsGuilds <- Future.sequence(toInsertOnGuilds.map(_.discordID).distinct.map(g => userGuildDAO.addGuildToUser(g, GuildID("736004357866389584"))))
        //all users now are registereds on guilds, add now all smurfs


      }yield{
        assert(insertionsGuilds.forall(i => i))
      }*/

    }
  }

}
