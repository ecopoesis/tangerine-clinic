package services

import java.io.{File, FileInputStream, IOException}
import java.nio.channels.FileChannel.MapMode._
import javax.inject._

import models._
import play.api.Logger

trait LineReader {
  def getLine(number: Long): Either[Errors, String]
}

/**
  * implementation of LineReader that uses an index of lines
  */
@Singleton
class IndexedLineReader @Inject()(fileService: FileService) extends LineReader {

  // setup the file we'll memmap into
  // we don't care about managing this, it's lifetime should match the JVM's lifetime
  val file = new File(fileService.getFilePath())
  val fileSize = file.length
  val inputstream = new FileInputStream(file)

  // create index
  val index = createIndex()
  index.left.map(ex => Logger.error("error reading file", ex))

  override def getLine(number: Long): Either[Errors, String] = {
    index.fold(
      e => Left(FileError),
      m => {
        number match {
          case n if n <= 0 => Left(InvalidLineNumber)
          case n if n > m.size => Left(LineNumberToLarge)
          case _ => {
            val start = m(number - 1)
            val size = (if (number == m.size) fileSize else m(number)) - m(number - 1)
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
    * create an index of lines in the source file
    * @return Either: exception or a map lines to their start positions
    */
  def createIndex() = {
    val newline = '\n'.toByte
    try {
      val buffer = inputstream.getChannel.map(READ_ONLY, 0, fileSize)

      case class BytePos(pos: Long, byte: Byte)

      // Scala's for-comprehension's range only works on Ints, which limits us to 2 gig file, so instead we use an iterator
      // we use a Map[Long, Long] instead of an Seq because Seq is a JVM Array and Array's use Int as an index, so we'd be limited to 2 billion lines
      Right(Iterator
        .iterate(0L)(_ + 1L)
        .takeWhile(_ < fileSize)
        .map(pos => BytePos(pos, buffer.get()))
        .withFilter(bp => bp.pos == 0 || bp.byte == newline)
        .zipWithIndex.foldLeft(scala.collection.mutable.Map[Long, Long]()) {
        (m, z) => m(z._2) = if (z._1.pos == 0) z._1.pos else z._1.pos + 1
          m
      })
    } catch {
      case ioe: IOException => Left(ioe)
    }
  }
}
