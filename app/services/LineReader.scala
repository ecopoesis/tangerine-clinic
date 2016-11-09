package services

import java.io.FileInputStream
import java.util.Scanner
import javax.inject._

import utils.Util

trait LineReader {
  def getLine(number: Long): Option[String]
}

/**
  * implementation of LineReader that generates an index of lines
  */
@Singleton
class IndexedLineReader @Inject()(fileService: FileService) extends LineReader {

  override def getLine(number: Long): Option[String] = {
    val i = index()
    i.right.foreach(println(_))
    None
  }

  def index() = {
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
