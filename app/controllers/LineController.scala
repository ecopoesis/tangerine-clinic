package controllers

import javax.inject._

import org.apache.commons.lang3.StringUtils
import play.api.mvc._
import services.LineReader

@Singleton
class LineController @Inject()(lineReader: LineReader) extends Controller {

  def getLine(n: Int) = Action {
    lineReader.getLine(n).fold(
      e => Status(e.code),
      s => Ok(StringUtils.appendIfMissing(s, "\n"))
    )
  }
}
