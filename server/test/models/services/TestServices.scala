package models.services

import java.io.File
import scala.concurrent.Future

object TestServices {
  val parseReplayFileService: ParseReplayFileService =
    (file: File) => {
      import sys.process._
      val buffer = new StringBuilder()
      val path = "\"" + { file.getAbsolutePath } + "\""
      val res =
        s"/home/vmchura/Games/screp/cmd/screp/screp $path"
          .!(ProcessLogger(line => buffer.append(line)))
      if (res == 0) {
        Future.successful(Right(buffer.toString()))
      } else {
        println(buffer.toString())
        Future.failed(
          new IllegalArgumentException("screp not installed correctly")
        )
      }

    }
}
