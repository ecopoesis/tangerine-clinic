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
          case _ =>
            val start = m(number - 1)
            val size = (if (number == m.size) fileSize else m(number)) - m(number - 1)
            val buffer = inputstream.getChannel.map(READ_ONLY, start, size)
            val bytes = new Array[Byte](size.toInt)
            buffer.get(bytes)
            Right(new String(bytes))
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

      case class BytePos(pos: Long, byte: Byte)

      val onePercent = fileSize / 100
      var workingPercent = onePercent

      val indexMap = scala.collection.mutable.Map[Long, Long]()

      // we need to process the file in 2-gig chunks because of limitations in the Java memory map API
      var start = 0L

      while (start < fileSize) {
        val chunkSize = if (start + Integer.MAX_VALUE > fileSize) fileSize - start else Integer.MAX_VALUE
        val buffer = inputstream.getChannel.map(READ_ONLY, start, chunkSize)
        val prevLines = indexMap.size

        // Scala's for-comprehension's range only works on Ints, which limits us to 2 gig file, so instead we use an iterator
        // we use a Map[Long, Long] instead of an Seq because Seq is a JVM Array and Array's use Int as an index, so we'd be limited to 2 billion lines
        Iterator
          .iterate(0L)(_ + 1L)
          .takeWhile(_ < chunkSize)
          .map(pos => {
            if (pos + start >= workingPercent) {
              Logger.info((pos + start).toString + " bytes processed")
              workingPercent += onePercent
            }
            BytePos(pos, buffer.get())
          })
          .withFilter(bp => bp.pos == 0 || bp.byte == newline)
          .zipWithIndex.foldLeft(indexMap) {
          // 0 is the correct starting location of the first line, but additional lines need to have their starting location incremented by one since we matched newlines
          // z._2 is the index, z._1 is the BytePos
          (m, z) => m(z._2 + prevLines) = if (z._1.pos == 0) z._1.pos else z._1.pos + 1
            m
        }

        start += chunkSize
      }

      Right(indexMap)
    } catch {
      case ioe: IOException => Left(ioe)
    }
  }
}
