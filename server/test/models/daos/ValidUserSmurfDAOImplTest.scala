package models.daos
import java.io.{File, FileInputStream}

import com.fasterxml.jackson.databind.JsonNode
import models.{DiscordID, GuildID, Smurf, ValidUserSmurf}
import models.services.SmurfService
import models.services.SmurfService.SmurfAdditionResult
import models.services.SmurfService.SmurfAdditionResult.Added
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
import cats.Traverse
import cats.instances.future._ // for Applicative
import cats.instances.list._
class ValidUserSmurfDAOImplTest extends PlaySpec with GuiceOneAppPerSuite with ScalaFutures{
  implicit override val patienceConfig: PatienceConfig =
    PatienceConfig(timeout =  Span(20, Seconds), interval = Span(1, Seconds))

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

  def traverse[A,R](a: List[A])(f: A => Future[R]): Future[List[R]] = {
    a.foldLeft(Future.successful(List.empty[R])){
      case (listaFut,a) => for{
        lista <- listaFut
        r <- f(a)
      }yield{
        r :: lista
      }
    }
  }

  "ByPass smurfs legacy" should{
    "transfer all smurfs" in {

      val legacySmurfs = for{
        userSmurfs <- service.showAcceptedSmurfs()
        guildInsertion <- traverse(userSmurfs.map(_.discordUser.discordID).map(DiscordID.apply).distinct.toList)( id =>
          userGuildDAO.addGuildToUser(id,GuildID("699897834685857792")))
        lista <- Future.successful(userSmurfs.flatMap(u => u.matchSmurf.map(_.smurf).map(Smurf.apply).distinct.map(smurf => (DiscordID(u.discordUser.discordID),smurf))).toList)
        smurfs <- traverse(lista){ case (id,smurf) =>  service.addSmurf(id,smurf)}

      }yield{
        println(lista.map(_._1).distinct.length)
        println(guildInsertion.count(x => x))
        println(guildInsertion.count(x => !x))
        smurfs
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
    /**
     *
     *  Process A, parse .json
     *    SU <- SmurfUser: (smurf, discordID)
     *    S <- Smurf: (smurf)
     *
     *  Process B
     *    Eliminar los Smurf que ya estén en smurfUser
     *    SFree <- S filtered not in SU
     *
     *  Process C, detectar los smurf que estén asociados a más de un codigo de usuario
     *    SM <- SmurfMultiple: smurf, Seq[discordID]
     *    SUnique <- SU filtered not in SM
     *
     *  Process R
     *    Registered <- Load Registered
     *
     *  Process D, Eliminar los Smurf que estén registrados
     *    SU-!Registered <- SUnique filtered not in Registered smurfs
     *
     *
     *  Process E, convertir todos los smurf a SmurfUser
     *    SU-ToRegister <- SU-!Registered + SM + SUnique
     *
     *  Process F,registrar a los discordID que no estén inscritos
     *    SU-ToRegister =>
     *
     *  Process G, filtrar los ya registrados y los que no corresponden al usuario mostrarlos
     *
     *    SU-ReadyToRegister <- SU-ToRegister filtered already registered
     *                                        if doesnt correspond => show
     *
     *  Process H, Registrar los Smurf restantes
     *
     */

    "transfer all smurfs" in {
      sealed trait SmurfTest
      case class SmurfUser(smurf: Smurf, discordID: DiscordID) extends SmurfTest
      case class SmurfFree(smurf: Smurf) extends SmurfTest
      type SeqSmurf = Seq[SmurfUser]
      type SmurfPair = (SeqSmurf,Seq[SmurfFree])

      def processA(input: File): SmurfPair = {
        sealed trait RecordPython
        case class CompleteRecord(discordID: DiscordID, smurfs: Seq[Smurf]) extends RecordPython
        case class IncompleteRecord(userName: Option[String], smurfs: Seq[Smurf]) extends RecordPython

        val i = new FileInputStream(input)
        val json= Json.parse(i).asInstanceOf[JsArray]
        val records: List[RecordPython] = json.value.map{ v =>
          val smurfs = (v \ "smurfs").asOpt[Seq[String]].getOrElse(Seq.empty[String]).map(_.trim)
          (v \ "id").asOpt[String] match {
            case Some(id) if id.length == 18 && id.forall(_.isDigit) =>
              CompleteRecord(DiscordID(id),smurfs.map(Smurf.apply))
            case _ => IncompleteRecord((v \ "username").asOpt[String],smurfs.map(Smurf.apply))
          }
        }.toList

        val smurfUser = records.flatMap{
          case CompleteRecord(discordID, smurfs) => smurfs.map(smurf => SmurfUser(smurf, discordID))
          case _ => Nil
        }
        val smurfFree = records.flatMap{
          case IncompleteRecord(_, smurfs) => smurfs.map(smurf => SmurfFree(smurf))
          case _ => Nil
        }
        (smurfUser,smurfFree)

      }
      def processB(input: SmurfPair): SmurfPair = {
        val (smurfUser, smurfFree) = input
        (smurfUser, smurfFree.filterNot(smurf => smurfUser.exists(_.smurf == smurf.smurf)))
      }
      def processC(input: SmurfPair): SmurfPair = {
        val (smurfUser, smurfFree) = input
        val multiple = smurfUser.groupBy(_.smurf).map{case (smurf, seqSmurf) => (smurf, seqSmurf.map(_.discordID))}
        val parsed = multiple.map{
          case (smurf, Seq(discordID)) => SmurfUser(smurf, discordID)
          case (smurf @ Smurf("[DF]vlady1K"), _) => SmurfUser(smurf, DiscordID("703255446940549251"))
          case (smurf @ Smurf("N.Tank"), _) => SmurfUser(smurf, DiscordID("712807917061144697"))
          case (smurf, smurfs) if smurfs.distinct.length == 1 => SmurfUser(smurf, smurfs.head)
          case x => throw new IllegalArgumentException(s"What? error *$x*")
        }
        (parsed.toList, smurfFree)
      }
      def processR(): SeqSmurf = {
        service.loadValidSmurfs().futureValue.flatMap{
          case ValidUserSmurf(discordID, smurfs) => smurfs.map(smurf => SmurfUser(smurf,discordID))
        }
      }
      def processD(registered: SeqSmurf)(input: SmurfPair): SmurfPair = {
        val (smurfUser, smurfFree) = input
        (smurfUser, smurfFree.filterNot(smurf => registered.exists(_.smurf == smurf.smurf)))
      }
      def processE(input: SmurfPair): SeqSmurf = {
        val (smurfUser, smurfFree) = input
        smurfUser.toList ::: smurfFree.map(smurf => SmurfUser(smurf.smurf,DiscordID("763382248127987722"))).toList
      }
      def processF(registered: SeqSmurf)(persistent: Boolean)(input: SeqSmurf): SeqSmurf = {
        val usersToRegister = input.map(_.discordID).distinct.filterNot(id => registered.exists(_.discordID == id))
        if(traverse(usersToRegister.toList){ id =>
          println(s"Adding to guild: $id: ${input.find(_.discordID == id)}")
          if(persistent)
            userGuildDAO.addGuildToUser(id,GuildID("699897834685857792"))
          else
            Future.successful(true)
        }.futureValue.forall(f => f)){
          input
        }else{
          throw new IllegalStateException("insert on guild not good")
        }

      }
      def processG(registered: SeqSmurf)(input: SeqSmurf): SeqSmurf = {
        input.filter{ i =>
          registered.find(_.smurf == i.smurf) match {
            case Some(r) =>
              if(r.discordID != i.discordID)
                println(s"$i != $r")
              false
            case None =>  true
          }
        }
      }

      def processH(persistent: Boolean)(input: SeqSmurf): Boolean = {
        traverse(input.toList){ i =>
          println(s"Adding smurf: $i")
          if(persistent)
            service.addSmurf(i.discordID,i.smurf)
          else
            Future.successful(Added)
        }.futureValue.forall{_ == Added}
      }



      /*
      [DF]vlady1K: {706106144782942219,703255446940549251}
      N.Tank: {720811894918610945,712807917061144697}
       */

      val registered = processR()
      val persistent = true
      val process = processA _  andThen
                    processB  andThen
                    processC andThen
                    processD(registered) andThen
                    processE andThen
                    processF(registered)(persistent) andThen
                    processG(registered) andThen
                    processH(persistent)
      val f = new File("/home/vmchura/Downloads/accountSmurfs.json")

      assert(process(f))

    }
  }

}
