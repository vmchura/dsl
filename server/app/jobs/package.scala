import scala.concurrent.{ExecutionContext, Future}

package object jobs {
  trait JobError extends Exception

  implicit class opt2Future[A](opt: Option[A]) {
    def withFailure[E <: JobError](f: E): Future[A] = opt match {
      case None => Future.failed(f)
      case Some(x) => Future.successful(x)
    }
  }
  implicit class flag2Future(flag: Boolean){
    def withFailure[E <: JobError](f: E): Future[Boolean] = if(flag) Future.successful(true) else Future.failed(f)
  }
  implicit class eitherError[A](either: Either[JobError,A]){
    def withFailure[E <: JobError]: Future[A] = either match {
      case Left(e) => Future.failed(e)
      case Right(a) => Future.successful(a)
    }
  }
  def convertToEither[A, E <: JobError](defaultError: String => E)(f: Future[A])(implicit executionContext: ExecutionContext): Future[Either[JobError,A]] = {
    f.map(v => Right(v)).recoverWith{
      case exception: JobError  => Future.successful(Left(exception))
      case exception => Future.successful(Left(defaultError(exception.toString)))
    }
  }
}
