package services

import java.io.{File, FileInputStream}
import java.nio.channels.FileChannel.MapMode._
import java.util.Scanner
import javax.inject._

import models._
import play.api.Logger
import utils.Util

trait LineReader {
  def getLine(number: Int): Either[Errors, String]
}

/**
  * implementation of LineReader that uses an index of lines
  */
@Singleton
class IndexedLineReader @Inject()(fileService: FileService) extends LineReader {

  val index = createIndex()
  index.left.map(ex => Logger.error("error reading file", ex))

  // setup the file we'll memmap into
  // we don't care about managing this, it's lifetime should match the JVM's lifetime
  val file = new File(fileService.getFilePath())
  val fileSize = file.length
  val inputstream = new FileInputStream(file)

  override def getLine(number: Int): Either[Errors, String] = {
    index.fold(
      e => Left(FileError),
      idx => {
        number match {
          case n if n <= 0 => Left(InvalidLineNumber)
          case n if n > idx.length => Left(LineNumberToLarge)
          case _ => {
            val start = idx(number - 1)
            val size = (if (number == idx.length) fileSize  else idx(number)) - idx(number - 1)
            val buffer = inputstream.getChannel.map(READ_ONLY, start, size)
            val bytes = new Array[Byte](size.toInt)
            buffer.get(bytes)
            Right(new String(bytes))
          }
        }
      }
    )
  }

  /**
    * create an index of lines in the our file
    * @return exception or a sequence of the start positions of lines
    */
  def createIndex() : Either[Exception, Seq[Int]] = {
    Util.cleanly(new FileInputStream(fileService.getFilePath()))(_.close) { is =>
      Util.cleanly(new Scanner(is, "UTF-8"))(_.close) { sc =>

        new Iterator[Int] {
          override def hasNext: Boolean = sc.hasNextLine

          override def next(): Int = {
            val idx = sc.`match`().start()
            sc.nextLine()
            idx
          }
        }.toList // force evaluation otherwise the scanner will fail when the underlying inputstream is closed
      }
    }.joinRight
  }
}
