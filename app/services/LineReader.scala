package services

import java.io.{File, FileInputStream, IOException}
import java.nio.channels.FileChannel.MapMode._
import javax.inject._

import collections.HugeList
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

      // bucket size: in the Project Gutenberg corpus, avg line length is 52
      // we'll set bucket size at 1/1000 the number of lines we think we'll have, so we don't waste too much memory
      val indexList = HugeList[Long](Math.min(fileSize / 52 / 1000, Int.MaxValue).toInt)

      // we need to process the file in 2-gig chunks because of limitations in the Java memory map API
      var start = 0L

      while (start < fileSize) {
        val chunkSize = if (start + Integer.MAX_VALUE > fileSize) fileSize - start else Integer.MAX_VALUE
        val buffer = inputstream.getChannel.map(READ_ONLY, start, chunkSize)

        // Scala's for-comprehension's range only works on Ints, which limits us to 2 gig file, so instead we use an iterator
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
          .foreach(bp => indexList.add(bp.pos))

        start += chunkSize
      }

      Right(indexList)
    } catch {
      case ioe: IOException => Left(ioe)
    }
  }
}
